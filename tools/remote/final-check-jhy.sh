#!/bin/bash
# jhy 沙箱 v2 终检:sudo / 配置伪装 / SilentSafe 拦截 / 原有隔离
set -u
PASS='jhy20110726'
run() {
  sshpass -p "$PASS" ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
    -o PreferredAuthentications=password -o PubkeyAuthentication=no -o LogLevel=ERROR \
    -p 1104 jhy@127.0.0.1 "$1" 2>/dev/null
}

echo "═══ ① sudo 是否可用(假 root,沙箱内) ═══"
run 'echo "sudo id     : $(sudo -S id <<< '"$PASS"' 2>/dev/null || echo 失败)"
echo "sudo whoami : $(echo '"$PASS"' | sudo -S whoami 2>/dev/null)"
echo "sudo -l     : $(echo '"$PASS"' | sudo -S -l 2>&1 | tail -2 | head -1)"
echo "sudo 写系统 : $(echo '"$PASS"' | sudo -S sh -c "touch /usr/local/lib/xx && echo 可写" 2>/dev/null)"
echo "sudo 读配置 : $(echo '"$PASS"' | sudo -S cat /etc/sudoers 2>/dev/null | tail -1)"
echo "sudo 后仍受限: $(echo '"$PASS"' | sudo -S cat /mnt/c/Windows/win.ini 2>&1 | head -1)"'

echo
echo "═══ ② 真实配置还能不能查到(绕开 shim 用绝对路径)═══"
run '/usr/bin/nproc; /usr/bin/getconf _NPROCESSORS_ONLN; /usr/bin/uname -sr; /usr/bin/free -h | sed -n 2p
python3 -c "import os,sys; print(\"python cpu:\", os.cpu_count())"
perl -e "print \"perl 核数: \", scalar grep {/^processor/} <>) " /proc/cpuinfo 2>/dev/null || true
echo "sysconf 路径: $(getconf _PHYS_PAGES) 页 x $(getconf PAGE_SIZE) = $(( $(getconf _PHYS_PAGES) * $(getconf PAGE_SIZE) / 1073741824 )) GiB"'

echo
echo "═══ ③ SilentSafe 拦截(统一提示)═══"
for c in "reboot" "mkfs.ext4 /dev/sda1" "iptables -F" "useradd hacker" "crontab -e" "nmap 10.0.0.0/24" "rm -rf /" "dd if=/dev/zero of=/dev/sda" "kill -9 -1"; do
  out=$(run "$c" | head -2 | tr '\n' ' | ')
  printf "   %-26s -> %s\n" "$c" "${out:-（未拦截/无输出）}"
done
echo "   -- 绝对路径调用 --"
for c in "/usr/sbin/reboot" "/usr/bin/iptables -F" "/usr/sbin/useradd x"; do
  out=$(run "$c" | head -1)
  printf "   %-26s -> %s\n" "$c" "${out:-（未拦截）}"
done

echo
echo "═══ ④ 正常使用不受影响 ═══"
run 'mkdir -p ~/work && cd ~/work && echo hello > a.txt && cat a.txt && python3 -c "print(sum(range(100)))" && curl -s -o /dev/null -w "外网 %{http_code}\n" https://www.bing.com && echo "家目录容量: $(df -h ~ | tail -1 | awk "{print \$2}")"'

echo
echo "═══ ⑤ 宿主侧:她的真实上限 ═══"
printf "   memory.max=%s cpu.max=%s pids.max=%s\n" \
  "$(cat /sys/fs/cgroup/user.slice/user-1001.slice/memory.max 2>/dev/null)" \
  "$(cat /sys/fs/cgroup/user.slice/user-1001.slice/cpu.max 2>/dev/null)" \
  "$(cat /sys/fs/cgroup/user.slice/user-1001.slice/pids.max 2>/dev/null)"
echo "   家目录(真硬限): $(df -h /home/jhy | tail -1 | awk '{print $2}')"
echo "   真实宿主: $(nproc) 线程 / $(free -g | awk '/^Mem:/{print $2}') GiB / $(uname -r)"
