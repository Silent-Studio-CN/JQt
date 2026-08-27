# ============================================================================
# run.ps1 - run the JQt demo
#
# Usage:
#   .\run.ps1                    normal demo (close the window to exit)
#   .\run.ps1 -AutoClose 3000    auto close after 3 s (automation)
#   .\run.ps1 -Class org.jqt.JQtDemo   pick the entry class
#
# NOTE: this file must stay ASCII-only (Windows PowerShell 5.1).
# ============================================================================

param(
    [int]$AutoClose = -1,
    [string]$Class = "org.jqt.JQtDemo",
    [string]$JDK = "C:\Program Files\Java\latest\jdk-26",
    [string]$AnimTheme = "",
    [string]$Qss = "",
    [string]$Rhi = "",
    [switch]$Fluent
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
    "-cp", $Out
)
if ($AutoClose -gt 0) { $javaArgs += "-Djqt.autoClose=$AutoClose" }
if ($AnimTheme -ne "") { $javaArgs += "-Djqt.animTheme=$AnimTheme" }
if ($Qss -ne "") { $javaArgs += "-Djqt.qss=$Qss" }
if ($Rhi -ne "") { $javaArgs += "-Djqt.rhi=$Rhi" }
if ($Fluent) { $javaArgs += "-Djqt.demoFluent=1" }
$javaArgs += $Class

& "$JDK\bin\java.exe" @javaArgs
exit $LASTEXITCODE
