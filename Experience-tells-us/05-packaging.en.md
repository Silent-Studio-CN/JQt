# 05. Packaging, distribution and runtime deployment

> Experience in packaging JQt applications (jpackage/JDK mode) and Qt runtime deployment.
> Coverage: JRE directory, Qt DLL location, plugin path, SQL driver, multi-location consistency.

## 1. jpackage application image Structure (Windows)

```
JQtGallery-Pack/
├── JQtGallery/                 # 免安装版（整个文件夹一起分发）
│   ├── JQtGallery.exe          # jpackage 启动器
│   ├── jqt.dll                 # JQt native 库（必须在 exe 目录！）
│   ├── Qt6*.dll                # Qt 运行时（Core/Gui/Widgets/...）
│   ├── platforms/qwindows.dll  # QPA 平台插件
│   ├── sqldrivers/qsqlite.dll  # SQL 驱动插件
│   ├── app/
│   │   ├── gallery.jar         # 应用 fat jar
│   │   └── JQtGallery.cfg      # jpackage 配置
│   └── src/                    # 源码副本（分发用）
├── lib/gallery.jar             # JDK 模式主程序
└── runtime/                    # JDK 模式 Qt 运行时
```

## 2. Critical Path Rules (All trodden out

| Rules | Reason 
|------|------|
| **The JRE is located in the runtime/ subdirectory**(jpackage automatic | Do not manually insert Qt 
| **The Qt DLL must be in the exe directory** | The Windows loader searches by the exe directory 
| **jqt.dll must be in the exe directory** | The same as above 
| `-Djava.library.path=$APPDIR/..` | $APPDIR parses to app/, and jqt.dll is at the upper level 
| qt.conf `Plugins = .` | The root directory of the plugin = exe directory 
| **The JDK mode should set QT_PLUGIN_PATH** | The application directory for command-line java is java.exe, and qt.conf does not take effect 

## 3. Two Mechanisms for Plugin Paths (Big Pitfall)

Sql-driven lookup in JQt`QCoreApplication::libraryPaths()` + `QLibraryInfo::PluginsPath`.

- **exe mode**: libraryPaths contains the exe directory + the Plugins path of qt.conf.
`plugins/sqldrivers/qsqlite.dll` with `sqldrivers/qsqlite.dll` **Put it in both places**The most stable.
- **JDK Pattern (start-gallery.bat)**The application directory is java.exe.`C:\...\javapath`"
qt.conf does not take effect → **The environment variable 'QT_PLUGIN_PATH=%RT%\plugins' must be set**.
Otherwise, QSqlDatabase.addDatabase("QSQLITE") throws an IllegalStateException
"The driver is not available (SQLITE does not find plugins/sqldrivers/qsqlite)".

```bat
set "RT=%~dp0runtime"
set "QT_QPA_PLATFORM_PLUGIN_PATH=%RT%platforms"
set "QT_PLUGIN_PATH=%RT%plugins"
set "PATH=%RT%;%PATH%"
```

## 4. fat jar build

- `jar --update --file gallery.jar -C workdir JQtGallery.class` Replace that in the official jar
demo class (The official jar comes with JQtGallery.class. If not replaced, the old version will run).
- Theme template into jar`jar --update --file gallery.jar -C . themes/fluent.qss.tpl`,
The program reads jar resources as a fallback → exe version does not require external template files.
- When running the fat jar **classpath order**Self-output first (out10;v06.jar;...) ,
Otherwise, the official built-in class will shadow your class.

## 5. Version Consistency Check (Mandatory after deployment)

PowerShell hash comparison for all distribution locations:

```
gallery.jar 三处一致（lib / pack-lib / pack-app）
jqt.dll 三处一致（runtime / pack / repo-lib）
源码四处一致（本地 / pack-src / staging / repo）
```

**pit**: PowerShell `Copy-Item` Occasionally, silent failure occurs when multiple source files are connected to one directory
(Copy only the first one) --**Copy and Item one by one** And recheck the hash. Don't believe a single command.

## 6. Version repository support

- Each version of jqt is released as follows: JQt-x.jar (Java API) + windows-x64.zip (full runtime) + bare library
(jqt-windows-6.8.3/6.11.2.dll, arm64, linux.so, macos.dylib).
- **Dual Qt version**(6.8.3 LTS + 6.11.2) : CI four platforms × dual versions all green to be released.
- The new API of Qt 9+ uses version guards (such as CI dual-version compilation assertions like Access Identifier).
- Publish a package containing the plugins/sqldrivers qsqlite. DLL (SQLite box).

