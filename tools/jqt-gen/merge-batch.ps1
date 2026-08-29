$ErrorActionPreference = "Stop"
$log = "D:\SilentStudio\JQt - Dev\merge-log.txt"
function W($m) { $m | Out-File -FilePath $log -Append -Encoding UTF8 }
"start $(Get-Date -Format o)" | Out-File -FilePath $log -Encoding UTF8
try {
Set-Location "D:\SilentStudio\JQt - Dev"
$nl = [char]13 + [char]10
$classes = @("QLabel","QPushButton","QLineEdit","QComboBox","QProgressBar","QGroupBox","QFrame","QMainWindow","QToolBar","QStatusBar","QMenu","QAction","QSplitter","QStackedWidget")
$javaMark = "// ---- 生成器批次（jqt-gen 自动生成，直传型） ----"
$cppMark = "// 生成器批次（jqt-gen 自动生成，直传型）"
$javaDone = @(); $cppDone = @()

foreach ($c in $classes) {
    W "== $c =="
    $jpart = "tools\jqt-gen\generated\${c}.java.part"
    $npart = "tools\jqt-gen\generated\${c}.native.part"
    if (-not (Test-Path $jpart)) { W "  skip: no jpart"; continue }
    $jtext = Get-Content $jpart -Raw
    $ntext = Get-Content $npart -Raw
    if ([string]::IsNullOrWhiteSpace($jtext)) { W "  skip: empty jtext"; continue }

    $jfile = "java\org\jqt\${c}.java"
    if (Test-Path $jfile) {
        $src = Get-Content $jfile -Raw
        if ($src.Contains($javaMark)) { throw "${c}.java: 已存在批次标记" }
        $trimmed = $src.TrimEnd()
        if (-not $trimmed.EndsWith("}")) { throw "${c}.java: 类尾不是 }" }
        $idx = $trimmed.LastIndexOf("}")
        $new = $trimmed.Substring(0, $idx) + $nl + $javaMark + $jtext + $nl + $trimmed.Substring($idx)
        Set-Content -Path $jfile -Value $new -Encoding UTF8 -NoNewline
        $javaDone += $c
        W "  java ok"
    } else {
        $head = "package org.jqt;" + $nl + $nl + "/** ${c}（Qt Widgets，JQt 绑定）。 */" + $nl + "public class ${c} extends QWidget {" + $nl + "    private final long nativeHandle;" + $nl + $nl + "    public ${c}(long nativeHandle) {" + $nl + "        this.nativeHandle = nativeHandle;" + $nl + "    }" + $nl + $javaMark + $jtext + $nl + "}" + $nl
        Set-Content -Path $jfile -Value $head -Encoding UTF8 -NoNewline
        $javaDone += "$c(new)"
        W "  java new ok"
    }

    $bfile = "native\jqt_bridge.cpp"
    $bsrc = Get-Content $bfile -Raw
    $firstFn = [regex]::Match($ntext, 'Java_org_jqt_\w+').Value; if ($firstFn -ne '' -and $bsrc.Contains($firstFn)) { throw "bridge: ${c} 批次已存在" }
    Add-Content -Path $bfile -Value ($nl + $cppMark + $ntext) -Encoding UTF8
    $cppDone += $c
    W "  bridge ok"
}

W "JAVA: $($javaDone -join ', ')"
W "CPP : $($cppDone -join ', ')"
} catch { W ('EXCEPTION: ' + $_) }
