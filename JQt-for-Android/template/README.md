# template/ — APK 模板工程（已完成）

Android 打包输入目录（androiddeployqt --input 指向 deployment-settings.json）：

| 文件/目录 | 说明 |
|-----------|------|
| deployment-settings.json | Qt 6.11 键格式部署配置（4 ABI / minSdk 28 / 显式依赖清单） |
| AndroidManifest.xml | package=org.jqt，activity=org.jqt.JQtPocActivity（继承 QtActivity），lib_name=jqt |
| jqt_android_main.cpp | Qt 线程入口：QApplication + PoC 按钮 + exec（attach 给桥复用） |
| java/org/jqt/ | Java 源码（gradle srcDir 'java'；含 7 个 AWT-free 变体 + JQtPocActivity；其余由 build-android.ps1 暂存） |

> 完整构建/验证流程见 docs/android-build-guide.md 与 docs/poc-status.md。
