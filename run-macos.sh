#!/usr/bin/env bash
# ============================================================================
# run-macos.sh - run a JQt demo on macOS
#   Headless smoke test:  QT_QPA_PLATFORM=offscreen ./run-macos.sh -AutoClose 2000
#
# Supported options (converted to Java system properties):
#   -AutoClose <ms>   auto quit after ms (maps to -Djqt.autoClose)
#   -Class <name>     entry class (default org.jqt.JQtDemo)
# ============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
LIB="$ROOT/lib"
OUT="$ROOT/out"

if [ ! -f "$LIB/libjqt.dylib" ]; then
  echo "libjqt.dylib not found - run ./build-macos.sh first" >&2
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

QT_BASE="${QT_BASE:?set QT_BASE to the Qt for macOS install dir}"
export DYLD_FRAMEWORK_PATH="$QT_BASE/lib:${DYLD_FRAMEWORK_PATH:-}"
export DYLD_LIBRARY_PATH="$LIB:${DYLD_LIBRARY_PATH:-}"

exec java \
  -Djava.library.path="$LIB" \
  -Dfile.encoding=UTF-8 \
  -Dstdout.encoding=UTF-8 \
  --enable-native-access=ALL-UNNAMED \
  "${JQT_OPTS[@]}" \
  -cp "$OUT" \
  "$CLASS"
