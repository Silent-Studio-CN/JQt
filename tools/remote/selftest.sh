#!/bin/bash
# 在节点上自测:/silent 路由、/console 路由、curl 终端 API
set -u
TOK=$(sudo /usr/bin/python3 /usr/local/bin/jqt-exec-api.py --show-tokens | awk '{print $2}')
echo "== token 前缀: ${TOK:0:12}…"

echo
echo "== 1) 三个入口的状态码(经 nginx:8080)"
for p in "/silent/" "/console/" "/api/exec/help" "/account/"; do
  printf "   %-16s -> HTTP %s\n" "$p" "$(curl -s -o /dev/null -w '%{http_code}' --max-time 8 "http://127.0.0.1:8080$p")"
done

echo
echo "== 2) 未带 token(应 401)"
curl -s --max-time 8 "http://127.0.0.1:9100/api/exec?cmd=id" | head -c 200; echo

echo
echo "== 3) 明文 HTTP 经 nginx(应 403,提示用 https)"
curl -s --max-time 8 "http://127.0.0.1:8080/api/exec?cmd=id&token=$TOK" | head -c 260; echo

echo
echo "== 4) 直连 API 一次性命令(JSON)"
curl -s --max-time 15 -X POST "http://127.0.0.1:9100/api/exec" \
     -H "Authorization: Bearer $TOK" --data-urlencode 'cmd=id; uname -r; uptime -p' | head -c 400; echo

echo
echo "== 5) format=text + 退出码头"
curl -s --max-time 15 -D /tmp/h.txt "http://127.0.0.1:9100/api/exec?cmd=echo%20hello-from-curl&format=text&token=$TOK"
grep -i 'x-jqt-exit\|x-jqt-ms' /tmp/h.txt | sed 's/^/   /'

echo
echo "== 6) 非零退出码透传"
curl -s --max-time 15 "http://127.0.0.1:9100/api/exec?cmd=exit%203&token=$TOK" | head -c 200; echo

echo
echo "== 7) 持久会话(tmux):cd 是否跨调用保留"
curl -s --max-time 20 -X POST "http://127.0.0.1:9100/api/exec" -H "X-JQt-Token: $TOK" \
     --data-urlencode 'cmd=cd /var/log && pwd' --data 'session=smoke' | head -c 300; echo
curl -s --max-time 20 "http://127.0.0.1:9100/api/exec?cmd=pwd&session=smoke&token=$TOK" | head -c 300; echo
echo "   -- 会话列表 --"
curl -s --max-time 8 "http://127.0.0.1:9100/api/exec/sessions?token=$TOK"; echo

echo
echo "== 8) 超时与超长输出保护"
curl -s --max-time 20 "http://127.0.0.1:9100/api/exec?cmd=sleep%205&timeout=2&token=$TOK" | head -c 200; echo

echo
echo "== 9) 审计日志(最后 3 条)"
sudo tail -3 /var/log/jqt/exec.log

echo
echo "== 10) ttyd 两个实例"
systemctl is-active jqt-term-silent jqt-term-silent-legacy jqt-exec-api jqt-auth nginx
