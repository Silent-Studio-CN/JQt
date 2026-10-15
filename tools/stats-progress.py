#!/usr/bin/env python3
"""统计 JQt 当前进度(全部数字来自仓库/CI/节点实测,不凭印象)"""
import io
import json
import os
import re
import subprocess

ROOT = r"D:\SilentStudio\JQt - Dev"
os.chdir(ROOT)


def sh(cmd, shell=False):
    p = subprocess.run(cmd, capture_output=True, text=True, shell=shell, encoding="utf-8", errors="replace")
    return (p.stdout or "").strip()


def lines(path):
    return len(io.open(path, encoding="utf-8", errors="replace").read().splitlines())


print("═══════ ① 代码规模 ═══════")
java = []
for dp, _, fs in os.walk("java"):
    for f in fs:
        if f.endswith(".java"):
            java.append(os.path.join(dp, f))
api = [f for f in java if not re.search(r"Smoke|Demo|Modality", os.path.basename(f))]
smoke = [f for f in java if "Smoke" in os.path.basename(f)]
java_lines = sum(lines(f) for f in java)
api_lines = sum(lines(f) for f in api)
bridge = "native/jqt_bridge.cpp"
bridge_lines = lines(bridge)
src = io.open(bridge, encoding="utf-8", errors="replace").read()
natives = len(re.findall(r"^JNIEXPORT .*?JNICALL (Java_org_jqt_\w+)", src, re.M))
print(f"  Java 源文件      : {len(java)}  (API {len(api)} + 冒烟 {len(smoke)})")
print(f"  Java 行数        : {java_lines}  (API {api_lines})")
print(f"  原生桥 jqt_bridge: {bridge_lines} 行,导出 {natives} 个 JNI 函数")
# 特性探测宏
macros = sorted(set(re.findall(r"#if(?:def| defined\()\s*\(?(JQT_HAVE_\w+)", src)))
print(f"  可选模块特性宏    : {len(macros)} 个 -> {', '.join(m.replace('JQT_HAVE_', '') for m in macros)}")
# 冒烟脚本列表(在 CI 里跑的)
ci = io.open(".github/workflows/ci.yml", encoding="utf-8").read()
smoke_in_ci = sorted(set(re.findall(r"org\.jqt\.(Smoke\w+)", ci)))
print(f"  CI 中冒烟类       : {len(smoke_in_ci)} 个 -> {', '.join(smoke_in_ci)}")

print()
print("═══════ ② 路线图(P0/P1/P2)═══════")
cmp_doc = io.open("docs/compare-qtjambi.md", encoding="utf-8").read()
for pat, label in ((r"\*\*汇总[^\n]*", "命中率"),
                   (r"\*\*P0 [^\n]*", "P0"),
                   (r"\*\*P1 [^\n]*", "P1"),
                   (r"\*\*P2 [^\n]*", "P2"),
                   (r"\*\*[0-9]+ 项仍缺失[^\n]*", "缺口")):
    m = re.search(pat, cmp_doc)
    if m:
        print("  " + m.group(0).replace("**", "")[:150])
rows = re.findall(r"^\| \*\*(.+?)\*\* \| (✅|🟡|❌|⛔[^|]*)", cmp_doc, re.M)
if rows:
    print("  能力点明细:", " ".join(f"{n}{s}" for n, s in rows[:40]))

print()
print("═══════ ③ 版本与提交 ═══════")
print("  最新 tag   :", sh(["git", "describe", "--tags", "--abbrev=0"]))
print("  提交总数   :", sh(["git", "rev-list", "--count", "HEAD"]))
tag = sh(["git", "describe", "--tags", "--abbrev=0"])
if tag:
    print(f"   tag 之后   : {sh(['git', 'rev-list', '--count', f'{tag}..HEAD'])} 个提交(未发布)")
print("  最近 5 条  :")
for l in sh(["git", "log", "--oneline", "-5"]).splitlines():
    print("     " + l[:100])

print()
print("═══════ ④ CI ═══════")
runs = sh(["gh", "run", "list", "--limit", "3", "--json", "databaseId,conclusion,headSha,createdAt,displayTitle"])
try:
    for r in json.loads(runs or "[]"):
        print(f"  {r['databaseId']}  {r['conclusion'] or 'running':8s}  {r['headSha'][:7]}  {r['displayTitle'][:60]}")
except Exception:
    print("  (gh 不可用)")

print()
print("═══════ ⑤ 文档与工具 ═══════")
for d in ("docs", "tools", "themes", "Community"):
    if os.path.isdir(d):
        n = sum(len(fs) for _, _, fs in os.walk(d))
        print(f"  {d}/: {n} 个文件")
print("  构建脚本:", ", ".join(f for f in sorted(os.listdir(".")) if f.startswith("build") or f.startswith("run")))
