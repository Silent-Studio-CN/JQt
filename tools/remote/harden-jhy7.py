#!/usr/bin/env python3
"""jhy 沙箱第七轮(收口):
  ① resolv.conf 绑定改为**运行时探测软链目标**(WSL 换代后目标可能变)
  ② 查清 sandbox 内 lsmod 那条 hv 命中
  ③ 准备 sshpass 以便做真实 SSH 登录验证(没有就装)
"""
import io
import os
import re
import subprocess

FAKE = "/opt/jhy-sandbox/fake"
WRAPPER = "/usr/local/bin/jhy-shell"

s = io.open(WRAPPER, encoding="utf-8").read()

# ① 动态探测
if "RC_TARGET" not in s:
    s = s.replace("SANDBOX=(", '''# /etc/resolv.conf 在 WSL 里常是指向 /mnt/wsl/resolv.conf 的软链,
# 而沙箱会把 /mnt 盖成 tmpfs -> 软链悬空、DNS 失效。
# 因此运行时探测真实目标,把可用的 DNS 绑到**那个**路径上(换 WSL 版本也不怕)。
RC_TARGET="$(readlink -f /etc/resolv.conf 2>/dev/null || echo /etc/resolv.conf)"
[ "$RC_TARGET" = "/etc/resolv.conf" ] && RC_TARGET="/etc/resolv.conf"
RESOLV_BIND=(--ro-bind /opt/jhy-sandbox/fake/resolv.conf "$RC_TARGET")

SANDBOX=(''', 1)
    # 用数组变量替换硬编码那行
    s = re.sub(r"\n\s*--ro-bind /opt/jhy-sandbox/fake/resolv\.conf \S+", "", s)
    s = s.replace('  --tmpfs /mnt\n', '  --tmpfs /mnt\n  "${RESOLV_BIND[@]}"\n', 1)
    io.open(WRAPPER, "w", encoding="utf-8", newline="\n").write(s)
    os.chmod(WRAPPER, 0o755)
    print("   已改为运行时探测 resolv.conf 目标")
p = subprocess.run(["bash", "-n", WRAPPER], capture_output=True, text=True)
print("   wrapper 语法:", "通过" if p.returncode == 0 else p.stderr[:300])

# ② 看那条 hv 命中
r = subprocess.run(["sudo", "-u", "jhy", "-H", WRAPPER], input="lsmod | grep -E 'hv_|hyperv'\n",
                   capture_output=True, text=True, timeout=60)
print("   sandbox 内 lsmod 命中行:", (r.stdout or "").strip()[:200] or "(无)")

# ③ sshpass
have = subprocess.run(["bash", "-lc", "command -v sshpass"], capture_output=True, text=True).stdout.strip()
if not have:
    print("   安装 sshpass(用于真实登录验证)…")
    p = subprocess.run(["apt-get", "install", "-y", "-qq", "sshpass"], capture_output=True, text=True)
    print("   apt:", (p.stdout or p.stderr or "").strip()[-160:])
have = subprocess.run(["bash", "-lc", "command -v sshpass"], capture_output=True, text=True).stdout.strip()
print("   sshpass:", have or "不可用(改用其它方式验证)")
