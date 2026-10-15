#!/usr/bin/env python3
"""改正 P1 的口径:P1 是 5 项,不是 4 项 —— DBus 那项一直没做。"""
import io

P = r"D:\SilentStudio\JQt - Dev\docs\compare-qtjambi.md"
s = io.open(P, encoding="utf-8").read()
before = s

s = s.replace(
    "**P1 4/4 已完成**:SQL 模型 ✅、QWebSocket 客户端 ✅、QTest 兼容层 ✅、"
    "**QStateMachine ✅(官方未分发该模块,自研纯 Java 实现补齐)**",
    "**P1 4/5**:SQL 模型 ✅、QWebSocket 客户端 ✅、QTest 兼容层 ✅、"
    "**QStateMachine ✅(官方未分发该模块,自研纯 Java 实现补齐)**;"
    "**`DBus` 仍未暴露** —— 该项一直写着「桥里已有,补 Java API 即可」但没人做"
    "(桥内其实已在用 QtDBus:屏保抑制/桌面通知)。这是 P1 最后一项")

s = s.replace("| **QStateMachine** | 状态机;比手写 if-else 稳 |",
              "| **QStateMachine** | ✅ 已完成(自研纯 Java 实现;官方未分发该模块) |")

s = s.replace("| **DBus(暴露已有桥能力)** | 桥里已有,补 Java API 即可 |",
              "| **DBus(暴露已有桥能力)** | ⏳ **未做**:桥内已在用 QtDBus(屏保抑制/桌面通知),"
              "待补 `QDBusConnection` / `QDBusMessage` 的 Java API |")

io.open(P, "w", encoding="utf-8", newline="\n").write(s)
print("已改:" if s != before else "(无变化)")
for pat in ("P1 4/5", "DBus 仍未暴露", "⏳ **未做**"):
    hit = [l for l in s.splitlines() if pat in l]
    print(f"  [{pat}] " + (hit[0][:120] if hit else "❌ 未找到"))
