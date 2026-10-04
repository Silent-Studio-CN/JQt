#!/bin/bash
# jhy 沙箱最终体检(第五轮后)
set -u
sudo -u jhy -H /usr/local/bin/jhy-shell <<'EOS' 2>&1 | sed 's/^/   /'
echo "── CPU / 内存 ──"
printf "nproc=%-4s cpuinfo=%-4s sys/online=%-7s stat=%-4s lscpu=%s\n" \
  "$(nproc)" "$(grep -c ^processor /proc/cpuinfo)" "$(cat /sys/devices/system/cpu/online)" \
  "$(grep -c '^cpu[0-9]' /proc/stat)" "$(lscpu | awk -F: '/^CPU\(s\)/{print $2}' | xargs)"
free -h | sed -n '1,3p'
echo "meminfo: $(head -1 /proc/meminfo)"
echo "── 内核 / 虚拟化 / 模块 ──"
uname -sr; echo "version: $(cut -c1-50 /proc/version)"; echo "virt: $(systemd-detect-virt)"
echo "lsmod 里 hv/hyperv: [$(lsmod 2>/dev/null | grep -cE 'hv_|hyperv')] 条; 模块总数 $(lsmod 2>/dev/null | tail -n +2 | wc -l)"
echo "── 磁盘 ──"
df -h | sed -n '2p;5p'; echo "df -h ~ : $(df -h ~ | tail -1)"
echo "── 网络 ──"
echo "resolv : $(head -1 /etc/resolv.conf)"
timeout 8 curl -s -o /dev/null -w "外网   : HTTP %{http_code}\n" https://www.bing.com || echo "外网   : 失败"
timeout 8 getent hosts archive.ubuntu.com >/dev/null && echo "DNS    : 正常" || echo "DNS    : 失败"
echo "── 痕迹 ──"
echo "/run/WSL: [$(ls -A /run/WSL 2>/dev/null | head -2 | tr '\n' ' ')]  /mnt: [$(ls -A /mnt | tr '\n' ' ')]"
echo "── 权限 ──"
sudo -n true 2>&1 | head -1
echo "写 /usr: $(touch /usr/zz 2>&1 | head -1)"
echo "── 资源限制(沙箱内看的还是真实 cgroup)──"
echo "cgroup: $(cat /proc/self/cgroup | head -1)"
EOS
echo
echo "== 宿主上核对她的 cgroup 上限 =="
sudo systemctl show user@1001.service -p CPUQuotaPerSecUSec -p MemoryMax -p TasksMax 2>/dev/null | sed 's/^/   /'
echo "== 家目录配额(真硬限) =="
df -h /home/jhy | tail -1 | sed 's/^/   /'
