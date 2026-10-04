#!/usr/bin/env python3
"""补 sched_getaffinity 覆盖 + 用独立脚本做沙箱内验证(避免 Python 内嵌 shell 引号地狱)"""
import io
import os
import subprocess

LIB = "/opt/jhy-sandbox/lib"
THREADS = 512
SB = "/opt/jhy-sandbox"

src = io.open(f"{LIB}/fakehw.c", encoding="utf-8").read()
if "sched_getaffinity" not in src:
    src = src.replace("#include <unistd.h>", "#include <unistd.h>\n#include <sched.h>")
    src += f'''
/* coreutils 的 nproc 走 sched_getaffinity(),不是 get_nprocs —— 漏了它
   /usr/bin/nproc 仍会吐真实核数(实测踩过) */
typedef int (*sched_getaffinity_t)(pid_t, size_t, cpu_set_t *);

int sched_getaffinity(pid_t pid, size_t cpusetsize, cpu_set_t *mask) {{
    static sched_getaffinity_t real = NULL;
    if (!real) real = (sched_getaffinity_t)dlsym(RTLD_NEXT, "sched_getaffinity");
    if (mask && cpusetsize) {{
        CPU_ZERO_S(cpusetsize, mask);
        for (int i = 0; i < {THREADS} && i < (int)(cpusetsize * 8); i++)
            CPU_SET_S(i, cpusetsize, mask);
        return 0;
    }}
    return real ? real(pid, cpusetsize, mask) : -1;
}}

int sched_setaffinity(pid_t pid, size_t cpusetsize, const cpu_set_t *mask) {{
    (void)pid; (void)cpusetsize; (void)mask;
    return 0;
}}
'''
    io.open(f"{LIB}/fakehw.c", "w", encoding="utf-8", newline="\n").write(src)
    print("   已补 sched_getaffinity / sched_setaffinity")

p = subprocess.run(["gcc", "-O2", "-shared", "-fPIC", "-o", f"{LIB}/fakehw.so", f"{LIB}/fakehw.c",
                    "-ldl"], capture_output=True, text=True)
print("   编译:", "成功" if p.returncode == 0 else p.stderr[:400])

# 沙箱内验证脚本(放在节点上执行,避免内嵌引号问题)
CHECK = r'''#!/bin/bash
echo "nproc 绝对路径 = $(/usr/bin/nproc)   nproc(shim) = $(nproc)"
echo "getconf        = $(getconf _NPROCESSORS_ONLN)   python = $(python3 -c 'import os;print(os.cpu_count())')"
echo "free/内存      = $(free -h | sed -n 2p | tr -s ' ')"
echo
echo "== SilentSafe 拦截(stderr 已合并)=="
for c in reboot "mkfs.ext4 /dev/sda1" iptables "useradd x" "crontab -e" "rm -rf /" "dd if=/dev/zero of=/dev/sda" "nmap 10.0.0.0/24"; do
  out=$($c 2>&1 | tr '\n' '|')
  printf "  %-26s -> %s\n" "$c" "${out:-（未拦截）}"
done
printf "  %-26s -> %s\n" "/usr/sbin/reboot" "$(/usr/sbin/reboot 2>&1 | tr '\n' '|')"
printf "  %-26s -> %s\n" "/usr/bin/iptables -F" "$(/usr/bin/iptables -F 2>&1 | tr '\n' '|')"
printf "  %-26s -> %s\n" "sudo reboot" "$(echo jhy20110726 | sudo -S reboot 2>&1 | tr '\n' '|')"
echo
echo "== 假 root 的写权限 =="
echo "  /usr/local/lib : $(echo jhy20110726 | sudo -S touch /usr/local/lib/ok 2>&1 && echo 成功)"
echo "  /srv/www       : $(echo jhy20110726 | sudo -S mkdir -p /srv/www 2>&1 && echo 成功)"
echo "  /etc/passwd 追加: $(echo jhy20110726 | sudo -S sh -c 'echo x >> /etc/passwd' 2>&1 && echo 成功)"
echo
echo "== 真实配置(绝对路径 + 假 root)=="
echo "  uname        : $(/usr/bin/uname -sr)"
echo "  phys pages   : $(getconf _PHYS_PAGES) 页 → $(( $(getconf _PHYS_PAGES) * 4096 / 1073741824 )) GiB"
echo "  cpuinfo      : $(grep -c ^processor /proc/cpuinfo) 条"
echo "  cmdline      : $(cut -c1-50 /proc/cmdline)"
echo "  mnt          : [$(ls -A /mnt | tr '\n' ' ')]"
'''
io.open(f"{SB}/silentsafe/sandbox-check.sh", "w", encoding="utf-8", newline="\n").write(CHECK)
os.chmod(f"{SB}/silentsafe/sandbox-check.sh", 0o755)
print("   验证脚本已就位")
