# JQt 用户指南（v0.7.5-Generator-Kit）

<details>
<summary>🌐 语言 / Language</summary>

- [中文版](#zh) · [English Version](#en)

</details>

---

<a id="zh"></a>
## 中文版

### 1. 环境要求

| 项目 | 要求 |
|------|------|
| Java | **JDK 17 或更高**（推荐 21+） |
| 操作系统 | Windows 10/11 x64、Linux x64、macOS x64 |
| 其他 | **无需**安装 C++ 编译器或 Qt SDK（发布包自带 Qt 运行库） |

### 2. 下载与选择发布包

从 **GitHub Releases**（https://github.com/Silent-Studio-CN/JQt/releases）下载：

| 你的平台 | 下载 | 说明 |
|----------|------|------|
| Windows | `jqt-windows-6.11.2-full.zip` | 完整包，内置 Qt 6.11.2 运行库（推荐，最新） |
| Windows | `jqt-windows-6.8.3-full.zip` | 完整包，内置 Qt 6.8.3 LTS 运行库 |
| Linux | `libjqt-linux-6.11.2.so` + 系统 Qt | 需要系统安装 Qt 6 运行库 |
| macOS | `libjqt-macos-6.11.2.dylib` + 系统 Qt | 需要系统安装 Qt 6 运行库 |
| 任意平台 | `jqt-0.1.0-alpha.jar` | Java API（必须） |

> **如何选版本**：两个 Qt 版本功能完全一致（JQt 同一套代码编译）。
> 6.11.2 是最新 Qt；6.8.3 是 LTS（长期维护）。Windows 用户推荐 `jqt-windows-6.11.2-full.zip`。

### 3. 安装（三步）

**第 1 步**：解压发布包，得到：
```
lib/
  jqt-0.1.0-alpha.jar
  jqt.dll            (或 libjqt.so / libjqt.dylib)
  Qt6*.dll ...       (Windows 完整包附带；Linux/macOS 用系统 Qt)
```

**第 2 步**：`jqt-0.1.0-alpha.jar` 加入项目 classpath（IDE 直接添加依赖）。

**第 3 步**：让程序找到动态库——

- **Windows**：`-Djava.library.path=lib`，把 `lib` 加入 `PATH`，并**必须**设置平台插件路径：
  ```powershell
  set PATH=%CD%\lib;%PATH%
  set QT_QPA_PLATFORM_PLUGIN_PATH=%CD%\lib\platforms
  java -Djava.library.path=lib --enable-native-access=ALL-UNNAMED -cp "lib\jqt-0.1.0-alpha.jar;." Hello
  ```
  > ⚠️ **新手必踩**：不设 `QT_QPA_PLATFORM_PLUGIN_PATH` 会报 `Could not find the Qt platform plugin "windows"`——
  > Java 进程中的 Qt **不会读取**包内的 qt.conf，必须显式指定 `<包目录>\platforms`。
  > Java 26 建议加 `--enable-native-access=ALL-UNNAMED`（`System::loadLibrary` 在 Java 26 是受限方法，将来会直接拦截）。
- **Linux**：`-Djava.library.path=lib`，且 `LD_LIBRARY_PATH` 包含 `lib` 与 Qt 库目录：
  ```bash
  export LD_LIBRARY_PATH=$PWD/lib:/usr/lib/x86_64-linux-gnu
  java -Djava.library.path=lib -cp "lib/jqt-0.1.0-alpha.jar:." Hello
  ```
- **macOS**：`-Djava.library.path=lib`，且 `DYLD_LIBRARY_PATH` 包含 `lib` 与 Qt 框架目录。

### 4. Hello World

```java
import org.jqt.*;

public class Hello {
    public static void main(String[] args) {
        QApplication app = new QApplication();   // 第一步：必须最先创建

        QMainWindow window = new QMainWindow("Hello JQt", 640, 480);
        QLabel label = new QLabel("我的第一个 JQt 程序");
        QPushButton button = new QPushButton("点我");

        button.onClicked(() -> label.setText("点击成功！"));

        QVBoxLayout vbox = new QVBoxLayout();
        vbox.setSpacing(12);
        vbox.addWidget(label);
        vbox.addWidget(button);
        window.setLayout(vbox);

        window.show();
        app.exec();   // 事件循环：关闭窗口后返回，程序结束
    }
}
```

### 5. 常用控件示例

**输入框 + 下拉框 + 列表：**

```java
QLineEdit edit = new QLineEdit("");
edit.setPlaceholderText("输入文字，回车确认");
edit.onReturnPressed(() -> System.out.println("输入了：" + edit.text()));

QComboBox combo = new QComboBox();
combo.addItem("选项 A");
combo.addItem("选项 B");
combo.onCurrentIndexChanged(i -> System.out.println("选中：" + combo.currentText()));

QListWidget list = new QListWidget();
list.addItem("条目 1");
list.addItem("条目 2");
list.onItemClicked(row -> System.out.println("点击了第 " + row + " 行"));
```

**勾选按钮：**

```java
QPushButton check = new QPushButton("开关");
check.setCheckable(true);
check.onToggled(checked -> System.out.println("状态：" + (checked ? "开" : "关")));
```

**窗口事件与定时任务：**

```java
window.onResized((w, h) -> System.out.println("窗口变为 " + w + "x" + h));
window.onClose(() -> System.out.println("窗口关闭"));
app.onAboutToQuit(() -> System.out.println("应用退出前"));
app.schedule(() -> window.resize(800, 600), 1000);   // 1 秒后在 GUI 线程执行
```

**水平布局 + 弹性空间：**

```java
QHBoxLayout hbox = new QHBoxLayout();
hbox.addWidget(button1);
hbox.addWidget(button2);
hbox.addStretch(1);   // 把按钮推向左端
window.setLayout(hbox);
```

### 6. 重要行为说明

| 主题 | 说明 |
|------|------|
| 线程 | 所有 `onXxx` 回调在 **GUI 主线程**执行；`schedule` 可从任意线程调用，任务在 GUI 线程执行 |
| 多监听器 | 同一信号可注册多个回调，按注册顺序触发 |
| 内存 | Java 对象不可达时自动释放底层 Qt 对象；`dispose()` 可提前手动释放 |
| 错误 | 调用已释放对象 → `IllegalStateException`（程序不会崩溃）；未创建 `QApplication` 就建控件 → 同样抛异常 |
| 中文 | Java 源码用 UTF-8 保存；运行加 `-Dfile.encoding=UTF-8`（JDK 18+ 默认） |

### 7. 常见问题（FAQ）

**Q1：报错 `UnsatisfiedLinkError: no jqt in java.library.path`**
→ 动态库路径没配对：确认 `-Djava.library.path` 指向含 `jqt.dll`（或 `.so`/`.dylib`）的目录。

**Q2：Windows 报错 `Could not find the Qt platform plugin "windows"`**
→ **必须**设置 `QT_QPA_PLATFORM_PLUGIN_PATH` 指向发布包的 `platforms` 目录（如 `set QT_QPA_PLATFORM_PLUGIN_PATH=%CD%\lib\platforms`）。
  包内的 qt.conf 在 Java 进程中不生效，只有显式环境变量有效。

**Q3：Linux/macOS 报 Qt 库找不到**
→ 系统需安装 Qt 6 运行库（`sudo apt install libqt6widgets6` 等 / brew install qt），并设置 `LD_LIBRARY_PATH`/`DYLD_LIBRARY_PATH`。

**Q4：窗口弹出后事件循环不返回**
→ `app.exec()` 会一直运行直到**所有窗口关闭**。关闭窗口后自动返回。

**Q5：可以同时注册多个点击回调吗？**
→ 可以。`button.onClicked(a); button.onClicked(b);` 两者都会触发。

**Q6：程序退出前想保存数据？**
→ `app.onAboutToQuit(() -> 保存...)`。

**Q7：Java 26 提示 `WARNING: Restricted method System::loadLibrary`？**
→ 正常警告。建议运行参数加 `--enable-native-access=ALL-UNNAMED`（Java 26 起 loadLibrary 是受限方法，未来版本会默认拦截）。

**Q8：控制台中文乱码？**
→ 纯显示问题：运行前执行 `chcp 65001` 切换 UTF-8 代码页即可。

**Q9：窗口是深色的，和原生 Qt 程序（白色）不一样？**
→ 这是 Qt 在 Java 进程中的暗色检测差异（系统深色模式下 Qt 误判 java.exe 为深色应用）。
  想要经典浅色：运行加 `-Djqt.lightMode=true`，或代码里 `app.setLightMode(true)`。
  想要深色主题：配合 QSS 自定义（见 docs/api-implemented.md）。

**Q9：窗口是深色的，和原生 Qt 程序（白色）不一样？**
→ 这是 Qt 在 Java 进程中的暗色检测差异（系统深色模式下 Qt 误判 java.exe 为深色应用）。
  想要经典浅色：运行加 `-Djqt.lightMode=true`，或代码里 `app.setLightMode(true)`。
  想要深色主题：配合 QSS 自定义（见 docs/api-implemented.md）。

### 8. 许可提醒

- JQt 采用 **JSL-1.0 分层授权**（详见 `LICENSE.md`）；
- 发布包内含 Qt 运行库（**LGPLv3**，详见 `LGPL-3.0.txt`）——动态链接满足合规要求，您的应用无需开源。

---

<a id="en"></a>
## English Version

### 1. Requirements

| Item | Requirement |
|------|-------------|
| Java | **JDK 17+** (21+ recommended) |
| OS | Windows 10/11 x64, Linux x64, macOS x64 |
| Other | **No** C++ compiler or Qt SDK needed (runtime bundled) |

### 2. Choose Your Package (GitHub Releases)

| Platform | Download | Notes |
|----------|----------|-------|
| Windows | `jqt-windows-6.11.2-full.zip` | full package, Qt 6.11.2 bundled (latest) |
| Windows | `jqt-windows-6.8.3-full.zip` | full package, Qt 6.8.3 LTS bundled |
| Linux | `libjqt-linux-6.11.2.so` + system Qt | requires system Qt 6 runtime |
| macOS | `libjqt-macos-6.11.2.dylib` + system Qt | requires system Qt 6 runtime |
| Any | `jqt-0.1.0-alpha.jar` | Java API (required) |

> Both Qt versions provide identical features (same JQt codebase). 6.11.2 is the latest Qt;
> 6.8.3 is LTS. Windows users: use `jqt-windows-6.11.2-full.zip`.

### 3. Install (3 Steps)

**Step 1**: unzip. You get:
```
lib/
  jqt-0.1.0-alpha.jar
  jqt.dll            (or libjqt.so / libjqt.dylib)
  Qt6*.dll ...       (Windows full package; Linux/macOS use system Qt)
```

**Step 2**: add `jqt-0.1.0-alpha.jar` to your classpath.

**Step 3**: let the JVM find the native library —

- **Windows**: `-Djava.library.path=lib`, add `lib` to `PATH`, and **must** set the plugin path:
  ```powershell
  set PATH=%CD%\lib;%PATH%
  set QT_QPA_PLATFORM_PLUGIN_PATH=%CD%\lib\platforms
  java -Djava.library.path=lib --enable-native-access=ALL-UNNAMED -cp "lib\jqt-0.1.0-alpha.jar;." Hello
  ```
  > ⚠️ **Newbie trap**: without `QT_QPA_PLATFORM_PLUGIN_PATH` you get `Could not find the Qt platform plugin "windows"` —
  > Qt inside a Java process **does not read** the bundled qt.conf; set it to `<pkg>\platforms` explicitly.
  > On Java 26 add `--enable-native-access=ALL-UNNAMED` (`System::loadLibrary` is restricted and will be blocked).
- **Linux**: `-Djava.library.path=lib` and `LD_LIBRARY_PATH` includes `lib` + Qt dirs:
  ```bash
  export LD_LIBRARY_PATH=$PWD/lib:/usr/lib/x86_64-linux-gnu
  java -Djava.library.path=lib -cp "lib/jqt-0.1.0-alpha.jar:." Hello
  ```
- **macOS**: `-Djava.library.path=lib` and `DYLD_LIBRARY_PATH` includes `lib` + Qt frameworks.

### 4. Hello World

```java
import org.jqt.*;

public class Hello {
    public static void main(String[] args) {
        QApplication app = new QApplication();   // always first

        QMainWindow window = new QMainWindow("Hello JQt", 640, 480);
        QLabel label = new QLabel("My first JQt app");
        QPushButton button = new QPushButton("Click me");

        button.onClicked(() -> label.setText("Clicked!"));

        QVBoxLayout vbox = new QVBoxLayout();
        vbox.setSpacing(12);
        vbox.addWidget(label);
        vbox.addWidget(button);
        window.setLayout(vbox);

        window.show();
        app.exec();   // blocks until the window closes
    }
}
```

### 5. More Widget Examples

**LineEdit + ComboBox + List:**
```java
QLineEdit edit = new QLineEdit("");
edit.setPlaceholderText("type and press Enter");
edit.onReturnPressed(() -> System.out.println("typed: " + edit.text()));

QComboBox combo = new QComboBox();
combo.addItem("Option A");
combo.onCurrentIndexChanged(i -> System.out.println("selected: " + combo.currentText()));

QListWidget list = new QListWidget();
list.addItem("Row 1");
list.onItemClicked(row -> System.out.println("clicked row " + row));
```

**Checkable button:**
```java
QPushButton check = new QPushButton("Toggle");
check.setCheckable(true);
check.onToggled(checked -> System.out.println(checked ? "on" : "off"));
```

**Window events & timers:**
```java
window.onResized((w, h) -> System.out.println("resized " + w + "x" + h));
window.onClose(() -> System.out.println("closed"));
app.onAboutToQuit(() -> System.out.println("quitting"));
app.schedule(() -> window.resize(800, 600), 1000);   // GUI thread, after 1s
```

**HBox with stretch:**
```java
QHBoxLayout hbox = new QHBoxLayout();
hbox.addWidget(button1);
hbox.addWidget(button2);
hbox.addStretch(1);
window.setLayout(hbox);
```

### 6. Behavior Notes

| Topic | Notes |
|-------|-------|
| Threading | all `onXxx` callbacks run on the **GUI thread**; `schedule` is thread-safe and runs on the GUI thread |
| Multi-listener | multiple callbacks per signal, in registration order |
| Memory | unreachable Java objects release their Qt objects automatically; `dispose()` for manual release |
| Errors | calling a disposed object throws `IllegalStateException` (no crash); same when creating widgets before `QApplication` |
| Unicode | save sources as UTF-8; add `-Dfile.encoding=UTF-8` on JDK 17 |

### 7. FAQ

**Q1: `UnsatisfiedLinkError: no jqt in java.library.path`** — point `-Djava.library.path` at the folder containing `jqt.dll`/`.so`/`.dylib`.

**Q2: Windows `Could not find the Qt platform plugin "windows"`** — you **must** set `QT_QPA_PLATFORM_PLUGIN_PATH` to the package `platforms` folder (e.g. `set QT_QPA_PLATFORM_PLUGIN_PATH=%CD%\lib\platforms`). The bundled qt.conf is ignored inside a Java process.

**Q3: Linux/macOS Qt libs missing** — install Qt 6 runtime (`sudo apt install libqt6widgets6` / `brew install qt`) and set `LD_LIBRARY_PATH`/`DYLD_LIBRARY_PATH`.

**Q4: `app.exec()` never returns** — it returns when **all windows are closed**.

**Q5: Multiple click callbacks?** — yes: `onClicked(a); onClicked(b);` both fire.

**Q6: Save data before exit?** — `app.onAboutToQuit(() -> save())`.

**Q7: Java 26 warns `Restricted method System::loadLibrary`?** — add `--enable-native-access=ALL-UNNAMED` to the java command (loadLibrary is restricted on Java 26 and will be blocked in future releases).

**Q8: Chinese text garbled in console?** — display-only issue: run `chcp 65001` before launching.

**Q9: Window is dark, unlike native Qt (white)?** — Qt's dark-mode detection differs inside a Java process (with a dark system theme it mistakes java.exe for a dark app). For the classic light look: add `-Djqt.lightMode=true` or call `app.setLightMode(true)`. For a custom dark theme, combine with QSS.

**Q9: Window is dark, unlike native Qt (white)?** — Qt's dark-mode detection differs inside a Java process (with a dark system theme it mistakes java.exe for a dark app). For the classic light look: add `-Djqt.lightMode=true` or call `app.setLightMode(true)`. For a custom dark theme, combine with QSS.

### 8. License Notes

- JQt: **JSL-1.0** tiered license (`LICENSE.md`);
- Qt runtime in the package: **LGPLv3** (`LGPL-3.0.txt`) — dynamic linking satisfies compliance; your app need not be open source.

