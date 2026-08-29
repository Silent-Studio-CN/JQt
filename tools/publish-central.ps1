# ============================================================
# JQt — Maven Central 发布脚本（固化流程，v1.0）
# 用法:
#   .\tools\publish-central.ps1 -Version 0.7.5
# 前置:
#   .signing/ 工具链（jqt-sec.asc 私钥 + Signer.class + bc*.jar）
#   .signing/creds.txt:  SONATYPE_USER=xxx / SONATYPE_PASS=xxx / SIGN_PASS=xxx
# 流程:
#   构建(-Pversion) -> 组装 bundle -> md5/sha1 -> GPG 签名 -> 上传16文件
#   -> manual 转入 Portal -> 提示网页 Publish + jqt.yaml 更新
# 版本约定: Central 用纯数字版本(0.7.5)；GitHub/JitPack 用 v0.7.5-代号
# ============================================================
param(
    [Parameter(Mandatory = $true)][string]$Version,
    [string]$GroupId = "io.github.silent-xiaomiao",
    [string]$ArtifactId = "jqt",
    [string]$RepoRoot = "D:\SilentStudio\JQt - Dev"
)

$ErrorActionPreference = "Stop"
$SignDir = Join-Path $RepoRoot ".signing"
$CredsFile = Join-Path $SignDir "creds.txt"

# ---------- 1. 凭据 ----------
if (-not (Test-Path $CredsFile)) {
    throw "凭据文件缺失: $CredsFile （格式: SONATYPE_USER= / SONATYPE_PASS= / SIGN_PASS=）"
}
$creds = @{}
Get-Content $CredsFile | ForEach-Object {
    if ($_ -match '^([A-Z_]+)=(.*)$') { $creds[$matches[1]] = $matches[2].Trim() }
}
$env:SONATYPE_USER = $creds["SONATYPE_USER"]
$env:SONATYPE_PASS = $creds["SONATYPE_PASS"]
$signPass = $creds["SIGN_PASS"]
if (-not $env:SONATYPE_USER -or -not $env:SONATYPE_PASS) { throw "creds.txt 缺少 SONATYPE_USER/SONATYPE_PASS" }
if (-not $signPass) { throw "creds.txt 缺少 SIGN_PASS" }

# ---------- 2. 构建（JDK17 + Gradle 8.9，-Pversion 注入） ----------
$env:JAVA_HOME = Join-Path $RepoRoot ".jdk17\jdk-17.0.2"
$gradle = Join-Path $RepoRoot ".gradle89\gradle-8.9\bin\gradle.bat"
if (-not (Test-Path $gradle)) { throw "Gradle 缺失: $gradle" }
Write-Host "[1/6] 构建 -Pversion=$Version ..."
Push-Location $RepoRoot
try {
    & $gradle -p $RepoRoot --no-daemon "-Pversion=$Version" clean jar sourcesJar javadocJar generatePomFileForMavenPublication 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Gradle 构建失败 (exit $LASTEXITCODE)" }
} finally { Pop-Location }

# ---------- 3. 组装 bundle + 校验和 ----------
$out = Join-Path $SignDir "bundle\$($GroupId -replace '\.','\')\$ArtifactId\$Version"
New-Item -ItemType Directory -Force -Path $out | Out-Null
Copy-Item (Join-Path $RepoRoot "build\libs\$ArtifactId-$Version.jar") "$out\$ArtifactId-$Version.jar"
Copy-Item (Join-Path $RepoRoot "build\libs\$ArtifactId-$Version-sources.jar") "$out\$ArtifactId-$Version-sources.jar"
Copy-Item (Join-Path $RepoRoot "build\libs\$ArtifactId-$Version-javadoc.jar") "$out\$ArtifactId-$Version-javadoc.jar"
Copy-Item (Join-Path $RepoRoot "build\publications\maven\pom-default.xml") "$out\$ArtifactId-$Version.pom"
Write-Host "[2/6] bundle 就绪: $out"
foreach ($f in Get-ChildItem $out -File | Where-Object { $_.Extension -notin @(".asc",".md5",".sha1") }) {
    Set-Content -Path "$($f.FullName).md5"  -Value ((Get-FileHash $f.FullName -Algorithm MD5).Hash.ToLower()) -NoNewline -Encoding ascii
    Set-Content -Path "$($f.FullName).sha1" -Value ((Get-FileHash $f.FullName -Algorithm SHA1).Hash.ToLower()) -NoNewline -Encoding ascii
}
Write-Host "[3/6] md5/sha1 已生成"

# ---------- 4. GPG 签名（BouncyCastle，无 gpg 依赖） ----------
$java26 = "C:\Program Files\Java\latest\jdk-26\bin\java.exe"
$cp = "bcpg-jdk18on-1.80.jar;bcprov-jdk18on-1.80.jar;bcutil-jdk18on-1.80.jar"
Push-Location $SignDir
try {
    foreach ($f in Get-ChildItem $out -File | Where-Object { $_.Extension -notin @(".asc",".md5",".sha1") }) {
        & $java26 -cp ".;$cp" Signer jqt-sec.asc $signPass $f.FullName "$($f.FullName).asc" 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "签名失败: $($f.Name)" }
    }
} finally { Pop-Location }
Write-Host "[4/6] 4 个文件已签名"

# ---------- 5. 上传 16 文件到 OSSRH 兼容层 ----------
$deployBase = "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/$($GroupId -replace '\.','/')/$ArtifactId/$Version"
$names = @("$ArtifactId-$Version.pom","$ArtifactId-$Version.pom.asc","$ArtifactId-$Version.pom.md5","$ArtifactId-$Version.pom.sha1",
           "$ArtifactId-$Version.jar","$ArtifactId-$Version.jar.asc","$ArtifactId-$Version.jar.md5","$ArtifactId-$Version.jar.sha1",
           "$ArtifactId-$Version-sources.jar","$ArtifactId-$Version-sources.jar.asc","$ArtifactId-$Version-sources.jar.md5","$ArtifactId-$Version-sources.jar.sha1",
           "$ArtifactId-$Version-javadoc.jar","$ArtifactId-$Version-javadoc.jar.asc","$ArtifactId-$Version-javadoc.jar.md5","$ArtifactId-$Version-javadoc.jar.sha1")
$fail = 0
foreach ($n in $names) {
    $c = curl.exe -s -o NUL -w "%{http_code}" -u "$($env:SONATYPE_USER):$($env:SONATYPE_PASS)" --upload-file "$out\$n" "$deployBase/$n"
    Write-Host "    $n => $c"
    if ($c -ne "201") { $fail++ }
}
if ($fail -gt 0) { throw "上传失败 $fail 个文件" }
Write-Host "[5/6] 16 文件全部 201"

# ---------- 6. 转入 Central Portal ----------
$auth = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("$($env:SONATYPE_USER):$($env:SONATYPE_PASS)"))
$url = "https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/$GroupId?publishing_type=user_managed"
$resp = curl.exe -s -w "|HTTP:%{http_code}" -X POST -H "Authorization: Bearer $auth" -H "Content-Type: application/json" -d "{}" $url
Write-Host "[6/6] 转入 Portal: $resp"
if ($resp -notmatch 'HTTP:200') { throw "转入失败" }

Write-Host ""
Write-Host "=============================================="
Write-Host "完成！剩余人工步骤："
Write-Host "  1. https://central.sonatype.com/publishing/deployments -> Publish (VALIDATED)"
Write-Host "  2. 验证: https://repo1.maven.org/maven2/$($GroupId -replace '\.','/')/$ArtifactId/$Version/$ArtifactId-$Version.jar"
Write-Host "  3. 更新 jqt.yaml (index 仓库): 追加版本 + latest + docs"
Write-Host "=============================================="
