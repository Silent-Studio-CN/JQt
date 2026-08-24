#!/usr/bin/env bash
# ============================================================================
# run-linux.sh - run the JQt demo on Linux
#   Headless smoke test:  QT_QPA_PLATFORM=offscreen ./run-linux.sh -AutoClose 2000
#   With a desktop:       ./run-linux.sh
# ============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
LIB="$ROOT/lib"
OUT="$ROOT/out"

if [ ! -f "$LIB/libjqt.so" ]; then
  echo "libjqt.so not found - run ./build-linux.sh first" >&2
  exit 1
fi

# Locate Qt runtime libs (Debian/Ubuntu or Qt installer layout)
QTLIB="${QT_BASE:-/usr}/lib"
[ -d "$QTLIB/x86_64-linux-gnu" ] && QTLIB="$QTLIB/x86_64-linux-gnu"

export LD_LIBRARY_PATH="$LIB:$QTLIB:${LD_LIBRARY_PATH:-}"

exec java \
  -Djava.library.path="$LIB" \
  -Dfile.encoding=UTF-8 \
  -Dstdout.encoding=UTF-8 \
  --enable-native-access=ALL-UNNAMED \
  -cp "$OUT" \
  org.jqt.JQtDemo "$@"
