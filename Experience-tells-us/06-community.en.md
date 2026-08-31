# 06. Community Project Agreement and Release Process

> Engineering conventions and release collaboration experience of the JQt Community (Community/ directory).
> Including version archiving of JQtGallery, automatic demonstrations, release note formats, and follow-up update strategies.

## Community Directory Version Archiving Agreement (User-Specified Format)

```
Community/JQtGallery/
├── JQtGallery.java        # 根目录 = 最新版本（跟随当前 release）
├── README.md
├── NordTheme.java / SolarizedTheme.java / TerminalTheme.java   # 主题包（版本无关）
└── v5.0/                  # 旧版本归档子目录
    ├── JQtGallery.java
    └── README.md
```

- **Place the latest version in the root directory and the old version in the vX.Y/ subdirectory**(Implemented from v0.6.0).
- The README uses a table to record the JQt versions compatible with each archive version.
- The version of the theme class (such as NordTheme, etc.) is irrelevant. Just one copy is needed.

## 2. auto Demonstration Mode (-Dg.auto=1)

Built-in automated demonstration of JQtGallery`app.schedule` Trigger the button clicks of each partition in sequence for verification

- Each new partition button with registration function (v6btn/v61btn/v7btn/v74btn/v75btn) registration into the list,
auto mode one by one `bb.click()` + Log`自动点击: 按钮名`"
- **Pop-up buttons do not enter the automatic list**(Demonstration of modal exec Freezing) :
QDialog.exec/QMessageBox.exec/QInputDialog.getText/Write the auto-start of the registry.
- Partition switching is also automatic`pivot.setCurrentIndex(n)` + "Partition Switch Trigger" log.
- At the end, switch back to the v0.6 partition (reproduce the user's manual path switching to prevent layout regression).
- Pass the standard`EXIT=0` The number of log lines is stable and there are no "click anomalies".

## 3. Follow Update Strategy (Must-do list for each release)

1. Check GitHub releases (api.github.com, note the tag name).
2. Read the release notes (body) to list the new classes/new apis.
3. Download jar + windows-x64.zip (or local repository dist/ product).
4. javap new class confirmation signature (must be checked before writing code).
5. Add a new partition to Gallery (pivot + panel + button registration + auto click).
6. Compilation + automatic demonstration all green.
7. Rebuild the fat jar + deployment (three locations: lib/pack/runtime + hash verification).
8. Update the README (version number + partition description + build command).
9. commit + push (push failure can be retried up to 8 times, and network jitter is the norm).

Followed versions: v0.6 (L1 API) → v0.6.1 (Exclusive Kit) → v0.7.0-0.7.2
(Universal Kit: QPrinter QSql/QAction/QListView/QClipboard image) - >
V0.7.3/0.7.4 (QOpenGLWidget/QSerialPort) - > v0.7.5 60 value type (class).

## 4. Three-part format for release instructions (specified by the user)

The release note must be condensed into three paragraphs:

1. **This update adds** -- List of New classes/New apis/New capabilities
2. **Fixed (Details)** -- bug fixes one by one
3. **Thank you for the community's contribution (details)** -- External contributors (such as QraftLab)

Example structure (v0.7.2) :
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

## 5. Version naming and distribution channels

- Version naming`0.7.5` Digital version + `-Generator-Kit` Release code name.
- The same code/product for three channels:
- GitHub Releases (main channel)
- Maven Central`io.github.silent-xiaomiao:jqt:0.7.5`(Digital version
- JitPack:`com.github.Silent-Studio-CN:JQt:0.7.5-Generator-Kit`
- GitHub Releases only upload the JDK26 main jar + complete package; Old version jar storage
`jqt.silentstudio.cn/releases`(The site has not been launched yet. The README shows Coming Soon.)

## 6. Testing Culture (JQt Quality Commitment)

- **Machines produce, while humans only do fine finishing**-- Each generated API goes through:
Compilation assertion (javac) + runtime Smoke (Smoke* series) four-platform CI.
- Smoking class naming: SmokeL1/SmokeL1b2, SmokeExclusive, SmokeV072
SmokeV073, SmokeV074, SmokeGenApi 16/16, SmokeInputDialog 13/13
SmokeSqlDb 10/10.
- The quality index of the dll export mangle count is: 369 → 8 (the remaining ones are harmless orphans without declaration in Java).
- Crash log: jqt-crash.log (SEH handler) + hs_err (CI captures native frames).

## 7. Lessons learned from user-driven iterations

- User feedback: "Clicking the button doesn't respond" → Check first**Window size/layout drift**Doubt the logic of the button again.
- User feedback: "There are remnants of topic cuts" → Check**Inline styles/control-level styles/hard-coded colors**Three-piece set.
- User feedback exit code -1 → onClose + shutdown hook dot location (see Chapter 04).
- Automatic click event in touchscreen environment: First verify whether the program logic has a trigger path (grep onClicked),
Don't attribute it to hardware easily - users will mind.

