@echo off
rem ============================================================
rem test-anim.bat - JQt Fluent animation theme tester
rem
rem Usage:
rem   test-anim.bat                 interactive menu
rem   test-anim.bat fast            run one theme directly
rem   test-anim.bat all             run all four in sequence
rem   test-anim.bat relaxed -AutoClose 6000
rem                                 pass extra args through
rem
rem NOTE: keep this file ASCII-only (cmd GBK console).
rem ============================================================
setlocal
cd /d "%~dp0"

if not exist "lib\jqt.dll" (
    echo [ERROR] jqt.dll not found - run build.ps1 first
    pause
    exit /b 1
)

if "%~1"=="all" goto all
if "%~1"=="" goto menu
set THEME=%~1
goto run

:all
for %%T in (default fast relaxed off) do (
    echo.
    echo ================================================
    echo   Theme: %%T
    echo ================================================
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-fluent.ps1" -AnimTheme %%T -AutoClose 5000
)
goto end

:menu
echo.
echo ==================================================
echo   JQt Fluent Animation Theme Tester
echo ==================================================
echo   1. DEFAULT   (1.0x, OutCubic)
echo   2. FAST      (0.65x, snappy)
echo   3. RELAXED   (1.6x, OutQuint)
echo   4. OFF       (animations disabled)
echo   5. ALL       (run all four in sequence)
echo   6. Exit
echo ==================================================
choice /c 123456 /n /m "Pick [1-6]: "
set THEME=
if %errorlevel%==1 set THEME=default
if %errorlevel%==2 set THEME=fast
if %errorlevel%==3 set THEME=relaxed
if %errorlevel%==4 set THEME=off
if %errorlevel%==5 goto all
if %errorlevel%==6 exit /b 0

:run
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-fluent.ps1" -AnimTheme %THEME% %2 %3

:end
endlocal
