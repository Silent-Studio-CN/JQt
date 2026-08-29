# JQt — Qt for Java

> Build native desktop apps in Java with the Qt (C++) rendering and event engine underneath.
> No C++ compiler. No Qt SDK. Just Java.

**中文版**：[简体中文](README.zh.md) · **English**: this file

![CI](https://img.shields.io/badge/CI-4%20platforms%20%E2%9C%93-green) ![Qt](https://img.shields.io/badge/Qt-6.8.3%20%2F%206.11.2-blue) ![API](https://img.shields.io/badge/API-580%2B%20methods%2C%2056%20classes-orange) ![License](https://img.shields.io/badge/License-JSL--1.0%20%2B%20LGPLv3-lightgrey)

JQt is a Java binding for [Qt 6](https://www.qt.io/), exposing Qt Widgets as plain Java classes.
Write your UI in Java; Qt handles rendering, events, theming, and platform integration.
Works on **Windows, Linux, and macOS** (x64 + ARM64), built against **both Qt 6.8.3 LTS and 6.11.2**.

```java
import org.jqt.*;

public class Hello {
    public static void main(String[] args) {
        QApplication app = new QApplication();
        QMainWindow window = new QMainWindow("Hello JQt", 640, 480);
        QPushButton button = new QPushButton("Click me");
        button.onClicked(() -> System.out.println("clicked!"));
        QVBoxLayout vbox = new QVBoxLayout();
        vbox.addWidget(button);
        window.setLayout(vbox);
        window.show();
        app.exec();   // blocks until window closes
    }
}
```

---

## Why JQt?

| | JQt | JavaFX / Swing | QtJambi |
|---|-----|---------------|---------|
| Native look & feel | ✅ Qt native | ⚠️ emulated | ✅ |
| API fidelity to Qt | ✅ 1:1 Widgets mapping | — | ⚠️ Qt Quick oriented |
| Chinese docs & support | ✅ | — | ❌ |
| Industrial modules (SQL, Serial, Print) | ✅ built-in | ❌ | ⚠️ |
| Lightweight runtime story | ✅ single zip | ✅ JDK | ⚠️ heavy |

JQt targets **L1/L2/L3 tiered coverage** of the full Qt 6 API surface (~2172 methods tracked in our roadmap).
L1 (common API) is **92.7% complete**; industrial modules and platform exclusives are shipping now.

---

## Quick Start

### 1. Download

Grab the latest release zip (self-contained: jar + native lib + Qt runtime):

```bash
# Windows: jqt-0.7.4-Universal-Kit-windows-x64.zip → extract → cd lib
java -Djava.library.path=. -cp "jqt-0.7.4-Universal-Kit.jar;.." Hello
# Linux / macOS: same pattern, or set LD_LIBRARY_PATH / DYLD_LIBRARY_PATH to lib/
```

> **Note**: the native lib (jqt.dll / libjqt.so / libjqt.dylib) depends on the Qt6 runtime DLLs
> shipped inside the zip; add the `lib` dir to the DLL search path (PATH / LD_LIBRARY_PATH / DYLD_LIBRARY_PATH).

### 2. Maven (JitPack) — zero-registration dependency

No account, no signing — JitPack builds straight from GitHub:

```gradle
// settings.gradle (Gradle 8+)
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

```gradle
// build.gradle
dependencies {
    implementation 'com.github.Silent-Studio-CN:JQt:0.7.4-Universal-Kit'
}
```

The jar is the pure-Java API (89 classes); the native lib and Qt runtime come
from the release zip below.

### 3. Write code

See [docs/getting-started.md](docs/getting-started.md) and the [JQtGallery](Community/JQtGallery/) demo app.

---

## Core API at a Glance

```java
app.schedule(() -> window.resize(800, 600), 1000);   // GUI-thread timer
app.onAboutToQuit(() -> save());                     // cleanup hook
app.runOnUiThread(() -> updateUi());                 // from any thread

button.onClicked(() -> ...);     button.onToggled(on -> ...);
edit.onTextChanged(s -> ...);    edit.onReturnPressed(() -> ...);
combo.onCurrentIndexChanged(i -> ...);
list.onItemClicked(row -> ...);  window.onClose(() -> ...);
window.onResized((w, h) -> ...); window.onMoved((x, y) -> ...);

// Theming: QSS templates + variable sets = unlimited themes
app.setTheme("fluent-dark");                                        // built-in
app.setTheme("themes/fluent.qss.tpl", myTheme.vars(), true);        // custom
```

### Exclusive Kit — same API on all 3 platforms

| API | Windows | macOS | Linux |
|-----|---------|-------|-------|
| `preventSleep(boolean)` | SetThreadExecutionState | NSProcessInfo | D-Bus Inhibit |
| `setAutoStart(enable, path)` | Run registry | LaunchAgent | XDG .desktop |
| `showNotification(t, b, ms)` | tray balloon | Notification Center | D-Bus Notifications |
| Taskbar progress / Dock badge | `setTaskbarProgress` | `setDockBadge` | — |
| Native window styling | DWM colors / Mica | transparent titlebar | — |
| Global hotkey | GlobalHotkey | — | planned |

### Industrial modules

- **QSerialPort** — full serial-port API (ports, baud, parity, flow control, async read/write)
- **QSql** — SQLite/PostgreSQL/MySQL via Qt SQL (open/query/result iteration)
- **QPrinter** — native printing + PDF export (`QTextEdit.printToPdf`, `QWidget.printToPdf`)
- **QOpenGLWidget** — GPU canvas; LWJGL attachable (GL context is current inside paintGL)
- **QAction / QDialog / QMenuBar / QListView / QColor / ...** — 56 classes and growing

---

## Releases

**Versioning**: the number is the version (`0.7.4`); anything after it is a *release codename*
(`-Universal-Kit` = the same full API on all 3 platforms). Same code, same artifacts —
codename only changes per major feature line.

| Channel | Version | Coordinate |
|---------|---------|-----------|
| GitHub Releases | `v0.7.4-Universal-Kit` | release assets |
| Maven Central | `0.7.4` (or `0.7.4-Universal-Kit`, identical) | `io.github.silent-xiaomiao:jqt:0.7.4` |
| JitPack | `0.7.4-Universal-Kit` | `com.github.Silent-Studio-CN:JQt:0.7.4-Universal-Kit` |

Latest: [v0.7.4-Universal-Kit](https://github.com/Silent-Studio-CN/JQt/releases/tag/v0.7.4-Universal-Kit)

| Asset | Platform |
|-------|----------|
| `jqt-0.7.4-Universal-Kit.jar` | all (Java API) |
| `jqt-0.7.4-Universal-Kit-windows-x64.zip` | Windows x64 full package (Qt 6.11.2 runtime) |
| `jqt-windows-6.11.2.dll` / `jqt-windows-6.8.3.dll` | Windows x64 bare libs (both Qt versions) |
| `jqt-windows-arm64-6.8.3.dll` | Windows ARM64 |
| `libjqt-linux-6.11.2.so` / `libjqt-linux-6.8.3.so` | Linux (both versions) |
| `libjqt-macos-6.11.2.dylib` / `libjqt-macos-6.8.3.dylib` | macOS (both versions) |

CI builds all 4 platforms (Windows x64/ARM64, Linux, macOS) × 2 Qt versions on every push —
[see the workflow](.github/workflows/ci.yml).

---

## Documentation

| Doc | What |
|-----|------|
| [docs/getting-started.md](docs/getting-started.md) | Install, run, FAQ |
| [docs/api-implemented.md](docs/api-implemented.md) | Full implemented-API list (bilingual) |
| [docs/api-tiering.md](docs/api-tiering.md) | L1/L2/L3 tiering design |
| [docs/behavior.md](docs/behavior.md) | Behavior contract (display rules, theming, DPI) |
| [CHANGELOG.md](CHANGELOG.md) | Changelog |

---

## Community

The [Community/](Community/) directory collects open-source, free contributions:

| Resource | What |
|----------|------|
| [jqt-theme-pack](Community/jqt-theme-pack/) | 3 original themes: **Nord** / **Solarized** / **Terminal** |
| [FluentAnimDemo](Community/FluentAnimDemo/) | qfluentwidgets-style motion demos mapped to JQt |
| [JQtGallery](Community/JQtGallery/) | Full-featured gallery: 5 themes, controls, animations, jpackage packaging |
| [QraftLab](Community/QraftLab/) | QSS art guide + separated styles + 4-zone lab demo |

Want to contribute? Put your source in `Community/` (no build artifacts) — reviewed and merged.

---

## How It Works

```
┌─────────────────────────────────────────┐
│  Java layer (your code)                 │
│  QPushButton btn = new QPushButton("Hi");   │
│  btn.onClicked(() -> ...);              │
└─────────────────────────────────────────┘
                    ↕ JNI
┌─────────────────────────────────────────┐
│  C++ glue (native/jqt_bridge.cpp)       │
│  Java calls → Qt; Qt signals → Java     │
└─────────────────────────────────────────┘
                    ↕ direct
┌─────────────────────────────────────────┐
│  Qt framework (QApplication/QWidget...) │
└─────────────────────────────────────────┘
```

| Mechanism | Implementation |
|-----------|----------------|
| Signals/slots | Qt signal → C++ lambda → JNI `CallVoidMethod` → Java `onXxx` callback (GUI thread) |
| Memory | handle registry (incrementing IDs, destroyed-sync) + Java Cleaner + dangling protection (throws, no crash) |
| Layouts | QVBoxLayout / QHBoxLayout / QGridLayout / QFormLayout / QStackedLayout |
| Cross-platform | 3-platform CI, artifacts libjqt.so / jqt.dll / libjqt.dylib |
| Dual Qt | same code → Qt 6.11.2 + 6.8.3 LTS |
| Timers | Qt timers → GUI-thread Java runnables (`app.schedule`, callable from any thread) |

---

## License

- **JQt**: JSL-1.0 (JQt Source License) — free to use, modify, and distribute; see [LICENSE.md](LICENSE.md)
- **Qt runtime**: LGPLv3 (dynamic linking) — see [LGPL-3.0.txt](LGPL-3.0.txt) and [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md)

---

## Contributors

- **DeepSeek-Work-In-SilentStudio** — AI development (all commits mapped via .mailmap)
- **Silent-xiaomiao** — project initiator / releases ([GitHub](https://github.com/Silent-xiaomiao))

---

© SilentStudio.