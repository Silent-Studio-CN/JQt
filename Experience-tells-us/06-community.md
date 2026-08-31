# 06 · 社区工程约定与发布流程

> JQt 社区（Community/ 目录）的工程约定与发布协作经验。
> 包括 JQtGallery 的版本归档、自动演示、发布说明格式、跟随更新策略。

## 1. 社区目录版本归档约定（用户钦定格式）

```
Community/JQtGallery/
├── JQtGallery.java        # 根目录 = 最新版本（跟随当前 release）
├── README.md
├── NordTheme.java / SolarizedTheme.java / TerminalTheme.java   # 主题包（版本无关）
└── v5.0/                  # 旧版本归档子目录
    ├── JQtGallery.java
    └── README.md
```

- **根目录放最新版，旧版进 vX.Y/ 子目录**（从 v0.6.0 起执行）。
- README 用表格记录每个归档版本兼容的 JQt 版本。
- 主题类（NordTheme 等）版本无关，只需一份。

## 2. 自动演示模式（-Dg.auto=1）

JQtGallery 内置自动化演示：`app.schedule` 顺序触发各分区按钮点击验证：

- 每个新分区按钮用注册函数（v6btn/v61btn/v7btn/v74btn/v75btn）登记进列表，
  auto 模式逐个 `bb.click()` + 日志（`自动点击: 按钮名`）。
- **弹窗类按钮不进自动列表**（模态 exec 会卡死演示）：
  QDialog.exec / QMessageBox.exec / QInputDialog.getText / 写注册表的自启。
- 分区切换也自动：`pivot.setCurrentIndex(n)` + "分区切换触发"日志。
- 收尾切回 v0.6 分区（复现用户手动切换路径，防布局回归）。
- 通过标准：`EXIT=0` + 日志行数稳定 + 无"点击异常"。

## 3. 跟随更新策略（每次 release 必做清单）

1. 查 GitHub releases（api.github.com，注意 tag 名）。
2. 读 release notes（body），列出新类/新 API。
3. 下载 jar + windows-x64.zip（或本地仓库 dist/ 产物）。
4. javap 新类确认签名（写代码前必查）。
5. Gallery 加新分区（pivot + 面板 + 按钮注册 + auto 点击）。
6. 编译 + 自动演示全绿。
7. 重建 fat jar + 部署（lib/pack/runtime 三处 + 哈希校验）。
8. 更新 README（版本号 + 分区说明 + 构建命令）。
9. commit + push（push 失败重试最多 8 次，网络抖动是常态）。

已跟进版本：v0.6（L1 API）→ v0.6.1（Exclusive Kit）→ v0.7.0~0.7.2
（Universal Kit：QPrinter/QSql/QAction/QListView/QClipboard 图像）→
v0.7.3/0.7.4（QOpenGLWidget/QSerialPort）→ v0.7.5（60 值类型类）。

## 4. 发布说明三段式格式（用户钦定）

发布说明必须精简三段：

1. **本次更新增加** —— 新类/新 API/新能力清单
2. **已修复（详细）** —— bug 修复逐条
3. **感谢社区贡献（详细）** —— 外部贡献者（如 QraftLab）

示例结构（v0.7.2）：
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

## 5. 版本命名与分发渠道

- 版本命名：`0.7.5` 数字版本 + `-Generator-Kit` 发布代号。
- 三渠道同一份代码/产物：
  - GitHub Releases（主渠道）
  - Maven Central：`io.github.silent-xiaomiao:jqt:0.7.5`（数字版本）
  - JitPack：`com.github.Silent-Studio-CN:JQt:0.7.5-Generator-Kit`
- GitHub Releases 只传 JDK26 主 jar + 完整包；旧版本 jar 放
  `jqt.silentstudio.cn/releases`（站点尚未上线，README 显示 Coming Soon）。

## 6. 测试文化（JQt 质量承诺）

- **机器生产，人只做精修**——每个生成的 API 都经过：
  编译断言（javac）+ 运行时冒烟（Smoke* 系列）四平台 CI。
- 冒烟类命名：SmokeL1/SmokeL1b2、SmokeExclusive、SmokeV072、
  SmokeV073、SmokeV074、SmokeGenApi 16/16、SmokeInputDialog 13/13、
  SmokeSqlDb 10/10。
- dll 导出 mangle 计数是质量指标：369 → 8（剩余为 Java 无声明的无害孤儿）。
- 崩溃日志：jqt-crash.log（SEH handler）+ hs_err（CI 捕获 native frames）。

## 7. 用户驱动的迭代教训

- 用户反馈"点按钮没反应" → 先查**窗口尺寸/布局漂移**，再怀疑按钮逻辑。
- 用户反馈"切主题有残留" → 查**内联样式/控件级样式/硬编码颜色**三件套。
- 用户反馈退出码 -1 → onClose + shutdown hook 打点定位（见 04 章）。
- 触摸屏环境的自动点击事件：先验证程序逻辑是否有触发路径（grep onClicked），
  不要轻易归因硬件——用户会介意。
