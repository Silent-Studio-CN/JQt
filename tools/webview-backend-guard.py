#!/usr/bin/env python3
"""给 QWebView 加"后端存在性"守卫:没有 plugins/webview 后端时拒绝建视图(否则 Qt 直接 abort)。"""
import io

# ---- QWebView.java ----
p = r"D:\SilentStudio\JQt - Dev\java\org\jqt\QWebView.java"
s = io.open(p, encoding="utf-8").read()
if "isBackendAvailable" not in s:
    s = s.replace(
        "    /** QtWebView 是否编译进本库（未探测到该模块时为 false）。 */\n"
        "    public static boolean isAvailable() { return nativeAvailable(); }",
        "    /** QtWebView 是否编译进本库（未探测到该模块时为 false）。 */\n"
        "    public static boolean isAvailable() { return nativeAvailable(); }\n\n"
        "    /**\n"
        "     * 本平台的 WebView <b>后端</b>是否真的在。\n"
        "     * <p><b>实测结论</b>：官方 Qt 6.12 的 qtwebview 包（MinGW 与 MSVC 都一样）只带\n"
        "     * Qt6WebView / Qt6WebViewQuick 与 QML 模块，<b>没有 plugins/webview/ 后端插件</b>；\n"
        "     * 这种环境下实例化 WebView 元素会让 Qt 直接 abort\n"
        "     * （{@code qFatal(\"No WebView plug-in found!\")}，进程 fail-fast）。\n"
        "     * 因此本类在构造时就拒绝，避免调用方把进程崩掉。\n"
        "     * <p>macOS/iOS 的系统 WebView 后端编译在 QtWebView 内部，不需要插件目录 → 返回 true。\n"
        "     */\n"
        "    public static boolean isBackendAvailable() { return nativeBackendAvailable(); }")
    s = s.replace(
        "    public QWebView(int width, int height) {\n        this.width",
        "    public QWebView(int width, int height) {\n"
        "        if (!isAvailable()) throw new IllegalStateException(\"本库未编译 QtWebView\");\n"
        "        if (!isBackendAvailable()) {\n"
        "            throw new IllegalStateException(\"本平台 Qt 未提供 WebView 后端插件(plugins/webview):\"\n"
        "                    + \"官方 6.12 的 qtwebview 包不含它,实例化会让 Qt 直接中止进程\");\n"
        "        }\n        this.width")
    s = s.replace("    private static native boolean nativeAvailable();",
                  "    private static native boolean nativeAvailable();\n"
                  "    private static native boolean nativeBackendAvailable();")
    io.open(p, "w", encoding="utf-8", newline="\n").write(s)
    print("  QWebView.java: 已加 isBackendAvailable + 构造守卫")
else:
    print("  QWebView.java: 已存在")

# ---- SmokeWebView.java ----
p2 = r"D:\SilentStudio\JQt - Dev\java\org\jqt\SmokeWebView.java"
s2 = io.open(p2, encoding="utf-8").read()
if "isBackendAvailable" not in s2:
    guard = ("        if (!QWebView.isBackendAvailable()) {\n"
             "            // 必须在这里就停:没有后端时建视图会让 Qt qFatal 直接杀进程(实测 Windows 官方包如此)\n"
             "            skipped(\"WebView 端到端断言\", \"本平台 Qt 缺 WebView 后端插件(plugins/webview);\"\n"
             "                    + \"官方 6.12 qtwebview 包在 Windows 上不含它,构造会让 Qt 中止进程\");\n"
             "            app.scheduleQuit(50);\n"
             "            app.exec();\n"
             "            System.out.println(\"[wv] pass=\" + pass + \" fail=\" + fail + \" skip=\" + skip);\n"
             "            System.out.println(\"[wv] ALL PASS ✅(含跳过)\");\n"
             "            return;\n"
             "        }\n\n"
             "        QWebView view = new QWebView(480, 320);")
    s2 = s2.replace("        QWebView view = new QWebView(480, 320);", guard, 1)
    io.open(p2, "w", encoding="utf-8", newline="\n").write(s2)
    print("  SmokeWebView.java: 已加后端守卫")
else:
    print("  SmokeWebView.java: 已存在")
