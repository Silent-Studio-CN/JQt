#!/usr/bin/env python3
"""jhy 沙箱 v2 第二半:
  · 用 shutil.which 正确解析危险二进制路径,生成"绑定覆盖清单"
  · 伪造 /etc/sudoers 与 /etc/shadow(让她 sudo 之后读得到、但不含任何真实信息)
  · 重写登录壳:--tmp-overlay /(假 root 可写)+ LD_PRELOAD + 覆盖危险二进制 + sudo shim
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

# ---------------------------------------------------------------- 危险二进制
os.makedirs(f"{BLOCK}/real", exist_ok=True)
os.makedirs(REAL, exist_ok=True)
manifest = []
found = 0
for code, names in BLOCK_BINARIES.items():
    for n in names:
        src = shutil.which(n)
        if not src:
            continue
        found += 1
        # shim 需要的真实程序(rm/dd/kill)另存
        if n in ("rm", "dd", "kill"):
            dst = f"{REAL}/{n}"
            if not os.path.exists(dst):
                shutil.copy2(src, dst)
        dst = f"{BLOCK}/real{src}"
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        with io.open(dst, "w", encoding="utf-8", newline="\n") as f:
            f.write(f'''#!/bin/bash
# SilentSafe 拦截:{n}(绝对路径调用同样被拦)
export SS_CODE="SS_ERR_ID_{code}"
export SS_DESC="执行高危系统管理命令({n})"
exec {BLOCK}/block.sh "$@"
''')
        os.chmod(dst, 0o755)
        manifest.append(src)
print(f"   危险二进制:找到 {found} 个,已全部生成拦截器")

# sudo shim 也要覆盖真实 sudo(绝对路径调用)
for src in filter(None, [shutil.which("sudo")]):
    dst = f"{BLOCK}/real{src}"
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    with io.open(dst, "w", encoding="utf-8", newline="\n") as f:
        f.write(f'#!/bin/bash\nexec {BIN}/sudo "$@"\n')
    os.chmod(dst, 0o755)
    manifest.append(src)
    print(f"   sudo:覆盖 {src}")

# rm/dd/kill shim 也覆盖真实路径(参数检查)
for n in ("rm", "dd", "kill"):
    src = shutil.which(n)
    if not src:
        continue
    dst = f"{BLOCK}/real{src}"
    with io.open(dst, "w", encoding="utf-8", newline="\n") as f:
        f.write(f'#!/bin/bash\nexec {BIN}/{n} "$@"\n')
    os.chmod(dst, 0o755)
    if src not in manifest:
        manifest.append(src)

with io.open(f"{SB}/silentsafe/bind-list.txt", "w", encoding="utf-8", newline="\n") as f:
    f.write("\n".join(sorted(set(manifest))) + "\n")
print(f"   绑定覆盖清单:{len(set(manifest))} 条")

# ---------------------------------------------------------------- 伪造 passwd 类文件
with io.open(f"{FAKE}/sudoers", "w", encoding="utf-8", newline="\n") as f:
    f.write("""## Sudoers allows particular users to run various commands as root
Defaults        env_reset
Defaults        mail_badpass
Defaults        secure_path="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"

root    ALL=(ALL:ALL) ALL
%admin  ALL=(ALL) ALL
%sudo   ALL=(ALL:ALL) ALL
jhy     ALL=(ALL:ALL) ALL
""")
with io.open(f"{FAKE}/shadow", "w", encoding="utf-8", newline="\n") as f:
    f.write("root:!:20300:0:99999:7:::\n"
            "daemon:*:20300:0:99999:7:::\n"
            "bin:*:20300:0:99999:7:::\n"
            "sys:*:20300:0:99999:7:::\n"
            "jhy:$6$kJ8fQ2vN$3Xy7QmZ1pL0dVvJ9wRb2cT8hN5sA6eF4gH1iK2lM3nO4pQ5rS6tU7vW8xY9zA0bC1dE2fG3hI4jK5lM6nO7p:20300:0:99999:7:::\n")

# ---------------------------------------------------------------- 重写登录壳
def w(path, content, mode=0o755):
    with io.open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    os.chmod(path, mode)


w(WRAPPER, f'''#!/bin/bash
# jhy 的沙箱壳(v2)
#   · --tmp-overlay / :她(及假 root)看起来可写系统目录,改动落在隐形 tmpfs,退出即消失
#   · LD_PRELOAD      :内核调用层伪装 uname/sysinfo/sysconf/get_nprocs/gethostname,
#                       绕开 PATH shim 直接调绝对路径也查不到真实配置
#   · 覆盖危险二进制  :SilentSafe 拦截,绝对路径调用同样命中
set -u

# resolv.conf 在 WSL 里是软链,WSL 换代后目标可能变 -> 运行时探测
RC_TARGET="$(readlink -f /etc/resolv.conf 2>/dev/null || echo /etc/resolv.conf)"

SANDBOX=(
  --die-with-parent
  --unshare-user --unshare-pid --unshare-uts --unshare-ipc
  --hostname {HOST}
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
  --ro-bind /usr /usr --ro-bind /lib /lib --ro-bind /lib64 /lib64
  --ro-bind /bin /bin --ro-bind /sbin /sbin
  --ro-bind {BIN} /opt/bin
  --ro-bind {BLOCK} /opt/silentsafe
  --ro-bind {LIB} {LIB}
  --ro-bind /etc /etc
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

# 让 root 看起来能写系统目录(改动进隐形 tmpfs,退出即消失)
if /usr/bin/bwrap --help 2>&1 | grep -q -- '--tmp-overlay'; then
  SANDBOX+=(--tmp-overlay /)
fi

# 危险二进制:绑定拦截器覆盖(绝对路径也拦)
if [ -r /opt/jhy-sandbox/silentsafe/bind-list.txt ]; then
  while IFS= read -r dst; do
    [ -n "$dst" ] && [ -e "$dst" ] && SANDBOX+=(--ro-bind "/opt/silentsafe/real$dst" "$dst")
  done < /opt/jhy-sandbox/silentsafe/bind-list.txt
fi

ORIG="${{SSH_ORIGINAL_COMMAND:-}}"
case "$ORIG" in
  "")            exec /usr/bin/bwrap "${{SANDBOX[@]}}" -- /bin/bash -l ;;
  *sftp-server*) exec /usr/bin/bwrap "${{SANDBOX[@]}}" -- /usr/lib/openssh/sftp-server ;;
  *)             exec /usr/bin/bwrap "${{SANDBOX[@]}}" -- /bin/bash -lc "$ORIG" ;;
esac
''')

# sudo -l 支持(看起来像真的)
s = io.open(f"{BIN}/sudo", encoding="utf-8").read()
if "may run the following" not in s:
    s = s.replace('NONINTERACTIVE=0', '''# sudo -l:列出"她自己的" sudoers 条目(内容来自我们伪造的 /etc/sudoers)
case " $* " in
  *" -l "*|*" --list "*)
    echo "Matching Defaults entries for $(id -un) on {HOST}:" >&2
    echo "    env_reset, mail_badpass, secure_path=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin" >&2
    echo "" >&2
    echo "User $(id -un) may run the following commands on {HOST}:" >&2
    echo "    (ALL : ALL) ALL" >&2
    exit 0 ;;
esac

NONINTERACTIVE=0'''.replace("{HOST}", HOST), 1)
    io.open(f"{BIN}/sudo", "w", encoding="utf-8", newline="\n").write(s)
    os.chmod(f"{BIN}/sudo", 0o755)
print("   sudo -l 已支持")

p = subprocess.run(["bash", "-n", WRAPPER], capture_output=True, text=True)
print("   wrapper 语法:", "通过" if p.returncode == 0 else p.stderr[:300])
print("done-part2")
