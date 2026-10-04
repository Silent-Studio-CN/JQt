#!/usr/bin/env python3
"""修退出登录:
  ① 后端补 /api/logout 别名(前端调的就是它 —— 之前只实现 /logout,所以点了没用)
  ② 退出做成**真失效**:把该 token 的指纹写进吊销表,verify() 时拒绝
     (否则只是删了 Cookie,token 被复制走仍然能用)
  ③ nginx 补 location = /logout 与 /login(合法路径也能走通)
"""
import io
import os
import subprocess

GW = "/usr/local/bin/jqt-auth.py"
NGINX = "/etc/nginx/sites-available/jqt"
s = io.open(GW, encoding="utf-8").read()
done = []

# ---------------- ① 吊销表基础设施 ----------------
if "REVOKED_FILE" not in s:
    anchor = 'SECRET_FILE = "/etc/jqt-auth.secret"'
    add = anchor + '''
REVOKED_FILE = "/etc/jqt-revoked"        # 已吊销的会话指纹(每行:<sha256> <过期时间>)


def _fp(tok):
    import hashlib as _h
    return _h.sha256(tok.encode()).hexdigest()


def revoke(tok):
    """把 token 指纹写入吊销表(带过期时间,便于自动清理)"""
    try:
        user, exp, _ = base64.urlsafe_b64decode(tok.encode()).rsplit(b"|", 2)
        with open(REVOKED_FILE, "a", encoding="utf-8") as f:
            f.write(f"{_fp(tok)} {int(exp)}\\n")
        os.chmod(REVOKED_FILE, 0o600)
        return True
    except Exception:
        return False


def is_revoked(tok):
    """检查并顺带清理过期条目"""
    if not os.path.exists(REVOKED_FILE):
        return False
    now = int(time.time())
    alive, hit = [], False
    try:
        with open(REVOKED_FILE, encoding="utf-8") as f:
            for line in f:
                parts = line.split()
                if len(parts) != 2 or not parts[1].isdigit():
                    continue
                if int(parts[1]) <= now:
                    continue
                alive.append(f"{parts[0]} {parts[1]}")
                if parts[0] == _fp(tok):
                    hit = True
        with open(REVOKED_FILE, "w", encoding="utf-8") as f:
            f.write("\\n".join(alive) + ("\\n" if alive else ""))
    except Exception:
        return False
    return hit
'''
    s = s.replace(anchor, add, 1)
    done.append("吊销表")

# ---------------- ② verify() 里加吊销检查 ----------------
old_v = '''          if hmac.compare_digest(hmac.new(secret(), user + b"|" + exp, hashlib.sha256).digest(), mac) \\
             and int(exp) > time.time():
              return user.decode()'''
new_v = '''          if hmac.compare_digest(hmac.new(secret(), user + b"|" + exp, hashlib.sha256).digest(), mac) \\
             and int(exp) > time.time() and not is_revoked(tok):
              return user.decode()'''
if old_v in s:
    s = s.replace(old_v, new_v, 1)
    done.append("verify 检查吊销")
else:
    print("⚠️ verify 锚点未匹配")

# ---------------- ③ 退出分支:/api/logout 别名 + 真吊销 ----------------
old_l = '''        if p.startswith("/logout"):
            return self._json(200, {"ok": True}, [("Set-Cookie", f"{COOKIE}=; Path=/; Max-Age=0")])'''
new_l = '''        if p.startswith("/logout") or p.startswith("/api/logout"):
            # 前端两个页面调的都是 /api/logout;同时兼容 /logout
            tok = self._cookie()
            revoked = revoke(tok) if tok else False
            print(f"[jqt-auth] logout user={user} revoked={revoked} ip={self.client_address[0]}",
                  flush=True)
            return self._json(200, {"ok": True, "revoked": revoked},
                              [("Set-Cookie", f"{COOKIE}=; Path=/; Max-Age=0")])'''
if old_l in s:
    s = s.replace(old_l, new_l, 1)
    done.append("/api/logout 别名 + 吊销")
else:
    print("⚠️ logout 锚点未匹配")

# ---------------- ④ 取 Cookie 的小工具 ----------------
if "def _cookie" not in s:
    anchor2 = "    def _xff(self):"
    add2 = '''    def _cookie(self):
        raw = self.headers.get("Cookie", "")
        for part in raw.split(";"):
            k, _, v = part.strip().partition("=")
            if k == COOKIE:
                return v.strip()
        return ""

''' + anchor2
    s = s.replace(anchor2, add2, 1)
    done.append("_cookie()")
else:
    done.append("_cookie() 已存在")

io.open(GW, "w", encoding="utf-8").write(s)
subprocess.run(["chmod", "755", GW])
p = subprocess.run(["python3", "-m", "py_compile", GW], capture_output=True, text=True)
print("后端已更新:" + "、".join(done) + ";语法:", "通过" if p.returncode == 0 else p.stderr[:300])

# ---------------- ⑤ nginx:/logout 与 /login 走网关 ----------------
n = io.open(NGINX, encoding="utf-8").read()
if "location = /logout" not in n:
    anchor3 = "    location = /api/login {"
    add3 = '''    # 退出与登录别名:网关实现了 /logout 与 /login(前端也用 /api/logout)
    location = /logout {
        proxy_pass http://127.0.0.1:9000;
    }
    location = /login {
        proxy_pass http://127.0.0.1:9000;
    }
    location = /api/login {'''
    n = n.replace(anchor3, add3, 1)
    io.open(NGINX, "w", encoding="utf-8").write(n)
    print("⑤ nginx 已加 /logout 与 /login")
else:
    print("⑤ nginx 已存在")

pt = subprocess.run(["nginx", "-t"], capture_output=True, text=True)
print("   nginx -t:", (pt.stderr or "ok").strip().splitlines()[-1])
if pt.returncode == 0:
    subprocess.run(["systemctl", "reload", "nginx"], capture_output=True)
subprocess.run(["systemctl", "restart", "jqt-auth"], capture_output=True)
time.sleep(1) if False else None
print("   jqt-auth:", subprocess.run(["systemctl", "is-active", "jqt-auth"],
                                    capture_output=True, text=True).stdout.strip())
