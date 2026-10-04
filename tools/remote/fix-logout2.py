#!/usr/bin/env python3
"""真正修好:
  ① verify() 加吊销检查(上一版被自己的错误判断跳过,压根没改)
  ② do_POST 也支持退出(前端若用 POST,之前 404)
"""
import io
import re
import subprocess

GW = "/usr/local/bin/jqt-auth.py"
s = io.open(GW, encoding="utf-8").read()
done = []

# ---------------- ① verify():按行替换,不用脆弱的整块匹配 ----------------
lines = s.splitlines()
patched = False
for i, ln in enumerate(lines):
    if "int(exp) > time.time()" in ln and "is_revoked" not in ln:
        lines[i] = re.sub(r"int\(exp\) > time\.time\(\)\s*:",
                          "int(exp) > time.time() and not is_revoked(tok):", ln)
        patched = True
        break
if patched:
    s = "\n".join(lines) + "\n"
    done.append("verify() 吊销检查")
else:
    print("⚠️ verify 未找到可替换的行")

# ---------------- ② do_POST 支持退出(放在登录分支之前)----------------
if '"/api/logout"' not in s.split("def do_POST")[1][:1200]:
    anchor = '''    def do_POST(self):
        p = self.path.split("?")[0]
'''
    add = anchor + '''
        # 退出:POST/GET 都要能用(前端两个页面走的是 /api/logout)
        if p in ("/logout", "/api/logout"):
            tok = self._cookie()
            revoked = revoke(tok) if tok else False
            print(f"[jqt-auth] logout(POST) revoked={revoked} ip={self.client_address[0]}", flush=True)
            return self._json(200, {"ok": True, "revoked": revoked},
                              [("Set-Cookie", f"{COOKIE}=; Path=/; Max-Age=0")])
'''
    if anchor in s:
        s = s.replace(anchor, add, 1)
        done.append("do_POST 退出")
    else:
        print("⚠️ do_POST 锚点未匹配")
else:
    done.append("do_POST 退出已存在")

io.open(GW, "w", encoding="utf-8").write(s)
p = subprocess.run(["python3", "-m", "py_compile", GW], capture_output=True, text=True)
print("已更新:" + "、".join(done) + " | 语法:", "通过" if p.returncode == 0 else p.stderr[:300])
if p.returncode == 0:
    subprocess.run(["systemctl", "restart", "jqt-auth"], capture_output=True)
    import time
    time.sleep(1)
    print("jqt-auth:", subprocess.run(["systemctl", "is-active", "jqt-auth"],
                                      capture_output=True, text=True).stdout.strip())
print(subprocess.run(["sed", "-n", "91,102p", GW], capture_output=True, text=True).stdout)
