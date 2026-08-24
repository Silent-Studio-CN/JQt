#!/usr/bin/env bash
# ============================================================================
# run-linux.sh - run a JQt demo on Linux
#   Headless smoke test:  QT_QPA_PLATFORM=offscreen ./run-linux.sh -AutoClose 2000
#   With a desktop:       ./run-linux.sh
#
# Supported options (converted to Java system properties):
#   -AutoClose <ms>   auto quit after ms (maps to -Djqt.autoClose)
#   -Class <name>     entry class (default org.jqt.JQtDemo)
# ============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
LIB="$ROOT/lib"
OUT="$ROOT/out"

if [ ! -f "$LIB/libjqt.so" ]; then
  echo "libjqt.so not found - run ./build-linux.sh first" >&2
  exit 1
fi

CLASS="org.jqt.JQtDemo"
JQT_OPTS=()
while [ $# -gt 0 ]; do
  case "$1" in
    -AutoClose) JQT_OPTS+=("-Djqt.autoClose=$2"); shift 2 ;;
    -Class)     CLASS="$2"; shift 2 ;;
    *)          JQT_OPTS+=("$1"); shift ;;
  esac
done

# Locate Qt runtime libs (Debian/Ubuntu or Qt installer layout)
QTLIB="${QT_BASE:-/usr}/lib"
[ -d "$QTLIB/x86_64-linux-gnu" ] && QTLIB="$QTLIB/x86_64-linux-gnu"

export LD_LIBRARY_PATH="$LIB:$QTLIB:${LD_LIBRARY_PATH:-}"

exec java \
  -Djava.library.path="$LIB" \
  -Dfile.encoding=UTF-8 \
  -Dstdout.encoding=UTF-8 \
  --enable-native-access=ALL-UNNAMED \
  "${JQT_OPTS[@]}" \
  -cp "$OUT" \
  "$CLASS"
