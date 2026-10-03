# JQt 快速上手（Getting Started）

<details>
<summary>🌐 语言 / Language</summary>

- [中文版](#zh) · [English Version](#en)

</details>

---

<a id="zh"></a>
## 中文版

### 你需要什么

| 文件 | 来源 |
|------|------|
| `jqt-0.1.0-alpha.jar` | 发布包 / Release 下载 |
| `jqt.dll`（Windows）/ `libjqt.so`（Linux）/ `libjqt.dylib`（macOS） | 对应平台发布包 |
| Qt 运行库 | 发布包已包含（lib/ 自包含） |

**不需要**安装 C++ 编译器或 Qt SDK 就能**写代码**。
> 运行时需要 Qt 库:Windows 完整包自带;macOS/Linux 请用发布页的 `*-qt-runtime.tar.gz`
> (含 Qt 运行库与启动器),或自行安装 Qt。

### 一键启动脚本（可选）

源码仓库提供自检式启动器 `run.sh`（Linux / macOS / Windows 的 Git Bash），会：找 JDK → 定位
`out/` 类或 `dist/*.jar` → 缺失时从 GitHub Release 自动下载 jar 与当前平台裸库 → 设置
`LD_LIBRARY_PATH` / `DYLD_LIBRARY_PATH` / `QT_QPA_PLATFORM_PLUGIN_PATH` → 启动指定类。

```bash
./run.sh --list                                   # 看有哪些可运行类
./run.sh --class org.jqt.SmokeL1 --auto-close 2000
JQT_QTVER=6.8.3 ./run.sh                          # 指定下载的裸库 Qt 版本
JQT_JAVA=/path/to/java ./run.sh                   # 指定 JDK
```

### 三步跑起来

1. 把 `jqt-0.1.0-alpha.jar` 加入项目依赖；
2. 把动态库放在 `java.library.path`（例如项目下 `lib/` 目录）；
3. 写代码：

```java
import org.jqt.*;

public class Hello {
    public static void main(String[] args) {
        QApplication app = new QApplication();

        QMainWindow window = new QMainWindow("Hello JQt", 640, 480);
        QLabel label = new QLabel("JQt 的第一个程序");
        QPushButton button = new QPushButton("点我");

        button.onClicked(() -> label.setText("点击成功！"));

        QVBoxLayout vbox = new QVBoxLayout();
        vbox.addWidget(label);
        vbox.addWidget(button);
        window.setLayout(vbox);

        window.show();
        app.exec();   // 阻塞，关闭窗口后返回
    }
}
```

### 运行命令（Windows 示例）

```powershell
java -Djava.library.path=lib -cp jqt-0.1.0-alpha.jar Hello
```

> 提示：动态库旁的 Qt6*.dll 需要能被 Windows 加载器找到——把 `lib/` 加入 PATH 即可
> （`set PATH=%CD%\lib;%PATH%`），Linux/macOS 同理设置 LD_LIBRARY_PATH / DYLD_LIBRARY_PATH。

### 许可须知

- JQt 采用 **JSL-1.0 分层授权**（`LICENSE.md`）：非商业署名即可；商业使用见条款
- Qt 运行时为 **LGPLv3**（`LGPL-3.0.txt`）：动态链接满足合规要求

---

<a id="en"></a>
## English Version

### What You Need

| File | Source |
|------|--------|
| `jqt-0.1.0-alpha.jar` | release package / GitHub Release |
| `jqt.dll` (Windows) / `libjqt.so` (Linux) / `libjqt.dylib` (macOS) | matching platform package |
| Qt runtime | included in the package (lib/ is self-contained) |

**No** C++ compiler or Qt SDK is required to **write code**.
> The Qt libraries are needed at runtime: the Windows zip bundles them, and
> macOS/Linux users should use the `*-qt-runtime.tar.gz` bundle (Qt libs + launcher)
> or install Qt themselves.

### Run in Three Steps

1. Add `jqt-0.1.0-alpha.jar` to your dependencies;
2. Put the native library on `java.library.path` (e.g. a `lib/` folder in your project);
3. Write code:

```java
import org.jqt.*;

public class Hello {
    public static void main(String[] args) {
        QApplication app = new QApplication();

        QMainWindow window = new QMainWindow("Hello JQt", 640, 480);
        QLabel label = new QLabel("My first JQt app");
        QPushButton button = new QPushButton("Click me");

        button.onClicked(() -> label.setText("Clicked!"));

        QVBoxLayout vbox = new QVBoxLayout();
        vbox.addWidget(label);
        vbox.addWidget(button);
        window.setLayout(vbox);

        window.show();
        app.exec();   // blocks until the window closes
    }
}
```

### Run (Windows example)

```powershell
java -Djava.library.path=lib -cp jqt-0.1.0-alpha.jar Hello
```

> Tip: the Qt6*.dll files next to the native library must be findable by the Windows loader —
> add the `lib/` folder to PATH (`set PATH=%CD%\lib;%PATH%`); on Linux/macOS set
> LD_LIBRARY_PATH / DYLD_LIBRARY_PATH accordingly.

### License Notes

- JQt is licensed under **JSL-1.0** tiered license (`LICENSE.md`): attribution for non-commercial use;
  commercial terms per the license
- The Qt runtime is **LGPLv3** (`LGPL-3.0.txt`): dynamic linking satisfies compliance
