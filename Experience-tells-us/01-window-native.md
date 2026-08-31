# 01 · Win32 窗口系统与 native 层

> 本章记录 JQt 在 Windows 平台窗口系统上踩过的坑与修复。
> 核心对象：JQtWindowShell（QWidget 子类）、nativeSetFrameless、DWM、WM_* 消息。

## 1. setFrameless 热切换失效 —— 本目录最深刻的一课

### 现象
窗口显示后调用 `w.setFrameless(false)` 切回原生边框，**第一次点击无效**，
必须先 `setFrameless(true)` 再 `setFrameless(false)` 才生效（"先关再开"）。

### 根因（两层叠加）

**第一层：setWindowFlag 只改 Qt 层标志**
```cpp
// 原实现（错误）：
win->setWindowFlag(Qt::FramelessWindowHint, false);
win->show();   // 窗口已显示，show() 是空操作
```
Qt 的 `setWindowFlag` 只更新 QWidget 内部 windowFlags，**不会重建 HWND，
也不会更新 Win32 样式位（WS_CAPTION/WS_THICKFRAME）**。窗口外观不变。

**第二层：DWM 扩展边框残留**
无边框模式调用了 `DwmExtendFrameIntoClientArea(hwnd, margins{1,1,1,1})` 做阴影。
切回原生边框时**从不清除**这个 DWM 扩展——扩展会把原生边框"吃掉"。

"先关再开"恰好生效，是因为 `setFrameless(true)` 时 applyShadow() 的调用
时序凑巧触发了 DWM 重算，第二次 `setFrameless(false)` 才真正生效。

### 修复（native 层根治，已合入并发布）

```cpp
win->frameless = (on == JNI_TRUE);
win->setWindowFlag(Qt::FramelessWindowHint, win->frameless);
#ifdef _WIN32
HWND hwnd = reinterpret_cast<HWND>(win->winId());
if (win->frameless) {
    LONG_PTR style = GetWindowLongPtrW(hwnd, GWL_STYLE);
    style &= ~(WS_CAPTION | WS_THICKFRAME | WS_SYSMENU | WS_MINIMIZEBOX | WS_MAXIMIZEBOX);
    SetWindowLongPtrW(hwnd, GWL_STYLE, style);
    win->applyShadow();
} else {
    LONG_PTR style = GetWindowLongPtrW(hwnd, GWL_STYLE);
    style |= WS_CAPTION | WS_THICKFRAME | WS_SYSMENU | WS_MINIMIZEBOX | WS_MAXIMIZEBOX;
    SetWindowLongPtrW(hwnd, GWL_STYLE, style);
    // 清除 DWM 扩展边框（margins=0）
    DwmExtendFrameIntoClientArea(hwnd, &margins{0,0,0,0});
}
// 强制非客户区立即重算
SetWindowPos(hwnd, nullptr, 0, 0, 0, 0,
             SWP_FRAMECHANGED | SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE);
#endif
```

### 教训
- **改窗口样式必须直接操作 Win32 样式位**，Qt 的 setWindowFlag 对已显示窗口不可靠。
- **DWM 扩展边框是"有状态"的**——设置后必须显式清除（margins=0），否则永久残留。
- 修复后验证方式：GetWindowLongPtrW(GWL_STYLE) 打点，确认样式位变化
  （`0x86CE0000` 有 WS_CAPTION、`0x86000000` 无）。

## 2. 窗口重建后布局漂移

### 现象
切「原生边框 开」（hide/show 重建窗口）后，按钮点击"没反应"——
实际是按钮位置整体漂移，用户点的还是旧位置。

### 根因
hide/show 重建窗口后，**客户区尺寸变化**（原生边框占用非客户区），
布局管理器重排，所有控件位置下移/右移。

### 修复
```java
w.setFrameless(false);
w.hide(); w.show();
w.setFixedSize(1280, 720);   // 重建后强制恢复固定尺寸
```

### 教训
- 任何 hide/show 或 setWindowFlag 重建路径后，**固定尺寸约束需要重新应用**。
- 用户触摸屏场景下，坐标换算（DPR）会让"点不中"更隐蔽——先检查窗口尺寸再怀疑按钮。

## 3. WM_NCHITTEST / WM_NCCALCSIZE / WM_GETMINMAXINFO 三件套

无边框窗口自定义缩放热区的关键消息（JQt 已实现，记录设计要点）：

- **WM_NCHITTEST**：手动实现缩放热区（qframelesswindow 同款）。标题栏空白区
  （顶部 40 逻辑 px，避开右侧按钮区 ~150px）返回 HTCAPTION → 走系统原生拖动链。
  注意 DPI：**消息坐标是物理像素，Qt 坐标是逻辑像素，需 /dpr 换算**。
- **WM_NCCALCSIZE**：无边框时返回 0，客户区铺满（避免系统边框占位）。
- **WM_GETMINMAXINFO**：最大化约束到显示器工作区（无边框窗口默认盖住任务栏）。
  注意 `_WIN32_WINNT 0x0A00` 后 GCC/llvm-mingw 声明一致（返回 UINT）。

## 4. 触摸 → 鼠标合成（JQtPointerFilter）

Windows 触摸（WM_POINTER*）需要合成 WM_LBUTTONDOWN/UP/MOUSEMOVE 才能喂给 Qt：

- 全局 QAbstractNativeEventFilter，覆盖所有 Qt 顶层窗口（含 QComboBox 弹层）。
- POINTERDOWN → PostMessage(WM_LBUTTONDOWN)；POINTERUP → WM_LBUTTONUP；
  UPDATE 带按键状态（g_pointerPressed）。
- 标题栏区域触摸按下 → 不合成，让系统处理（HTCAPTION 原生拖动链跟手）。
- 坐标用 ScreenToClient 物理像素，Qt 内部按 DPR 换算——**验证时注意物理/逻辑坐标混用**。

## 5. 屏幕键盘（TabTip）

Qt frameless 窗口的已知缺陷：无边框窗口聚焦时不自动弹屏幕键盘。
JQt 在聚焦/失焦时显式 Toggle TabTip（`jqtToggleTabTip`），触摸设备必备。

## 6. 崩溃日志与异常码

- Windows SEH 未处理异常 → `SetUnhandledExceptionFilter` 写 `jqt-crash.log`
  （时间/异常码/地址/线程），然后继续交给系统。
- **退出码 -1 排查**：JVM 正常退出是 0。-1 通常是退出清理阶段 native 崩溃
  （如定时器回调在 QApplication 析构后触发）。诊断手段：
  1. onClose 打点确认走了正常退出路径；
  2. JVM shutdown hook 打点确认 main 正常结束；
  3. 若两行都缺 → JVM 在 main 返回前崩溃。
- Gallery 的教训：scheduleGeo 每秒递归调度，退出后回调可能触发
  "QBasicTimer destroyed" → 用 `volatile boolean appRunning` 在 onClose 时停止递归。

## 7. 平台差异（跨平台设计）

- **macOS / Windows ARM64**：Qt 官方构建不含 OpenGLWidgets 模块
  （Apple 弃用 OpenGL）→ 构造抛 `UnsupportedOperationException`，API 存在但降级。
- **macOS**：setDockBadge/clearDockBadge（NSDockTile）、setMacTitlebarTransparent、
  setMacFullSizeContentView。建议在 show() 前调用。
- **Linux**：XDG autostart、D-Bus Inhibit（org.freedesktop.ScreenSaver）。
- 全局热键：Windows WM_HOTKEY 分发（jqtDispatchHotkey）；Linux 依赖 libX11
  （Wayland 受限）→ v0.7.x 候选。
