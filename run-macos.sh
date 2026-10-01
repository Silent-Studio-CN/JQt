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

# java 必须用绝对路径:macOS 的 SIP 会剥掉以脚本方式启动的子进程的 DYLD_* 变量,
# 只有 /usr/bin/java 这类系统 stub 才会保留(它自己会重新走 launchd 解析)。
# 走绝对路径的 JVM 直接继承环境,Qt 框架才能被找到。
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA="$JAVA_HOME/bin/java"
else
  JAVA="$(/usr/libexec/java_home 2>/dev/null)/bin/java"
fi
[ -x "$JAVA" ] || { echo "no JDK found - set JAVA_HOME or install a JDK" >&2; exit 1; }

export DYLD_FRAMEWORK_PATH="$QT_BASE/lib:${DYLD_FRAMEWORK_PATH:-}"
export DYLD_LIBRARY_PATH="$LIB:$QT_BASE/lib:${DYLD_LIBRARY_PATH:-}"

# -XstartOnFirstThread:macOS 上 Cocoa 只能在主线程使用(NSWindow 等),
# JVM 默认把 main 放到一个非主线程 -> 真窗口会以
#   "NSWindow should only be instantiated on the main thread" 崩溃。
# 无头(offscreen)冒烟下该参数同样安全。
exec "$JAVA" \
  -XstartOnFirstThread \
  -Djava.library.path="$LIB" \
  -Dfile.encoding=UTF-8 \
  -Dstdout.encoding=UTF-8 \
  --enable-native-access=ALL-UNNAMED \
  "${JQT_OPTS[@]}" \
  -cp "$OUT" \
  "$CLASS"
