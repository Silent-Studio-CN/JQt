#!/usr/bin/env python3
"""最终修正:
  ① 拦截器/shim 里的路径写成**沙箱内路径**(/opt/silentsafe、/opt/real)
  ② 危险命令即使系统里没有(iptables/nmap...),也要有 shim -> 统一拦截提示
  ③ 登录时在 tmpfs 里建目录骨架(/usr/local/lib 等),否则假 root 写会 No such file
"""
import io
import os
import shutil
import subprocess

SB = "/opt/jhy-sandbox"
FAKE, BIN, LIB, BLOCK, REAL = f"{SB}/fake", f"{SB}/bin", f"{SB}/lib", f"{SB}/block", f"{SB}/real"
WRAPPER = "/usr/local/bin/jhy-shell"
SB_BLOCK, SB_REAL = "/opt/silentsafe", "/opt/real"   # 沙箱内路径

ALL_BLOCKED = {
    "101": ["mkfs", "mkfs.ext4", "mkfs.xfs", "mkfs.vfat", "mke2fs", "fdisk", "sfdisk", "cfdisk",
            "parted", "sgdisk", "mkswap", "swapon", "swapoff", "fsck", "e2fsck", "tune2fs",
            "resize2fs", "mdadm", "lvcreate", "lvreduce", "pvcreate", "vgcreate", "wipefs", "blkdiscard"],
    "102": ["shutdown", "reboot", "poweroff", "halt", "telinit", "kexec", "insmod", "rmmod",
            "modprobe", "depmod"],
    "103": ["iptables", "ip6tables", "nft", "ufw", "firewall-cmd", "arptables", "ebtables"],
    "104": ["useradd", "userdel", "usermod", "adduser", "deluser", "groupadd", "groupdel",
            "chpasswd", "passwd", "chsh", "visudo", "chage", "gpasswd"],
    "105": ["crontab", "at", "batch", "systemctl", "systemd-run", "timedatectl", "localectl",
            "systemd-nspawn"],
    "106": ["docker", "podman", "lxc-start", "lxc-create", "virt-install", "qemu-system-x86_64"],
    "107": ["wget", "curl-pipe"],       # 见下方特判
    "108": ["nmap", "masscan", "nc", "ncat", "netcat", "socat", "tcpdump", "ettercap", "hping3"],
}

def w(path, content, mode=0o755):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with io.open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    os.chmod(path, mode)


# 统一拦截器(沙箱内路径)
w(f"{BLOCK}/block.sh", f'''#!/bin/bash
# SilentSafe 统一拦截器(提示格式固定)
CODE="${{SS_CODE:-SS_ERR_ID_100}}"
DESC="${{SS_DESC:-危险操作}}"
echo "[SilentSafe]: 您的行为${{DESC}}根据服务器规则配置文件，已经被拦截。" >&2
echo "[SilentSafe]  ErrCode: ${{CODE}}" >&2
exit 1
''')

# ① 覆盖真实二进制(宿主侧生成,绑定进沙箱;脚本内用沙箱路径)
shutil.rmtree(f"{BLOCK}/real", ignore_errors=True)
os.makedirs(f"{BLOCK}/real", exist_ok=True)
manifest = set()
found = 0
for code, names in ALL_BLOCKED.items():
    for n in names:
        if n == "curl-pipe":
            continue
        src = shutil.which(n)
        if src:
            found += 1
            real = os.path.realpath(src)
            w(f"{BLOCK}/real{real}", f'''#!/bin/bash
export SS_CODE="SS_ERR_ID_{code}"
export SS_DESC="执行高危系统管理命令({n})"
exec {SB_BLOCK}/block.sh "$@"
''')
            manifest.add(real)
        # ② 不论是否存在都放一个 PATH shim(不存在的命令也拦)
        w(f"{BIN}/{n}", f'''#!/bin/bash
export SS_CODE="SS_ERR_ID_{code}"
export SS_DESC="执行高危系统管理命令({n})"
exec {SB_BLOCK}/block.sh "$@"
''')

# 需要保留真实程序的 shim(rm/dd/kill 走参数检查)
for n in ("rm", "dd", "kill"):
    src = shutil.which(n)
    if not src:
        continue
    real = os.path.realpath(src)
    if not os.path.exists(f"{REAL}/{n}"):
        shutil.copy2(real, f"{REAL}/{n}")
    w(f"{BIN}/{n}", f'''#!/bin/bash
# 正常用法放行;命中危险用法则拦截
for a in "$@"; do
  case "$a" in
    /|/*|--no-preserve-root|/etc|/usr|/var|/boot|/sys|/proc|/dev|/home)
      export SS_CODE="SS_ERR_ID_100"; export SS_DESC="删除系统关键目录或根目录"
      exec {SB_BLOCK}/block.sh ;;
    of=/dev/*|of=/sys/*|of=/proc/*)
      export SS_CODE="SS_ERR_ID_101"; export SS_DESC="向块设备或内核接口写入数据"
      exec {SB_BLOCK}/block.sh ;;
    -9|-KILL|--signal=KILL) KILLALL=1 ;;
  esac
done
case "$*" in *"-1"*) if [ "${{KILLALL:-0}}" = "1" ]; then
    export SS_CODE="SS_ERR_ID_109"; export SS_DESC="向全部进程发送强制终止信号"
    exec {SB_BLOCK}/block.sh; fi ;; esac
exec {SB_REAL}/{n} "$@"
''')
    w(f"{BLOCK}/real{real}", f'#!/bin/bash\nexec {BIN}/{n} "$@"\n')
    manifest.add(real)

# sudo:覆盖真实 sudo + 沙箱内路径
w(f"{BIN}/sudo", f'''#!/bin/bash
# 沙箱内"假 root":嵌套 user namespace 提升为 uid 0,但仍在沙箱之内
CACHE="$HOME/.cache/.sudo-ts"
case " $* " in
  *" -l "*|*" --list "*)
    echo "Matching Defaults entries for $(id -un) on compute-node-01:"
    echo "    env_reset, mail_badpass, secure_path=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
    echo ""
    echo "User $(id -un) may run the following commands on compute-node-01:"
    echo "    (ALL : ALL) ALL"
    exit 0 ;;
esac
NONINTERACTIVE=0; ARGS=()
for a in "$@"; do
  case "$a" in
    -n|--non-interactive) NONINTERACTIVE=1 ;;
    -S|--stdin|-E|--preserve-env|-H|--set-home|-b|--background) : ;;
    -k|--reset-timestamp) rm -f "$CACHE" ;;
    -v|--validate) mkdir -p "$(dirname "$CACHE")"; date +%s > "$CACHE"; exit 0 ;;
    --) : ;;
    *) ARGS+=("$a") ;;
  esac
done
mkdir -p "$(dirname "$CACHE")"
fresh=0
if [ -f "$CACHE" ]; then
  ts=$(cat "$CACHE" 2>/dev/null || echo 0)
  [ $(( $(date +%s) - ts )) -lt 300 ] && fresh=1
fi
if [ $fresh -eq 0 ]; then
  if [ $NONINTERACTIVE -eq 1 ]; then echo "sudo: a password is required" >&2; exit 1; fi
  t=0
  while [ $t -lt 3 ]; do
    printf "[sudo] password for %s: " "$(id -un)" >&2
    read -rs pw; echo >&2
    if [ -n "$pw" ]; then break; fi
    t=$((t+1)); sleep 1; echo "Sorry, try again." >&2
  done
  [ $t -ge 3 ] && {{ echo "sudo: 3 incorrect password attempts" >&2; exit 1; }}
  date +%s > "$CACHE"
fi
[ ${{#ARGS[@]}} -eq 0 ] && {{ echo "usage: sudo command"; exit 1; }}
exec unshare -Ur --map-root-user -- /bin/bash -c "$(printf '%q ' "${{ARGS[@]}}")"
''')
src = shutil.which("sudo")
if src:
    w(f"{BLOCK}/real{os.path.realpath(src)}", f'#!/bin/bash\nexec {BIN}/sudo "$@"\n')
    manifest.add(os.path.realpath(src))

# ⑧ 登录骨架:tmpfs 里建目录,并把 /opt/real 绑进沙箱
w(f"{SB}/silentsafe/login-init.sh", '''#!/bin/bash
# 沙箱内登录初始化:tmpfs 挂上来的目录原本是空的,补出合理骨架
mkdir -p /usr/local/bin /usr/local/lib /usr/local/share /usr/local/etc 2>/dev/null
mkdir -p /var/log/nginx /var/log/apt /var/log/journal 2>/dev/null
mkdir -p /root/.ssh /root/.config 2>/dev/null
mkdir -p /srv/data /srv/www 2>/dev/null
mkdir -p /opt/tools 2>/dev/null
touch /var/log/syslog /var/log/auth.log /var/log/dpkg.log 2>/dev/null
exit 0
''')

s = io.open(WRAPPER, encoding="utf-8").read()
if SB_REAL not in s:
    s = s.replace(f"  --ro-bind {LIB} {LIB}\n",
                  f"  --ro-bind {LIB} {LIB}\n  --ro-bind {REAL} {SB_REAL}\n")
if "login-init.sh" not in s:
    s = s.replace("  --setenv TERM",
                  f"  --ro-bind {SB}/silentsafe/login-init.sh /opt/login-init.sh\n  --setenv TERM", 1)
    # 启动时先跑骨架脚本
    s = s.replace('-- /bin/bash -l ;;', f'-- /bin/bash -lc "/opt/login-init.sh; exec /bin/bash -l" ;;')
io.open(WRAPPER, "w", encoding="utf-8", newline="\n").write(s)
os.chmod(WRAPPER, 0o755)

with io.open(f"{SB}/silentsafe/bind-list.txt", "w", encoding="utf-8", newline="\n") as f:
    f.write("\n".join(sorted(manifest)) + "\n")
print(f"   覆盖二进制 {len(manifest)} 个;PATH shim 覆盖全部危险命令名")
p = subprocess.run(["bash", "-n", WRAPPER], capture_output=True, text=True)
print("   wrapper 语法:", "通过" if p.returncode == 0 else p.stderr[:300])
