#!/bin/bash
# 建 jhy 账户前的环境侦察
echo "== 真实规格(仅我们可见)=="
echo "   CPU 物理: $(nproc) 线程; 型号: $(grep -m1 'model name' /proc/cpuinfo | cut -d: -f2- | xargs)"
echo "   内存: $(free -g | awk '/^Mem:/{print $2}') GiB; 磁盘: $(df -h / | awk 'NR==2{print $2" 总 / "$4" 可用"}')"
echo "   内核: $(uname -r)"
echo "   虚拟化: $(systemd-detect-virt 2>/dev/null || echo '-')"

echo
echo "== WSL 相关痕迹(她的账号可能看到的)=="
for p in /init /etc/wsl.conf /usr/lib/wsl /mnt/wsl /mnt/c /mnt/d /proc/version /proc/sys/kernel/osrelease; do
  [ -e "$p" ] && printf "   %-28s 存在 (%s)\n" "$p" "$(stat -c '%A %U' "$p" 2>/dev/null)"
done
echo "   os-release: $(grep -m1 PRETTY_NAME /etc/os-release)"
echo "   hostname: $(hostname)"

echo
echo "== 隔离手段可用性 =="
echo -n "   非特权 user namespace: "
if unshare -Urm true 2>/dev/null; then echo "可用 ✅"; else echo "不可用 ❌"; fi
echo -n "   user ns + pid + mount-proc: "
if unshare -Urmpf --mount-proc true 2>/dev/null; then echo "可用 ✅"; else echo "不可用 ❌"; fi
echo -n "   bwrap(bubblewrap): "; command -v bwrap || echo "未安装"
echo -n "   mkfs.ext4: "; command -v mkfs.ext4 || echo "无"
echo -n "   quota 工具: "; command -v setquota || echo "无"
echo -n "   cgroup2: "; [ -f /sys/fs/cgroup/cgroup.controllers ] && echo "已挂载($(cat /sys/fs/cgroup/cgroup.controllers))" || echo "未挂载"
echo -n "   systemd user slice 支持: "; systemctl show user.slice -p MemoryMax --value 2>/dev/null | head -1

echo
echo "== 现有账号 =="
awk -F: '$3>=1000 && $3<65534 {print "   "$1" uid="$3" home="$6" shell="$7}' /etc/passwd
echo "   wheel/sudo 组: $(getent group sudo | cut -d: -f4)"

echo
echo "== 是否已有 jhy =="
id jhy 2>/dev/null || echo "   尚未创建"
