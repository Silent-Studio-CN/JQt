#!/usr/bin/env python3
"""补 GET 侧的管理员守卫(上一版只加了 POST -> /api/status 与 /api/account/audit 对 jhy 敞开,
而 /api/status 会返回真实主机名/WSL 内核/真实核数与内存,直接破坏沙箱前提)。
顺带把非管理员的 shell 字段做个掩饰。
"""
import io
import subprocess

GW = "/usr/local/bin/jqt-auth.py"
s = io.open(GW, encoding="utf-8").read()
done = []

# ---------------- ① 统一的管理员判定 ----------------
if "def admin_only(" not in s:
    anchor = "def entries_for(user):"
    add = '''def admin_only(path):
    """管理类接口:仅 silent 可用(jhy 等普通用户 403)"""
    return (path.startswith("/api/status") or path.startswith("/api/account/audit")
            or path in ("/api/account/brand", "/api/account/service", "/api/account/logout-all"))


''' + anchor
    s = s.replace(anchor, add, 1)
    done.append("admin_only()")

# ---------------- ② do_GET 里加守卫 ----------------
old_get = '''        user = self._user()
        if p.startswith("/api/status"):
            if not user: return self._json(401, {"ok": False, "msg": "未登录"})
            d = status(); d["user"] = user
            return self._json(200, d)'''
new_get = '''        user = self._user()
        if admin_only(p) and user != "silent":
            if not user:
                return self._json(401, {"ok": False, "msg": "未登录"})
            print(f"[jqt-auth] DENY(GET) {p} user={user}", flush=True)
            return self._json(403, {"ok": False, "msg": "该操作仅管理员可用"})
        if p.startswith("/api/status"):
            if not user: return self._json(401, {"ok": False, "msg": "未登录"})
            d = status(); d["user"] = user
            return self._json(200, d)'''
if old_get in s:
    s = s.replace(old_get, new_get, 1)
    done.append("do_GET 守卫(status)")
else:
    print("⚠️ do_GET status 锚点未匹配")

# audit 分支同样受 admin_only 保护(上面已统一拦,这里只是确认存在)
if 'if p.startswith("/api/account/audit"):' in s:
    done.append("audit 已被 admin_only 覆盖")

# ---------------- ③ POST 侧改用同一个判定 ----------------
old_post = '''        if user != "silent" and (p.startswith("/api/status") or p.startswith("/api/account/audit")
                                 or p == "/api/account/brand" or p == "/api/account/service"
                                 or p == "/api/account/logout-all"):'''
new_post = '''        if user != "silent" and admin_only(p):'''
if old_post in s:
    s = s.replace(old_post, new_post, 1)
    done.append("POST 侧复用 admin_only")

# ---------------- ④ 非管理员的 shell 字段做掩饰 ----------------
old_acc = '''          "shell": (sh(["getent", "passwd", user]).split(":")[6]
                    if len(sh(["getent", "passwd", user]).split(":")) > 6 else ""),'''
new_acc = '''          # 非管理员:不暴露"自定义登录壳"这种沙箱线索
          "shell": ("/bin/bash" if user != "silent" else
                    (sh(["getent", "passwd", user]).split(":")[6]
                     if len(sh(["getent", "passwd", user]).split(":")) > 6 else "")),'''
if old_acc in s:
    s = s.replace(old_acc, new_acc, 1)
    done.append("shell 掩饰")

io.open(GW, "w", encoding="utf-8").write(s)
c = subprocess.run(["python3", "-m", "py_compile", GW], capture_output=True, text=True)
print("已更新:" + "、".join(done) + " | 语法:", "通过" if c.returncode == 0 else c.stderr[:300])
if c.returncode == 0:
    subprocess.run(["systemctl", "restart", "jqt-auth"], capture_output=True)
