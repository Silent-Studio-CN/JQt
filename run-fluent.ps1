# ============================================================================
# run-fluent.ps1 - launch the JQt Fluent demo (switch / animation / titlebar)
#
# Usage:
#   .\run-fluent.ps1                dark theme, close window to exit
#   .\run-fluent.ps1 -Theme light   light theme
#   .\run-fluent.ps1 -AutoClose 8000  auto close after 8 s (automation)
#   .\run-fluent.bat                double-click entry (same options)
#
# NOTE: this file must stay ASCII-only (Windows PowerShell 5.1).
# ============================================================================

param(
    [string]$Theme = "dark",
    [int]$AutoClose = -1,
    [string]$JDK = "C:\Program Files\Java\latest\jdk-26"
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Lib  = Join-Path $Root "lib"
$Out  = Join-Path $Root "out"

if (-not (Test-Path (Join-Path $Lib "jqt.dll"))) {
    throw "jqt.dll not found - run .\build.ps1 first"
}

# Let the Windows loader find Qt6*.dll next to jqt.dll
$env:PATH = "$Lib;$env:PATH"

# Point Qt at the QPA platform plugin (windeployqt deploys it to lib\platforms)
$env:QT_QPA_PLATFORM_PLUGIN_PATH = Join-Path $Lib "platforms"

# Make sure PS decodes Java's UTF-8 output correctly (console codepage is GBK)
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$javaArgs = @(
    "-Djava.library.path=$Lib",
    "-Dfile.encoding=UTF-8",
    "-Dstdout.encoding=UTF-8",
    "--enable-native-access=ALL-UNNAMED",
    "-Djqt.theme=$Theme",
    "-cp", $Out
)
if ($AutoClose -gt 0) { $javaArgs += "-Djqt.autoClose=$AutoClose" }
$javaArgs += "org.jqt.JQtFluentDemo"

& "$JDK\bin\java.exe" @javaArgs
exit $LASTEXITCODE
