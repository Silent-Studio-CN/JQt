# 09 · jpackage 打包 JQt 应用实战（ServerControlKit 案例）

> 用 jpackage 打包"JQt 应用 + 原生 Qt DLL"的完整踩坑记录。
> 案例：ServerControlKit（分币必赚）—— JQt 0.7.4 + 10 个 Qt DLL + 平台插件。
> 核心结论：**jpackage 对"Java + 原生 DLL"混合应用的布局有陷阱，必须手动干预**。

## 1. 错误现象

打包后 exe 启动：
- `UnsatisfiedLinkError: jqt.dll: Can't find dependent libraries`
- 或退出码 `0xC0000142`（DLL 初始化失败）且无任何 stdout
- 或 `qt.qpa.plugin: Could not find the Qt platform plugin "windows"`

## 2. 根因一：jqt.dll 依赖的 Qt DLL 缺失

`objdump -p jqt.dll | grep "DLL Name"` 查导入表：

```
DLL Name: Qt6Core.dll
DLL Name: Qt6Gui.dll
DLL Name: Qt6OpenGLWidgets.dll
DLL Name: Qt6PrintSupport.dll
DLL Name: Qt6SerialPort.dll   ← 易漏！
DLL Name: Qt6Sql.dll          ← 易漏！
DLL Name: Qt6Widgets.dll
...
```

**教训**：jqt.dll 依赖的 Qt DLL 不止 Core/Gui/Widgets。打包前必须 objdump 查
完整导入表，把**所有**依赖 DLL 放进 exe 目录。JQtGallery 之前也漏过
Qt6SerialPort/Qt6Sql（v0.7.4 起 jqt.dll 依赖它们）。

## 3. 根因二：jpackage 把 input 全部文件复制进 app/ → 启动器预加载冲突

**jpackage 会把 input 目录里的所有文件（含 DLL）都复制到 app/**。
这导致：启动器 exe（MSVC 构建）启动时**预加载 app/ 里的 MinGW 构建的
Qt6*.dll** → CRT 冲突 → `0xC0000142`（DLL init failed）。

**正确做法**：
```
input/ 只放 jar（sck.jar + jqt-0.7.4-Universal-Kit.jar）
打包后手动把 DLL/plugins/themes 复制到 exe 根目录
java-options: -Djava.library.path=$APPDIR/..   （指向 exe 根目录）
```

## 4. 根因三：qt.conf 的 Plugins 路径写错

```
# 错误：Plugins = .   → Qt 找 <exe>/platforms/（实际在 plugins/platforms/）
# 正确：
[Paths]
Plugins = plugins      → Qt 找 <exe>/plugins/platforms/qwindows.dll
```

**教训**：qt.conf 的 Plugins 是相对 exe 目录的子路径。写 `.` 只在
plugins 目录与 exe 目录同级时正确（JQtGallery 的部署方式），
plugins/ 作为子目录时必须写 `plugins`。

## 5. 根因四：-Duser.dir 引发退出崩溃

`--java-options "-Duser.dir=$APPDIR"` 会让程序在启动后随机崩溃
（0xC0000142）。**不要设 -Duser.dir**。程序读资源用 jar 兜底/多路径回退，
不依赖 user.dir。

## 6. 根因五：主题/资源文件硬编码开发机绝对路径

```java
// 错误：打包后路径不存在
readFile("D:/SilentStudio/分币必赚/rc/ui/themes/fluent.qss.tpl")

// 正确：多路径回退 + jar 资源兜底
static String readThemeFile(String name) {
    String[] cands = { "themes/" + name, "app/themes/" + name, "<dev path>/" + name };
    for (String c : cands) { String s = readFile(c); if (!s.isEmpty()) return s; }
    try (InputStream in = ServerControlKit.class.getResourceAsStream("/themes/" + name)) {
        if (in != null) return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (Exception ignored) { }
    return "";
}
```
主题文件打进 jar（`jar --create ... themes/`），exe 部署版自给自足。

## 7. 诊断方法论（这次很关键）

1. **--win-console 打包诊断版**：能直接看到 stdout/stderr，
   一条命令暴露真实错误（platform plugin / UnsatisfiedLinkError）。
2. **RedirectStandardOutput 陷阱**：对 GUI 版 exe 重定向 stdio 会**误报
   0xC0000142**（进程其实活着）。诊断必须用 --win-console 版。
3. **objdump 查 DLL 依赖**：先查缺什么，再查错什么。
4. **最小化对比**：用 Hello 程序验证 jpackage 环境本身 OK，
   再逐步加回 SCK 的配置（java-options/多 jar），二分定位。

## 8. 最终正确的打包流程（build_sck_fixed.bat 已沉淀）

```bat
1. input/ 只放 2 个 jar
2. jpackage --type app-image --main-jar sck.jar --main-class ServerControlKit
   --java-options "-Djava.library.path=$APPDIR/.."
3. 手动复制到 exe 根目录：
   jqt.dll + 10 个 Qt6*.dll + libgcc/libstdc++/libwinpthread + plugins/ + sqldrivers/ + themes/
4. qt.conf:  [Paths]  Plugins = plugins
5. 验证：运行 exe 12 秒无崩溃，日志出现 "[SCK] theme=... qss len=..."
```

## 9. 部署布局（最终）

```
C:\SCK\ServerControlKit/
├── ServerControlKit.exe      # jpackage 启动器
├── jqt.dll + Qt6*.dll(10个) + MinGW 运行库   # native 层
├── plugins/platforms/qwindows.dll + sqldrivers/
├── themes/                   # QSS 模板（程序也走 jar 兜底）
├── qt.conf                   # Plugins = plugins
├── app/                      # 只有 jar + cfg（jpackage 生成）
└── runtime/                  # 捆绑 JRE
```
