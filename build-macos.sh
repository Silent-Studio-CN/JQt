#!/usr/bin/env bash
# ============================================================================
# build-macos.sh - JQt macOS one-click build
#   Produces lib/libjqt.dylib + deploys license notices.
#
# Prerequisites:
#   Xcode Command Line Tools (clang++) and a JDK (JAVA_HOME set)
#   Qt for macOS: aqt install-qt mac desktop 6.11.2 macos -O ~/Qt
#   QT_BASE=$HOME/Qt/6.11.2/macos ./build-macos.sh
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
QTINC="$QTLIB/QtWidgets.framework/Headers $QTLIB/QtGui.framework/Headers $QTLIB/QtCore.framework/Headers"

echo "==> Compiling native bridge (libjqt.dylib)"
clang++ -std=c++17 -O2 -shared -fPIC \
    -o "$LIB/libjqt.dylib" \
    -install_name @rpath/libjqt.dylib \
    -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/darwin" \
    -I$QTINC \
    -I"$NATIVE" \
    "$NATIVE/jqt_bridge.cpp" \
    -F"$QTLIB" -framework QtWidgets -framework QtGui -framework QtCore

# ---- 3. Deploy license notices ----
cp "$ROOT/LGPL-3.0.txt" "$ROOT/THIRD-PARTY-NOTICES.md" "$ROOT/LICENSE.md" "$ROOT/LICENSE" "$LIB/" 2>/dev/null || true

echo "Build OK"
echo "  Dynamic lib   : $LIB/libjqt.dylib"
echo "  Run demo      : QT_QPA_PLATFORM=offscreen ./run-macos.sh -AutoClose 3000"
