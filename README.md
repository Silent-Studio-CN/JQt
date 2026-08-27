> 🔗 **全部功能导航：https://jqt.silentstudio.cn/docs **（⚠️ 暂未开放 · Coming Soon）

# JQt — Java 绑定 Qt 框架 / Java Bindings for Qt

> 用 Java 写桌面应用，Qt（C++）负责渲染与事件。无需 C++ 编译器、无需 Qt SDK。
> Desktop apps in Java, powered by Qt underneath.

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
# 下载 jqt-0.5.0-TEST-windows-x64.zip → 解压
# 运行注意：jqt.dll 依赖 Qt6*.dll，需把 lib 目录加入 DLL 搜索路径
# （cd 到 lib 目录，或把 lib 加入 PATH）——-Djava.library.path 只定位 jqt.dll 本身
cd lib
java -Djava.library.path=. -cp "jqt-0.5.0-TEST.jar;.." Hello
# 或：不切目录，用 PATH 方式
# $env:PATH = "$PWD\lib;$env:PATH"
# java -Djava.library.path=lib -cp "lib\jqt-0.5.0-TEST.jar;." Hello
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

> 🔗 **全部功能与完整 API：https://jqt.silentstudio.cn/docs**（暂未开放 · Coming Soon）

---

## 📦 发布包（v0.5.0-TEST）

| 资产 | 平台 |
|------|------|
| `jqt-0.5.0-TEST.jar` | 全部（Java API） |
| `jqt-0.5.0-TEST-windows-x64.zip` | Windows x64 完整包（Qt 6.11.2 运行库） |
| `jqt-windows-6.11.2.dll` / `jqt-windows-6.8.3.dll` | Windows x64 裸库（双 Qt 版本） |
| `jqt-windows-arm64-6.8.3.dll` | Windows ARM64 |
| `libjqt-linux-6.11.2.so` / `libjqt-linux-6.8.3.so` | Linux（双版本） |
| `libjqt-macos-6.11.2.dylib` / `libjqt-macos-6.8.3.dylib` | macOS（双版本） |

> 最新发布见 [GitHub Releases](https://github.com/Silent-Studio-CN/JQt/releases)
## 📄 仓库内文档

| 文档 | 内容 |
|------|------|
| [docs/api-implemented.md](docs/api-implemented.md) | 已实现 API 完整清单（双语） |
| [docs/user-guide.md](docs/user-guide.md) | 安装配置 / 三平台运行 / FAQ |
| [docs/api-tiering.md](docs/api-tiering.md) | API 分级设计（L1/L2/L3） |
| [CHANGELOG.md](CHANGELOG.md) | 变更日志 |

## 🎁 社区资源（Community）

仓库 [Community/](Community/) 目录收录社区贡献的**全部开源免费**资源：

- **许可**：JSL-1.0（与 JQt 相同，见 [LICENSE.md](LICENSE.md)）——**免费使用、免费修改、免费分发**（遵守 JSL-1.0 条款即可）
- **当前收录**：

| 资源 | 说明 |
|------|------|
| [jqt-theme-pack](Community/jqt-theme-pack/) | 三个原创主题：**Nord**（北极蓝·暗）/ **Solarized**（米黄护眼·亮）/ **Terminal**（荧光绿·暗） |
| [FluentAnimDemo](Community/FluentAnimDemo/) | qfluentwidgets 经典动效的 JQt 映射演示（缩放/按压下沉/滑块/淡入） |
| [JQtGallery](Community/JQtGallery/) | 全功能演示：5 套主题 / 强调色 / 自动跟随 / 控件 / 动画 / 窗口（含 jpackage 打包方案） |

```java
// 社区主题即插即用（一套模板，无限主题）
app.setTheme("themes/fluent.qss.tpl", NordTheme.vars(), false);       // 暗色
app.setTheme("themes/fluent.qss.tpl", SolarizedTheme.vars(), true);   // 亮色
```

> 想提交自己的作品？把源码放进 `Community/`（请勿提交编译产物），我们审核后合入。

---

## 👥 贡献者（Contributors）

| 头像 | 贡献者 | 角色 |
|------|--------|------|
| <img src="assets/deepseek-2.svg" width="64" alt="DeepSeek-Work-In-SilentStudio"/> | **DeepSeek-Work-In-SilentStudio** (@DeepSeek-Work-In-SilentStudio) | AI 开发（提交署名，GitHub 账号头像；`.mailmap` 映射全部历史提交） |
| | **Silent-xiaomiao** | 项目发起 / 发布（GitHub: [Silent-xiaomiao](https://github.com/Silent-xiaomiao)） |


---

## 🤝 参与

- 提交政策：仅限 SilentStudio 成员（CONTRIBUTING.md）；反馈：GitHub Issues
- 许可：JSL-1.0 分层授权（LICENSE.md）；Qt 运行时 LGPLv3（LGPL-3.0.txt）

---

© SilentStudio.
