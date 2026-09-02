# PoC Status

| Step | Status | Date |
|------|--------|------|
| Environment (kit/SDK/NDK/Gradle) | Done | 2026-08-31 |
| Bridge platform adaptation (DBus/SerialPort/Linux-branch/JNI) | Done | 2026-09-01 |
| NDK clang -fsyntax-only | Done (exit 0) | 2026-09-01 |
| Full .so compile + link (libjqt_arm64-v8a.so, Qt libs linked) | Done | 2026-09-01 |
| APK build (androiddeployqt + Gradle 9.3.1 + AGP 9.0.0, compileSdk 36) | Done - jqtpoc-debug.apk (30 MB) | 2026-09-01 |
| **Emulator run (SVM dev2, x86_64 QEMU + ARM translation, Android 15)** | **Done - app runs, button shown, clicks handled** | 2026-09-02 |
| **Multi-ABI (arm64/armv7/x86_64/x86) + minSdk 28** | **Done - native x86_64 run verified (click works)** | 2026-09-02 |

## Verified on emulator (logcat tag "jqt")

```
main entry argc=1
QApplication created
button shown
button clicked, count=1     <- adb input tap 540 955
button clicked, count=2     <- second tap; button text changes to "Clicked! N"
```

## Multi-ABI / backward compatibility

- Four ABIs packaged: arm64-v8a, armeabi-v7a (32-bit old devices), x86_64, x86
- minSdk 28 (Android 9) = Qt 6.11 official floor (Android 9-16 / API 28-36)
- NDK sysroot triples: aarch64-linux-android / arm-linux-androideabi / x86_64-linux-android / i686-linux-android
- Verified on dev2 (x86_64 native, no translation): start 4.3s, button click works
- Verified earlier via ARM translation (arm64-v8a on x86_64 image)
- MuMu Android 15: reported OK by user
