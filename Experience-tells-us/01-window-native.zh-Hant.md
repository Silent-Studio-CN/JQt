# 01 · Win32 窗口系統與 native 層

> 本章記錄 JQt 在 Windows 平臺窗口系統上踩過的坑與修復。
> 核心對象：JQtWindowShell（QWidget 子類）、nativeSetFrameless、DWM、WM_* 消息。

## 1. setFrameless 熱切換失效 —— 本目錄最深刻的一課

### 現象
窗口顯示後調用 `w.setFrameless(false)` 切回原生邊框，**第一次點擊無效**，
必須先 `setFrameless(true)` 再 `setFrameless(false)` 才生效（"先關再開"）。

### 根因（兩層疊加）

**第一層：setWindowFlag 只改 Qt 層標誌**
```cpp
// 原实现（错误）：
win->setWindowFlag(Qt::FramelessWindowHint, false);
win->show();   // 窗口已显示，show() 是空操作
```
Qt 的 `setWindowFlag` 只更新 QWidget 內部 windowFlags，**不會重建 HWND，
也不會更新 Win32 樣式位（WS_CAPTION/WS_THICKFRAME）**。窗口外觀不變。

**第二層：DWM 擴展邊框殘留**
無邊框模式調用了 `DwmExtendFrameIntoClientArea(hwnd, margins{1,1,1,1})` 做陰影。
切回原生邊框時**從不清除**這個 DWM 擴展——擴展會把原生邊框"吃掉"。

"先關再開"恰好生效，是因爲 `setFrameless(true)` 時 applyShadow() 的調用
時序湊巧觸發了 DWM 重算，第二次 `setFrameless(false)` 才真正生效。

### 修復（native 層根治，已合入併發布）

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

### 教訓
- **改窗口樣式必須直接操作 Win32 樣式位**，Qt 的 setWindowFlag 對已顯示窗口不可靠。
- **DWM 擴展邊框是"有狀態"的**——設置後必須顯式清除（margins=0），否則永久殘留。
- 修復後驗證方式：GetWindowLongPtrW(GWL_STYLE) 打點，確認樣式位變化
（`0x86CE0000` 有 WS_CAPTION、`0x86000000` 無）。

## 2. 窗口重建後佈局漂移

### 現象
切「原生邊框 開」（hide/show 重建窗口）後，按鈕點擊"沒反應"——
實際是按鈕位置整體漂移，用戶點的還是舊位置。

### 根因
hide/show 重建窗口後，**客戶區尺寸變化**（原生邊框佔用非客戶區），
佈局管理器重排，所有控件位置下移/右移。

### 修復
```java
w.setFrameless(false);
w.hide(); w.show();
w.setFixedSize(1280, 720);   // 重建后强制恢复固定尺寸
```

### 教訓
- 任何 hide/show 或 setWindowFlag 重建路徑後，**固定尺寸約束需要重新應用**。
- 用戶觸摸屏場景下，座標換算（DPR）會讓"點不中"更隱蔽——先檢查窗口尺寸再懷疑按鈕。

## 3. WM_NCHITTEST / WM_NCCALCSIZE / WM_GETMINMAXINFO 三件套

無邊框窗口自定義縮放熱區的關鍵消息（JQt 已實現，記錄設計要點）：

- **WM_NCHITTEST**：手動實現縮放熱區（qframelesswindow 同款）。標題欄空白區
（頂部 40 邏輯 px，避開右側按鈕區 ~150px）返回 HTCAPTION → 走系統原生拖動鏈。
注意 DPI：**消息座標是物理像素，Qt 座標是邏輯像素，需 /dpr 換算**。
- **WM_NCCALCSIZE**：無邊框時返回 0，客戶區鋪滿（避免系統邊框佔位）。
- **WM_GETMINMAXINFO**：最大化約束到顯示器工作區（無邊框窗口默認蓋住任務欄）。
注意 `_WIN32_WINNT 0x0A00` 後 GCC/llvm-mingw 聲明一致（返回 UINT）。

## 4. 觸摸 → 鼠標合成（JQtPointerFilter）

Windows 觸摸（WM_POINTER*）需要合成 WM_LBUTTONDOWN/UP/MOUSEMOVE 才能餵給 Qt：

- 全局 QAbstractNativeEventFilter，覆蓋所有 Qt 頂層窗口（含 QComboBox 彈層）。
- POINTERDOWN → PostMessage(WM_LBUTTONDOWN)；POINTERUP → WM_LBUTTONUP；
UPDATE 帶按鍵狀態（g_pointerPressed）。
- 標題欄區域觸摸按下 → 不合成，讓系統處理（HTCAPTION 原生拖動鏈跟手）。
- 座標用 ScreenToClient 物理像素，Qt 內部按 DPR 換算——**驗證時注意物理/邏輯座標混用**。

## 5. 屏幕鍵盤（TabTip）

Qt frameless 窗口的已知缺陷：無邊框窗口聚焦時不自動彈屏幕鍵盤。
JQt 在聚焦/失焦時顯式 Toggle TabTip（`jqtToggleTabTip`），觸摸設備必備。

## 6. 崩潰日誌與異常碼

- Windows SEH 未處理異常 → `SetUnhandledExceptionFilter` 寫 `jqt-crash.log`
（時間/異常碼/地址/線程），然後繼續交給系統。
- **退出碼 -1 排查**：JVM 正常退出是 0。-1 通常是退出清理階段 native 崩潰
（如定時器回調在 QApplication 析構後觸發）。診斷手段：
1. onClose 打點確認走了正常退出路徑；
2. JVM shutdown hook 打點確認 main 正常結束；
3. 若兩行都缺 → JVM 在 main 返回前崩潰。
- Gallery 的教訓：scheduleGeo 每秒遞歸調度，退出後回調可能觸發
"QBasicTimer destroyed" → 用 `volatile boolean appRunning` 在 onClose 時停止遞歸。

## 7. 平臺差異（跨平臺設計）

- **macOS / Windows ARM64**：Qt 官方構建不含 OpenGLWidgets 模塊
（Apple 棄用 OpenGL）→ 構造拋 `UnsupportedOperationException`，API 存在但降級。
- **macOS**：setDockBadge/clearDockBadge（NSDockTile）、setMacTitlebarTransparent、
setMacFullSizeContentView。建議在 show() 前調用。
- **Linux**：XDG autostart、D-Bus Inhibit（org.freedesktop.ScreenSaver）。
- 全局熱鍵：Windows WM_HOTKEY 分發（jqtDispatchHotkey）；Linux 依賴 libX11
（Wayland 受限）→ v0.7.x 候選。

