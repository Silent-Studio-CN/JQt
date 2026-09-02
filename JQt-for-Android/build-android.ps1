# ============================================================================
# build-android.ps1 - JQt Android build (PoC stage)
# Stage 1: NDK clang compile jqt_bridge.cpp -> libjqt_<abi>.so (per ABI)
# Stage 2: java tree staging into template/java (for android-package-source-directory)
# NOTE: ASCII-only (PowerShell 5.1 reads BOM-less files as ANSI)
# ============================================================================

param(
    [string]$NDK = "C:\AndroidSdk\ndk\27.2.12479018",
    [string]$QtRoot = "C:\Qt\6.11.2",
    [string]$Repo = "C:\JQt",
    [string]$ABI = "",          # empty = all: arm64-v8a,armeabi-v7a,x86_64,x86
    [switch]$SyntaxOnly,
    [switch]$Full
)

$ErrorActionPreference = "Stop"
$Pre = "$NDK\toolchains\llvm\prebuilt\windows-x86_64"
$Sysroot = "$Pre\sysroot"

$abiMap = @{
    "arm64-v8a"   = @{ triple = "aarch64-linux-android24";      kit = "android_arm64_v8a"; out = "libjqt_arm64-v8a.so" }
    "armeabi-v7a" = @{ triple = "armv7a-linux-androideabi24";   kit = "android_armv7";     out = "libjqt_armeabi-v7a.so" }
    "x86_64"      = @{ triple = "x86_64-linux-android24";       kit = "android_x86_64";    out = "libjqt_x86_64.so" }
    "x86"         = @{ triple = "i686-linux-android24";         kit = "android_x86";       out = "libjqt_x86.so" }
}

$abiList = @()
if ($ABI -ne "") { $abiList = @($ABI) } else { $abiList = @("arm64-v8a", "armeabi-v7a", "x86_64", "x86") }

$outDir = Join-Path $Repo "out-android"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

foreach ($abi in $abiList) {
    if (-not $abiMap.ContainsKey($abi)) { throw "unknown ABI: $abi" }
    $info = $abiMap[$abi]
    $QtAndroid = Join-Path $QtRoot $info.kit
    if (-not (Test-Path $QtAndroid)) { Write-Host "skip $abi (kit missing: $QtAndroid)"; continue }
    $Clang = "$Pre\bin\$($info.triple)-clang++.cmd"

    $args = @("-target", $info.triple, "--sysroot=$Sysroot", "-std=c++17")
    if ($SyntaxOnly) { $args += "-fsyntax-only" }
    else {
        $args += @("-O2", "-shared", "-fPIC")
        $args += @("-o", (Join-Path $outDir $info.out))
    }
    $args += @("-I", "$QtAndroid\include", "-I", "$QtAndroid\include\QtWidgets", "-I", "$QtAndroid\include\QtGui", "-I", "$QtAndroid\include\QtCore")
    $args += @("-I", "$QtAndroid\include\QtPrintSupport", "-I", "$QtAndroid\include\QtSql", "-I", "$QtAndroid\include\QtOpenGLWidgets", "-I", "$QtAndroid\include\QtOpenGL")
    $args += @("-I", "$Repo\native", "-I", "$Repo\native\generated")
    $args += @(Join-Path $Repo "native\jqt_bridge.cpp")
    $args += @(Join-Path $Repo "JQt-for-Android\template\jqt_android_main.cpp")

    # ---- link Qt libs (Android: full paths, _<abi> suffix) ----
    if (-not $SyntaxOnly) {
        $QtLibDir = Join-Path $QtAndroid "lib"
        # NOTE: Qt6SerialPort not shipped in Android kits (bridge guards it with __ANDROID__)
        foreach ($m in @("Qt6Widgets", "Qt6Gui", "Qt6Core", "Qt6PrintSupport", "Qt6Sql", "Qt6OpenGLWidgets", "Qt6OpenGL")) {
            $args += @((Join-Path $QtLibDir ("lib" + $m + "_" + $abi + ".so")))
        }
        $args += @("-llog")   # Android logcat (__android_log_print)
    }

    Write-Host "==> clang [$abi] (SyntaxOnly=$SyntaxOnly)"
    & $Clang @args 2>&1 | Select-Object -First 40
    if ($LASTEXITCODE -ne 0) { throw "clang compile failed for $abi" }
    Write-Host "==> OK $info.out"
}

# ---- Stage 2 prep: java tree into template/java ----
# androiddeployqt (Qt 6.11) ignores --java-source; sources must be copied
# by android-package-source-directory. Gradle srcDirs includes 'java'.
$tmplJava = Join-Path $Repo "JQt-for-Android\template\java\org\jqt"
$srcJava = Join-Path $Repo "java\org\jqt"
if (Test-Path $srcJava) {
    # remove stale staged files (keep committed android variants)
    $keep = @("QColor.java","QFont.java","QCursor.java","QFontMetrics.java","QBitmap.java","QImage.java","QPixmap.java","JQtPocActivity.java")
    if (Test-Path $tmplJava) { Get-ChildItem $tmplJava -Filter *.java | Where-Object { $keep -notcontains $_.Name } | Remove-Item -Force }
    New-Item -ItemType Directory -Force -Path $tmplJava | Out-Null
    Get-ChildItem $srcJava -Filter *.java | Where-Object { -not (Test-Path (Join-Path $tmplJava $_.Name)) } | Copy-Item -Destination $tmplJava
    Write-Host "==> staged java tree into template/java"
}

if ($Full) {
    Write-Host ("==> Stage 3: APK assembly via androiddeployqt (see docs/poc-status.md). .so at: " + $outDir)
}
