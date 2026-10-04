#!/bin/bash
# 最终验证:四种 curl 写法 + 会话 + 退出码
set -u
H=http://127.0.0.1:8080
T=$(sudo /usr/bin/python3 /usr/local/bin/jqt-exec-api.py --show-tokens | awk '{print $2}')

echo "① 请求体直接是命令(curl 默认 form CT 也要能过)"
curl -s -X POST "$H/api/exec" -H "Authorization: Bearer $T" \
     --data-binary 'hostname; echo body-as-command-ok' | head -c 260; echo

echo "② 含等号的命令也必须当命令(不被误判成表单)"
curl -s -X POST "$H/api/exec" -H "Authorization: Bearer $T" \
     --data-binary 'echo A=1; date +%Y-%m-%d' | head -c 260; echo

echo "③ 表单写法仍正常"
curl -s -X POST "$H/api/exec" -H "X-JQt-Token: $T" --data-urlencode 'cmd=echo form-ok' | head -c 200; echo

echo "④ GET 一行式 + format=text"
curl -s "$H/api/exec?cmd=uptime%20-p&token=$T" | head -c 200; echo
curl -s "$H/api/exec?cmd=ls%20-1%20/etc/jqt&format=text&token=$T"

echo "⑤ 持久会话(query 指定 session,请求体给命令)"
curl -s -X POST "$H/api/exec?session=api2" -H "Authorization: Bearer $T" \
     --data-binary 'cd /etc; pwd' | head -c 200; echo
curl -s "$H/api/exec?cmd=pwd;%20ls%20-1%20%7C%20head%20-2&session=api2&token=$T" | head -c 240; echo

echo "⑥ 非零退出码 / 超时"
curl -s "$H/api/exec?cmd=false&token=$T" | head -c 140; echo
curl -s "$H/api/exec?cmd=sleep%209&timeout=2&token=$T" | head -c 190; echo
