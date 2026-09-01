# ============================================================================
# build-android.ps1 - JQt Android 构建（PoC 阶段骨架）
# 阶段 1: NDK clang 编译 jqt_bridge.cpp -> libjqt_arm64-v8a.so（fsyntax 验证）
# 阶段 2: 组装 APK（template/ + .so + Java DEX）-> 待模板就绪
# ============================================================================

param(
    [string]$NDK = "C:\AndroidSdk\ndk\27.2.12479018",
    [string]$QtAndroid = "C:\Qt\6.11.2\android_arm64_v8a",
    [string]$Repo = "C:\JQt",
    [switch]$SyntaxOnly,
    [switch]$Full
)

$ErrorActionPreference = "Stop"
$Pre = "$NDK\toolchains\llvm\prebuilt\windows-x86_64"
$Clang = "$Pre\bin\aarch64-linux-android24-clang++.cmd"
$Sysroot = "$Pre\sysroot"

# ---------- 阶段 1: 编译检查 / 完整编译 ----------
$outDir = Join-Path $Repo "out-android"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$args = @("-target", "aarch64-linux-android24", "--sysroot=$Sysroot", "-std=c++17")
if ($SyntaxOnly) { $args += "-fsyntax-only" }
else {
    $args += @("-O2", "-shared", "-fPIC")
    $args += @("-o", (Join-Path $outDir "libjqt_arm64-v8a.so"))
}
$args += @("-I", "$QtAndroid\include", "-I", "$QtAndroid\include\QtWidgets", "-I", "$QtAndroid\include\QtGui", "-I", "$QtAndroid\include\QtCore")
$args += @("-I", "$QtAndroid\include\QtPrintSupport", "-I", "$QtAndroid\include\QtSql", "-I", "$QtAndroid\include\QtSerialPort")
$args += @("-I", "$Repo\native", "-I", "$Repo\native\generated")
$args += @(Join-Path $Repo "native\jqt_bridge.cpp")

Write-Host "==> clang (SyntaxOnly=$SyntaxOnly)"
& $Clang @args 2>&1 | Select-Object -First 40
if ($LASTEXITCODE -ne 0) { throw "clang 编译失败" }
Write-Host "==> OK"

if ($Full) {
    Write-Host "==> 阶段 2: APK 组装（模板就绪后实现）"
    Write-Host ("    产物: " + $outDir + "\libjqt_arm64-v8a.so")
}
