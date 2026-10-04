#!/usr/bin/env python3
"""overlayfs 在此内核的 user ns 里不可用(userxattr EINVAL)-> 改为:
  把"root 常写的位置"挂成隐形 tmpfs(写得进、退出即消失),
  另外挡掉 /var/log(真日志里有宿主/nginx 痕迹)。
"""
import io
import os
import subprocess

WRAPPER = "/usr/local/bin/jhy-shell"
s = io.open(WRAPPER, encoding="utf-8").read()

# 去掉 overlay 两行
s = s.replace("  --overlay-src /\n  --tmp-overlay /\n", "")
# 加可写 tmpfs(顺序:先 /opt 再绑 /opt/bin 等)
if "--tmpfs /usr/local" not in s:
    s = s.replace("  --proc /proc\n",
                  "  # 假 root 需要能\"写系统\"的地方:挂隐形 tmpfs(改动只在内存)\n"
                  "  # overlayfs 在 user namespace 里不可用,故用这个办法\n"
                  "  --tmpfs /opt\n"
                  "  --tmpfs /usr/local\n"
                  "  --tmpfs /root\n"
                  "  --tmpfs /srv\n"
                  "  --tmpfs /var/tmp\n"
                  "  --tmpfs /var/log\n"
                  "  --proc /proc\n", 1)
    io.open(WRAPPER, "w", encoding="utf-8", newline="\n").write(s)
    os.chmod(WRAPPER, 0o755)
    print("   已改为 tmpfs 可写位置")
else:
    print("   = 已是 tmpfs 方案")

p = subprocess.run(["bash", "-n", WRAPPER], capture_output=True, text=True)
print("   语法:", "通过" if p.returncode == 0 else p.stderr[:300])

# 自测:沙箱能否起、假 root 能否写系统目录、拦截是否命中
test = r'''
echo "== 沙箱自测 =="
echo "谁: $(whoami)@$(hostname)  nproc=$(nproc)  uname=$(uname -sr)"
echo "LD_PRELOAD 生效: /usr/bin/nproc=$(/usr/bin/nproc)  绝对路径 uname=$(/usr/bin/uname -r)"
echo "python cpu: $(python3 -c 'import os;print(os.cpu_count())' 2>/dev/null)"
echo "--- sudo(假 root)---"
echo 'jhy20110726' | sudo -S id 2>/dev/null || echo "sudo 失败"
echo 'jhy20110726' | sudo -S sh -c 'touch /usr/local/lib/ok && echo "写 /usr/local 成功" && id -u'
echo "读 sudoers: $(echo 'jhy20110726' | sudo -S tail -1 /etc/sudoers 2>/dev/null)"
echo "--- SilentSafe 拦截 ---"
for c in "reboot" "mkfs.ext4 /dev/sda1" "iptables -F" "useradd x" "crontab -e" "nmap 127.0.0.1" "rm -rf /" "dd if=/dev/zero of=/dev/sda" "kill -9 -1"; do
  printf "  %-24s -> %s\n" "$c" "$($c 2>&1 | head -2 | tr '\n' '|')"
done
echo "--- 绝对路径 ---"
for c in "/usr/sbin/reboot" "/usr/sbin/useradd x" "/usr/bin/iptables -F"; do
  printf "  %-24s -> %s\n" "$c" "$($c 2>&1 | head -1)"
done
echo "--- 正常用法 ---"
mkdir -p ~/w && echo ok > ~/w/f && cat ~/w/f && df -h ~ | tail -1
'''
p = subprocess.run(["sudo", "-u", "jhy", "-H", WRAPPER], input=test, capture_output=True,
                   text=True, timeout=180)
print(p.stdout.rstrip()[:4000])
if p.stderr.strip():
    print("   [stderr]", p.stderr.strip()[:600])
