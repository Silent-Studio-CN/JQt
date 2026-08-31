# 05 · 打包分發與運行時部署

> JQt 應用的打包（jpackage / JDK 模式）與 Qt 運行時部署經驗。
> 覆蓋：JRE 目錄、Qt DLL 位置、插件路徑、SQL 驅動、多位置一致性。

## 1. jpackage 應用鏡像結構（Windows）

```
JQtGallery-Pack/
├── JQtGallery/                 # 免安装版（整个文件夹一起分发）
│   ├── JQtGallery.exe          # jpackage 启动器
│   ├── jqt.dll                 # JQt native 库（必须在 exe 目录！）
│   ├── Qt6*.dll                # Qt 运行时（Core/Gui/Widgets/...）
│   ├── platforms/qwindows.dll  # QPA 平台插件
│   ├── sqldrivers/qsqlite.dll  # SQL 驱动插件
│   ├── app/
│   │   ├── gallery.jar         # 应用 fat jar
│   │   └── JQtGallery.cfg      # jpackage 配置
│   └── src/                    # 源码副本（分发用）
├── lib/gallery.jar             # JDK 模式主程序
└── runtime/                    # JDK 模式 Qt 运行时
```

## 2. 關鍵路徑規則（全是踩出來的）

| 規則 | 原因 
|------|------|
| **JRE 落在 runtime/ 子目錄**（jpackage 自動） | 不要手動把 Qt 放進去 
| **Qt DLL 必須在 exe 目錄** | Windows 加載器按 exe 目錄搜索 
| **jqt.dll 必須在 exe 目錄** | 同上 
| `-Djava.library.path=$APPDIR/..` | $APPDIR 解析到 app/，jqt.dll 在上一級 
| qt.conf `Plugins = .` | 插件根目錄 = exe 目錄 
| **JDK 模式要設 QT_PLUGIN_PATH** | 命令行 java 的應用目錄是 java.exe，qt.conf 不生效 

## 3. 插件路徑的兩套機制（大坑）

JQt 的 SQL 驅動查找：`QCoreApplication::libraryPaths()` + `QLibraryInfo::PluginsPath`。

- **exe 模式**：libraryPaths 含 exe 目錄 + qt.conf 的 Plugins 路徑。
`plugins/sqldrivers/qsqlite.dll` 與 `sqldrivers/qsqlite.dll` **兩處都放**最穩。
- **JDK 模式（start-gallery.bat）**：應用目錄是 java.exe（`C:\...\javapath`），
qt.conf 不生效 → **必須設置 `QT_PLUGIN_PATH=%RT%\plugins` 環境變量**。
否則 QSqlDatabase.addDatabase("QSQLITE") 拋 IllegalStateException
"驅動不可用（SQLITE 未找到 plugins/sqldrivers/qsqlite）"。

```bat
set "RT=%~dp0runtime"
set "QT_QPA_PLATFORM_PLUGIN_PATH=%RT%platforms"
set "QT_PLUGIN_PATH=%RT%plugins"
set "PATH=%RT%;%PATH%"
```

## 4. fat jar 構建

- `jar --update --file gallery.jar -C workdir JQtGallery.class` 替換官方 jar 裏的
demo class（官方 jar 自帶 JQtGallery.class，不替換會跑舊版）。
- 主題模板進 jar：`jar --update --file gallery.jar -C . themes/fluent.qss.tpl`，
程序讀 jar 資源兜底 → exe 版無需外部模板文件。
- 運行 fat jar 時 **classpath 順序**：自己輸出在前（out10;v06.jar;...），
否則官方內置類 shadow 你的類。

## 5. 版本一致性檢查（部署後必做）

PowerShell 哈希比對所有分發位置：

```
gallery.jar 三处一致（lib / pack-lib / pack-app）
jqt.dll 三处一致（runtime / pack / repo-lib）
源码四处一致（本地 / pack-src / staging / repo）
```

**坑**：PowerShell `Copy-Item` 多源文件到一個目錄時偶爾靜默失敗
（只複製第一個）——**逐個 Copy-Item** 並複查哈希，別信一條命令。

## 6. 版本庫配套

- JQt 每版發佈：jqt-X.jar（Java API）+ windows-x64.zip（完整運行時）+ 裸庫
（jqt-windows-6.8.3/6.11.2.dll、arm64、linux .so、macos .dylib）。
- **雙 Qt 版本**（6.8.3 LTS + 6.11.2）：CI 四平臺 × 雙版本全綠才發佈。
- Qt 9+ 新 API 用版本守衛（accessibleIdentifier 等 CI 雙版本編譯斷言）。
- 發佈包包含 plugins/sqldrivers/qsqlite.dll（SQLite 開箱即用）。

