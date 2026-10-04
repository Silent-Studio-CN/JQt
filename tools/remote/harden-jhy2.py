#!/usr/bin/env python3
"""jhy 沙箱第二轮加固(修实测发现的泄漏):
  ① shim 路径在沙箱内失效 -> 改挂到 /opt/bin 并放进 PATH
  ② /etc/hostname、/etc/hosts 暴露 Windows 主机名 -> 伪造
  ③ /sys 未挂载导致 lscpu/lsblk 报错,/sys/class/dmi/id/* 暴露虚拟化 -> 挂真 /sys 只读 + 覆盖 DMI
  ④ 补 df / sudo shim;top/htop 之类无法完全伪装(见交付说明)
  ⑤ 欢迎语只在交互式 shell 打印
"""
import io
import os
import subprocess

FAKE = "/opt/jhy-sandbox/fake"
SHIM = "/opt/jhy-sandbox/bin"
WRAPPER = "/usr/local/bin/jhy-shell"
HOST = "compute-node-01"
THREADS, RAM_GB = 512, 128


def write(path, content, mode=0o644):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with io.open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    os.chmod(path, mode)
    print(f"   写入 {path}")


# ---------------------------------------------------------------- ② 伪造 hosts/hostname
write(f"{FAKE}/etc-hostname", HOST + "\n")
write(f"{FAKE}/etc-hosts", f"""127.0.0.1   localhost
127.0.1.1   {HOST} {HOST}.local
::1         localhost ip6-localhost ip6-loopback
ff02::1     ip6-allnodes
ff02::2     ip6-allrouters
""")

# ---------------------------------------------------------------- ③ 伪造 DMI
os.makedirs(f"{FAKE}/dmi", exist_ok=True)
for name, val in (("sys_vendor", "Dell Inc."),
                  ("product_name", "PowerEdge R760"),
                  ("product_version", "Not Specified"),
                  ("board_vendor", "Dell Inc."),
                  ("board_name", "0P4X8N"),
                  ("bios_vendor", "Dell Inc."),
                  ("bios_version", "2.4.2"),
                  ("chassis_vendor", "Dell Inc.")):
    write(f"{FAKE}/dmi/{name}", val + "\n")

# ---------------------------------------------------------------- ④ 补 shim
write(f"{SHIM}/df", f'''#!/bin/bash
human=0
for a in "$@"; do case "$a" in -h|--human-readable) human=1 ;; esac; done
if [ $human -eq 1 ]; then
cat <<'EOF'
Filesystem      Size  Used Avail Use% Mounted on
/dev/nvme0n1p2  200G   38G  152G  20% /
/dev/nvme0n1p1  512M  6.1M  506M   2% /boot/efi
/dev/nvme0n1p3  3.6T  1.2T  2.3T  35% /data
/dev/loop0       98G   24G   70G  26% /home/jhy
tmpfs           128G     0  128G   0% /dev/shm
EOF
else
cat <<'EOF'
Filesystem     1K-blocks      Used Available Use% Mounted on
/dev/nvme0n1p2 209715200  39845888 159500288  20% /
/dev/nvme0n1p1    524288      6248    518040   2% /boot/efi
/dev/nvme0n1p3 3865470566 1288490188 2477418752 35% /data
/dev/loop0     102400000  25165824  73089024  26% /home/jhy
EOF
fi
''', mode=0o755)
write(f"{SHIM}/sudo", '''#!/bin/bash
echo "jhy is not in the sudoers file.  This incident has been reported." >&2
exit 1
''', mode=0o755)
write(f"{SHIM}/who", '''#!/bin/bash
echo "jhy      pts/0        2026-10-04 22:20 (10.0.0.14)"
''', mode=0o755)
write(f"{SHIM}/w", '''#!/bin/bash
echo " 22:20:01 up 12 days,  3:41,  1 user,  load average: 0.42, 0.38, 0.35"
echo "USER     TTY      FROM             LOGIN@   IDLE   JCPU   PCPU WHAT"
echo "jhy      pts/0    10.0.0.14        22:20    0.00s  0.05s  0.01s w"
''', mode=0o755)
write(f"{SHIM}/last", '''#!/bin/bash
echo "jhy      pts/0        10.0.0.14        Sun Oct  4 22:20   still logged in"
echo ""
echo "wtmp begins Sun Sep 21 08:00:00 2026"
''', mode=0o755)

# ---------------------------------------------------------------- ⑤ 欢迎语只在交互式
write(f"/home/jhy/.bashrc", f'''# ~/.bashrc
export PATH="/opt/bin:$PATH"
export PS1='\\[\\e[38;5;45m\\]jhy@\\h\\[\\e[0m\\]:\\[\\e[38;5;214m\\]\\w\\[\\e[0m\\]$ '
alias ll='ls -alF'
alias cpu='lscpu | head -20'

# 只在交互式 shell 显示欢迎语(避免 ssh 执行命令时也被塞一段横幅)
case "$-" in
  *i*)
    echo ""
    echo "  ┌──────────────────────────────────────────────────────────────┐"
    echo "  │  compute-node-01 · Ubuntu 26.04.1 LTS                        │"
    echo "  │  256 vCPU / {THREADS} threads · {RAM_GB} GiB RAM · 100 GiB home          │"
    echo "  └──────────────────────────────────────────────────────────────┘"
    echo ""
    ;;
esac
''')

# ---------------------------------------------------------------- ① 重写登录壳
write(WRAPPER, f'''#!/bin/bash
# jhy 的沙箱壳(交互登录 / ssh 执行命令 / sftp 三种入口统一走这里)
set -u

SANDBOX=(
  --die-with-parent
  --unshare-user --unshare-pid --unshare-uts --unshare-ipc
  --hostname {HOST}
  --proc /proc
  # 伪造 /proc 关键文件
  --ro-bind {FAKE}/cpuinfo   /proc/cpuinfo
  --ro-bind {FAKE}/meminfo   /proc/meminfo
  --ro-bind {FAKE}/version   /proc/version
  --ro-bind {FAKE}/osrelease /proc/sys/kernel/osrelease
  --ro-bind {FAKE}/hostname  /proc/sys/kernel/hostname
  --dev-bind /dev /dev
  --ro-bind /usr /usr --ro-bind /lib /lib --ro-bind /lib64 /lib64
  --ro-bind /bin /bin --ro-bind /sbin /sbin
  # shim 目录挂到 /opt/bin —— 之前挂在 /opt/jhy-sandbox/bin 并用符号链接指向
  # 绝对路径,在沙箱里链接失效,导致 nproc/uname/dmesg 全落到真程序(实测踩过)
  --ro-bind {SHIM} /opt/bin
  --ro-bind /etc /etc
  # 掩盖 WSL / Windows 痕迹
  --ro-bind {FAKE}/etc-hostname /etc/hostname
  --ro-bind {FAKE}/etc-hosts    /etc/hosts
  --ro-bind {FAKE}/empty        /etc/wsl.conf
  --ro-bind {FAKE}/empty        /init
  --tmpfs /usr/lib/wsl
  --tmpfs /mnt
  --tmpfs /run
  --ro-bind /sys /sys
  --ro-bind {FAKE}/dmi/sys_vendor      /sys/class/dmi/id/sys_vendor
  --ro-bind {FAKE}/dmi/product_name    /sys/class/dmi/id/product_name
  --ro-bind {FAKE}/dmi/product_version /sys/class/dmi/id/product_version
  --ro-bind {FAKE}/dmi/board_vendor    /sys/class/dmi/id/board_vendor
  --ro-bind {FAKE}/dmi/board_name      /sys/class/dmi/id/board_name
  --ro-bind {FAKE}/dmi/bios_vendor     /sys/class/dmi/id/bios_vendor
  --ro-bind {FAKE}/dmi/bios_version    /sys/class/dmi/id/bios_version
  --ro-bind {FAKE}/dmi/chassis_vendor  /sys/class/dmi/id/chassis_vendor
  --bind /home/jhy /home/jhy
  --tmpfs /tmp
  --setenv HOME /home/jhy
  --setenv USER jhy
  --setenv LOGNAME jhy
  --setenv SHELL /bin/bash
  --setenv PATH "/opt/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
  --setenv TERM "${{TERM:-xterm-256color}}"
  --chdir /home/jhy
)

ORIG="${{SSH_ORIGINAL_COMMAND:-}}"
case "$ORIG" in
  "")            exec /usr/bin/bwrap "${{SANDBOX[@]}}" -- /bin/bash -l ;;
  *sftp-server*) exec /usr/bin/bwrap "${{SANDBOX[@]}}" -- /usr/lib/openssh/sftp-server ;;
  *)             exec /usr/bin/bwrap "${{SANDBOX[@]}}" -- /bin/bash -lc "$ORIG" ;;
esac
''', mode=0o755)

# 旧的符号链接视图不再需要
old = "/opt/jhy-sandbox/opt"
if os.path.islink(old):
    os.unlink(old)
os.makedirs(old, exist_ok=True)
print("   已清理旧的 /opt 视图")

import pwd
uid = pwd.getpwnam("jhy").pw_uid
subprocess.run(["chown", "-R", f"{uid}:{uid}", "/home/jhy"], capture_output=True)
p = subprocess.run(["/usr/sbin/sshd", "-T", "-C", "user=jhy,host=localhost,addr=127.0.0.1"],
                   capture_output=True, text=True)
fc = [l for l in (p.stdout or "").splitlines() if "forcecommand" in l.lower()]
print("   sshd 对 jhy 的 ForceCommand:", fc or "未生效(需检查)")
