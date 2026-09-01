$ErrorActionPreference = "Continue"
$NDK = "C:\AndroidSdk\ndk\27.2.12479018"
$QT = "C:\Qt\6.11.2\android_arm64_v8a"
$PRE = "$NDK\toolchains\llvm\prebuilt\windows-x86_64"
$CLANG = "$PRE\bin\aarch64-linux-android24-clang++.cmd"
$SYSROOT = "$PRE\sysroot"
$REPO = "C:\JQt"

Write-Host "=== Android arm64 编译检查 (fsyntax-only) ==="
$args = @("-target", "aarch64-linux-android24", "--sysroot=$SYSROOT", "-std=c++17", "-fsyntax-only")
$args += @("-I", "$QT\include", "-I", "$QT\include\QtWidgets", "-I", "$QT\include\QtGui", "-I", "$QT\include\QtCore")
$args += @("-I", "$QT\include\QtPrintSupport", "-I", "$QT\include\QtSql", "-I", "$QT\include\QtSerialPort", "-I", "$QT\include\QtOpenGLWidgets", "-I", "$QT\include\QtOpenGL")
$args += @("-I", "$REPO\native", "-I", "$REPO\native\generated")
$args += @("$REPO\native\jqt_bridge.cpp")
& $CLANG @args 2>&1 | Select-Object -First 30
Write-Host "CLANG exit: $LASTEXITCODE"
