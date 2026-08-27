# Windows ARM64 构建笔记（v0.4 实战踩坑）

JQt 的 Windows ARM64 支持在 v0.4.0-alpha 落地：原生 arm64 runner + MSVC 工具链 + Qt win64_msvc2022_arm64 包。
本文记录全部踩坑，供后续开发/维护直接避坑。

## 架构概览

```
CI: windows-11-arm runner（GitHub 原生 ARM64）
  ├─ JDK: Temurin 23（windows-aarch64 尚无 26）
  ├─ Qt: 官方 win64_msvc2022_arm64 包（MSVC 构建，非 MinGW）
  ├─ 编译: cl.exe（vcvarsall.bat arm64）+ build-arm64.ps1
  └─ 部署: windeployqt（arm64 版）
```

## 五连坑（按出现顺序）

### 1. aqt 解析 XML 失败（qt_base not found）

```
ERROR: The packages ['qt_base'] were not found while parsing XML of package information!
```

**原因**：aqtinstall 无法解析新仓库格式的 Updates.xml（Linux 同款 bug）。
**解决**：手动 7z 下载。**关键：ARM64 模块名不是 win64_arm64，而是 win64_msvc2022_arm64**：

```powershell
# Updates.xml: https://download.qt.io/online/qtsdkrepository/windows_arm64/desktop/qt6_683/qt6_683/Updates.xml
# 模块: qt.qt6.683.win64_msvc2022_arm64（6.11.2 同理: qt.qt6.6112.win64_msvc2022_arm64）
$url = "https://download.qt.io/online/qtsdkrepository/windows_arm64/desktop/qt6_683/qt6_683/qt.qt6.683.win64_msvc2022_arm64/6.8.3-0-202503201308qtbase-Windows-Windows_11_22H2-MSVC2022-Windows-Windows_11_22H2-AARCH64.7z"
curl.exe -fsSL -o C:/qtbase.7z $url
7z x -y -oC:/Qt C:/qtbase.7z
# 解压后用 Get-ChildItem 找 qmake.exe 定位 QtRoot（结构不固定，勿假设路径）
```

### 2. cl.exe 的 /Fe 参数不能拆开

```
cl : Command line warning D9024 : unrecognized source file type 'C:\...\lib\jqt.dll'
```

**原因**：PowerShell 数组传参时 `/Fe` 和路径是两个参数，cl 把路径当源文件。
**解决**：合并为单参数：`"/Fe:" + (Join-Path $LibDir "jqt.dll")`。

### 3. Qt 6 对 MSVC 的硬性编译参数

```
qcompilerdetection.h: #error "Qt requires a C++17 compiler... /Zc:__cplusplus"
qcompilerdetection.h: #error "On MSVC you must pass the /permissive- option"
```

**解决**：cl 参数必须包含（缺一不可）：

```
/std:c++17 /Zc:__cplusplus /permissive-
```

### 4. windeployqt 的 stderr 会杀死脚本

**现象**：编译成功、windeployqt 只打印 Translations 警告，但脚本以 exit 1 结束。
**原因**：`$ErrorActionPreference = "Stop"` 下，原生命令写 stderr 触发 NativeCommandError，直接终止脚本。
**解决**：windeployqt 调用前后临时切换：

```powershell
$ErrorActionPreference = "Continue"
& $deploy ... 2>&1 | Out-Host
$ErrorActionPreference = "Stop"
```

### 5. 双版本构建时，冒烟必须在第一个版本后跑

**现象**：6.8.3 构建 → 6.11.2 构建 → 冒烟，冒烟 hang（进程不退）。
**原因**：第二次构建（windeployqt）把 lib/ 里的 Qt 运行时覆盖成 6.11.2，
但冒烟步骤的 `QT_PLUGIN_PATH` 仍指向 6.8.3 的 plugins → 版本不匹配 → 挂起。
**解决**：流程改为：**6.8.3 构建 → 冒烟（6.8.3）→ 6.11.2 构建**；
第二次构建后只复制命名 dll（jqt-windows-arm64-6.11.2.dll），不再冒烟。

## 构建脚本入口

```powershell
# 本机（需 VS2022 ARM64 工具集 + Qt win64_msvc2022_arm64 包）
.\build-arm64.ps1 -JDK C:/jdk -QtRoot C:/Qt/6.8.3/win64_msvc2022_arm64
```

CI 参考：`.github/workflows/ci.yml` 的 `windows-arm64` job。

## 版本矩阵（v0.4.0-alpha）

| 平台 | Qt 6.11.2 | Qt 6.8.3 LTS |
|------|-----------|-------------|
| Windows x64 | ✅ | ✅ |
| Windows ARM64 | ✅ | ✅ |
| Linux x64 | ✅ | ✅ |
| macOS x64 | ✅ | ✅ |
