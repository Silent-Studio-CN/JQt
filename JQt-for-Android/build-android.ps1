# ============================================================================
# build-android.ps1 - JQt Android build (PoC stage)
# Stage 1: NDK clang compile jqt_bridge.cpp -> libjqt_arm64-v8a.so
# Stage 2: APK assembly (template/) - pending template
# NOTE: ASCII-only (PowerShell 5.1 reads BOM-less files as ANSI)
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

# ---------- Stage 1: compile ----------
$outDir = Join-Path $Repo "out-android"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$args = @("-target", "aarch64-linux-android24", "--sysroot=$Sysroot", "-std=c++17")
if ($SyntaxOnly) { $args += "-fsyntax-only" }
else {
    $args += @("-O2", "-shared", "-fPIC")
    $args += @("-o", (Join-Path $outDir "libjqt_arm64-v8a.so"))
}
$args += @("-I", "$QtAndroid\include", "-I", "$QtAndroid\include\QtWidgets", "-I", "$QtAndroid\include\QtGui", "-I", "$QtAndroid\include\QtCore")
$args += @("-I", "$QtAndroid\include\QtPrintSupport", "-I", "$QtAndroid\include\QtSql", "-I", "$QtAndroid\include\QtSerialPort", "-I", "$QtAndroid\include\QtOpenGLWidgets", "-I", "$QtAndroid\include\QtOpenGL")
$args += @("-I", "$Repo\native", "-I", "$Repo\native\generated")
$args += @(Join-Path $Repo "native\jqt_bridge.cpp")
$args += @(Join-Path $Repo "JQt-for-Android\template\jqt_android_main.cpp")

# ---- link Qt libs (Android: full paths, _arm64-v8a suffix) ----
# Needed so DT_NEEDED lists Qt libs -> androiddeployqt deploys Qt + platform plugin.
if (-not $SyntaxOnly) {
    $QtLibDir = Join-Path $QtAndroid "lib"
    foreach ($m in @("Qt6Widgets", "Qt6Gui", "Qt6Core", "Qt6PrintSupport", "Qt6Sql", "Qt6SerialPort", "Qt6OpenGLWidgets", "Qt6OpenGL")) {
        $args += @((Join-Path $QtLibDir ("lib" + $m + "_arm64-v8a.so")))
    }
}

Write-Host "==> clang (SyntaxOnly=$SyntaxOnly)"
& $Clang @args 2>&1 | Select-Object -First 40
if ($LASTEXITCODE -ne 0) { throw "clang compile failed" }
Write-Host "==> OK"

if ($Full) {
    Write-Host ("==> Stage 2: APK assembly pending template. .so at: " + $outDir + "\libjqt_arm64-v8a.so")
}
