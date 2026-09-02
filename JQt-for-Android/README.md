# JQt-for-Android

> JQt Android 支持工程（绑定同源：Java API + jqt_bridge.cpp 与桌面共用一份，
> 本目录只含 Android 打包层与平台适配）。

## 现状（2026-09-02）

| 项 | 状态 |
|------|------|
| 绑定代码（java/ + native/jqt_bridge.cpp） | 桌面全平台验证（0.7.5-Generator-Kit）；Android 平台适配已提交（DBus/SerialPort/回调/attach） |
| Qt 6.11.2 android 4 ABI kit（arm64/armv7/x86_64/x86） | 已安装（远程构建机） |
| Android SDK + NDK 27.2 + Gradle 9.3.1 + JDK 17 | 已安装 |
| bridge Android 编译（4 ABI libjqt_<abi>.so） | 完成 |
| 多 ABI APK（minSdk 28，jqtpoc-debug.apk ~109MB） | **完成** |
| 模拟器运行验证（SVM dev2：原生 x86_64 + ARM 翻译层均通过，按钮点击交互正常；MuMu Android 15 用户侧通过） | **完成** |

## 技术栈

| 层 | 技术 | 说明 |
|------|------|------|
| 绑定语言 | Java 17 (org.jqt.*) + C++17 | 同一份源码，桌面/Android 共用；JNI 按名匹配（Java_org_jqt_*），无需 JNI_OnLoad |
| GUI 框架 | Qt 6.11.2 Widgets | QPA android 平台插件（qtforandroid）；QApplication 单实例由 main() 创建并 attach 给桥 |
| 交叉编译 | NDK r27 clang（aarch64 / armv7a / x86_64 / i686-linux-android24） | build-android.ps1 参数化 4 ABI；链接 Qt6 Widgets/Gui/Core/OpenGL/OpenGLWidgets/PrintSupport/Sql + -llog |
| Android 工程 | androiddeployqt（Qt 6.11 键格式）→ Gradle 9.3.1 + AGP 9.0.0 | compileSdk 36 / minSdk 28（Android 9）/ targetSdk 34；androidx.core 1.17.0 |
| 目标 ABI | arm64-v8a / armeabi-v7a（32 位老机）/ x86_64 / x86 | 通用 APK 四 ABI 全打包（~109 MB），gradle 自动 strip 符号 |
| 平台适配 | Q_OS_ANDROID 守卫（DBus/SerialPort/Linux 分支）+ AWT-free Java 变体 + 主题读取降级（readAllBytes） | 详见 docs/android-build-guide.md §5 |
| 运行验证 | SVM（Silent Virtual Machine：Rust + QEMU + adb）、ARM 翻译层（libndk_translation）、uiautomator、logcat（tag=jqt）、screencap | 原生 x86_64 与 arm64 翻译层均通过；MuMu Android 15 用户侧通过 |
| 调试 | __android_log_print（fprintf(stderr) 在 Android 被丢弃）、adb shell input tap | 点击交互（clicked 信号）logcat 可证 |

## 路线（对应 docs/JQt移动端意见书.md）

1. PoC 编译：NDK clang 编译 jqt_bridge.cpp（-fsyntax-only → 完整 .so）
2. PoC 运行：最小 APK（QMainWindow + QPushButton + 输入框 + 软键盘）真机/模拟器验证
3. 结论：**Widgets-on-Android 可行**（QApplication/控件/事件循环/点击交互全链路验证）→ 平台适配（触摸/生命周期/安全区）继续
   不可行 → 转 Qt Quick 绑定层立项（意见书替代路线）
4. 发布：Android 产物随 JQt 版本发布（CI 加 android job）

## 关键踩坑（2026-09）

- aqt 3.3.0 元数据缺 6.11.2 android 包（官方 online 仓库改布局 qt6_6112_mingw/）
  → 用官方 MaintenanceTool headless 安装（账号已登录）
- QtDBus：Android 是 __linux__ 但无 D-Bus 模块 → bridge 加 Q_OS_ANDROID 守卫（已提交）
- JNI 按名查找（Java_org_jqt_*）在 Android ART 同样工作，无需 JNI_OnLoad

## 文件

| 文件 | 说明 |
|------|------|
| build-android.ps1 | 一键构建（4 ABI 编译 bridge .so + java 暂存） |
| docs/android-build-guide.md | **完整开发步骤指南（环境/构建/验证/FAQ）** |
| template/ | APK 模板（Manifest/gradle/Activity） |
| docs/ | PoC 状态与决策记录 |