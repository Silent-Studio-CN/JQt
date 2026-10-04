#!/usr/bin/env python3
"""jhy 沙箱第五轮:修 DNS/外网
  上一轮把 /run 变成 tmpfs,而 /etc/resolv.conf 是软链(指向 /run 或 /mnt/wsl),
  沙箱里变成悬空链接 -> 既绑不上假文件,也没有 DNS。
  改为:**保留真实 /run**(DNS 正常),只把 WSL 特有的 /run/WSL 用 tmpfs 盖掉。
"""
import io
import os
import subprocess

WRAPPER = "/usr/local/bin/jhy-shell"
s = io.open(WRAPPER, encoding="utf-8").read()
orig = s

# 去掉假 resolv.conf 绑定(悬空软链绑不上)
s = s.replace("  --ro-bind /opt/jhy-sandbox/fake/resolv.conf /etc/resolv.conf\n", "")
# /run 保留真实(tmpfs -> 去掉),改为只盖 /run/WSL
s = s.replace("  --tmpfs /run\n", "  --tmpfs /run/WSL\n")
if "--tmpfs /run/WSL" not in s:
    s = s.replace("  --tmpfs /mnt\n", "  --tmpfs /mnt\n  --tmpfs /run/WSL\n")

io.open(WRAPPER, "w", encoding="utf-8", newline="\n").write(s)
os.chmod(WRAPPER, 0o755)
print("   改动:", "已更新" if s != orig else "无变化")
print("   /run 相关绑定:", [l.strip() for l in s.splitlines() if "/run" in l])
p = subprocess.run(["bash", "-n", WRAPPER], capture_output=True, text=True)
print("   语法:", "通过" if p.returncode == 0 else p.stderr[:200])
print("   resolv.conf 真实指向:", subprocess.run(["readlink", "-f", "/etc/resolv.conf"],
                                               capture_output=True, text=True).stdout.strip())
