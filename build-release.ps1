# ============================================================================
# build-release.ps1 - build the jqt.jar and a platform release package
#
# Produces:
#   dist/jqt-<VERSION>.jar            platform-independent Java API jar
#   dist/jqt-<VERSION>-<platform>.zip full platform package (jar + dll + runtime + docs)
#
# Usage:
#   .\build-release.ps1               (uses local JDK 26 and current lib/)
# ============================================================================

param(
    [string]$JDK = "C:\Program Files\Java\latest\jdk-26"
)

$ErrorActionPreference = "Stop"

$Root   = Split-Path -Parent $MyInvocation.MyCommand.Path
$Out    = Join-Path $Root "out"
$Lib    = Join-Path $Root "lib"
$Dist   = Join-Path $Root "dist"
$Version = (Get-Content (Join-Path $Root "VERSION") -Raw).Trim()
$Platform = "windows-x64"

if (-not (Test-Path (Join-Path $Lib "jqt.dll"))) {
    throw "lib\jqt.dll not found - run .\build.ps1 first"
}

New-Item -ItemType Directory -Force -Path $Dist | Out-Null

Write-Host "==> Packaging jqt-$Version.jar"
& "$JDK\bin\jar.exe" --create --file (Join-Path $Dist "jqt-$Version.jar") -C $Out .
if ($LASTEXITCODE -ne 0) { throw "jar failed" }

Write-Host "==> Assembling $Platform package"
$Pkg = Join-Path $Dist "jqt-$Version-$Platform"
New-Item -ItemType Directory -Force -Path $Pkg | Out-Null

# Java API jar + native lib + Qt runtime (lib/ is self-contained) + docs
Copy-Item (Join-Path $Dist "jqt-$Version.jar") $Pkg
Copy-Item (Join-Path $Lib "jqt.dll") $Pkg
Get-ChildItem $Lib -File | Where-Object { $_.Name -notmatch "^jqt\.dll$" } | Copy-Item -Destination $Pkg
Get-ChildItem $Lib -Directory | Copy-Item -Destination $Pkg -Recurse

# 文档（双语）
Copy-Item (Join-Path $Root "README.md"), (Join-Path $Root "LICENSE.md"), (Join-Path $Root "LICENSE"),
          (Join-Path $Root "LGPL-3.0.txt"), (Join-Path $Root "THIRD-PARTY-NOTICES.md"),
          (Join-Path $Root "CHANGELOG.md"), (Join-Path $Root "VERSION"),
          (Join-Path $Root "docsqt-mapping.md"), (Join-Path $Root "docsapi-implemented.md") $Pkg

# 示例代码（用户快速上手）
New-Item -ItemType Directory -Force -Path (Join-Path $Pkg "examples") | Out-Null
Copy-Item (Join-Path $Root "java\org\jqt\JQtDemo.java") (Join-Path $Pkg "examples\JQtDemo.java")

Write-Host "==> Compressing"
Compress-Archive -Path (Join-Path $Pkg "*") -DestinationPath (Join-Path $Dist "jqt-$Version-$Platform.zip") -Force
Remove-Item $Pkg -Recurse -Force

Write-Host ""
Write-Host "Release package ready:"
Get-ChildItem $Dist | Select-Object Name, Length
