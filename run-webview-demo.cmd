@echo off
REM ===========================================================================
REM run-webview-demo.cmd - double-click entry for the JQt WebView2 demo window.
REM
REM Just wraps run-webview-demo.ps1 (which does the real work: PATH /
REM QT_PLUGIN_PATH / -Djava.library.path / -cp out).
REM
REM On failure the window would close instantly, so we pause - that way the
REM "run build.ps1 first" / "install WebView2 Runtime" messages stay readable.
REM ===========================================================================
setlocal

set "SCRIPT=%~dp0run-webview-demo.ps1"

powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT%"
set "CODE=%ERRORLEVEL%"

if not "%CODE%"=="0" (
    echo.
    echo   Demo exited with code %CODE%.
    pause
)

endlocal & exit /b %CODE%
