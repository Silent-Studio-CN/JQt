# 06 · 社區工程約定與發佈流程

> JQt 社區（Community/ 目錄）的工程約定與發佈協作經驗。
> 包括 JQtGallery 的版本歸檔、自動演示、發佈說明格式、跟隨更新策略。

## 1. 社區目錄版本歸檔約定（用戶欽定格式）

```
Community/JQtGallery/
├── JQtGallery.java        # 根目录 = 最新版本（跟随当前 release）
├── README.md
├── NordTheme.java / SolarizedTheme.java / TerminalTheme.java   # 主题包（版本无关）
└── v5.0/                  # 旧版本归档子目录
    ├── JQtGallery.java
    └── README.md
```

- **根目錄放最新版，舊版進 vX.Y/ 子目錄**（從 v0.6.0 起執行）。
- README 用表格記錄每個歸檔版本兼容的 JQt 版本。
- 主題類（NordTheme 等）版本無關，只需一份。

## 2. 自動演示模式（-Dg.auto=1）

JQtGallery 內置自動化演示：`app.schedule` 順序觸發各分區按鈕點擊驗證：

- 每個新分區按鈕用註冊函數（v6btn/v61btn/v7btn/v74btn/v75btn）登記進列表，
auto 模式逐個 `bb.click()` + 日誌（`自动点击: 按钮名`）。
- **彈窗類按鈕不進自動列表**（模態 exec 會卡死演示）：
QDialog.exec / QMessageBox.exec / QInputDialog.getText / 寫註冊表的自啓。
- 分區切換也自動：`pivot.setCurrentIndex(n)` + "分區切換觸發"日誌。
- 收尾切回 v0.6 分區（復現用戶手動切換路徑，防佈局迴歸）。
- 通過標準：`EXIT=0` + 日誌行數穩定 + 無"點擊異常"。

## 3. 跟隨更新策略（每次 release 必做清單）

1. 查 GitHub releases（api.github.com，注意 tag 名）。
2. 讀 release notes（body），列出新類/新 API。
3. 下載 jar + windows-x64.zip（或本地倉庫 dist/ 產物）。
4. javap 新類確認簽名（寫代碼前必查）。
5. Gallery 加新分區（pivot + 面板 + 按鈕註冊 + auto 點擊）。
6. 編譯 + 自動演示全綠。
7. 重建 fat jar + 部署（lib/pack/runtime 三處 + 哈希校驗）。
8. 更新 README（版本號 + 分區說明 + 構建命令）。
9. commit + push（push 失敗重試最多 8 次，網絡抖動是常態）。

已跟進版本：v0.6（L1 API）→ v0.6.1（Exclusive Kit）→ v0.7.0~0.7.2
（Universal Kit：QPrinter/QSql/QAction/QListView/QClipboard 圖像）→
v0.7.3/0.7.4（QOpenGLWidget/QSerialPort）→ v0.7.5（60 值類型類）。

## 4. 發佈說明三段式格式（用戶欽定）

發佈說明必須精簡三段：

1. **本次更新增加** —— 新類/新 API/新能力清單
2. **已修復（詳細）** —— bug 修復逐條
3. **感謝社區貢獻（詳細）** —— 外部貢獻者（如 QraftLab）

示例結構（v0.7.2）：
```
# JQt v0.7.2-Universal-Kit — 工业模块：QPrinter + QSql
> QtPrintSupport / Qt6Sql 首次 API 化
## QPrinter（打印/PDF）
- QPrinter 类：setOutputFormat(NATIVE/PDF)、setPageSize(A4/A3/...)
- QTextEdit.print(QPrinter) / printToPdf(path)
## QSql（数据库）
- QSqlDatabase：addDatabase(SQLITE/PSQL/MYSQL)、exec、lastError
- 驱动插件 workaround + 可用性预检
## 验证
- SmokeV072 四平台 CI 全绿
## 资产
- 各平台 jar/zip/裸库列表
```

## 5. 版本命名與分發渠道

- 版本命名：`0.7.5` 數字版本 + `-Generator-Kit` 發佈代號。
- 三渠道同一份代碼/產物：
- GitHub Releases（主渠道）
- Maven Central：`io.github.silent-xiaomiao:jqt:0.7.5`（數字版本）
- JitPack：`com.github.Silent-Studio-CN:JQt:0.7.5-Generator-Kit`
- GitHub Releases 傳 Java 17（--release 17）主 jar + 完整包（每版資產見 Releases 頁）；
  官網 jqt.silentstudio.cn 已上線（文件/下載按版本歸檔）。

## 6. 測試文化（JQt 質量承諾）

- **機器生產，人只做精修**——每個生成的 API 都經過：
編譯斷言（javac）+ 運行時冒煙（Smoke* 系列）四平臺 CI。
- 冒煙類命名：SmokeL1/SmokeL1b2、SmokeExclusive、SmokeV072、
SmokeV073、SmokeV074、SmokeGenApi 16/16、SmokeInputDialog 13/13、
SmokeSqlDb 10/10。
- dll 導出 mangle 計數是質量指標：369 → 8（剩餘爲 Java 無聲明的無害孤兒）。
- 崩潰日誌：jqt-crash.log（SEH handler）+ hs_err（CI 捕獲 native frames）。

## 7. 用戶驅動的迭代教訓

- 用戶反饋"點按鈕沒反應" → 先查**窗口尺寸/佈局漂移**，再懷疑按鈕邏輯。
- 用戶反饋"切主題有殘留" → 查**內聯樣式/控件級樣式/硬編碼顏色**三件套。
- 用戶反饋退出碼 -1 → onClose + shutdown hook 打點定位（見 04 章）。
- 觸摸屏環境的自動點擊事件：先驗證程序邏輯是否有觸發路徑（grep onClicked），
不要輕易歸因硬件——用戶會介意。


