#!/usr/bin/env python3
"""不依赖 WebView2 那路的收口:
  1) 新增 .gitattributes —— 防 CRLF 复发(本机 core.autocrlf=true 会把 *.sh 变 CRLF,
     而 bash 见到 `do\\r` 直接语法错;CI 检出 LF blob 所以一直没暴露)
  2) 改正文档里"这两处 SKIP 未修复"的旧表述 —— 实际已修,并写清真正的根因
  3) .gitignore 隔离并行任务的临时证据文件(.q3d_*),避免误提交
"""
import io
import os

R = r"D:\SilentStudio\JQt - Dev"

# ---------- 1) .gitattributes ----------
ga = os.path.join(R, ".gitattributes")
if not os.path.exists(ga):
    io.open(ga, "w", encoding="utf-8", newline="\n").write(
        "# 行尾约定(防 CRLF 复发)\n"
        "# 背景:本机 core.autocrlf=true,git 触碰工作区时会把 *.sh 变成 CRLF,\n"
        "# 而 bash 见到 `do\\r` 会直接语法错(本地 bash -n 就这么挂过一次;CI 检出 LF blob 所以一直没事)。\n"
        "* text=auto\n"
        "*.sh   text eol=lf\n"
        "*.bash text eol=lf\n"
        "*.yml  text eol=lf\n"
        "*.yaml text eol=lf\n"
        "*.java text eol=lf\n"
        "*.py   text eol=lf\n"
        "*.md   text eol=lf\n"
        "*.png  binary\n"
        "*.dll  binary\n"
        "*.7z   binary\n")
    print("  已新增 .gitattributes")

# ---------- 2) 文档旧表述改正 ----------
doc = os.path.join(R, "docs", "compare-qtjambi.md")
s = io.open(doc, encoding="utf-8").read()
old_marker = "**2026-10-15 更新**:尝试在构建脚本里补 `-DJQT_HAVE_SVG` 与部署 `plugins/sqldrivers` 来消除这两处 SKIP"
if old_marker in s:
    start = s.find(old_marker)
    end = s.find("**因此**:`QSvg` 与 `SQL 模型`", start)
    if end == -1:
        end = s.find("\n\n", start)
    new = ("**2026-10-15 已修复**(根因与最初的判断不同,记下来免得再误判):\n"
           "· **QSvg**:不是少了 `-DJQT_HAVE_SVG`,而是 **`qtsvg` 这个独立 7z 从来没装进 CI 的 Qt 树**"
           "(CI 只解了 qtbase;Linux 与 macOS 6.12.0 两条线都缺)。现已在两处安装步骤里补装 qtsvg 并加硬断言"
           "(缺 `libQt6Svg.so` / 缺 `include/QtSvg/QSvgRenderer` 直接红)。\n"
           "· **SQLite**:插件**一直在包里**,是桥的兜底按 Windows 命名 `sqldrivers/qsqlite.<ext>` 去找,"
           "而 Linux/macOS 上文件叫 **`libqsqlite.so` / `libqsqlite.dylib`** → 永远找不到"
           "(日志原话 `driver SQLITE unavailable (plugin: )`,plugin 后面是空的)。"
           "现已按兜底期望的名字部署到 `<部署目录>/sqldrivers/`。\n"
           "· 验证方式:用 CI 同款归档在 WSL 里**先复现失败、再证明修复**(建表/插入/查询真跑),"
           "并对断言脚本做了 14 个场景(含下载 404、归档损坏、0 字节符号链接占位、缺头文件)。\n"
           "· 顺带纠正:`SmokeSqlDb` 依赖同一驱动,已从 Linux/macOS 清单移出(只保留 Windows),"
           "原因与实测数字见下。\n\n")
    s = s[:start] + new + s[end:]
    io.open(doc, "w", encoding="utf-8", newline="\n").write(s)
    print("  文档旧表述已改正")
else:
    print("  文档旧表述未找到(可能已改)")

# ---------- 3) .gitignore 隔离临时证据 ----------
gi = os.path.join(R, ".gitignore")
s = io.open(gi, encoding="utf-8").read() if os.path.exists(gi) else ""
add = []
for pat in (".q3d_*", "*.log", "*.png"):
    if pat not in s:
        add.append(pat)
if add:
    if not s.endswith("\n"):
        s += "\n"
    s += "\n# 并行任务的临时证据文件(CI 校验脚本/日志/抓图)\n" + "\n".join(add) + "\n"
    io.open(gi, "w", encoding="utf-8", newline="\n").write(s)
    print(f"  .gitignore 追加: {', '.join(add)}")
else:
    print("  .gitignore 无需改")
