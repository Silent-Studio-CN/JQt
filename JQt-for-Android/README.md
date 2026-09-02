# JQt-for-Android

> JQt Android 支持工程（绑定同源：Java API + jqt_bridge.cpp 与桌面共用一份，
> 本目录只含 Android 打包层与平台适配）。

## 现状（2026-09-02）

| 项 | 状态 |
|------|------|
| 绑定代码（java/ + native/jqt_bridge.cpp） | 桌面全平台验证（0.7.5-Generator-Kit）；Android 平台适配已提交（DBus/SerialPort/回调/attach） |
| Qt 6.11.2 android 4 ABI kit（arm64/armv7/x86_64/x86） | 已安装（远程构建机） |
| Android SDK + NDK 27.2 + Gradle 8.9 + JDK 17 | 已安装 |
| bridge Android 编译（4 ABI libjqt_<abi>.so） | 完成 |
| 多 ABI APK（minSdk 28，jqtpoc-debug.apk ~109MB） | **完成** |
| 模拟器运行验证（SVM dev2：原生 x86_64 + ARM 翻译层均通过，按钮点击交互正常；MuMu Android 15 用户侧通过） | **完成** |

## 为什么不用新仓库

绑定（Java API + JNI 桥）是平台无关的同一份代码——Android 只是换编译目标
（NDK clang 编译 bridge → libjqt_arm64-v8a.so，Java 类进 APK DEX）。
绑定留在主仓库单一事实源，本目录只承载 Android 工程层，避免双维护分叉。

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