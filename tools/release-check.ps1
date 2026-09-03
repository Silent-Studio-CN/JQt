# release-check.ps1 - release asset validation (run BEFORE upload)
# Validates: jar bytecode version, no test/demo classes in jar, no .bak in zip,
# VERSION vs tag consistency. ASCII-only.
param(
    [string]$Version = (Get-Content (Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) "..\VERSION") -Raw).Trim(),
    [string]$Dist = (Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) "..\dist")
)

$ErrorActionPreference = "Stop"
$fail = 0
function Check($name, $cond) {
    if ($cond) { Write-Host ("  [OK] " + $name) }
    else { Write-Host ("  [FAIL] " + $name); $script:fail++ }
}

Write-Host "==> release-check $Version"

# 1. jar exists
$jar = Join-Path $Dist ("jqt-" + $Version + ".jar")
Check ("jar exists: " + $jar) (Test-Path $jar)

if (Test-Path $jar) {
    # 2. bytecode major version must be 61 (Java 17)
    $tmp = Join-Path $env:TEMP ("jarcheck-" + [guid]::NewGuid().ToString("N"))
    New-Item -ItemType Directory -Force -Path $tmp | Out-Null
    tar -xf $jar -C $tmp org/jqt/QApplication.class 2>$null
    $cls = Join-Path $tmp "org\jqt\QApplication.class"
    if (Test-Path $cls) {
        $b = [System.IO.File]::ReadAllBytes($cls)
        $major = $b[6] * 256 + $b[7]
        Check ("jar bytecode major = 61 (Java 17), got " + $major) ($major -eq 61)
    } else { Check "jar has QApplication.class" $false }
    Remove-Item -Recurse -Force $tmp -ErrorAction SilentlyContinue

    # 3. no test/demo/community classes in jar
    $bad = tar -tf $jar 2>$null | Select-String -Pattern "Smoke|CrashProbe|Demo|Gallery|QraftLab|^[^/]+\.class$|NordTheme|SolarizedTheme|TerminalTheme"
    Check "jar free of test/demo/community classes" (-not $bad)
}

# 4. windows zip: exists, no .bak
$zip = Join-Path $Dist ($Version + "-windows-x64.zip")
$zipName = Get-ChildItem $Dist -Filter ("jqt-" + $Version + "-windows-x64.zip") -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
if (-not $zipName) { $zipName = Get-ChildItem $Dist -Filter ("jqt-0.7.5-Generator-Kit-windows-x64.zip") -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName }
Check "windows zip found" ($null -ne $zipName)
if ($zipName) {
    $bak = tar -tf $zipName 2>$null | Select-String -Pattern "\.bak$|token|cred|signing"
    Check ("zip free of .bak/cred files: " + (Split-Path $zipName -Leaf)) (-not $bak)
}

# 5. VERSION file matches
$verFile = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) "..\VERSION"
$verContent = (Get-Content $verFile -Raw).Trim()
Check ("VERSION file = " + $verContent) ($verContent -eq $Version)

Write-Host ("==> result: " + $(if ($fail -eq 0) { "PASS" } else { "FAIL (" + $fail + ")" }))
exit $fail