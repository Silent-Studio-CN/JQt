# JQt 架构（权威版）· Architecture (Authoritative)

> 数据基线：**v1.8.0-Emerge-Kit**（2026-09-07）。所有数字均为仓库实测，可复现（见文末命令）。
> English summary: JQt is a **hand-crafted** Qt 6 binding for Java. Controls go through a JNI bridge with an
> explicit handle registry and ownership model; most value objects are **pure Java** reimplementations;
> `QMainWindow` is emulated by a C++ `JQtWindowShell` because Qt's real `QMainWindow` forbids `setLayout`.
> A Rust generator produces the bulk-but-boring API surface; the tiered core (L1/L2) is hand-written.

---

## 1. 分层总览

```mermaid
flowchart TD
  dev(("Java 开发者"))

  subgraph JAVA["Java API 层 · java/org/jqt（157 文件 / 16,189 行）"]
    direction TB
    subgraph JAVA_CORE["手写核心（分级账本 L1/L2）"]
      app["QApplication"]
      mw["QMainWindow（壳模型宿主）"]
      widget["QWidget（基类）"]
      ctl["控件族：QPushButton / QLabel / QLineEdit<br/>QComboBox / QListWidget / QTableView …"]
      lay["布局：QVBox / QHBox / QGrid / QForm / QStacked / QPageLayout"]
      sig["信号回调：onXxx(Consumer …) 处理器列表"]
      facade["分组门面：window() / style() / drag()<br/>focus() / event() / nativeApi()"]
      val["validator 对象体系：QValidator<br/>QInt / QDouble / QRegularExpression"]
    end
    subgraph JAVA_GEN["生成器批（jqt-gen 产出 + 人工精修）"]
      genapi["直传型 API 面（约 2000+ 方法）"]
    end
    subgraph JAVA_VALUE["值对象（多数纯 Java，零 JNI）"]
      vo["QColor / QPalette / QPen / QBrush<br/>QSize / QRect / QPoint …"]
      font["QFont（例外：24 处 native，字体引擎/度量）"]
    end
  end

  subgraph NATIVE["JNI 桥 · native/jqt_bridge.cpp（9,777 行）"]
    reg["句柄注册表：g_handles  id(int64) → QObject*<br/>requireHandle()：失效即抛 IllegalStateException"]
    own["所有权：g_javaOwned · markQtOwned() · Java Cleaner 回收"]
    tramp["信号跳板：C++ signal → JNI → Java Consumer 列表"]
    shell["JQtWindowShell : public QWidget（主窗口壳，L522）"]
  end

  qt["Qt 6.11.2 / 6.8.3（Windows x64/ARM64 · Linux · macOS）"]

  gen["生成器流水线 · tools/jqt-gen（Rust）<br/>main → parse → model → generate → golden"]
  android["JQt-for-Android 变体<br/>template/java/org/jqt（8 文件，AWT-free）"]
  build["构建与交付<br/>build.ps1 · run.ps1 · build-release.ps1 · GitHub Actions（5 jobs）"]

  dev --> JAVA_CORE
  dev --> JAVA_GEN
  ctl -->|"普通控件：JNI"| reg
  app -->|"生命周期/查询：JNI"| reg
  lay --> reg
  val --> reg
  mw -->|"创建/驱动壳"| shell
  shell --> reg
  reg --> own
  reg --> tramp
  tramp -.->|"回调入 Java"| sig
  reg --> qt
  shell --> qt
  vo -.->|"无 JNI"| dev
  gen -->|"生成 + 金标准 diff"| JAVA_GEN
  JAVA_VALUE --> android
  build --> qt
  build --> JAVA
```

## 2. 主窗口壳模型（JQt 最特有的结构）

Qt 的真 `QMainWindow` 不允许 `setLayout()`；JQt 的模型是**「窗口 = 布局宿主」**，
因此 `org.jqt.QMainWindow` 背后不是 Qt 的 `QMainWindow`，而是 C++ 侧自建的壳：

```mermaid
flowchart TD
  jmw["org.jqt.QMainWindow（Java）"]
  nativecreate["JNI: nativeCreate → new JQtWindowShell"]
  shell["JQtWindowShell : public QWidget（L522）"]
  bands["壳内分带布局（自建，非 Qt dock 系统）<br/>menuBar → 工具条（纵叠）→ ［左 dock｜central｜右 dock］→ statusBar"]
  force["setLayout 路由：nativeSetLayoutForce()<br/>（先 delete 旧布局再 setLayout，规避 Qt 的静默忽略）"]
  dock["dock：QWidget 容器 + 四向列表（LEFT/RIGHT/TOP/BOTTOM）"]
  state["结构持久化：QMainWindow.saveState()/restoreState()<br/>（objectName 匹配 + 原子恢复）"]

  jmw --> nativecreate --> shell --> bands
  jmw -->|"setLayout(用户布局)"| force --> bands
  bands --> dock
  jmw --> state
  state -.->|"重排工具条顺序 / 停靠分布"| bands
```

**含义**：`QMainWindow.saveState/restoreState` 不是 Qt 的透传——壳没有 Qt 的 dock 状态机，
我们序列化的是**自身壳结构**（chrome 存在性 + 工具条顺序 + 四向停靠分布）。IDE 级停靠交互
（浮动/拖拽/tabify）属后续 dock 管理器立项。

## 3. 对象生命周期与所有权

```mermaid
flowchart LR
  new["Java: new QXxx()"] --> reg["nativeCreate → registerHandle(obj, javaOwned=true)"]
  reg --> use["使用期：requireHandle(handle) 取回 QObject*"]
  use -->|"句柄失效/已回收"| ise["抛 IllegalStateException（明确报错，不 UB）"]
  reg --> cleaner["Java Cleaner → nativeDispose(handle) → delete（主线程队列）"]
  use -->|"被 Qt 重新父化<br/>setItemWidget / addDockWidget / setParent …"| qtown["markQtOwned(handle)：改由 Qt 析构"]
  qtown --> qtdel["Qt 父对象析构时一并释放"]
```

要点：**谁创建谁负责，重父化即转移**。桥不做猜测式 delete，避免 Qt/Java 双重释放。

## 4. 信号回调路径

```mermaid
sequenceDiagram
  participant Q as Qt 控件/窗口
  participant B as jqt_bridge.cpp
  participant W as Java 控件对象
  participant A as 应用 lambda
  Q->>B: emit signal（如 clicked / textChanged）
  B->>W: nativeHandleXxx(...)（JNI 静态跳板）
  W->>W: 遍历 onXxxHandlers（List<Consumer>）
  W->>A: handler.accept(...)
```
- 首次 `onXxx(handler)` 才建立原生连接（惰性 `nativeConnectXxx`），无监听零成本。
- 回调在 Qt 主线程；Java 侧只做分发，不做线程切换假设。

## 5. 生成器流水线与人工精修

```mermaid
flowchart LR
  fetch["fetch：Qt 头文件/元信息"] --> parse["parse：C++ 声明解析"]
  parse --> model["model：API 模型（含语义黑名单）"]
  model --> generate["generate：Java + JNI 骨架<br/>重载后缀 / 参数命名 / javadoc"]
  generate --> golden["golden：金标准 diff 收敛"]
  golden --> merge["merge-batch.ps1 → 分批入库"]
  merge --> hand["人工精修批（L1/L2：语义别名、壳语义、门面、validator）"]
```
- 生成器负责"多而无聊"的直传面；**分级核心（docs/api-tiering.md 的 L1/L2）由人写**——这是 JQt 的质量护城河。

## 6. 构建与交付

```mermaid
flowchart LR
  b["build.ps1 -QtRoot D:\\Qt\\6.11.2 -Mingw …<br/>javac -h + g++ → out/ + lib/jqt.dll"] --> r["run.ps1 -Class org.jqt.SmokeL1<br/>（冒烟：SmokeL1 / SmokeL2b1 / SmokeL2b2 / SmokeGenApi …）"]
  r --> br["build-release.ps1<br/>jqt-VERSION.jar + jqt-VERSION-windows-x64.zip"]
  b --> ci["GitHub Actions（5 jobs）：<br/>linux · windows · windows-arm64 · macos · release-package"]
  ci --> rel["GitHub Release（tag vX.Y.Z-Codename）"]
  br --> rel
  rel --> central["Maven Central（纯数字版本号）"]
  rel --> jitpack["JitPack（tag 即构建）"]
```
- 版本约定：`1.8.0-Emerge-Kit` = 语义版本 + 代号；**Maven Central 用纯数字 `1.8.0`**。
- 平台矩阵：Qt **6.8.3 LTS** 与 **6.11.2** 双版本 × Windows x64 / Windows ARM64 / Linux / macOS。
- Android 走独立模板（`JQt-for-Android/`），Java 层为 AWT-free 值对象子集（8 文件）。

## 7. 有意为之的"不存在"（避免误读）

| 常被误画成 | 事实 |
|---|---|
| Java 层全部经 JNI 调 Qt | 值对象多为**纯 Java 重实现**（QColor 106 个 public 方法、零 native）；仅 QFont 部分委托 native |
| QMainWindow → JNI → Qt::QMainWindow | 背后是 **`JQtWindowShell : QWidget`**（壳模型，见 §2） |
| JNI 桥只是一个转发盒子 | 桥含**句柄注册表 + 所有权模型 + 信号跳板 + 壳实现**（9,777 行） |
| 全部 API 都由生成器产出 | 生成器只做直传面；**L1/L2 核心手写**（L1 蓝图 178/178 完成） |

## 8. 数字复现命令

```powershell
(Get-ChildItem java/org/jqt -Filter *.java -Recurse).Count                     # 157
(Get-ChildItem java/org/jqt -Filter *.java -Recurse | Get-Content | Measure-Object -Line).Lines   # 16189
(Get-Content native/jqt_bridge.cpp | Measure-Object -Line).Lines               # 9777
(Get-ChildItem tools/jqt-gen/src -Filter *.rs).Name                            # main/parse/model/generate/golden
(Get-ChildItem 'JQt-for-Android/template/java/org/jqt' -Filter *.java).Count   # 8
```
