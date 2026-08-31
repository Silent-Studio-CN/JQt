# 01 · Win32 Window System and native Layer

> This chapter documents the pitfalls encountered by JQt on Windows platform window systems and their fixes.
> Core objects: JQtWindowShell (QWidget subclass), nativeSetFrameless, DWM, WM_* messages.

## 1. setFrameless Hot switching Failure - The most profound lesson in this directory

### Phenomenon
It is called after the window is displayed `w.setFrameless(false)` Switch back to the original border**The first click is invalid**,
Must first `setFrameless(true)` again `setFrameless(false)` It only takes effect (" close first, then open ").

### Root cause (two layers superimposed)

**The first layer: setWindowFlag only changes the Qt layer flag**
```cpp
// 原实现（错误）：
win->setWindowFlag(Qt::FramelessWindowHint, false);
win->show();   // 窗口已显示，show() 是空操作
```
"Qt" `setWindowFlag` Only update the internal windowFlags of the QWidget. ** Do not rebuild the HWND.
Nor will the Win32 style bit (WS_CAPTION/WS_THICKFRAME) be updated. The appearance of the window remains unchanged.

**The second layer: Residual DWM extended borders**
The borderless mode has been invoked `DwmExtendFrameIntoClientArea(hwnd, margins{1,1,1,1})` Create a shadow.
When switching back to the original border**Never clear**This DWM extension - the extension will "eat up" the native border.

The saying "close first, then open" works precisely because `setFrameless(true)` It is the call of applyShadow()
The timing sequence happened to trigger the DWM recalculation for the second time `setFrameless(false)` It only truly takes effect.

### Fix (native layer root cure, merged and released)

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

### Lesson
- **To change the window style, you must directly operate on the Win32 style bit**Qt's setWindowFlag is unreliable for displayed Windows.
- **The DWM extended border is "stateful"**-- After setting, it must be explicitly cleared (margins=0); otherwise, it will remain permanently.
- Verification method after repair: GetWindowLongPtrW(GWL_STYLE) dot to confirm the change of the style bit
(`0x86CE0000` There is WS_CAPTION,`0x86000000` None.

## 2. Layout drift after window reconstruction

### Phenomenon
After cutting the "Native border Open" (hide/show reconstruction window), click the button "No response" --
In fact, the overall position of the button has shifted, and the user still clicks at the old position.

### Root cause
After the hide/show reconstruction window,**The size of the customer area has changed**(The native border occupies the non-client area)
The layout manager is rearranged, and all control positions are moved down/right.

### "Repair
```java
w.setFrameless(false);
w.hide(); w.show();
w.setFixedSize(1280, 720);   // 重建后强制恢复固定尺寸
```

### Lesson
- After any hide/show or setWindowFlag reconstruction path,**The fixed-size constraints need to be reapplied**.
- In the user touchscreen scenario, coordinate conversion (DPR) will make "missing clicks" more concealed - first check the window size and then suspect the button.

## The three-piece set of WM_NCHITTEST/WM_NCCALCSIZE/WM_GETMINMAXINFO

Key messages for customizing and scaling hot zones in borderless Windows (implemented in JQt, record design highlights) :

- **WM_NCHITTEST**Manually scale the hot zone (the same as qframelesswindow). Blank area in the title bar
(40 logical px at the top, avoiding the right button area ~150px) Return to HTCAPTION → Go through the system's native drag chain.
Pay attention to DPI**Message coordinates are physical pixels, while Qt coordinates are logical pixels and need to be converted to /dpr**.
- **WM_NCCALCSIZE**When there is no border, return 0 and fill the client area (to avoid system borders occupying space).
- **WM_GETMINMAXINFO**Maximize constraints to the display workspace (the borderless window covers the taskbar by default).
Attention `_WIN32_WINNT 0x0A00` The GCC/llvm-mingw declaration is consistent (returns UINT).

## 4. Touch → Mouse Composite (JQtPointerFilter)

Windows Touch (WM_POINTER*) needs to synthesize WM_LBUTTONDOWN/UP/MOUSEMOVE to be fed to Qt:

- Global QAbstractNativeEventFilter, covering all Qt top-level Windows (including QComboBox layer).
- POINTERDOWN → PostMessage(WM_LBUTTONDOWN); POINTERUP → WM_LBUTTONUP;"
UPDATE with key state (g_pointerPressed).
- Touch and press the title bar area → Do not synthesize, let the system handle it (HTCAPTION native drag chain follows).
- The coordinates use ScreenToClient physical pixels, and Qt internally converts them to DPR**When verifying, pay attention to the mixed use of physical and logical coordinates**.

## 5. On-screen Keyboard (TabTip

Known defect of Qt frameless window: The frameless window does not automatically pop up the screen keyboard when focusing.
JQt explicitly Toggle TabTip (`jqtToggleTabTip`It is essential for touch devices.

## 6. Crash logs and exception codes

- Windows SEH unhandled exception → `SetUnhandledExceptionFilter` write `jqt-crash.log`
(Time/exception code/address/thread), and then continue to hand it over to the system.
- **Exit code -1 for troubleshooting**The normal exit of the JVM is 0. -1 is usually a native crash when exiting the cleanup phase
(For example, the timer callback is triggered after the destruction of QApplication). Diagnostic methods
1. "onClose" confirms that the normal exit path has been taken.
2. The JVM shutdown hook tick confirms that main has ended normally.
3. If both lines are missing → The JVM crashes before main returns.
- Lesson from Gallery: scheduleGeo is scheduled recursively every second, and callbacks may be triggered after exiting
"QBasicTimer destroyed" → Use `volatile boolean appRunning` Stop the recursion at onClose.

## 7. Platform Differences (Cross-platform Design)

- **macOS / Windows ARM64**The official Qt build does not include the OpenGLWidgets module
(Apple abandons OpenGL) → Build Throw `UnsupportedOperationException`The API exists but is downgraded.
- **macOS**: setDockBadge/clearDockBadge (NSDockTile), setMacTitlebarTransparent,
SetMacFullSizeContentView. It is recommended to call it before show().
- **Linux**: XDG autostart, d-bus Inhibit (org. Freedesktop. ScreenSaver).
- Global hotkey: Windows WM_HOTKEY distribution (jqtDispatchHotkey); Linux relies on libX11
(Wayland Restricted) → v0.7.x candidate.

