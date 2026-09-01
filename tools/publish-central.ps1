# ============================================================
# JQt — Maven Central 发布脚本 v2.0（Portal 原生流程，固化 2026-09-01）
# 用法:  .\tools\publish-central.ps1 -Version 0.7.5 [-DryRun]
# 前置:  .signing/（jqt-sec.asc + Signer.class + bc*.jar）
#        .signing/creds.txt: SONATYPE_USER= / SONATYPE_PASS= / SIGN_PASS= / SONATYPE_TOKEN_B64=
# 流程:  构建 -> bundle -> md5/sha1 -> GPG 签名 -> maven 路径 zip
#        -> portal upload (bundle=@zip) -> 轮询 VALIDATED -> POST publish -> 确认 PUBLISHED
# 版本约定: Central 用纯数字(0.7.5)；GitHub/JitPack 用 v0.7.5-代号
# 踩坑（2026-09）:
#   - OSSRH staging manual/upload 转入 400（profile 授权，旧流程失效）
#   - portal upload: 字段名 bundle；zip 内文件必须按 Maven 路径组织
#     (groupId/artifactId/version/文件名)，zip 根放文件 -> "File path ./ is not valid"
#   - 认证: Bearer <SONATYPE_TOKEN_B64>（Portal User Token base64）
# ============================================================
param(
    [Parameter(Mandatory = $true)][string]$Version,
    [string]$GroupId = "io.github.silent-xiaomiao",
    [string]$ArtifactId = "jqt",
    [string]$RepoRoot = "D:\SilentStudio\JQt - Dev",
    [string]$Jdk = "C:\Program Files\Java\latest\jdk-26",
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$SignDir = Join-Path $RepoRoot ".signing"
$CredsFile = Join-Path $SignDir "creds.txt"

# ---------- 1. 凭据 ----------
if (-not (Test-Path $CredsFile)) { throw "凭据文件缺失: $CredsFile" }
$creds = @{}
Get-Content $CredsFile | ForEach-Object { if ($_ -match "^([A-Z0-9_]+)=(.*)$") { $creds[$matches[1]] = $matches[2].Trim() } }
$signPass = $creds["SIGN_PASS"]
$tokenB64 = $creds["SONATYPE_TOKEN_B64"]
if (-not $signPass) { throw "SIGN_PASS 缺失" }
if (-not $tokenB64) { throw "SONATYPE_TOKEN_B64 缺失（Portal User Token base64）" }

# ---------- 2. Gradle 构建（pom） ----------
Write-Host "[1/7] Gradle 构建 -Pversion=$Version"
$gradle = Get-ChildItem (Join-Path $RepoRoot ".gradle89") -Recurse -Filter "gradle.bat" -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $gradle) { $gradle = Get-ChildItem (Join-Path $RepoRoot ".gradle") -Recurse -Filter "gradle.bat" -ErrorAction SilentlyContinue | Select-Object -First 1 }
if (-not $gradle) { throw "gradle.bat 未找到（.gradle89/.gradle）" }
Push-Location $RepoRoot
try {
    & $gradle.FullName generatePomFileForMavenPublication -Pversion=$Version --no-daemon 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "gradle 构建失败" }
} finally { Pop-Location }

# ---------- 3. bundle 组装 ----------
$gPath = $GroupId.Replace(".", "\")
$out = Join-Path $SignDir ("bundle" + "\" + $gPath)
$out = Join-Path $out "$ArtifactId\$Version"
New-Item -ItemType Directory -Force -Path $out | Out-Null
$libs = Join-Path $RepoRoot "build\libs"
# jar/sources/javadoc 由 gradle 生成（纯数字版本名，build/libs）
Push-Location $RepoRoot
try {
    & $gradle.FullName jar sourcesJar javadocJar -Pversion=$Version --no-daemon 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "gradle jar 构建失败" }
} finally { Pop-Location }
Copy-Item (Join-Path $libs "$ArtifactId-$Version.jar") $out
Copy-Item (Join-Path $libs "$ArtifactId-$Version-sources.jar") $out
Copy-Item (Join-Path $libs "$ArtifactId-$Version-javadoc.jar") $out
Copy-Item (Join-Path $RepoRoot "build\publications\maven\pom-default.xml") "$out\$ArtifactId-$Version.pom"
Write-Host "[2/7] bundle 就绪: $out"

# ---------- 4. md5/sha1 ----------
foreach ($f in Get-ChildItem $out -File | Where-Object { $_.Extension -notin @(".asc",".md5",".sha1") }) {
    Set-Content -Path "$($f.FullName).md5"  -Value ((Get-FileHash $f.FullName -Algorithm MD5).Hash.ToLower()) -NoNewline -Encoding ascii
    Set-Content -Path "$($f.FullName).sha1" -Value ((Get-FileHash $f.FullName -Algorithm SHA1).Hash.ToLower()) -NoNewline -Encoding ascii
}
Write-Host "[3/7] md5/sha1 已生成"

# ---------- 5. GPG 签名（BouncyCastle） ----------
$java = Join-Path $Jdk "bin\java.exe"
$cp = "bcpg-jdk18on-1.80.jar;bcprov-jdk18on-1.80.jar;bcutil-jdk18on-1.80.jar"
Push-Location $SignDir
try {
    foreach ($f in Get-ChildItem $out -File | Where-Object { $_.Extension -notin @(".asc",".md5",".sha1") }) {
        & $java -cp ".;$cp" Signer jqt-sec.asc $signPass $f.FullName "$($f.FullName).asc" 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "签名失败: $($f.Name)" }
    }
} finally { Pop-Location }
Write-Host "[4/7] 签名完成"

# ---------- 6. maven 路径 zip（.NET ZipFile） ----------
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zipPath = Join-Path $SignDir "$ArtifactId-$Version-central.zip"
Remove-Item $zipPath -Force -ErrorAction SilentlyContinue
$zip = [System.IO.Compression.ZipFile]::Open($zipPath, [System.IO.Compression.ZipArchiveMode]::Create)
$prefix = ($GroupId.Replace(".", "/")) + "/" + $ArtifactId + "/" + $Version
Get-ChildItem $out -File | ForEach-Object {
    [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $_.FullName, "$prefix/$($_.Name)") | Out-Null
}
$zip.Dispose()
Write-Host "[5/7] zip 就绪: $zipPath ($((Get-Item $zipPath).Length) bytes)"

if ($DryRun) {
    Write-Host "[DRY-RUN] 停止（未上传）。zip 条目:"
    $z = [System.IO.Compression.ZipFile]::OpenRead($zipPath)
    $z.Entries | Select-Object -First 3 -ExpandProperty FullName
    $z.Dispose()
    exit 0
}

# ---------- 7. portal upload -> 轮询 -> publish ----------
$api = "https://central.sonatype.com/api/v1/publisher"
$auth = "Authorization: Bearer $tokenB64"
Write-Host "[6/7] portal upload..."
$resp = curl.exe -s -w "|HTTP:%{http_code}" -X POST -H $auth -F "bundle=@$zipPath" "$api/upload?name=$ArtifactId-$Version&publishingType=USER_MANAGED"
if ($resp -notmatch "HTTP:201") { throw "上传失败: $resp" }
$deployId = ($resp -split "|")[0].Trim()
Write-Host "  deployment: $deployId"

$state = ""
for ($i = 1; $i -le 20; $i++) {
    Start-Sleep -Seconds 15
    $list = curl.exe -s -H $auth "$api/deployments" | ConvertFrom-Json
    $d = $list.deployments | Where-Object { $_.deploymentId -eq $deployId }
    if (-not $d) { Write-Host "  [$i] 未找到 deployment"; continue }
    $state = $d.deploymentState
    Write-Host "  [$i] state=$state"
    if ($state -eq "FAILED") {
        $d.deploymentComponents | ForEach-Object { $_.errors | Select-Object -First 5 | ForEach-Object { Write-Host "    ERR: $_" } }
        throw "校验失败（见上）"
    }
    if ($state -eq "VALIDATED") { break }
}
if ($state -ne "VALIDATED") { throw "超时：deployment 未达 VALIDATED（当前 $state）" }

Write-Host "[7/7] publish..."
$p = curl.exe -s -w "|HTTP:%{http_code}" -X POST -H $auth "$api/deployment/$deployId"
if ($p -notmatch "HTTP:204") { throw "publish 失败: $p" }
Write-Host "  204 已接受（PUBLISHING -> PUBLISHED）"

for ($i = 1; $i -le 12; $i++) {
    Start-Sleep -Seconds 15
    $list = curl.exe -s -H $auth "$api/deployments" | ConvertFrom-Json
    $d = $list.deployments | Where-Object { $_.deploymentId -eq $deployId }
    if ($d -and $d.deploymentState -eq "PUBLISHED") { Write-Host ("PUBLISHED OK " + $GroupId + ":" + $ArtifactId + ":" + $Version); break }
    if ($d) { Write-Host "  等待 PUBLISHED... ($($d.deploymentState))" }
}

Write-Host ""
Write-Host "=============================================="
Write-Host ("完成！验证: https://repo1.maven.org/maven2/" + $gPath + "/" + $ArtifactId + "/" + $Version + "/" + $ArtifactId + "-" + $Version + ".jar")
Write-Host "提醒: 更新 jqt.yaml（index 仓库）版本条目"
Write-Host "=============================================="
