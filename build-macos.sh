#!/usr/bin/env bash
# ============================================================================
# build-macos.sh - JQt macOS one-click build
#   Produces lib/libjqt.dylib + deploys license notices.
#
# Prerequisites:
#   Xcode Command Line Tools (clang++) and a JDK (JAVA_HOME set)
#   Qt for macOS: aqt install-qt mac desktop 6.12.0 clang_64 -O ~/Qt
#   QT_BASE=$HOME/Qt/6.12.0/macos ./build-macos.sh
# ============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/out"
LIB="$ROOT/lib"
NATIVE="$ROOT/native"
GEN="$NATIVE/generated"

mkdir -p "$OUT" "$LIB" "$GEN"

# ---- 1. Compile Java + generate JNI headers ----
find "$ROOT/java" -name "*.java" | sed 's/.*/"&"/' > "$ROOT/.jqt_sources.txt"
javac -encoding UTF-8 -d "$OUT" -h "$GEN" @"$ROOT/.jqt_sources.txt"
rm -f "$ROOT/.jqt_sources.txt"

# ---- 2. Qt layout ----
QT_BASE="${QT_BASE:?set QT_BASE to the Qt for macOS install dir}"
QTLIB="$QT_BASE/lib"
QTINC="$QTLIB/QtWidgets.framework/Headers $QTLIB/QtGui.framework/Headers $QTLIB/QtCore.framework/Headers $QTLIB/QtPrintSupport.framework/Headers $QTLIB/QtSql.framework/Headers $QTLIB/QtSerialPort.framework/Headers $QTLIB/QtNetwork.framework/Headers"

echo "QTINC=$QTINC"
ls "$QTLIB/QtGui.framework/Headers/qevent.h" && echo "QtGui headers OK"
# Qt 6 mac frameworks in 7z may lack extension-less forwarding headers (QCloseEvent etc.)
# Generate them if missing (Qt's own forwarding header is just: #include "qevent.h")
echo "==> Ensuring forwarding headers"
for dir in QtWidgets QtGui QtCore; do
  H="$QTLIB/$dir.framework/Headers"
  [ -d "$H" ] || continue
  for f in "$H"/*.h; do
    [ -f "$f" ] || continue
    base=$(basename "$f" .h)
    if [ ! -f "$H/$base" ]; then
      echo "#include \"$(basename "$f")\"" > "$H/$base"
    fi
  done
done
echo "forwarding headers ensured"
echo "=== diag: QCloseEvent ==="
ls -la "$QTLIB/QtGui.framework/Headers/QCloseEvent" 2>&1
head -3 "$QTLIB/QtGui.framework/Headers/QCloseEvent" 2>&1
echo "=== diag: QtWidgets dir ==="
ls "$QTLIB/QtWidgets.framework/Headers/" 2>&1 | head -8
# v0.7.4: QtSerialPort framework 头含 fake header，复制到标准模块布局
# （$LIB/sp_include/QtSerialPort → <QtSerialPort/QSerialPort> 前缀 include 可解析）
if [ -d "$QTLIB/QtSerialPort.framework/Headers" ]; then
  mkdir -p "$LIB/sp_include/QtSerialPort"
  cp -R "$QTLIB/QtSerialPort.framework/Headers/"* "$LIB/sp_include/QtSerialPort/" 2>/dev/null || true
  # 无前缀 include（<QSerialPort>）也能解析：头复制到 QtCore Headers
  cp "$LIB/sp_include/QtSerialPort/"QSerialPort "$LIB/sp_include/QtSerialPort/"QSerialPortInfo "$LIB/sp_include/QtSerialPort/"*.h "$QTLIB/QtCore.framework/Headers/" 2>/dev/null || true
  ls "$LIB/sp_include/QtSerialPort/" 2>&1 | head -5 || true
fi
# QtQuick 探测:除 QtQuick.framework 外还必须找得到 qqmlintegration.h ——
# 否则 <QQmlComponent> 会把它 include 进来而报 "file not found"
# (CI 的 6.8.3 macOS 树就是这种"半个 QML"状态)。
if [ -d "$QTLIB/QtHelp.framework" ]; then
  HELP_FLAGS="-DJQT_HAVE_HELP -I$QTLIB/QtHelp.framework/Headers -framework QtHelp"
  echo "==> QtHelp found - QHelpEngineCore enabled"
else
  HELP_FLAGS=""
  echo "==> QtHelp not found - QHelpEngineCore will report unavailable"
fi

if [ -d "$QTLIB/QtPositioning.framework" ]; then
  GEO_FLAGS="-DJQT_HAVE_POSITIONING -I$QTLIB/QtPositioning.framework/Headers -framework QtPositioning"
  echo "==> QtPositioning found - QGeoCoordinate enabled"
else
  GEO_FLAGS=""
  echo "==> QtPositioning not found - QGeoCoordinate will report unavailable"
fi

if [ -d "$QTLIB/QtQuick.framework" ] && { [ -f "$QTLIB/QtQmlIntegration.framework/Headers/qqmlintegration.h" ] || [ -f "$QTLIB/QtQml.framework/Headers/QtQmlIntegration/qqmlintegration.h" ]; }; then
  QML_FLAGS="-DJQT_HAVE_QML -I$QTLIB/QtQuick.framework/Headers -I$QTLIB/QtQml.framework/Headers -I$QTLIB/QtQmlIntegration.framework/Headers -framework QtQuick -framework QtQml -framework QtQmlIntegration"
  echo "==> QtQuick found - QQuickView enabled"
else
  QML_FLAGS=""
  echo "==> QtQuick not found - QQuickView will report unavailable"
fi

if [ -d "$QTLIB/QtMultimedia.framework" ]; then
  MM_FLAGS="-DJQT_HAVE_MULTIMEDIA -I$QTLIB/QtMultimedia.framework/Headers -framework QtMultimedia"
  echo "==> QtMultimedia found - QMediaPlayer enabled"
else
  MM_FLAGS=""
  echo "==> QtMultimedia not found - QMediaPlayer will report unavailable"
fi

if [ -d "$QTLIB/QtCharts.framework" ]; then
  CHART_FLAGS="-DJQT_HAVE_CHARTS -I$QTLIB/QtCharts.framework/Headers -framework QtCharts"
  echo "==> QtCharts found - QChart enabled"
else
  CHART_FLAGS=""
  echo "==> QtCharts not found - QChart will report unavailable"
fi

if [ -d "$QTLIB/QtWebSockets.framework" ]; then
  WS_FLAGS="-DJQT_HAVE_WEBSOCKETS -I$QTLIB/QtWebSockets.framework/Headers -framework QtWebSockets"
  echo "==> QtWebSockets found - QWebSocket enabled"
else
  WS_FLAGS=""
  echo "==> QtWebSockets not found - QWebSocket will report unavailable"
fi

# QtDBus(v1.9.2 P1 收尾):macOS 上 QtDBus.framework 在,但系统默认没有会话总线 ->
# isAvailable()=true 而 isSessionBusConnected()=false,冒烟里分开记录。
if [ -d "$QTLIB/QtDBus.framework" ]; then
  DBUS_FLAGS="-DJQT_HAVE_DBUS -I$QTLIB/QtDBus.framework/Headers -framework QtDBus"
  echo "==> QtDBus found - QDBusConnection enabled (无会话总线时连接状态为 false)"
else
  DBUS_FLAGS=""
  echo "==> QtDBus not found - DBus API will report unavailable"
fi

# QtSvg 探测(v1.9.2 补漏):与 Linux 同样的问题 —— 包里有 QtSvg,但脚本没传 -DJQT_HAVE_SVG,
# 于是 macOS 上 QSvgRenderer 断言一直 SKIP。
if [ -d "$QTLIB/QtSvg.framework" ]; then
  SVG_FLAGS="-DJQT_HAVE_SVG -I$QTLIB/QtSvg.framework/Headers -framework QtSvg"
  echo "==> QtSvg found - QSvgRenderer enabled"
else
  SVG_FLAGS=""
  echo "==> QtSvg not found - QSvgRenderer will report unavailable"
fi

# QtWebView(v1.9.2 P2):macOS 的系统 WKWebView 后端编译在 QtWebView 内部(无需插件目录)。
if [ -d "$QTLIB/QtWebView.framework" ]; then
  WEBVIEW_FLAGS="-DJQT_HAVE_WEBVIEW -I$QTLIB/QtWebView.framework/Headers -framework QtWebView"
  echo "==> QtWebView found - QWebView enabled (WKWebView 后端内建)"
else
  WEBVIEW_FLAGS=""
  echo "==> QtWebView not found - QWebView will report unavailable"
fi

echo "==> Compiling native bridge (libjqt.dylib)"
clang++ -std=c++17 -O2 -shared -fPIC \
    -o "$LIB/libjqt.dylib" \
    -install_name @rpath/libjqt.dylib \
    -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/darwin" \
    -I"$QTLIB/QtWidgets.framework/Headers" -I"$QTLIB/QtGui.framework/Headers" -I"$QTLIB/QtCore.framework/Headers" -I"$QTLIB/QtPrintSupport.framework/Headers" -I"$QTLIB/QtSql.framework/Headers" -I"$LIB/sp_include" -I"$QTLIB/QtSerialPort.framework/Headers" -I"$QTLIB/QtNetwork.framework/Headers" \
    -I"$NATIVE" \
    "$NATIVE/jqt_bridge.cpp" \
    -F"$QTLIB" -framework QtWidgets -framework QtGui -framework QtCore -framework QtPrintSupport -framework QtSql -framework QtSerialPort -framework QtNetwork -DJQT_HAVE_SQL_MODELS $DBUS_FLAGS $SVG_FLAGS $WEBVIEW_FLAGS $WS_FLAGS $CHART_FLAGS $MM_FLAGS $QML_FLAGS $GEO_FLAGS $HELP_FLAGS -framework AppKit -framework Foundation -framework CoreFoundation

# ---- 2.5 Deploy SQLite driver (v1.9.2 补漏):与 Linux 同因(插件在包里但没拷) ----
if [ -d "$QTLIB/plugins/sqldrivers" ] && ls "$QTLIB"/plugins/sqldrivers/libqsqlite*.dylib >/dev/null 2>&1; then
  mkdir -p "$LIB/plugins/sqldrivers"
  cp "$QTLIB"/plugins/sqldrivers/libqsqlite*.dylib "$LIB/plugins/sqldrivers/" 2>/dev/null || true
  echo "==> SQLite driver deployed (plugins/sqldrivers)"
else
  echo "==> SQLite driver not found in this Qt - SQL smoke will skip"
fi

# ---- 3. Deploy license notices ----
cp "$ROOT/LGPL-3.0.txt" "$ROOT/THIRD-PARTY-NOTICES.md" "$ROOT/LICENSE.md" "$ROOT/LICENSE" "$LIB/" 2>/dev/null || true

echo "Build OK"
echo "  Dynamic lib   : $LIB/libjqt.dylib"
echo "  Run demo      : QT_QPA_PLATFORM=offscreen ./run-macos.sh -AutoClose 3000"
