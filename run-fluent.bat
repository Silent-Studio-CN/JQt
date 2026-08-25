@echo off
rem Launch the JQt Fluent demo (switch / animation / titlebar)
rem Usage: run-fluent.bat [-Theme light] [-AutoClose 8000]
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-fluent.ps1" %*
endlocal
