#!/usr/bin/env bash
# ============================================================================
# run.sh - launch JQt (HMCL-style self-checking launcher) for Linux / macOS /
#          Git Bash on Windows.
#
# What it does (like HMCL does for Minecraft):
#   1. finds a JDK (JQT_JAVA -> JAVA_HOME -> common paths -> java on PATH)
#   2. locates the JQt artifacts (out/ dev classes or dist/jqt-<VERSION>.jar)
#   3. if artifacts are missing, downloads them from the GitHub Release
#      (disable with JQT_NO_DOWNLOAD=1)
#   4. sets the Qt native paths (PATH / LD_LIBRARY_PATH / DYLD_LIBRARY_PATH,
#      QT_QPA_PLATFORM_PLUGIN_PATH=lib/platforms)
#   5. runs the requested class and forwards its exit code
#
# Usage:
#   ./run.sh                            # run the demo (org.jqt.JQtDemo)
#   ./run.sh --auto-close 2000          # close after 2 s (automation)
#   ./run.sh --class org.jqt.SmokeL1    # pick entry class
#   ./run.sh --list                     # list runnable classes in out/
#   ./run.sh org.jqt.JQtDemo -- --my-arg
#
# Environment overrides:
#   JQT_JAVA, JQT_JDK        explicit java binary / JDK home
#   JQT_QTVER=6.11.2         Qt version of the native lib to download
#   JQT_NO_DOWNLOAD=1        never download artifacts
#   JQT_LIB / JQT_JAR / JQT_OUT   override artifact locations
#   JQT_LD_DEBUG=1           print resolved values
# ============================================================================

set -uo pipefail

# ---------------------------------------------------------------- locations
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" && pwd)"
ROOT="$SCRIPT_DIR"
LIB="${JQT_LIB:-$ROOT/lib}"
OUT="${JQT_OUT:-$ROOT/out}"
DIST="$ROOT/dist"
VERSION="$(tr -d '\r\n' < "$ROOT/VERSION" 2>/dev/null || true)"
QTVER="${JQT_QTVER:-6.11.2}"

# ---------------------------------------------------------------- OS detect
case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) JQT_OS="windows" ;;
    Darwin)               JQT_OS="macos" ;;
    Linux)                JQT_OS="linux" ;;
    *)                    JQT_OS="unknown" ;;
esac

# msys/cygwin java wants Windows paths for -Djava.library.path / -cp
native_path() {
    if [ "$JQT_OS" = "windows" ] && command -v cygpath >/dev/null 2>&1; then
        cygpath -w "$1"
    else
        printf '%s' "$1"
    fi
}

native_lib_name() {
    case "$JQT_OS" in
        windows) printf 'jqt.dll' ;;
        macos)   printf 'libjqt.dylib' ;;
        *)       printf 'libjqt.so' ;;
    esac
}

release_asset_for_lib() {
    case "$JQT_OS" in
        windows) printf 'jqt-windows-%s.dll' "$QTVER" ;;
        macos)   printf 'libjqt-macos-%s.dylib' "$QTVER" ;;
        *)       printf 'libjqt-linux-%s.so' "$QTVER" ;;
    esac
}

say() { printf '%s\n' "$*"; }
die() { printf 'error: %s\n' "$*" >&2; exit 1; }

# fetch <url> <dest>
# Windows curl uses Schannel; on some corporate/proxy networks the certificate
# revocation check cannot complete (CRYPT_E_NO_REVOCATION_CHECK). Fall back to
# --ssl-revoke-best-effort (Schannel-only flag) before giving up.
fetch() {
    local url="$1" dest="$2"
    if curl -fsSL -o "$dest" "$url"; then
        return 0
    fi
    if curl --version 2>/dev/null | head -n 1 | grep -qi schannel; then
        say "retrying with --ssl-revoke-best-effort (Schannel revocation check failed) ..."
        curl -fsSL --ssl-revoke-best-effort -o "$dest" "$url"
        return $?
    fi
    return 1
}

# ---------------------------------------------------------------- arguments
CLASS="org.jqt.JQtDemo"
AUTOCLOSE=""
ANIMTHEME=""
QSS=""
RHI=""
FLUENT=""
PROG_ARGS=()
LIST_ONLY=0

while [ $# -gt 0 ]; do
    case "$1" in
        --list)        LIST_ONLY=1; shift ;;
        -h|--help)     sed -n '2,32p' "$0"; exit 0 ;;
        --auto-close)  AUTOCLOSE="${2:-}"; shift 2 ;;
        --class)       CLASS="${2:-}"; shift 2 ;;
        --anim-theme)  ANIMTHEME="${2:-}"; shift 2 ;;
        --qss)         QSS="${2:-}"; shift 2 ;;
        --rhi)         RHI="${2:-}"; shift 2 ;;
        --fluent)      FLUENT=1; shift ;;
        --)            shift; while [ $# -gt 0 ]; do PROG_ARGS+=("$1"); shift; done ;;
        -*)            die "unknown option: $1 (try --help)" ;;
        *)             CLASS="$1"; shift; while [ $# -gt 0 ]; do PROG_ARGS+=("$1"); shift; done ;;
    esac
done

# ---------------------------------------------------------------- JDK
find_java() {
    if [ -n "${JQT_JAVA:-}" ]; then printf '%s' "$JQT_JAVA"; return; fi
    if [ -n "${JQT_JDK:-}" ] && [ -x "$JQT_JDK/bin/java" ]; then printf '%s' "$JQT_JDK/bin/java"; return; fi
    if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then printf '%s' "$JAVA_HOME/bin/java"; return; fi
    for cand in \
        "/c/Program Files/Java/latest/jdk-26/bin/java.exe" \
        "/c/Program Files/Java/latest/jdk-21/bin/java.exe" \
        "/usr/lib/jvm/default-java/bin/java" \
        "/opt/homebrew/opt/openjdk/bin/java" ; do
        [ -x "$cand" ] && { printf '%s' "$cand"; return; }
    done
    if command -v java >/dev/null 2>&1; then command -v java; return; fi
    printf ''
}

JAVA="$(find_java)"
[ -n "$JAVA" ] || die "no JDK found - set JQT_JAVA=/path/to/java or JAVA_HOME"

# ---------------------------------------------------------------- artifacts
JAR="${JQT_JAR:-}"
if [ -z "$JAR" ]; then
    if [ -f "$DIST/jqt-$VERSION.jar" ]; then
        JAR="$DIST/jqt-$VERSION.jar"
    else
        for f in "$DIST"/jqt-*.jar; do
            [ -f "$f" ] && { JAR="$f"; break; }
        done
    fi
fi

NATIVE_NAME="$(native_lib_name)"
[ -f "$LIB/$NATIVE_NAME" ] || NATIVE_OK=0
NATIVE_OK=$([ -f "$LIB/$NATIVE_NAME" ] && echo 1 || echo 0)
HAVE_CLASSES=0
[ -d "$OUT/org/jqt" ] && HAVE_CLASSES=1

if [ "$LIST_ONLY" = "1" ]; then
    say "JQt $VERSION  ($JQT_OS, Qt $QTVER)"
    say "java : $JAVA"
    say "out  : $OUT  (classes: $HAVE_CLASSES)"
    say "jar  : ${JAR:-<none>}"
    say "lib  : $LIB/$NATIVE_NAME  (present: $NATIVE_OK)"
    say ""
    say "runnable classes in out/:"
    if [ "$HAVE_CLASSES" = "1" ]; then
        LIST="$( cd "$OUT/org/jqt" && ls *.class 2>/dev/null \
            | sed 's/\.class$//' \
            | grep -Ev '\$' \
            | grep -E '^(JQtDemo|Smoke|JQt[A-Za-z]*Demo)' \
            | sort )"
        if [ -n "$LIST" ]; then
            printf '%s\n' "$LIST" | sed 's/^/  org.jqt./'
        else
            say "  <none - build-release.ps1 purges Smoke/Demo classes; run ./build.ps1 to regenerate>"
        fi
    else
        say "  <out/ is empty - run ./build.ps1 (Windows) or build-linux.sh / build-macos.sh first>"
    fi
    exit 0
fi

# auto-download missing artifacts from the GitHub Release (HMCL-style)
if { [ "$HAVE_CLASSES" = "0" ] && [ -z "$JAR" ]; } || [ "$NATIVE_OK" = "0" ]; then
    if [ "${JQT_NO_DOWNLOAD:-0}" = "1" ]; then
        die "artifacts missing and JQT_NO_DOWNLOAD=1 (out/ classes: $HAVE_CLASSES, jar: ${JAR:-none}, native: $NATIVE_OK)"
    fi
    command -v curl >/dev/null 2>&1 || die "artifacts missing and curl not available for download"
    [ -n "$VERSION" ] || die "VERSION file is empty - cannot download"
    TAG="v$VERSION"
    BASE="https://github.com/Silent-Studio-CN/JQt/releases/download/$TAG"
    mkdir -p "$DIST" "$LIB"

    if [ -z "$JAR" ]; then
        say "downloading jqt-$VERSION.jar from $TAG ..."
        fetch "$BASE/jqt-$VERSION.jar" "$DIST/jqt-$VERSION.jar" \
            || die "download failed: $BASE/jqt-$VERSION.jar"
        JAR="$DIST/jqt-$VERSION.jar"
    fi
    if [ "$NATIVE_OK" = "0" ]; then
        ASSET="$(release_asset_for_lib)"
        say "downloading $ASSET from $TAG ..."
        fetch "$BASE/$ASSET" "$LIB/$NATIVE_NAME" \
            || die "download failed: $BASE/$ASSET (set JQT_QTVER to match a published Qt version)"
        NATIVE_OK=1
        say "note: the bare lib needs a Qt $QTVER runtime on this machine"
        say "      (Linux/macOS: install Qt or set LD_LIBRARY_PATH / DYLD_LIBRARY_PATH)"
    fi
fi

# ---------------------------------------------------------------- classpath
# Windows java needs Windows-style paths and ';' as the separator.
CP_SEP=":"
[ "$JQT_OS" = "windows" ] && CP_SEP=";"
CP=""
[ "$HAVE_CLASSES" = "1" ] && CP="$(native_path "$OUT")"
if [ -n "$JAR" ]; then
    [ -n "$CP" ] && CP="$CP$CP_SEP$(native_path "$JAR")" || CP="$(native_path "$JAR")"
fi
[ -n "$CP" ] || die "nothing to run - no out/ classes and no jar (run ./build.ps1 first, or drop JQT_NO_DOWNLOAD=1)"

# ---------------------------------------------------------------- Qt env
if [ -d "$LIB/platforms" ]; then
    export QT_QPA_PLATFORM_PLUGIN_PATH="$(native_path "$LIB/platforms")"
fi
case "$JQT_OS" in
    windows) export PATH="$LIB:$PATH" ;;
    macos)   export DYLD_LIBRARY_PATH="$LIB${DYLD_LIBRARY_PATH:+:$DYLD_LIBRARY_PATH}" ;;
    linux)   export LD_LIBRARY_PATH="$LIB${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}" ;;
esac

# ---------------------------------------------------------------- java args
JAVA_ARGS=(
    "-Dfile.encoding=UTF-8"
    "-Dstdout.encoding=UTF-8"
    "--enable-native-access=ALL-UNNAMED"
    "-cp" "$CP"
)
[ "$JQT_OS" = "windows" ] && JAVA_ARGS+=("-Djava.library.path=$(native_path "$LIB")")
[ -n "$AUTOCLOSE" ] && JAVA_ARGS+=("-Djqt.autoClose=$AUTOCLOSE")
[ -n "$ANIMTHEME" ] && JAVA_ARGS+=("-Djqt.animTheme=$ANIMTHEME")
[ -n "$QSS" ] && JAVA_ARGS+=("-Djqt.qss=$QSS")
[ -n "$RHI" ] && JAVA_ARGS+=("-Djqt.rhi=$RHI")
[ -n "$FLUENT" ] && JAVA_ARGS+=("-Djqt.demoFluent=1")

if [ "${JQT_LD_DEBUG:-0}" = "1" ]; then
    say "java      : $JAVA"
    say "classpath : $CP"
    say "lib dir   : $LIB"
    say "qt plugin : ${QT_QPA_PLATFORM_PLUGIN_PATH:-<unset>}"
    say "class     : $CLASS ${PROG_ARGS[*]:-}"
fi

say "JQt $VERSION -> $CLASS  ($JQT_OS, Qt $QTVER)"
exec "$JAVA" "${JAVA_ARGS[@]}" "$CLASS" ${PROG_ARGS[@]+"${PROG_ARGS[@]}"}
