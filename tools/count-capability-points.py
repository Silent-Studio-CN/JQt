#!/usr/bin/env python3
"""权威口径(修正版):只数"能力矩阵"那张表;CI 覆盖按整份 ci.yml 的并集算"""
import io
import os
import re

ROOT = r"D:\SilentStudio\JQt - Dev"
doc = io.open(ROOT + r"\docs\compare-qtjambi.md", encoding="utf-8").read()

# 只取能力矩阵:从表头 "| 能力 | JQt | 说明 |" 起,到第一个非表格行为止
start = doc.find("| 能力 | JQt | 说明 |")
rows = []
if start >= 0:
    for line in doc[start:].splitlines()[2:]:          # 跳过表头与分隔线
        if not line.startswith("|"):
            break
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) >= 2:
            rows.append((cells[0].strip("*"), cells[1]))


def kind(st):
    if st.startswith("✅"): return "ok"
    if st.startswith("🟡"): return "partial_no" if "不计入" in st else "partial"
    if st.startswith("❌"): return "missing"
    if st.startswith("🌉"): return "bridge"
    if st.startswith("⛔"): return "blocked"
    return "other"


counts = {}
for n, s in rows:
    counts.setdefault(kind(s), []).append(n)

total = len(rows)
ok, partial = len(counts.get("ok", [])), len(counts.get("partial", []))
partial_no, missing = len(counts.get("partial_no", [])), len(counts.get("missing", []))
bridge = len(counts.get("bridge", []))

print(f"能力矩阵行数(权威分母): {total}")
print(f"  ✅ 完整 {ok} · 🟡 部分(计入){partial} · 🟡 部分(不计入){partial_no}"
      f" · ❌ 缺失 {missing} · 🌉 {bridge}")
print(f"  🟡 明细: {', '.join(counts.get('partial', []))}  +  不计入: {', '.join(counts.get('partial_no', []))}")
print(f"  ❌ 明细: {', '.join(counts.get('missing', []))}")
print(f"\n  宽松(✅+🟡计入) = {ok}+{partial} = {ok+partial}/{total} ≈ {round((ok+partial)/total*100)}%")
print(f"  严格(只算 ✅)   = {ok}/{total} ≈ {round(ok/total*100)}%")

ci = io.open(ROOT + r"\.github\workflows\ci.yml", encoding="utf-8").read()
lists = []
for m in re.finditer(r'(?:for C in|foreach \(\$cls in @\()(.*?)(?:; do|\)\))', ci, re.S):
    names = re.findall(r'(org\.jqt\.Smoke\w+)', m.group(1))
    if names:
        lists.append(names)
union_all = sorted(set(re.findall(r'org\.jqt\.(Smoke\w+)', ci)))
files = sorted(f[:-5] for f in os.listdir(ROOT + r"\java\org\jqt") if f.startswith("Smoke"))
print(f"\n  CI 清单跑: {' / '.join(str(len(l)) for l in lists)}；整份 ci.yml 引用冒烟并集 = {len(union_all)} 个")
print(f"  冒烟文件总数 {len(files)}；未被 CI 引用: {', '.join(sorted(set(files) - set(union_all))) or '(无)'}")
