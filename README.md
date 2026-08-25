# JQt — Java 绑定 Qt 框架 / Java Bindings for Qt

> 用 Java 写桌面应用，Qt（C++）负责渲染与事件。无需 C++ 编译器、无需 Qt SDK。
> Desktop apps in Java, powered by Qt underneath.

> 📚 **全部功能与 API 文档：https://jqt.silentstudio.cn/docs**

---

## 🛠 怎么实现的（How It Works）

三层架构，Java 只是薄薄一层包装：

```
┌─────────────────────────────────────────┐
│  Java 层（你写的代码）                   │
│  JQtButton btn = new JQtButton("点我");   │
│  btn.onClick(() -> ...);                  │
└─────────────────────────────────────────┘
                    ↕ JNI
┌─────────────────────────────────────────┐
│  C++ 胶水层（jqt_bridge.cpp）             │
│  翻译 Java 调用 → Qt；信号回调回 Java      │
└─────────────────────────────────────────┘
                    ↕ 直接调用
┌─────────────────────────────────────────┐
│  Qt 框架（QApplication/QWidget/...）      │
└─────────────────────────────────────────┘
```

| 机制 | 实现方式 |
|------|---------|
| **信号槽** | Qt 信号 → C++ lambda → JNI `CallVoidMethod` → Java `onXxx` 回调（GUI 线程） |
| **内存管理** | 句柄注册表（自增 ID，destroyed 同步注销）+ Java Cleaner 回收 + 悬垂保护（抛异常不崩溃） |
| **布局** | QVBoxLayout / QHBoxLayout 封装（间距/弹性空间） |
| **跨平台** | 三平台 CI（Windows/Linux/macOS），产物 libjqt.so / jqt.dll / libjqt.dylib |
| **双 Qt 版本** | 同一套代码编译 Qt 6.11.2 与 6.8.3 LTS 两个版本 |
| **定时任务** | Qt 定时器 → GUI 线程执行 Java Runnable（`app.schedule`，任意线程可调） |

---

## ⚡ 快速开始（Hello World）

```powershell
# 下载发布包（GitHub Releases）→ 解压 → jar 加入 classpath
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

## ✨ 核心 API（示例）

```java
app.schedule(() -> window.resize(800, 600), 1000);   // 定时任务（GUI 线程）
app.onAboutToQuit(() -> save());                      // 退出前保存

button.onClick(() -> ...);       button.onToggled(on -> ...);
edit.onTextChanged(s -> ...);    edit.onReturnPressed(() -> ...);
combo.onCurrentIndexChanged(i -> ...);
list.onItemClicked(row -> ...);  window.onClose(() -> ...);
window.onResized((w, h) -> ...); window.onMoved((x, y) -> ...);
```

> 🔗 **全部功能与完整 API：https://jqt.silentstudio.cn/docs**

---

## 📦 发布包（v0.1.0-alpha）

| 资产 | 平台 |
|------|------|
| `jqt-0.1.0-alpha.jar` | 全部（Java API） |
| `jqt-windows-6.11.2-full.zip` / `jqt-windows-6.8.3-full.zip` | Windows（内置 Qt 运行库） |
| `libjqt-linux-6.11.2.so` / `libjqt-linux-6.8.3.so` | Linux |
| `libjqt-macos-6.11.2.dylib` / `libjqt-macos-6.8.3.dylib` | macOS |

---

## 📄 仓库内文档

| 文档 | 内容 |
|------|------|
| [docs/api-implemented.md](docs/api-implemented.md) | 已实现 API 完整清单（双语） |
| [docs/user-guide.md](docs/user-guide.md) | 安装配置 / 三平台运行 / FAQ |
| [docs/api-tiering.md](docs/api-tiering.md) | API 分级设计（L1/L2/L3） |
| [114514.md](114514.md) | Qt 全部 2172 方法分级清单 |
| [CHANGELOG.md](CHANGELOG.md) | 变更日志 |

## 🤝 参与

- 提交政策：仅限 SilentStudio 成员（CONTRIBUTING.md）；反馈：GitHub Issues
- 许可：JSL-1.0 分层授权（LICENSE.md）；Qt 运行时 LGPLv3（LGPL-3.0.txt）

---

© SilentStudio.