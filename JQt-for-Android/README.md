# JQt-for-Android

> JQt Android 支持工程（绑定同源：Java API + jqt_bridge.cpp 与桌面共用一份，
> 本目录只含 Android 打包层与平台适配）。

## 现状（2026-09-01）

| 项 | 状态 |
|------|------|
| 绑定代码（java/ + native/jqt_bridge.cpp） | 桌面全平台验证（0.7.5-Generator-Kit） |
| Qt 6.11.2 android_arm64_v8a / x86_64 kit | 已安装（远程构建机） |
| Android SDK + NDK 27.2 + Gradle 8.9 + JDK 17 | 已安装 |
| bridge Android 编译验证（NDK clang -fsyntax-only） | 进行中（PoC 第一步） |
| APK 模板工程 | 规划中 |
| 真机运行验证（QMainWindow + 控件 + 软键盘） | PoC 第二步 |

## 为什么不用新仓库

绑定（Java API + JNI 桥）是平台无关的同一份代码——Android 只是换编译目标
（NDK clang 编译 bridge → libjqt_arm64-v8a.so，Java 类进 APK DEX）。
绑定留在主仓库单一事实源，本目录只承载 Android 工程层，避免双维护分叉。

## 路线（对应 docs/JQt移动端意见书.md）

1. PoC 编译：NDK clang 编译 jqt_bridge.cpp（-fsyntax-only → 完整 .so）
2. PoC 运行：最小 APK（QMainWindow + QPushButton + 输入框 + 软键盘）真机/模拟器验证
3. 结论：Widgets-on-Android 可行 → 平台适配（触摸/生命周期/安全区）+ build-android.ps1 固化
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
| build-android.ps1 | 一键构建（编译 bridge .so → 组装 APK） |
| template/ | APK 模板（Manifest/gradle/Activity） |
| docs/ | PoC 状态与决策记录 |
