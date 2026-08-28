# Changelog

<details>
<summary>🌐 语言 / Language</summary>

- [中文版](#zh) · [English Version](#en)

</details>

---

<a id="zh"></a>
## 中文版

### v0.7.0-Universal-Kit（2026-08-29）— 跨平台独家能力包

**Exclusive Kit 从 Windows 扩展到 macOS / Linux（不区别对待，同一 API 三平台同语义）**：

- **防休眠/防息屏**（QApplication.preventSleep）— Windows SetThreadExecutionState / macOS NSProcessInfo / Linux D-Bus Inhibit（ScreenSaver 优先，gnome-SessionManager 回退）
- **开机自启**（QApplication.setAutoStart）— 三平台统一：Windows Run 注册表（已有）/ macOS LaunchAgent plist / Linux XDG autostart .desktop
- **桌面通知**（QApplication.showNotification）— Linux D-Bus Notifications / Windows 托盘气泡 / macOS NSUserNotification

**macOS 独家（对齐 Windows 任务栏进度 / DWM 能力）**：

- **Dock 角标**（setDockBadge / clearDockBadge，NSDockTile）
- **透明标题栏**（setMacTitlebarTransparent，保留红黄绿按钮 + 内容上延）
- **全尺寸内容视图**（setMacFullSizeContentView，沉浸式布局）

**构建**：build-linux.sh 增加 QtDBus 链接；build-macos.sh 链接 AppKit/Foundation/CoreFoundation；CI 三平台冒烟新增 SmokeExclusive（新 API 全量调用验证）。

**已知限制**：Linux 全局热键（X11/Wayland）依赖与限制较多，列为 v0.7.x 候选。

版本命名：x.x.x-Universal-Kit（跨平台统一独家能力包标识）。

### v0.6.1-Exclusive-Kit（2026-08-28）— Windows 独家能力包

**Qt 官方未封装的 Windows 能力，JQt 首次 API 化**：

- **DWM 原生边框颜色**（setNativeBorderColor，Win11 22H2+）
- **原生标题栏颜色**（setNativeCaptionColor）+ **标题栏文字颜色**（setNativeCaptionTextColor）
- **深色标题栏**（setNativeDarkTitleBar，Win10 1809+ 支持）
- **Mica 背景**（setMicaBackground，Win11 22H2+ 原生材质）
- **任务栏进度**（setTaskbarProgress/clearTaskbarProgress，Win10+ ITaskbarList3）
- **全局热键**（GlobalHotkey 类：register("Ctrl+Shift+X", handler)，应用失焦也生效）
- **开机自启**（QApplication.setAutoStart，HKCU Run 注册表）

版本命名：x.x.x-Exclusive-Kit（独家能力包标识）。旧系统调用静默忽略（不报错不崩溃）。

### v0.6.0-TEST（2026-08-27）— L1 常用 API 补全 + 社区反馈修复

**L1 补全（114514.md 对齐，135 → 149/205 直接对应；可实现口径 ~99%）**：
- QWidget 基类：close/move/resize/update/repaint/size/geometry/pos/contentsMargins/styleSheet/toolTip/windowTitle/windowState/focusPolicy/acceptDrops/cursor/font/palette/graphicsEffect + windowTitleChanged/customContextMenuRequested 信号
- 控件类：QLineEdit 编辑全套+4 信号 / QComboBox count-editable+4 信号 / QLabel alignment-wordWrap-margin-indent-buddy+2 信号 / QListWidget count-item-currentText+6 信号 / QTreeWidget currentItem+6 信号 / QCheckBox 三态+checkStateChanged / QSlider 自绘刻度 / QSpinBox prefix-suffix-step-minmax-cleanText / QTextEdit 编辑七件套+find+2 信号 / QTabWidget clear-count / QMenu exec-title / QToolBar iconSize+2 信号 / QProgressBar text-alignment / QScrollArea alignment / QSplitter count / QPushButton click-toggle-isChecked-menu / QMessageBox showAbout
- 工具类：QClipboard / QSettings / QFile / QDir + 值类 QPoint / QSize / QRect
- 应用级：beep/alert/styleSheet/font/setFont/rhiBackend

**社区反馈修复**：
- 用户显式 hide() 的控件不再被窗口 show 强制显示
- QLayout.addSpacing / QWidget.pos() / palette() 简化查询
- README 旧 API 示例全部 Q 化；发布 zip 附带 qt-mapping/api-implemented 文档
- 新增 docs/behavior.md 行为契约（显示规则/QStackedLayout 用法/主题双轨/动画清理/DPI/迁移速查）

**Community**：QraftLab 入库（QSS 美术指南/分离样式/四区实验室）；JQT_PITFALLS.md 移除

**规模**：API ~470 方法 / 47 类；双 Qt（6.8.3+6.11.2）× x64/ARM64 CI 全绿

### v0.5.1-TEST（2026-08-27）— 启动体验修复 + 主题降级健壮性

**修复**：
- **启动窗口闪现**（污点）：布局未安装时 addWidget 不再 show（防顶层窗口闪现）；子控件由父窗口 setLayout 递归显示（QStackedLayout 页跳过，防多页堆叠）
- **窗口尺寸被布局 sizeHint 撑大**（1280x720 16:9 被破坏）：面板级 setLayout 不再触发子控件显示，统一由父窗口递归
- **QApplication.setTheme 主题模板缺失时崩溃**：降级为警告 + 默认配色（Qt 一致行为）
- **jpackage 分发修复**：runtime/bin/java.exe 必需（launcher 依赖）；themes/ 目录需随包分发

**验证**：启动窗口峰值 10-15 → 1；JQtGallery 16:9 保持；无 themes 目录不崩溃；三 demo 回归全绿。

### v0.5.0-TEST（开发中）— 控件海啸 + 窗口体系 + 渲染适配 + 自绘画布/多线程

**批 1 控件海啸**：
- 对话框家族：QInputDialog / QFileDialog / QColorDialog / QFontDialog；QMessageBox 增强（showWarning / showCritical / showOkCancel）
- 数据表格/树：QTableWidget（单元格/表头/行列/点击信号）、QTreeWidget（itemId 节点体系/父子/展开折叠）
- 选项卡/分组/分割：QTabWidget / QGroupBox / QStackedLayout（addPage）/ QSplitter（方向/尺寸/手柄）
- 输入控件：QSpinBox / QDial / QRadioButton / QDateTimeEdit
- 布局升级：QGridLayout（行列/跨行跨列/拉伸）/ QFormLayout（标签行）

**批 2 窗口体系**：
- 菜单/工具栏/状态栏/托盘：QMenu / QToolBar / QStatusBar / QSystemTrayIcon
- 富文本编辑：QTextEdit（纯文本/追加/只读/textChanged）

**批 3 渲染适配**：
- QApplication.rhiBackend() + --rhi 参数（d3d11 默认 / software 兜底）
- 实测结论：QSG_RHI_BACKEND 仅影响 Qt Quick；QWidget 在 Windows 固定 D3D11（opengl/vulkan 请求回退 d3d11，已在 API 文档注明）

**批 4 能力**：
- 自绘画布：QCanvasWidget + QPainter（线/矩形/圆/圆角/文本/字体/平移/旋转，12 个 2D API）
- 多线程：QApplication.runOnUiThread()（后台线程安全回 UI 线程）

**本次修复**：QTimer::singleShot 缺少 context 导致跨线程 schedule 永不执行（已修复）。

### v0.4.1-alpha（2026-08-27）— Qt API 对齐（类名 Q 化 + 信号对齐 + 链式）

**命名策略（SilentStudio 理念：好好对待程序员）**：
- **有 Qt 对应物的类改用 Q 前缀**：QPushButton / QLineEdit / QComboBox / QListWidget /
  QCheckBox / QMainWindow / QWidget / QApplication / QSlider / QProgressBar / QScrollArea /
  QMessageBox / QFrame / QVBoxLayout / QHBoxLayout / QLayout —— **Qt 文档直接可查，零认知成本**
- **JQt 原创控件保留 JQt 前缀**：JQtSwitch / JQtPivot / JQtNavigation / JQtTitleBar /
  JQtInfoBar / JQtEasing / JQtAnimation 系列
- **信号对齐 Qt**：onClick → **onClicked**（其余本就对齐：onPressed/onToggled/onTextChanged...）
- **链式 API**：所有 onXxx 返回 this：`btn.onClicked(...).setText("OK").setFixedSize(100,40)`
- 映射表：[docs/qt-mapping.md](docs/qt-mapping.md)（类名 + 信号 + 翻译规则）

> 破坏性变更：旧类名（JQtButton 等）不再存在，迁移见映射表（Java→C++ 翻译一条规则）。
### v0.4.0-alpha（2026-08-27）— 新控件六件套 / ARM64

**新控件（v0.4 六件套）**：
- **JQtSlider**：Fluent 滑块（自绘轨道+圆钮，accent 填充，点击跳转 120ms 动画，拖动跟手，明暗感知）
- **JQtScrollArea**：滚动区（内容可滚动，滚动条 QSS 样式化）
- **JQtProgressBar**：进度条（QSS chunk 圆角 + accent）
- **JQtNavigation**：Fluent 侧栏导航（图标+文字，选中高亮 200ms 滑动，主题色淡色背景）
- **JQtMessageBox**：模态对话框（询问/信息，QSS 可样式化）
- **JQtInfoBar**：顶部通知条（滑入→停留→滑出，自动清理）

**平台**：
- **Windows ARM64 恢复**（windows-11-arm 原生 runner + aqt win64_arm64 + MSVC 构建）

**皮肤**：
- **qf light 皮肤**：qfluentwidgets light QSS（34 文件）合并映射完成（themes/qf/qf-light-jqt.qss）

**demo**：卡片 4 集成全部新控件（滑块联动进度条/通知条/询问框）
### v0.3.0-alpha（2026-08-26）— 触摸实战 / 主题系统 / 基础 API 补齐

**触摸与窗口（HiteVision 一体机实战打磨）**：
- 全局 POINTER→鼠标合成（含下拉弹层等所有窗口）——触摸屏按钮/开关/下拉/弹层全可点
- 标题栏触摸拖动：系统原生路径（HTCAPTION）+ 消息层兜底，复刻系统手感
- 无边框窗口最大化：手动几何管理（工作区约束，不盖任务栏）
- 无边框窗口输入框聚焦自动弹触摸键盘（TabTip / ITipInvocation）
- WM_NCHITTEST DPI 感知 + 不再吞标题栏按钮；WM_GETMINMAXINFO 工作区约束

**主题系统**：
- **setAccentColor(#RRGGBB)**：一键换主题色（QSS 重渲染 + 调色板 Highlight + 自绘开关同步）
- **setAutoTheme(true)**：自动跟随 Windows 系统深浅色 + 系统强调色（注册表轮询）
- **setFontFamily**：全局字体（构造时自动应用中文字体，根治 CJK 问号/乱码）
- 主题一致性：demo 硬编码颜色全部清除，自绘控件（开关轨道）明暗感知
- 黑白整体切换 demo（主题色跨主题保留）

**动画系统**：
- JQtAnimations（hover 过渡 / 卡片入场退场）、JQtPivot（滑动指示器）、
  JQtAnimationTheme（DEFAULT/FAST/RELAXED/OFF）、setHoverIntensity

**样式能力**：
- setDropShadow（QSS box-shadow 替代）、setBorderRadius、布局 setContentsMargins
- qfluentwidgets（GPLv3）34 个 QSS 文件皮肤直通验证（JQtQfDemo）

**基础 API 补齐**：
- JQtWidget：width/height/x/y、show/hide/isVisible、setEnabled/isEnabled、setFixedSize
- JQtLabel.text()
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

### v0.6.0-TEST (2026-08-27) — L1 API completion + community feedback fixes

**L1 completion (114514.md aligned, ~99% of feasible)**: QWidget base 20+ / widget classes full edit+signals / QCheckBox tristate / QSlider custom ticks / QSpinBox complete / QTextEdit editing / dialogs / QMenu exec / tools QClipboard QSettings QFile QDir / value classes QPoint QSize QRect / app-level beep alert font palette

**Feedback fixes**: user hide() respected; QLayout.addSpacing / QWidget.pos() / palette(); README Q-prefixed; zip ships qt-mapping + api-implemented; docs/behavior.md behavior contract

**Community**: QraftLab added (QSS art guide / separated styles / lab demo) - credit: QraftLab

**Scale**: ~470 methods / 47 classes; dual Qt CI green

### v0.5.1-TEST (2026-08-27) — Startup polish + theme degrade robustness

**Fixes**:
- Startup window flash: no show() before layout attached (no top-level flashes); children shown recursively by parent setLayout (QStackedLayout pages skipped)
- Window inflated by layout sizeHint (16:9 broken): panel-level setLayout no longer shows children
- QApplication.setTheme crashes on missing template: degrade to default palette (Qt-consistent)
- jpackage distribution: runtime/bin/java.exe required; themes/ must ship

**Verified**: window peak 10-15 → 1; 16:9 kept; no crash without themes; demos green.

### v0.5.0-TEST (in development) — Widget wave + Window family + Rendering + Canvas/Multithreading

**Batch 1 — widget wave**: QInputDialog/QFileDialog/QColorDialog/QFontDialog (+QMessageBox showWarning/showCritical/showOkCancel) · QTableWidget/QTreeWidget · QTabWidget/QGroupBox/QStackedLayout/QSplitter · QSpinBox/QDial/QRadioButton/QDateTimeEdit · QGridLayout/QFormLayout
**Batch 2 — window family**: QMenu/QToolBar/QStatusBar/QSystemTrayIcon · QTextEdit
**Batch 3 — rendering**: QApplication.rhiBackend() + --rhi (d3d11 default / software fallback); verified: QSG_RHI_BACKEND only affects Qt Quick — QWidget is fixed D3D11 on Windows
**Batch 4 — capability**: QCanvasWidget + QPainter (12 2D APIs) · QApplication.runOnUiThread()

**Fixes**: QTimer::singleShot without context never fired across threads (fixed).

### v0.4.0-alpha (2026-08-27) — Six new widgets / ARM64

**New widgets**: JQtSlider (Fluent painted, drag + animated), JQtScrollArea,
JQtProgressBar (QSS chunk), JQtNavigation (sidebar sliding highlight),
JQtMessageBox (modal), JQtInfoBar (toast auto-dismiss)

**Platform**: Windows ARM64 restored (native arm64 runner + aqt win64_arm64 + MSVC)

**Skin**: qf light QSS (34 files) merged and mapped (themes/qf/qf-light-jqt.qss)

**Demo**: card 4 integrates all new controls (slider->progress, toast, question box)
### v0.3.0-alpha (2026-08-26) — Touch-tested / Theme system / Base API completion

**Touch & windows (battle-tested on HiteVision all-in-one)**:
- Global POINTER->mouse synthesis covering ALL windows incl. popups - buttons/switches/dropdowns clickable on touch panels
- Titlebar touch drag: system native path (HTCAPTION) with message-layer fallback
- Frameless maximize: manual geometry management (work-area constrained, no taskbar overlap)
- Frameless inputs auto-show touch keyboard (TabTip / ITipInvocation)
- DPI-aware WM_NCHITTEST; buttons no longer swallowed; WM_GETMINMAXINFO work-area clamp

**Theme system**:
- **setAccentColor(#RRGGBB)**: switch accent (QSS re-render + palette Highlight + custom switch)
- **setAutoTheme(true)**: follow Windows light/dark + accent (registry polling)
- **setFontFamily**: global font (auto CJK font at construction - fixes CJK question marks)
- Theme consistency: demo hardcoded colors removed, custom widgets lightness-aware
- Dark/light toggle demo (accent preserved across themes)

**Animation**: JQtAnimations (hover/card entrance-exit), JQtPivot (sliding indicator),
JQtAnimationTheme (DEFAULT/FAST/RELAXED/OFF), setHoverIntensity

**Styling**: setDropShadow (QSS box-shadow substitute), setBorderRadius,
layout setContentsMargins; qfluentwidgets (GPLv3) 34-file QSS skin verification (JQtQfDemo)

**Base API**: JQtWidget width/height/x/y, show/hide/isVisible, setEnabled/isEnabled,
setFixedSize; JQtLabel.text()
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
