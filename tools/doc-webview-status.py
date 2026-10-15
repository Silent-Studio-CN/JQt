#!/usr/bin/env python3
"""文档:WebView 行改为如实状态 + 2.1 节补 WebView 的平台限制。"""
import io

P = r"D:\SilentStudio\JQt - Dev\docs\compare-qtjambi.md"
s = io.open(P, encoding="utf-8").read()
changed = []

# 能力矩阵:WebEngine/WebView 行
old = "| **WebEngine / WebView** | ❌ | |"
new = ("| **WebEngine / WebView** | 🟡 **部分(不计入命中)** | "
       "`QWebView`(QtWebView 封装:load/loadHtml/url/title/isReady/grabToPng)已实现并有特性探测;"
       "**但官方 6.12 的 qtwebview 包在 Windows 上不含 `plugins/webview/` 后端插件**"
       "(MinGW 与 MSVC 包都实测过,只有 Qt6WebView/Qt6WebViewQuick + QML 模块),"
       "此时 Qt 会 `qFatal(\"No WebView plug-in found!\")` 直接中止进程 —— "
       "故 `QWebView.isBackendAvailable()` 为 false 时本类**拒绝构造**,冒烟如实 SKIP。"
       "macOS 的 WKWebView 后端内建(待真机确认);Linux 侧 QtWebView 依赖 QtWebEngine。"
       "**要 Windows 可用只能自研 WebView2(COM)宿主** —— 已记录为后续项 |")
if old in s:
    s = s.replace(old, new, 1)
    changed.append("WebView 行 → 🟡(如实)")

# 2.1 节补一行
if "Windows 官方包缺 WebView 后端" not in s:
    anchor = "**因此**:`QSvg` 与 `SQL 模型` 目前**只有 Windows 线真跑过**;"
    add = ("| Windows / macOS / Linux | `SmokeWebView` | 官方 qtwebview 包不含 `plugins/webview/` 后端插件;"
           "无后端时 Qt 会中止进程,故冒烟在 `isBackendAvailable()`(Windows 官方包缺 WebView 后端)为 false 时明确 SKIP |\n\n" + anchor)
    s = s.replace(anchor, add, 1)
    changed.append("2.1 节补 WebView 行")

io.open(P, "w", encoding="utf-8", newline="\n").write(s)
print("已改:" + "、".join(changed) if changed else "无需改动")
