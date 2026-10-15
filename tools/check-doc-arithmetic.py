#!/usr/bin/env python3
"""核对文档算术:能力点行数 / 各状态计数 / 命中率是否自洽 / CI 冒烟数"""
import io
import re
import subprocess

ROOT = r"D:\SilentStudio\JQt - Dev"
doc = io.open(ROOT + r"\docs\compare-qtjambi.md", encoding="utf-8").read()
sec2 = doc[doc.find("## 2."):doc.find("## 3.")]

rows = []
for line in sec2.splitlines():
    m = re.match(r"^\| ([^|]+?) \| (✅|🟡|❌|🌉|⛔[^|]*) \|(.*)\|$", line)
    if m:
        rows.append((m.group(1).strip().strip("*"), m.group(2).strip(), m.group(3).strip()))

by = {}
for name, st, _ in rows:
    key = "⛔" if st.startswith("⛔") else st
    by.setdefault(key, []).append(name)

print(f"第 2 节表格行数: {len(rows)}")
for k in ("✅", "🟡", "❌", "🌉", "⛔"):
    if k in by:
        print(f"  {k} × {len(by[k])}: {', '.join(by[k])}")

# 命中率自洽性
ok = len(by.get("✅", []))
partial = [n for n in by.get("🟡", []) if "不计入" not in n]
help_excluded = len(by.get("🟡", [])) - len(partial)
print(f"\n  ✅ {ok} 个 · 🟡(计入){len(partial)} 个 · 🟡(不计入){help_excluded} 个 · ❌ {len(by.get('❌', []))} 个"
      f" · 🌉 {len(by.get('🌉', []))} 个 · ⛔ {len(by.get('⛔', []))} 个")
print(f"  宽松 = ✅ + 🟡(计入) = {ok + len(partial)}")
print(f"  严格 = ✅            = {ok}")
m = re.search(r"宽松\*\*\(✅ \+ 🟡 都算命中\) \| \*\*(\d+)/(\d+)", doc)
m2 = re.search(r"严格\*\*\(只算 ✅,🟡 不计\) \| \*\*(\d+)/(\d+)", doc)
print(f"  文档写的: 宽松 {m.group(1)}/{m.group(2) if m else '?'} · 严格 {m2.group(1)}/{m2.group(2) if m2 else '?'}")

# CI 冒烟数
ci = io.open(ROOT + r"\.github\workflows\ci.yml", encoding="utf-8").read()
# PowerShell 清单是全集
psl = re.search(r'foreach \(\$cls in @\((.*?)\)\)', ci, re.S)
names = re.findall(r'"(org\.jqt\.\w+)"', psl.group(1)) if psl else []
print(f"\n  CI PowerShell 清单冒烟数: {len(names)}")
print(f"  文档写的 27 个: {'✅ 一致' if len(names) == 27 else '❌ 不一致'}")
smoke_files = len([f for f in __import__("os").listdir(ROOT + r"\java\org\jqt") if f.startswith("Smoke")])
print(f"  冒烟文件总数: {smoke_files}(文档写 33: {'✅' if smoke_files == 33 else '❌'})")
