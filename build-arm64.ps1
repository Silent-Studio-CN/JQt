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
Write-Host "==> [3/5] Compiling native bridge (jqt.dll, ARM64)"
$clArgs = @(
    "/nologo", "/std:c++17", "/O2", "/LD", "/EHsc", "/MD", "/W3", "/Zc:__cplusplus", "/permissive-",
    "/I", (Join-Path $JDK "include"),
    "/I", (Join-Path $JDK "include\win32"),
    "/I", (Join-Path $QtRoot "include"),
    "/I", (Join-Path $QtRoot "include\QtWidgets"),
    "/I", (Join-Path $QtRoot "include\QtGui"),
    "/I", (Join-Path $QtRoot "include\QtCore"),
    "/I", (Join-Path $QtRoot "include\QtPrintSupport"),
    "/I", (Join-Path $QtRoot "include\QtSql"),
    "/I", (Join-Path $Root "native"),
    (Join-Path $Root "native\jqt_bridge.cpp"),
    ("/Fe:" + (Join-Path $LibDir "jqt.dll")),
    "/link",
    (Join-Path $QtRoot "lib\Qt6Widgets.lib"),
    (Join-Path $QtRoot "lib\Qt6Gui.lib"),
    (Join-Path $QtRoot "lib\Qt6Core.lib"),
    (Join-Path $QtRoot "lib\Qt6PrintSupport.lib"),
    (Join-Path $QtRoot "lib\Qt6Sql.lib"),
    "ole32.lib", "user32.lib", "dwmapi.lib", "shell32.lib", "gdi32.lib",
    "advapi32.lib", "ws2_32.lib", "winmm.lib", "netapi32.lib", "userenv.lib",
    "version.lib", "comdlg32.lib", "oleaut32.lib"
)
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
