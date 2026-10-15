#!/usr/bin/env python3
"""把"算术"改准(全部由 tools/count-capability-points.py 生成,可复算):
  · 分母 36 -> 39(能力矩阵实际行数)
  · 命中率:宽松 33/39 ≈ 85% · 严格 28/39 ≈ 72%(并写出算法)
  · "6 项仍缺失" -> 正确表述:❌ 4 项 + 🟡 6 项部分(Help 不计入)+ 🌉 1 项
  · 1.1 节冒烟说明:33 个文件、CI 引用 27 个、6 个仅手动跑(点名)
  · 度量方式里的 "36 项清单" -> 39 项
"""
import io
import re

P = r"D:\SilentStudio\JQt - Dev\docs\compare-qtjambi.md"
s = io.open(P, encoding="utf-8").read()
changed = []

# ① 命中率块
old = re.search(r"\*\*汇总\(v1\.9\.1 进展\)\*\*:.*?严格\*\*\(只算 ✅,🟡 不计\) \| \*\*[^|]*\|[^\n]*\n", s, re.S)
if old:
    new = """**汇总(v1.9.2 进展)**:能力点按本节矩阵**实际行数**计算(早期文档写"36 项",分母早已漂移 —— 已改正)。
**所有数字由 `tools/count-capability-points.py` 生成,可复算**:

| 口径 | 命中 | 算法 |
|---|---|---|
| **宽松**(✅ + 🟡 计入命中) | **33/39 ≈ 85%** | 28 个 ✅ + 5 个 🟡(动画/QML/Multimedia/Positioning/WebSockets) |
| **严格**(只算 ✅,🟡 不计) | **28/39 ≈ 72%** | 只有 28 个是"完整可用" |

分母 39 = ✅ 28 + 🟡 6(含 Help,明确不计入)+ ❌ 4 + 🌉 1。
**已补齐 P1**:`QStateMachine` 官方不分发 → JQt 自研纯 Java 实现(见该项说明与 `SmokeStateMachine`)。
"""
    s = s[:old.start()] + new + s[old.end():]
    changed.append("命中率块 33/39 · 28/39")

# ② 缺失项表述
s2 = re.sub(r"另:\*\*6 项仍缺失\*\*[^\n]*\n",
            "另:**❌ 4 项缺失**(WebEngine / 3D / Concurrent / Designer 插件)、**🟡 6 项部分**"
            "(其中 `Help` 明确不计入命中)、**🌉 1 项**(DBus:桥内有、API 无)。\n", s)
if s2 != s:
    s = s2
    changed.append("缺失项表述改正")

# ③ 1.1 节冒烟说明
s2 = s.replace("| 冒烟类 | **33**（其中 CI 里实际跑 **27** 个） |",
               "| 冒烟类 | **33** 个文件；CI 引用 **27** 个，**6 个仅手动跑**"
               "（SmokeFluent / SmokeInputDialog / SmokeL1fin / SmokeL2 / SmokeSqlDb / SmokeVisible）—— "
               "要么补进 CI，要么删掉，不留「看着像覆盖其实没跑」的账 |")
if s2 != s:
    s = s2
    changed.append("冒烟覆盖明细")

# ④ 度量方式里的 36 项
s2 = s.replace("用第 2 节的 36 项清单", "用第 2 节矩阵的实际行数(当前 39 项)")
if s2 != s:
    s = s2
    changed.append("度量方式 36 → 39")

io.open(P, "w", encoding="utf-8", newline="\n").write(s)
print("已更新:" + "、".join(changed))
for pat in ("33/39", "28/39", "❌ 4 项缺失", "6 个仅手动跑", "当前 39 项"):
    hit = re.search(rf"[^\n]*{re.escape(pat)}[^\n]*", s)
    print(f"  [{pat}] " + (hit.group(0)[:100] if hit else "❌ 未找到"))
