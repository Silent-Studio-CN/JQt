# JQt 已实现 API 清单（v0.1.0-alpha）

<details>
<summary>🌐 语言 / Language</summary>

- [中文版](#zh) · [English Version](#en)

</details>

---

<a id="zh"></a>
## 中文版

> 本清单从源码自动提取，与 v0.1.0-alpha 发布包一一对应。
> 所有控件继承 `JQtWidget`，共享其基础方法（见 1.0）。

### 1.0 基类 JQtWidget（所有控件共享）

| 方法 | 说明 |
|------|------|
| `boolean isCreated()` | 控件是否已创建 |
| `void dispose()` | 手动释放 C++ 对象（一般无需调用，GC 自动回收） |
| `boolean isDisposed()` | 是否已释放 |
| `long nativeHandle()` | C++ 句柄（高级用途） |

### 1.1 JQtApplication —— 应用入口

| 方法 | 说明 |
|------|------|
| `JQtApplication()` | 创建应用（整个进程仅一个，**必须先创建**） |
| `void exec()` | 进入事件循环（阻塞，最后一个窗口关闭后返回） |
| `void quit()` | 退出事件循环 |
| `void scheduleQuit(long ms)` | 延迟 ms 毫秒后自动退出 |
| `void schedule(Runnable task, long delayMs)` | 延迟在 GUI 线程执行任务（线程安全） |
| `void onAboutToQuit(Runnable)` | 退出前回调（Qt aboutToQuit 信号） |

### 1.2 JQtWindow —— 窗口

| 方法 | 说明 |
|------|------|
| `JQtWindow(String title)` | 创建 800x600 窗口 |
| `JQtWindow(String title, int w, int h)` | 创建指定大小窗口 |
| `void show()` / `void hide()` | 显示 / 隐藏 |
| `void resize(int w, int h)` | 修改窗口大小（触发 onResized） |
| `void setTitle(String)` | 修改标题 |
| `void addWidget(JQtWidget child)` | 添加子控件（无布局时自动摆放） |
| `void setLayout(JQtLayout layout)` | 设置布局管理器 |
| `void onClose(Runnable)` | 窗口关闭回调 |
| `void onResized(BiConsumer<Integer,Integer>)` | 尺寸变化回调（参数：宽,高） |
| `void onMoved(BiConsumer<Integer,Integer>)` | 位置变化回调（参数：x,y） |

### 1.3 JQtButton —— 按钮

| 方法 | 说明 |
|------|------|
| `JQtButton(String text)` | 创建按钮 |
| `void setText(String)` | 修改文字 |
| `void setCheckable(boolean)` / `void setChecked(boolean)` | 勾选模式 |
| `void onClick(Runnable)` | 点击回调（clicked 信号） |
| `void onPressed(Runnable)` | 按下回调（pressed 信号） |
| `void onReleased(Runnable)` | 释放回调（released 信号） |
| `void onToggled(Consumer<Boolean>)` | 勾选切换回调（toggled 信号，参数为新状态） |

### 1.4 JQtLabel —— 文本标签

| 方法 | 说明 |
|------|------|
| `JQtLabel(String text)` | 创建标签 |
| `void setText(String)` | 修改文本 |

### 1.5 JQtLineEdit —— 单行输入框

| 方法 | 说明 |
|------|------|
| `JQtLineEdit(String text)` | 创建输入框 |
| `String text()` | 当前文本 |
| `void setText(String)` | 设置文本（触发 onTextChanged） |
| `void setPlaceholderText(String)` | 占位提示 |
| `void onTextChanged(Consumer<String>)` | 文本变化回调 |
| `void onReturnPressed(Runnable)` | 回车确认回调 |

### 1.6 JQtComboBox —— 下拉框

| 方法 | 说明 |
|------|------|
| `JQtComboBox()` | 创建下拉框 |
| `void addItem(String)` | 追加选项 |
| `int currentIndex()` / `String currentText()` | 当前选项 |
| `void setCurrentIndex(int)` | 选中指定项（触发 onCurrentIndexChanged） |
| `void onCurrentIndexChanged(Consumer<Integer>)` | 选项切换回调（参数：新索引） |

### 1.7 JQtListWidget —— 列表

| 方法 | 说明 |
|------|------|
| `JQtListWidget()` | 创建列表 |
| `void addItem(String)` | 追加列表项 |
| `int currentRow()` | 当前行号 |
| `void onItemClicked(Consumer<Integer>)` | 点击回调（参数：行号） |
| `void onCurrentRowChanged(Consumer<Integer>)` | 当前行切换回调 |

### 1.8 布局：JQtVBoxLayout / JQtHBoxLayout（继承 JQtLayout）

| 方法 | 说明 |
|------|------|
| `JQtVBoxLayout()` / `JQtHBoxLayout()` | 创建垂直 / 水平布局 |
| `void addWidget(JQtWidget)` | 加入控件 |
| `void setSpacing(int)` | 控件间距（像素） |
| `void addStretch(int)` | 弹性空间（占满剩余空间的比例） |

### 1.9 信号注册规则

- 所有 `onXxx` 均可**注册多个监听器**，按注册顺序触发；
- 回调**始终在 GUI 主线程**执行，可直接操作任何控件，无需加锁；
- 信号对应关系：`onClick`←clicked、`onPressed`←pressed、`onReleased`←released、
  `onToggled`←toggled、`onTextChanged`←textChanged、`onReturnPressed`←returnPressed、
  `onCurrentIndexChanged`←currentIndexChanged、`onItemClicked`←itemClicked、
  `onCurrentRowChanged`←currentRowChanged、`onClose`←closeEvent、
  `onResized`←resizeEvent、`onMoved`←moveEvent、`onAboutToQuit`←aboutToQuit。

### 1.10 内存管理

- Java 对象不可达时自动释放 C++ 对象（Cleaner，GUI 线程安全）；
- 控件加入窗口/布局后由 Qt 父子关系管理；
- 调用已释放对象的方法会抛出 `IllegalStateException`（不会崩溃）；
- 未创建 `JQtApplication` 就创建控件会抛出 `IllegalStateException`。

---

<a id="en"></a>
## English Version

> Extracted from the v0.1.0-alpha sources. All widgets extend `JQtWidget` (base methods in 1.0).

### 1.0 JQtWidget (base, shared by all widgets)

| Method | Description |
|--------|-------------|
| `boolean isCreated()` | whether the native object exists |
| `void dispose()` | release the C++ object manually (usually not needed; GC handles it) |
| `boolean isDisposed()` | whether disposed |
| `long nativeHandle()` | native handle (advanced) |

### 1.1 JQtApplication

| Method | Description |
|--------|-------------|
| `JQtApplication()` | create the app (one per process; **must be created first**) |
| `void exec()` | run the event loop (blocks until the last window closes) |
| `void quit()` | quit the event loop |
| `void scheduleQuit(long ms)` | auto-quit after ms |
| `void schedule(Runnable task, long delayMs)` | run a task on the GUI thread after delay (thread-safe) |
| `void onAboutToQuit(Runnable)` | callback before app quits (aboutToQuit signal) |

### 1.2 JQtWindow

| Method | Description |
|--------|-------------|
| `JQtWindow(String title)` | 800x600 window |
| `JQtWindow(String title, int w, int h)` | sized window |
| `void show()` / `void hide()` | show / hide |
| `void resize(int w, int h)` | resize (fires onResized) |
| `void setTitle(String)` | set title |
| `void addWidget(JQtWidget child)` | add a child (auto-placed without a layout) |
| `void setLayout(JQtLayout layout)` | install a layout manager |
| `void onClose(Runnable)` | window close callback |
| `void onResized(BiConsumer<Integer,Integer>)` | resize callback (w, h) |
| `void onMoved(BiConsumer<Integer,Integer>)` | move callback (x, y) |

### 1.3 JQtButton

| Method | Description |
|--------|-------------|
| `JQtButton(String text)` | create |
| `void setText(String)` | set text |
| `void setCheckable(boolean)` / `void setChecked(boolean)` | checkable mode |
| `void onClick(Runnable)` | clicked signal |
| `void onPressed(Runnable)` | pressed signal |
| `void onReleased(Runnable)` | released signal |
| `void onToggled(Consumer<Boolean>)` | toggled signal (new state) |

### 1.4 JQtLabel

| Method | Description |
|--------|-------------|
| `JQtLabel(String text)` | create |
| `void setText(String)` | set text |

### 1.5 JQtLineEdit

| Method | Description |
|--------|-------------|
| `JQtLineEdit(String text)` | create |
| `String text()` | current text |
| `void setText(String)` | set text (fires onTextChanged) |
| `void setPlaceholderText(String)` | placeholder |
| `void onTextChanged(Consumer<String>)` | text changed |
| `void onReturnPressed(Runnable)` | enter pressed |

### 1.6 JQtComboBox

| Method | Description |
|--------|-------------|
| `JQtComboBox()` | create |
| `void addItem(String)` | append item |
| `int currentIndex()` / `String currentText()` | current selection |
| `void setCurrentIndex(int)` | select (fires onCurrentIndexChanged) |
| `void onCurrentIndexChanged(Consumer<Integer>)` | selection changed |

### 1.7 JQtListWidget

| Method | Description |
|--------|-------------|
| `JQtListWidget()` | create |
| `void addItem(String)` | append item |
| `int currentRow()` | current row |
| `void onItemClicked(Consumer<Integer>)` | item clicked (row) |
| `void onCurrentRowChanged(Consumer<Integer>)` | current row changed |

### 1.8 Layouts: JQtVBoxLayout / JQtHBoxLayout (extend JQtLayout)

| Method | Description |
|--------|-------------|
| `JQtVBoxLayout()` / `JQtHBoxLayout()` | vertical / horizontal layout |
| `void addWidget(JQtWidget)` | add widget |
| `void setSpacing(int)` | spacing in px |
| `void addStretch(int)` | stretch factor |

### 1.9 Signal Rules

- `onXxx` supports **multiple listeners**, invoked in registration order;
- callbacks always run on the **GUI main thread** — safe to touch any widget, no locking needed;
- mapping: onClick←clicked, onPressed←pressed, onReleased←released, onToggled←toggled,
  onTextChanged←textChanged, onReturnPressed←returnPressed, onCurrentIndexChanged←currentIndexChanged,
  onItemClicked←itemClicked, onCurrentRowChanged←currentRowChanged, onClose←closeEvent,
  onResized←resizeEvent, onMoved←moveEvent, onAboutToQuit←aboutToQuit.

### 1.10 Memory Management

- unreachable Java objects release their C++ objects automatically (Cleaner, GUI-thread safe);
- widgets added to a window/layout are owned by the Qt parent-child relationship;
- calling a disposed object throws `IllegalStateException` (no native crash);
- creating widgets before `JQtApplication` throws `IllegalStateException`.
