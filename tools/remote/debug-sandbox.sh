#!/bin/bash
# 诊断沙箱为什么起不来
echo "== 直接跑 wrapper(显示 stderr)=="
sudo -u jhy -H /usr/local/bin/jhy-shell </dev/null 2>&1 | head -6

echo
echo "== 绑定清单前 8 条的解析情况 =="
head -8 /opt/jhy-sandbox/silentsafe/bind-list.txt | while read -r f; do
  printf "   %-30s realpath=%s  是软链=%s\n" "$f" "$(readlink -f "$f")" "$([ -L "$f" ] && echo 是 || echo 否)"
done

echo
echo "== 拦截器源文件是否存在 =="
head -3 /opt/jhy-sandbox/silentsafe/bind-list.txt | while read -r f; do
  printf "   %-30s 源=%s %s\n" "$f" "/opt/silentsafe/real$f" \
    "$([ -e "/opt/silentsafe/real$f" ] && echo 存在 || echo 缺失)"
done

echo
echo "== 用最小参数集试 bwrap(只保留基本绑定)=="
sudo -u jhy -H /usr/bin/bwrap --die-with-parent --unshare-user --unshare-pid --unshare-uts \
  --hostname compute-node-01 --proc /proc --dev-bind /dev /dev \
  --ro-bind /usr /usr --ro-bind /lib /lib --ro-bind /lib64 /lib64 --ro-bind /bin /bin --ro-bind /sbin /sbin \
  --ro-bind /etc /etc --bind /home/jhy /home/jhy --tmpfs /tmp --chdir /home/jhy -- /bin/bash -c 'echo 最小沙箱 OK' 2>&1 | head -3

echo
echo "== 加一个文件覆盖试试(用 sudo 那份拦截器)=="
sudo -u jhy -H /usr/bin/bwrap --die-with-parent --unshare-user --unshare-pid --proc /proc \
  --dev-bind /dev /dev --ro-bind /usr /usr --ro-bind /lib /lib --ro-bind /lib64 /lib64 \
  --ro-bind /bin /bin --ro-bind /sbin /sbin --ro-bind /etc /etc \
  --ro-bind /opt/silentsafe/real/usr/bin/sudo /usr/bin/sudo \
  --bind /home/jhy /home/jhy --tmpfs /tmp -- /bin/bash -c 'echo 覆盖 OK' 2>&1 | head -3

echo
echo "== 试 --tmp-overlay =="
sudo -u jhy -H /usr/bin/bwrap --die-with-parent --unshare-user --unshare-pid --proc /proc \
  --dev-bind /dev /dev --ro-bind /usr /usr --ro-bind /lib /lib --ro-bind /lib64 /lib64 \
  --ro-bind /bin /bin --ro-bind /sbin /sbin --ro-bind /etc /etc \
  --bind /home/jhy /home/jhy --tmpfs /tmp --tmp-overlay / -- /bin/bash -c 'echo overlay OK' 2>&1 | head -3
