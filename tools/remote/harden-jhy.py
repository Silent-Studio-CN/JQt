#!/usr/bin/env python3
"""加固 jhy 沙箱:
  ① 补 uname / getconf shim(uname 走系统调用,读不到伪造的 /proc)
  ② 登录壳支持 ForceCommand(SSH_ORIGINAL_COMMAND)与 sftp —— 否则
     `ssh jhy@host 'cat /proc/version'` 会绕过沙箱直接看到真系统
  ③ sshd Match User jhy 强制走沙箱壳,并关掉转发类功能
"""
import io
import os
import subprocess

SHIM = "/opt/jhy-sandbox/bin"
WRAPPER = "/usr/local/bin/jhy-shell"
FAKE_HOSTNAME = "compute-node-01"
FAKE_THREADS = 512


def write(path, content, mode=0o644):
    with io.open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    os.chmod(path, mode)
    print(f"   写入 {path}")


# ---------------------------------------------------------------- ① uname / getconf
write(f"{SHIM}/uname", f'''#!/bin/bash
# uname 直接问内核,伪造 /proc 对它无效 -> 用 shim 覆盖
SHORT=0; NODE=0; REL=0; VER=0; MACH=0; PROC=0; ALL=0; OS=0
[ $# -eq 0 ] && ALL=1
for a in "$@"; do
  case "$a" in
    -a|--all) ALL=1 ;; -s|--kernel-name) [ $ALL -eq 0 ] && OS=1 ;; -n|--nodename) NODE=1 ;;
    -r|--kernel-release) REL=1 ;; -v|--kernel-version) VER=1 ;; -m|--machine) MACH=1 ;;
    -p|--processor) PROC=1 ;; -o|--operating-system) SHORT=1 ;;
  esac
done
K="Linux"; N="{FAKE_HOSTNAME}"; R="6.8.0-45-generic"
V="#45-Ubuntu SMP PREEMPT_DYNAMIC Fri Aug 30 12:02:04 UTC 2026"; M="x86_64"; P="x86_64"; O="GNU/Linux"
if [ $ALL -eq 1 ]; then echo "$K $N $R $V $M $P $O"; exit 0; fi
out=""
[ $OS -eq 1 ] && out="$K"; [ $NODE -eq 1 ] && out="$out $N"; [ $REL -eq 1 ] && out="$out $R"
[ $VER -eq 1 ] && out="$out $V"; [ $MACH -eq 1 ] && out="$out $M"; [ $PROC -eq 1 ] && out="$out $P"
[ $SHORT -eq 1 ] && out="$out $O"
echo "${{out# }}"
''', mode=0o755)

write(f"{SHIM}/getconf", f'''#!/bin/bash
case "$1" in
  _NPROCESSORS_ONLN|_NPROCESSORS_CONF|NPROCESSORS_ONLN) echo {FAKE_THREADS}; exit 0 ;;
  LEVEL1_DCACHE_SIZE) echo 49152; exit 0 ;;
  LONG_BIT|WORD_BIT) echo 64; exit 0 ;;
esac
exec /usr/bin/getconf "$@"
''', mode=0o755)

# ---------------------------------------------------------------- ② 登录壳:ForceCommand + sftp
write(WRAPPER, f'''#!/bin/bash
# jhy 的沙箱壳。三种入口都要覆盖:
#   1) 交互登录(shell = 本脚本)
#   2) ssh 执行命令:sshd 的 ForceCommand 会把原命令放进 SSH_ORIGINAL_COMMAND
#      —— 不处理这条,`ssh jhy@host 'cat /proc/version'` 就能绕过沙箱
#   3) sftp/scp(内部 sftp-server 也放进沙箱,只能碰自己的家目录)
set -u

SANDBOX=(
  --die-with-parent
  --unshare-user --unshare-pid --unshare-uts --unshare-ipc
  --hostname {FAKE_HOSTNAME}
  --proc /proc
  --ro-bind /opt/jhy-sandbox/fake/cpuinfo /proc/cpuinfo
  --ro-bind /opt/jhy-sandbox/fake/meminfo /proc/meminfo
  --ro-bind /opt/jhy-sandbox/fake/version /proc/version
  --ro-bind /opt/jhy-sandbox/fake/osrelease /proc/sys/kernel/osrelease
  --ro-bind /opt/jhy-sandbox/fake/hostname /proc/sys/kernel/hostname
  --dev-bind /dev /dev
  --ro-bind /usr /usr --ro-bind /lib /lib --ro-bind /lib64 /lib64
  --ro-bind /bin /bin --ro-bind /sbin /sbin
  --ro-bind /opt/jhy-sandbox/opt /opt
  --ro-bind /etc /etc
  --ro-bind /opt/jhy-sandbox/fake/empty /etc/wsl.conf
  --ro-bind /opt/jhy-sandbox/fake/empty /init
  --tmpfs /usr/lib/wsl
  --tmpfs /mnt
  --tmpfs /run
  --bind /home/jhy /home/jhy
  --tmpfs /tmp
  --setenv HOME /home/jhy
  --setenv USER jhy
  --setenv LOGNAME jhy
  --setenv SHELL /bin/bash
  --setenv PATH "/opt/jhy-sandbox/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
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

# 宿主上给 shim 目录做一份"只暴露 shim"的 /opt 视图(bwrap 里 /opt 只挂这一层)
os.makedirs("/opt/jhy-sandbox/opt", exist_ok=True)
for name in ("bin",):
    dst = f"/opt/jhy-sandbox/opt/{name}"
    if os.path.islink(dst) or os.path.exists(dst):
        if os.path.islink(dst):
            os.unlink(dst)
    if not os.path.exists(dst):
        os.symlink(f"/opt/jhy-sandbox/{name}", dst)
print("   /opt/jhy-sandbox/opt/bin -> shim 目录")

# ---------------------------------------------------------------- ③ sshd
MARK = "# --- jhy sandbox (managed) ---"
cfg = io.open("/etc/ssh/sshd_config", encoding="utf-8").read()
if MARK not in cfg:
    with io.open("/etc/ssh/sshd_config", "a", encoding="utf-8") as f:
        f.write(f"""
{MARK}
Match User jhy
    ForceCommand /usr/local/bin/jhy-shell
    AllowTcpForwarding no
    PermitTunnel no
    X11Forwarding no
    AllowAgentForwarding no
    PermitOpen none
""")
    print("   已写入 sshd Match User jhy")
else:
    print("   sshd 配置已存在")

p = subprocess.run(["/usr/sbin/sshd", "-t"], capture_output=True, text=True)
print("   sshd -t:", (p.stderr or p.stdout or "ok").strip()[:200])
if p.returncode == 0:
    subprocess.run(["systemctl", "reload", "ssh"], capture_output=True)
    print("   sshd 已重载")
