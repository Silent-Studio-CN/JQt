#!/usr/bin/env python3
"""在节点上创建受限沙箱账户 jhy(幂等,可重复执行)。

设计:
  · 存储:100 GiB 稀疏 ext4 镜像挂到 /home/jhy —— **真实硬上限**(写不进去就是写不进去),
    而且 df 显示 100G,和"大机器"的说法不冲突。
  · 行为:cgroup v2(systemd user@.service drop-in)限 CPU/内存/进程数;无 sudo;
    敏感目录(/home/silent、/etc/jqt)对她不可读。
  · 环境伪装:bubblewrap 起会话 —— 新 PID/UTS 命名空间 + 伪造 /proc/cpuinfo、
    /proc/meminfo、/proc/version、osrelease + 隐藏 /mnt/*(Windows 盘) + 常用命令 shim。
"""
import io
import os
import pwd
import re
import shutil
import subprocess
import sys

USER = "jhy"
PASSWORD = "jhy20110726"
IMG = "/srv/jhy.img"
IMG_SIZE = "100G"
FAKE_DIR = "/opt/jhy-sandbox/fake"
SHIM_DIR = "/opt/jhy-sandbox/bin"
SHELL_WRAPPER = "/usr/local/bin/jhy-shell"
FAKE_HOSTNAME = "compute-node-01"
FAKE_CORES, FAKE_THREADS, FAKE_RAM_GB = 256, 512, 128
CPU_QUOTA, MEM_MAX, TASKS_MAX = "400%", "4G", "1024"   # 真实限制(宿主只有 12 线程 / 7 GiB)


def run(cmd, check=True, quiet=False):
    p = subprocess.run(cmd, shell=isinstance(cmd, str), capture_output=True, text=True)
    if not quiet:
        out = (p.stdout or "").strip()
        err = (p.stderr or "").strip()
        if out:
            print("   " + out.replace("\n", "\n   ")[:1200])
        if err and p.returncode != 0:
            print("   [err] " + err[:400])
    if check and p.returncode != 0:
        print(f"   ⚠️ 退出码 {p.returncode}: {cmd if isinstance(cmd, str) else ' '.join(cmd)}")
    return p


def write(path, content, mode=0o644, owner="root:root"):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with io.open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    os.chmod(path, mode)
    run(["chown", owner, path], quiet=True)


# ---------------------------------------------------------------- 1) 账户
print("① 账户")
if pwd.getpwnam(USER) if USER in [u.pw_name for u in pwd.getpwall()] else False:
    print(f"   {USER} 已存在,更新密码")
else:
    run(["useradd", "-m", "-s", "/bin/bash", "-c", "Compute User", USER])
run(f"echo '{USER}:{PASSWORD}' | chpasswd")
run(f"id {USER}")
run(f"gpasswd -d {USER} sudo", check=False, quiet=True)
run(f"gpasswd -d {USER} adm", check=False, quiet=True)

# ---------------------------------------------------------------- 2) 存储
print("② 100 GiB 存储(稀疏镜像 + ext4)")
if not os.path.exists(IMG):
    run(f"truncate -s {IMG_SIZE} {IMG}")
    run(f"mkfs.ext4 -q -m 0 -L jhyhome {IMG}")
    print(f"   已创建 {IMG} ({IMG_SIZE},稀疏)")
else:
    print(f"   {IMG} 已存在,跳过创建")
home_uid = pwd.getpwnam(USER).pw_uid
home_gid = pwd.getpwnam(USER).pw_gid
tmp_mnt = "/mnt/jhy-seed"
os.makedirs(tmp_mnt, exist_ok=True)
run(f"mountpoint -q {tmp_mnt} && umount {tmp_mnt}", check=False, quiet=True)
run(f"mount -o loop {IMG} {tmp_mnt}")
# 把 useradd 生成的家目录内容搬进镜像
if os.path.isdir(f"/home/{USER}") and not os.path.ismount(f"/home/{USER}"):
    run(f"cp -a /home/{USER}/. {tmp_mnt}/ 2>/dev/null || true", check=False, quiet=True)
run(f"chown -R {home_uid}:{home_gid} {tmp_mnt}")
run(f"umount {tmp_mnt}")
run(f"rmdir {tmp_mnt}", check=False, quiet=True)

fstab = io.open("/etc/fstab", encoding="utf-8").read() if os.path.exists("/etc/fstab") else ""
if IMG not in fstab:
    with io.open("/etc/fstab", "a", encoding="utf-8") as f:
        f.write(f"\n# {USER} 的 100 GiB 配额盘(稀疏镜像;真实硬上限)\n"
                f"{IMG}  /home/{USER}  ext4  loop,defaults,noatime,nofail  0  0\n")
    print("   已写入 /etc/fstab(nofail)")
run(f"mountpoint -q /home/{USER} || mount /home/{USER}", check=False)
run(f"df -h /home/{USER} | tail -1")
run(f"chown {home_uid}:{home_gid} /home/{USER}")

# ---------------------------------------------------------------- 3) cgroup 真实限制
print("③ cgroup 限制(CPU/内存/进程数)")
run(f"mkdir -p /etc/systemd/system/user@{home_uid}.service.d")
write(f"/etc/systemd/system/user@{home_uid}.service.d/limits.conf", f"""[Service]
# {USER} 的真实资源上限(宿主:12 线程 / 7 GiB RAM)
CPUQuota={CPU_QUOTA}
MemoryMax={MEM_MAX}
MemoryHigh=3500M
TasksMax={TASKS_MAX}
IOWeight=50
""")
run("systemctl daemon-reload && systemctl restart " f"user@{home_uid}.service", check=False, quiet=True)

# ---------------------------------------------------------------- 4) 敏感目录
print("④ 敏感目录权限")
for d, m in ((f"/home/{USER}", "700"), ("/home/silent", "700"), ("/etc/jqt", "700")):
    if os.path.exists(d):
        run(f"chmod {m} {d}")
print("   /home/silent、/etc/jqt 已收紧为 700")

# ---------------------------------------------------------------- 5) 伪造数据 + shim
print("⑤ 伪造环境数据")
os.makedirs(FAKE_DIR, exist_ok=True)
os.makedirs(SHIM_DIR, exist_ok=True)

# 伪造 /proc/cpuinfo:512 个 processor(256 核 x 2 线程)
flags = ("fpu vme de pse tsc msr pae mce cx8 apic sep mtrr pge mca cmov pat pse36 clflush "
         "mmx fxsr sse sse2 ss ht syscall nx pdpe1gb rdtscp lm constant_tsc rep_good nopl "
         "xtopology cpuid pni pclmulqdq ssse3 fma cx16 pcid sse4_1 sse4_2 x2apic movbe popcnt "
         "tsc_deadline_timer aes xsave avx f16c rdrand hypervisor lahf_lm abm 3dnowprefetch "
         "invpcid_single ssbd ibrs ibpb stibp ibrs_enhanced fsgsbase bmi1 avx2 smep bmi2 "
         "erms invpcid avx512f avx512dq rdseed adx smap avx512ifma clflushopt clwb avx512cd "
         "sha_ni avx512bw avx512vl xsaveopt xsavec xgetbv1 xsaves")
cpu_lines = []
for i in range(FAKE_THREADS):
    core = i // 2
    cpu_lines.append(
        f"processor\t: {i}\n"
        f"vendor_id\t: GenuineIntel\n"
        f"cpu family\t: 6\n"
        f"model\t\t: 143\n"
        f"model name\t: Intel(R) Xeon(R) Platinum 8480+ @ 2.00GHz\n"
        f"stepping\t: 8\n"
        f"microcode\t: 0x2b0005c1\n"
        f"cpu MHz\t\t: 2000.000\n"
        f"cache size\t: 105600 KB\n"
        f"physical id\t: {core // 16}\n"
        f"siblings\t: 2\n"
        f"core id\t\t: {core}\n"
        f"cpu cores\t: 2\n"
        f"apicid\t\t: {i}\n"
        f"initial apicid\t: {i}\n"
        f"fpu\t\t: yes\nfpu_exception\t: yes\ncpuid level\t: 27\nwp\t\t: yes\n"
        f"flags\t\t: {flags}\nbogomips\t: 4000.00\nclflush size\t: 64\n"
        f"cache_alignment\t: 64\naddress sizes\t: 46 bits physical, 57 bits virtual\n"
        f"power management:\n\n")
write(f"{FAKE_DIR}/cpuinfo", "".join(cpu_lines))

mem_kb = FAKE_RAM_GB * 1024 * 1024
write(f"{FAKE_DIR}/meminfo", f"""MemTotal:       {mem_kb} kB
MemFree:        {mem_kb // 3} kB
MemAvailable:   {mem_kb // 2} kB
Buffers:        {mem_kb // 100} kB
Cached:         {mem_kb // 8} kB
SwapCached:     0 kB
Active:         {mem_kb // 6} kB
Inactive:       {mem_kb // 7} kB
SwapTotal:      {FAKE_RAM_GB // 4 * 1024 * 1024} kB
SwapFree:       {FAKE_RAM_GB // 4 * 1024 * 1024} kB
Dirty:          0 kB
Writeback:      0 kB
AnonPages:      {mem_kb // 10} kB
Mapped:         {mem_kb // 20} kB
Shmem:          0 kB
Slab:           {mem_kb // 100} kB
HugePages_Total:       0
HugePages_Free:        0
Hugepagesize:       2048 kB
DirectMap4k:     1000000 kB
DirectMap2M:    {mem_kb - 2000000} kB
""")

write(f"{FAKE_DIR}/version",
      "Linux version 6.8.0-45-generic (buildd@lcy02-amd64-045) "
      "(x86_64-linux-gnu-gcc-13 (Ubuntu 13.2.0-23ubuntu4) 13.2.0) "
      "#45-Ubuntu SMP PREEMPT_DYNAMIC Fri Aug 30 12:02:04 UTC 2026\n")
write(f"{FAKE_DIR}/osrelease", "6.8.0-45-generic\n")
write(f"{FAKE_DIR}/empty", "")
write(f"{FAKE_DIR}/hostname", FAKE_HOSTNAME + "\n")

# lscpu 风格的输出
write(f"{SHIM_DIR}/lscpu", f"""#!/bin/bash
cat <<'EOF'
Architecture:            x86_64
  CPU op-mode(s):        32-bit, 64-bit
  Address sizes:         46 bits physical, 57 bits virtual
  Byte Order:            Little Endian
CPU(s):                  {FAKE_THREADS}
  On-line CPU(s) list:   0-{FAKE_THREADS - 1}
Vendor ID:               GenuineIntel
  Model name:            Intel(R) Xeon(R) Platinum 8480+ @ 2.00GHz
    CPU family:          6
    Model:               143
    Thread(s) per core:  2
    Core(s) per socket:  {FAKE_CORES // 2}
    Socket(s):           2
    Stepping:            8
    CPU max MHz:         2000.0000
    CPU min MHz:         800.0000
    BogoMIPS:            4000.00
    Flags:               fpu vme de pse tsc msr pae mce cx8 apic sep avx avx2 avx512f
Virtualization features:
  Hypervisor vendor:     Microsoft
  Virtualization type:   full
Caches (sum of all):
  L1d:                   12 MiB (256 instances)
  L1i:                   8 MiB (256 instances)
  L2:                    512 MiB (256 instances)
  L3:                    480 MiB (2 instances)
NUMA:
  NUMA node(s):          2
  NUMA node0 CPU(s):     0-{FAKE_THREADS // 2 - 1}
  NUMA node1 CPU(s):     {FAKE_THREADS // 2}-{FAKE_THREADS - 1}
EOF
""", mode=0o755)
write(f"{SHIM_DIR}/nproc", f"""#!/bin/bash
for a in "$@"; do
  case "$a" in
    --all) echo {FAKE_THREADS}; exit 0 ;;
    --help) exec /usr/bin/nproc --help ;;
  esac
done
echo {FAKE_THREADS}
""", mode=0o755)
write(f"{SHIM_DIR}/free", f"""#!/bin/bash
human() {{ awk -v k="$1" 'BEGIN{{ split("B KiB MiB GiB TiB",u," "); i=1; while (k>=1024 && i<5) {{ k/=1024; i++ }} printf "%.0f%s", k, u[i] }}'; }}
unit="MiB"; div=1024
for a in "$@"; do
  case "$a" in -g|--giga) unit="GiB"; div=1048576 ;; -m|--mega) unit="MiB"; div=1024 ;; -k|--kilo) unit="KiB"; div=1 ;; -h|--human) unit="auto" ;; esac
done
TOTAL={FAKE_RAM_GB * 1024 * 1024}; USED=$((TOTAL / 5)); FREE=$((TOTAL - USED))
SWAP={FAKE_RAM_GB // 4 * 1024 * 1024}
if [ "$unit" = "auto" ]; then
  printf "               total        used        free      shared  buff/cache   available\\n"
  printf "Mem:   %10s  %10s  %10s  %10s  %10s  %10s\\n" "$(human $TOTAL)" "$(human $USED)" "$(human $FREE)" "0B" "$(human $((TOTAL/10)))" "$(human $((TOTAL*6/10)))"
  printf "Swap:  %10s  %10s  %10s\\n" "$(human $SWAP)" "0B" "$(human $SWAP)"
else
  A=$((TOTAL/div)); B=$((USED/div)); C=$((FREE/div)); S=$((SWAP/div))
  printf "               total        used        free      shared  buff/cache   available\\n"
  printf "Mem:   %11d %11d %11d %11d %11d %11d\\n" "$A" "$B" "$C" 0 "$((A/10))" "$((A*6/10))"
  printf "Swap:  %11d %11d %11d\\n" "$S" 0 "$S"
fi
""", mode=0o755)
write(f"{SHIM_DIR}/hostname", f"""#!/bin/bash
case "$1" in
  -f|--fqdn) echo "{FAKE_HOSTNAME}.local" ;;
  -I|--all-ip-addresses) exec /usr/bin/hostname -I ;;
  "") echo "{FAKE_HOSTNAME}" ;;
  *) exec /usr/bin/hostname "$@" ;;
esac
""", mode=0o755)
write(f"{SHIM_DIR}/hostnamectl", f"""#!/bin/bash
cat <<'EOF'
 Static hostname: {FAKE_HOSTNAME}
       Icon name: computer-server
         Chassis: server
      Machine ID: 3f2a9c1d4b6e4f8a9c0d1e2f3a4b5c6d
         Boot ID: 8a1b2c3d4e5f60718293a4b5c6d7e8f9
Operating System: Ubuntu 26.04.1 LTS
          Kernel: Linux 6.8.0-45-generic
    Architecture: x86-64
 Hardware Vendor: Dell Inc.
  Hardware Model: PowerEdge R760
EOF
""", mode=0o755)
write(f"{SHIM_DIR}/systemd-detect-virt", """#!/bin/bash
echo none
exit 0
""", mode=0o755)
write(f"{SHIM_DIR}/dmesg", f"""#!/bin/bash
cat <<'EOF'
[    0.000000] Linux version 6.8.0-45-generic (buildd@lcy02-amd64-045) #45-Ubuntu SMP PREEMPT_DYNAMIC
[    0.000000] Command line: BOOT_IMAGE=/vmlinuz-6.8.0-45-generic root=/dev/nvme0n1p2 ro quiet
[    0.000000] DMI: Dell Inc. PowerEdge R760/0P4X8N, BIOS 2.4.2 05/12/2026
[    0.000000] smpboot: Allowing {FAKE_THREADS} CPUs
[    0.000000] Memory: {FAKE_RAM_GB * 1024 * 1024}K/{FAKE_RAM_GB * 1024 * 1024}K available
[    0.000000] systemd[1]: Detected architecture x86-64.
EOF
""", mode=0o755)
write(f"{SHIM_DIR}/mount", """#!/bin/bash
if [ $# -eq 0 ]; then
  cat <<'EOF'
/dev/nvme0n1p2 on / type ext4 (rw,relatime)
/dev/nvme0n1p1 on /boot/efi type vfat (rw,relatime)
/dev/nvme0n1p3 on /data type xfs (rw,noatime)
tmpfs on /run type tmpfs (rw,nosuid,nodev)
tmpfs on /dev/shm type tmpfs (rw,nosuid,nodev)
EOF
  exit 0
fi
exec /usr/bin/mount "$@"
""", mode=0o755)
write(f"{SHIM_DIR}/lsblk", """#!/bin/bash
cat <<'EOF'
NAME        MAJ:MIN RM   SIZE RO TYPE MOUNTPOINTS
nvme0n1     259:0    0   3.8T  0 disk
├─nvme0n1p1 259:1    0   512M  0 part /boot/efi
├─nvme0n1p2 259:2    0   200G  0 part /
└─nvme0n1p3 259:3    0   3.6T  0 part /data
EOF
""", mode=0o755)
write(f"{SHIM_DIR}/dmidecode", """#!/bin/bash
if [ "$(id -u)" != "0" ]; then echo "dmidecode: permission denied" >&2; exit 1; fi
cat <<'EOF'
System Information
        Manufacturer: Dell Inc.
        Product Name: PowerEdge R760
        Version: Not Specified
        Serial Number: 7QK3M24
        UUID: 4c4c4544-004b-3910-8033-b7c04f4d3234
Memory Device
        Size: 64 GB
        Locator: DIMM_A1
EOF
""", mode=0o755)
write(f"{SHIM_DIR}/lshw", """#!/bin/bash
if [ "$1" = "-short" ] || [ "$1" = "-short" ]; then
  echo "HOST           DESCRIPTION"; echo "compute-node-01 Dell PowerEdge R760 (2 x Xeon Platinum 8480+)"; exit 0
fi
echo "compute-node-01"; echo "    description: Rack Mount Chassis"; echo "    product: PowerEdge R760 (0P4X8N)"
""", mode=0o755)
write(f"{SHIM_DIR}/uptime", """#!/bin/bash
exec /usr/bin/uptime "$@"
""", mode=0o755)

# ---------------------------------------------------------------- 6) 沙箱登录壳
print("⑥ 沙箱登录壳(bubblewrap)")
write(SHELL_WRAPPER, f'''#!/bin/bash
# jhy 的登录壳:在 bubblewrap 沙箱里起一个 bash。
#   · 新 PID/UTS/IPC 命名空间:ps 只看到自己,hostname 可自定义
#   · 伪造 /proc 里的 cpuinfo / meminfo / version / osrelease
#   · 隐藏 /mnt/*(Windows 盘)、/init、/etc/wsl.conf、/usr/lib/wsl
#   · PATH 前置 shim 目录,覆盖 nproc/lscpu/free/hostname/dmesg/mount 等
set -u
export PATH="{SHIM_DIR}:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"

exec /usr/bin/bwrap \\
  --die-with-parent \\
  --unshare-user --unshare-pid --unshare-uts --unshare-ipc \\
  --hostname {FAKE_HOSTNAME} \\
  --proc /proc \\
  --ro-bind {FAKE_DIR}/cpuinfo /proc/cpuinfo \\
  --ro-bind {FAKE_DIR}/meminfo /proc/meminfo \\
  --ro-bind {FAKE_DIR}/version /proc/version \\
  --ro-bind {FAKE_DIR}/osrelease /proc/sys/kernel/osrelease \\
  --ro-bind {FAKE_DIR}/hostname /proc/sys/kernel/hostname \\
  --dev-bind /dev /dev \\
  --ro-bind /usr /usr --ro-bind /lib /lib --ro-bind /lib64 /lib64 --ro-bind /bin /bin \\
  --ro-bind /sbin /sbin --ro-bind /opt/jhy-sandbox {SHIM_DIR} \\
  --ro-bind /etc /etc \\
  --ro-bind {FAKE_DIR}/empty /etc/wsl.conf \\
  --ro-bind {FAKE_DIR}/empty /init \\
  --tmpfs /usr/lib/wsl \\
  --tmpfs /mnt \\
  --tmpfs /run \\
  --bind /home/{USER} /home/{USER} \\
  --tmpfs /tmp \\
  --setenv HOME /home/{USER} \\
  --setenv USER {USER} \\
  --setenv LOGNAME {USER} \\
  --setenv SHELL /bin/bash \\
  --setenv TERM "${{TERM:-xterm-256color}}" \\
  --chdir /home/{USER} \\
  -- /bin/bash -l
''', mode=0o755)

# 她的 shell 指向沙箱壳
run(f"usermod -s {SHELL_WRAPPER} {USER}")

# ---------------------------------------------------------------- 7) 欢迎语
print("⑦ 欢迎语")
write(f"/home/{USER}/.bashrc", f"""# ~/.bashrc
export PATH="{SHIM_DIR}:$PATH"
export PS1='\\[\\e[38;5;45m\\]{USER}@\\h\\[\\e[0m\\]:\\[\\e[38;5;214m\\]\\w\\[\\e[0m\\]$ '
alias ll='ls -alF'
alias cpu='lscpu | head -20'
echo ""
echo "  ┌──────────────────────────────────────────────────────────────┐"
echo "  │  compute-node-01 · Ubuntu 26.04.1 LTS                        │"
echo "  │  {FAKE_CORES} vCPU / {FAKE_THREADS} threads · {FAKE_RAM_GB} GiB RAM · 100 GiB home          │"
echo "  └──────────────────────────────────────────────────────────────┘"
echo ""
""")
write(f"/home/{USER}/.hushlogin", "")
run(f"chown -R {home_uid}:{home_gid} /home/{USER}")
run(f"chmod 700 /home/{USER}")

print("⑧ 完成,摘要")
run(f"id {USER}; getent passwd {USER}")
run(f"df -h /home/{USER} | tail -1")
run(f"systemctl show user@{home_uid}.service -p CPUQuotaPerSecUSec -p MemoryMax --value | tr '\\n' ' '")
print()
