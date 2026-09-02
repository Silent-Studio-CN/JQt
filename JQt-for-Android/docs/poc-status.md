# PoC Status

| Step | Status | Date |
|------|--------|------|
| Environment (kit/SDK/NDK/Gradle) | Done | 2026-08-31 |
| Bridge platform adaptation (DBus/SerialPort/Linux-branch/JNI) | Done | 2026-09-01 |
| NDK clang -fsyntax-only | Done (exit 0) | 2026-09-01 |
| Full .so compile + link (libjqt_arm64-v8a.so, Qt libs linked) | Done | 2026-09-01 |
| APK build (androiddeployqt + Gradle 9.3.1 + AGP 9.0.0, compileSdk 36) | Done - jqtpoc-debug.apk (30 MB) | 2026-09-01 |
| **Emulator run (SVM dev2, x86_64 QEMU + ARM translation, Android 15)** | **Done - app runs, button shown, clicks handled** | 2026-09-02 |

## Verified on emulator (logcat tag "jqt")

```
main entry argc=1
QApplication created
button shown
button clicked, count=1     <- adb input tap 540 955
button clicked, count=2     <- second tap; button text changes to "Clicked! N"
```

- App: org.jqt (JQtPocActivity extends QtActivity), lib_name=jqt
- arm64-v8a .so loaded via ARM translation on x86_64 image (libndk_translation)
- QApplication created on the Qt thread by main(); jqt_bridge reuses it via
  jqtAndroidAttachApp (double-construction abort fixed)
- Qt event loop + QPushButton clicked signal work end to end

## APK build recipe (remote build machine)

1. `build-android.ps1` - stage java tree into `template/java` (keeps committed AWT-free
   variants of QColor/QFont/QCursor/QFontMetrics/QBitmap/QImage/QPixmap), then NDK clang
   compile jqt_bridge.cpp + jqt_android_main.cpp -> `out-android/libjqt_arm64-v8a.so`
   (links Qt6 Widgets/Gui/Core/OpenGL/OpenGLWidgets/PrintSupport/Sql/Svg + -llog).
2. Pre-place the .so at `apk/libs/arm64-v8a/libjqt_arm64-v8a.so`, run
   `androiddeployqt --input template/deployment-settings.json --output apk`
   (deployment-settings.json uses Qt 6.11 hyphenated keys; explicit
   `deployment-dependencies` file list: Qt libs + plugins + `jar/Qt6Android.jar`).
3. Patch `gradle-wrapper.properties` to `distributionUrl=file:///C:/BuildTools/gradle-9.3.1-bin.zip`
   (services.gradle.org blocked on build machine; Tencent mirror download).
4. Create project-local `settings.gradle` (repo root has one - gradle would walk up).
5. `gradlew assembleDebug` (JAVA_HOME=C:\\BuildTools\\jdk17, AGP 9.0.0 + androidx.core
   1.17.0 from dl.google.com; requires compileSdk 36 -> platforms;android-36 installed).

## Key gotchas (Qt 6.11.2 + AGP 9)

- QJsonValue::toString() returns null for non-string JSON values: version keys must be strings.
- `toolchain-prefix` = "llvm" (NDK r27 layout); `stdcpp-path` = sysroot/usr/lib (triple appended).
- `application-binary` is the lib base name ("jqt"); .so must be pre-placed under
  `<output>/libs/<abi>/`.
- `--java-source` is ignored in Qt 6.11: java sources must ride the
  `android-package-source-directory` copy under a `java/` subdir (gradle srcDir).
- Java AWT bridge (QColor/QFont/QCursor/QFontMetrics/QBitmap/QImage/QPixmap) is desktop-only;
  Android build uses AWT-free variants under `template/java/org/jqt/`.
- AGP 9 compiles with a transformed JDK image (--system): java.awt is absent and
  java.nio.file.Files.readString is missing -> QApplication theme loading uses
  readAllBytes/Paths.get (API 26+).
- Qt6Android.jar (holds QtActivityBase/QtApplicationBase/QtServiceBase) must be listed in
  deployment-dependencies.
- System.loadLibrary("jqt") fails on Android (lib is libjqt_arm64-v8a.so, loaded by
  QtLoader): QApplication static init swallows UnsatisfiedLinkError on Android runtime.
- QApplication must be created once, on the Qt thread (main()); Java-side creation races
  Qt thread startup and aborts in createPlatformIntegration. jqtAndroidAttachApp hands the
  main()-created instance to the bridge.
- fprintf(stderr) is dropped on Android; use __android_log_print (tag "jqt").
- First launch is slow under ARM translation (JIT compiling Qt java bindings): button
  appears ~10s after "QApplication created".
