#!/usr/bin/env python3
"""按对账结果修正 documents/compare-qtjambi.md(改的是"假账",不是代码)：
  ① 动画行:声称 ✅ 且点名 QPropertyAnimation —— 该类不存在 -> 改 🟡 并修正文案
  ② 命中率:把 4 个 🟡 项计入命中是宽松口径 -> 同时给出严格口径
  ③ QStateMachine:"需安装器勾选"不成立 -> 改为多路核实后的准确表述
  ④ P1"完成" -> 3/4
  ⑤ Java 源文件数 193 -> 194
  ⑥ 新增"CI 覆盖真相":哪些平台哪些冒烟其实在 SKIP
"""
import io
import re

P = r"D:\SilentStudio\JQt - Dev\docs\compare-qtjambi.md"
s = io.open(P, encoding="utf-8").read()
orig = s
changed = []

# ① 动画行
old = "| 动画 | ✅ | `QPropertyAnimation` + JQt 动画门面(`JQtAnimation(s)` `JQtEasing` `JQtAnimationTheme`) |"
new = ("| 动画 | 🟡 | **JQt 动画门面**可用(`JQtAnimation(s)` `JQtEasing` `JQtAnimationTheme`);"
       "**Qt 的 `QPropertyAnimation` 没有暴露** —— 属性动画只能用门面或自己用 `QTimer` 拼(此前的 ✅ 是错的,已改正) |")
if old in s:
    s = s.replace(old, new, 1)
    changed.append("动画行 ✅→🟡")

# ③ QStateMachine 行
m = re.search(r"\| \*\*QStateMachine 状态机\*\* \| ⛔[^|]*\|[^|]*\|", s)
if m:
    s = s.replace(m.group(0),
        "| **QStateMachine 状态机** | ⛔ 官方未分发 | **Qt 6.12 文档里该模块存在**,但**官方任何我们能拿到的发行物里都没有它**:"
        "mingw 6.12(base+addons)✗ · MSVC 6.12 addons ✗ · Linux 6.12 base ✗ · "
        "macOS 官方 Qt 6.11.2(aqt 列出的 68 个模块含 webengine/webview,无 statemachine)✗ · "
        "Windows 官方 Qt 6.11.1 ✗ · `qtbase/src` 与 `qtdeclarative/src`(6.12 分支)✗ · 无 `qt/qtstatemachine` 仓库 ✗。"
        "**结论已修正**:不是\"需要安装器勾选\",而是官方不分发 —— 除非找到并自行编译源码,否则做不了 |", 1)
    changed.append("QStateMachine 定性修正")

# ④ P1 完成 -> 3/4
s2 = s.replace("**P1 完成**:SQL 模型 ✅、QWebSocket 客户端 ✅、QTest 兼容层 ✅",
               "**P1 3/4**:SQL 模型 ✅、QWebSocket 客户端 ✅、QTest 兼容层 ✅")
if s2 != s:
    s = s2
    changed.append("P1 完成→3/4")

# ⑤ 源文件数
s2 = s.replace("| Java 源文件 | 193(", "| Java 源文件 | 194(")
if s2 != s:
    s = s2
    changed.append("源文件数 193→194")

# ② 命中率双口径 + ⑥ CI 覆盖真相(替换原来那段汇总)
old_sum = re.search(r"\*\*汇总\(v1\.9\.1 进展\)\*\*:.*?命中率 \*\*28/36 ≈ 78%\*\*。", s, re.S)
if old_sum:
    new_sum = """**汇总(v1.9.1 进展)**:36 个能力点 —— 命中率**必须给两个口径**(此前只给宽松口径,属虚高):

| 口径 | 命中 | 说明 |
|---|---|---|
| **宽松**(✅ + 🟡 都算命中) | **28/36 ≈ 78%** | 🟡 项 = 只做了子集 |
| **严格**(只算 ✅,🟡 不计) | **24/36 ≈ 67%** | 这才是"完整可用"的真实比例 |

被计入宽松口径的 🟡 项共 **4 个**:`QML/Quick`(只有 QQuickView)、`Multimedia`(无视频/录制)、
`Positioning`(只有纯几何坐标)、`WebSockets`(只有客户端);`Help` 已明确**不计入**任一命中。

另:**7 项仍缺失**、`QStateMachine` 官方未分发、`DBus` 属"桥内有、API 无"。"""
    s = s[:old_sum.start()] + new_sum + s[old_sum.end():]
    changed.append("命中率双口径")

# ⑥ CI 覆盖真相(插在汇总之后)
if "### 2.1 CI 覆盖真相" not in s:
    anchor = "另:**7 项仍缺失**、`QStateMachine` 官方未分发、`DBus` 属\"桥内有、API 无\"。"
    add = anchor + """

### 2.1 CI 覆盖真相(哪些其实在 SKIP)

"CI 五作业全绿"不等于"每个模块在每个平台都验过"。实测最近一次成功 run
(`37216151859`)的日志,下列冒烟是**跳过**的 —— 所以这些能力点**在对应平台上从未真正跑过**:

| 平台 | 跳过的冒烟 | 原因 |
|---|---|---|
| Linux x64 | `SmokeSvg` | QtSvg 未编译进 libjqt(该平台探测未命中) |
| Linux x64 | `SmokeSqlModel` | 平台无 SQLite 驱动插件 |
| macOS x64 | `SmokeSvg` | 同上 |
| macOS x64 | `SmokeSqlModel` | 同上 |
| macOS x64 | `SmokeQml` | 6.8.3 线缺 qtdeclarative |
| macOS x64 / Windows ARM64 | `QOpenGLWidget` 相关 | 这两平台的 Qt 构建不含 OpenGLWidgets 模块 |

**因此**:`QSvg` 与 `SQL 模型` 目前**只有 Windows 线真跑过**;`QML` 在 macOS 只覆盖 6.12 线;
`QOpenGLWidget` 在 macOS 与 Windows ARM64 不可用。要补齐需:Linux/macOS 侧把 qtsvg 编进库、
部署 qsqlite 插件、给 6.8.3 线补 qtdeclarative。"""
    s = s.replace(anchor, add, 1)
    changed.append("新增 CI 覆盖真相小节")

io.open(P, "w", encoding="utf-8", newline="\n").write(s)
print("已修改:" + ("、".join(changed) if changed else "(无)"))
print("文件:", len(orig), "->", len(s), "字节")
