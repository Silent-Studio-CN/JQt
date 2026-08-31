# 05 · 打包分发与运行时部署

> JQt 应用的打包（jpackage / JDK 模式）与 Qt 运行时部署经验。
> 覆盖：JRE 目录、Qt DLL 位置、插件路径、SQL 驱动、多位置一致性。

## 1. jpackage 应用镜像结构（Windows）

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

## 2. 关键路径规则（全是踩出来的）

| 规则 | 原因 |
|------|------|
| **JRE 落在 runtime/ 子目录**（jpackage 自动） | 不要手动把 Qt 放进去 |
| **Qt DLL 必须在 exe 目录** | Windows 加载器按 exe 目录搜索 |
| **jqt.dll 必须在 exe 目录** | 同上 |
| `-Djava.library.path=$APPDIR/..` | $APPDIR 解析到 app/，jqt.dll 在上一级 |
| qt.conf `Plugins = .` | 插件根目录 = exe 目录 |
| **JDK 模式要设 QT_PLUGIN_PATH** | 命令行 java 的应用目录是 java.exe，qt.conf 不生效 |

## 3. 插件路径的两套机制（大坑）

JQt 的 SQL 驱动查找：`QCoreApplication::libraryPaths()` + `QLibraryInfo::PluginsPath`。

- **exe 模式**：libraryPaths 含 exe 目录 + qt.conf 的 Plugins 路径。
  `plugins/sqldrivers/qsqlite.dll` 与 `sqldrivers/qsqlite.dll` **两处都放**最稳。
- **JDK 模式（start-gallery.bat）**：应用目录是 java.exe（`C:\...\javapath`），
  qt.conf 不生效 → **必须设置 `QT_PLUGIN_PATH=%RT%\plugins` 环境变量**。
  否则 QSqlDatabase.addDatabase("QSQLITE") 抛 IllegalStateException
  "驱动不可用（SQLITE 未找到 plugins/sqldrivers/qsqlite）"。

```bat
set "RT=%~dp0runtime"
set "QT_QPA_PLATFORM_PLUGIN_PATH=%RT%platforms"
set "QT_PLUGIN_PATH=%RT%plugins"
set "PATH=%RT%;%PATH%"
```

## 4. fat jar 构建

- `jar --update --file gallery.jar -C workdir JQtGallery.class` 替换官方 jar 里的
  demo class（官方 jar 自带 JQtGallery.class，不替换会跑旧版）。
- 主题模板进 jar：`jar --update --file gallery.jar -C . themes/fluent.qss.tpl`，
  程序读 jar 资源兜底 → exe 版无需外部模板文件。
- 运行 fat jar 时 **classpath 顺序**：自己输出在前（out10;v06.jar;...），
  否则官方内置类 shadow 你的类。

## 5. 版本一致性检查（部署后必做）

PowerShell 哈希比对所有分发位置：

```
gallery.jar 三处一致（lib / pack-lib / pack-app）
jqt.dll 三处一致（runtime / pack / repo-lib）
源码四处一致（本地 / pack-src / staging / repo）
```

**坑**：PowerShell `Copy-Item` 多源文件到一个目录时偶尔静默失败
（只复制第一个）——**逐个 Copy-Item** 并复查哈希，别信一条命令。

## 6. 版本库配套

- JQt 每版发布：jqt-X.jar（Java API）+ windows-x64.zip（完整运行时）+ 裸库
  （jqt-windows-6.8.3/6.11.2.dll、arm64、linux .so、macos .dylib）。
- **双 Qt 版本**（6.8.3 LTS + 6.11.2）：CI 四平台 × 双版本全绿才发布。
- Qt 9+ 新 API 用版本守卫（accessibleIdentifier 等 CI 双版本编译断言）。
- 发布包包含 plugins/sqldrivers/qsqlite.dll（SQLite 开箱即用）。
