# Experience Tells Us — JQt 開發經驗與踩坑實錄

> 本目錄沉澱 JQt（Java bindings for Qt）開發過程中積累的經驗教訓。
> 內容聚焦** JQt 開發本身**：Java API 設計、JNI/native 橋接、Qt 行爲陷阱、
> Windows 平臺特性、主題渲染、打包分發、社區工程約定。
> 由參與 JQt 開發的 AI 工程方向成員撰寫，隨版本演進持續補充。

## 我的主要負責方向

1. **JQtGallery 社區演示工程**（Community/JQtGallery）
- 功能分區演示（主題/控件/動畫/窗口/v0.5~v0.7.5 各版本新 API）
- 自動演示模式（-Dg.auto=1 逐個點擊驗證）與探針測試
- 跟隨每個 release 更新：v0.6 → v0.6.1 → v0.7.0~v0.7.5 全部跟進
2. **JQt native 層 Windows 平臺問題排查與修復**
- setFrameless 熱切換失效（Win32 樣式位 + DWM 擴展邊框）—— 已根治
- 窗口重建/佈局漂移、固定尺寸約束、觸摸合成事件座標
3. **主題渲染系統**
- fluent.qss.tpl 模板 + 變量表渲染機制
- 雙主題（淺/深）切換、強調色動態變量
4. **打包與分發**
- jpackage 應用鏡像、Qt 運行時部署、插件路徑（qt.conf / QT_PLUGIN_PATH）
- 多位置部署一致性校驗
5. **社區協作與發佈**
- 版本歸檔約定（根目錄=最新 + vX.Y/ 子目錄）
- 發佈說明三段式格式、測試報告

## 文檔索引

| 文件 | 主題 |
|------|------|
| [01-window-native.md](01-window-native.md) | Win32 窗口系統與 native 層（setFrameless 大坑全解）
| [02-theme-qss.md](02-theme-qss.md) | 主題渲染與 QSS（模板變量/優先級/殘留）
| [03-java-api.md](03-java-api.md) | Java API 設計與使用陷阱
| [04-lifecycle-threads.md](04-lifecycle-threads.md) | 對象生命週期、線程、信號回調
| [05-packaging.md](05-packaging.md) | 打包分發與運行時部署
| [06-community.md](06-community.md) | 社區工程約定與發佈流程
| [07-probes.md](07-probes.md) | 探針測試方法論（復現 native 問題）
| [08-setFrameless-case.md](08-setFrameless-case.md) | setFrameless 修復全記錄（native 排查範例）

