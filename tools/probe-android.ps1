$urls = @(
    "https://download.qt.io/online/qtsdkrepository/windows_x86/android/qt6_6112_android/",
    "https://download.qt.io/online/qtsdkrepository/windows_x86/android_arm64_v8a/qt6_6112_android_arm64_v8a/",
    "https://download.qt.io/online/qtsdkrepository/windows_x86/android_arm64_v8a/",
    "https://download.qt.io/online/qtsdkrepository/windows_x86/android/qt6_6112/"
)
foreach ($u in $urls) {
    $code = curl.exe -s -o NUL -w "%{http_code}" --max-time 20 $u
    Write-Host "$code $u"
}
