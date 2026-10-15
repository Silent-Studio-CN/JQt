#!/usr/bin/env python3
"""DBus 完成后按脚本实测数字更新文档(数字全部来自 measure-hard-data / count-capability-points)"""
import io

P = r"D:\SilentStudio\JQt - Dev\docs\compare-qtjambi.md"
s = io.open(P, encoding="utf-8").read()
changed = []

# ① 能力矩阵:DBus 🌉 -> ✅
s2 = s.replace("| **DBus** | 🌉 | 桥内部出现,未暴露 |",
               "| **DBus** | ✅ | v1.9.2 P1 收尾:`QDBusConnection`(会话/系统总线连接状态、调用、发信号)"
               "+ `QDBusMessage`(方法调用/信号构造、参数编组、错误与回复读取);"
               "`JQT_HAVE_DBUS` 特性探测。此前标 🌉(桥内有 API 无)—— 现已暴露 |")
if s2 != s:
    s = s2; changed.append("DBus 🌉 → ✅")

# ② P1 4/5 -> 5/5
s2 = s.replace("**P1 4/5**:SQL 模型 ✅、QWebSocket 客户端 ✅、QTest 兼容层 ✅、"
               "**QStateMachine ✅(官方未分发该模块,自研纯 Java 实现补齐)**;"
               "**`DBus` 仍未暴露** —— 该项一直写着「桥里已有,补 Java API 即可」但没人做"
               "(桥内其实已在用 QtDBus:屏保抑制/桌面通知)。这是 P1 最后一项",
               "**P1 5/5 已完成**:SQL 模型 ✅、QWebSocket 客户端 ✅、QTest 兼容层 ✅、"
               "**QStateMachine ✅(官方未分发该模块,自研纯 Java 实现)**、"
               "**DBus ✅(桥内 QtDBus 能力已暴露为 Java API)**")
if s2 != s:
    s = s2; changed.append("P1 → 5/5")

# ③ P1 表格里的 DBus 行
s2 = s.replace("| **DBus(暴露已有桥能力)** | ⏳ **未做**:桥内已在用 QtDBus(屏保抑制/桌面通知),"
               "待补 `QDBusConnection` / `QDBusMessage` 的 Java API |",
               "| **DBus(暴露已有桥能力)** | ✅ 已完成(v1.9.2):`QDBusConnection` / `QDBusMessage`;"
               "CI Linux 用 `dbus-run-session` 起真实总线做端到端验证 |")
if s2 != s:
    s = s2; changed.append("P1 表格 DBus ✅")

# ④ 硬数据(脚本实测)
pairs = [("| API 类 | **169**", "| API 类 | **171**"),
         ("| 冒烟类 | **33** 个文件；CI 引用 **27** 个", "| 冒烟类 | **34** 个文件；CI 引用 **29** 个"),
         ("| Java 源文件合计 | 202", "| Java 源文件合计 | 205"),
         ("| Java 行数 | **24,688**(API 21,519 + 冒烟 3,169) |", "| Java 行数 | **25,122**(API 21,801 + 冒烟 3,321) |"),
         ("| JNI 桥行数 | **12,851** |", "| JNI 桥行数 | **13,052** |"),
         ("| JNI 导出函数 | **1,417** |", "| JNI 导出函数 | **1,470** |"),
         ("| native 方法声明 | 1,313 |", "| native 方法声明 | 1,340 |"),
         ("| 公开成员声明 | 3,360 |", "| 公开成员声明 | 3,403 |")]
for a, b in pairs:
    if a in s:
        s = s.replace(a, b, 1)
changed.append("硬数据按实测更新")

# ⑤ 命中率(DBus 从 🌉 变 ✅):29 + 5 = 34/39;严格 29/39
s = s.replace("| **宽松**(✅ + 🟡 计入命中) | **33/39 ≈ 85%** | 28 个 ✅ + 5 个 🟡(动画/QML/Multimedia/Positioning/WebSockets) |",
              "| **宽松**(✅ + 🟡 计入命中) | **34/39 ≈ 87%** | 29 个 ✅ + 5 个 🟡(动画/QML/Multimedia/Positioning/WebSockets) |")
s = s.replace("| **严格**(只算 ✅,🟡 不计) | **28/39 ≈ 72%** | 只有 28 个是\"完整可用\"的真实比例 |",
              "| **严格**(只算 ✅,🟡 不计) | **29/39 ≈ 74%** | 只有 29 个是\"完整可用\"的真实比例 |")
s = s.replace("分母 39 = ✅ 28 + 🟡 6(含 Help,明确不计入)+ ❌ 4 + 🌉 1。",
              "分母 39 = ✅ 29 + 🟡 6(含 Help,明确不计入)+ ❌ 4。")
changed.append("命中率 34/39 · 29/39")

# ⑥ 源文件代码块里的数字
s = s.replace("(Get-ChildItem java\\org\\jqt -Filter *.java).Count            # 202 个文件（169 API + 33 冒烟）",
              "(Get-ChildItem java\\org\\jqt -Filter *.java).Count            # 205 个文件（171 API + 34 冒烟）")
s = s.replace("(Get-Content java\\org\\jqt\\*.java | Measure-Object -Line)    # 24,688 行（API 21,519 + 冒烟 3,169）",
              "(Get-Content java\\org\\jqt\\*.java | Measure-Object -Line)    # 25,122 行（API 21,801 + 冒烟 3,321）")
s = s.replace("(Get-Content native\\jqt_bridge.cpp).Count                   # 12,851 行手写 JNI 桥",
              "(Get-Content native\\jqt_bridge.cpp).Count                   # 13,052 行手写 JNI 桥")
s = s.replace("Select-String native\\jqt_bridge.cpp -Pattern 'JNIEXPORT'    # 1,417 个 JNI 导出函数",
              "Select-String native\\jqt_bridge.cpp -Pattern 'JNIEXPORT'    # 1,470 个 JNI 导出函数")
s = s.replace("Select-String java\\org\\jqt\\*.java -Pattern '^\\s+public '    # 3,360 处公开成员声明",
              "Select-String java\\org\\jqt\\*.java -Pattern '^\\s+public '    # 3,403 处公开成员声明")
s = s.replace("Select-String java\\org\\jqt\\*.java -Pattern '\\bnative\\b'     # 1,313 个 native 方法声明",
              "Select-String java\\org\\jqt\\*.java -Pattern '\\bnative\\b'     # 1,340 个 native 方法声明")

io.open(P, "w", encoding="utf-8", newline="\n").write(s)
print("已更新:" + "、".join(changed))
for pat in ("P1 5/5", "DBus | ✅", "34/39", "29/39", "| API 类 | **171**", "13,052"):
    hit = [l for l in s.splitlines() if pat in l]
    print(f"  [{pat}] " + (hit[0].strip()[:110] if hit else "❌ 未找到"))
