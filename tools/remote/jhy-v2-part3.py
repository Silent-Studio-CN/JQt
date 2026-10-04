#!/usr/bin/env python3
"""jhy 沙箱 v2 修正:
  ① --tmp-overlay 必须跟 --overlay-src(改用 "整根 overlay",不再逐个 ro-bind /usr 等)
  ② 绑定源必须是**宿主路径**(之前误用了沙箱内路径)
  ③ 软链命令(dd→coreutils/dd、nc→nc.openbsd)按 realpath 生成拦截器与清单
"""
import io
import os
import shutil
import subprocess

SB = "/opt/jhy-sandbox"
FAKE, BIN, LIB, BLOCK, REAL = f"{SB}/fake", f"{SB}/bin", f"{SB}/lib", f"{SB}/block", f"{SB}/real"
WRAPPER = "/usr/local/bin/jhy-shell"
HOST = "compute-node-01"

BLOCK_BINARIES = {
    "101": ["mkfs", "mkfs.ext4", "mkfs.xfs", "mkfs.vfat", "mke2fs", "fdisk", "sfdisk", "cfdisk",
            "parted", "sgdisk", "mkswap", "swapon", "swapoff", "fsck", "e2fsck", "tune2fs",
            "resize2fs", "mdadm", "lvcreate", "lvreduce", "pvcreate", "vgcreate"],
    "102": ["shutdown", "reboot", "poweroff", "halt", "telinit", "kexec",
            "insmod", "rmmod", "modprobe", "depmod"],
    "103": ["iptables", "ip6tables", "nft", "ufw", "firewall-cmd", "arptables", "ebtables"],
    "104": ["useradd", "userdel", "usermod", "adduser", "deluser", "groupadd", "groupdel",
            "chpasswd", "passwd", "chsh", "visudo", "chage", "gpasswd", "su"],
    "105": ["crontab", "at", "batch", "systemctl", "systemd-run", "timedatectl", "localectl"],
    "106": ["docker", "podman", "lxc-start", "lxc-create", "systemd-nspawn", "virt-install",
            "qemu-system-x86_64"],
    "108": ["nmap", "masscan", "nc", "ncat", "netcat", "socat", "tcpdump", "ettercap"],
}

# 清掉上一轮产物
shutil.rmtree(f"{BLOCK}/real", ignore_errors=True)
os.makedirs(f"{BLOCK}/real", exist_ok=True)

manifest = set()
for code, names in BLOCK_BINARIES.items():
    for n in names:
        src = shutil.which(n)
        if not src:
            continue
        real = os.path.realpath(src)          # ③ 解析软链
        # 需要保留真实程序的(rm/dd/kill)先拷出来
        if n in ("rm", "dd", "kill") and not os.path.exists(f"{REAL}/{n}"):
            shutil.copy2(real, f"{REAL}/{n}")
        dst = f"{BLOCK}/real{real}"
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        with io.open(dst, "w", encoding="utf-8", newline="\n") as f:
            f.write(f'''#!/bin/bash
# SilentSafe 拦截:{n}(含绝对路径/软链调用)
export SS_CODE="SS_ERR_ID_{code}"
export SS_DESC="执行高危系统管理命令({n})"
exec {BLOCK}/block.sh "$@"
''')
        os.chmod(dst, 0o755)
        manifest.add(real)

# sudo 覆盖 + rm/dd/kill 参数检查覆盖
for n in ("sudo", "rm", "dd", "kill"):
    src = shutil.which(n)
    if not src:
        continue
    real = os.path.realpath(src)
    dst = f"{BLOCK}/real{real}"
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    target = f"{BIN}/{n}" if n != "sudo" else f"{BIN}/sudo"
    with io.open(dst, "w", encoding="utf-8", newline="\n") as f:
        f.write(f'#!/bin/bash\nexec {target} "$@"\n')
    os.chmod(dst, 0o755)
    manifest.add(real)

with io.open(f"{SB}/silentsafe/bind-list.txt", "w", encoding="utf-8", newline="\n") as f:
    f.write("\n".join(sorted(manifest)) + "\n")
print(f"   拦截器 {len(manifest)} 个(已按 realpath 生成)")

# ---------------------------------------------------------------- 重写登录壳
def w(path, content, mode=0o755):
    with io.open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    os.chmod(path, mode)


w(WRAPPER, f'''#!/bin/bash
# jhy 的沙箱壳(v2.1)
#   根文件系统用 overlay 挂在隐形 tmpfs 上 -> 她/假 root 看起来可以写系统目录,
#   改动只存在于内存,退出即恢复(所以不再逐个 ro-bind /usr /etc ...)
set -u

RC_TARGET="$(readlink -f /etc/resolv.conf 2>/dev/null || echo /etc/resolv.conf)"

SANDBOX=(
  --die-with-parent
  --unshare-user --unshare-pid --unshare-uts --unshare-ipc
  --hostname {HOST}
  # ① 整根 overlay(必须 --overlay-src 在前,否则 bwrap 报
  #    "--tmp-overlay requires at least one --overlay-src")
  --overlay-src /
  --tmp-overlay /
  --proc /proc
  --ro-bind {FAKE}/cpuinfo   /proc/cpuinfo
  --ro-bind {FAKE}/meminfo   /proc/meminfo
  --ro-bind {FAKE}/version   /proc/version
  --ro-bind {FAKE}/osrelease /proc/sys/kernel/osrelease
  --ro-bind {FAKE}/hostname  /proc/sys/kernel/hostname
  --ro-bind {FAKE}/cmdline   /proc/cmdline
  --ro-bind {FAKE}/stat      /proc/stat
  --ro-bind {FAKE}/modules   /proc/modules
  --dev-bind /dev /dev
  --ro-bind {BIN} /opt/bin
  --ro-bind {BLOCK} /opt/silentsafe
  --ro-bind {LIB} {LIB}
  --ro-bind {FAKE}/etc-hostname /etc/hostname
  --ro-bind {FAKE}/etc-hosts    /etc/hosts
  --ro-bind {FAKE}/sudoers      /etc/sudoers
  --ro-bind {FAKE}/shadow       /etc/shadow
  --ro-bind {FAKE}/empty        /etc/wsl.conf
  --ro-bind {FAKE}/empty        /init
  --tmpfs /usr/lib/wsl
  --tmpfs /mnt
  --ro-bind {FAKE}/resolv.conf "$RC_TARGET"
  --tmpfs /run/WSL
  --ro-bind /sys /sys
  --ro-bind {FAKE}/sys-cpu /sys/devices/system/cpu
  --tmpfs /sys/module
  --bind /home/jhy /home/jhy
  --tmpfs /tmp
  --setenv HOME /home/jhy
  --setenv USER jhy
  --setenv LOGNAME jhy
  --setenv SHELL /bin/bash
  --setenv PATH "/opt/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
  --setenv LD_PRELOAD {LIB}/fakehw.so
  --setenv TERM "${{TERM:-xterm-256color}}"
  --chdir /home/jhy
)

# 危险二进制:用**宿主路径**作绑定源(之前误用沙箱内路径 -> "Can't find source path")
if [ -r /opt/jhy-sandbox/silentsafe/bind-list.txt ]; then
  while IFS= read -r dst; do
    [ -z "$dst" ] && continue
    src="/opt/jhy-sandbox/block/real$dst"
    [ -e "$src" ] && [ -e "$dst" ] && SANDBOX+=(--ro-bind "$src" "$dst")
  done < /opt/jhy-sandbox/silentsafe/bind-list.txt
fi

ORIG="${{SSH_ORIGINAL_COMMAND:-}}"
case "$ORIG" in
  "")            exec /usr/bin/bwrap "${{SANDBOX[@]}}" -- /bin/bash -l ;;
  *sftp-server*) exec /usr/bin/bwrap "${{SANDBOX[@]}}" -- /usr/lib/openssh/sftp-server ;;
  *)             exec /usr/bin/bwrap "${{SANDBOX[@]}}" -- /bin/bash -lc "$ORIG" ;;
esac
''')

p = subprocess.run(["bash", "-n", WRAPPER], capture_output=True, text=True)
print("   wrapper 语法:", "通过" if p.returncode == 0 else p.stderr[:300])
t = subprocess.run(["sudo", "-u", "jhy", "-H", WRAPPER], input="echo 沙箱起来了\n",
                   capture_output=True, text=True, timeout=90)
print("   自测输出:", (t.stdout or "").strip().splitlines()[-1] if t.stdout.strip() else "(空)")
if t.stderr.strip():
    print("   stderr:", t.stderr.strip()[:300])
