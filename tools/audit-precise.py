#!/usr/bin/env python3
"""精确对账(只查能确定的事,不猜):
  ① CI 里每个冒烟的 SKIP/真跑 情况(取最近一次成功 run 的日志,分平台)
  ② 文档里出现的类名(反引号包裹、以 Q/JQt 开头)哪些**根本不存在**
  ③ 文档硬数据(类数/行数/命中率算法)与实测差异
"""
import io
import json
import os
import re
import subprocess

ROOT = r"D:\SilentStudio\JQt - Dev"
os.chdir(ROOT)
api_dir = "java/org/jqt"
api_files = {f[:-5] for f in os.listdir(api_dir) if f.endswith(".java")}
doc = io.open("docs/compare-qtjambi.md", encoding="utf-8").read()

print("═══════ ① 文档提到的类名 → 是否存在 ═══════")
sec = doc[doc.find("## 2."):doc.find("## 3.")]
missing = []
for line in sec.splitlines():
    if not line.startswith("|"):
        continue
    name = line.split("|")[1].strip().strip("*")
    for tok in re.findall(r"`([A-Za-z_][A-Za-z0-9_]*)`", line):
        if not re.match(r"^(Q|JQt)[A-Za-z0-9_]+$", tok):
            continue
        if tok not in api_files:
            missing.append((name, tok, line.strip()[:110]))
if missing:
    for name, tok, ctx in missing:
        print(f"  ❗ [{name}] 提到的 `{tok}` 没有对应 Java 类")
        print(f"      原文: {ctx}")
else:
    print("  ✅ 文档里提到的类名全部存在(方法名/明确标注缺失的除外)")

print()
print("═══════ ② CI 里 SKIP 情况(最近成功 run)═══════")
run_id = subprocess.run(["gh", "run", "list", "--limit", "5", "--json", "databaseId,conclusion,headSha"],
                        capture_output=True, text=True).stdout
try:
    runs = json.loads(run_id)
    rid = next(r["databaseId"] for r in runs if r["conclusion"] == "success")
except Exception as e:
    rid = None
    print("  取 run 失败:", e)
if rid:
    log = subprocess.run(["gh", "run", "view", str(rid), "--log"], capture_output=True, text=True,
                         encoding="utf-8", errors="replace").stdout
    # 每个冒烟的 SKIP 行
    skips = re.findall(r"(\S+)\s+(\S+)\s+.*?\[(\w+)\] SKIP ([^\n]*)", log)
    per_platform = {}
    for line in log.splitlines():
        m = re.search(r"^\S+\s+(\S+)\s+\S*\s*(.*)$", line)
    # 简单粗暴:找形如 "  [tag] SKIP ..." 并记录前一行里的平台信息
    cur_platform = "?"
    counts = {}
    for line in log.splitlines():
        for plats in ("Linux x64", "Windows x64", "Windows ARM64", "macOS x64"):
            if plats in line:
                cur_platform = plats
        m = re.search(r"\[[a-z0-9]+\] SKIP ([^\r\n]*)", line)
        if m:
            counts.setdefault(cur_platform, []).append(m.group(1).strip()[:90])
    for plat, items in counts.items():
        print(f"  {plat}: {len(items)} 处 SKIP")
        for it in items[:8]:
            print(f"      - {it}")
    if not counts:
        print("  ✅ 该 run 日志里没有 SKIP")
    print(f"  (run {rid})")

print()
print("═══════ ③ 硬数据核对 ═══════")
java_total = len([f for f in os.listdir(api_dir) if f.endswith(".java")])
smoke_n = len([f for f in os.listdir(api_dir) if f.startswith("Smoke")])
print(f"  实测:API 类 {len(api_files) - smoke_n} · 冒烟 {smoke_n} · 合计 {java_total}")
m = re.search(r"\| 类数 \| \*\*(\d+)", doc)
m2 = re.search(r"\| Java 源文件 \| (\d+)", doc)
if m: print(f"  文档:类数 {m.group(1)}")
if m2: print(f"  文档:源文件 {m2.group(1)}")
m3 = re.search(r"命中 (\d+)/36", doc)
print(f"  文档:命中 {m3.group(1) if m3 else '?'}/36 —— 其中 🟡 项按'命中'计入")
partials = re.findall(r"\| ([^|]+?) \| 🟡[^|]*\|", sec)
print(f"  被计为命中的 🟡 项({len(partials)} 个): {', '.join(p.strip().strip('*') for p in partials)}")
