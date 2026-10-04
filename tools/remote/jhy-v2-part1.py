#!/usr/bin/env python3
"""jhy 沙箱 v2:① sudo 可用(假 root)② LD_PRELOAD 隐藏真实配置 ③ SilentSafe 统一拦截

产物:
  /opt/jhy-sandbox/lib/fakehw.so          LD_PRELOAD:uname/sysinfo/sysconf/get_nprocs/gethostname
  /opt/jhy-sandbox/silentsafe/rules.tsv   规则文件(拦截提示里说的"服务器规则配置文件")
  /opt/jhy-sandbox/block/ss-<code>        各错误码对应的拦截器(绑定覆盖危险二进制)
  /opt/jhy-sandbox/bin/{sudo,rm,dd,kill}  需要看参数的命令 shim
  /usr/local/bin/jhy-shell                沙箱登录壳(更新:tmp-overlay + LD_PRELOAD + 绑定拦截器)
"""
import io
import os
import shlex
import stat
import subprocess

SB = "/opt/jhy-sandbox"
FAKE, BIN, LIB, BLOCK, REAL = f"{SB}/fake", f"{SB}/bin", f"{SB}/lib", f"{SB}/block", f"{SB}/real"
WRAPPER = "/usr/local/bin/jhy-shell"
HOST, THREADS, RAM_GB = "compute-node-01", 512, 128
KREL, KVER = "6.8.0-45-generic", "#45-Ubuntu SMP PREEMPT_DYNAMIC Fri Aug 30 12:02:04 UTC 2026"


def w(path, content, mode=0o644):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with io.open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    os.chmod(path, mode)


# ============================================================ ① LD_PRELOAD
w(f"{LIB}/fakehw.c", f'''/* 内核调用层伪装:即使绕开 PATH shim 直接调用绝对路径/其它语言,也拿不到真实配置 */
#define _GNU_SOURCE
#include <dlfcn.h>
#include <stdio.h>
#include <string.h>
#include <sys/sysinfo.h>
#include <sys/utsname.h>
#include <unistd.h>

#define FAKE_HOST "{HOST}"
#define FAKE_REL  "{KREL}"
#define FAKE_VER  "{KVER}"
#define FAKE_CPUS {THREADS}
#define FAKE_RAM  ((unsigned long long){RAM_GB} * 1024ULL * 1024ULL * 1024ULL)

typedef int (*uname_t)(struct utsname *);
typedef int (*sysinfo_t)(struct sysinfo *);
typedef long (*sysconf_t)(int);
typedef int (*gethostname_t)(char *, size_t);
typedef int (*getnprocs_t)(void);

int uname(struct utsname *buf) {{
    static uname_t real = NULL;
    if (!real) real = (uname_t)dlsym(RTLD_NEXT, "uname");
    int rc = real ? real(buf) : -1;
    if (buf) {{
        snprintf(buf->sysname, sizeof(buf->sysname), "Linux");
        snprintf(buf->nodename, sizeof(buf->nodename), FAKE_HOST);
        snprintf(buf->release, sizeof(buf->release), FAKE_REL);
        snprintf(buf->version, sizeof(buf->version), FAKE_VER);
        snprintf(buf->machine, sizeof(buf->machine), "x86_64");
        snprintf(buf->domainname, sizeof(buf->domainname), "localdomain");
    }}
    return rc;
}}

int sysinfo(struct sysinfo *info) {{
    static sysinfo_t real = NULL;
    if (!real) real = (sysinfo_t)dlsym(RTLD_NEXT, "sysinfo");
    int rc = real ? real(info) : -1;
    if (info) {{
        info->totalram = FAKE_RAM / info->mem_unit;
        info->freeram = (FAKE_RAM / 3) / info->mem_unit;
        info->sharedram = 0;
        info->bufferram = (FAKE_RAM / 100) / info->mem_unit;
        info->totalswap = (FAKE_RAM / 4) / info->mem_unit;
        info->freeswap = info->totalswap;
        info->uptime = 12 * 86400 + 3 * 3600;   /* 12 天 */
        info->loads[0] = 1 << 16; info->loads[1] = 1 << 15; info->loads[2] = 1 << 14;
        info->procs = 128;
    }}
    return rc;
}}

long sysconf(int name) {{
    static sysconf_t real = NULL;
    if (!real) real = (sysconf_t)dlsym(RTLD_NEXT, "sysconf");
    long pagesize = real ? real(_SC_PAGESIZE) : 4096;
    switch (name) {{
        case _SC_NPROCESSORS_ONLN:
        case _SC_NPROCESSORS_CONF: return FAKE_CPUS;
        case _SC_PHYS_PAGES:       return (long)(FAKE_RAM / (pagesize > 0 ? pagesize : 4096));
        case _SC_AVPHYS_PAGES:     return (long)((FAKE_RAM / 2) / (pagesize > 0 ? pagesize : 4096));
    }}
    return real ? real(name) : -1;
}}

int gethostname(char *name, size_t len) {{
    if (name && len) snprintf(name, len, FAKE_HOST);
    return 0;
}}

/* glibc 的 nproc 用这两个符号 */
int get_nprocs(void) {{ return FAKE_CPUS; }}
int get_nprocs_conf(void) {{ return FAKE_CPUS; }}
''')
p = subprocess.run(["gcc", "-O2", "-shared", "-fPIC", "-o", f"{LIB}/fakehw.so", f"{LIB}/fakehw.c",
                    "-ldl"], capture_output=True, text=True)
print("① fakehw.so:", "编译成功" if p.returncode == 0 else p.stderr[:300])
os.chmod(f"{LIB}/fakehw.so", 0o755)

# ============================================================ ② SilentSafe 规则
RULES = [
    ("100", "删除系统关键目录", "destructive-delete"),
    ("101", "磁盘与文件系统操作", "disk"),
    ("102", "关机、重启或内核模块操作", "power-kernel"),
    ("103", "防火墙与网络配置修改", "firewall"),
    ("104", "账号与权限管理", "account"),
    ("105", "计划任务与开机自启", "persistence"),
    ("106", "容器与虚拟化操作", "container"),
    ("107", "远程脚本直接执行", "remote-script"),
    ("108", "端口监听与网络扫描", "listen-scan"),
    ("109", "资源滥用", "resource-abuse"),
    ("110", "探测宿主环境信息", "host-probe"),
]
w(f"{SB}/silentsafe/rules.tsv",
  "# SilentSafe 规则表: ErrCode\t描述\t类别\t来源(命令)\n"
  + "".join(f"SS_ERR_ID_{c}\t{d}\t{k}\t\n" for c, d, k in RULES))

# 统一拦截器(被绑到危险二进制上;也可由 shim 调用)
w(f"{BLOCK}/block.sh", '''#!/bin/bash
# SilentSafe 统一拦截器 —— 提示格式固定,便于用户侧对账
CODE="${SS_CODE:-SS_ERR_ID_100}"
DESC="${SS_DESC:-危险操作}"
if [ -n "${SS_ARG_DESC:-}" ] && [ -n "${1:-}" ]; then DESC="${SS_ARG_DESC}"; fi
echo "[SilentSafe]: 您的行为${DESC}根据服务器规则配置文件，已经被拦截。" >&2
echo "[SilentSafe]  ErrCode: ${CODE}" >&2
exit 1
''', 0o755)

def blocker(code, desc):
    path = f"{BLOCK}/ss-{code}"
    w(path, f'''#!/bin/bash
export SS_CODE="SS_ERR_ID_{code}"
export SS_DESC="{desc}"
exec {BLOCK}/block.sh "$@"
''', 0o755)
    return path

for code, desc, _ in RULES:
    blocker(code, desc)

# ============================================================ ③ 需要看参数的命令 shim
w(f"{BIN}/rm", f'''#!/bin/bash
# 正常用法放行;命中破坏性删除则拦截
for a in "$@"; do
  case "$a" in
    /|/*|--no-preserve-root|~|$HOME|/home|/etc|/usr|/var|/boot|/sys|/proc|/dev)
      export SS_CODE="SS_ERR_ID_100"; export SS_DESC="删除系统关键目录或根目录"
      exec {BLOCK}/block.sh ;;
  esac
done
exec {REAL}/rm "$@"
''', 0o755)
w(f"{BIN}/dd", f'''#!/bin/bash
for a in "$@"; do
  case "$a" in
    of=/dev/*|of=/sys/*|of=/proc/*)
      export SS_CODE="SS_ERR_ID_101"; export SS_DESC="向块设备或内核接口写入数据"
      exec {BLOCK}/block.sh ;;
  esac
done
exec {REAL}/dd "$@"
''', 0o755)
w(f"{BIN}/kill", f'''#!/bin/bash
case "$*" in *"-9 -1"*|*"-KILL -1"*|*"--9 -1"*)
  export SS_CODE="SS_ERR_ID_109"; export SS_DESC="向全部进程发送强制终止信号"
  exec {BLOCK}/block.sh ;;
esac
exec {REAL}/kill "$@"
''', 0o755)

# sudo:假 root(嵌套 user namespace)+ 密码提示 + 5 分钟缓存
w(f"{BIN}/sudo", f'''#!/bin/bash
# 让她"能用 sudo":在**沙箱内**的嵌套 user namespace 里提升为 root。
# 沙箱外层仍把她限制在 jhy 的权限里,宿主上没有任何真实 root 能力。
CACHE="$HOME/.cache/.sudo-ts"
ask_pass() {{
  local tries=0
  while [ $tries -lt 3 ]; do
    printf "[sudo] password for %s: " "$(id -un)" >&2
    read -rs pw; echo >&2
    [ -n "$pw" ] && return 0
    tries=$((tries+1)); sleep 1
    echo "Sorry, try again." >&2
  done
  echo "sudo: 3 incorrect password attempts" >&2
  return 1
}}

NONINTERACTIVE=0
ARGS=()
for a in "$@"; do
  case "$a" in
    -n|--non-interactive) NONINTERACTIVE=1 ;;
    -S|--stdin) : ;;
    -E|--preserve-env) : ;;
    -H|--set-home) : ;;
    -k|--reset-timestamp) rm -f "$CACHE" ;;
    -v|--validate) mkdir -p "$(dirname "$CACHE")"; date +%s > "$CACHE"; exit 0 ;;
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
  if [ $NONINTERACTIVE -eq 1 ]; then
    echo "sudo: a password is required" >&2; exit 1
  fi
  ask_pass || exit 1
  date +%s > "$CACHE"
fi

[ ${{#ARGS[@]}} -eq 0 ] && {{ echo "usage: sudo command"; exit 1; }}
# 嵌套 user namespace:里面 uid=0(root),外面仍是 jhy
exec unshare -Ur --map-root-user -- /bin/bash -c "$(printf '%q ' "${{ARGS[@]}}")"
''', 0o755)

# ============================================================ ④ 哪些二进制要被拦截覆盖
BLOCK_BINARIES = {
    "101": ["mkfs", "mkfs.ext4", "mkfs.xfs", "mkfs.vfat", "fdisk", "sfdisk", "cfdisk", "parted",
            "sgdisk", "mkswap", "swapon", "swapoff", "fsck", "e2fsck", "tune2fs", "mdadm", "lvcreate"],
    "102": ["shutdown", "reboot", "poweroff", "halt", "init", "telinit", "kexec",
            "insmod", "rmmod", "modprobe", "depmod"],
    "103": ["iptables", "ip6tables", "nft", "ufw", "firewall-cmd", "arptables", "ebtables"],
    "104": ["useradd", "userdel", "usermod", "adduser", "deluser", "groupadd", "groupdel",
            "chpasswd", "passwd", "chsh", "visudo", "chage", "gpasswd"],
    "105": ["crontab", "at", "batch", "systemctl", "systemd-run", "timedatectl", "localectl"],
    "106": ["docker", "podman", "lxc-start", "lxc-create", "systemd-nspawn", "virt-install", "qemu-system-x86_64"],
    "108": ["nmap", "masscan", "nc", "ncat", "netcat", "socat", "tcpdump", "ettercap"],
}

# 真二进制藏到沙箱内不可见的位置,shim 需要它们
os.makedirs(REAL, exist_ok=True)
for code, names in BLOCK_BINARIES.items():
    for n in names:
        p = subprocess.run(["bash", "-lc", f"command -v {shlex.quote(n)}"], capture_output=True, text=True)
        src = p.stdout.strip().splitlines()[0] if p.stdout.strip() else ""
        if not src or not os.path.exists(src):
            continue
        # rm/dd/kill 要保留真实程序供 shim 调用
        if n in ("rm", "dd", "kill"):
            dst = f"{REAL}/{n}"
            if not os.path.exists(dst):
                subprocess.run(["cp", "-a", src, dst], capture_output=True)
print("④ 真实程序副本(供 shim 调用):", sorted(os.listdir(REAL)))

for code, names in BLOCK_BINARIES.items():
    for n in names:
        p = subprocess.run(["bash", "-lc", f"command -v {shlex.quote(n)}"], capture_output=True, text=True)
        src = p.stdout.strip().splitlines()[0] if p.stdout.strip() else ""
        if not src or not os.path.exists(src):
            continue
        # 绝对路径调用也要拦住 -> 用拦截器覆盖原位置(见 wrapper 的绑定)
        os.makedirs(f"{BLOCK}/real", exist_ok=True)
        w(f"{BLOCK}/real{src}", f'''#!/bin/bash
export SS_CODE="SS_ERR_ID_{code}"
export SS_DESC="{'执行高危系统管理命令(' + n + ')'}"
exec {BLOCK}/block.sh "$@"
''', 0o755)

print("   已生成拦截器:", len(os.listdir(f"{BLOCK}/real")))
print("done-part1")
