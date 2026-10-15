#!/usr/bin/env python3
"""文档数字 vs 代码实测 —— 不一致就失败(exit 1)。

接进 CI 后,只要有人改了代码却没同步文档数字(或反过来),CI 直接红。
这一条是"假账"的机械防线:人可以不记得,脚本不会。
"""
import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
os.chdir(ROOT)
DOC = "docs/compare-qtjambi.md"
D = "java/org/jqt"

doc = io.open(DOC, encoding="utf-8").read()

# ---------- 实测 ----------
files = sorted(f for f in os.listdir(D) if f.endswith(".java"))
api = [f for f in files if not f.startswith("Smoke")]
smokes = [f for f in files if f.startswith("Smoke")]
lines_of = lambda p: len(io.open(p, encoding="utf-8", errors="replace").read().splitlines())
java_lines = sum(lines_of(os.path.join(D, f)) for f in files)
bridge_path = "native/jqt_bridge.cpp"
bridge = io.open(bridge_path, encoding="utf-8", errors="replace").read()
bridge_lines = lines_of(bridge_path)
jni = len(re.findall(r"^JNIEXPORT .*?JNICALL (Java_org_jqt_\w+)", bridge, re.M))

# 能力矩阵
start = doc.find("| 能力 | JQt | 说明 |")
rows = []
if start >= 0:
    for line in doc[start:].splitlines()[2:]:
        if not line.startswith("|"):
            break
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) >= 2:
            rows.append(cells[1])
total = len(rows)
ok = sum(1 for c in rows if c.startswith("✅"))
partial = sum(1 for c in rows if c.startswith("🟡") and "不计入" not in c)
loose = ok + partial

# ---------- 文档声称 ----------
claims = {}
m = re.search(r"\| \*\*宽松\*\*[^|]*\| \*\*(\d+)/(\d+)", doc)
if m: claims["宽松"] = (int(m.group(1)), int(m.group(2)))
m = re.search(r"\| \*\*严格\*\*[^|]*\| \*\*(\d+)/(\d+)", doc)
if m: claims["严格"] = (int(m.group(1)), int(m.group(2)))
m = re.search(r"\| API 类 \| \*\*(\d+)\*\*", doc)
if m: claims["API 类"] = int(m.group(1))
m = re.search(r"\| 冒烟类 \| \*\*(\d+)\*\*", doc)
if m: claims["冒烟类"] = int(m.group(1))
m = re.search(r"CI 引用 \*\*(\d+)\*\* 个", doc)
if m: claims["CI 引用冒烟"] = int(m.group(1))
m = re.search(r"\| Java 源文件合计 \| (\d+)", doc)
if m: claims["源文件"] = int(m.group(1))
m = re.search(r"\| Java 行数 \| \*\*([\d,]+)\*\*", doc)
if m: claims["Java 行数"] = int(m.group(1).replace(",", ""))
m = re.search(r"\| JNI 桥行数 \| \*\*([\d,]+)\*\*", doc)
if m: claims["桥行数"] = int(m.group(1).replace(",", ""))
m = re.search(r"\| JNI 导出函数 \| \*\*([\d,]+)\*\*", doc)
if m: claims["JNI 导出"] = int(m.group(1).replace(",", ""))

# ---------- 对比 ----------
actual = {
    "宽松": (loose, total), "严格": (ok, total),
    "API 类": len(api), "冒烟类": len(smokes), "源文件": len(files),
    "Java 行数": java_lines, "桥行数": bridge_lines, "JNI 导出": jni,
    "CI 引用冒烟": len(set(re.findall(r"org\.jqt\.(Smoke\w+)",
                                      io.open(".github/workflows/ci.yml", encoding="utf-8").read()))),
}
bad = []
print("文档数字 vs 代码实测:")
for k, want in claims.items():
    got = actual.get(k)
    same = (got == want)
    print(f"  {k:<10} 文档={want}  实测={got}  {'✅' if same else '❌ 不一致'}")
    if not same:
        bad.append(k)
missing = [k for k in actual if k not in claims]
if missing:
    print(f"  ⚠️ 文档缺少这些指标: {', '.join(missing)}")

# P1 完成度自检:矩阵里不允许再出现"未做/未暴露"的字样而不被计数
if "仍未暴露" in doc or "⏳ **未做**" in doc:
    print("  ⚠️ 文档里仍写着某项「未做/未暴露」—— 请确认它不是被漏算的完成项")
    bad.append("P1 完成度文案")

if bad:
    print(f"\n❌ 文档与代码不一致: {', '.join(bad)}")
    print("   修法:跑 tools/measure-hard-data.py 与 tools/count-capability-points.py 取实测值,再更新文档。")
    sys.exit(1)
print("\n✅ 文档数字与代码一致")
