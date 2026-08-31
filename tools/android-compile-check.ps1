$ErrorActionPreference = "Continue"
$NDK = "C:\AndroidSdk\ndk\27.2.12479018"
$QT = "C:\Qt\6.11.2\android_arm64_v8a"
$PRE = "$NDK\toolchains\llvm\prebuilt\windows-x86_64"
$CLANG = "$PRE\bin\aarch64-linux-android24-clang++.cmd"
$SYSROOT = "$PRE\sysroot"
$REPO = "C:\JQt"

Write-Host "=== Android arm64 编译检查 (fsyntax-only) ==="
& $CLANG -target aarch64-linux-android24 --sysroot=$SYSROOT -std=c++17 -fsyntax-only
  -I "$QT\include" -I "$QT\include\QtWidgets" -I "$QT\include\QtGui" -I "$QT\include\QtCore"
  -I "$QT\include\QtPrintSupport" -I "$QT\include\QtSql" -I "$QT\include\QtSerialPort"
  -I "$REPO\native" -I "$REPO\native\generated"
  "$REPO\native\jqt_bridge.cpp" 2>&1 | Select-Object -First 25
Write-Host "CLANG exit: $LASTEXITCODE"
