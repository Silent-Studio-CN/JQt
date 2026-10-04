#!/usr/bin/env python3
"""按行替换 shell 字段(锚点缩进很容易错,改用行匹配)"""
import io
import re
import subprocess

GW = "/usr/local/bin/jqt-auth.py"
lines = io.open(GW, encoding="utf-8").read().splitlines()
hit = -1
for i, ln in enumerate(lines):
    if '"shell": (' in ln:
        hit = i
        break
if hit == -1:
    print("⚠️ 未找到 shell 字段")
else:
    indent = len(lines[hit]) - len(lines[hit].lstrip())
    pad = " " * indent
    new_block = [
        f'{pad}"shell": ("/bin/bash" if user != "silent" else',
        f'{pad}          (sh(["getent", "passwd", user]).split(":")[6]',
        f'{pad}           if len(sh(["getent", "passwd", user]).split(":")) > 6 else "")),',
    ]
    # 吃掉原来的续行(到出现 "else \"\"))," 结束)
    end = hit
    while end + 1 < len(lines) and not lines[end].rstrip().endswith('else ""),'):
        end += 1
    print(f"替换 {hit+1}..{end+1} 行")
    lines[hit:end + 1] = new_block
    io.open(GW, "w", encoding="utf-8", newline="\n").write("\n".join(lines) + "\n")
    p = subprocess.run(["python3", "-m", "py_compile", GW], capture_output=True, text=True)
    print("语法:", "通过" if p.returncode == 0 else p.stderr[:300])
    if p.returncode == 0:
        subprocess.run(["systemctl", "restart", "jqt-auth"], capture_output=True)
print(subprocess.run(["sed", "-n", "/def account_info/,/password_status/p", GW],
                     capture_output=True, text=True).stdout)
