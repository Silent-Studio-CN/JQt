#!/usr/bin/env python3
"""让 jhy 能登录网页控制台 —— 但给她**独立的沙箱终端**,而不是 silent 的终端。

设计:
  · 新 ttyd 实例(jqt-term-jhy.service,端口 7684,挂载点 /terminal)以 **jhy 身份**运行,
    并且直接执行 /usr/local/bin/jhy-shell -> 沙箱、伪装、SilentSafe、封禁全部生效
  · 网关 ALLOWED 改为「用户 -> 允许的入口元组」:
        silent -> ("/console/", "/silent/")
        jhy    -> ("/terminal/",)
    顺带去掉了上一轮那个"所有人都能进 /silent/"的口子
  · 权限收敛:非 silent 用户访问管理类接口(status/audit/brand/service/logout-all)一律 403
  · 新增 /api/entry:登录用户访问别人的入口时自动跳回自己的入口(避免看到登录页死循环)
"""
import io
import os
import subprocess

GW = "/usr/local/bin/jqt-auth.py"
NGINX = "/etc/nginx/sites-available/jqt"

# ---------------------------------------------------------------- ① ttyd 实例
io.open("/etc/systemd/system/jqt-term-jhy.service", "w", encoding="utf-8", newline="\n").write(
    """[Unit]
Description=SilentRemoveKit sandbox console for jhy (/terminal)
After=network.target

[Service]
User=jhy
Group=jhy
WorkingDirectory=/home/jhy
Environment=TERM=xterm-256color
# 直接执行沙箱壳:伪装配置 / SilentSafe 拦截 / 封禁 都在这一层生效
ExecStart=/usr/bin/ttyd -i 127.0.0.1 -p 7684 -b /terminal -W /usr/local/bin/jhy-shell
Restart=always
RestartSec=2

[Install]
WantedBy=multi-user.target
""")
subprocess.run(["systemctl", "daemon-reload"], capture_output=True)
p = subprocess.run(["systemctl", "enable", "--now", "jqt-term-jhy.service"],
                   capture_output=True, text=True)
print("① jqt-term-jhy:", "已启动" if p.returncode == 0 else p.stderr[:200])
print("   ttyd 端口:", subprocess.run(["ss", "-tlnp"], capture_output=True, text=True).stdout.count("7684"))

# ---------------------------------------------------------------- ② 网关
s = io.open(GW, encoding="utf-8").read()
done = []

old_allowed = 'ALLOWED = {"silent": "/console/"}   # 入口路径不含身份信息;root 入口不暴露'
new_allowed = '''# 用户 -> 允许的入口前缀(路径不含身份信息)。
# 每个用户只能进自己的终端:silent 的实例跑 silent 身份,jhy 的实例跑 jhy 沙箱身份。
ALLOWED = {
    "silent": ("/console/", "/silent/"),
    "jhy": ("/terminal/",),
}


def entries_for(user):
    v = ALLOWED.get(user)
    if v is None:
        return ()
    return (v,) if isinstance(v, str) else tuple(v)


def default_entry(user):
    e = entries_for(user)
    return e[0] if e else "/console/"'''
if old_allowed in s:
    s = s.replace(old_allowed, new_allowed, 1)
    done.append("ALLOWED 多用户化")

# 旧的 safe_next 用 ALLOWED.get(user, "/console/") 当默认值 -> 改成 default_entry
s = s.replace('    default = ALLOWED.get(user, "/console/")\n    want = (want or "").replace("\\\\", "").strip()',
              '    default = default_entry(user)\n    want = (want or "").replace("\\\\", "").strip()')
s = s.replace('''    if not want.startswith("/") or want.startswith("//"):
        return default
    for pre in ENTRY_PREFIXES:
        if want.startswith(pre):
            return want
    return default''',
'''    if not want.startswith("/") or want.startswith("//"):
        return default
    for pre in entries_for(user):          # 只允许本人有权限的入口
        if want.startswith(pre):
            return want
    return default''')

# authcheck:按本人入口元组判断,去掉"所有人可进 /silent/"的口子
old_chk = 'if not (uri.startswith(ALLOWED.get(user, "/console/")) or uri.startswith("/silent/") or uri.startswith("/api/") or uri.startswith("/account/")):'
new_chk = 'if not (any(uri.startswith(p) for p in entries_for(user)) or uri.startswith("/api/") or uri.startswith("/account/")):'
if old_chk in s:
    s = s.replace(old_chk, new_chk, 1)
    done.append("authcheck 按用户校验")
else:
    print("⚠️ authcheck 锚点未匹配")

# 登录成功后返回 next=本人默认入口
s = s.replace('"next": safe_next(user, str(data.get("next", ""))),',
              '"next": safe_next(user, str(data.get("next", ""))) or default_entry(user),')

# whoami 带上入口;新增 /api/entry(登录用户访问别人入口时跳回自己的)
old_who = '''        if p.startswith("/api/whoami"):
            return self._json(200, {"ok": bool(user), "user": user or ""})'''
new_who = '''        if p.startswith("/api/whoami"):
            return self._json(200, {"ok": bool(user), "user": user or "",
                                    "entry": default_entry(user) if user else ""})
        if p.startswith("/api/entry"):
            # 已登录 -> 跳回自己的入口;未登录 -> 去登录页
            target = default_entry(user) if user else "/login.html"
            return self._json(302, {"ok": True, "entry": target},
                              [("Location", target)])'''
if old_who in s:
    s = s.replace(old_who, new_who, 1)
    done.append("/api/entry + whoami.entry")
else:
    print("⚠️ whoami 锚点未匹配")

# 管理类接口仅 silent 可用
guard = '''
        # 管理类接口仅管理员(silent)可用;jhy 等普通用户 403
        if user != "silent" and (p.startswith("/api/status") or p.startswith("/api/account/audit")
                                 or p == "/api/account/brand" or p == "/api/account/service"
                                 or p == "/api/account/logout-all"):
            print(f"[jqt-auth] DENY {p} user={user}", flush=True)
            return self._json(403, {"ok": False, "msg": "该操作仅管理员可用"})
'''
anchor = '''        # ---- 以下都需要登录 ----
        user = self._user()
        if not user:
            return self._json(401, {"ok": False, "msg": "未登录"})
'''
if "该操作仅管理员可用" not in s and anchor in s:
    s = s.replace(anchor, anchor + guard, 1)
    done.append("管理接口仅管理员")
else:
    print("   管理接口守卫:已存在或锚点未匹配")

io.open(GW, "w", encoding="utf-8").write(s)
c = subprocess.run(["python3", "-m", "py_compile", GW], capture_output=True, text=True)
print("② 网关已更新:" + "、".join(done) + " | 语法:", "通过" if c.returncode == 0 else c.stderr[:300])

# ---------------------------------------------------------------- ③ nginx
n = io.open(NGINX, encoding="utf-8").read()
if "/terminal/" not in n:
    add = '''    # jhy 的沙箱终端(独立 ttyd 实例,以 jhy 身份 + 沙箱壳运行)
    location /terminal/ {
        auth_request /_auth;
        error_page 401 =200 /login.html;
        error_page 403 = @entry;
        proxy_pass http://127.0.0.1:7684;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_read_timeout 3600s;
        proxy_set_header Accept-Encoding "";
        sub_filter_once on;
        sub_filter '</body>' '<script src="/assets/console-mobile.js" defer></script></body>';
    }

    # 登录用户误闯别人的入口 -> 跳回自己的入口
    location @entry {
        proxy_pass http://127.0.0.1:9000/api/entry;
    }

'''
    # 让 /console/ 与 /silent/ 也走 @entry(403 时跳回本人入口),401 仍给登录页
    n = n.replace('''    location /console/ {
        auth_request /_auth;
        error_page 401 403 =200 /login.html;''',
                  '''    location /console/ {
        auth_request /_auth;
        error_page 403 = @entry;
        error_page 401 =200 /login.html;''', 1)
    n = n.replace('''    location /silent/ {
        auth_request /_auth;
        error_page 401 403 =200 /login.html;''',
                  '''    location /silent/ {
        auth_request /_auth;
        error_page 403 = @entry;
        error_page 401 =200 /login.html;''', 1)
    anchor = "    # 账号与设置(需登录)\n"
    n = n.replace(anchor, add + anchor, 1)
    io.open(NGINX, "w", encoding="utf-8").write(n)
    print("③ nginx 已加 /terminal/ 与 @entry")
else:
    print("③ nginx 已存在")

pt = subprocess.run(["nginx", "-t"], capture_output=True, text=True)
print("   nginx -t:", (pt.stderr or "ok").strip().splitlines()[-1])
if pt.returncode == 0:
    subprocess.run(["systemctl", "reload", "nginx"], capture_output=True)
subprocess.run(["systemctl", "restart", "jqt-auth"], capture_output=True)
import time
time.sleep(1)
for u in ("jqt-auth", "nginx", "jqt-term-jhy"):
    print(f"   {u}: {subprocess.run(['systemctl', 'is-active', u], capture_output=True, text=True).stdout.strip()}")
