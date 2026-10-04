#!/usr/bin/env python3
"""收口:① 补全模块过滤(pci_hyperv 之类)② 真实 SSH 登录验证 jhy"""
import io
import re
import subprocess

FAKE = "/opt/jhy-sandbox/fake"
BAD = re.compile(r"hv_|hyperv|vmbus|msft|xen_|virtio", re.I)

# ① 重新生成 /proc/modules(名字里含 hyperv/hv_/vmbus 等一律剔除)
mods = []
try:
    import glob
    for d in sorted(glob.glob("/sys/module/*")):
        name = d.rsplit("/", 1)[-1]
        if BAD.search(name):
            continue
        mods.append(f"{name} 16384 1 - Live 0x0000000000000000\n")
except Exception as e:
    print("   读取模块失败:", e)
io.open(f"{FAKE}/modules", "w", encoding="utf-8", newline="\n").write("".join(mods[:200]))
print(f"   已重写 /proc/modules:{len(mods)} 个模块,其中被剔除的虚拟化相关名字:")
import glob
print("   ", [d.rsplit('/', 1)[-1] for d in sorted(glob.glob('/sys/module/*'))
              if BAD.search(d.rsplit('/', 1)[-1])][:12])

# ② 真实 SSH 登录验证
TEST = r'''
set -u
PASS='jhy20110726'
echo "== 真实 ssh 登录(密码认证 + ForceCommand 沙箱)=="
sshpass -p "$PASS" ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
  -o PreferredAuthentications=password -o PubkeyAuthentication=no -o LogLevel=ERROR \
  -p 1104 jhy@127.0.0.1 'bash -lc "
     echo \"whoami       : \$(whoami)\"
     echo \"hostname     : \$(hostname)\"
     echo \"nproc        : \$(nproc)\"
     echo \"uname        : \$(uname -sr)\"
     echo \"virt         : \$(systemd-detect-virt)\"
     echo \"meminfo      : \$(head -1 /proc/meminfo)\"
     echo \"free         : \$(free -h | sed -n 2p)\"
     echo \"hyperv 模块   : \$(lsmod | grep -ciE \\\"hv_|hyperv|vmbus\\\")\"
     echo \"home 容量    : \$(df -h ~ | tail -1)\"
     echo \"外网         : \$(timeout 8 curl -s -o /dev/null -w %{http_code} https://www.bing.com)\"
     echo \"cgroup       : \$(cat /proc/self/cgroup)\"
     echo \"sudo         : \$(sudo -n true 2>&1 | head -1)\"
  "'
'''
p = subprocess.run(["bash", "-lc", TEST], capture_output=True, text=True, timeout=180)
print(p.stdout.strip() or "(无输出)")
if p.stderr.strip():
    print("   [stderr]", p.stderr.strip()[:400])

# ③ 她的 slice 是否真的限住
print()
print("== 她的登录会话落在哪个 slice / 上限 ==")
for line in subprocess.run(["bash", "-lc",
                            "systemctl status user-1001.slice --no-pager 2>/dev/null | head -5; "
                            "cat /sys/fs/cgroup/user.slice/user-1001.slice/memory.max 2>/dev/null | sed 's/^/memory.max=/'; "
                            "cat /sys/fs/cgroup/user.slice/user-1001.slice/cpu.max 2>/dev/null | sed 's/^/cpu.max=/'"],
                           capture_output=True, text=True).stdout.splitlines():
    print("   " + line)
