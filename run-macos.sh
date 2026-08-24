#!/usr/bin/env bash
# ============================================================================
# run-macos.sh - run the JQt demo on macOS
# ============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
LIB="$ROOT/lib"
OUT="$ROOT/out"

if [ ! -f "$LIB/libjqt.dylib" ]; then
  echo "libjqt.dylib not found - run ./build-macos.sh first" >&2
  exit 1
fi

QT_BASE="${QT_BASE:?set QT_BASE to the Qt for macOS install dir}"
export DYLD_FRAMEWORK_PATH="$QT_BASE/lib:${DYLD_FRAMEWORK_PATH:-}"
export DYLD_LIBRARY_PATH="$LIB:${DYLD_LIBRARY_PATH:-}"

exec java \
  -Djava.library.path="$LIB" \
  -Dfile.encoding=UTF-8 \
  -Dstdout.encoding=UTF-8 \
  --enable-native-access=ALL-UNNAMED \
  -cp "$OUT" \
  org.jqt.JQtDemo "$@"
