# JQt — Java 绑定 Qt 框架 / Java Bindings for Qt

> 让 Java 程序员能用 Qt 写桌面应用：Java 写业务逻辑，底层渲染和事件由 Qt（C++）完成。
> Java developers write desktop apps with Qt: Java for the logic, Qt (C++) for rendering and events.

<details>
<summary>🌐 语言 / Language</summary>

- [中文版](#zh) · [English Version](#en)

</details>

---

<a id="zh"></a>
## 中文版

### 架构（三层结构）

- **Java 层**：你写的代码（`JQtButton btn = new JQtButton("点我")`）
- **JNI 胶水层**：`native/jqt_bridge.cpp`，把 Java 调用翻译成 Qt 调用，并把 Qt 信号回调回 Java
- **Qt 底层**：Qt 6.11.2（mingw_64 kit）

### 当前进度（路线图）

| 阶段 | 内容 | 状态 |
|------|------|------|
| Phase 0 | JNI + Qt 最小 Demo（Java 弹出 Qt 窗口） | ✅ 完成 |
| Phase 1 | QApplication / QWidget / QPushButton / QLabel | ✅ 完成 |
| Phase 2 | 信号槽（点击/按下/释放/勾选、窗口事件、退出前回调） | ✅ 完成 |
| Phase 3 | 布局管理器（QVBoxLayout / QHBoxLayout + Stretch） | ✅ 完成 |
| Phase 4 | 更多控件（QLineEdit / QComboBox / QListWidget） | ⬜ 未开始 |
| Phase 5 | 内存管理优化、异常处理 | ⬜ 未开始 |
| Phase 6 | Windows / Linux 跨平台编译 | ⬜ 未开始（Windows 已通） |
| Phase 7 | Alpha 发布 | ⬜ 未开始 |

### 项目结构

```
JQt - Dev/
├── java/org/jqt/          # Java API 层
│   ├── JQtApplication.java  # QApplication 封装（exec/quit）
│   ├── JQtWidget.java       # 控件基类（持有 nativeHandle 指针）
│   ├── JQtWindow.java       # 窗口（show/hide/addWidget/onClose）
│   ├── JQtButton.java       # 按钮（onClick/onPressed/onReleased/onToggled）
│   ├── JQtLabel.java        # 标签（setText）
│   ├── JQtLayout.java       # 布局基类（addWidget/setSpacing/addStretch）
│   ├── JQtVBoxLayout.java   # 垂直布局（QVBoxLayout）
│   ├── JQtHBoxLayout.java   # 水平布局（QHBoxLayout）
│   └── JQtDemo.java         # 演示程序
├── native/
│   ├── jqt_bridge.cpp       # JNI 胶水层（C++ 包装层）
│   └── generated/           # javac -h 生成的 JNI 头（构建时生成）
├── build.ps1              # 一键构建（Java + C++ + Qt 部署）
├── run.ps1                # 运行演示
├── LICENSE.md             # JSL-1.0 分层许可（双语）
├── CONTRIBUTING.md        # 贡献政策（双语）
├── THIRD-PARTY-NOTICES.md # Qt LGPLv3 第三方声明（双语）
├── COMMERCIAL.md          # 商业许可说明（双语）
├── out/                   # Java 字节码（构建产物）
└── lib/                   # jqt.dll + Qt 运行时（构建产物，可整体分发）
```

### 快速开始

```powershell
.uild.ps1        # 编译 Java、生成 JNI 头、编译 jqt.dll、部署 Qt 运行时
.un.ps1          # 弹出 Qt 窗口（点击按钮验证 C++→Java 回调）
.un.ps1 -AutoClose 3000   # 3 秒后自动关闭（自动化验证用）
```

### 演示代码（JQtDemo.java）

```java
JQtApplication app = new JQtApplication();
JQtWindow window = new JQtWindow("JQt 窗口", 640, 480);
JQtLabel label = new JQtLabel("Hello, JQt!");
JQtButton button = new JQtButton("点我试试");

button.onClick(() -> {          // Qt clicked 信号 → JNI 回调 → Java lambda
    label.setText("点击成功！");
});
button.onPressed(() -> System.out.println("pressed"));
button.onReleased(() -> System.out.println("released"));
window.onResized((w, h) -> System.out.println("resized: " + w + "x" + h));
app.onAboutToQuit(() -> System.out.println("app quitting"));

JQtVBoxLayout vbox = new JQtVBoxLayout();   // Phase 3：布局管理器
vbox.setSpacing(12);
vbox.addWidget(label);
vbox.addWidget(button);
vbox.addStretch(1);
window.setLayout(vbox);
window.show();
app.exec();                     // 阻塞，最后一个窗口关闭后返回
```

### 事件回调机制（伪信号槽）

```
用户点击按钮
   → Qt 发出 clicked 信号
   → C++ lambda（jqt_bridge.cpp 中 connect）
   → JNI CallVoidMethod 调用 JQtButton.nativeHandleClick()
   → 执行 Java 侧 onClickHandler.run()
```

要点：
- Java 对象通过 `long nativeHandle` 持有 C++ 对象指针
- C++ 侧通过 `NewGlobalRef` 持有 Java 对象引用（防止 GC）
- 回调必然发生在 GUI 主线程（Qt 信号线程），该线程已附加 JVM，无需额外同步
- 控件内存由 Qt 父子关系管理：父窗口销毁时自动销毁子控件

### 发布形态（目标）

```
jqt.jar          ← Java 侧全部代码
jqt.dll          ← Windows 版动态库（当前已产出）
libjqt.so        ← Linux 版动态库（Phase 6）
libjqt.dylib     ← macOS 版动态库（Phase 6）
```

用户侧无需安装 C++ 编译器或 Qt SDK —— `lib/` 目录已自包含 Qt 运行时（Qt6*.dll + 平台插件 + qt.conf）。

### 许可证

JQt 采用 **JQt Source License v1.0（JSL-1.0）** 分层授权（详见 `LICENSE.md`，中英双语，歧义以中文为准）：

| 层 | 用户 | 义务 | 费用 |
|----|------|------|------|
| L1 | 非商业使用 | 署名 SilentStudio | 免费 |
| L2 | 商业使用（累计营收 < $1M） | 应用开源（OSI 许可）+ 署名 | 免费 |
| L3 | 商业使用（累计营收 ≥ $1M） | 开源 或 商业许可（年利润 5%） | 见 COMMERCIAL.md |

- 提交政策：仅限 SilentStudio 成员（`CONTRIBUTING.md`）
- Qt 运行时为 LGPLv3（`THIRD-PARTY-NOTICES.md` + `LGPL-3.0.txt`）
- 许可由 AI 协助起草，不构成法律意见

### 构建依赖（本机）

| 组件 | 路径 |
|------|------|
| JDK 26 | `C:\Program Files\Java\latest\jdk-26` |
| Qt 6.11.2 (mingw_64) | `D:\Qt\6.11.2\mingw_64` |
| MinGW 13.1 | `D:\Qt\Tools\mingw1310_64` |

---

<a id="en"></a>
## English Version

### Architecture (Three Layers)

- **Java layer**: the code you write (`JQtButton btn = new JQtButton("Click me")`)
- **JNI bridge**: `native/jqt_bridge.cpp`, translates Java calls into Qt calls and forwards Qt signals back to Java
- **Qt underneath**: Qt 6.11.2 (mingw_64 kit)

### Roadmap Status

| Phase | Scope | Status |
|-------|-------|--------|
| Phase 0 | Minimal JNI + Qt demo (a Qt window from Java) | ✅ Done |
| Phase 1 | QApplication / QWidget / QPushButton / QLabel | ✅ Done |
| Phase 2 | Signals & slots (click/press/release/toggle, window events, aboutToQuit) | ✅ Done |
| Phase 3 | Layout managers (QVBoxLayout / QHBoxLayout + stretch) | ✅ Done |
| Phase 4 | More widgets (QLineEdit / QComboBox / QListWidget) | ⬜ Not started |
| Phase 5 | Memory management & exception handling | ⬜ Not started |
| Phase 6 | Cross-platform builds (Windows / Linux) | ⬜ Not started (Windows works) |
| Phase 7 | Alpha release | ⬜ Not started |

### Project Layout

```
JQt - Dev/
├── java/org/jqt/          # Java API layer
│   ├── JQtApplication.java  # QApplication wrapper (exec/quit)
│   ├── JQtWidget.java       # widget base class (holds nativeHandle)
│   ├── JQtWindow.java       # window (show/hide/addWidget/onClose)
│   ├── JQtButton.java       # button (onClick/onPressed/onReleased/onToggled)
│   ├── JQtLabel.java        # label (setText)
│   ├── JQtLayout.java       # layout base (addWidget/setSpacing/addStretch)
│   ├── JQtVBoxLayout.java   # vertical layout (QVBoxLayout)
│   ├── JQtHBoxLayout.java   # horizontal layout (QHBoxLayout)
│   └── JQtDemo.java         # demo program
├── native/
│   ├── jqt_bridge.cpp       # JNI bridge (C++ wrapper layer)
│   └── generated/           # JNI headers from javac -h (build-time)
├── build.ps1              # one-click build (Java + C++ + Qt deploy)
├── run.ps1                # run the demo
├── LICENSE.md             # JSL-1.0 layered license (bilingual)
├── CONTRIBUTING.md        # contribution policy (bilingual)
├── THIRD-PARTY-NOTICES.md # Qt LGPLv3 third-party notices (bilingual)
├── COMMERCIAL.md          # commercial license info (bilingual)
├── out/                   # Java bytecode (build artifact)
└── lib/                   # jqt.dll + Qt runtime (build artifact, distributable)
```

### Quick Start

```powershell
.uild.ps1        # compile Java, generate JNI headers, build jqt.dll, deploy Qt runtime
.un.ps1          # show a Qt window (click the button to verify C++→Java callback)
.un.ps1 -AutoClose 3000   # auto close after 3 s (for automation)
```

### Demo Code (JQtDemo.java)

```java
JQtApplication app = new JQtApplication();
JQtWindow window = new JQtWindow("JQt window", 640, 480);
JQtLabel label = new JQtLabel("Hello, JQt!");
JQtButton button = new JQtButton("Click me");

button.onClick(() -> {          // Qt clicked signal → JNI callback → Java lambda
    label.setText("Clicked!");
});
button.onPressed(() -> System.out.println("pressed"));
button.onReleased(() -> System.out.println("released"));
window.onResized((w, h) -> System.out.println("resized: " + w + "x" + h));
app.onAboutToQuit(() -> System.out.println("app quitting"));

JQtVBoxLayout vbox = new JQtVBoxLayout();   // Phase 3: layout manager
vbox.setSpacing(12);
vbox.addWidget(label);
vbox.addWidget(button);
vbox.addStretch(1);
window.setLayout(vbox);
window.show();
app.exec();                     // blocks until the last window closes
```

### Signal-Slot Mechanism (Pseudo Signals)

```
User clicks the button
   → Qt emits the clicked signal
   → C++ lambda (connected in jqt_bridge.cpp)
   → JNI CallVoidMethod invokes JQtButton.nativeHandleClick()
   → Java onClickHandler.run() executes
```

Key points:
- Java objects hold C++ object pointers via `long nativeHandle`
- The C++ side holds Java object references via `NewGlobalRef` (prevents GC)
- Callbacks always occur on the GUI main thread (Qt signal thread), which is already attached to the JVM — no extra synchronization needed
- Widget memory is managed by the Qt parent-child relationship: children are destroyed when the parent is destroyed

### Distribution Target

```
jqt.jar          ← all Java-side code
jqt.dll          ← Windows dynamic library (currently built)
libjqt.so        ← Linux dynamic library (Phase 6)
libjqt.dylib     ← macOS dynamic library (Phase 6)
```

End users need no C++ compiler or Qt SDK — `lib/` is self-contained with the Qt runtime (Qt6*.dll + platform plugins + qt.conf).

### License

JQt is licensed under the **JQt Source License v1.0 (JSL-1.0)** layered license (see `LICENSE.md`, bilingual; the Chinese version prevails in case of ambiguity):

| Tier | User | Obligation | Fee |
|------|------|------------|-----|
| L1 | Non-commercial use | Attribution to SilentStudio | Free |
| L2 | Commercial use (Cumulative Revenue < $1M) | Application open source (OSI license) + attribution | Free |
| L3 | Commercial use (Cumulative Revenue ≥ $1M) | Open source, or commercial license (5% of annual profit) | See COMMERCIAL.md |

- Commit policy: SilentStudio members only (`CONTRIBUTING.md`)
- Qt runtime is LGPLv3 (`THIRD-PARTY-NOTICES.md` + `LGPL-3.0.txt`)
- The license was drafted with AI assistance and does not constitute legal advice

### Build Dependencies (This Machine)

| Component | Path |
|-----------|------|
| JDK 26 | `C:\Program Files\Java\latest\jdk-26` |
| Qt 6.11.2 (mingw_64) | `D:\Qt\6.11.2\mingw_64` |
| MinGW 13.1 | `D:\Qt\Tools\mingw1310_64` |
