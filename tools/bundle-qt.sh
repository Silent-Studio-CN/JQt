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
    NATIVE_LIB="libjqt.dylib"; # 架构自适应:Apple Silicon 上是 arm64,Intel 上是 x64(真机实测:写死 macos-x64 会让 arm64 产物自称 x64)。
PLATFORM="macos-$(uname -m | sed 's/x86_64/x64/' | sed 's/aarch64/arm64/' | sed 's/arm64/arm64/')"; QTDIR="$QT_BASE/lib"
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

# ---- QML 模块:只带"需要的最小集合"(v1.9.3) ----
# 为什么必须带:Qt 把 QtQuick/QtQml 编进了库的资源里(不需要文件),但 **QtQuick3D 是文件形态的
# QML 模块** —— 不打进包里,发布包里的 `import QtQuick3D` 就报
#   module "QtQuick3D" is not installed
# (与 Windows 上 lib/qml 那一课同一个道理:Qt 6 的可重定位前缀 = Qt6Core 所在目录,
#  启动器把 <包>/qt 放进库路径 ⇒ QmlImportsPath = <包>/qt/qml;启动器另外显式导出
#  QML2_IMPORT_PATH 兜底。)
# 取舍:**不**无脑拷整棵 qml/ 树(本机 6.12.0 实测 ~10.1 MB / 2292 文件),只带 QtQuick3D(含
# qquick3dplugin 与各子模块 QtQuick3D.Helpers/Particles3D/Effects/...),再剪掉 Design Studio
# 用的工具目录 designer/ ⇒ ~1.9 MB / 356 文件。子模块是独立 module,顶层 qmldir 不引用它们,
# 所以剪枝不会影响 `import QtQuick3D` 本身。
QML_MODULES=("QtQuick3D")
QML_PRUNE=("designer")            # Qt Design Studio 编辑器插件,运行期用不到
qml_files=0
for m in "${QML_MODULES[@]}"; do
  qmlsrc="$QT_BASE/qml/$m"
  if [ ! -d "$qmlsrc" ]; then
    echo "    !! 该 Qt 没有 qml/$m —— 包里 import $m 会失败(装 qtquick3d addon 后重跑本脚本)"
    continue
  fi
  mkdir -p "$STAGE/qt/qml"
  cp -R "$qmlsrc" "$STAGE/qt/qml/$m"
  for ex in "${QML_PRUNE[@]}"; do
    if [ -d "$STAGE/qt/qml/$m/$ex" ]; then
      rm -rf "$STAGE/qt/qml/$m/$ex"
      echo "    剪掉工具目录 qml/$m/$ex(运行期不需要)"
    fi
  done
  if [ ! -f "$STAGE/qt/qml/$m/qmldir" ]; then
    echo "    !! qml/$m/qmldir 缺失 —— 该模块在包里不可用" >&2
  fi
  n=$(find "$STAGE/qt/qml/$m" -type f | wc -l | tr -d ' ')
  sz=$(du -sh "$STAGE/qt/qml/$m" | cut -f1)
  echo "    qml/$m: $n 个文件 ($sz)"
  qml_files=$((qml_files + n))
done
echo "    QML 模块合计: $qml_files 个文件"

# ---- 断言 Quick3D 的 C++ 侧也在包里 ----
# 插件链:qquick3dplugin -> Qt6Quick3D -> Qt6Quick3DRuntimeRender -> **Qt6ShaderTools**。
# 它们本来靠上面的 QTPATTERNS 通配进来(Linux: libQt6*.so* / macOS: Qt*.framework),
# 这里只做存在性检查 —— 免得哪天通配改了、包里少一环却没人知道(那正是"能探测但渲染不出来"的成因)。
have_q3d=0
have_st=0
if [ "$OS" = "Darwin" ]; then
  if [ -d "$STAGE/qt/QtQuick3D.framework" ] || ls "$STAGE/qt"/libQt6Quick3D*.dylib >/dev/null 2>&1; then have_q3d=1; fi
  if [ -d "$STAGE/qt/QtShaderTools.framework" ] || ls "$STAGE/qt"/libQt6ShaderTools*.dylib >/dev/null 2>&1; then have_st=1; fi
else
  if ls "$STAGE/qt"/libQt6Quick3D*.so* >/dev/null 2>&1; then have_q3d=1; fi
  if ls "$STAGE/qt"/libQt6ShaderTools*.so* >/dev/null 2>&1; then have_st=1; fi
fi
if [ "$have_q3d" = 1 ]; then echo "    Quick3D 运行库 : 在"; else echo "    Quick3D 运行库 : !! 缺"; fi
if [ "$have_st" = 1 ]; then echo "    QtShaderTools  : 在"; else echo "    QtShaderTools  : !! 缺"; fi
if [ "$have_q3d" != 1 ] || [ "$have_st" != 1 ]; then
  echo "    !! 包里缺 Quick3D 运行库或 ShaderTools:装 qtquick3d + qtshadertools 两个 addon 后重跑" >&2
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
# QML 模块在 <包>/qt/qml(Qt 的可重定位前缀本来也能推出来 —— 这里显式给,省得靠推导)
export QML2_IMPORT_PATH="$HERE/qt/qml:${QML2_IMPORT_PATH:-}"
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
# QML 模块在 <包>/qt/qml(Qt 的可重定位前缀本来也能推出来 —— 这里显式给,省得靠推导)
export QML2_IMPORT_PATH="$HERE/qt/qml:${QML2_IMPORT_PATH:-}"
CLASS="${1:-org.jqt.JQtDemo}"; shift || true
exec "$JAVA" -Djava.library.path="$HERE/lib" \
  --enable-native-access=ALL-UNNAMED "$@" -cp "$HERE/lib/*:$HERE:$HERE/out:." "$CLASS"
EOF
fi
chmod +x "$LAUNCHER"

cat > "$STAGE/README-BUNDLE.md" <<EOF
# JQt $VER — $PLATFORM 自带 Qt 运行库包

- \`lib/$NATIVE_LIB\`  —— JQt 原生库
- \`qt/\`                —— 打包进来的 Qt 运行库($copied 项);Quick3D 的
  \`QtQuick3D*.framework\`/\`libQt6Quick3D*.so*\` 与 \`QtShaderTools\` 也在其中
- \`qt/qml/QtQuick3D\`   —— QtQuick3D 的 QML 模块(\`import QtQuick3D\` 靠它;Qt 只把
  QtQuick/QtQml 编进库资源,Quick3D 必须带文件;$qml_files 个文件)
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
