#!/usr/bin/env python3
"""JQt 声称 vs 证据 全量对账

对每个能力点检查四件事,缺一即标注:
  ① Java 类真的存在吗(源码文件)
  ② 原生桥里有对应实现吗(JNI 符号)  —— 纯 Java 项豁免
  ③ 有冒烟覆盖吗(Smoke*.java 里出现)
  ④ 该冒烟在 CI 里是**真跑**还是 **SKIP**(CI 缺模块/缺后端时会跳过)

另外核对文档里的硬数据(类数/native 数/命中率算法)与实测是否一致。
"""
import io
import os
import re
import subprocess

ROOT = r"D:\SilentStudio\JQt - Dev"
os.chdir(ROOT)

API_DIR = "java/org/jqt"
BRIDGE = "native/jqt_bridge.cpp"


def read(p):
    return io.open(p, encoding="utf-8", errors="replace").read()


api_files = {f[:-5] for f in os.listdir(API_DIR)
             if f.endswith(".java") and not f.startswith("Smoke")}
smoke_files = sorted(f for f in os.listdir(API_DIR) if f.startswith("Smoke"))
bridge = read(BRIDGE)
ci = read(".github/workflows/ci.yml")
cmp_doc = read("docs/compare-qtjambi.md")

# CI 里跑的冒烟类(三处清单合并)
ci_smokes = set(re.findall(r"org\.jqt\.(Smoke\w+)", ci))

# 从文档第 2 节表格抽行
rows = []
for line in cmp_doc.splitlines():
    m = re.match(r"\| ([^|]+?) \| (✅|🟡|❌|🌉|⛔[^|]*) \|(.*)\|", line)
    if m:
        rows.append((m.group(1).strip().strip("*"), m.group(2).strip(), m.group(3).strip()))

print(f"能力点行数: {len(rows)}")
print()
print(f"{'能力点':<28} {'声称':<10} {'Java':<5} {'原生':<5} {'冒烟':<5} {'CI':<10} 判定")
print("-" * 96)

problems = []
for name, claim, note in rows:
    # ① 文档说明里出现的反引号类名
    classes = re.findall(r"`([A-Za-z_][A-Za-z0-9_]*)`", note)
    classes = [c for c in classes if c not in ("JQT_HAVE_CHARTS", "JQT_HAVE_WEBSOCKETS",
                                               "JQT_HAVE_QML", "JQT_HAVE_MULTIMEDIA",
                                               "JQT_HAVE_POSITIONING", "JQT_HAVE_HELP",
                                               "JQT_HAVE_SVG", "JQT_HAVE_NETWORK",
                                               "JQT_HAVE_SQL_MODELS")]
    found = [c for c in classes if c in api_files]
    missing = [c for c in classes if c not in api_files]
    has_java = "是" if found else ("—" if not classes else "部分")

    # ② 原生实现是否存在(任一相关类有 JNI 符号)
    native_ok = any(f"Java_org_jqt_{c}_" in bridge for c in found) if found else None

    # ③ 冒烟覆盖
    smoke_hits = []
    for s in smoke_files:
        body = read(os.path.join(API_DIR, s))
        if any(c in body for c in found):
            smoke_hits.append(s[:-5])
    # ④ CI 覆盖
    ci_hits = [s for s in smoke_hits if s in ci_smokes]

    flag = ""
    if claim in ("✅",) and (not found or (native_ok is False)):
        flag = "❗声称 ✅ 但证据不足"
    elif claim in ("✅",) and not ci_hits:
        flag = "⚠️ 有冒烟但 CI 未跑"
    elif claim == "🟡":
        flag = "(部分:文档已标 🟡)"
    if missing and claim == "✅":
        flag = f"❗文档提到但无该类: {','.join(missing[:3])}"
    if flag.startswith("❗"):
        problems.append((name, claim, flag))

    print(f"{name[:27]:<28} {claim[:9]:<10} {has_java:<5} "
          f"{('有' if native_ok else ('无' if native_ok is False else '—')):<5} "
          f"{(str(len(smoke_hits)) if smoke_hits else '无'):<5} "
          f"{(str(len(ci_hits)) if ci_hits else '无'):<10} {flag}")

print()
print("═══════ 硬数据核对 ═══════")
api_count = len(api_files)
java_lines = sum(len(read(os.path.join(API_DIR, f)).splitlines()) for f in os.listdir(API_DIR)
                 if f.endswith(".java"))
natives = len(re.findall(r"^JNIEXPORT .*?JNICALL (Java_org_jqt_\w+)", bridge, re.M))
print(f"  实测:API 类 {api_count} · Java 行 {java_lines} · JNI 函数 {natives} · 冒烟文件 {len(smoke_files)} · CI 冒烟 {len(ci_smokes)}")
for pat, label in ((r"\| 类数 \| \*\*(\d+)", "文档声称类数"),
                   (r"\| Java 源文件 \| (\d+)", "文档声称源文件数"),
                   (r"命中 (\d+)/36", "文档声称命中"),
                   (r"命中率 \*\*(\d+)/36", "文档声称命中率分子")):
    m = re.search(pat, cmp_doc)
    if m:
        print(f"  {label}: {m.group(1)}")

print()
print("═══════ 结论 ═══════")
print(f"  发现 {len(problems)} 处需要修正的声称:")
for n, c, f in problems:
    print(f"    - {n} (声称 {c}): {f}")
