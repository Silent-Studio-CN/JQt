# JQt — Java 绑定 Qt 框架 / Java Bindings for Qt

> 让 Java 程序员用最优雅的方式写出漂亮的桌面应用：Java 写业务逻辑，Qt（C++）负责渲染与事件。
> Write desktop apps in Java with the Qt engine underneath — no C++, no Qt SDK required.

---

## 🚀 路线图 / Roadmap

| 阶段 | 内容 | 状态 |
|------|------|------|
| Phase 0 | JNI + Qt 最小 Demo | ✅ 完成 |
| Phase 1 | QApplication / QWidget / QPushButton / QLabel | ✅ 完成 |
| Phase 2 | 信号槽（点击/按下/释放/勾选/文本/选项/窗口事件/退出前） | ✅ 完成 |
| Phase 3 | 布局管理器（VBox / HBox + 弹性空间） | ✅ 完成 |
| Phase 4 | 更多控件（QLineEdit / QComboBox / QListWidget） | ✅ 完成 |
| Phase 5 | 内存管理（句柄注册表 + Cleaner + 悬垂保护） | ✅ 完成 |
| Phase 6 | 跨平台编译（Windows / Linux / macOS 三平台 CI） | ✅ 完成 |
| Phase 7 | Alpha 发布（v0.1.0-alpha） | ✅ 已发布 |
| Phase 8 | 第二批控件（菜单/树/滚动区/对话框，见 docs/api-tiering.md） | ⬜ 规划中 |
| Phase 9 | HtmlWorkbench（JQt 旗舰应用） | ⬜ 规划中 |

---

## 📁 项目结构 / Structure

```
JQt - Dev/
├── java/org/jqt/          # Java API（11 个类：应用/窗口/按钮/标签/输入框/下拉/列表/布局）
├── native/jqt_bridge.cpp  # JNI 胶水层（C++ → Qt，信号回调回 Java）
├── build.ps1              # Windows 一键构建
├── build-linux.sh         # Linux 构建
├── build-macos.sh         # macOS 构建
├── build-release.ps1      # 发布包打包（jar + 动态库 + 运行时 + 文档）
├── .github/workflows/ci.yml  # 三平台 CI + 发布包自动构建
├── docs/
│   ├── user-guide.md        # 📘 用户指南（安装/配置/FAQ）
│   ├── api-implemented.md   # 📄 已实现 API 完整清单
│   ├── api-tiering.md       # API 分级设计蓝图（L1/L2/L3）
│   ├── getting-started.md   # 快速上手
│   └── qt-ref/              # Qt 官方方法原始数据（45 类）
├── 114514.md              # Qt 全部 2172 方法分级清单
├── LICENSE.md             # JSL-1.0 分层许可（双语）
├── LGPL-3.0.txt           # Qt 运行时许可
├── CHANGELOG.md           # 变更日志
└── VERSION                # 0.1.0-alpha
```

---

## ⚡ 快速开始 / Quick Start

```powershell
# 1. 下载发布包（GitHub Releases）：jqt-0.1.0-alpha.jar + 对应平台动态库
# 2. 把 jar 加入 classpath，动态库目录加入 java.library.path
java -Djava.library.path=lib -cp "lib/jqt-0.1.0-alpha.jar;." Hello
```

```java
import org.jqt.*;

public class Hello {
    public static void main(String[] args) {
        JQtApplication app = new JQtApplication();      // 必须先创建
        JQtWindow window = new JQtWindow("Hello JQt", 640, 480);
        JQtButton button = new JQtButton("点我");
        button.onClick(() -> System.out.println("clicked!"));
        JQtVBoxLayout vbox = new JQtVBoxLayout();
        vbox.addWidget(button);
        window.setLayout(vbox);
        window.show();
        app.exec();                                     // 阻塞至窗口关闭
    }
}
```

---

## ✨ 已实现 API（v0.1.0-alpha）

> 完整清单见 docs/api-implemented.md。所有控件继承 JQtWidget（isCreated/dispose/isDisposed）。

### JQtApplication

| 方法 | 说明 |
|------|------|
| `JQtApplication()` | 创建应用（进程唯一，必须最先创建） |
| `exec()` | 进入事件循环（阻塞，最后窗口关闭后返回） |
| `quit()` / `scheduleQuit(ms)` | 退出 / 延迟退出 |
| `schedule(Runnable, ms)` | 延迟在 GUI 线程执行任务（线程安全） |
| `onAboutToQuit(Runnable)` | 退出前回调 |

### JQtWindow

| 方法 | 说明 |
|------|------|
| `JQtWindow(title[, w, h])` | 创建窗口 |
| `show()` / `hide()` / `resize(w,h)` / `setTitle(s)` | 窗口操作 |
| `addWidget(widget)` / `setLayout(layout)` | 添加控件 / 布局 |
| `onClose()` / `onResized(w,h)` / `onMoved(x,y)` | 窗口事件回调 |

### JQtButton

| 方法 | 说明 |
|------|------|
| `setText(s)` / `setCheckable(b)` / `setChecked(b)` | 属性 |
| `onClick()` / `onPressed()` / `onReleased()` | 点击三件套 |
| `onToggled(boolean)` | 勾选切换（需 setCheckable） |

### JQtLabel / JQtLineEdit / JQtComboBox / JQtListWidget

| 控件 | 关键 API |
|------|---------|
| JQtLabel | `setText(s)` |
| JQtLineEdit | `text()` `setText` `setPlaceholderText` `onTextChanged` `onReturnPressed` |
| JQtComboBox | `addItem` `currentIndex()` `currentText()` `setCurrentIndex` `onCurrentIndexChanged` |
| JQtListWidget | `addItem` `currentRow()` `onItemClicked(row)` `onCurrentRowChanged` |

### 布局：JQtVBoxLayout / JQtHBoxLayout

| 方法 | 说明 |
|------|------|
| `addWidget(w)` / `setSpacing(px)` / `addStretch(n)` | 添加 / 间距 / 弹性空间 |

### 信号规则

- 所有 `onXxx` 支持**多个监听器**，按注册顺序触发；
- 回调始终在 **GUI 主线程**，可直接操作控件，无需加锁；
- 内存：Java 对象不可达自动释放（Cleaner）；调用已释放对象抛 `IllegalStateException`，不会崩溃。

---

## 💡 用法示例 / Examples

**输入框 + 下拉 + 列表：**

```java
JQtLineEdit edit = new JQtLineEdit("");
edit.setPlaceholderText("输入文字，回车确认");
edit.onReturnPressed(() -> System.out.println("输入了：" + edit.text()));

JQtComboBox combo = new JQtComboBox();
combo.addItem("选项 A");
combo.onCurrentIndexChanged(i -> System.out.println("选中：" + combo.currentText()));

JQtListWidget list = new JQtListWidget();
list.addItem("条目 1");
list.onItemClicked(row -> System.out.println("点击第 " + row + " 行"));
```

**勾选按钮 + 窗口事件 + 定时任务：**

```java
JQtButton check = new JQtButton("开关");
check.setCheckable(true);
check.onToggled(on -> System.out.println(on ? "开" : "关"));

window.onResized((w, h) -> System.out.println("尺寸 " + w + "x" + h));
app.schedule(() -> window.resize(800, 600), 1000);  // 1 秒后 GUI 线程执行
app.onAboutToQuit(() -> saveYourData());
```

**更多用法**（安装配置、三平台命令、FAQ）→ 📘 [docs/user-guide.md](docs/user-guide.md)

---

## 📚 文档索引 / Docs

| 文档 | 内容 |
|------|------|
| [docs/user-guide.md](docs/user-guide.md) | 用户指南：安装 / 配置 / 三平台运行 / FAQ |
| [docs/api-implemented.md](docs/api-implemented.md) | 已实现 API 完整清单（双语） |
| [docs/api-tiering.md](docs/api-tiering.md) | API 分级设计蓝图（L1 常用 / L2 分组 / L3 native） |
| [114514.md](114514.md) | Qt 全部 2172 方法分级清单 |
| [CHANGELOG.md](CHANGELOG.md) | 变更日志 |
| [LICENSE.md](LICENSE.md) | JSL-1.0 分层授权 |

## 🤝 参与

- 提交政策：仅限 SilentStudio 成员（见 CONTRIBUTING.md）
- 反馈：GitHub Issues

---

© SilentStudio. JQt is licensed under the JQt Source License v1.0 (see LICENSE.md).