#!/usr/bin/env python3
"""重写登录壳(修正绑定顺序):
  根 tmpfs -> 基础只读绑定(/usr /bin /sbin /lib /lib64 /etc /sys) -> 伪造文件
  -> 可写 tmpfs(/usr/local /opt /root /srv /var/tmp /var/log) -> 家目录可写绑定
  -> 危险二进制覆盖 -> 环境变量
"""
import io
import os
import subprocess

SB = "/opt/jhy-sandbox"
FAKE, BIN, LIB, BLOCK = f"{SB}/fake", f"{SB}/bin", f"{SB}/lib", f"{SB}/block"
WRAPPER = "/usr/local/bin/jhy-shell"
HOST = "compute-node-01"

CONTENT = f'''#!/bin/bash
# jhy 的沙箱壳 v2.2
#  · 根是空 tmpfs,真正可见的内容靠下面显式绑定(顺序很重要:
#    先基础只读绑定,再在可写位置挂 tmpfs,最后覆盖危险二进制)
#  · LD_PRELOAD 在内核调用层伪装 uname/sysinfo/sysconf/get_nprocs/gethostname
#  · SilentSafe:危险二进制被拦截器覆盖(绝对路径调用同样命中)
set -u

RC_TARGET="$(readlink -f /etc/resolv.conf 2>/dev/null || echo /etc/resolv.conf)"

SANDBOX=(
  --die-with-parent
  --unshare-user --unshare-pid --unshare-uts --unshare-ipc
  --hostname {HOST}

  # ① 基础文件系统(只读)
  --ro-bind /usr /usr
  --ro-bind /bin /bin
  --ro-bind /sbin /sbin
  --ro-bind /lib /lib
  --ro-bind /lib64 /lib64
  --ro-bind /etc /etc
  --ro-bind /sys /sys
  --dev-bind /dev /dev

  # ② 进程与内核视图(伪造)
  --proc /proc
  --ro-bind {FAKE}/cpuinfo   /proc/cpuinfo
  --ro-bind {FAKE}/meminfo   /proc/meminfo
  --ro-bind {FAKE}/version   /proc/version
  --ro-bind {FAKE}/osrelease /proc/sys/kernel/osrelease
  --ro-bind {FAKE}/hostname  /proc/sys/kernel/hostname
  --ro-bind {FAKE}/cmdline   /proc/cmdline
  --ro-bind {FAKE}/stat      /proc/stat
  --ro-bind {FAKE}/modules   /proc/modules

  # ③ 掩盖宿主痕迹
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
  --ro-bind {FAKE}/sys-cpu /sys/devices/system/cpu
  --tmpfs /sys/module

  # ④ 假 root 能"写系统"的位置(改动只在内存,退出即消失;
  #    overlayfs 在该内核的 user namespace 里不可用,只能这么做)
  --tmpfs /usr/local
  --tmpfs /opt
  --tmpfs /root
  --tmpfs /srv
  --tmpfs /var/tmp
  --tmpfs /var/log

  # ⑤ 她的家目录(真实可写,100 GiB 硬限)
  --bind /home/jhy /home/jhy
  --tmpfs /tmp

  # ⑥ 工具与注入
  --ro-bind {BIN} /opt/bin
  --ro-bind {BLOCK} /opt/silentsafe
  --ro-bind {LIB} {LIB}

  --setenv HOME /home/jhy
  --setenv USER jhy
  --setenv LOGNAME jhy
  --setenv SHELL /bin/bash
  --setenv PATH "/opt/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
  --setenv LD_PRELOAD {LIB}/fakehw.so
  --setenv TERM "${{TERM:-xterm-256color}}"
  --chdir /home/jhy
)

# ⑦ 危险二进制:用宿主路径作源(绑定清单里是 realpath)
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
'''
with io.open(WRAPPER, "w", encoding="utf-8", newline="\n") as f:
    f.write(CONTENT)
os.chmod(WRAPPER, 0o755)
print("   已重写 wrapper")
p = subprocess.run(["bash", "-n", WRAPPER], capture_output=True, text=True)
print("   语法:", "通过" if p.returncode == 0 else p.stderr[:300])

test = r'''
echo "谁: $(whoami)@$(hostname)  nproc=$(nproc)"
echo "绝对路径: /usr/bin/nproc=$(/usr/bin/nproc)  /usr/bin/uname=$(/usr/bin/uname -r)  python=$(python3 -c 'import os;print(os.cpu_count())')"
echo "内存: $(free -h | sed -n 2p)"
echo "--- sudo ---"
echo 'jhy20110726' | sudo -S id 2>/dev/null | head -1
echo 'jhy20110726' | sudo -S sh -c 'touch /usr/local/lib/ok && echo "写 /usr/local 成功 uid=$(id -u)"'
echo "sudoers 末行: $(echo 'jhy20110726' | sudo -S tail -1 /etc/sudoers 2>/dev/null)"
echo "--- 拦截 ---"
for c in "reboot" "mkfs.ext4 /dev/sda1" "iptables -F" "useradd x" "crontab -e" "rm -rf /" "dd if=/dev/zero of=/dev/sda"; do
  printf "  %-22s -> %s\n" "$c" "$($c 2>&1 | head -2 | tr '\n' '|')"
done
printf "  %-22s -> %s\n" "/usr/sbin/reboot" "$(/usr/sbin/reboot 2>&1 | head -1)"
echo "--- 正常 ---"
mkdir -p ~/w && echo ok > ~/w/f && cat ~/w/f && df -h ~ | tail -1
'''
p = subprocess.run(["sudo", "-u", "jhy", "-H", WRAPPER], input=test, capture_output=True,
                   text=True, timeout=180)
print(p.stdout.rstrip()[:3500])
if p.stderr.strip():
    print("   [stderr]", p.stderr.strip()[:500])
