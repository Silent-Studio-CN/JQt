# 08 · setFrameless 修復全記錄（native 排查實戰範例）

> 這是 JQt 開發中最典型的一次 native 層 bug 排查+修復全流程，
> 從用戶反饋到根治發佈，完整還原方法論。可作爲後續排查的模板。

## 時間線

1. **用戶反饋**："邊框熱更新沒問題，但是要先點開，再關閉，再打開才顯示"
2. **Gallery 層 workaround**：hide/show 強制重建（第一次嘗試，治標）
3. **用戶再測**："多按幾次沒反應，一定要關一次再開，排查JQt問題" —— 要求根治
4. **讀 native 源碼**定位：nativeSetFrameless 只用 setWindowFlag
5. **修復**：Win32 樣式位 + DWM 清除 + SWP_FRAMECHANGED
6. **驗證**：FrameProbe + GetWindowLongPtrW 樣式位打點
7. **發佈**：編譯 jqt.dll → 部署 → 自動演示全綠 → commit/push

## 源碼定位（jqt_bridge.cpp）

```cpp
// JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeSetFrameless(...)
win->frameless = (on == JNI_TRUE);
if (win->frameless) {
    win->setWindowFlag(Qt::FramelessWindowHint, true);
    win->applyShadow();
} else {
    win->setWindowFlag(Qt::FramelessWindowHint, false);
}
win->show();
```

## 疑點排查過程（排除法）

| 假設 | 驗證 | 結論 
|------|------|------|
| QSpinBox 等 API 不存在 | javap | 無關 
| GL 控件崩潰 | GlCrashProbe | 無關 
| 定時器遞歸崩潰 | TimerProbe | 無關 
| 窗口被拉大 | SizeProbe（resize 被約束） | 無關 
| setWindowFlag 不更新 HWND | **GetWindowLongPtrW 打點：樣式位不變** | **確診** 

## 根治修復

```cpp
// 切无边框：清除样式位
style &= ~(WS_CAPTION | WS_THICKFRAME | WS_SYSMENU | WS_MINIMIZEBOX | WS_MAXIMIZEBOX);
// 切原生边框：恢复样式位 + 清 DWM 扩展 + 强制重算
style |= WS_CAPTION | WS_THICKFRAME | WS_SYSMENU | WS_MINIMIZEBOX | WS_MAXIMIZEBOX;
DwmExtendFrameIntoClientArea(hwnd, &margins{0,0,0,0});
SetWindowPos(hwnd, nullptr, 0,0,0,0,
             SWP_FRAMECHANGED | SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE);
```

## 驗證證據（樣式位日誌）

```
[JQt] setFrameless(1) before style=0x860B0000
[JQt] setFrameless(1) after  style=0x86000000    // 边框位清除 ✓
[JQt] setFrameless(0) before style=0x86CE0000
[JQt] setFrameless(0) after  style=0x86CF0000    // WS_CAPTION 置位 ✓ 第一次就生效
[JQt] setFrameless(1) before style=0x860A0000
[JQt] setFrameless(1) after  style=0x86000000
```

## 派生問題與連環修復

修復 native 後，用戶測試又發現：

1. **切原生邊框後按鈕點擊落空** → hide/show 重建窗口後佈局漂移
→ 重建後重新 setFixedSize(1280,720)。
2. **exited (code=-1)** → scheduleGeo 遞歸調度在退出後觸發
→ appRunning 標誌 + onClose/shutdown 診斷日誌。
3. **切主題黑殘留**（分幣必賺項目）→ 內聯樣式/控件級樣式/硬編碼三件套
→ 全部變量化 + 模板渲染（見 02 章）。

## 方法論總結

1. **先讀 native 源碼再猜**——setWindowFlag 的語義錯誤，讀代碼 10 分鐘確診。
2. **用最小探針 + 客觀信號（樣式位/色值/計數）驗證**，不靠肉眼看。
3. **Gallery workaround 只是臨時止血**，用戶要求根治時立刻轉 native。
4. **修完 native 要同步重編 dll + 部署所有位置 + 哈希校驗**。
5. **一個 bug 往往帶出連環 bug**（重建漂移、退出定時器）——修復後
完整迴歸自動演示，別隻驗證單點。

