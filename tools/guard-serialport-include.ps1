$ErrorActionPreference = "Stop"
Set-Location "D:\SilentStudio\JQt - Dev"
$f = "native\jqt_bridge.cpp"
$t = Get-Content $f -Raw
$old = "#include <QSerialPort>" + [char]13 + [char]10 + "#include <QSerialPortInfo>" + [char]13 + [char]10 + "#include <QtSerialPort/QSerialPort>" + [char]13 + [char]10 + "#include <QtSerialPort/QSerialPortInfo>"
$new = "#if !defined(__ANDROID__) // qtserialport android 模块后续安装" + [char]13 + [char]10 + $old + [char]13 + [char]10 + "#endif // !__ANDROID__"
if (-not $t.Contains($old)) { throw "include 块未匹配" }
$t = $t.Replace($old, $new)
Set-Content -Path $f -Value $t -Encoding UTF8 -NoNewline
Write-Host "include guarded"
