#!/usr/bin/env python3
"""P1 补齐后的文档更新:
  ① QStateMachine 行:⛔ 官方未分发 -> ✅ JQt 自研实现(附背景与差异说明)
  ② P1:3/4 -> 4/4
  ③ 命中率重算:宽松 29/36 ≈ 81%、严格 25/36 ≈ 69%(状态机从"缺失"变"命中")
  ④ 缺失项 7 -> 6
  ⑤ "Java 源文件"口径拆清楚(用户问过:这个统计混了库与测试,改成分开给数)
  ⑥ 平台表述约定:win64 / arm64 / macOS 统一写"真机构建",不加修饰
"""
import io
import re

P = r"D:\SilentStudio\JQt - Dev\docs\compare-qtjambi.md"
s = io.open(P, encoding="utf-8").read()
changed = []

# ① QStateMachine 行
m = re.search(r"\| \*\*QStateMachine 状态机\*\* \| ⛔ 官方未分发 \|[^|]*\|", s)
if m:
    s = s.replace(m.group(0),
        "| **QStateMachine 状态机** | ✅ **JQt 自研** | 官方**未分发**该模块(mingw/MSVC/Linux/macOS 各版本官方模块清单里都查不到,已多路核实),"
        "因此按 Qt 文档语义**自研纯 Java 实现**:`QStateMachine` / `QState` / `QFinalState` / `QHistoryState` / "
        "`QAbstractTransition` / `QEventTransition`(含并行状态、历史状态、错误状态、嵌套终态、跨线程 postEvent)。"
        "零原生依赖 → 五平台都会真跑,不存在 SKIP。三处刻意差异见 `QStateMachine` 类注释 |", 1)
    changed.append("QStateMachine → ✅ 自研")

# ② P1 3/4 -> 4/4
s2 = s.replace("**P1 3/4**:SQL 模型 ✅、QWebSocket 客户端 ✅、QTest 兼容层 ✅(**QStateMachine 官方未分发该模块,已多路核实并记录**)",
               "**P1 4/4 已完成**:SQL 模型 ✅、QWebSocket 客户端 ✅、QTest 兼容层 ✅、"
               "**QStateMachine ✅(官方未分发该模块,自研纯 Java 实现补齐)**")
if s2 != s:
    s = s2
    changed.append("P1 → 4/4")

# ③ 命中率
s2 = s.replace("| **宽松**(✅ + 🟡 都算命中) | **28/36 ≈ 78%** | 🟡 项 = 只做了子集 |",
               "| **宽松**(✅ + 🟡 都算命中) | **29/36 ≈ 81%** | 🟡 项 = 只做了子集 |")
s2 = s2.replace("| **严格**(只算 ✅,🟡 不计) | **24/36 ≈ 67%** | 这才是\"完整可用\"的真实比例 |",
                "| **严格**(只算 ✅,🟡 不计) | **25/36 ≈ 69%** | 这才是\"完整可用\"的真实比例 |")
if s2 != s:
    s = s2
    changed.append("命中率 29/36 · 25/36")

# ④ 缺失项
s2 = s.replace("另:**7 项仍缺失**、`QStateMachine` 官方未分发、`DBus` 属\"桥内有、API 无\"。",
               "另:**6 项仍缺失**(WebEngine、3D、Concurrent、Bluetooth/NFC、QML 广度、Help 写入),"
               "`DBus` 属\"桥内有、API 无\"。")
if s2 != s:
    s = s2
    changed.append("缺失 7 → 6")

# ⑤ Java 源文件口径
s2 = s.replace("| Java 源文件 | 194(API 162 + 冒烟 31) |",
               "| API 类 | **169**(java/org/jqt 下非 Smoke 的 .java) |\n"
               "| 冒烟类 | **33**(Smoke*.java) |\n"
               "| Java 源文件合计 | 202(API + 冒烟;此前的\"源文件数\"把两者混在一起,口径不清,故拆开) |")
if s2 != s:
    s = s2
    changed.append("源文件口径拆分")

# ⑥ 平台表述约定
if "平台表述约定" not in s:
    anchor = "## 5. 复现本文所有数字"
    add = """## 4.1 平台表述约定

Windows x64、Windows ARM64、macOS 三个平台的构建与验证，文档与提交信息里统一写作「**真机构建**」
（这三条线跑在真实机器/真实 ARM 硬件上，不是容器）—— 如实记录，不加修饰词。
需要时再补具体环境（Qt 版本、编译器、机器）。

## 5. 复现本文所有数字"""
    s = s.replace(anchor, add, 1)
    changed.append("新增平台表述约定")

io.open(P, "w", encoding="utf-8", newline="\n").write(s)
print("已更新:" + "、".join(changed))
for pat in ("QStateMachine 状态机", "P1 4/4", "29/36", "25/36", "API 类", "平台表述约定", "6 项仍缺失"):
    hit = re.search(rf"[^\n]*{re.escape(pat)}[^\n]*", s)
    print(f"  [{pat}] {(hit.group(0)[:105] if hit else '❌ 未找到')}")
