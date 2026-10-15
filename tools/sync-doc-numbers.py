#!/usr/bin/env python3
"""把文档里的数字按代码实测**自动改写**(配合 check-doc-numbers.py 使用)。

工作流:改了代码 -> python tools/sync-doc-numbers.py -> 提交。
这样"文档数字必须来自实测"就不靠人记,也不会因为忘改而让 CI 红。
"""
import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
os.chdir(ROOT)
DOC = "docs/compare-qtjambi.md"
D = "java/org/jqt"

files = sorted(f for f in os.listdir(D) if f.endswith(".java"))
api = [f for f in files if not f.startswith("Smoke")]
smokes = [f for f in files if f.startswith("Smoke")]
lines_of = lambda p: len(io.open(p, encoding="utf-8", errors="replace").read().splitlines())
java_lines = sum(lines_of(os.path.join(D, f)) for f in files)
api_lines = sum(lines_of(os.path.join(D, f)) for f in api)
smoke_lines = sum(lines_of(os.path.join(D, f)) for f in smokes)
bp = "native/jqt_bridge.cpp"
bridge = io.open(bp, encoding="utf-8", errors="replace").read()
bridge_lines = lines_of(bp)
jni = len(re.findall(r"^JNIEXPORT .*?JNICALL (Java_org_jqt_\w+)", bridge, re.M))
native_decl = sum(len(re.findall(r"\bnative\b\s+[\w<>\[\], .]+\s+\w+\s*\(", io.open(os.path.join(D, f), encoding="utf-8", errors="replace").read())) for f in files)
public_decl = sum(len(re.findall(r"^\s+public\s", io.open(os.path.join(D, f), encoding="utf-8", errors="replace").read(), re.M)) for f in files)

s = io.open(DOC, encoding="utf-8").read()
before = s
subs = [
    (r"\| API 类 \| \*\*\d+\*\*", f"| API 类 | **{len(api)}**"),
    (r"\| 冒烟类 \| \*\*\d+\*\* 个文件；CI 引用 \*\*\d+\*\* 个", None),   # 见下方单独处理
    (r"\| Java 源文件合计 \| \d+", f"| Java 源文件合计 | {len(files)}"),
    (r"\| Java 行数 \| \*\*[\d,]+\*\*（API [\d,]+ \+ 冒烟 [\d,]+）",
     f"| Java 行数 | **{java_lines:,}**（API {api_lines:,} + 冒烟 {smoke_lines:,}）"),
    (r"\| JNI 桥行数 \| \*\*[\d,]+\*\*", f"| JNI 桥行数 | **{bridge_lines:,}**"),
    (r"\| JNI 导出函数 \| \*\*[\d,]+\*\*", f"| JNI 导出函数 | **{jni:,}**"),
    (r"\| native 方法声明 \| [\d,]+", f"| native 方法声明 | {native_decl:,}"),
    (r"\| 公开成员声明 \| [\d,]+", f"| 公开成员声明 | {public_decl:,}"),
    (r"\(Get-ChildItem java\\org\\jqt -Filter \*\.java\)\.Count[^\n]*",
     f"(Get-ChildItem java\\org\\jqt -Filter *.java).Count            # {len(files)} 个文件（{len(api)} API + {len(smokes)} 冒烟）"),
    (r"\(Get-Content java\\org\\jqt\\\*\.java \| Measure-Object -Line\)[^\n]*",
     f"(Get-Content java\\org\\jqt\\*.java | Measure-Object -Line)    # {java_lines:,} 行（API {api_lines:,} + 冒烟 {smoke_lines:,}）"),
    (r"\(Get-Content native\\jqt_bridge\.cpp\)\.Count[^\n]*",
     f"(Get-Content native\\jqt_bridge.cpp).Count                   # {bridge_lines:,} 行手写 JNI 桥"),
    (r"Select-String native\\jqt_bridge\.cpp -Pattern 'JNIEXPORT'[^\n]*",
     f"Select-String native\\jqt_bridge.cpp -Pattern 'JNIEXPORT'    # {jni:,} 个 JNI 导出函数"),
    (r"Select-String java\\org\\jqt\\\*\.java -Pattern '\^\\s\+public '[^\n]*",
     f"Select-String java\\org\\jqt\\*.java -Pattern '^\\s+public '    # {public_decl:,} 处公开成员声明"),
    (r"Select-String java\\org\\jqt\\\*\.java -Pattern '\\bnative\\b'[^\n]*",
     f"Select-String java\\org\\jqt\\*.java -Pattern '\\bnative\\b'     # {native_decl:,} 个 native 方法声明"),
]
for pat, rep in subs:
    if rep is None:
        continue
    # 用 lambda:替换串里有 \org\jqt、\s 这类反斜杠,直接当 repl 会被 re 当转义解析(bad escape)
    s = re.sub(pat, lambda m, r=rep: r, s, count=1)

# 冒烟类那行:保留"CI 引用 N 个"的 N(由 CI 文件实测)
ci = io.open(".github/workflows/ci.yml", encoding="utf-8").read()
ci_smokes = sorted(set(re.findall(r"org\.jqt\.(Smoke\w+)", ci)))
s = re.sub(r"\| 冒烟类 \| \*\*\d+\*\* 个文件；CI 引用 \*\*\d+\*\* 个",
           f"| 冒烟类 | **{len(smokes)}** 个文件；CI 引用 **{len(ci_smokes)}** 个", s, count=1)

io.open(DOC, "w", encoding="utf-8", newline="\n").write(s)
print("同步完成:" if s != before else "无需改动")
print(f"  API {len(api)} · 冒烟 {len(smokes)}(CI 引用 {len(ci_smokes)}) · 源文件 {len(files)}")
print(f"  Java {java_lines:,} 行(API {api_lines:,} + 冒烟 {smoke_lines:,}) · 桥 {bridge_lines:,} · JNI {jni:,}")
print(f"  native 声明 {native_decl:,} · 公开成员 {public_decl:,}")
