$env:JAVA_HOME = "C:\Program Files\Java\jdk-26.0.2.1"
$env:JAVA_HOME | Write-Host
& "C:\AndroidSdk\cmdline-tools\latest\bin\sdkmanager.bat" --version 2>&1 | Select-Object -Last 5
Write-Host ("exit=" + $LASTEXITCODE)
