# ============================================================================
# build.ps1 - JQt one-click build
#   1. javac compiles Java sources and generates JNI headers (javac -h)
#   2. g++ compiles the native bridge into jqt.dll
#   3. windeployqt deploys the Qt runtime into lib
#   4. copies the QPA platform plugin + writes qt.conf (lib is self-contained)
#
# Prerequisites (already installed on this machine):
#   JDK 26      C:\Program Files\Java\latest\jdk-26
#   Qt 6.12.0   D:\Qt\6.12.0\mingw_64
#   MinGW 13.1  D:\Qt\Tools\mingw1310_64
#
# NOTE: this file must stay ASCII-only (Windows PowerShell 5.1 reads
#       BOM-less files as ANSI and would garble non-ASCII text).
# NOTE: --release 17 keeps the published jar runnable on Java 17+ (LTS).
# ============================================================================

param(
    [string]$JDK    = "C:\Program Files\Java\latest\jdk-26",
    [string]$QtRoot = "D:\Qt\6.12.0",
    [string]$Mingw  = "D:\Qt\Tools\mingw1310_64"
)

$ErrorActionPreference = "Stop"

$Root      = Split-Path -Parent $MyInvocation.MyCommand.Path
$Kit       = Join-Path $QtRoot "mingw_64"

$OutDir    = Join-Path $Root "out"             # Java bytecode
$LibDir    = Join-Path $Root "lib"             # jqt.dll + Qt runtime
$NativeDir = Join-Path $Root "native"
$GenDir    = Join-Path $NativeDir "generated"  # JNI headers from javac -h

$env:PATH = "$Mingw\bin;$Kit\bin;$env:PATH"

Write-Host "==> [1/4] Compiling Java and generating JNI headers"
New-Item -ItemType Directory -Force -Path $OutDir, $LibDir, $GenDir | Out-Null
$javaFiles = Get-ChildItem (Join-Path $Root "java") -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
& "$JDK\bin\javac.exe" --release 17 -encoding UTF-8 -d $OutDir -h $GenDir $javaFiles
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Write-Host "==> [2/4] Compiling native bridge (jqt.dll)"
$gppArgs = @(
    "-std=c++17", "-O2", "-shared",
    "-o", (Join-Path $LibDir "jqt.dll"),
    "-I", (Join-Path $JDK "include"),
    "-I", (Join-Path $JDK "include\win32"),
    "-I", (Join-Path $Kit "include"),
    "-I", (Join-Path $Kit "include\QtWidgets"),
    "-I", (Join-Path $Kit "include\QtGui"),
    "-I", (Join-Path $Kit "include\QtCore"),
    "-I", (Join-Path $Kit "include\QtPrintSupport"),
    "-I", (Join-Path $Kit "include\QtSql"),
    "-I", (Join-Path $Kit "include\QtOpenGLWidgets"),
    "-I", (Join-Path $Kit "include\QtSerialPort"),
    "-I", (Join-Path $Kit "include\QtOpenGL"),
    "-I", (Join-Path $Kit "include\QtNetwork"),
    "-I", $NativeDir,
    (Join-Path $NativeDir "jqt_bridge.cpp"),
    "-L", (Join-Path $Kit "lib"),
    "-lQt6Widgets", "-lQt6Gui", "-lQt6Core", "-lQt6PrintSupport", "-lQt6Sql", "-lQt6OpenGLWidgets", "-lQt6OpenGL", "-lQt6SerialPort", "-lQt6Network", "-lole32", "-luuid", "-loleaut32",
    "-static-libgcc", "-static-libstdc++"
)
# QtSvg 特性探测(v1.9.1):装了 qtsvg 就编进 QSvgRenderer,没装则跳过(Java isAvailable()=false)
$svgLib = Join-Path $Kit "lib\libQt6Svg.a"
$svgDll = Join-Path $Kit "bin\Qt6Svg.dll"
$gppArgs += @("-DJQT_HAVE_NETWORK", "-DJQT_HAVE_SQL_MODELS")   # QtNetwork 属 qtbase,始终可用
# QtDBus 特性探测(v1.9.2 P1 收尾):qtbase 自带(Android 无)。Windows 上模块在但默认没有会话总线,
# 所以 isAvailable()=true 而 isSessionBusConnected()=false —— 冒烟里如实分开记录,不混为一谈。
if ((Test-Path (Join-Path $Kit "lib\libQt6DBus.a")) -or (Test-Path (Join-Path $Kit "bin\Qt6DBus.dll"))) {
    $gppArgs += @("-DJQT_HAVE_DBUS", "-I", (Join-Path $Kit "include\QtDBus"), "-lQt6DBus")
    Write-Host "==> QtDBus found - QDBusConnection enabled"
} else {
    Write-Host "==> QtDBus not found - DBus API will report unavailable"
}
# QtWebView 特性探测(v1.9.2 P2):官方不给 MinGW 版 QtWebEngine,但给 qtwebview ->
# 这就是"Java 里能显示网页"的现实路径(Windows 走系统 WebView2)。
if ((Test-Path (Join-Path $Kit "lib\libQt6WebView.a")) -or (Test-Path (Join-Path $Kit "bin\Qt6WebView.dll"))) {
    $gppArgs += @("-DJQT_HAVE_WEBVIEW", "-I", (Join-Path $Kit "include\QtWebView"), "-lQt6WebView")
    Write-Host "==> QtWebView found - QWebView enabled (Windows 用系统 WebView2)"
} else {
    Write-Host "==> QtWebView not found - QWebView will report unavailable"
}
# WebView2 自研宿主探测(v1.9.3,Windows):官方 qtwebview 包**不含 plugins/webview 后端插件**
# (MinGW 与 MSVC 两个包都实测过),实例化 QML 的 WebView 元素会让 Qt qFatal("No WebView plug-in found!")
# 直接 fail-fast(实测退出码 0xC0000409)。所以 Windows 上 QWebView 改走 JQt 自己写的 WebView2(COM)宿主
# (native/jqt_bridge.cpp 的 jqtwv2 命名空间),不再依赖 qtwebview 的后端插件。
#   - SDK 头 native/webview2/include/WebView2.h 来自 nuget.org 的 Microsoft.Web.WebView2 包;
#     **只 include 头,不链接任何 .lib** —— 该头里的 IID 全是 __declspec(selectany) 自带定义,
#     正好绕开 MSVC 专有的 WebView2LoaderStatic.lib(MinGW 的 ld 吃不下 MSVC 的 import lib)。
#   - WebView2Loader.dll 运行期用 LoadLibrary 动态加载,构建末段拷进 lib/。
# 没有这个头就退回"Windows 上不可用",不影响其它平台编译。
$wv2Header = Join-Path $NativeDir "webview2\include\WebView2.h"
$wv2Loader = Join-Path $NativeDir "webview2\x64\WebView2Loader.dll"
$HasWebView2 = ($env:OS -eq "Windows_NT") -and (Test-Path $wv2Header)
if ($HasWebView2) {
    $gppArgs += @("-DJQT_HAVE_WEBVIEW2", "-I", (Join-Path $NativeDir "webview2\include"))
    Write-Host "==> WebView2 SDK found - Windows native WebView2 host enabled"
} else {
    Write-Host "==> WebView2 SDK not found - QWebView will report unavailable on Windows"
    Write-Host "    (need native/webview2/include/WebView2.h; see native/webview2/README.md)"
}
# QtQuick3D 探测(v1.9.2 P2):Quick 3D 是 QML 模块,加载仍走 QQuickView -> 不链接 Qt6Quick3D,
# JQT_HAVE_QUICK3D 只是"构建期看见过它"的信息位(status() 会打印);能不能用是运行期结论。
if ((Test-Path (Join-Path $Kit "lib\libQt6Quick3D.a")) -or (Test-Path (Join-Path $Kit "bin\Qt6Quick3D.dll"))) {
    $gppArgs += @("-DJQT_HAVE_QUICK3D", "-I", (Join-Path $Kit "include\QtQuick3D"))
    Write-Host "==> QtQuick3D found - QQuick3D enabled (可用性由运行期探测)"
} else {
    Write-Host "==> QtQuick3D not found - QQuick3D will report unavailable"
}
if ((Test-Path (Join-Path $Kit "lib\libQt6Help.a")) -or (Test-Path (Join-Path $Kit "bin\Qt6Help.dll"))) {
    $gppArgs += @("-DJQT_HAVE_HELP", "-I", (Join-Path $Kit "include\QtHelp"), "-lQt6Help")
    Write-Host "==> QtHelp found - QHelpEngineCore enabled"
} else {
    Write-Host "==> QtHelp not found - QHelpEngineCore will report unavailable"
}
if ((Test-Path (Join-Path $Kit "lib\libQt6Positioning.a")) -or (Test-Path (Join-Path $Kit "bin\Qt6Positioning.dll"))) {
    $gppArgs += @("-DJQT_HAVE_POSITIONING", "-I", (Join-Path $Kit "include\QtPositioning"), "-lQt6Positioning")
    Write-Host "==> QtPositioning found - QGeoCoordinate enabled"
} else {
    Write-Host "==> QtPositioning not found - QGeoCoordinate will report unavailable"
}
if (((Test-Path (Join-Path $Kit "lib\libQt6Quick.a")) -or (Test-Path (Join-Path $Kit "bin\Qt6Quick.dll"))) -and (Test-Path (Join-Path $Kit "include\QtQmlIntegration\qqmlintegration.h"))) {
    $gppArgs += @("-DJQT_HAVE_QML", "-I", (Join-Path $Kit "include\QtQuick"),
                  "-I", (Join-Path $Kit "include\QtQml"), "-I", (Join-Path $Kit "include\QtQmlIntegration"), "-lQt6Quick", "-lQt6Qml")
    Write-Host "==> QtQuick found - QQuickView enabled"
} else {
    Write-Host "==> QtQuick not found - QQuickView will report unavailable"
}
if ((Test-Path (Join-Path $Kit "lib\libQt6Multimedia.a")) -or (Test-Path (Join-Path $Kit "bin\Qt6Multimedia.dll"))) {
    $gppArgs += @("-DJQT_HAVE_MULTIMEDIA", "-I", (Join-Path $Kit "include\QtMultimedia"), "-lQt6Multimedia")
    Write-Host "==> QtMultimedia found - QMediaPlayer enabled"
} else {
    Write-Host "==> QtMultimedia not found - QMediaPlayer will report unavailable"
}
if ((Test-Path (Join-Path $Kit "lib\libQt6Charts.a")) -or (Test-Path (Join-Path $Kit "bin\Qt6Charts.dll"))) {
    $gppArgs += @("-DJQT_HAVE_CHARTS", "-I", (Join-Path $Kit "include\QtCharts"), "-lQt6Charts")
    Write-Host "==> QtCharts found - QChart enabled"
} else {
    Write-Host "==> QtCharts not found - QChart will report unavailable"
}
if ((Test-Path (Join-Path $Kit "lib\libQt6WebSockets.a")) -or (Test-Path (Join-Path $Kit "bin\Qt6WebSockets.dll"))) {
    $gppArgs += @("-DJQT_HAVE_WEBSOCKETS", "-I", (Join-Path $Kit "include\QtWebSockets"), "-lQt6WebSockets")
    Write-Host "==> QtWebSockets found - QWebSocket enabled"
} else {
    Write-Host "==> QtWebSockets not found - QWebSocket will report unavailable"
}
if ((Test-Path $svgLib) -or (Test-Path $svgDll)) {
    $gppArgs += @("-DJQT_HAVE_SVG", "-I", (Join-Path $Kit "include\QtSvg"), "-lQt6Svg")
    Write-Host "==> QtSvg found - QSvgRenderer enabled"
} else {
    Write-Host "==> QtSvg not found - QSvgRenderer will report unavailable"
}
& "$Mingw\bin\g++.exe" @gppArgs
if ($LASTEXITCODE -ne 0) { throw "g++ failed" }

Write-Host "==> [3/4] Deploying Qt runtime into lib"
$deployArgs = @(
    "--no-translations", "--no-system-d3d-compiler", "--no-opengl-sw", "--compiler-runtime",
    (Join-Path $LibDir "jqt.dll")
)
& "$Kit\bin\windeployqt.exe" @deployArgs
if ($LASTEXITCODE -ne 0) {
    Write-Warning "windeployqt failed; copying Qt runtime manually"
    Copy-Item (Join-Path $Kit "bin\Qt6Core.dll"), (Join-Path $Kit "bin\Qt6Gui.dll"), (Join-Path $Kit "bin\Qt6Widgets.dll") $LibDir
    Copy-Item (Join-Path $Mingw "bin\libwinpthread-1.dll") $LibDir -ErrorAction SilentlyContinue
}


# 运行期要能 import QtQuick3D。两处都要做,而且顺序/位置都有讲究(本机 6.12.0 MinGW 实测):
#  · QML 模块:windeployqt 不认 QML 模块,而 Qt 6 的"可重定位前缀"是按 **Qt6Core.dll 所在目录**
#    推导的 —— 运行期 Qt6Core.dll 是从 lib 加载的,于是 QmlImportsPath 落在 lib/qml,
#    Qt 安装目录里的 qml/QtQuick3D 对用户完全不可见(实测报 module "QtQuick3D" is not installed)。
#    所以必须把整个模块拷进 lib/qml/QtQuick3D;仓库根 qml/ 也放一份供开发/测试(cwd)导入。
#  · C++ 依赖链:qquick3dplugin.dll -> Qt6Quick3D.dll -> Qt6Quick3DRuntimeRender.dll
#    -> **Qt6ShaderTools.dll**。Qt Shader Tools 是 addon(qtshadertools),官方 MinGW 在线安装器
#    默认不装 -> 插件加载失败,报 "Cannot load library ...qquick3dplugin.dll: 找不到指定的模块"
#    (本机实测就是这个原因,缺的不是 Quick3D 自己而是 ShaderTools)。
$q3dQmlSrc = Join-Path $Kit "qml\QtQuick3D"
if (Test-Path $q3dQmlSrc) {
    foreach ($dst in @((Join-Path $LibDir "qml\QtQuick3D"), (Join-Path $Root "qml\QtQuick3D"))) {
        New-Item -ItemType Directory -Force -Path $dst | Out-Null
        Copy-Item (Join-Path $q3dQmlSrc "*") $dst -Recurse -Force
    }
    Write-Host "==> 已部署 QtQuick3D QML 模块(lib/qml/QtQuick3D + qml/QtQuick3D)"
} else {
    Write-Host "==> 未找到 QtQuick3D QML 模块(该 Qt 未装 qtquick3d): $q3dQmlSrc"
}
# Quick3D 的 DLL 全家桶(含各 QML 子模块插件 QtQuick3D.Helpers/Particles3D... 依赖的
# Qt6Quick3DHelpers/Effects/Particles 等)+ ShaderTools。缺 ShaderTools 时整个 Quick3D 都不可用。
$q3dDlls = @(Get-ChildItem (Join-Path $Kit "bin\Qt6Quick3D*.dll") -ErrorAction SilentlyContinue)
# 子模块的间接依赖:QtQuick3D.SpatialAudio 的插件要 Qt6SpatialAudio + Qt6MultimediaQuick。
foreach ($extra in @("Qt6SpatialAudio.dll","Qt6MultimediaQuick.dll","Qt6ShaderTools.dll")) {
    $p = Join-Path $Kit "bin\$extra"
    if (Test-Path $p) { $q3dDlls += Get-Item $p }
}
foreach ($d in $q3dDlls) { Copy-Item $d.FullName $LibDir -Force }
Write-Host ("==> 已部署 Quick3D/ShaderTools 运行库 {0} 个: {1}" -f $q3dDlls.Count, (($q3dDlls | ForEach-Object { $_.Name }) -join ", "))
# 自检:核心依赖链必须完整,否则 QtQuick3D 插件加载必失败(本机修的就是这个坑)
$q3dChain = @("Qt6ShaderTools.dll","Qt6Quick3D.dll","Qt6Quick3DRuntimeRender.dll","Qt6Quick3DUtils.dll")
$q3dMissing = @($q3dChain | Where-Object { -not (Test-Path (Join-Path $LibDir $_)) })
if ($q3dMissing.Count -gt 0) {
    Write-Warning ("Quick3D 依赖链不完整,缺: " + ($q3dMissing -join ", ") +
                   " -> qquick3dplugin.dll 加载会失败(Quick3D 冒烟只能 SKIP)。" +
                   " ShaderTools 装法:官方在线安装器勾选 Qt Shader Tools,或取在线仓库 MinGW 包 " +
                   "qt.qt6.6120.addons.qtshadertools.win64_mingw 解到 " + $Kit)
} else {
    Write-Host "==> Quick3D 依赖链完整(qquick3dplugin -> Qt6Quick3D -> Qt6Quick3DRuntimeRender -> Qt6ShaderTools)"
}
if (-not (Test-Path (Join-Path $Kit "bin\Qt6QuickTimeline.dll"))) {
    Write-Host "==> 提示: 未装 qtquicktimeline(Qt6QuickTimeline.dll)-> QtQuick3D.AssetUtils 子模块不可用(核心 Quick3D 不受影响)"
}

Write-Host "==> [4/4] Deploying QPA platform plugin (qwindows)"
# QtWebView 的 QML 模块(qml/QtWebView)不在 Qt 资源里,必须跟着走 ——
# 否则 `import QtWebView` 报 "module is not installed"(实测踩过,而且 QML 不一定报错)。
# 拷到仓库根 qml/ 与 lib/qml/ 两处:前者是开发/测试时的导入路径,后者是打包后的布局。
$wvQmlSrc = Join-Path $Kit "qml\QtWebView"
if (Test-Path $wvQmlSrc) {
    foreach ($dst in @((Join-Path $Root "qml\QtWebView"), (Join-Path $LibDir "qml\QtWebView"))) {
        New-Item -ItemType Directory -Force -Path $dst | Out-Null
        Copy-Item (Join-Path $wvQmlSrc "*") $dst -Recurse -Force
    }
    Write-Host "==> 已部署 QtWebView QML 模块(qml/QtWebView + lib/qml/QtWebView)"
    # 插件 qtwebviewquickplugin.dll 依赖 Qt6WebViewQuick.dll,而 windeployqt 不会带它
    # (它只跟 Qt6WebView.dll)—— 少了它插件加载失败,报"找不到指定的模块"(实测踩过)。
    $wvQuick = Join-Path $Kit "bin\Qt6WebViewQuick.dll"
    if (Test-Path $wvQuick) {
        Copy-Item $wvQuick $LibDir -Force
        Copy-Item $wvQuick (Join-Path $Root "qml\QtWebView") -Force
        Write-Host "==> 已补 Qt6WebViewQuick.dll(lib/ 与 qml/QtWebView/)"
    }
} else {
    Write-Host "==> 未找到 QtWebView QML 模块(该 Qt 未装 qtwebview)"
}
# WebView2 自研宿主(v1.9.3):WebView2Loader.dll 必须能被运行期找到。
# native 侧用 LoadLibrary 动态加载它(优先 jqt.dll 同目录 -> 这里),所以 jqt.dll 对它没有硬链接依赖:
# 没装 WebView2 运行时的机器上 jqt.dll 照样加载,只是 QWebView.isBackendAvailable() 返回 false。
if ($HasWebView2 -and (Test-Path $wv2Loader)) {
    Copy-Item $wv2Loader $LibDir -Force
    Write-Host "==> 已部署 WebView2Loader.dll(x64,自研 WebView2 宿主用)"
} elseif ($HasWebView2) {
    Write-Warning "缺少 $wv2Loader -> Windows 上 QWebView 将报告不可用"
}
# 补 qoffscreen 平台插件:用户侧常以 QT_QPA_PLATFORM=offscreen 跑无头模式,
# 而 windeployqt 默认只带 qwindows(问题报告附录 B 实测)
$offscreenSrc = Join-Path $Kit "plugins\platforms\qoffscreen.dll"
if (Test-Path $offscreenSrc) {
    Copy-Item $offscreenSrc (Join-Path $LibDir "platforms\") -Force
    Write-Host "==> 已补 qoffscreen.dll"
} else {
    Write-Host "==> 未找到 qoffscreen.dll: $offscreenSrc"
}
$PlatformsDir = Join-Path $LibDir "plugins\platforms"
New-Item -ItemType Directory -Force -Path $PlatformsDir | Out-Null
Copy-Item (Join-Path $Kit "plugins\platforms\qwindows.dll") $PlatformsDir
# v0.7.2: 部署 SQL 驱动插件（SQLite 等）
$SqlDriversDir = Join-Path $LibDir "plugins\sqldrivers"
New-Item -ItemType Directory -Force -Path $SqlDriversDir | Out-Null
Get-ChildItem (Join-Path $Kit "plugins\sqldrivers") -Filter "qsqlite.dll" -ErrorAction SilentlyContinue | Copy-Item -Destination $SqlDriversDir
$qtConf = @"
[Paths]
Plugins = plugins
"@
Set-Content -Path (Join-Path $LibDir "qt.conf") -Value $qtConf -Encoding ascii

Write-Host "==> [5/5] Deploying license notices into lib (LGPL compliance)"
Copy-Item (Join-Path $Root "LGPL-3.0.txt"), (Join-Path $Root "THIRD-PARTY-NOTICES.md"), (Join-Path $Root "LICENSE.md"), (Join-Path $Root "LICENSE") $LibDir -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "Build OK"
Write-Host "  Java bytecode : $OutDir"
Write-Host "  Dynamic lib   : $(Join-Path $LibDir 'jqt.dll')"
Write-Host "  Run demo      : .\run.ps1"