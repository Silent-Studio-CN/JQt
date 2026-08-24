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
#   QT_BASE=$HOME/Qt/6.11.2/gcc_64 ./build-linux.sh   (Qt online installer layout)
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
g++ -std=c++17 -O2 -shared -fPIC     -o "$LIB/libjqt.so"     -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux"     -I"$QTINC" -I"$QTINC/QtWidgets" -I"$QTINC/QtGui" -I"$QTINC/QtCore"     -I"$NATIVE"     "$NATIVE/jqt_bridge.cpp"     -L"$QTLIB" -lQt6Widgets -lQt6Gui -lQt6Core

# ---- 3. Deploy license notices (LGPL compliance) ----
cp "$ROOT/LGPL-3.0.txt" "$ROOT/THIRD-PARTY-NOTICES.md" "$ROOT/LICENSE.md" "$ROOT/LICENSE" "$LIB/" 2>/dev/null || true

echo "Build OK"
echo "  Dynamic lib   : $LIB/libjqt.so"
echo "  Run demo      : QT_QPA_PLATFORM=offscreen ./run-linux.sh -AutoClose 3000"
