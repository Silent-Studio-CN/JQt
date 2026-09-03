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
| **M2: Java-driven UI (runOnQtThread/isQtReady + Java QApplication/button)** | **Done - full Java->JNI->Qt->signal->Java loop on x86_64 (4 taps logged)** | 2026-09-03 |

## Verified on emulator (logcat tag "jqt")
