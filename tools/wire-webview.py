#!/usr/bin/env python3
"""WebView 接线:三个构建脚本加 QtWebView 探测 + CI 三处清单加 SmokeWebView。"""
import io

R = r"D:\SilentStudio\JQt - Dev"

# ---- build-arm64.ps1 ----
p = R + r"\build-arm64.ps1"
s = io.open(p, encoding="utf-8").read()
if "Qt6WebView.lib" not in s:
    s = s.replace("& cl.exe @clArgs", """# QtWebView(v1.9.2 P2):ARM64 包未必带;带了才启用(没有后端插件时 Java 侧会拒绝建视图)
$wvLib = Join-Path $QtRoot "lib\\Qt6WebView.lib"
if (Test-Path $wvLib) {
    $linkIdxW = [Array]::IndexOf($clArgs, "/link")
    if ($linkIdxW -lt 0) { $linkIdxW = $clArgs.Count }
    $preW = @("/DJQT_HAVE_WEBVIEW", "/I", (Join-Path $QtRoot "include\\QtWebView"))
    $clArgs = $clArgs[0..($linkIdxW - 1)] + $preW + $clArgs[$linkIdxW..($clArgs.Count - 1)]
    $clArgs += @("/link", $wvLib)
    Write-Host "==> QtWebView found - QWebView enabled"
} else {
    Write-Host "==> QtWebView not found - QWebView will report unavailable"
}
& cl.exe @clArgs""", 1)
    io.open(p, "w", encoding="utf-8", newline="\n").write(s)
    print("  build-arm64.ps1: 已加 QtWebView 探测")

# ---- build-linux.sh ----
p = R + r"\build-linux.sh"
s = io.open(p, encoding="utf-8").read()
if "WEBVIEW_FLAGS" not in s:
    s = s.replace("""# ---- 2.5 Deploy SQLite driver""", """# QtWebView(v1.9.2 P2):Linux 上 QtWebView 依赖 QtWebEngine,官方包通常都不带 -> 多半不可用。
if ls "$QTLIB"/libQt6WebView.so* >/dev/null 2>&1; then
  WEBVIEW_FLAGS="-DJQT_HAVE_WEBVIEW -I$QTINC/QtWebView -lQt6WebView"
  echo "==> QtWebView found - QWebView enabled"
else
  WEBVIEW_FLAGS=""
  echo "==> QtWebView not found - QWebView will report unavailable"
fi

# ---- 2.5 Deploy SQLite driver""", 1)
    s = s.replace("$SVG_FLAGS $WS_FLAGS", "$SVG_FLAGS $WEBVIEW_FLAGS $WS_FLAGS", 1)
    io.open(p, "w", encoding="utf-8", newline="\n").write(s)
    print("  build-linux.sh: 已加 QtWebView 探测")

# ---- build-macos.sh ----
p = R + r"\build-macos.sh"
s = io.open(p, encoding="utf-8").read()
if "WEBVIEW_FLAGS" not in s:
    s = s.replace('echo "==> Compiling native bridge (libjqt.dylib)"', """# QtWebView(v1.9.2 P2):macOS 的系统 WKWebView 后端编译在 QtWebView 内部(无需插件目录)。
if [ -d "$QTLIB/QtWebView.framework" ]; then
  WEBVIEW_FLAGS="-DJQT_HAVE_WEBVIEW -I$QTLIB/QtWebView.framework/Headers -framework QtWebView"
  echo "==> QtWebView found - QWebView enabled (WKWebView 后端内建)"
else
  WEBVIEW_FLAGS=""
  echo "==> QtWebView not found - QWebView will report unavailable"
fi

echo "==> Compiling native bridge (libjqt.dylib)\"""", 1)
    s = s.replace("$DBUS_FLAGS $SVG_FLAGS", "$DBUS_FLAGS $SVG_FLAGS $WEBVIEW_FLAGS", 1)
    io.open(p, "w", encoding="utf-8", newline="\n").write(s)
    print("  build-macos.sh: 已加 QtWebView 探测")

# ---- ci.yml:三处清单加 SmokeWebView ----
p = R + r"\.github\workflows\ci.yml"
s = io.open(p, encoding="utf-8").read()
n = 0
for a, b in (("org.jqt.SmokeDBus org.jqt.SmokeCharts", "org.jqt.SmokeDBus org.jqt.SmokeWebView org.jqt.SmokeCharts"),
             ('"org.jqt.SmokeDBus","org.jqt.SmokeCharts"', '"org.jqt.SmokeDBus","org.jqt.SmokeWebView","org.jqt.SmokeCharts"')):
    if b not in s:
        s = s.replace(a, b)
        n += 1
io.open(p, "w", encoding="utf-8", newline="\n").write(s)
print(f"  ci.yml: SmokeWebView 已加入 {n} 处清单;总出现 {s.count('SmokeWebView')} 次")
