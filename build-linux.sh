#!/usr/bin/env bash
# ============================================================================
# build-linux.sh - JQt Linux one-click build (mirrors build.ps1)
#   Produces lib/libjqt.so + deploys license notices.
#
# Prerequisites (Ubuntu):
#   sudo apt-get install -y qt6-base-dev g++ libgl1-mesa-dev
#   JAVA_HOME must point at a JDK (e.g. from actions/setup-java)
#
# Usage:
#   QT_BASE=/usr ./build-linux.sh          (Debian/Ubuntu Qt6 layout)
#   QT_BASE=$HOME/Qt/6.12.0/gcc_64 ./build-linux.sh   (Qt online installer layout)
# ============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/out"
LIB="$ROOT/lib"
NATIVE="$ROOT/native"
GEN="$NATIVE/generated"

mkdir -p "$OUT" "$LIB" "$GEN"

# ---- 1. Compile Java + generate JNI headers ----
# 生成带引号的 argfile（路径含空格也能正确解析）
find "$ROOT/java" -name "*.java" | sed 's/.*/"&"/' > "$ROOT/.jqt_sources.txt"
javac -encoding UTF-8 -d "$OUT" -h "$GEN" @"$ROOT/.jqt_sources.txt"
rm -f "$ROOT/.jqt_sources.txt"

# ---- 2. Locate Qt include/lib dirs ----
QT_BASE="${QT_BASE:-/usr}"
QTINC="$QT_BASE/include"
# Debian/Ubuntu puts Qt6 headers under /usr/include/<arch>/qt6
if [ -d "$QTINC/x86_64-linux-gnu/qt6" ]; then
  QTINC="$QTINC/x86_64-linux-gnu/qt6"
fi
QTLIB="$QT_BASE/lib"
[ -d "$QTLIB/x86_64-linux-gnu" ] && QTLIB="$QTLIB/x86_64-linux-gnu"

echo "==> Compiling native bridge (libjqt.so)"
# 同 macOS:QML 需要 QtQmlIntegration 的头,缺了就不要开(否则编译期报错)
if ls "$QTLIB"/libQt6Help.so* >/dev/null 2>&1; then
  HELP_FLAGS="-DJQT_HAVE_HELP -I$QTINC/QtHelp -lQt6Help"
  echo "==> QtHelp found - QHelpEngineCore enabled"
else
  HELP_FLAGS=""
  echo "==> QtHelp not found - QHelpEngineCore will report unavailable"
fi

if ls "$QTLIB"/libQt6Positioning.so* >/dev/null 2>&1; then
  GEO_FLAGS="-DJQT_HAVE_POSITIONING -I$QTINC/QtPositioning -lQt6Positioning"
  echo "==> QtPositioning found - QGeoCoordinate enabled"
else
  GEO_FLAGS=""
  echo "==> QtPositioning not found - QGeoCoordinate will report unavailable"
fi

if ls "$QTLIB"/libQt6Quick.so* >/dev/null 2>&1 && [ -f "$QTINC/QtQmlIntegration/qqmlintegration.h" ]; then
  QML_FLAGS="-DJQT_HAVE_QML -I$QTINC/QtQuick -I$QTINC/QtQml -I$QTINC/QtQmlIntegration -lQt6Quick -lQt6Qml"
  echo "==> QtQuick found - QQuickView enabled"
else
  QML_FLAGS=""
  echo "==> QtQuick not found - QQuickView will report unavailable"
fi

if ls "$QTLIB"/libQt6Multimedia.so* >/dev/null 2>&1; then
  MM_FLAGS="-DJQT_HAVE_MULTIMEDIA -I$QTINC/QtMultimedia -lQt6Multimedia"
  echo "==> QtMultimedia found - QMediaPlayer enabled"
else
  MM_FLAGS=""
  echo "==> QtMultimedia not found - QMediaPlayer will report unavailable"
fi

if ls "$QTLIB"/libQt6Charts.so* >/dev/null 2>&1; then
  CHART_FLAGS="-DJQT_HAVE_CHARTS -I$QTINC/QtCharts -lQt6Charts"
  echo "==> QtCharts found - QChart enabled"
else
  CHART_FLAGS=""
  echo "==> QtCharts not found - QChart will report unavailable"
fi

if ls "$QTLIB"/libQt6WebSockets.so* >/dev/null 2>&1; then
  WS_FLAGS="-DJQT_HAVE_WEBSOCKETS -I$QTINC/QtWebSockets -lQt6WebSockets"
  echo "==> QtWebSockets found - QWebSocket enabled"
else
  WS_FLAGS=""
  echo "==> QtWebSockets not found - QWebSocket will report unavailable"
fi

# QtDBus(v1.9.2 P1 收尾):qtbase 自带,链接早已存在(桥内屏保抑制/通知在用),
# 这里只是再加上 -DJQT_HAVE_DBUS,把通用面(QDBusConnection/QDBusMessage)编进库。
echo "==> QtDBus: assume available (qtbase) - QDBusConnection enabled"

# QtSvg 探测(v1.9.2 补漏):qtsvg 一直在 CI 里装了,但这个脚本从来没传 -DJQT_HAVE_SVG,
# 导致 Linux 上 SmokeEvent 里的 QSvgRenderer 断言一直 SKIP(文档 2.1 节记的就是这件事)。
if ls "$QTLIB"/libQt6Svg.so* >/dev/null 2>&1; then
  SVG_FLAGS="-DJQT_HAVE_SVG -I$QTINC/QtSvg -lQt6Svg"
  echo "==> QtSvg found - QSvgRenderer enabled"
else
  SVG_FLAGS=""
  echo "==> QtSvg not found - QSvgRenderer will report unavailable"
fi

g++ -std=c++17 -O2 -shared -fPIC     -o "$LIB/libjqt.so"     -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux"     -I"$QTINC" -I"$QTINC/QtWidgets" -I"$QTINC/QtGui" -I"$QTINC/QtCore" -I"$QTINC/QtDBus" -I"$QTINC/QtPrintSupport" -I"$QTINC/QtSql" -I"$QTINC/QtOpenGLWidgets" -I"$QTINC/QtOpenGL" -I"$QTINC/QtSerialPort" -I"$QTINC/QtNetwork" -DJQT_HAVE_NETWORK -DJQT_HAVE_SQL_MODELS -DJQT_HAVE_DBUS     -I"$NATIVE"     "$NATIVE/jqt_bridge.cpp"     -L"$QTLIB" -lQt6Widgets -lQt6Gui -lQt6Core -lQt6PrintSupport -lQt6Sql -lQt6OpenGLWidgets -lQt6OpenGL -lQt6SerialPort -lQt6DBus -lQt6Network $SVG_FLAGS $WEBVIEW_FLAGS $WS_FLAGS $CHART_FLAGS $MM_FLAGS $QML_FLAGS $GEO_FLAGS $HELP_FLAGS

# QtWebView(v1.9.2 P2):Linux 上 QtWebView 依赖 QtWebEngine,官方包通常都不带 -> 多半不可用。
if ls "$QTLIB"/libQt6WebView.so* >/dev/null 2>&1; then
  WEBVIEW_FLAGS="-DJQT_HAVE_WEBVIEW -I$QTINC/QtWebView -lQt6WebView"
  echo "==> QtWebView found - QWebView enabled"
else
  WEBVIEW_FLAGS=""
  echo "==> QtWebView not found - QWebView will report unavailable"
fi

# ---- 2.5 Deploy SQLite driver (v1.9.2 补漏) ----
# CI 的 Linux Qt 包装了 plugins/sqldrivers/libqsqlite.so,但从来没拷进 lib/ ->
# SQL 冒烟一直 SKIP("本平台无 SQLite 驱动")。这里按 Windows 侧同样的布局部署。
if [ -f "$QTLIB/plugins/sqldrivers/libqsqlite.so" ]; then
  mkdir -p "$LIB/plugins/sqldrivers"
  cp "$QTLIB/plugins/sqldrivers/libqsqlite.so" "$LIB/plugins/sqldrivers/" 2>/dev/null || true
  echo "==> SQLite driver deployed (plugins/sqldrivers/libqsqlite.so)"
else
  echo "==> SQLite driver not found in this Qt - SQL smoke will skip"
fi

# ---- 3. Deploy license notices (LGPL compliance) ----
cp "$ROOT/LGPL-3.0.txt" "$ROOT/THIRD-PARTY-NOTICES.md" "$ROOT/LICENSE.md" "$ROOT/LICENSE" "$LIB/" 2>/dev/null || true

echo "Build OK"
echo "  Dynamic lib   : $LIB/libjqt.so"
echo "  Run demo      : QT_QPA_PLATFORM=offscreen ./run-linux.sh -AutoClose 3000"
