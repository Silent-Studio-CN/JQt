#!/usr/bin/env python3
"""修 nginx:命名 location 里的 proxy_pass 不能带 URI -> 用变量形式绕过"""
import io
import subprocess

P = "/etc/nginx/sites-available/jqt"
s = io.open(P, encoding="utf-8").read()
old = '''    location @entry {
        proxy_pass http://127.0.0.1:9000/api/entry;
    }'''
new = '''    location @entry {
        # 命名 location 里 proxy_pass 不允许带 URI,用变量形式绕过该限制
        set $jqt_entry_path /api/entry;
        proxy_pass http://127.0.0.1:9000$jqt_entry_path;
    }'''
if old in s:
    io.open(P, "w", encoding="utf-8").write(s.replace(old, new, 1))
    print("已改为变量形式")
else:
    print("⚠️ 未匹配(可能已改)")

p = subprocess.run(["nginx", "-t"], capture_output=True, text=True)
print("nginx -t:", (p.stderr or "ok").strip().splitlines()[-1])
if p.returncode == 0:
    subprocess.run(["systemctl", "reload", "nginx"], capture_output=True)
    print("已重载")
    for u in ("/console/", "/silent/", "/terminal/", "/login.html", "/openapi.yaml"):
        r = subprocess.run(["curl", "-s", "-o", "/dev/null", "-w", "%{http_code}", "--max-time", "5",
                            f"http://127.0.0.1:8080{u}"], capture_output=True, text=True)
        print(f"   {u:16s} -> {r.stdout}")
