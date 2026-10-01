#!/usr/bin/env bash
# ============================================================================
# bundle-qt.sh - 把 Qt 运行库和 JQt 原生库打进一个自包含运行包
#
#   macOS: QT_BASE=$HOME/Qt/6.12.0/macos ./tools/bundle-qt.sh
#   Linux: QT_BASE=/opt/6.12.0/gcc_64     ./tools/bundle-qt.sh
#
# 产物(在 dist/ 下):
#   macOS: jqt-<版本>-macos-x64-qt-runtime.tar.gz
#   Linux: jqt-<版本>-linux-x64-qt-runtime.tar.gz
#   内含: 原生库 + Qt 运行库 + 许可证 + run-with-bundled-qt.sh 启动器
#
# 设计取舍:不重写二进制里的 install_name/rpath(需要 install_name_tool/patchelf,
# 跨平台差异大且容易改坏),而是随包提供启动器 —— 它把 Qt 库路径注入
# DYLD_FRAMEWORK_PATH(macOS)/ LD_LIBRARY_PATH(Linux) 后再启动 JVM。
# 关键点:启动器里 java 必须用**绝对路径**,否则 macOS 的 SIP 会剥掉 DYLD_*。
# ============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
QT_BASE="${QT_BASE:?set QT_BASE to the Qt install dir (e.g. \$HOME/Qt/6.12.0/macos)}"
VER="$(cat "$ROOT/VERSION" | tr -d '\r\n')"
OS="$(uname -s)"

case "$OS" in
  Darwin)
    NATIVE_LIB="libjqt.dylib"; PLATFORM="macos-x64"; QTDIR="$QT_BASE/lib"
    # 整套 Qt 框架都带上:只带子集会让动态链接器回落到系统 Qt,出现
    # "libQt6DBus.so.6: undefined symbol" 这类混版问题
    QTPATTERNS=("Qt*.framework")
    ;;
  Linux)
    NATIVE_LIB="libjqt.so"; PLATFORM="linux-x64"; QTDIR="$QT_BASE/lib"
    QTPATTERNS=("libQt6"*.so* "libicu"*.so*)
    ;;
  *) echo "unsupported OS: $OS" >&2; exit 1 ;;
esac

[ -f "$ROOT/lib/$NATIVE_LIB" ] || { echo "lib/$NATIVE_LIB not found - build first" >&2; exit 1; }
[ -d "$QTDIR" ] || { echo "Qt lib dir not found: $QTDIR" >&2; exit 1; }

NAME="jqt-$VER-$PLATFORM-qt-runtime"
STAGE="$(mktemp -d)/$NAME"
mkdir -p "$STAGE/qt" "$STAGE/lib" "$STAGE/licenses"

echo "==> 收集 Qt 运行库 -> $STAGE/qt"
copied=0
for pat in "${QTPATTERNS[@]}"; do
  for src in "$QTDIR"/$pat; do
    [ -e "$src" ] || continue
    if [ -d "$src" ]; then cp -R "$src" "$STAGE/qt/"; else cp "$src" "$STAGE/qt/"; fi
    copied=$((copied + 1))
  done
done
echo "    $copied 个 Qt 运行库/框架"

# Qt 插件(平台插件/图片格式/SQL 驱动/样式)必须随包,否则 offscreen/xcb 平台起不来
if [ -d "$QT_BASE/plugins" ]; then
  mkdir -p "$STAGE/qt/plugins"
  cp -R "$QT_BASE/plugins/." "$STAGE/qt/plugins/"
  echo "    plugins: $(find "$STAGE/qt/plugins" -type f | wc -l) 个文件"
fi

cp "$ROOT/lib/$NATIVE_LIB" "$STAGE/lib/"
[ -f "$ROOT/LGPL-3.0.txt" ] && cp "$ROOT/LGPL-3.0.txt" "$STAGE/licenses/"
[ -f "$ROOT/THIRD-PARTY-NOTICES.md" ] && cp "$ROOT/THIRD-PARTY-NOTICES.md" "$STAGE/licenses/"
[ -d "$QTDIR/../LICENSES" ] && cp -R "$QTDIR/../LICENSES" "$STAGE/licenses/Qt-LICENSES" 2>/dev/null || true

# ---- 启动器:注入 Qt 库路径后再起 JVM(java 必须绝对路径)----
LAUNCHER="$STAGE/run-with-bundled-qt.sh"
if [ "$OS" = "Darwin" ]; then
cat > "$LAUNCHER" <<'EOF'
#!/usr/bin/env bash
# 用本包自带的 Qt 运行库启动 JQt 应用
#   ./run-with-bundled-qt.sh [JQt 主类] [其他 java 参数...]
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then JAVA="$JAVA_HOME/bin/java"
else JAVA="$(/usr/libexec/java_home 2>/dev/null)/bin/java"; fi
[ -x "$JAVA" ] || { echo "no JDK found" >&2; exit 1; }
export DYLD_FRAMEWORK_PATH="$HERE/qt:${DYLD_FRAMEWORK_PATH:-}"
export DYLD_LIBRARY_PATH="$HERE/lib:$HERE/qt:${DYLD_LIBRARY_PATH:-}"
export QT_PLUGIN_PATH="$HERE/qt/plugins:${QT_PLUGIN_PATH:-}"
CLASS="${1:-org.jqt.JQtDemo}"; shift || true
exec "$JAVA" -XstartOnFirstThread -Djava.library.path="$HERE/lib" \
  --enable-native-access=ALL-UNNAMED "$@" -cp "$HERE/lib/*:$HERE:$HERE/out:." "$CLASS"
EOF
else
cat > "$LAUNCHER" <<'EOF'
#!/usr/bin/env bash
# 用本包自带的 Qt 运行库启动 JQt 应用
#   ./run-with-bundled-qt.sh [JQt 主类] [其他 java 参数...]
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then JAVA="$JAVA_HOME/bin/java"
else JAVA="$(command -v java || true)"; fi
[ -x "$JAVA" ] || { echo "no JDK found - set JAVA_HOME" >&2; exit 1; }
export LD_LIBRARY_PATH="$HERE/lib:$HERE/qt:${LD_LIBRARY_PATH:-}"
export QT_PLUGIN_PATH="$HERE/qt/plugins:${QT_PLUGIN_PATH:-}"
CLASS="${1:-org.jqt.JQtDemo}"; shift || true
exec "$JAVA" -Djava.library.path="$HERE/lib" \
  --enable-native-access=ALL-UNNAMED "$@" -cp "$HERE/lib/*:$HERE:$HERE/out:." "$CLASS"
EOF
fi
chmod +x "$LAUNCHER"

cat > "$STAGE/README-BUNDLE.md" <<EOF
# JQt $VER — $PLATFORM 自带 Qt 运行库包

- \`lib/$NATIVE_LIB\`  —— JQt 原生库
- \`qt/\`                —— 打包进来的 Qt 运行库($copied 项)
- \`run-with-bundled-qt.sh\` —— 启动器:注入 Qt 库路径后启动 JVM
- \`licenses/\`          —— LGPL-3.0 与 Qt 许可证

用法:
\`\`\`bash
./run-with-bundled-qt.sh org.jqt.JQtDemo          # 跑 demo
./run-with-bundled-qt.sh com.example.MyApp        # 跑你的应用(把 jar/classes 放同目录)
\`\`\`

为什么要启动器:JQt 的 jar 是纯 Java,但原生库要链接 Qt。此包把 Qt 运行库一并带上,
启动器负责把路径注入 \`DYLD_FRAMEWORK_PATH\`/\`LD_LIBRARY_PATH\` 后再起 JVM(不改二进制)。
EOF

mkdir -p "$ROOT/dist"
OUT="$ROOT/dist/$NAME.tar.gz"
tar -czf "$OUT" -C "$(dirname "$STAGE")" "$NAME"
size=$(du -h "$OUT" | cut -f1)
echo "==> 产物: $OUT ($size)"
echo "    内容: $(tar -tzf "$OUT" | wc -l) 项"
