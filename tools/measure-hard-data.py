#!/usr/bin/env python3
"""实测 1.1 节需要的所有硬数字(一次算清,避免文档自相矛盾)"""
import io
import os
import re
import subprocess

ROOT = r"D:\SilentStudio\JQt - Dev"
os.chdir(ROOT)
D = "java/org/jqt"

files = sorted(f for f in os.listdir(D) if f.endswith(".java"))
api = [f for f in files if not f.startswith("Smoke")]
smokes = [f for f in files if f.startswith("Smoke")]


def lines(p):
    return len(io.open(p, encoding="utf-8", errors="replace").read().splitlines())


java_lines = sum(lines(os.path.join(D, f)) for f in files)
api_lines = sum(lines(os.path.join(D, f)) for f in api)
smoke_lines = sum(lines(os.path.join(D, f)) for f in smokes)
bridge = "native/jqt_bridge.cpp"
bsrc = io.open(bridge, encoding="utf-8", errors="replace").read()
jni = len(re.findall(r"^JNIEXPORT .*?JNICALL (Java_org_jqt_\w+)", bsrc, re.M))
native_decl = 0
public_decl = 0
connect_entries = 0
for f in files:
    body = io.open(os.path.join(D, f), encoding="utf-8", errors="replace").read()
    native_decl += len(re.findall(r"\bnative\b\s+[\w<>\[\], .]+\s+\w+\s*\(", body))
    public_decl += len(re.findall(r"^\s+public\s", body, re.M))
    connect_entries += len(re.findall(r"nativeConnect\w+", body))
macros = sorted(set(re.findall(r"#if(?:def| defined\()\s*\(?(JQT_HAVE_\w+)", bsrc)))

print(f"API 类            : {len(api)}")
print(f"冒烟类            : {len(smokes)}")
print(f"Java 源文件合计    : {len(files)}")
print(f"Java 行数          : {java_lines}  (API {api_lines} + 冒烟 {smoke_lines})")
print(f"JNI 桥行数         : {lines(bridge)}")
print(f"JNI 导出函数       : {jni}")
print(f"Java native 声明   : {native_decl}")
print(f"公开成员声明        : {public_decl}")
print(f"nativeConnect* 入口 : {connect_entries}")
print(f"特性宏             : {len(macros)} -> {', '.join(m.replace('JQT_HAVE_','') for m in macros)}")
jars = []
for d in ("dist", "build"):
    if os.path.isdir(d):
        for dp, _, fs in os.walk(d):
            for f in fs:
                if f.endswith(".jar"):
                    jars.append(os.path.join(dp, f))
print("现有 jar          :", jars[:3] if jars else "(无)")
