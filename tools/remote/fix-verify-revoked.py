#!/usr/bin/env python3
"""精确修 verify():加吊销检查(上一版锚点没匹配上)"""
import io
import subprocess

GW = "/usr/local/bin/jqt-auth.py"
s = io.open(GW, encoding="utf-8").read()

OLD = """        if hmac.compare_digest(hmac.new(secret(), user + b"|" + exp, hashlib.sha256).digest(), mac) \\
           and int(exp) > time.time():
            return user.decode()"""
NEW = """        if hmac.compare_digest(hmac.new(secret(), user + b"|" + exp, hashlib.sha256).digest(), mac) \\
           and int(exp) > time.time() and not is_revoked(tok):
            return user.decode()"""

if "is_revoked(tok)" in s:
    print("verify 已含吊销检查,无需改动")
elif OLD in s:
    io.open(GW, "w", encoding="utf-8").write(s.replace(OLD, NEW, 1))
    print("✅ verify() 已加吊销检查")
else:
    # 兜底:按行定位,替换 "and int(exp) > time.time():" 那一行
    lines = s.splitlines()
    hit = 0
    for i, ln in enumerate(lines):
        if "int(exp) > time.time()" in ln and "is_revoked" not in ln:
            lines[i] = ln.rstrip().rstrip(":") + " and not is_revoked(tok):"
            hit += 1
            break
    if hit:
        io.open(GW, "w", encoding="utf-8").write("\n".join(lines) + "\n")
        print("✅ 兜底替换完成")
    else:
        print("⚠️ 仍未匹配,请人工检查")

p = subprocess.run(["python3", "-m", "py_compile", GW], capture_output=True, text=True)
print("语法:", "通过" if p.returncode == 0 else p.stderr[:300])
if p.returncode == 0:
    subprocess.run(["systemctl", "restart", "jqt-auth"], capture_output=True)
    import time
    time.sleep(1)
    print("jqt-auth:", subprocess.run(["systemctl", "is-active", "jqt-auth"],
                                      capture_output=True, text=True).stdout.strip())
# 确认 verify 现状
out = subprocess.run(["grep", "-n", "-A 10", "^def verify", GW], capture_output=True, text=True).stdout
print(out)
