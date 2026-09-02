# JQt 已实现 API 清单（v0.7.5-Generator-Kit）

<details>
<summary>🌐 语言 / Language</summary>

- [中文版](#zh) · [English Version](#en)

</details>

---

<a id="zh"></a>
## 中文版

> 本清单从源码自动提取，与 v0.7.5-Generator-Kit 发布包对应（最新 API 以源码为准）。
> 所有控件继承 `QWidget`，共享其基础方法（见 1.0）。

### 1.0 基类 QWidget（所有控件共享）

| 方法 | 说明 |
|------|------|
| `boolean isCreated()` | 控件是否已创建 |
| `void dispose()` | 手动释放 C++ 对象（一般无需调用，GC 自动回收） |
| `boolean isDisposed()` | 是否已释放 |
| `long nativeHandle()` | C++ 句柄（高级用途） |
| `void animateMove(x,y,ms)` | 平滑移动（OutCubic 缓动） |
| `void animateMove(x,y,ms,JQtEasing)` | 平滑移动，指定缓动函数 |
| `void animateResize(w,h,ms)` | 平滑缩放（OutCubic 缓动） |
| `void animateResize(w,h,ms,JQtEasing)` | 平滑缩放，指定缓动函数 |
| `void setDropShadow(blur,alpha[,dx,dy])` | 投影阴影（QSS box-shadow 的替代：Qt QSS 不支持 box-shadow） |
| `void clearDropShadow()` | 移除投影阴影 |
| `void setBorderRadius(int)` | 自定义圆角（像素；与控件级 setStyleSheet 内部合并，互不覆盖） |
| `int width()` / `height()` / `x()` / `y()` | 几何查询（像素） |
| `void show()` / `hide()` / `isVisible()` | 显隐控制 |
| `void setEnabled(boolean)` / `isEnabled()` | 启用/禁用 |
| `void setFixedSize(w,h)` | 固定尺寸 |
| `void setStyleSheet(String qss)` | 控件级 QSS（与全局样式可叠加，控件级优先） |

### 1.1 QApplication —— 应用入口

| 方法 | 说明 |
|------|------|
| `QApplication()` | 创建应用（整个进程仅一个，**必须先创建**） |
| `void exec()` | 进入事件循环（阻塞，最后一个窗口关闭后返回） |
| `void quit()` | 退出事件循环 |
| `void scheduleQuit(long ms)` | 延迟 ms 毫秒后自动退出 |
| `void schedule(Runnable task, long delayMs)` | 延迟在 GUI 线程执行任务（线程安全） |
| `void setStyleSheet(String qss)` | 设置全局样式表（QSS，Qt Style Sheets） |
| `void setStyle(String style)` | 切换风格（如 `"Fusion"` 经典 Qt 扁平风） |
| `void setLightMode(boolean)` | 切换浅色/默认配色（Java 进程中 Qt 可能误判系统暗色，用此 API 显式控制；也可用 `-Djqt.lightMode=true` 启动时自动开启） |
| `void setLightMode(boolean)` | 切换浅色/默认配色（Java 进程中 Qt 可能误判系统暗色，用此 API 显式控制；也可用 `-Djqt.lightMode=true` 启动时自动开启） |
| `void setTheme(String)` | 应用主题（fluent-dark / fluent-light，QSS + 调色板一致打包） |
| `void setTheme(path, vars[,light])` | QSS 模板 + 变量集渲染主题（%var% 占位符） |
| `void setAccentColor(String hex)` | 切换主题色（#RRGGBB；模板主题重渲染 + 调色板 Highlight + 自绘开关同步） |
| `void setFontFamily(String[,size])` | 全局字体（构造时自动应用系统中文字体） |
| `void onAboutToQuit(Runnable)` | 退出前回调（Qt aboutToQuit 信号） |

### 1.2 QMainWindow —— 窗口

| 方法 | 说明 |
|------|------|
| `QMainWindow(String title)` | 创建 800x600 窗口 |
| `QMainWindow(String title, int w, int h)` | 创建指定大小窗口 |
| `void show()` / `void hide()` / `void close()` | 显示 / 隐藏 / 关闭（触发 onClose，最后窗口关闭时 exec 返回） |
| `void resize(int w, int h)` | 修改窗口大小（触发 onResized） |
| `void setTitle(String)` | 修改标题 |
| `void addWidget(QWidget child)` | 添加子控件（无布局时自动摆放） |
| `void setLayout(QLayout layout)` | 设置布局管理器 |
| `void onClose(Runnable)` | 窗口关闭回调 |
| `void onResized(BiConsumer<Integer,Integer>)` | 尺寸变化回调（参数：宽,高） |
| `void onMoved(BiConsumer<Integer,Integer>)` | 位置变化回调（参数：x,y） |
| `void setFrameless(boolean)` | 无边框模式（DWM 阴影 + 边框缩放热区 + 顶部拖拽，Fluent 风格） |
| `void setAcrylic(boolean)` | 亚克力背景（Win10+ 模糊半透明） |
| `void setRoundedCorners(boolean)` | Windows 11 圆角 |
| `void setDraggable(boolean)` / `setBorderWidth(int)` | 拖拽开关 / 缩放热区宽度 |
| `void minimize()` / `maximize()` / `toggleMaximize()` / `isMaximized()` | 窗口状态 |

### 1.3 QPushButton —— 按钮

| 方法 | 说明 |
|------|------|
| `QPushButton(String text)` | 创建按钮 |
| `void setText(String)` | 修改文字 |
| `void setCheckable(boolean)` / `void setChecked(boolean)` | 勾选模式 |
| `void onClicked(Runnable)` | 点击回调（clicked 信号） |
| `void onPressed(Runnable)` | 按下回调（pressed 信号） |
| `void onReleased(Runnable)` | 释放回调（released 信号） |
| `void onToggled(Consumer<Boolean>)` | 勾选切换回调（toggled 信号，参数为新状态） |

### 1.4 QLabel —— 文本标签

| 方法 | 说明 |
|------|------|
| `QLabel(String text)` | 创建标签 |
| `void setText(String)` | 修改文本 |

### 1.5 QLineEdit —— 单行输入框

| 方法 | 说明 |
|------|------|
| `QLineEdit(String text)` | 创建输入框 |
| `String text()` | 当前文本 |
| `void setText(String)` | 设置文本（触发 onTextChanged） |
| `void setPlaceholderText(String)` | 占位提示 |
| `void onTextChanged(Consumer<String>)` | 文本变化回调 |
| `void onReturnPressed(Runnable)` | 回车确认回调 |

### 1.6 QComboBox —— 下拉框

| 方法 | 说明 |
|------|------|
| `QComboBox()` | 创建下拉框 |
| `void addItem(String)` | 追加选项 |
| `int currentIndex()` / `String currentText()` | 当前选项 |
| `void setCurrentIndex(int)` | 选中指定项（触发 onCurrentIndexChanged） |
| `void onCurrentIndexChanged(Consumer<Integer>)` | 选项切换回调（参数：新索引） |

### 1.7 QListWidget —— 列表

| 方法 | 说明 |
|------|------|
| `QListWidget()` | 创建列表 |
| `void addItem(String)` | 追加列表项 |
| `int currentRow()` | 当前行号 |
| `void onItemClicked(Consumer<Integer>)` | 点击回调（参数：行号） |
| `void onCurrentRowChanged(Consumer<Integer>)` | 当前行切换回调 |

### 1.8 QFrame —— 卡片/容器（Fluent 卡片基座）

| 方法 | 说明 |
|------|------|
| `QFrame()` | 创建面板（QFrame，QSS 可样式化：圆角/边框/背景） |
| `addWidget(widget)` | 直接添加子控件 |
| `setLayout(layout)`（继承） | 内部布局 |

### 1.9 JQtSwitch —— Fluent 开关（自绘 + 滑块动画）【新增 v0.2.0】

| 方法 | 说明 |
|------|------|
| `JQtSwitch()` / `JQtSwitch(boolean checked)` | 创建开关（默认关 / 指定初始状态） |
| `isChecked()` / `setChecked(boolean)` | 状态查询 / 设置（滑块位移动画，轨道颜色随进度渐变） |
| `onToggled(Consumer<Boolean>)` | 状态切换回调（参数：新状态） |

### 1.10 JQtEasing —— 缓动函数（40 种）【新增 v0.2.0】

| 常量 | 说明 |
|------|------|
| `LINEAR` | 线性 |
| `IN_QUAD` ~ `OUT_IN_QUINT` | 二次 ~ 五次曲线 |
| `IN_SINE` / `OUT_SINE` / `IN_OUT_SINE` | 正弦 |
| `IN_EXPO` ~ `OUT_IN_EXPO` | 指数 |
| `IN_CIRC` ~ `OUT_IN_CIRC` | 圆形 |
| `IN_ELASTIC` ~ `OUT_IN_ELASTIC` | 弹性 |
| `IN_BACK` ~ `OUT_IN_BACK` | 回退 |
| `IN_BOUNCE` / `OUT_BOUNCE` / `IN_OUT_BOUNCE` / `OUT_IN_BOUNCE` | 弹跳 |

映射 Qt `QEasingCurve::Type` 0~40，可作为所有动画方法的可选参数。

### 1.11 JQtAnimation —— 高级属性动画【新增 v0.2.0】

| 方法 | 说明 |
|------|------|
| `JQtAnimation(widget, property, from, to, ms, easing)` | 创建动画（如 `windowOpacity` 1.0→0.2） |
| `setLoopCount(int)` | 循环次数（-1 = 无限循环） |
| `start()` / `stop()` | 启动 / 停止 |
| `onFinished(Consumer<JQtAnimation>)` | 完成回调（finished 信号，GUI 线程，一次性） |

> 实现说明：C++ 侧 QPropertyAnimation + DeleteWhenStopped 自清理；
> Java 侧用 **弱引用** 注册回调，动画完成后自动注销，不泄漏、不钉住 GC。

### 1.12 JQtAnimations —— Fluent 动效库【新增 v0.2.0】

| 方法 | 说明 |
|------|------|
| `entrance(QWidget)` / `(w, ms, easing)` | 控件入场：下方 24px 滑入（时长经动画主题缩放） |
| `exit(QWidget)` / `(w, ms, easing)` | 控件退场：下移，动画结束后隐藏 |
| `setHoverEnabled(boolean)` | 全局按钮悬停动画开关（跟随动画主题） |

**hover**：所有 QPushButton 默认带 150ms OutCubic 悬停高亮过渡（白色 8.5% 叠加层，
clean-room 独立实现；Fluent 公开动效规范参数）。触摸屏无 hover 不受影响。

> 实现说明：入场/退场用纯位移动画（QPropertyAnimation pos）。
> 踩坑记录：QSS 样式化控件 + QGraphicsOpacityEffect 组合会触发 Qt 空指针崩溃，故不使用透明度特效。

### 1.13 JQtPivot —— Fluent 选项卡（滑动指示器）【新增 v0.2.0】

| 方法 | 说明 |
|------|------|
| `JQtPivot()` | 创建选项卡组（高 36px，纯自绘） |
| `addItem(String)` | 追加选项卡 |
| `currentIndex()` / `setCurrentIndex(int)` | 当前选中项（指示器 200ms OutCubic 滑动） |
| `onChanged(Consumer<Integer>)` | 选中项变化回调（参数：新索引） |

> 文本色跟随 QPalette，指示器用 Highlight 色（可被主题定制）。

### 1.14 JQtAnimationTheme —— 动画主题【新增 v0.2.0】

| 常量 / 方法 | 说明 |
|------|------|
| `DEFAULT`（1.0x, OutCubic） | 标准节奏 |
| `FAST`（0.65x） | 轻快（触摸屏/演示） |
| `RELAXED`（1.6x, OutQuint） | 舒缓（桌面沉浸） |
| `OFF`（0x） | 禁用全部动效（无障碍/低配） |
| `new JQtAnimationTheme(speed, easing)` | 自定义（speed=时长倍率，0=禁用） |
| `QApplication.setAnimationTheme(theme)` | 全局应用（所有动效统一跟随） |

> 启动参数 `-Djqt.animTheme=fast|relaxed|off|default`（或启动脚本 `-AnimTheme`）。

### 1.15 QSlider —— Fluent 滑块【新增 v0.4.0】

| 方法 | 说明 |
|------|------|
| `QSlider(min, max, value)` | 创建滑块（自绘：轨道+圆钮，accent 填充） |
| `value()` / `setValue(int)` | 取值 / 设置（点击跳转 120ms 动画） |
| `setRange(min, max)` | 范围 |
| `onValueChanged(Consumer<Integer>)` | 值变化回调（拖动高频） |

### 1.16 QScrollArea / QProgressBar / JQtNavigation / QMessageBox / JQtInfoBar【新增 v0.4.0】

| 类 | 方法 | 说明 |
|------|------|------|
| `QScrollArea` | `setWidget(w)` / `setWidgetResizable(b)` | 滚动区（QScrollArea） |
| `QProgressBar` | `value()` / `setValue(v)` / `setRange(min,max)` | 进度条（QSS chunk） |
| `JQtNavigation` | `addItem(icon,text)` / `setCurrentIndex(i)` / `onChanged(...)` | 侧栏导航（选中高亮滑动动画） |
| `QMessageBox` | `showQuestion(win,title,text)` / `showInfo(win,title,text)` | 模态对话框（阻塞） |
| `JQtInfoBar` | `show(window,text,durationMs)` | 顶部通知条（自动消失） |
### 1.9 QCheckBox —— 复选框（QSS 可呈现 Fluent 开关）

| 方法 | 说明 |
|------|------|
| `QCheckBox(String text)` | 创建 |
| `isChecked()` / `setChecked(boolean)` | 勾选状态 |
| `onToggled(Consumer<Boolean>)` | 状态切换回调 |

### 1.10 布局：QVBoxLayout / QHBoxLayout（继承 QLayout）

> 新增：`addLayout(QLayout)` —— **布局嵌套**（VBox 中嵌 HBox，可自绘标题栏/工具行）

| 方法 | 说明 |
|------|------|
| `QVBoxLayout()` / `QHBoxLayout()` | 创建垂直 / 水平布局 |
| `void addWidget(QWidget)` | 加入控件 |
| `void setSpacing(int)` | 控件间距（像素） |
| `void setContentsMargins(int)` / `(l,t,r,b)` | 布局四周留白（外边距） |
| `void setContentsMargins(int)` / `(l,t,r,b)` | 布局四周留白（外边距） |
| `void addStretch(int)` | 弹性空间（占满剩余空间的比例） |

### 1.9 信号注册规则

- 所有 `onXxx` 均可**注册多个监听器**，按注册顺序触发；
- 回调**始终在 GUI 主线程**执行，可直接操作任何控件，无需加锁；
- 信号对应关系：`onClicked`←clicked、`onPressed`←pressed、`onReleased`←released、
  `onToggled`←toggled、`onTextChanged`←textChanged、`onReturnPressed`←returnPressed、
  `onCurrentIndexChanged`←currentIndexChanged、`onItemClicked`←itemClicked、
  `onCurrentRowChanged`←currentRowChanged、`onClose`←closeEvent、
  `onResized`←resizeEvent、`onMoved`←moveEvent、`onAboutToQuit`←aboutToQuit。

### 1.10 内存管理

- Java 对象不可达时自动释放 C++ 对象（Cleaner，GUI 线程安全）；
- 控件加入窗口/布局后由 Qt 父子关系管理；
- 调用已释放对象的方法会抛出 `IllegalStateException`（不会崩溃）；
- 未创建 `QApplication` 就创建控件会抛出 `IllegalStateException`。

---

<a id="en"></a>
## English Version

> Extracted from the v0.7.5-Generator-Kit sources. All widgets extend `QWidget` (base methods in 1.0).

### 1.0 QWidget (base, shared by all widgets)

| Method | Description |
|--------|-------------|
| `boolean isCreated()` | whether the native object exists |
| `void dispose()` | release the C++ object manually (usually not needed; GC handles it) |
| `boolean isDisposed()` | whether disposed |
| `long nativeHandle()` | native handle (advanced) |
| `void setStyleSheet(String qss)` | widget-level QSS (layers over the global style) |
| `void animateMove(x,y,ms)` / `(x,y,ms,JQtEasing)` | animated move (OutCubic / custom easing) |
| `void animateResize(w,h,ms)` / `(w,h,ms,JQtEasing)` | animated resize (OutCubic / custom easing) |
| `void setDropShadow(blur,alpha[,dx,dy])` | drop shadow (QSS box-shadow substitute - Qt QSS lacks box-shadow) |
| `void clearDropShadow()` | remove drop shadow |
| `void setBorderRadius(int)` | custom corner radius in px (merged with widget-level setStyleSheet) |
| `int width()` / `height()` / `x()` / `y()` | geometry queries (px) |
| `void show()` / `hide()` / `isVisible()` | visibility control |
| `void setEnabled(boolean)` / `isEnabled()` | enable/disable |
| `void setFixedSize(w,h)` | fixed size |

### 1.1 QApplication

| Method | Description |
|--------|-------------|
| `QApplication()` | create the app (one per process; **must be created first**) |
| `void exec()` | run the event loop (blocks until the last window closes) |
| `void quit()` | quit the event loop |
| `void scheduleQuit(long ms)` | auto-quit after ms |
| `void schedule(Runnable task, long delayMs)` | run a task on the GUI thread after delay (thread-safe) |
| `void setStyleSheet(String qss)` | set global style sheet (QSS) |
| `void setStyle(String style)` | switch style (e.g. `"Fusion"`) |
| `void setLightMode(boolean)` | force light palette (Qt may mis-detect dark mode inside a Java process); or use `-Djqt.lightMode=true` at startup |
| `void setLightMode(boolean)` | force light palette (Qt may mis-detect dark mode inside a Java process); or use `-Djqt.lightMode=true` at startup |
| `void setTheme(String)` | apply theme (fluent-dark / fluent-light, QSS + palette packaged) |
| `void setTheme(path, vars[,light])` | render theme from QSS template + vars (%var% placeholders) |
| `void setAccentColor(String hex)` | switch accent color (#RRGGBB; template re-render + palette Highlight + custom switch) |
| `void setFontFamily(String[,size])` | global font (auto-applies system CJK font at construction) |
| `void onAboutToQuit(Runnable)` | callback before app quits (aboutToQuit signal) |

### 1.2 QMainWindow

| Method | Description |
|--------|-------------|
| `QMainWindow(String title)` | 800x600 window |
| `QMainWindow(String title, int w, int h)` | sized window |
| `void show()` / `void hide()` / `void close()` | show / hide / close (fires onClose; exec returns when the last window closes) |
| `void resize(int w, int h)` | resize (fires onResized) |
| `void setTitle(String)` | set title |
| `void addWidget(QWidget child)` | add a child (auto-placed without a layout) |
| `void setLayout(QLayout layout)` | install a layout manager |
| `void onClose(Runnable)` | window close callback |
| `void onResized(BiConsumer<Integer,Integer>)` | resize callback (w, h) |
| `void onMoved(BiConsumer<Integer,Integer>)` | move callback (x, y) |
| `void setFrameless(boolean)` | frameless mode (DWM shadow + edge resize + title drag, Fluent style) |
| `void setAcrylic(boolean)` | acrylic background (Win10+ blur) |
| `void setRoundedCorners(boolean)` | Windows 11 rounded corners |
| `void setDraggable(boolean)` / `setBorderWidth(int)` | drag toggle / resize hotzone width |
| `void minimize()` / `maximize()` / `toggleMaximize()` / `isMaximized()` | window states |

### 1.3 QPushButton

| Method | Description |
|--------|-------------|
| `QPushButton(String text)` | create |
| `void setText(String)` | set text |
| `void setCheckable(boolean)` / `void setChecked(boolean)` | checkable mode |
| `void onClicked(Runnable)` | clicked signal |
| `void onPressed(Runnable)` | pressed signal |
| `void onReleased(Runnable)` | released signal |
| `void onToggled(Consumer<Boolean>)` | toggled signal (new state) |

### 1.4 QLabel

| Method | Description |
|--------|-------------|
| `QLabel(String text)` | create |
| `void setText(String)` | set text |

### 1.5 QLineEdit

| Method | Description |
|--------|-------------|
| `QLineEdit(String text)` | create |
| `String text()` | current text |
| `void setText(String)` | set text (fires onTextChanged) |
| `void setPlaceholderText(String)` | placeholder |
| `void onTextChanged(Consumer<String>)` | text changed |
| `void onReturnPressed(Runnable)` | enter pressed |

### 1.6 QComboBox

| Method | Description |
|--------|-------------|
| `QComboBox()` | create |
| `void addItem(String)` | append item |
| `int currentIndex()` / `String currentText()` | current selection |
| `void setCurrentIndex(int)` | select (fires onCurrentIndexChanged) |
| `void onCurrentIndexChanged(Consumer<Integer>)` | selection changed |

### 1.7 QListWidget

| Method | Description |
|--------|-------------|
| `QListWidget()` | create |
| `void addItem(String)` | append item |
| `int currentRow()` | current row |
| `void onItemClicked(Consumer<Integer>)` | item clicked (row) |
| `void onCurrentRowChanged(Consumer<Integer>)` | current row changed |

### 1.8 QFrame — card/container (Fluent card base)

| Method | Description |
|--------|-------------|
| `QFrame()` | create a panel (QFrame, QSS-stylable) |
| `addWidget(widget)` | add a child directly |
| `setLayout(layout)` (inherited) | inner layout |

### 1.9 JQtSwitch — Fluent switch (custom painted + slide animation) [v0.2.0]

| Method | Description |
|--------|-------------|
| `JQtSwitch()` / `JQtSwitch(boolean checked)` | create switch (off by default / given state) |
| `isChecked()` / `setChecked(boolean)` | query / set state (slide animation, track color eases with progress) |
| `onToggled(Consumer<Boolean>)` | state change callback (new state) |

### 1.10 JQtEasing — easing functions (40 types) [v0.2.0]

| Constant | Description |
|----------|-------------|
| `LINEAR` | linear |
| `IN_QUAD` ~ `OUT_IN_QUINT` | quad .. quint curves |
| `IN_SINE` / `OUT_SINE` / `IN_OUT_SINE` | sine |
| `IN_EXPO` ~ `OUT_IN_EXPO` | exponential |
| `IN_CIRC` ~ `OUT_IN_CIRC` | circular |
| `IN_ELASTIC` ~ `OUT_IN_ELASTIC` | elastic |
| `IN_BACK` ~ `OUT_IN_BACK` | back |
| `IN_BOUNCE` / `OUT_BOUNCE` / `IN_OUT_BOUNCE` / `OUT_IN_BOUNCE` | bounce |

Maps Qt `QEasingCurve::Type` 0~40; optional param of every animation method.

### 1.11 JQtAnimation — advanced property animation [v0.2.0]

| Method | Description |
|--------|-------------|
| `JQtAnimation(widget, property, from, to, ms, easing)` | create animation (e.g. `windowOpacity` 1.0→0.2) |
| `setLoopCount(int)` | loop count (-1 = infinite) |
| `start()` / `stop()` | start / stop |
| `onFinished(Consumer<JQtAnimation>)` | finished callback (GUI thread, one-shot) |

> Impl: QPropertyAnimation + DeleteWhenStopped on the C++ side;
> Java callback registered via **weak reference**, auto-unregistered after finish — no leaks, no GC pinning.

### 1.12 JQtAnimations — Fluent motion library [v0.2.0]

| Method | Description |
|--------|-------------|
| `entrance(QWidget)` / `(w, ms, easing)` | entrance: slide up 24px from below (duration scaled by animation theme) |
| `exit(QWidget)` / `(w, ms, easing)` | exit: slide down, hides when finished |
| `setHoverEnabled(boolean)` | global button hover animation toggle (follows theme) |

**hover**: every QPushButton has a 150ms OutCubic hover highlight by default
(white 8.5% overlay; clean-room implementation, Fluent public motion spec).

> Impl: entrance/exit use pure position animation (QPropertyAnimation pos).
> Pitfall: QSS-styled widgets + QGraphicsOpacityEffect crash Qt (null deref), so no opacity effects.

### 1.13 JQtPivot — Fluent tabs with sliding indicator [v0.2.0]

| Method | Description |
|--------|-------------|
| `JQtPivot()` | create tab group (36px high, custom painted) |
| `addItem(String)` | append a tab |
| `currentIndex()` / `setCurrentIndex(int)` | current tab (indicator slides 200ms OutCubic) |
| `onChanged(Consumer<Integer>)` | tab change callback (new index) |

> Text color follows QPalette, indicator uses Highlight color (themeable).

### 1.14 JQtAnimationTheme — animation themes [v0.2.0]

| Constant / Method | Description |
|------------------|-------------|
| `DEFAULT` (1.0x, OutCubic) | standard pace |
| `FAST` (0.65x) | snappy (touch panels / demos) |
| `RELAXED` (1.6x, OutQuint) | relaxed (desktop immersion) |
| `OFF` (0x) | disable all motion (accessibility / low-end) |
| `new JQtAnimationTheme(speed, easing)` | custom (speed = duration multiplier, 0 = off) |
| `QApplication.setAnimationTheme(theme)` | apply globally (all motion follows) |

> Startup: `-Djqt.animTheme=fast|relaxed|off|default` (or `-AnimTheme` in launchers).

### 1.15 QSlider / QScrollArea / QProgressBar / JQtNavigation / QMessageBox / JQtInfoBar [v0.4.0]

| Class | Key methods | Description |
|-------|-------------|-------------|
| `QSlider` | `(min,max,value)` / `value()` / `setValue` / `onValueChanged` | Fluent painted slider (accent fill, drag + 120ms animation) |
| `QScrollArea` | `setWidget` / `setWidgetResizable` | scroll area |
| `QProgressBar` | `value` / `setValue` / `setRange` | progress bar (QSS chunk) |
| `JQtNavigation` | `addItem(icon,text)` / `setCurrentIndex` / `onChanged` | sidebar navigation (sliding highlight) |
| `QMessageBox` | `showQuestion` / `showInfo` | modal dialogs (blocking) |
| `JQtInfoBar` | `show(window,text,ms)` | top toast, auto-dismiss |
### 1.9 QCheckBox — check box (QSS can render a Fluent switch)

| Method | Description |
|--------|-------------|
| `QCheckBox(String text)` | create |
| `isChecked()` / `setChecked(boolean)` | checked state |
| `onToggled(Consumer<Boolean>)` | toggle callback |

### 1.10 Layouts: QVBoxLayout / QHBoxLayout (extend QLayout)

> New: `addLayout(QLayout)` — **layout nesting** (HBox inside VBox for custom title bars / tool rows)

| Method | Description |
|--------|-------------|
| `QVBoxLayout()` / `QHBoxLayout()` | vertical / horizontal layout |
| `void addWidget(QWidget)` | add widget |
| `void setSpacing(int)` | spacing in px |
| `void setContentsMargins(int)` / `(l,t,r,b)` | layout margins |
| `void addStretch(int)` | stretch factor |

### 1.9 Signal Rules

- `onXxx` supports **multiple listeners**, invoked in registration order;
- callbacks always run on the **GUI main thread** — safe to touch any widget, no locking needed;
- mapping: onClicked←clicked, onPressed←pressed, onReleased←released, onToggled←toggled,
  onTextChanged←textChanged, onReturnPressed←returnPressed, onCurrentIndexChanged←currentIndexChanged,
  onItemClicked←itemClicked, onCurrentRowChanged←currentRowChanged, onClose←closeEvent,
  onResized←resizeEvent, onMoved←moveEvent, onAboutToQuit←aboutToQuit.

### 1.10 Memory Management

- unreachable Java objects release their C++ objects automatically (Cleaner, GUI-thread safe);
- widgets added to a window/layout are owned by the Qt parent-child relationship;
- calling a disposed object throws `IllegalStateException` (no native crash);
- creating widgets before `QApplication` throws `IllegalStateException`.


---

## 新增（v0.5.0-TEST 开发中）· New in v0.5.0-TEST (in development)

> 23 个新类 / 104 个新公共方法（批 1-4）。
> 23 new classes / 104 new public methods (batch 1-4).

**对话框家族 · Dialogs**

| 类 Class | 方法 Methods |
|----------|--------------|
| QInputDialog | `getText` `getInt` `getItem` |
| QFileDialog | `getOpenFileName` `getSaveFileName` `getExistingDirectory` |
| QColorDialog | `getColor` |
| QFontDialog | `getFont` |
| QMessageBox+ | `showWarning` `showCritical` `showOkCancel` |

**数据展示 · Data**

| 类 Class | 方法 Methods |
|----------|--------------|
| QTableWidget | `setItemText` `itemText` `setColumnHeaders` `setRowCount` `setColumnCount` `rowCount` `columnCount` `setColumnWidth` `setRowHeight` `resizeColumnsToContents` `clearContents` `currentRow` `onCellClicked` `onCurrentRowChanged` |
| QTreeWidget | `addTopLevelItem` `addChild` `itemText` `setItemText` `expandAll` `collapseAll` `clear` `onItemClicked` |

**界面组织 · Layout**

| 类 Class | 方法 Methods |
|----------|--------------|
| QTabWidget | `addTab` `setCurrentIndex` `currentIndex` `setTabText` `onCurrentChanged` |
| QGroupBox | `setTitle` `title` |
| QStackedLayout | `addPage` `setCurrentIndex` `currentIndex` `setCurrentWidget` |
| QSplitter | `setOrientation` `addWidget` `setSizes` `sizes` `setHandleWidth` |
| QGridLayout | `addWidget(3/5 参)` `setColumnStretch` `setRowStretch` |
| QFormLayout | `addRow(文本/控件)` |

**输入控件 · Input**

| 类 Class | 方法 Methods |
|----------|--------------|
| QSpinBox | `setRange` `value` `setValue` `onValueChanged` `onTextChanged` |
| QDial | `setRange` `value` `setValue` `onValueChanged` |
| QRadioButton | `setText` `isChecked` `setChecked` `onToggled` |
| QDateTimeEdit | `setDisplayFormat` `setDateTime` `text` `onTextChanged` |

**窗口体系 · Window**

| 类 Class | 方法 Methods |
|----------|--------------|
| QMenu | `addItem` `popup(坐标/锚点)` `onTriggered` |
| QToolBar | `addButton` `addWidget` `onTriggered` |
| QStatusBar | `showMessage` `clearMessage` `currentMessage` |
| QSystemTrayIcon | `show` `hide` `isVisible` `setToolTip` `showMessage` `dispose` |

**富文本 · Rich text**

| 类 Class | 方法 Methods |
|----------|--------------|
| QTextEdit | `setPlainText` `toPlainText` `append` `setReadOnly` `isReadOnly` `onTextChanged` |

**自绘 · Canvas**

| 类 Class | 方法 Methods |
|----------|--------------|
| QCanvasWidget | `onPaint` `repaint` |
| QPainter | `setColor` `setStrokeWidth` `drawLine` `drawRect` `fillRect` `drawCircle` `fillCircle` `drawRoundRect` `drawText` `setFont` `translate` `rotate` |

**应用 · Application**

| 类 Class | 方法 Methods |
|----------|--------------|
| QApplication+ | `rhiBackend(String/无参)` `runOnUiThread` |

---

## 新增（v0.7.4-Universal-Kit）· New in v0.7.4-Universal-Kit

> QSerialPort 完整绑定（Qt SerialPort 模块）。

| 类 Class | 方法 Methods |
|----------|--------------|
| QSerialPort | 静态 `availablePorts()`；`setPortName/portName` `setBaudRate/baudRate` `setDataBits` `setParity` `setStopBits` `setFlowControl` `open(READ/WRITE/READ_WRITE)` `close` `isOpen` `write(byte[]/String)` `readAll` `readAllText` `readLine` `bytesAvailable` `waitForReadyRead(ms)` `flush` `clear` `errorString` `onReadyRead` `onBytesWritten` |

**依赖**：qtserialport 为独立 Qt 模块；发布包含 Qt6SerialPort 运行库。

---

## 新增（v0.7.3-Universal-Kit）· New in v0.7.3-Universal-Kit

> QOpenGLWidget 绑定：通用 GPU 渲染画布（Qt6OpenGLWidgets）。

| 类 Class | 方法 Methods |
|----------|--------------|
| QOpenGLWidget | `onInitialize`（initializeGL）`onPaint`（paintGL，context current）`onResized`（resizeGL）`setClearColor(0xAARRGGBB)` `setAutoClear(boolean)` `makeCurrent` `doneCurrent` |

**LWJGL 挂接**：paintGL 回调内 GL context 已 current，Java 侧 `GL.createCapabilities()` 直接可用；JQt 提供画布与 context，GL 调用由 Java 生态标准（LWJGL）接管。

---

## 新增（v0.7.2-Universal-Kit）· New in v0.7.2-Universal-Kit

> 工业模块：QPrinter（打印/PDF）+ QSql（数据库）。

**打印 · Printing**

| 类 Class | 方法 Methods |
|----------|--------------|
| QPrinter | `setOutputFormat(NATIVE/PDF)` `setOutputFileName` `setResolution` `setPageSize(A4/A3/A5/Letter/Legal)` `newPage` |
| QTextEdit+ | `print(QPrinter)` `printToPdf(path)` |
| QWidget+ | `printToPdf(path)`（render 导出） |

**数据库 · Database**

| 类 Class | 方法 Methods |
|----------|--------------|
| QSqlDatabase | `addDatabase(driver[, connName])` `setDatabaseName` `setUserName` `setPassword` `setHostName` `setPort` `open` `close` `isOpen` `exec` `lastError` |
| QSqlQuery | `next` `value` `valueCount` `isSelect` `numRowsAffected` `lastError` |

**说明**：驱动名 SQLITE/PSQL/MYSQL；Windows 已内置插件预加载 workaround（qsqlite.dll 开箱即用）；发布包含 plugins/sqldrivers。

---

## 新增（v0.7.1-Universal-Kit）· New in v0.7.1-Universal-Kit

> L1 收尾：149 → 192/205（93.7%）；剩余 13 项 ⛔ 阻塞归档（model/validator/pixmap 对象化依赖、Qt 6 无此 API；QPlainTextEdit print 由 v0.7.2 printToPdf 解锁，QSpinBox textChanged 由 v0.7.4 绑定解锁——Qt 6 文档核实存在）。

**新类 · New classes**

| 类 Class | 方法 Methods |
|----------|--------------|
| QAction | `text` `setText` `icon` `setIcon(路径)` `shortcut` `setShortcut` `toolTip` `setToolTip` `setFont` `checkable` `checked` `toggle` `trigger` `onTriggered` `onToggled` `setMenu` |
| QDialog | `exec` `open` `accept` `reject` |
| QMenuBar | `addMenu(String/QMenu)` `clear` `onTriggered` |
| QListView | `addItem` `setItems` `item` `count` `clear` `setSpacing/spacing` `setWordWrap/wordWrap` `currentItem` `setCurrentItem` `onSelectionChanged` |

**补全 · Completions**

| 类 Class | 方法 Methods |
|----------|--------------|
| QMessageBox | 实例化：`setText` `setWindowTitle` `setIcon` `exec` `open` `close` |
| QFile | 实例：`open(READ_ONLY/WRITE_ONLY/READ_WRITE/APPEND)` `close` `isOpen` `write` `readAll` `readLine` |
| QClipboard | `setPixmap(byte[] PNG/JPEG)` `pixmap()` |
| QTextEdit | `setPlaceholderText/placeholderText`（底层 QPlainTextEdit） |
| QLineEdit | `placeholderText` getter |
| QPushButton | `text` `setIcon/icon` `setShortcut/shortcut` |
| QMenu | `setIcon/icon` |
| QListWidget | `onItemChanged` `row(text)` |
| QMainWindow | `onIconSizeChanged` `onToolButtonStyleChanged`（委托 QToolBar 信号） |
| QWidget | `find(winId)` `layout()` `setWindowIcon(路径)` |
| QColorDialog | `open()` `onColorSelected`（非阻塞） |
| QSize | `width()` `height()` 方法 |
| QColor | 值类：`value/hue/saturation(#RRGGBB)` |
| QApplication | `paletteText()` `palettePlaceholderText()` |

**⛔ 阻塞归档 · Blocked (documented)**（已对照 doc.qt.io/qt-6 重新核实）：
- **对象化依赖**（Qt 6 API 存在，需 QPixmap/QIcon/QValidator 等对象体系，v0.8.0 对象值类型覆盖）：QComboBox model/validator、QLineEdit validator、QLabel movie/picture/pixmap、QIcon pixmap、QCursor mask/pixmap、QFont Stretch、QWidget mask
- **Qt 6 无此 API**：QUrl clear、QBoxLayout stretch(int) getter（注：setStretch 存在，可做）
> 已解锁：QPlainTextEdit print（v0.7.2 printToPdf）、QSpinBox textChanged（v0.7.4 绑定，Qt 6 确认存在）。

---

## 新增（v0.7.0-Universal-Kit）· New in v0.7.0-Universal-Kit

> 跨平台独家能力：Exclusive Kit 从 Windows 扩展到 macOS / Linux，同一 API 三平台一致语义。
> Cross-platform exclusive capabilities: Exclusive Kit extended to macOS/Linux with identical API semantics.

**统一 API（三平台同签名）· Unified APIs (same signature on all 3 platforms)**

| API | Windows | macOS | Linux |
|-----|---------|-------|-------|
| `QApplication.preventSleep(boolean)` | SetThreadExecutionState（阻止睡眠+关屏） | NSProcessInfo idleSystemSleepDisabled | D-Bus ScreenSaver/gnome-SessionManager Inhibit |
| `QApplication.setAutoStart(boolean, path)` | HKCU Run 注册表（v0.6.1） | LaunchAgent plist | XDG autostart .desktop |
| `QApplication.showNotification(title, body, ms)` | 托盘气泡（QSystemTrayIcon） | NSUserNotification（通知中心） | D-Bus org.freedesktop.Notifications |

**macOS 独家 · macOS exclusive**

| API | 说明 |
|-----|------|
| `QMainWindow.setDockBadge(String)` / `clearDockBadge()` | Dock 角标（对齐 Windows 任务栏进度） |
| `QMainWindow.setMacTitlebarTransparent(boolean)` | 透明标题栏（保留红黄绿按钮） |
| `QMainWindow.setMacFullSizeContentView(boolean)` | 全尺寸内容视图（内容延伸到标题栏） |

**已知限制 · Known limits**

- Linux 全局热键（X11 XGrabKey / Wayland portal）需 libX11 依赖且 Wayland 受限，列为 v0.7.x 候选，暂未实现
- macOS 通知使用 NSUserNotification（Apple 已弃用但可用，无需权限弹窗）；Windows 通知为托盘气泡而非通知中心 Toast（Toast 需打包身份）
- macOS 标题栏 API 建议在窗口 show() 前调用（NSWindow styleMask 修改时机）

