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

# ---- 0. 可选模块开关:先把每个变量都定义成空串 ----
# 本脚本开着 set -euo pipefail,任何"只在 if 分支里赋值、却在赋值前被引用"的变量
# 都会以 `unbound variable` 让整个构建挂掉(Linux 侧 v1.9.2 就这么翻过一次车)。
# 统一前置初始化,从结构上消灭这一类错误。
SVG_FLAGS=""
WEBVIEW_FLAGS=""
HELP_FLAGS=""
GEO_FLAGS=""
QML_FLAGS=""
MM_FLAGS=""
CHART_FLAGS=""
WS_FLAGS=""
DBUS_FLAGS=""
QUICK3D_FLAGS=""

# ---- 1. Compile Java + generate JNI headers ----
find "$ROOT/java" -name "*.java" | sed 's/.*/"&"/' > "$ROOT/.jqt_sources.txt"
javac -encoding UTF-8 -d "$OUT" -h "$GEN" @"$ROOT/.jqt_sources.txt"
rm -f "$ROOT/.jqt_sources.txt"

# ---- 2. Qt layout ----
QT_BASE="${QT_BASE:?set QT_BASE to the Qt for macOS install dir}"
QTLIB="$QT_BASE/lib"
QTINC="$QTLIB/QtWidgets.framework/Headers $QTLIB/QtGui.framework/Headers $QTLIB/QtCore.framework/Headers $QTLIB/QtPrintSupport.framework/Headers $QTLIB/QtSql.framework/Headers $QTLIB/QtSerialPort.framework/Headers $QTLIB/QtNetwork.framework/Headers"

# 插件目录是 <Qt 安装前缀>/plugins —— **不是** <前缀>/lib/plugins。
# v1.9.2 的失败尝试写的是 "$QTLIB/plugins/sqldrivers",在 macOS SDK 布局下不存在,
# CI 日志里因此打出 "SQLite driver not found in this Qt"(探测路径错了,不是插件没装)。
QT_PLUGINS="$QT_BASE/plugins"
[ -d "$QT_PLUGINS/sqldrivers" ] || QT_PLUGINS="$QTLIB/plugins"

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
  echo "==> QtHelp not found - QHelpEngineCore will report unavailable"
fi

if [ -d "$QTLIB/QtPositioning.framework" ]; then
  GEO_FLAGS="-DJQT_HAVE_POSITIONING -I$QTLIB/QtPositioning.framework/Headers -framework QtPositioning"
  echo "==> QtPositioning found - QGeoCoordinate enabled"
else
  echo "==> QtPositioning not found - QGeoCoordinate will report unavailable"
fi

if [ -d "$QTLIB/QtQuick.framework" ] && { [ -f "$QTLIB/QtQmlIntegration.framework/Headers/qqmlintegration.h" ] || [ -f "$QTLIB/QtQml.framework/Headers/QtQmlIntegration/qqmlintegration.h" ]; }; then
  QML_FLAGS="-DJQT_HAVE_QML -I$QTLIB/QtQuick.framework/Headers -I$QTLIB/QtQml.framework/Headers -I$QTLIB/QtQmlIntegration.framework/Headers -framework QtQuick -framework QtQml -framework QtQmlIntegration"
  echo "==> QtQuick found - QQuickView enabled"
else
  echo "==> QtQuick not found - QQuickView will report unavailable"
fi

if [ -d "$QTLIB/QtMultimedia.framework" ]; then
  MM_FLAGS="-DJQT_HAVE_MULTIMEDIA -I$QTLIB/QtMultimedia.framework/Headers -framework QtMultimedia"
  echo "==> QtMultimedia found - QMediaPlayer enabled"
else
  echo "==> QtMultimedia not found - QMediaPlayer will report unavailable"
fi

if [ -d "$QTLIB/QtCharts.framework" ]; then
  CHART_FLAGS="-DJQT_HAVE_CHARTS -I$QTLIB/QtCharts.framework/Headers -framework QtCharts"
  echo "==> QtCharts found - QChart enabled"
else
  echo "==> QtCharts not found - QChart will report unavailable"
fi

if [ -d "$QTLIB/QtWebSockets.framework" ]; then
  WS_FLAGS="-DJQT_HAVE_WEBSOCKETS -I$QTLIB/QtWebSockets.framework/Headers -framework QtWebSockets"
  echo "==> QtWebSockets found - QWebSocket enabled"
else
  echo "==> QtWebSockets not found - QWebSocket will report unavailable"
fi

# QtSvg(v1.9.2 补漏):判定条件与实际布局一致 —— macOS 的 qtsvg 装的是
#   lib/QtSvg.framework/Versions/A/Headers/QSvgRenderer  (+ Headers -> Versions/Current/Headers 符号链接)
# 它和 qtbase 是**两个独立的 7z**(7z 清单实测)。v1.9.2 的尝试之所以"探测没命中",
# 是因为 6.12.0 那棵树是手工 7z 装的、只解了 qtbase —— QtSvg.framework 压根不存在
# (同一份日志里 6.8.3 那棵 aqt 树却打出了 "QtSvg found",因为 aqt 默认把整个
#  qt.qt6.683.clang_64 包都装了,含 qtsvg)。
# 所以除了探测,CI 里还必须真的把 qtsvg 那个 7z 解进 $QT_BASE。
if [ -d "$QTLIB/QtSvg.framework" ] && [ -f "$QTLIB/QtSvg.framework/Headers/QSvgRenderer" ]; then
  SVG_FLAGS="-DJQT_HAVE_SVG -I$QTLIB/QtSvg.framework/Headers -framework QtSvg"
  echo "==> QtSvg found - QSvgRenderer enabled"
else
  echo "==> QtSvg not found (需要 $QTLIB/QtSvg.framework/Headers/QSvgRenderer) - QSvgRenderer will report unavailable"
fi

# QtWebView(v1.9.2 P2):判定条件与实际文件布局一致 —— 只认 QtWebView.framework /
# libQt6WebView*.dylib。CI 的 macOS Qt 里**没有** qtwebview:它是独立 addon 包
# (qt.qt6.6120.addons.qtwebview.clang_64),CI 只解 qtbase/qtsvg 系列。
# 这里如实打印"该 Qt 未带 qtwebview",而不是让人以为"探测过了只是没生效"。
if [ -d "$QTLIB/QtWebView.framework" ] || ls "$QTLIB"/libQt6WebView*.dylib >/dev/null 2>&1; then
  WEBVIEW_FLAGS="-DJQT_HAVE_WEBVIEW -I$QTLIB/QtWebView.framework/Headers -framework QtWebView"
  echo "==> QtWebView found ($QTLIB) - QWebView enabled (WKWebView 后端内建)"
else
  echo "==> 该 Qt 未带 qtwebview(未找到 $QTLIB/QtWebView.framework) - QWebView will report unavailable"
fi

# QtQuick3D(v1.9.3):Quick 3D 是 QML 模块(import QtQuick3D),加载走 QQuickView ——
# 本库**不链接** QtQuick3D.macOS 是 framework 布局,所以探测要认两种形态:
#   lib/QtQuick3D.framework(官方 7z/aqt 装出来就是这个)或 libQt6Quick3D*.dylib;
# QML 模块则在 <Qt前缀>/qml/QtQuick3D/qmldir。判定只看实际文件,不做"假装探测"。
QUICK3D_QML=""
for d in "$QT_BASE/qml/QtQuick3D" "$QTLIB/qml/QtQuick3D"; do
  if [ -f "$d/qmldir" ]; then QUICK3D_QML="$d"; break; fi
done
QUICK3D_LIB=0
if [ -d "$QTLIB/QtQuick3D.framework" ] || ls "$QTLIB"/libQt6Quick3D*.dylib >/dev/null 2>&1; then QUICK3D_LIB=1; fi
if [ "$QUICK3D_LIB" = 1 ]; then
  QUICK3D_FLAGS="-DJQT_HAVE_QUICK3D"
fi
if [ "$QUICK3D_LIB" = 1 ] && [ -n "$QUICK3D_QML" ]; then
  echo "==> QtQuick3D found (framework=$QTLIB/QtQuick3D.framework, qml=$QUICK3D_QML) - QQuick3D enabled (可用性由运行期探测)"
else
  echo "==> 该 Qt 未带 qtquick3d(需要 $QTLIB/QtQuick3D.framework 与 <Qt前缀>/qml/QtQuick3D/qmldir) - Quick3D will report unavailable"
fi

# QtDBus(v1.9.2 P1 收尾):macOS 上 QtDBus.framework 在,但系统默认没有会话总线 ->
# isAvailable()=true 而 isSessionBusConnected()=false,冒烟里分开记录。
if [ -d "$QTLIB/QtDBus.framework" ]; then
  DBUS_FLAGS="-DJQT_HAVE_DBUS -I$QTLIB/QtDBus.framework/Headers -framework QtDBus"
  echo "==> QtDBus found - QDBusConnection enabled (无会话总线时连接状态为 false)"
else
  echo "==> QtDBus not found - DBus API will report unavailable"
fi

echo "==> Compiling native bridge (libjqt.dylib)"
clang++ -std=c++17 -O2 -shared -fPIC \
    -o "$LIB/libjqt.dylib" \
    -install_name @rpath/libjqt.dylib \
    -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/darwin" \
    -I"$QTLIB/QtWidgets.framework/Headers" -I"$QTLIB/QtGui.framework/Headers" -I"$QTLIB/QtCore.framework/Headers" -I"$QTLIB/QtPrintSupport.framework/Headers" -I"$QTLIB/QtSql.framework/Headers" -I"$LIB/sp_include" -I"$QTLIB/QtSerialPort.framework/Headers" -I"$QTLIB/QtNetwork.framework/Headers" \
    -I"$NATIVE" \
    "$NATIVE/jqt_bridge.cpp" \
    -F"$QTLIB" -framework QtWidgets -framework QtGui -framework QtCore -framework QtPrintSupport -framework QtSql -framework QtSerialPort -framework QtNetwork -DJQT_HAVE_SQL_MODELS $QUICK3D_FLAGS $DBUS_FLAGS $WS_FLAGS $CHART_FLAGS $MM_FLAGS $QML_FLAGS $GEO_FLAGS $HELP_FLAGS $SVG_FLAGS $WEBVIEW_FLAGS -framework AppKit -framework Foundation -framework CoreFoundation

# ---- 2.5 Deploy the SQLite driver where the bridge actually looks for it ----
# 与 Linux 同因、同解(实测不再靠猜):native/jqt_bridge.cpp 的兜底分支按
#   <libraryPath>/sqldrivers/q<driver><ext>
# 找插件 —— 那是 **Windows 命名**(qsqlite.dll),所以 Windows 一直能用;macOS 的插件
# 叫 libqsqlite.dylib,兜底找不到,于是 "SKIP 本平台无 SQLite 驱动"。
# 桥里 addLibraryPath(<cwd>/lib),CI 的冒烟就是在仓库根目录跑 ./run-macos.sh,
# 所以按兜底期望的名字放到 <repo>/lib/sqldrivers/qsqlite.dylib 即可命中。
# 注意:必须用**当前 $QT_BASE 的**插件 —— 6.12.0 的构建在本脚本里后跑,会覆盖 6.8.3 的,
# 而 CI 冒烟用的正是 6.12.0,两者对齐。
if [ -f "$QT_PLUGINS/sqldrivers/libqsqlite.dylib" ]; then
  mkdir -p "$LIB/sqldrivers"
  cp -f "$QT_PLUGINS/sqldrivers/libqsqlite.dylib" "$LIB/sqldrivers/qsqlite.dylib"
  echo "==> SQLite driver deployed: lib/sqldrivers/qsqlite.dylib"
else
  echo "==> 警告:本 Qt 未带 SQLite 驱动插件($QT_PLUGINS/sqldrivers/libqsqlite.dylib 不存在) - SmokeSqlModel 仍会 SKIP"
fi

# ---- 2.6 Deploy QtQuick3D runtime bits (v1.9.3, macOS framework 布局) ----
# 与 Linux 同一套做法、同样两件事:
#   · QML 模块(<Qt前缀>/qml/QtQuick3D)→ lib/qml(打包布局)+ qml/(开发/测试布局);
#   · 插件链最后一环 QtShaderTools。macOS 是 framework 布局:官方包装出来的是
#     lib/QtShaderTools.framework(不是 libQt6ShaderTools.dylib),两者都认 ——
#     有 dylib 就按 dylib 拷,只有 framework 就连 framework 一起拷(运行期需要
#     DYLD_FRAMEWORK_PATH 含该目录;开发/CI 下 Qt 自己的 $QTLIB 也能找到)。
if [ -n "$QUICK3D_QML" ]; then
  for dst in "$LIB/qml/QtQuick3D" "$ROOT/qml/QtQuick3D"; do
    mkdir -p "$dst"
    cp -R "$QUICK3D_QML/." "$dst/"
  done
  echo "==> QtQuick3D QML 模块已部署:lib/qml/QtQuick3D + qml/QtQuick3D"
  echo "    提示:tools/bundle-qt.sh 只打包 Qt framework 与 plugins,**不打 qml/** —— 自带 Qt 的运行包里"
  echo "    import QtQuick3D 仍会找不到模块,需要把 qml/QtQuick3D 一并打进包里。"
else
  echo "==> 未部署 QtQuick3D QML 模块(本 Qt 未带 qtquick3d)"
fi

SHADERTOOLS=0
for dylib in "$QTLIB"/libQt6ShaderTools*.dylib; do
  if [ -e "$dylib" ]; then cp -a "$dylib" "$LIB/"; SHADERTOOLS=1; fi
done
if [ -d "$QTLIB/QtShaderTools.framework" ]; then
  cp -R "$QTLIB/QtShaderTools.framework" "$LIB/"
  SHADERTOOLS=1
fi
if [ "$SHADERTOOLS" = 1 ]; then
  echo "==> QtShaderTools 已部署到 lib/(Quick3D 插件链最后一环;framework 形态时运行期需要 DYLD_FRAMEWORK_PATH 含 lib/)"
else
  echo "==> 警告:本 Qt 未带 Qt Shader Tools(既没有 $QTLIB/QtShaderTools.framework 也没有 libQt6ShaderTools*.dylib)"
  echo "    —— 即使装了 qtquick3d,qquick3dplugin 也加载不出来,Quick3D 只会 SKIP"
  echo "    (需要 qt.qt6.6120.addons.qtshadertools.clang_64)"
fi

# ---- 3. Deploy license notices ----
cp "$ROOT/LGPL-3.0.txt" "$ROOT/THIRD-PARTY-NOTICES.md" "$ROOT/LICENSE.md" "$ROOT/LICENSE" "$LIB/" 2>/dev/null || true

echo "Build OK"
echo "  Dynamic lib   : $LIB/libjqt.dylib"
echo "  Run demo      : QT_QPA_PLATFORM=offscreen ./run-macos.sh -AutoClose 3000"
