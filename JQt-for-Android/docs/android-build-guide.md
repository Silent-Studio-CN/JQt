# JQt Android 开发步骤（完整流程）

> 适用版本：Qt 6.11.2 / NDK r27 (27.2.12479018) / Gradle 9.3.1 / AGP 9.0.0 / JDK 17
> 目标：多 ABI（arm64-v8a / armeabi-v7a / x86_64 / x86）+ minSdk 28（Android 9，Qt 6.11 官方底线）
> 状态：2026-09-02 全链路验证通过（模拟器原生运行 + 点击交互）

---

## 0. 架构总览

绑定（java/ + native/jqt_bridge.cpp）与桌面共用同一份源码，Android 只是换编译目标：

- C++ 侧：NDK clang 编译 jqt_bridge.cpp + jqt_android_main.cpp -> libjqt_<abi>.so
- Java 侧：java/org/jqt/*.java 进 APK DEX（JNI 按名匹配 Java_org_jqt_*，无需 JNI_OnLoad）
- 入口：JQtPocActivity 继承 QtActivity，QtLoader 加载 libjqt 并在 Qt 线程调用 main()

```
Java (JQtPocActivity) -- QtLoader --> libjqt_<abi>.so main()
                                       |-- QApplication（唯一实例，attach 给桥）
                                       |-- QPushButton + show()
                                       `-- exec() 事件循环
Java (QApplication 等 API) --JNI--> jqt_bridge.cpp（g_app 复用守卫）
```

---

## 1. 环境准备（远程构建机）

| 组件 | 位置 | 说明 |
|------|------|------|
| Qt 6.11.2 桌面 kit | C:\Qt\6.11.2\mingw_64 | **androiddeployqt.exe 在这里**（不在 android kit bin） |
| Qt 6.11.2 android kits | C:\Qt\6.11.2\android_{arm64_v8a,armv7,x86_64,x86} | 4 个 ABI 全装 |
| Android SDK | C:\AndroidSdk | platforms;android-34、android-36（AGP9 要求）、build-tools 34.0.0（36.0.0 由 AGP 自动装） |
| NDK | C:\AndroidSdk\ndk\27.2.12479018 | 与 Qt 官方一致（避免符号缺失） |
| JDK | C:\BuildTools\jdk17 | Gradle/AGP 用 |
| Gradle 9.3.1 | C:\BuildTools\gradle-9.3.1-bin.zip | services.gradle.org 被墙 -> 腾讯镜像下载，wrapper 指 file:// 本地 |

网络要点：dl.google.com（AGP/androidx）可达；services.gradle.org 不可达。

---

## 2. 源码结构

```
JQt-for-Android/
|-- README.md                    # 项目说明
|-- build-android.ps1            # 多 ABI 编译 .so + java 暂存（ASCII-only！PS5.1 中文会乱码）
|-- docs/
|   |-- poc-status.md            # PoC 状态表
|   `-- android-build-guide.md   # 本文档
`-- template/                    # androiddeployqt 输入目录
    |-- AndroidManifest.xml      # package=org.jqt，activity=JQtPocActivity，lib_name=jqt
    |-- deployment-settings.json # 部署配置（Qt 6.11 键格式，见 §4）
    |-- jqt_android_main.cpp     # Qt 线程入口（QApplication + 按钮 + exec + 日志）
    `-- java/org/jqt/            # Java 源码（gradle srcDir 'java'）
        |-- JQtPocActivity.java  # 入口 Activity（继承 QtActivity）
        `-- <7 个 AWT-free 变体>  # QColor/QFont/QCursor/QFontMetrics/QBitmap/QImage/QPixmap
```

template/java/org/jqt/ 中除 8 个提交文件外，其余由 build-android.ps1 从仓库
java/org/jqt/ 暂存复制（--java-source 在 Qt 6.11 被忽略，必须走
android-package-source-directory 的目录复制；gradle 的 java.srcDirs 含 'java'）。

---

## 3. 构建步骤（按序执行）

### 3.1 同步源码并编译 4 个 ABI 的 .so

```powershell
# 在远程构建机（C:\JQt 为仓库 clone）
& powershell -ExecutionPolicy Bypass -File C:\JQt\JQt-for-Android\build-android.ps1
# 产出：C:\JQt\out-android\libjqt_{arm64-v8a,armeabi-v7a,x86_64,x86}.so
# 同时把 java 树暂存进 template/java/（保留 8 个提交文件，清除旧暂存）
```

### 3.2 生成 Gradle 工程（androiddeployqt）

```powershell
$exe = 'C:\Qt\6.11.2\mingw_64\bin\androiddeployqt.exe'
$out = 'C:\JQt\apk'
# 预放各 ABI 的 app 库（androiddeployqt 要求 <output>/libs/<abi>/lib<name>_<abi>.so 已存在）
foreach ($abi in @('arm64-v8a','armeabi-v7a','x86_64','x86')) {
    New-Item -ItemType Directory -Force -Path "$out\libs\$abi" | Out-Null
    Copy-Item "C:\JQt\out-android\libjqt_$abi.so" "$out\libs\$abi\libjqt_$abi.so" -Force
}
& $exe --input C:\JQt\JQt-for-Android\template\deployment-settings.json --output $out
# 预期退出码 14：末尾尝试下载 gradle wrapper 失败（服务被墙），工程已完整生成
```

### 3.3 打补丁（每次重新生成后都要做）

```powershell
# a) wrapper 指向本地 Gradle zip
$props = "$out\gradle\wrapper\gradle-wrapper.properties"
$c = Get-Content $props -Raw
$c = $c -replace 'distributionUrl=.*', 'distributionUrl=file:///C:/BuildTools/gradle-9.3.1-bin.zip'
$c = $c -replace 'validateDistributionUrl=true', 'validateDistributionUrl=false'
Set-Content $props $c -Encoding ASCII

# b) 项目局部 settings.gradle（仓库根有 settings.gradle，gradle 会向上找到它）
Set-Content "$out\settings.gradle" -Value @"
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
rootProject.name = 'jqtpoc'
"@ -Encoding ASCII
```

### 3.4 构建 APK

```powershell
$env:JAVA_HOME = 'C:\BuildTools\jdk17'
$env:ANDROID_HOME = 'C:\AndroidSdk'
$env:ANDROID_SDK_ROOT = 'C:\AndroidSdk'
Set-Location $out
& .\gradlew.bat --no-daemon assembleDebug
# 产物：$out\build\outputs\apk\debug\jqtpoc-debug.apk（4 ABI 约 109 MB）
```

---

## 4. deployment-settings.json 键说明（Qt 6.11 全部坑）

| 键 | 值示例 | 要点 |
|------|--------|------|
| android-compile-sdk-version | "36" | **必须是字符串**（QJsonValue::toString 对数字返回 null -> "android-" 警告） |
| android-min-sdk-version | "28" | Qt 6.11 官方最低 API 28 |
| android-target-sdk-version | "34" | |
| application-binary | "jqt" | **库基名**（非路径）；.so 需预放在输出 libs/<abi>/ |
| architectures | {"arm64-v8a":"aarch64-linux-android", ...} | **对象**，值为 NDK sysroot 库目录名（stdcpp 拼接用） |
| deployment-dependencies | "jar/Qt6Android.jar,lib/libQt6Core_arm64-v8a.so,..." | **显式文件清单**（禁 ELF 扫描后必须全列）；含 Qt6Android.jar（QtActivityBase 等绑定类） |
| ndk / ndk-host | .../27.2.12479018 / windows-x86_64 | |
| qml-importscanner-binary | 桌面 kit 的 qmlimportscanner.exe | android kit bin 里没有 |
| qt | **对象**：{"arm64-v8a":".../android_arm64_v8a", ...} | 多 ABI 必须 per-arch 对象，否则从 arm64 kit 找库 |
| sdk | C:/AndroidSdk | |
| sdkBuildToolsRevision | "34.0.0" | 会被 AGP9 忽略（最低 36.0.0） |
| stdcpp-path | .../sysroot/usr/lib | 程序自动拼 /<triple>/libc++_shared.so |
| toolchain-prefix | "llvm" | NDK r27 布局（toolchains/llvm/） |
| android-package-source-directory | C:/JQt/JQt-for-Android/template | **必须显式设置**（默认不生效）；Java 源 + Manifest 从这里复制/合并 |

---

## 5. Java 侧平台适配（已提交，勿回退）

| 文件 | 适配 |
|------|------|
| QApplication.java | ① loadLibrary("jqt") 在 Android 上容忍 UnsatisfiedLinkError（库由 QtLoader 加载）；② 主题读取用 readAllBytes/Paths.get（AGP9 转换 JDK 镜像缺 Files.readString） |
| QColor/QFont/QCursor/QFontMetrics/QBitmap/QImage/QPixmap | **AWT-free 变体**（template/java/org/jqt/）——AWT 桥仅桌面可用 |
| jqt_bridge.cpp | 导出 jqtAndroidAttachApp()：main() 创建的 QApplication attach 给 g_app，Java 侧复用（防双实例 abort） |
| jqt_android_main.cpp | 用 __android_log_print 打日志（fprintf(stderr) 在 Android 被丢弃）；按钮带点击计数 |

---

## 6. 安装与运行验证

```powershell
$adb = 'C:\AndroidSdk\platform-tools\adb.exe'
$d = '<设备>（如 127.0.0.1:5724 = SVM dev2）'

# 安装
& $adb -s $d install -r C:\JQt\apk\build\outputs\apk\debug\jqtpoc-debug.apk
# 启动
& $adb -s $d shell am start -n org.jqt/org.jqt.JQtPocActivity
# 日志（tag=jqt）
& $adb -s $d logcat -d -s jqt:*
#   预期：main entry -> QApplication created -> button shown
# 点击按钮（1080x1920 屏，按钮在内容区中央）
& $adb -s $d shell input tap 540 955
#   预期：button clicked, count=1（按钮文字变 Clicked! N）
# 截图（注意：必须用 cmd 重定向，PowerShell 的 > 会破坏 PNG 二进制）
cmd /c "adb -s $d exec-out screencap -p > C:\QtSetup\shot.png"
```

已知表现：ARM 翻译层（x86_64 模拟器跑 arm64 库）首次启动约 10s（JIT 编译 Qt Java 绑定）；
原生 ABI 启动约 4s。

---

## 7. 常见问题速查

| 症状 | 原因 | 处理 |
|------|------|------|
| No SDK path / No target architecture | 旧版键（android_home/android_abis） | 改用 Qt 6.11 连字符键 + architectures 对象 |
| Android platform 'android-' 警告 | 版本键写成数字 | 全部改字符串 |
| Command does not exist: .../toolchains/aarch64-linux-android/... | toolchain-prefix 应为 "llvm" | |
| STL library does not exist at .../libc++_shared.so/<triple>/... | stdcpp-path 多写了末尾文件 | 指向 sysroot/usr/lib |
| Cannot find application binary in build dir | application-binary 是库基名；.so 未预放 | 预放 libs/<abi>/libjqt_<abi>.so |
| Failed to copy .../plugins/platforms | deployment-dependencies 写目录（解析期 qtInstallDirectory 为空） | 逐个列 .so 文件 |
| QtActivityBase 找不到符号 | Qt6Android.jar 未部署 | 加进 deployment-dependencies |
| 包 java.awt 不存在 | AGP9 转换 JDK 镜像无 AWT | 用 template/java 的 AWT-free 变体 |
| 找不到符号 readString | 同上（镜像 java.base 缺新 API） | readAllBytes/Paths.get（API 26+） |
| System.loadLibrary("jqt") 失败 | APK 内是 libjqt_<abi>.so | QApplication 静态块 Android 容忍 |
| createPlatformIntegration abort | 双 QApplication（main 与 Java 竞争） | main() attach g_app；Java 侧暂缓创建 |
| fprintf(stderr) 无日志 | Android stderr 被丢弃 | __android_log_print（-llog 链接） |
| 截图 PNG 损坏 | PowerShell > 重定向破坏二进制 | cmd /c 重定向 |
| gradle: not part of the build | 缺项目局部 settings.gradle | §3.3-b |
| wrapper 下载超时 | services.gradle.org 被墙 | 腾讯镜像 + file:// URL |

---

## 8. 后续路线

1. Java API 调用链接入：JQtPocActivity 延迟创建 QApplication（等 main() attach 完成）-> Java→JNI→Qt 全链路
2. 触摸/生命周期/安全区适配（Widgets-on-Android 结论确认）
3. CI 加 android job（Ubuntu + aqt 安装 4 ABI kit -> 自动出包）
4. 随 JQt 版本发布 Android 产物