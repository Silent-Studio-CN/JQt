# ============================================================================
# build.ps1 - JQt one-click build
#   1. javac compiles Java sources and generates JNI headers (javac -h)
#   2. g++ compiles the native bridge into jqt.dll
#   3. windeployqt deploys the Qt runtime into lib
#   4. copies the QPA platform plugin + writes qt.conf (lib is self-contained)
#
# Prerequisites (already installed on this machine):
#   JDK 26      C:\Program Files\Java\latest\jdk-26
#   Qt 6.11.2   D:\Qt\6.11.2\mingw_64
#   MinGW 13.1  D:\Qt\Tools\mingw1310_64
#
# NOTE: this file must stay ASCII-only (Windows PowerShell 5.1 reads
#       BOM-less files as ANSI and would garble non-ASCII text).
# ============================================================================

param(
    [string]$JDK    = "C:\Program Files\Java\latest\jdk-26",
    [string]$QtRoot = "D:\Qt\6.11.2",
    [string]$Mingw  = "D:\Qt\Tools\mingw1310_64"
)

$ErrorActionPreference = "Stop"

$Root      = Split-Path -Parent $MyInvocation.MyCommand.Path
$Kit       = Join-Path $QtRoot "mingw_64"

$OutDir    = Join-Path $Root "out"             # Java bytecode
$LibDir    = Join-Path $Root "lib"             # jqt.dll + Qt runtime
$NativeDir = Join-Path $Root "native"
$GenDir    = Join-Path $NativeDir "generated"  # JNI headers from javac -h

$env:PATH = "$Mingw\bin;$Kit\bin;$env:PATH"

Write-Host "==> [1/4] Compiling Java and generating JNI headers"
New-Item -ItemType Directory -Force -Path $OutDir, $LibDir, $GenDir | Out-Null
$javaFiles = Get-ChildItem (Join-Path $Root "java") -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
& "$JDK\bin\javac.exe" -encoding UTF-8 -d $OutDir -h $GenDir $javaFiles
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Write-Host "==> [2/4] Compiling native bridge (jqt.dll)"
$gppArgs = @(
    "-std=c++17", "-O2", "-shared",
    "-o", (Join-Path $LibDir "jqt.dll"),
    "-I", (Join-Path $JDK "include"),
    "-I", (Join-Path $JDK "include\win32"),
    "-I", (Join-Path $Kit "include"),
    "-I", (Join-Path $Kit "include\QtWidgets"),
    "-I", (Join-Path $Kit "include\QtGui"),
    "-I", (Join-Path $Kit "include\QtCore"),
    "-I", (Join-Path $Kit "include\QtPrintSupport"),
    "-I", (Join-Path $Kit "include\QtSql"),
    "-I", $NativeDir,
    (Join-Path $NativeDir "jqt_bridge.cpp"),
    "-L", (Join-Path $Kit "lib"),
    "-lQt6Widgets", "-lQt6Gui", "-lQt6Core", "-lQt6PrintSupport", "-lQt6Sql", "-lole32", "-luuid", "-loleaut32",
    "-static-libgcc", "-static-libstdc++"
)
& "$Mingw\bin\g++.exe" @gppArgs
if ($LASTEXITCODE -ne 0) { throw "g++ failed" }

Write-Host "==> [3/4] Deploying Qt runtime into lib"
$deployArgs = @(
    "--no-translations", "--no-system-d3d-compiler", "--no-opengl-sw", "--compiler-runtime",
    (Join-Path $LibDir "jqt.dll")
)
& "$Kit\bin\windeployqt.exe" @deployArgs
if ($LASTEXITCODE -ne 0) {
    Write-Warning "windeployqt failed; copying Qt runtime manually"
    Copy-Item (Join-Path $Kit "bin\Qt6Core.dll"), (Join-Path $Kit "bin\Qt6Gui.dll"), (Join-Path $Kit "bin\Qt6Widgets.dll") $LibDir
    Copy-Item (Join-Path $Mingw "bin\libwinpthread-1.dll") $LibDir -ErrorAction SilentlyContinue
}

Write-Host "==> [4/4] Deploying QPA platform plugin (qwindows)"
$PlatformsDir = Join-Path $LibDir "plugins\platforms"
New-Item -ItemType Directory -Force -Path $PlatformsDir | Out-Null
Copy-Item (Join-Path $Kit "plugins\platforms\qwindows.dll") $PlatformsDir
# v0.7.2: 部署 SQL 驱动插件（SQLite 等）
$SqlDriversDir = Join-Path $LibDir "plugins\sqldrivers"
New-Item -ItemType Directory -Force -Path $SqlDriversDir | Out-Null
Get-ChildItem (Join-Path $Kit "plugins\sqldrivers") -Filter "qsqlite.dll" -ErrorAction SilentlyContinue | Copy-Item -Destination $SqlDriversDir
$qtConf = @"
[Paths]
Plugins = plugins
"@
Set-Content -Path (Join-Path $LibDir "qt.conf") -Value $qtConf -Encoding ascii

Write-Host "==> [5/5] Deploying license notices into lib (LGPL compliance)"
Copy-Item (Join-Path $Root "LGPL-3.0.txt"), (Join-Path $Root "THIRD-PARTY-NOTICES.md"), (Join-Path $Root "LICENSE.md"), (Join-Path $Root "LICENSE") $LibDir -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "Build OK"
Write-Host "  Java bytecode : $OutDir"
Write-Host "  Dynamic lib   : $(Join-Path $LibDir 'jqt.dll')"
Write-Host "  Run demo      : .\run.ps1"
