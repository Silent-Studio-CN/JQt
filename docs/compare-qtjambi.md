# JQt vs QtJambi(QtJ)对照

> 目的:把"追赶 QtJambi"从口号变成**可核对的数字与清单**。
> 本文所有数字都给了复现命令/来源,读者可以自己跑一遍核对;没有实测支撑的说法一律标注为"待核实"。

---

## 0. 一句话结论

| | JQt | QtJambi |
|---|---|---|
| 定位 | **手写 + 生成的精选绑定**,面向"Java 桌面小工具/内部工具" | **全量生成绑定**,面向"用 Java 做完整 Qt 应用" |
| 类数 | **161 个 API 类**(另有 30 个冒烟类;jar 内 class 含内部类) | 核心 jar **2,881 个 class**;Maven Central 上 **1,088 个构件**(模块 × 平台原生库) |
| Qt 版本线 | 6.12.0(最新)+ 6.8.3 LTS | 6.12.0(最新)+ 历史各线 |
| 广度差距 | —— | **约 20×**(核心类数比) |
| 优势 | 纯 Java 值对象、显式所有权、精选 API、自带高层控件门面、单端口远程控制台配套 | 模块覆盖近乎完整(QML/Quick、Multimedia、WebEngine、Charts、Bluetooth…)、Maven Central 直发、生态成熟 |

**结论**:JQt 在"版本线"上已经追平(同为 Qt 6.12),差距在**模块广度**;且 JQt 有若干 QtJambi 不提供的取舍与门面,
所以目标不是"变成 QtJambi",而是**把常用模块补齐到能覆盖真实项目**,同时保留自己的取舍。

---

## 1. 硬数据(可复核)

### 1.1 JQt(本机实测,1.9.0-Qt612-Kit)

```powershell
# 规模
(Get-ChildItem java -Recurse -Filter *.java).Count           # 170 个文件(148 API + 22 冒烟)
(Get-Content java\org\jqt\*.java | Measure-Object -Line)     # 16,304 行
(Get-Content native\jqt_bridge.cpp).Count                    # 10,987 行手写 JNI 桥
jar tf dist\jqt-1.9.0-Qt612-Kit.jar | grep -c '\.class$'     # 205 个 class
Select-String java\org\jqt\*.java -Pattern '^\s+public '     # 2,933 处公开成员声明
Select-String java\org\jqt\*.java -Pattern '\bnative\b'      # 1,174 个 native 方法声明
```

| 指标 | 数值 |
|---|---|
| Java 源文件 | 191(API 161 + 冒烟 30) |
| Java 行数 | 17,186 |
| JNI 桥行数 | 11,497 |
| jar 内 class | 205 |
| 公开成员声明 | 2,933 |
| native 方法声明 | 1,217 |
| 信号/事件连接入口 | 44 `nativeConnect*`(8 个兼容空壳)+ 事件过滤器 + 全部经 `jqtConnectOnce()` 去重 |
| Qt 版本线 | 6.12.0 / 6.8.3 LTS |
| CI 平台 | Windows x64 · Windows ARM64 · Linux x64 · macOS x64(各双 Qt 版本) |
| 冒烟 | 10 个(含 20 断言的信号投递确定性回归) |

### 1.2 QtJambi(取自 Maven Central 元数据,可复核)

```bash
curl -s https://repo1.maven.org/maven2/io/qtjambi/ | grep -c 'href="[a-z0-9.-]*/"'   # 1088 个构件
curl -s https://repo1.maven.org/maven2/io/qtjambi/qtjambi/maven-metadata.xml          # 版本线,最新 6.12.0
```

| 指标 | 数值 |
|---|---|
| Maven Central 构件数 | **1,088**(模块 × 平台原生库 × debuginfo) |
| 核心 `qtjambi-6.12.0.jar` | 5.19 MB,**2,881 个 class** |
| `qtjambi-quick-6.12.0.jar` | 167 个 class |
| `qtjambi-multimedia-6.12.0.jar` | 103 个 class |
| `qtjambi-sql-6.12.0.jar` | 42 个 class |
| 最新版本线 | **6.12.0**(与 JQt 同线) |

> 说明:上表 `qtjambi-widgets`、`qtjambi-webengine` 在 6.12.0 下未取到同名构件 —— 它们可能在核心 jar 内或改了命名坐标,**待核实**;
> 这不影响"模块覆盖远超 JQt"的结论(1,088 个构件本身就说明了规模)。

---

## 2. 能力矩阵(逐项实测)

判定方式:在 **Java 源码 + 原生桥** 两侧做内容级检索(`java/org/jqt/*.java` 与 `native/jqt_bridge.cpp`)。
`🌉` = 桥内部用了但**没有暴露成 Java API**(这类是"最低成本的待补项")。

| 能力 | JQt | 说明 |
|---|---|---|
| Core:文件/目录/IO | ✅ | `QFile` `QDir` |
| Core:JSON / 设置 | ✅ | `QJsonObject/Array/Value` `QSettings` |
| Core:正则 / 字符串 | ✅ | `QRegularExpression` `QStringList` `QChar` `QByteArray` |
| Core:日期时间 | ✅ | `QDate` `QTime` `QDateTime` `QTimeZone` |
| Core:环境 / UUID / 版本号 | ✅ | `QProcessEnvironment` `QUuid` `QVersionNumber` |
| Core:**QTimer 定时器** | ✅ | v1.9.1 P0-①:`QTimer`(重复/单次/`singleShot` 跨线程编组) |
| Core:**QThread 线程** | ✅ | v1.9.1 P0-③:`QThread` + `QThreadPool.runAsync`(后台任务 → 主线程编组) |
| Core:**QEvent 事件体系** | ✅ | v1.9.1 P0-②:`QWidget.onEvent` + `QEvent`(鼠标/键盘/滚轮/Resize/Show/Hide/焦点,类型名与 Qt 一致) |
| Gui:绘图 / 画笔 / 画刷 | ✅ | `QPainter` `QPen` `QBrush` `QPainterPath` `QTransform` `QRegion` |
| Gui:图像 / 像素图 / 图标 | ✅ | `QImage` `QPixmap` `QBitmap` `QIcon` `QPicture` `QGlyphRun` `QStaticText` |
| Gui:字体 / 调色板 | ✅ | `QFont*` `QPalette` `QFontMetrics` `QFontInfo` |
| Gui:几何 / 矩阵 / 向量 | ✅ | `QPoint(F)` `QRect(F)` `QSize(F)` `QLine(F)` `QMargins(F)` `QPolygon(F)` `QMatrix4x4` `QVector2D/3D/4D` `QQuaternion` |
| Gui:剪贴板 | ✅ | `QClipboard` |
| Widgets:基础控件 | ✅ | `QPushButton` `QLabel` `QLineEdit` `QCheckBox` `QRadioButton` `QComboBox` `QSlider` `QSpinBox` `QDial` `QProgressBar` `QDateTimeEdit` `QTextEdit` `QGroupBox` `QFrame` |
| Widgets:列表 / 树 / 表 | ✅ | `QListWidget` `QTreeWidget` `QTableWidget` `QListView` `QModelIndex` |
| Widgets:布局 | ✅ | 盒式/网格/表单/堆叠 |
| Widgets:菜单 / 工具栏 / 状态栏 | ✅ | `QMenu` `QMenuBar` `QToolBar` `QStatusBar` `QAction` |
| Widgets:对话框 | ✅ | `QFileDialog` `QMessageBox` `QColorDialog` `QFontDialog` `QInputDialog` |
| Widgets:滚动 / 分割 / 堆叠 / 画布 | ✅ | `QScrollArea` `QSplitter` `QStackedWidget` `QCanvasWidget` |
| OpenGL | ✅ | `QOpenGLWidget` |
| 动画 | ✅ | `QPropertyAnimation` + JQt 动画门面(`JQtAnimation(s)` `JQtEasing` `JQtAnimationTheme`) |
| Sql | ✅ | `QSqlDatabase` `QSqlQuery` + **`QSqlQueryModel` / `QSqlTableModel`**(v1.9.1 P1:只读查询模型与可写表模型,支持显式绑定连接) |
| PrintSupport | ✅ | `QPrinter` `QPageSize` `QPageLayout` `QPageRanges` |
| SerialPort | ✅ | `QSerialPort`(CI 单独构建该模块) |
| **QStateMachine 状态机** | ⛔ 上游受阻 | 本 SDK 路径下**拿不到 QtStateMachine**(无 DLL/无 cmake 配置,在线仓库也无该 addon);需 Qt 官方安装器勾选 "Qt State Machine" 才能实现 —— 已记录待上游模块可得再做 |
| **QSvg 矢量渲染** | ✅ | v1.9.1 P0-④:`QSvgRenderer`(文件/字节 → `renderToPng`;缺 qtsvg 时构建特性探测自动跳过) |
| **QNetwork 网络** | ✅ | v1.9.1 P0-⑤:`QNetworkAccessManager`(GET/POST + 管理器级 `onFinished` + 超时/UA);`QTcpSocket`/`QUdpSocket` 仍无 |
| **QML / Quick** | 🟡 | v1.9.1 P2:`QQuickView`(加载 QML 文件/内联文本 + `grabToPng()` 离屏渲染,像素级验证通过);`QQmlApplicationEngine`/`QQuickWidget`/QML↔Java 互操作未做 |
| **Multimedia** | 🟡 | v1.9.1 P2:`QMediaPlayer`(播放/暂停/停止/跳转/时长/位置/状态/错误信号)+ `QAudioOutput`(音量/静音);**已验证**:素材时长解析、状态机迁移、错误路径(时长 300ms 精确、缺失文件触发 "Could not open file")。`QVideoWidget`/摄像头/录制未做;真实出声依赖音频设备 |
| **WebEngine / WebView** | ❌ | |
| **Charts** | ✅ | v1.9.1 P2:`QChart` / `QLineSeries` / `QChartView`(含 `toPng()` 离屏渲染);`JQT_HAVE_CHARTS` 特性探测 |
| **3D** | ❌ | |
| **Positioning** | 🟡 | v1.9.1 P2:`QGeoCoordinate`(大圆距离/方位角/`atDistanceAndAzimuth` 推算/6 种格式化);**纯几何计算,无需 GPS 即可确定性使用**。`QGeoPositionInfoSource`(真实定位)与 Bluetooth/NFC 未做 |
| **WebSockets / WebChannel** | 🟡 | v1.9.1 P1:**WebSocket 客户端已支持**(`QWebSocket`:连接/文本/二进制/错误/关闭,`JQT_HAVE_WEBSOCKETS` 特性探测);WebChannel 仍无 |
| **DBus** | 🌉 | 桥内部出现,未暴露 |
| **Concurrent** | ❌ | |
| **Designer 插件** | ❌ | |
| **Help** | ❌ | |
| **QTest** | ✅ | v1.9.1 P1:`JQtTest` 兼容层(QVERIFY/QVERIFY2/QCOMPARE/QFAIL/QSKIP + test*/init/cleanup 运行器,零原生依赖) |

**汇总(v1.9.1 进展)**:36 个能力点中 **JQt 命中 28 个**(v1.9.1 新增 QTimer / QThread / QEvent / QSvg / QNetwork),
**7 项仍缺失**(另 1 项上游受阻、`DBus` 属"桥内有、API 无")。命中率 **28/36 ≈ 78%**。
**P0 五项已全部完成**(QTimer ✅  QEvent ✅  QThread+并发子集 ✅  QSvg ✅  QNetwork 子集 ✅);**P1 完成**:SQL 模型 ✅、QWebSocket 客户端 ✅、QTest 兼容层 ✅(QStateMachine 因上游 SDK 未提供模块而受阻,已记录);**P2 进行中**:Charts ✅、Multimedia 🟡、QML/Quick 🟡、Positioning 🟡;**Designer 明确跳过**(Qt Designer 是 C++ 插件 API,Java 侧无插件宿主,价值极低);Help 待评估。

---

## 3. 设计取舍对比(不只看数量)

| 维度 | JQt | QtJambi |
|---|---|---|
| 对象模型 | 句柄注册表 + 显式所有权(`g_javaOwned` / Cleaner),重父化即转移 | 自有对象模型,`dispose()` 显式释放,可配置 GC 介入 |
| 值对象 | **纯 Java 实现**(QColor/QPen/QRect… 零 native 调用) | 多为原生对象包装 |
| 桥的形态 | 手写 10,987 行 + 生成器辅助(44 个信号入口手工精修) | 全量生成(数千类) |
| API 风格 | 精选 + 高层门面(标题栏/导航/开关/信息条/全局热键/拖拽) | 贴近 C++ 的原样映射 |
| 线程/主线程 | 回调在 Qt 主线程;macOS 需 `-XstartOnFirstThread`(已写进 `run-macos.sh`) | 同样受 Qt 主线程约束,官方文档有说明 |
| 分发 | GitHub Release + JitPack(代号版);Central 停在 0.7.5 | **Maven Central 全量直发**(1,088 构件) |
| 平台 | Windows x64/ARM64、Linux x64、macOS x64(CI 全绿);Android 目录为实验 | 覆盖更广(含 Android 等) |
| 构建 | PowerShell + MinGW + gradle(部分) | CMake + gradle |
| 许可 | LGPL-3.0 | LGPL 2.1/3 + 商业授权(以官方为准) |

**JQt 的不可替代点**(QtJambi 不提供):纯 Java 值对象带来的零 native 开销、`JQtTitleBar/JQtNavigation/JQtPivot/JQtSwitch/JQtInfoBar` 这类开箱门面、
以及"同一套东西还能当远程控制端"的配套(`SilentRemoveKit` 控制台 + 网页终端 + 单端口复用)。
**QtJambi 的不可替代点**:模块广度、Maven Central 直发、社区与文档体量、QML/Quick 与多媒体生态。

---

## 4. 追赶路线(按性价比排序)

### P0 —— 低成本高收益(桥里已有底子,主要是"暴露 + 测试")

| 项 | 为什么先做 | 预估 |
|---|---|---|
| **QTimer** ✅ 已完成(v1.9.1) | 桥里已在用(主题轮询);Java 侧没有 → 大量"定时刷新"需求只能绕 | 0.5 天 |
| **QEvent 事件体系** ✅ 已完成(v1.9.1) | 事件过滤器 → Java 回调;键盘/鼠标/滚轮/Resize 全通 | 1–2 天 |
| **QThread + QtConcurrent 子集** ✅ 已完成(v1.9.1) | `QThread` + `QThreadPool.runAsync`;跨线程编组用 `QTimer.singleShot` | 1–2 天 |
| **QSvgRenderer** ✅ 已完成(v1.9.1) | 文件/字节 → `renderToPng`;缺 qtsvg 的平台自动降级(isAvailable=false) | 0.5 天 |
| **QNetworkAccessManager 子集** ✅ 已完成(v1.9.1) | HTTP GET/POST + 信号回调;"下载/上报"类需求的高频刚需 | 2–3 天 |

### P1 —— 补齐常用模块

| 项 | 说明 |
|---|---|
| **QStateMachine** | 状态机;比手写 if-else 稳 |
| **QWebSocket(客户端)** | ✅ 已完成(v1.9.1 P1) |
| **DBus(暴露已有桥能力)** | 桥里已有,补 Java API 即可 |
| **QSqlQueryModel/QSqlTableModel** | 现在只有 `QSqlDatabase/QSqlQuery`,做表格应用要手写映射 |
| **QTest 兼容层** | ✅ 已完成(v1.9.1 P1) |

### P2 —— 长期/按需(投入大,先不做)

`QML/Quick`、`Multimedia`、`WebEngine`、`Charts`、`3D`、`Bluetooth/NFC/Positioning`、`Designer`、`Help`。
这些要么需要额外 Qt 模块与平台依赖,要么与"精选绑定"的定位冲突 —— **按真实项目需求驱动**,不做面子工程。

### 度量方式(每个版本都要给数字)

1. **能力点命中率**:用第 2 节的 36 项清单,发布说明里给 `命中/总数`(本轮 **16/36**)
2. **类数与 native 方法数**:`jar tf | grep -c class`、`grep -c native`
3. **相对 QtJambi 的对比表**:本文档随每个版本更新,差异项只减不增

---

## 5. 复现本文所有数字

```powershell
# JQt 侧
(Get-ChildItem java -Recurse -Filter *.java).Count
(Get-Content native\jqt_bridge.cpp).Count
& "$env:JAVA_HOME\bin\jar.exe" tf dist\jqt-1.9.0-Qt612-Kit.jar | Select-String '\.class$' | Measure-Object
Select-String java\org\jqt\*.java -Pattern '\bnative\b' | Measure-Object

# QtJambi 侧(Maven Central)
curl.exe -s https://repo1.maven.org/maven2/io/qtjambi/ | Select-String 'href="[a-z0-9.-]+/"' | Measure-Object
curl.exe -s https://repo1.maven.org/maven2/io/qtjambi/qtjambi/maven-metadata.xml | Select-String '<version>'
curl.exe -sL -o qtjambi-6.12.0.jar https://repo1.maven.org/maven2/io/qtjambi/qtjambi/6.12.0/qtjambi-6.12.0.jar
& "$env:JAVA_HOME\bin\jar.exe" tf qtjambi-6.12.0.jar | Select-String '\.class$' | Measure-Object
```

**来源**:QtJambi 版本线与构件清单取自 Maven Central 元数据(权威、可复核);
QtJambi 功能/平台特性以其官网 [qtjambi.io](https://www.qtjambi.io/) 与仓库 [OmixVisualization/qtjambi](https://github.com/OmixVisualization/qtjambi) 为准(本文未能直接抓取其页面,相关表述标注为待核实)。

---

*本文档随版本更新;数字有疑问请用第 5 节命令自行复现 —— 不复现的说法不进本文。*
