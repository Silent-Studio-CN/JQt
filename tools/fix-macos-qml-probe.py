#!/usr/bin/env python3
"""修 build-macos.sh 的 QtQuick 探测假阴性(Mac 真机实测发现):
   Qt 6.11.2 把 QtQmlIntegration 装成**普通 include 目录**($QT/include/QtQmlIntegration),
   没有 QtQmlIntegration.framework;而脚本只探测 framework 形态
   -> QML_FLAGS 为空 -> QQuickView 报 unavailable -> SmokeQml 以"模块没装"名义 SKIP(其实装得好好的)。
   修法:两种形态都认(framework 或 include 目录)。
"""
import io
import re

P = r"D:\SilentStudio\JQt - Dev\build-macos.sh"
s = io.open(P, encoding="utf-8").read()
before = s

# 找到 QML 探测块(它同时要求 QtQmlIntegration.framework)
m = re.search(r"if \[ -d \"\$QTLIB/QtQuick\.framework\" \][^\n]*\n(?:.*\n)*?fi\n", s)
if m:
    old = m.group(0)
    new = '''# QtQuick 探测:两种模块布局都要认 —— framework 形态(Qt 安装器 / 官方 dmg)
# 与 include 目录形态(Qt 6.11.2 的 macos 包把 QtQmlIntegration 装成 $QT/include/QtQmlIntegration,
# 没有 .framework)。只认 framework 会造成**假阴性**:QML_FLAGS 为空 -> QQuickView 报 unavailable
# -> SmokeQml 以"模块没装"的名义 SKIP,其实 qtdeclarative 装得好好的(真机实测踩过)。
if [ -d "$QTLIB/QtQuick.framework" ] && \\
   { [ -d "$QTLIB/QtQmlIntegration.framework" ] || [ -d "$QTINC/QtQmlIntegration" ]; }; then
  QML_FLAGS="-DJQT_HAVE_QML -I$QTLIB/QtQuick.framework/Headers -I$QTLIB/QtQml.framework/Headers -framework QtQuick -framework QtQml"
  if [ -d "$QTLIB/QtQmlIntegration.framework" ]; then
    QML_FLAGS="$QML_FLAGS -I$QTLIB/QtQmlIntegration.framework/Headers -framework QtQmlIntegration"
  elif [ -d "$QTINC/QtQmlIntegration" ]; then
    QML_FLAGS="$QML_FLAGS -I$QTINC/QtQmlIntegration"
    echo "==> 注意:QtQmlIntegration 是 include 目录形态(非 framework)—— 已按 include 处理"
  fi
  echo "==> QtQuick found - QQuickView enabled"
else
  QML_FLAGS=""
  echo "==> QtQuick not found(需要 QtQuick.framework 且 QtQmlIntegration(framework 或 include 目录))- QQuickView will report unavailable"
fi
'''
    s = s.replace(old, new, 1)
    print("  build-macos.sh: QtQuick 探测已改为兼容两种模块布局")
else:
    print("  ⚠️ 未匹配到 QtQuick 探测块,请人工检查")

io.open(P, "w", encoding="utf-8", newline="\n").write(s)
print("  改动:", "有" if s != before else "无")
print("  CR 数(应为 0):", io.open(P, encoding="utf-8").read().count("\r"))
