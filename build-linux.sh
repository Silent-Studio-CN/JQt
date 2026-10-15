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

# ---- 0. 可选模块开关:先把每个变量都定义成空串 ----
# 本脚本开着 set -euo pipefail:任何"只在 if 分支里赋值、却在赋值前被引用"的变量
# 都会让整个构建以 `unbound variable` 直接挂掉。v1.9.2 就是这样挂过一次 ——
# WEBVIEW_FLAGS 的赋值块被写在 g++ 行**之后**,于是 Linux 6.8.3 构建报
#   ./build-linux.sh: line 104: WEBVIEW_FLAGS: unbound variable
# (那次被误读成"加了 -DJQT_HAVE_SVG 导致构建失败",其实与 QtSvg 无关)。
# 这里统一前置初始化,从结构上消灭这一类错误。
SVG_FLAGS=""
WEBVIEW_FLAGS=""
HELP_FLAGS=""
GEO_FLAGS=""
QML_FLAGS=""
MM_FLAGS=""
CHART_FLAGS=""
WS_FLAGS=""
QUICK3D_FLAGS=""

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

# 插件目录在 <Qt 安装前缀>/plugins —— **不是** <前缀>/lib/plugins。
# v1.9.2 的失败尝试写的是 "$QTLIB/plugins/sqldrivers",在 CI 的 SDK 布局下永远不存在,
# 于是 macOS 日志里打出的是 "SQLite driver not found in this Qt"(探测用了错的路径)。
# Debian 布局把插件放在 <QTLIB>/qt6/plugins,所以两种都探一下。
QT_PLUGINS="$QT_BASE/plugins"
if [ ! -d "$QT_PLUGINS/sqldrivers" ]; then
  if [ -d "$QTLIB/qt6/plugins/sqldrivers" ]; then
    QT_PLUGINS="$QTLIB/qt6/plugins"
  else
    QT_PLUGINS="$QTLIB/plugins"
  fi
fi

echo "==> Compiling native bridge (libjqt.so)"
# 同 macOS:QML 需要 QtQmlIntegration 的头,缺了就不要开(否则编译期报错)
if ls "$QTLIB"/libQt6Help.so* >/dev/null 2>&1; then
  HELP_FLAGS="-DJQT_HAVE_HELP -I$QTINC/QtHelp -lQt6Help"
  echo "==> QtHelp found - QHelpEngineCore enabled"
else
  echo "==> QtHelp not found - QHelpEngineCore will report unavailable"
fi

if ls "$QTLIB"/libQt6Positioning.so* >/dev/null 2>&1; then
  GEO_FLAGS="-DJQT_HAVE_POSITIONING -I$QTINC/QtPositioning -lQt6Positioning"
  echo "==> QtPositioning found - QGeoCoordinate enabled"
else
  echo "==> QtPositioning not found - QGeoCoordinate will report unavailable"
fi

if ls "$QTLIB"/libQt6Quick.so* >/dev/null 2>&1 && [ -f "$QTINC/QtQmlIntegration/qqmlintegration.h" ]; then
  QML_FLAGS="-DJQT_HAVE_QML -I$QTINC/QtQuick -I$QTINC/QtQml -I$QTINC/QtQmlIntegration -lQt6Quick -lQt6Qml"
  echo "==> QtQuick found - QQuickView enabled"
else
  echo "==> QtQuick not found - QQuickView will report unavailable"
fi

if ls "$QTLIB"/libQt6Multimedia.so* >/dev/null 2>&1; then
  MM_FLAGS="-DJQT_HAVE_MULTIMEDIA -I$QTINC/QtMultimedia -lQt6Multimedia"
  echo "==> QtMultimedia found - QMediaPlayer enabled"
else
  echo "==> QtMultimedia not found - QMediaPlayer will report unavailable"
fi

if ls "$QTLIB"/libQt6Charts.so* >/dev/null 2>&1; then
  CHART_FLAGS="-DJQT_HAVE_CHARTS -I$QTINC/QtCharts -lQt6Charts"
  echo "==> QtCharts found - QChart enabled"
else
  echo "==> QtCharts not found - QChart will report unavailable"
fi

if ls "$QTLIB"/libQt6WebSockets.so* >/dev/null 2>&1; then
  WS_FLAGS="-DJQT_HAVE_WEBSOCKETS -I$QTINC/QtWebSockets -lQt6WebSockets"
  echo "==> QtWebSockets found - QWebSocket enabled"
else
  echo "==> QtWebSockets not found - QWebSocket will report unavailable"
fi

# QtSvg(v1.9.2 补漏):实时证据 —— qtsvg 在 Qt 官方仓库里是**独立于 qtbase 的一个 7z**,
# 位于 qt.qt6.<ver>.linux_gcc_64/ 目录下(7z l 实测内容:lib/libQt6Svg.so、
# lib/libQt6Svg.so.6.12.0、include/QtSvg/QSvgRenderer)。
# 6.8.3 与 6.12.0 都是这个结构。CI 以前只解 qtbase -> libQt6Svg.so 根本不存在,
# 所以 QSvgRenderer 在 Linux 上一直 SKIP(不是没传宏,是模块压根没装)。
# 探测条件同时要求头文件,避免"库在、头不在"时反而把编译搞挂。
if ls "$QTLIB"/libQt6Svg.so* >/dev/null 2>&1 && [ -f "$QTINC/QtSvg/QSvgRenderer" ]; then
  SVG_FLAGS="-DJQT_HAVE_SVG -I$QTINC/QtSvg -lQt6Svg"
  echo "==> QtSvg found - QSvgRenderer enabled"
else
  echo "==> QtSvg not found (需要 $QTLIB/libQt6Svg.so* 与 $QTINC/QtSvg/QSvgRenderer) - QSvgRenderer will report unavailable"
fi

# QtWebView(v1.9.2 P2):判定条件与实际文件布局一致 —— 只认 libQt6WebView.so*。
# CI 的 Linux Qt 里**没有** qtwebview:它是独立 addon 包
# (qt.qt6.6120.addons.qtwebview.linux_gcc_64),CI 只解 qtbase/qtsvg 系列。
# 这里如实打印"该 Qt 未带 qtwebview",不再让人以为"探测过了只是没生效"。
if ls "$QTLIB"/libQt6WebView.so* >/dev/null 2>&1; then
  WEBVIEW_FLAGS="-DJQT_HAVE_WEBVIEW -I$QTINC/QtWebView -lQt6WebView"
  echo "==> QtWebView found ($QTLIB) - QWebView enabled"
else
  echo "==> 该 Qt 未带 qtwebview(未找到 $QTLIB/libQt6WebView.so*) - QWebView will report unavailable"
fi

# QtQuick3D(v1.9.3):Quick 3D 是 QML 模块(import QtQuick3D),加载走 QQuickView ——
# 本库**不链接** libQt6Quick3D(与 Windows 的 build.ps1 同一设计),所以这里做两件事:
#   1) 探测并把 JQT_HAVE_QUICK3D 传给编译器(纯信息位:status() 打印用,不参与可用性判定);
#   2) 把运行期真需要的两样东西部署到 lib/(见下面 2.6)。
# 判定条件与实际布局一致:libQt6Quick3D.so*(C++ 侧插件链要它)+
# <Qt前缀>/qml/QtQuick3D/qmldir(QML 模块)。少了任何一个都只会"绿着 SKIP"。
QUICK3D_QML=""
for d in "$QT_BASE/qml/QtQuick3D" "$QTLIB/qml/QtQuick3D"; do
  if [ -f "$d/qmldir" ]; then QUICK3D_QML="$d"; break; fi
done
QUICK3D_LIB=0
if ls "$QTLIB"/libQt6Quick3D.so* >/dev/null 2>&1; then QUICK3D_LIB=1; fi
if [ "$QUICK3D_LIB" = 1 ]; then
  QUICK3D_FLAGS="-DJQT_HAVE_QUICK3D"
fi
if [ "$QUICK3D_LIB" = 1 ] && [ -n "$QUICK3D_QML" ]; then
  echo "==> QtQuick3D found (lib=$QTLIB, qml=$QUICK3D_QML) - QQuick3D enabled (可用性由运行期探测)"
else
  echo "==> 该 Qt 未带 qtquick3d(需要 $QTLIB/libQt6Quick3D.so* 与 <Qt前缀>/qml/QtQuick3D/qmldir) - Quick3D will report unavailable"
fi

# QtDBus(v1.9.2 P1 收尾):qtbase 自带,链接早已存在(桥内屏保抑制/通知在用),
# 这里只是再加上 -DJQT_HAVE_DBUS,把通用面(QDBusConnection/QDBusMessage)编进库。
echo "==> QtDBus: assume available (qtbase) - QDBusConnection enabled"

g++ -std=c++17 -O2 -shared -fPIC     -o "$LIB/libjqt.so"     -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux"     -I"$QTINC" -I"$QTINC/QtWidgets" -I"$QTINC/QtGui" -I"$QTINC/QtCore" -I"$QTINC/QtDBus" -I"$QTINC/QtPrintSupport" -I"$QTINC/QtSql" -I"$QTINC/QtOpenGLWidgets" -I"$QTINC/QtOpenGL" -I"$QTINC/QtSerialPort" -I"$QTINC/QtNetwork" -DJQT_HAVE_NETWORK -DJQT_HAVE_SQL_MODELS -DJQT_HAVE_DBUS     -I"$NATIVE"     "$NATIVE/jqt_bridge.cpp"     -L"$QTLIB" -lQt6Widgets -lQt6Gui -lQt6Core -lQt6PrintSupport -lQt6Sql -lQt6OpenGLWidgets -lQt6OpenGL -lQt6SerialPort -lQt6DBus -lQt6Network $QUICK3D_FLAGS $WS_FLAGS $CHART_FLAGS $MM_FLAGS $QML_FLAGS $GEO_FLAGS $HELP_FLAGS $SVG_FLAGS $WEBVIEW_FLAGS

# ---- 2.5 Deploy the SQLite driver where the bridge actually looks for it ----
# 实测结论(native/jqt_bridge.cpp:6919-6965,本地用真 Qt 6.12.0 跑过):
#   QSqlDatabase::drivers() 返回的键带 Q 前缀 -> "QSQLITE";
#   而 Java 侧用的是不带前缀的 "SQLITE" -> isDriverAvailable("SQLITE") **恒为 false**,
#   与插件装没装无关。真正把驱动救回来的是桥里的兜底分支,它按
#       <libraryPath>/sqldrivers/q<driver><ext>      找插件文件。
# 那个文件名是 **Windows 命名**(qsqlite.dll)—— 所以 Windows 上一直能用;
# 而 Linux/macOS 的插件叫 libqsqlite.so / libqsqlite.dylib,兜底永远找不到 ->
# "本平台无 SQLite 驱动"。库里的 libQt6Sql 探针也是同一个道理。
# 桥里还会 addLibraryPath(<cwd>/lib),而 CI 的冒烟就是在仓库根目录跑 ./run-linux.sh,
# 所以把插件按兜底期望的名字放到 <repo>/lib/sqldrivers/ 即可命中。
# 注意:这里必须用**当前 $QT_BASE 的**插件 —— 两个 Qt 版本都会跑一遍本脚本,
# 后跑的 6.12.0 会覆盖成 6.12.0 的插件,而冒烟用的正是 6.12.0,两者对齐。
if [ -f "$QT_PLUGINS/sqldrivers/libqsqlite.so" ]; then
  mkdir -p "$LIB/sqldrivers"
  cp -f "$QT_PLUGINS/sqldrivers/libqsqlite.so" "$LIB/sqldrivers/qsqlite.so"
  echo "==> SQLite driver deployed: lib/sqldrivers/qsqlite.so"
else
  echo "==> 警告:本 Qt 未带 SQLite 驱动插件($QT_PLUGINS/sqldrivers/libqsqlite.so 不存在) - SmokeSqlModel 仍会 SKIP"
fi

# ---- 2.6 Deploy QtQuick3D runtime bits (v1.9.3) ----
# 两处都放(与 Windows 的 build.ps1 同一套做法):
#   · 开发/CI 下 libQt6Core.so 是从 $QTLIB 加载的,Qt 6 的"可重定位前缀"就等于 $QT_BASE,
#     所以 QmlImportsPath = $QT_BASE/qml —— Quick3D 的 QML 模块是从**Qt 安装目录**找到的
#     (前提:CI 真的装了 qtquick3d,见 ci.yml 的 Install QtQuick3D 那步);
#   · 打包/自带 Qt 的运行包需要一份随仓库走的副本 —— lib/qml(打包布局)+ qml/(开发布局;
#     桥在定义了 JQT_HAVE_WEBVIEW 时会把 cwd/qml 加进 QML2_IMPORT_PATH)。
if [ -n "$QUICK3D_QML" ]; then
  for dst in "$LIB/qml/QtQuick3D" "$ROOT/qml/QtQuick3D"; do
    mkdir -p "$dst"
    cp -R "$QUICK3D_QML/." "$dst/"
  done
  echo "==> QtQuick3D QML 模块已部署:lib/qml/QtQuick3D + qml/QtQuick3D"
  echo "    提示:tools/bundle-qt.sh 只打包 Qt 库与 plugins,**不打 qml/** —— 自带 Qt 的运行包里"
  echo "    import QtQuick3D 仍会找不到模块,需要把 qml/QtQuick3D 一并打进包里。"
else
  echo "==> 未部署 QtQuick3D QML 模块(本 Qt 未带 qtquick3d)"
fi

# ShaderTools 是 Quick3D 插件链的最后一环:
#   qquick3dplugin -> libQt6Quick3D -> libQt6Quick3DRuntimeRender -> **libQt6ShaderTools**
# 缺它插件加载失败(报 "Cannot load library .../qquick3dplugin.so")。注意要连**带版本号的
# .so**(SONAME,如 libQt6ShaderTools.so.6)一起拷:只拷无版本号那个名字,运行期照样找不到。
SHADERTOOLS=0
for so in "$QTLIB"/libQt6ShaderTools.so*; do
  if [ -e "$so" ]; then cp -a "$so" "$LIB/"; SHADERTOOLS=1; fi
done
if [ "$SHADERTOOLS" = 1 ]; then
  echo "==> Qt6ShaderTools 已部署到 lib/(Quick3D 插件链最后一环)"
else
  echo "==> 警告:本 Qt 未带 Qt Shader Tools($QTLIB/libQt6ShaderTools.so* 不存在)—— 即使装了 qtquick3d,"
  echo "    qquick3dplugin 也加载不出来,Quick3D 只会 SKIP(需要 qt.qt6.6120.addons.qtshadertools.linux_gcc_64)"
fi

# ---- 3. Deploy license notices (LGPL compliance) ----
cp "$ROOT/LGPL-3.0.txt" "$ROOT/THIRD-PARTY-NOTICES.md" "$ROOT/LICENSE.md" "$ROOT/LICENSE" "$LIB/" 2>/dev/null || true

echo "Build OK"
echo "  Dynamic lib   : $LIB/libjqt.so"
echo "  Run demo      : QT_QPA_PLATFORM=offscreen ./run-linux.sh -AutoClose 3000"
