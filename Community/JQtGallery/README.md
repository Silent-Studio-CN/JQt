# JQt Gallery —— JQt 全功能演示（原始源码版 · v0.5.0-TEST）

主题换肤 / 控件 / 动画 / 窗口 / v5 新控件 五大分区，全部可点击体验。
本目录为**原始源码**（非反编译恢复），随 JQt v0.5.0-TEST 同步更新。

## 功能

- **主题**：官方暗色 / 官方浅色 + ThemePack 三套原创（Nord 北极蓝 / Solarized 护眼 / Terminal 荧光绿）+ 4 种强调色 + 自动跟随系统深浅色 + 动画节奏
- **控件**：Switch 开关 / 复选框 / 输入框 / 下拉框 / 列表 / 禁用态 / 悬停保护演示
- **动画**：窗口淡入淡出 / 卡片缩放（3 种缓动）/ 投影 / 圆角
- **窗口**：毛玻璃（acrylic）/ 圆角 / 无边框 / 最小化 / 最大化 / 几何信息实时显示
- **v5 面板**：QTableWidget 表格 / QTreeWidget 树 / QTabWidget 选项卡 / QTextEdit 富文本 / QCanvasWidget 自绘画布

## 运行（源码模式）

```powershell
# 1. 编译 JQt 主库（首次）
.\build.ps1
# 2. 编译 Gallery
javac -encoding UTF-8 -cp out -d out Community\JQtGallery\*.java
# 3. 运行（cwd 为仓库根目录，主题模板读取 themes\fluent.qss.tpl）
.\run.ps1 -Class JQtGallery
```

## jpackage 打包（免安装 exe，含定制 JRE + Qt 运行时）

```powershell
# 参考：jlink 定制 runtime 后
# jpackage --input lib --main-jar gallery.jar --main-class JQtGallery \
#          --runtime-image runtime --java-options '--enable-native-access=ALL-UNNAMED' \
#          --java-options '-Djava.library.path=$APPDIR/..'
```

主题模板 `themes/fluent.qss.tpl`（22 个 %var% 占位符）优先读文件系统，找不到时回退 jar 内资源，因此 exe 打包版无需外部模板文件。

## 文件

| 文件 | 说明 |
|------|------|
| `JQtGallery.java` | 主程序（550 行，五大分区） |
| `NordTheme.java` / `SolarizedTheme.java` / `TerminalTheme.java` | ThemePack 原创主题变量表 |

(C) SilentStudio
