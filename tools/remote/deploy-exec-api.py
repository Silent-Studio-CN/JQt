#!/usr/bin/env python3
"""部署到远程节点:
  ① 恢复 /silent 路由(独立 ttyd 实例 + nginx location)
  ② 安装 curl 直连的终端 API(独立服务 + nginx location)
  ③ 放行登录网关对 /silent/ 前缀的校验
用法:python tools/remote/deploy-exec-api.py
"""
import os
import subprocess
import sys

KEY = os.path.expanduser(r"~\.ssh\id_ed25519_jqt")
HOST = "silent@192.168.211.7"
SSH = ["ssh", "-p", "1104", "-i", KEY, "-o", "StrictHostKeyChecking=no", HOST]
SCP = ["scp", "-P", "1104", "-i", KEY, "-o", "StrictHostKeyChecking=no"]
HERE = os.path.dirname(os.path.abspath(__file__))


def run(cmd, check=True, quiet=False):
    p = subprocess.run(SSH + [cmd], capture_output=True, text=True,
                       encoding="utf-8", errors="replace")
    if not quiet:
        for line in (p.stdout or "").strip().splitlines():
            print("   " + line[:400])
        if p.stderr.strip():
            print("   [stderr] " + p.stderr.strip()[:400])
    if check and p.returncode != 0:
        print(f"   ⚠️ 退出码 {p.returncode}")
    return p


def send(local, remote):
    p = subprocess.run(SCP + [local, f"{HOST}:{remote}"], capture_output=True, text=True)
    print(("   ✅ 已上传 " if p.returncode == 0 else "   ⚠️ 上传失败 ") + os.path.basename(local))
    return p.returncode == 0


UNIT_API = """[Unit]
Description=JQt remote terminal API (curl)
After=network.target

[Service]
Type=simple
ExecStart=/usr/bin/python3 /usr/local/bin/jqt-exec-api.py
Restart=always
RestartSec=2
User=root

[Install]
WantedBy=multi-user.target
"""

UNIT_SILENT = """[Unit]
Description=SilentRemoveKit legacy console (ttyd, /silent)
After=network.target

[Service]
User=silent
Group=silent
WorkingDirectory=/home/silent
Environment=TERM=xterm-256color
ExecStart=/usr/bin/ttyd -i 127.0.0.1 -p 7683 -b /silent -W bash -l
Restart=always
RestartSec=2

[Install]
WantedBy=multi-user.target
"""

print("① 上传终端 API 服务与远程补丁脚本")
ok = True
ok &= send(os.path.join(HERE, "jqt-exec-api.py"), "/tmp/jqt-exec-api.py")
ok &= send(os.path.join(HERE, "patch-nginx.py"), "/tmp/patch-nginx.py")
ok &= send(os.path.join(HERE, "patch-auth.py"), "/tmp/patch-auth.py")
if not ok:
    sys.exit("上传失败,终止")

print("② 安装服务脚本")
run("sudo install -m 0755 -o root -g root /tmp/jqt-exec-api.py /usr/local/bin/jqt-exec-api.py && "
    "sudo mkdir -p /var/log/jqt && sudo chmod 700 /var/log/jqt && "
    "ls -l /usr/local/bin/jqt-exec-api.py")

print("③ 写入 systemd 单元并启动")
for name, body in (("jqt-exec-api.service", UNIT_API),
                   ("jqt-term-silent-legacy.service", UNIT_SILENT)):
    local = os.path.join(HERE, name)
    with open(local, "w", encoding="utf-8", newline="\n") as f:
        f.write(body)
    send(local, f"/tmp/{name}")
run("sudo install -m 0644 /tmp/jqt-exec-api.service /tmp/jqt-term-silent-legacy.service "
    "/etc/systemd/system/ && sudo systemctl daemon-reload && "
    "sudo systemctl enable --now jqt-exec-api.service jqt-term-silent-legacy.service && "
    "sleep 2 && systemctl is-active jqt-exec-api jqt-term-silent-legacy")

print("④ 应用 nginx 与网关补丁")
run("sudo python3 /tmp/patch-nginx.py")
run("sudo python3 /tmp/patch-auth.py")
run("sudo nginx -t && sudo systemctl reload nginx && echo nginx已重载")
run("sudo systemctl restart jqt-auth && sleep 1 && systemctl is-active jqt-auth")

print("⑤ token(请妥善保存)")
run("sudo /usr/bin/python3 /usr/local/bin/jqt-exec-api.py --show-tokens")
print("完成")
