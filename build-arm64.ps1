# ============================================================================
# build-arm64.ps1 - build jqt.dll for Windows ARM64 (MSVC toolchain)
#
# Requires:
#   Visual Studio 2022 with ARM64 tools (vcvarsall.bat arm64)
#   Qt 6.x win64_arm64 (MSVC build) - e.g. C:/Qt/6.8.3/win64_arm64
#   JDK with windows-aarch64 support
#
# Usage:
#   .\build-arm64.ps1 -JDK C:/jdk -QtRoot C:/Qt/6.8.3/win64_arm64
#
# NOTE: ASCII-only (Windows PowerShell 5.1).
# ============================================================================

param(
    [string]$JDK = "C:\Program Files\Java\latest\jdk-26",
    [string]$QtRoot = "C:\Qt\6.8.3\win64_arm64"
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$OutDir = Join-Path $Root "out"
$LibDir = Join-Path $Root "lib"
$GenDir = Join-Path $Root "native\generated"

# ---- 1) Load MSVC ARM64 environment (vcvarsall) ----
Write-Host "==> [1/5] Loading MSVC ARM64 environment"
$vswhere = "${env:ProgramFiles(x86)}\Microsoft Visual Studio\Installer\vswhere.exe"
$vsPath = & $vswhere -latest -property installationPath 2>$null
if (-not $vsPath) { throw "Visual Studio not found via vswhere" }
$vcvars = Join-Path $vsPath "VC\Auxiliary\Build\vcvarsall.bat"
if (-not (Test-Path $vcvars)) { throw "vcvarsall.bat not found: $vcvars" }
$envBlock = cmd /c "call `"$vcvars`" arm64 >nul 2>&1 && set"
if ($LASTEXITCODE -ne 0) { throw "vcvarsall failed" }
foreach ($line in $envBlock) {
    if ($line -match '^([^=]+)=(.*)$') {
        try { Set-Item -Path "Env:$($matches[1])" -Value $matches[2] -ErrorAction SilentlyContinue } catch {}
    }
}
if (-not $env:VCToolsInstallDir) { throw "VCToolsInstallDir not set - vcvarsall did not run" }

# ---- 2) Compile Java + generate JNI headers ----
Write-Host "==> [2/5] Compiling Java and generating JNI headers"
New-Item -ItemType Directory -Force -Path $OutDir, $LibDir, $GenDir | Out-Null
$javaFiles = Get-ChildItem (Join-Path $Root "java") -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
& "$JDK\bin\javac.exe" -encoding UTF-8 -d $OutDir -h $GenDir $javaFiles
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

# ---- 3) Compile native bridge (cl.exe, ARM64) ----
# v0.7.4: 确保 QtSerialPort 头完整（转发头 + qserialport.h）——7z 解压可能不完整或嵌套
$spInc = Join-Path $QtRoot "include\QtSerialPort"
if (-not (Test-Path (Join-Path $spInc "qserialport.h"))) {
    Write-Host "WARN: qserialport.h missing at $spInc - searching source"
    $src = Get-ChildItem C:/Qt -Recurse -Filter qserialport.h -ErrorAction SilentlyContinue | Where-Object { $_.FullName -notlike "$spInc*" } | Select-Object -First 1
    if ($src) {
        $srcDir = $src.Directory
        while ($srcDir.Name -ne "QtSerialPort" -and $srcDir.Parent) { $srcDir = $srcDir.Parent }
        New-Item -ItemType Directory -Force -Path $spInc | Out-Null
        Copy-Item "$srcDir\*" $spInc -Recurse -Force
        Write-Host "qserialport.h recovered from $srcDir"
    } else { Write-Host "WARN: no qserialport.h source found" }
} else { Write-Host "QtSerialPort headers OK" }
Write-Host "==> [3/5] Compiling native bridge (jqt.dll, ARM64)"
if (-not (Test-Path (Join-Path $QtRoot "include\QtSerialPort\QSerialPort"))) {
    Write-Host "WARN: QtSerialPort headers missing at $(Join-Path $QtRoot 'include\QtSerialPort') - listing include:"
    Get-ChildItem (Join-Path $QtRoot "include") -ErrorAction SilentlyContinue | Select-Object -First 15 -ExpandProperty Name | Out-Host
}
# 无前缀 include（<QSerialPort>）也能解析：头复制到 QtCore include
Get-ChildItem (Join-Path $QtRoot "include\QtSerialPort") -Filter "*.h" -ErrorAction SilentlyContinue | Copy-Item -Destination (Join-Path $QtRoot "include\QtCore") -Force
Copy-Item (Join-Path $QtRoot "include\QtSerialPort\QSerialPort") (Join-Path $QtRoot "include\QtCore") -Force -ErrorAction SilentlyContinue
Copy-Item (Join-Path $QtRoot "include\QtSerialPort\QSerialPortInfo") (Join-Path $QtRoot "include\QtCore") -Force -ErrorAction SilentlyContinue
$clArgs = @(
    "/nologo", "/std:c++17", "/DJQT_HAVE_NETWORK", "/DJQT_HAVE_SQL_MODELS", "/O2", "/LD", "/EHsc", "/MD", "/W3", "/Zc:__cplusplus", "/permissive-",
    "/I", (Join-Path $JDK "include"),
    "/I", (Join-Path $JDK "include\win32"),
    "/I", (Join-Path $QtRoot "include"),
    "/I", (Join-Path $QtRoot "include\QtWidgets"),
    "/I", (Join-Path $QtRoot "include\QtGui"),
    "/I", (Join-Path $QtRoot "include\QtCore"),
    "/I", (Join-Path $QtRoot "include\QtPrintSupport"),
    "/I", (Join-Path $QtRoot "include\QtSql"),
    "/I", (Join-Path $QtRoot "include\QtNetwork"),
    "/I", (Join-Path $QtRoot "include\QtSerialPort"),
    "/I", (Join-Path $Root "native"),
    (Join-Path $Root "native\jqt_bridge.cpp"),
    ("/Fe:" + (Join-Path $LibDir "jqt.dll")),
    "/link",
    (Join-Path $QtRoot "lib\Qt6Widgets.lib"),
    (Join-Path $QtRoot "lib\Qt6Gui.lib"),
    (Join-Path $QtRoot "lib\Qt6Core.lib"),
    (Join-Path $QtRoot "lib\Qt6PrintSupport.lib"),
    (Join-Path $QtRoot "lib\Qt6Sql.lib"),
    (Join-Path $QtRoot "lib\Qt6Network.lib"),
    (Join-Path $QtRoot "lib\Qt6SerialPort.lib"),
    "ole32.lib", "user32.lib", "dwmapi.lib", "shell32.lib", "gdi32.lib",
    "advapi32.lib", "ws2_32.lib", "winmm.lib", "netapi32.lib", "userenv.lib",
    "version.lib", "comdlg32.lib", "oleaut32.lib"
)
# v0.7.4 诊断：确认 QtSerialPort 头可见
Write-Host "=== QtSerialPort diag ==="
Write-Host "incDir=$(Join-Path $QtRoot 'include\QtSerialPort')"
Get-ChildItem (Join-Path $QtRoot "include\QtSerialPort") -ErrorAction SilentlyContinue | Select-Object -First 8 -ExpandProperty Name | Out-Host
Test-Path (Join-Path $QtRoot "include\QtSerialPort\QSerialPort") | Out-Host
# QtWebSockets 探测(非 qtbase;ARM64 包通常不带 -> 自动降级)。
# 注意:编译标志必须插在 "/link" **之前** —— 追加到数组末尾会落进链接器段,
# cl 会把包含目录当成 .obj 输入(LNK1181,实测踩过)。
$helpLib = Join-Path $QtRoot "lib\Qt6Help.lib"
if (Test-Path $helpLib) {
    $linkIdx6 = [Array]::IndexOf($clArgs, "/link")
    if ($linkIdx6 -lt 0) { $linkIdx6 = $clArgs.Count }
    $pre6 = @("/DJQT_HAVE_HELP", "/I", (Join-Path $QtRoot "include\QtHelp"))
    $clArgs = $clArgs[0..($linkIdx6 - 1)] + $pre6 + $clArgs[$linkIdx6..($clArgs.Count - 1)]
    $clArgs += @("/link", $helpLib)
    Write-Host "==> QtHelp found - QHelpEngineCore enabled"
} else {
    Write-Host "==> QtHelp not found - QHelpEngineCore will report unavailable"
}
$geoLib = Join-Path $QtRoot "lib\Qt6Positioning.lib"
if (Test-Path $geoLib) {
    $linkIdx5 = [Array]::IndexOf($clArgs, "/link")
    if ($linkIdx5 -lt 0) { $linkIdx5 = $clArgs.Count }
    $pre5 = @("/DJQT_HAVE_POSITIONING", "/I", (Join-Path $QtRoot "include\QtPositioning"))
    $clArgs = $clArgs[0..($linkIdx5 - 1)] + $pre5 + $clArgs[$linkIdx5..($clArgs.Count - 1)]
    $clArgs += @("/link", $geoLib)
    Write-Host "==> QtPositioning found - QGeoCoordinate enabled"
} else {
    Write-Host "==> QtPositioning not found - QGeoCoordinate will report unavailable"
}
$qmlLib = Join-Path $QtRoot "lib\Qt6Quick.lib"
$qmlIntegration = Join-Path $QtRoot "include\QtQmlIntegration\qqmlintegration.h"
if ((Test-Path $qmlLib) -and (Test-Path $qmlIntegration)) {
    $linkIdx4 = [Array]::IndexOf($clArgs, "/link")
    if ($linkIdx4 -lt 0) { $linkIdx4 = $clArgs.Count }
    $pre4 = @("/DJQT_HAVE_QML", "/I", (Join-Path $QtRoot "include\QtQuick"), "/I", (Join-Path $QtRoot "include\QtQml"))
    $clArgs = $clArgs[0..($linkIdx4 - 1)] + $pre4 + $clArgs[$linkIdx4..($clArgs.Count - 1)]
    $clArgs += @("/link", $qmlLib, (Join-Path $QtRoot "lib\Qt6Qml.lib"))
    Write-Host "==> QtQuick found - QQuickView enabled"
} else {
    Write-Host "==> QtQuick not found - QQuickView will report unavailable"
}
$mmLib = Join-Path $QtRoot "lib\Qt6Multimedia.lib"
if (Test-Path $mmLib) {
    $linkIdx3 = [Array]::IndexOf($clArgs, "/link")
    if ($linkIdx3 -lt 0) { $linkIdx3 = $clArgs.Count }
    $pre3 = @("/DJQT_HAVE_MULTIMEDIA", "/I", (Join-Path $QtRoot "include\QtMultimedia"))
    $clArgs = $clArgs[0..($linkIdx3 - 1)] + $pre3 + $clArgs[$linkIdx3..($clArgs.Count - 1)]
    $clArgs += @("/link", $mmLib)
    Write-Host "==> QtMultimedia found - QMediaPlayer enabled"
} else {
    Write-Host "==> QtMultimedia not found - QMediaPlayer will report unavailable"
}
$chartLib = Join-Path $QtRoot "lib\Qt6Charts.lib"
if (Test-Path $chartLib) {
    $linkIdx2 = [Array]::IndexOf($clArgs, "/link")
    if ($linkIdx2 -lt 0) { $linkIdx2 = $clArgs.Count }
    $pre2 = @("/DJQT_HAVE_CHARTS", "/I", (Join-Path $QtRoot "include\QtCharts"))
    $clArgs = $clArgs[0..($linkIdx2 - 1)] + $pre2 + $clArgs[$linkIdx2..($clArgs.Count - 1)]
    $clArgs += @("/link", $chartLib)
    Write-Host "==> QtCharts found - QChart enabled"
} else {
    Write-Host "==> QtCharts not found - QChart will report unavailable"
}
$wsLib = Join-Path $QtRoot "lib\Qt6WebSockets.lib"
if (Test-Path $wsLib) {
    $linkIdx = [Array]::IndexOf($clArgs, "/link")
    if ($linkIdx -lt 0) { $linkIdx = $clArgs.Count }
    $pre = @("/DJQT_HAVE_WEBSOCKETS", "/I", (Join-Path $QtRoot "include\QtWebSockets"))
    $clArgs = $clArgs[0..($linkIdx - 1)] + $pre + $clArgs[$linkIdx..($clArgs.Count - 1)]
    $clArgs += @("/link", $wsLib)
    Write-Host "==> QtWebSockets found - QWebSocket enabled"
} else {
    Write-Host "==> QtWebSockets not found - QWebSocket will report unavailable"
}
& cl.exe @clArgs
if ($LASTEXITCODE -ne 0) { throw "cl.exe failed" }

# ---- 4) Deploy Qt runtime (windeployqt) ----
Write-Host "==> [4/5] Deploying Qt runtime"
$deploy = Join-Path $QtRoot "bin\windeployqt.exe"
if (-not (Test-Path $deploy)) { throw "windeployqt not found: $deploy" }
# windeployqt 的 stderr 警告（如 Translations）在 $ErrorActionPreference=Stop 下
# 会触发 NativeCommandError 终止脚本——临时切回 Continue
$ErrorActionPreference = "Continue"
& $deploy (Join-Path $LibDir "jqt.dll") --no-translations --no-system-d3d-compiler --no-opengl-sw --compiler-runtime 2>&1 | Out-Host
$ErrorActionPreference = "Stop"

# ---- 5) qt.conf ----
Write-Host "==> [5/5] Writing qt.conf"
Set-Content -Path (Join-Path $LibDir "qt.conf") -Value "[Paths]`nPlugins = plugins" -Encoding ascii

Write-Host ""
Write-Host "Build OK (ARM64)"
Write-Host "  Java bytecode : $OutDir"
Write-Host "  Dynamic lib   : $(Join-Path $LibDir 'jqt.dll')"
