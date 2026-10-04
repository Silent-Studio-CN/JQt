#!/bin/bash
# 找出沙箱里还可能暴露"这是 WSL/虚拟机"的位置
echo "== 可疑路径是否存在 =="
for p in /sys/class/dmi /sys/hypervisor /proc/xen /sys/firmware/efi /sys/module/hv_vmbus \
         /sys/module/hv_utils /sys/devices/virtual/dmi /proc/cmdline /etc/wsl.conf \
         /usr/lib/wsl /mnt/wsl /init /run/WSL /var/run/WSL; do
  [ -e "$p" ] && echo "   存在: $p"
done

echo
echo "== /sys/module 里的 hv_ / msft 痕迹 =="
ls /sys/module 2>/dev/null | grep -iE '^(hv_|msft|hyperv|vmbus)' | sed 's/^/   /' | head -10

echo
echo "== /proc/cmdline(明晃晃写着 WSL)=="
head -c 220 /proc/cmdline; echo

echo
echo "== /proc 下其它线索 =="
ls /proc | grep -iE 'wsl|lxss|drvfs' | sed 's/^/   /'
head -3 /proc/sys/kernel/version 2>/dev/null | sed 's/^/   kernel.version: /'
head -3 /proc/sys/kernel/ostype /proc/sys/kernel/domainname 2>/dev/null | sed 's/^/   /'
