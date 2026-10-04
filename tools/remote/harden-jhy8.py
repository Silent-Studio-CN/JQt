#!/usr/bin/env python3
"""最后收口:① lsmod shim(沙箱里 /sys/module 是空 tmpfs,真 lsmod 会刷错误)
            ② journalctl / systemctl 会不会漏 WSL 线索 -> 探明并处理
"""
import io
import os
import subprocess

SHIM = "/opt/jhy-sandbox/bin"


def write(path, content, mode=0o755):
    io.open(path, "w", encoding="utf-8", newline="\n").write(content)
    os.chmod(path, mode)
    print("   写入", path)


# ① lsmod:直接读伪造的 /proc/modules,别去碰空 /sys/module
write(f"{SHIM}/lsmod", '''#!/bin/bash
# 真 lsmod 会去读 /sys/module/<name>/holders,而沙箱里 /sys/module 是空 tmpfs,
# 会刷一堆 ERROR(显得像被动了手脚)-> 自己按同样格式打印。
printf "Module                  Size  Used by\\n"
awk '{ printf "%-20s %8s  %s\\n", $1, $2, ($3 == 0 ? "" : $3) }' /proc/modules 2>/dev/null | head -80
''')

# ② journalctl / systemctl:先探明她能不能用
probe = r'''
sudo -u jhy -H /usr/local/bin/jhy-shell <<'EOS'
echo "== systemctl =="
systemctl status nginx 2>&1 | head -3
echo "== journalctl =="
journalctl -n 3 --no-pager 2>&1 | head -3
echo "== top 头部 =="
top -bn1 | head -4
echo "== /sys/devices/system/cpu 目录里有多少 cpuN =="
ls -d /sys/devices/system/cpu/cpu[0-9]* 2>/dev/null | wc -l
echo "== python 看到的 CPU 数 =="
python3 -c "import os; print(os.cpu_count())" 2>/dev/null || echo "(无 python)"
EOS
'''
p = subprocess.run(["bash", "-lc", probe], capture_output=True, text=True, timeout=120)
print(p.stdout.rstrip()[:2000])
if p.stderr.strip():
    print("   [stderr]", p.stderr.strip()[:300])
