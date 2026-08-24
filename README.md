# JQt — Java 绑定 Qt 框架

> 让 Java 程序员能用 Qt 写桌面应用：Java 写业务逻辑，底层渲染和事件由 Qt（C++）完成。

- **Java 层**：你写的代码（`JQtButton btn = new JQtButton("点我")`）
- **JNI 胶水层**：`native/jqt_bridge.cpp`，把 Java 调用翻译成 Qt 调用，并把 Qt 信号回调回 Java
- **Qt 底层**：Qt 6.11.2（mingw_64 kit）

## 当前进度（路线图）

| 阶段 | 内容 | 状态 |
|------|------|------|
| Phase 0 | JNI + Qt 最小 Demo（Java 弹出 Qt 窗口） | ✅ 完成 |
| Phase 1 | QApplication / QWidget / QPushButton / QLabel | ✅ 完成 |
| Phase 2 | 信号槽（点击事件、窗口关闭事件） | 🟡 点击 + 关闭已完成，更多信号待扩展 |
| Phase 3 | 布局管理器（QVBoxLayout / QHBoxLayout） | ⬜ 未开始 |
| Phase 4 | 更多控件（QLineEdit / QComboBox / QListWidget） | ⬜ 未开始 |
| Phase 5 | 内存管理优化、异常处理 | ⬜ 未开始 |
| Phase 6 | Windows / Linux 跨平台编译 | ⬜ 未开始（Windows 已通） |
| Phase 7 | Alpha 发布 | ⬜ 未开始 |

## 项目结构

```
JQt - Dev/
├── java/org/jqt/          # Java API 层
│   ├── JQtApplication.java  # QApplication 封装（exec/quit）
│   ├── JQtWidget.java       # 控件基类（持有 nativeHandle 指针）
│   ├── JQtWindow.java       # 窗口（show/hide/addWidget/onClose）
│   ├── JQtButton.java       # 按钮（onClick 信号槽）
│   ├── JQtLabel.java        # 标签（setText）
│   └── JQtDemo.java         # 演示程序
├── native/
│   ├── jqt_bridge.cpp       # JNI 胶水层（C++ 包装层）
│   └── generated/           # javac -h 生成的 JNI 头（构建时生成）
├── build.ps1              # 一键构建（Java + C++ + Qt 部署）
├── run.ps1                # 运行演示
├── out/                   # Java 字节码（构建产物）
└── lib/                   # jqt.dll + Qt 运行时（构建产物，可整体分发）
```

## 快速开始

```powershell
.uild.ps1        # 编译 Java、生成 JNI 头、编译 jqt.dll、部署 Qt 运行时
.un.ps1          # 弹出 Qt 窗口（点击按钮验证 C++→Java 回调）
.un.ps1 -AutoClose 3000   # 3 秒后自动关闭（自动化验证用）
```

## 演示代码（`JQtDemo.java`）

```java
JQtApplication app = new JQtApplication();
JQtWindow window = new JQtWindow("JQt 第一个窗口", 800, 600);
JQtLabel label = new JQtLabel("Hello, JQt!");
JQtButton button = new JQtButton("点我试试");

button.onClick(() -> {          // Qt clicked 信号 → JNI 回调 → Java lambda
    label.setText("点击成功！");
});
window.addWidget(label);
window.addWidget(button);
window.show();
app.exec();                     // 阻塞，最后一个窗口关闭后返回
```

## 事件回调机制（伪信号槽）

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

## 发布形态（目标）

```
jqt.jar          ← Java 侧全部代码
jqt.dll          ← Windows 版动态库（当前已产出）
libjqt.so        ← Linux 版动态库（Phase 6）
libjqt.dylib     ← macOS 版动态库（Phase 6）
```

用户侧无需安装 C++ 编译器或 Qt SDK —— `lib/` 目录已自包含 Qt 运行时
（Qt6*.dll + 平台插件 + qt.conf）。

## 构建依赖（本机）

| 组件 | 路径 |
|------|------|
| JDK 26 | `C:\Program Files\Java\latest\jdk-26` |
| Qt 6.11.2 (mingw_64) | `D:\Qt\6.11.2\mingw_64` |
| MinGW 13.1 | `D:\Qt\Tools\mingw1310_64` |
