$ErrorActionPreference = "Stop"
Set-Location "D:\SilentStudio\JQt - Dev"
$f = "native\jqt_bridge.cpp"
$t = Get-Content $f -Raw

# 1. include 守卫
$t = $t.Replace("#include <QSerialPort>" + [char]13 + [char]10 + "#include <QSerialPortInfo>", "#if !defined(__ANDROID__)" + [char]13 + [char]10 + "#include <QSerialPort>" + [char]13 + [char]10 + "#include <QSerialPortInfo>" + [char]13 + [char]10 + "#endif // !__ANDROID__ (qtserialport android 模块后续安装)")

# 2. 39 个 JNI 函数守卫（从后往前处理避免偏移）
$fns = @()
$lines = Get-Content $f
for ($i = 0; $i -lt $lines.Count; $i++) {
    if ($lines[$i] -match "JNIEXPORT.*Java_org_jqt_QSerialPort_") { $fns += $i }  # 0-based
}
# 按行号倒序包守卫
$fns = $fns | Sort-Object -Descending
$count = 0
foreach ($li in $fns) {
    # 找函数结束（从函数行起找 "}" 行）
    $end = $li
    $depth = 0
    for ($j = $li; $j -lt $lines.Count; $j++) {
        $open = ([regex]::Matches($lines[$j], "\{")).Count
        $close = ([regex]::Matches($lines[$j], "\}")).Count
        $depth += $open - $close
        if ($depth -le 0 -and $j -gt $li) { $end = $j; break }
    }
    # 插入守卫（行数组操作：在 $li 前插 #if，在 $end 后插 #endif）
    $guard = "#if !defined(__ANDROID__) // qtserialport android 模块后续安装"
    $endif = "#endif // !__ANDROID__"
    $lines = $lines[0..($li-1)] + $guard + $lines[$li..$end] + $endif + $lines[($end+1)..($lines.Count-1)]
    $count++
}
Set-Content -Path $f -Value $lines -Encoding UTF8
Write-Host ("guarded fns: " + $count)
