# ============================================================================
# run-webview-demo.ps1 - open the JQt WebView2 demo window
#
# Double-click run-webview-demo.cmd (recommended), or:
#   powershell -NoProfile -ExecutionPolicy Bypass -File .\run-webview-demo.ps1
#
# The window stays open until YOU close it (closing it exits the process).
#
# NOTE: this file must stay ASCII-only (Windows PowerShell 5.1 reads BOM-less
#       files as ANSI and would garble non-ASCII text).
# ============================================================================

param(
    [string]$JDK = ""
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Lib  = Join-Path $Root "lib"
$Out  = Join-Path $Root "out"
$Main = "org.jqt.JQtWebViewDemo"

function Fail([string]$msg) {
    Write-Host ""
    Write-Host "  [X] $msg" -ForegroundColor Red
    Write-Host ""
    exit 1
}

Write-Host "==> JQt WebView demo"

# ---- 1) Are the build artifacts there? If not, say so - do not fail silently.
if (-not (Test-Path (Join-Path $Lib "jqt.dll"))) {
    Fail "lib\jqt.dll not found - run .\build.ps1 first (native library not built)."
}
if (-not (Test-Path (Join-Path $Out "org\jqt\JQtWebViewDemo.class"))) {
    Fail "out\org\jqt\JQtWebViewDemo.class not found - run .\build.ps1 first (Java not compiled)."
}

# ---- 2) Pick a JDK: prefer the repo-local .jdk17 (self-contained), then system JDK / PATH.
if ($JDK -eq "") {
    $candidates = @(
        (Join-Path $Root ".jdk17\jdk-17.0.2"),
        "C:\Program Files\Java\latest\jdk-26"
    )
    foreach ($c in $candidates) {
        if (Test-Path (Join-Path $c "bin\java.exe")) { $JDK = $c; break }
    }
}
if ($JDK -ne "" -and (Test-Path (Join-Path $JDK "bin\java.exe"))) {
    $Java = Join-Path $JDK "bin\java.exe"
} else {
    $cmd = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($null -eq $cmd) {
        Fail "No Java runtime found (looked for .jdk17\jdk-17.0.2 and java.exe on PATH)."
    }
    $Java = $cmd.Source
}

# ---- 3) Let the Windows loader find the Qt6*.dll next to jqt.dll,
#         and let Qt find the QPA platform plugin.
$env:PATH = "$Lib;$env:PATH"
$env:QT_PLUGIN_PATH = $Lib
$env:QT_QPA_PLATFORM_PLUGIN_PATH = Join-Path $Lib "platforms"

# Java writes UTF-8 to stdout; the console defaults to GBK, so set this or it garbles.
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# ---- 4) Run.
$javaArgs = @(
    "-Djava.library.path=$Lib",
    "-Dfile.encoding=UTF-8",
    "-Dstdout.encoding=UTF-8",
    "-cp", $Out,
    $Main
)

Write-Host "==> java   : $Java"
Write-Host "==> lib    : $Lib"
Write-Host "==> class  : $Main"
Write-Host ""

& $Java @javaArgs
$code = $LASTEXITCODE

Write-Host ""
if ($code -eq 0) {
    Write-Host "==> demo exited normally."
} else {
    Write-Host "==> demo exited with code $code."
}
exit $code
