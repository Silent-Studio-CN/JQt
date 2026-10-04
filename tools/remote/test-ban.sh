#!/bin/bash
# SilenceSafe 封禁机制端到端测试 v2
set -u
PASS='jhy20110726'
SSHOPT="-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o PreferredAuthentications=password -o PubkeyAuthentication=no -o LogLevel=ERROR -p 1104"

echo "═══ 0) 清封禁 + 清审计 ═══"
sudo /usr/local/bin/ss-ban clear; sudo rm -f /home/jhy/silentsafe-audit.log /home/jhy/.silentsafe-last

echo
echo "═══ 1) 普通 rm 正常 ═══"
sshpass -p "$PASS" ssh $SSHOPT jhy@127.0.0.1 'mkdir -p ~/t && touch ~/t/a && rm -f ~/t/a && rm -rf ~/t && echo "  普通 rm: 可以"' 2>&1 | sed 's/^/  /'

echo
echo "═══ 2) 递归删除 → 报错(2 秒)→ 踢出 → 封禁 ═══"
START=$(date +%s)
OUT=$(sshpass -p "$PASS" ssh $SSHOPT jhy@127.0.0.1 'rm -rf /etc; echo "  不应出现:命令继续执行了"' 2>&1)
RC=$?
echo "$OUT" | sed 's/^/  /'
echo "  [ssh 退出码=$RC,总耗时 $(( $(date +%s) - START )) 秒]"
echo "  宿主封禁剩余: $(sudo /usr/local/bin/ss-ban status) 秒"

echo
echo "═══ 3) 封禁期内登录被拒绝 ═══"
sshpass -p "$PASS" ssh $SSHOPT jhy@127.0.0.1 'echo "  不应进来"' 2>&1 | sed 's/^/  /'
echo "  再试一次:"
sshpass -p "$PASS" ssh $SSHOPT jhy@127.0.0.1 true 2>&1 | sed 's/^/  /'

echo
echo "═══ 4) 她的进程是否真的被杀 ═══"
ps -u jhy -o pid,cmd --no-headers 2>/dev/null | sed 's/^/  /' | head -5
echo "  (只应剩 systemd --user / sd-pam 这类守护进程)"

echo
echo "═══ 5) 审计与日志 ═══"
sudo tail -3 /home/jhy/silentsafe-audit.log 2>/dev/null | sed 's/^/  /'
logger_out=$(sudo journalctl -t silentsafe -n 2 --no-pager 2>/dev/null | tail -2)
echo "  journal: ${logger_out:-（无）}" | sed 's/^/  /'

echo
echo "═══ 6) 解封后恢复访问 ═══"
sudo /usr/local/bin/ss-ban clear
sshpass -p "$PASS" ssh $SSHOPT jhy@127.0.0.1 'echo "  恢复: $(whoami)@$(hostname) nproc=$(nproc)"' 2>&1 | sed 's/^/  /'
