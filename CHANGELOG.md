# Changelog

<details>
<summary>🌐 语言 / Language</summary>

- [中文版](#zh) · [English Version](#en)

</details>

---

<a id="zh"></a>
## 中文版

### v0.2.0-alpha（2026-08-26）— Fluent 全家桶：开关 / 动画 / 标题栏 / 触摸 / 皮肤

**控件与外观**：
- 新控件 **JQtSwitch**：Fluent 风格开关（轨道 + 滑块 + 位移动画，轨道颜色随进度渐变，纯自绘）
- 新控件 **JQtPivot**：Fluent 选项卡 + 底部滑动指示器（200ms OutCubic，跟随主题色）
- 标题栏打磨：Windows 用 Segoe MDL2 Assets 原生字形（最小化/最大化/关闭），
  最大化 ↔ 还原图标随状态切换；macOS 交通灯保持；objectName 可皮肤化

**动画系统**：
- **JQtEasing**：40 种缓动函数（映射 QEasingCurve 0~40）
- **JQtAnimation**：任意属性动画（loopCount / onFinished / 弱引用防泄漏）
- **JQtAnimations** Fluent 动效库：按钮 hover 过渡（150ms OutCubic 白色高亮层）、
  卡片入场 / 退场（滑入滑出）
- **JQtAnimationTheme** 动画主题：DEFAULT / FAST / RELAXED / OFF + 自定义，
  `JQtApplication.setAnimationTheme()` 全局统一节奏（-AnimTheme 启动参数）
- 动画重载：animateMove / animateResize / fadeIn / fadeOut 均可指定缓动

**触摸与窗口（HiteVision 一体机实战修复）**：
- 全局 POINTER→鼠标合成（覆盖下拉弹层等所有窗口）——触摸屏按钮/开关/下拉全部可点
- WM_NCHITTEST 不再吞标题栏按钮点击；DPI 感知坐标；标题栏空白区拖动改 Qt 事件冒泡
- 无边框窗口输入框聚焦自动弹触摸键盘（TabTip / ITipInvocation）

**QSS 引擎验证**：
- 完整加载 qfluentwidgets（GPLv3）全部 34 个 QSS 文件 → 类名映射为 JQt 控件类名后
  皮肤直接生效（按钮/输入/下拉/导航/标题栏全命中），演示 JQtQfDemo
- 印证设计：JQt 是纯 QSS 渲染引擎，第三方皮肤导入的合规责任在用户侧

**修复**：QSS + QGraphicsOpacityEffect 组合的 Qt 崩溃（入场动画改纯位移）；
新 JNI 类必须 include javac -h 生成的 extern "C" 头（否则 C++ 修饰名导出 → UnsatisfiedLinkError）

### v0.1.0-alpha（2026-08-25）— 首个 Alpha

**里程碑**：Phase 0-6 全部完成，三平台 CI 构建全绿。

**功能**：
- 控件：JQtWindow / JQtButton / JQtLabel / JQtLineEdit / JQtComboBox / JQtListWidget
- 信号槽（伪信号槽，全部可多监听器注册）：
  点击 / 按下 / 释放 / 勾选切换 / 文本变化 / 回车 / 选项切换 / 列表点击 / 窗口关闭 / 尺寸变化 / 位置变化 / 退出前回调
- 布局：JQtVBoxLayout / JQtHBoxLayout（间距、弹性空间）
- 应用：exec / quit / schedule（Qt 定时器 → Java 回调）/ scheduleQuit
- 样式：QSS 全局样式表（JQtApplication.setStyleSheet）、控件级 QSS（JQtWidget.setStyleSheet）、
  风格切换（setStyle("Fusion") 等）
- 内存管理：句柄注册表（自增 ID）、Qt destroyed 同步注销、所有权模型（Qt vs Java Cleaner）、
  GUI 线程安全回收、悬垂保护（IllegalStateException）
- 异常处理：未创建 QApplication 保护、JNI 异常清理

**构建**：
- Windows（本机）：Qt 6.11.2 + MinGW 13.1，产物 jqt.dll
- CI 三平台：Linux（apt Qt 6.8.3）/ Windows（Qt 6.8.3 + MinGW）/ macOS（Qt 6.8.3 clang_64），
  产物 libjqt.so / jqt.dll / libjqt.dylib，offscreen 冒烟测试全过

**许可**：JQt Source License v1.0（JSL-1.0），分层授权（详见 LICENSE.md）

**已知限制**：
- 控件集较小（Phase 4 完成度），菜单/树/滚动区等按 docs/api-tiering.md 第二批发货
- CI 使用 Qt 6.8.3 LTS（Qt 官方 Windows 6.11.2 在线仓库元数据 404）

---

<a id="en"></a>
## English Version

### v0.2.0-alpha (2026-08-26) — Fluent Kit: Switch / Animation / Titlebar / Touch / Skins

**Widgets & look**:
- New **JQtSwitch**: Fluent-style switch (track + thumb + slide animation, track color eases with progress, custom painted)
- New **JQtPivot**: Fluent tabs with sliding bottom indicator (200ms OutCubic, theme-colored)
- Titlebar polish: native Segoe MDL2 Assets glyphs on Windows (min/max/close),
  maximize <-> restore icon swaps with state; macOS traffic lights kept; objectName for skinning

**Animation system**:
- **JQtEasing**: 40 easing functions (mapping QEasingCurve 0~40)
- **JQtAnimation**: arbitrary property animation (loopCount / onFinished / weak-ref leak guard)
- **JQtAnimations** Fluent motion library: button hover transition (150ms OutCubic white overlay),
  card entrance / exit (slide in / out)
- **JQtAnimationTheme**: DEFAULT / FAST / RELAXED / OFF + custom,
  `JQtApplication.setAnimationTheme()` global pacing (-AnimTheme launcher param)
- Easing overloads on animateMove / animateResize / fadeIn / fadeOut

**Touch & windows (battle-tested on HiteVision all-in-one)**:
- Global POINTER->mouse synthesis covering ALL windows incl. combo popups - buttons/switches/dropdowns all clickable on touch panels
- WM_NCHITTEST no longer swallows titlebar buttons; DPI-aware hit-test coords; titlebar drag via Qt event bubbling
- Frameless windows auto-show touch keyboard (TabTip / ITipInvocation) on input focus

**QSS engine validation**:
- Loaded all 34 qfluentwidgets (GPLv3) QSS files, class-mapped to JQt widget classes -
  skin works directly (buttons/inputs/dropdowns/nav/titlebar all hit); JQtQfDemo
- Proves JQt is a pure QSS rendering engine; skin compliance is the user's responsibility

**Fixes**: Qt crash on QSS-styled widget + QGraphicsOpacityEffect (entrance now pure-move);
new JNI classes must include javac -h generated extern "C" headers (mangled exports -> UnsatisfiedLinkError)

### v0.1.0-alpha (2026-08-25) — First Alpha

**Milestone**: Phases 0-6 complete; three-platform CI builds green.

**Features**:
- Widgets: JQtWindow / JQtButton / JQtLabel / JQtLineEdit / JQtComboBox / JQtListWidget
- Signals (pseudo signal-slot, multi-listener):
  click / press / release / toggle / text changed / return pressed / index changed / item clicked /
  window close / resized / moved / about to quit
- Layouts: JQtVBoxLayout / JQtHBoxLayout (spacing, stretch)
- App: exec / quit / schedule (Qt timer to Java callback) / scheduleQuit
- Memory: handle registry (incrementing IDs), Qt destroyed sync, ownership model (Qt vs Java Cleaner),
  GUI-thread-safe disposal, dangling guard (IllegalStateException)
- Errors: missing-QApplication guard, JNI exception hygiene

**Builds**:
- Windows (local): Qt 6.11.2 + MinGW 13.1, artifact jqt.dll
- CI matrix: Linux (apt Qt 6.8.3) / Windows (Qt 6.8.3 + MinGW) / macOS (Qt 6.8.3 clang_64),
  artifacts libjqt.so / jqt.dll / libjqt.dylib; offscreen smoke tests pass

**License**: JQt Source License v1.0 (JSL-1.0), tiered (see LICENSE.md)

**Known limits**:
- Widget set is small (Phase 4 scope); menus/tree/scroll area ship in the second batch per docs/api-tiering.md
- CI uses Qt 6.8.3 LTS (Qt's official Windows 6.11.2 online repo metadata is 404)
