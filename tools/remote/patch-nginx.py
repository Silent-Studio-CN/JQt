#!/usr/bin/env python3
"""在节点上执行:给 nginx 加 /api/exec 与 /silent/ 两个 location。"""
import io

P = "/etc/nginx/sites-available/jqt"
s = io.open(P, encoding="utf-8").read()
changed = []

ANCHOR_API = "    location /api/ { proxy_pass http://127.0.0.1:9000; }\n"
ADD_API = ANCHOR_API + """
    # curl 直连的终端 API(自带 token 鉴权,因此不经 cookie 登录)
    location /api/exec {
        proxy_pass http://127.0.0.1:9100;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_read_timeout 320s;
    }
"""
if "/api/exec" not in s and ANCHOR_API in s:
    s = s.replace(ANCHOR_API, ADD_API, 1)
    changed.append("/api/exec")

ANCHOR_ACC = "    # 账号与设置(需登录)\n"
ADD_SILENT = """    # 旧入口 /silent:用户仍在使用,保留可用;首页不展示该路径
    location /silent/ {
        auth_request /_auth;
        error_page 401 403 =200 /login.html;
        proxy_pass http://127.0.0.1:7683;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_read_timeout 3600s;
        proxy_set_header Accept-Encoding "";
        sub_filter_types text/html;
        sub_filter_once on;
        sub_filter '</body>' '<script src="/assets/console-mobile.js" defer></script></body>';
    }

""" + ANCHOR_ACC
if "location /silent/" not in s and ANCHOR_ACC in s:
    s = s.replace(ANCHOR_ACC, ADD_SILENT, 1)
    changed.append("/silent/")

if changed:
    io.open(P, "w", encoding="utf-8").write(s)
    print("已更新 nginx:", ", ".join(changed))
else:
    print("nginx 无需改动(或锚点未命中)")
