#!/usr/bin/env python3
"""修两个真问题:
  ① 限额写错层:登录会话属于 user-1001.slice(不是 user@1001.service)-> 限制加到 slice 上
  ② 节点禁用了密码登录 -> 仅对 jhy 打开密码认证(其余保持公钥)
"""
import io
import os
import re
import subprocess

# ---------------- ① slice 级限制 ----------------
os.makedirs("/etc/systemd/system/user-1001.slice.d", exist_ok=True)
io.open("/etc/systemd/system/user-1001.slice.d/limits.conf", "w",
        encoding="utf-8", newline="\n").write("""[Slice]
# jhy(uid 1001)的真实资源上限。必须加在 **slice** 上:
# 登录会话是 user-1001.slice/session-*.scope,加在 user@1001.service 上不生效(实测踩过)。
CPUQuota=400%
MemoryMax=4G
MemoryHigh=3500M
TasksMax=1024
IOWeight=50
""")
print("   已写入 user-1001.slice.d/limits.conf")
subprocess.run(["systemctl", "daemon-reload"], capture_output=True)
subprocess.run(["systemctl", "restart", "user-1001.slice"], capture_output=True)
for f in ("memory.max", "cpu.max", "pids.max"):
    p = subprocess.run(["cat", f"/sys/fs/cgroup/user.slice/user-1001.slice/{f}"],
                       capture_output=True, text=True)
    print(f"   {f} = {p.stdout.strip() or p.stderr.strip()[:60]}")

# ---------------- ② 只给 jhy 开密码登录 ----------------
cfg_path = "/etc/ssh/sshd_config"
cfg = io.open(cfg_path, encoding="utf-8").read()
print("   全局 PasswordAuthentication:",
      next((l.strip() for l in cfg.splitlines()
            if re.match(r"^\s*PasswordAuthentication", l, re.I)), "(未显式设置,默认 no)"))

if "PasswordAuthentication yes" not in cfg.split("Match User jhy")[-1]:
    i = cfg.find("Match User jhy")
    block = cfg[i:]
    if "PasswordAuthentication" not in block:
        cfg = cfg[:i] + block.replace(
            "    ForceCommand /usr/local/bin/jhy-shell",
            "    # 仅 jhy 允许密码登录(节点全局是公钥),配合 fail2ban 防爆破\n"
            "    PasswordAuthentication yes\n"
            "    KbdInteractiveAuthentication no\n"
            "    ForceCommand /usr/local/bin/jhy-shell", 1)
        io.open(cfg_path, "w", encoding="utf-8").write(cfg)
        print("   已在 Match User jhy 中加入 PasswordAuthentication yes")
    else:
        print("   已存在密码认证设置")
else:
    print("   jhy 已允许密码登录")

p = subprocess.run(["/usr/sbin/sshd", "-t"], capture_output=True, text=True)
print("   sshd -t:", (p.stderr or "ok").strip()[:200])
if p.returncode == 0:
    subprocess.run(["systemctl", "reload", "ssh"], capture_output=True)
    print("   sshd 已重载")

# 该用户是否被锁/密码是否可用
for cmd in (["passwd", "-S", "jhy"],):
    p = subprocess.run(cmd, capture_output=True, text=True)
    print("   passwd -S:", p.stdout.strip() or p.stderr.strip()[:120])
