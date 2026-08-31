# 07 · 探針測試方法論（復現與驗證 native 問題）

> JQt native 層問題（崩潰/樣式/座標）在開發環境難復現時的系統排查方法。
> 核心思路：**最小探針 + 時間軸調度 + 外部可觀測信號**。

## 爲什麼需要探針

- 開發環境與真實用戶環境隔離（DSH 後臺 vs 交互桌面），EnumWindows/
FindWindow 可能拿不到窗口。
- 自動演示（-Dg.auto=1）只覆蓋"點擊路徑"，覆蓋不到"重建後交互"、
"模態框關閉"、"退出清理"等邊界。
- 用戶現場才能復現的 bug（exit -1），需要可帶回現場的診斷探針。

## 1. 最小探針模板（Java + schedule 時間軸）

```java
public class XxxProbe {
    static QApplication app;
    static QMainWindow w;
    public static void main(String[] args) {
        app = new QApplication();
        w = new QMainWindow("XxxProbe", 800, 500);
        w.setFrameless(true);
        w.setFixedSize(800, 500);
        w.show();
        System.out.println("P1 started");
        app.schedule(() -> { ... }, 2000);   // STEP1
        app.schedule(() -> { ... }, 3000);   // STEP2
        app.schedule(() -> { System.out.println("DONE"); app.quit(); }, 5000);
        app.exec();
        System.out.println("P exec returned normally");
    }
}
```

要點：每步 System.out 打點；結束顯式 quit；Start-Process 重定向 stdout/stderr
到文件；超時判斷是否卡死。

## 2. 已用探針清單（可複用）

| 探針 | 驗證內容 | 結論 
|------|---------|------|
| FrameProbe | setFrameless 熱切換是否生效 | 樣式位 0x86CE0000（有邊框）✓ 
| SizeProbe | 原生邊框模式下 setFixedSize 是否約束 | resize(1200,900) 被彈回 800x500 ✓ 
| GlCrashProbe | 窗口重建後 QOpenGLWidget update/close | 正常（排除 GL 崩潰） 
| TimerProbe | scheduleGeo 遞歸 + close 是否崩潰 | 正常（DSH 下不復現 -1） 
| ModalProbe | QDialog.exec + reject + getText 關閉路徑 | 正常退出 0 
| FullProbe | 全分區切換 + onClose 路徑 | 正常退出 0 

教訓：**全部探針都返回 0，唯獨用戶現場 -1** —— 說明差異在環境
（觸摸屏合成事件 + 真實交互序列），此時用診斷日誌（onClose/shutdown hook）
帶到現場定位，而不是繼續猜。

## 3. 外部觀測手段

- **Win32 樣式位**：GetWindowLongPtrW(GWL_STYLE) 打點（native 層 fprintf），
驗證 WS_CAPTION/WS_THICKFRAME 是否真的變化 —— 最客觀。
- **窗口枚舉**：EnumWindows + GetWindowThreadProcessId 按 pid 過濾，
但**受 desktop 隔離影響**（後臺進程的窗口在另一個 desktop 看不到）。
- **GetWindowRect vs GetClientRect**：原生邊框窗口兩者不同，無邊框相同。
- **截圖**：CopyFromScreen（同樣受 desktop 隔離限制）。
- **JVM 診斷**：onClose 打點 + Runtime.addShutdownHook 打點，
判斷退出路徑是正常還是崩潰。

## 4. 網絡受限環境的下載策略（與 JQt 開發相關部分）

- GitHub release 大文件（zip 16-20MB）用 IWR 重試循環（10-12 次，
間隔 8s），中途可能斷點續傳式增長。
- 只取 zip 中單個 dll 時用 Range 請求 + zlib inflate（本地解析 central
directory），避免下載整個包。
- 本地倉庫 dist/ 通常已有最新構建產物（jar/dll），優先複製本地。

## 5. 迴歸紀律

1. 每個修復後跑**完整自動演示**（全部分區 + 收尾切回），記 EXIT 和日誌行數基線。
2. 修復不能只驗證"不崩"——要驗證"行爲對"（樣式位、渲染色值、回調計數）。
3. 部署後哈希校驗三處一致性（jar/dll/源碼）。
4. 發佈前先 push 到本地倉庫再推 GitHub（網絡抖動常態，push 重試最多 8 次）。

