#!/usr/bin/env python3
"""修 rm shim:只拦"关键目录/根",放行她自己家目录内的普通删除。
(之前把所有绝对路径都当危险 -> 连缓存文件都删不掉,连带 sudo 缓存失效逻辑失灵)
"""
import io
import os
import subprocess

BIN = "/opt/jhy-sandbox/bin"

RM = '''#!/bin/bash
# 正常删除放行;仅当目标是系统关键目录(或对它们递归删除)时拦截。
CRIT="/ /bin /sbin /lib /lib64 /usr /etc /var /boot /sys /proc /dev /root /opt /srv /mnt /home"
rec=0
for a in "$@"; do
  case "$a" in -*) case "$a" in *[rR]*) rec=1 ;; esac ;; esac
done
for a in "$@"; do
  case "$a" in
    -*) continue ;;
    --no-preserve-root) 
      export SS_CODE="SS_ERR_ID_100"; export SS_DESC="递归删除根目录"
      exec /opt/silentsafe/block.sh ;;
  esac
  real=$(readlink -f -- "$a" 2>/dev/null || echo "$a")
  # 她自己家目录内:放行
  case "$real" in /home/jhy|/home/jhy/*) continue ;; esac
  case "$real" in /tmp/*|/var/tmp/*) continue ;; esac
  for c in $CRIT; do
    if [ "$real" = "$c" ]; then
      export SS_CODE="SS_ERR_ID_100"
      export SS_DESC="删除系统关键目录($c)"
      exec /opt/silentsafe/block.sh
    fi
    if [ $rec -eq 1 ]; then
      case "$real" in
        "$c"/*) export SS_CODE="SS_ERR_ID_100"
                export SS_DESC="递归删除系统目录($c)"
                exec /opt/silentsafe/block.sh ;;
      esac
    fi
  done
done
exec /opt/real/rm "$@"
'''
io.open(f"{BIN}/rm", "w", encoding="utf-8", newline="\n").write(RM)
os.chmod(f"{BIN}/rm", 0o755)
print("   rm shim 已改为精确匹配")

t = subprocess.run(["sudo", "-u", "jhy", "-H", "/usr/local/bin/jhy-shell"],
                   input='rm -f ~/.cache/.sudo-ts; echo "缓存已清: $?"\n'
                         'echo "普通删除: $(mkdir -p ~/t && touch ~/t/a && rm -f ~/t/a && echo 可以)"\n'
                         'echo "删关键目录: $(rm -rf /etc 2>&1 | head -1)"\n'
                         'rm -f ~/.cache/.sudo-ts\n'
                         'echo "错误密码 → $(echo wrongpass | sudo -S id 2>&1 | tail -1)"\n'
                         'rm -f ~/.cache/.sudo-ts\n'
                         'echo "正确密码 → $(echo jhy20110726 | sudo -S id 2>&1 | head -1)"\n'
                         'rm -f ~/.cache/.sudo-ts\n'
                         'echo "无缓存 + -n → $(sudo -n id 2>&1 | head -1)"\n'
                         'echo "有缓存后 -n → $(echo jhy20110726 | sudo -S true; sudo -n id 2>&1 | head -1)"\n',
                   capture_output=True, text=True, timeout=180)
print(t.stdout.rstrip()[:1800])
if t.stderr.strip():
    print("   [stderr]", t.stderr.strip()[:400])
