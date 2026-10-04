#!/bin/bash
# 验证 jhy 的网页控制台:登录 / 入口隔离 / 权限收敛 / 沙箱是否生效
set -u
J='jhy20110726'

login() {  # $1=user $2=password $3=next
  curl -s -i -X POST http://127.0.0.1:8080/api/login -H 'Content-Type: application/json' \
    -d "{\"user\":\"$1\",\"password\":\"$2\",\"next\":\"$3\"}"
}
cookie_of() { grep -i '^set-cookie' | sed 's/.*jqt_auth=\([^;]*\).*/\1/' | tr -d '\r'; }

echo "=== 1) jhy 用网页登录 ==="
R=$(login jhy "$J" /terminal/)
echo "$R" | grep -iE '^HTTP/|ok' | head -2 | sed 's/^/   /'
C=$(echo "$R" | cookie_of)
echo "   cookie 前缀: ${C:0:14}…"

echo
echo "=== 2) 登录后身份与入口 ==="
curl -s -H "Cookie: jqt_auth=$C" http://127.0.0.1:8080/api/whoami | sed 's/^/   /'

echo
echo "=== 3) 她能进自己的终端,进不了别人的 ==="
for p in /terminal/ /console/ /silent/ /account/; do
  out=$(curl -s -o /dev/null -w '%{http_code} -> %{redirect_url}' -H "Cookie: jqt_auth=$C" "http://127.0.0.1:8080$p")
  echo "   $p -> $out"
done

echo
echo "=== 4) 权限收敛(管理类接口应 403)==="
for p in /api/status /api/account/audit; do
  echo "   $p -> $(curl -s -H "Cookie: jqt_auth=$C" http://127.0.0.1:8080$p)"
done
for p in /api/account/service /api/account/brand /api/account/logout-all; do
  echo "   POST $p -> $(curl -s -X POST -H "Cookie: jqt_auth=$C" -H 'Content-Type: application/json' \
      -d '{"name":"nginx","action":"stop","line":"x"}' http://127.0.0.1:8080$p)"
done

echo
echo "=== 5) 她自己的功能应可用 ==="
echo "   /api/account -> $(curl -s -H "Cookie: jqt_auth=$C" http://127.0.0.1:8080/api/account | head -c 150)"
echo "   /api/exec(无 token)-> $(curl -s http://127.0.0.1:8080/api/exec?cmd=id | head -c 90)"

echo
echo "=== 6) silent 仍能进两个老入口 ==="
R2=$(login silent "$(cat /tmp/silent_pw 2>/dev/null || echo '')" /console/ 2>/dev/null || true)
echo "   （silent 密码不在脚本里,跳过实际登录;只验证路径可达性)）"
for p in /console/ /silent/ /terminal/; do
  code=$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:8080$p")
  echo "   未登录访问 $p -> HTTP $code(401 -> 登录页属正常)"
done

echo
echo "=== 7) 网页终端实例状态 ==="
systemctl is-active jqt-term-silent jqt-term-silent-legacy jqt-term-jhy | tr '\n' ' '; echo
for port in 7682 7683 7684; do
  echo "   127.0.0.1:$port -> HTTP $(curl -s -o /dev/null -w '%{http_code}' --max-time 4 http://127.0.0.1:$port/)"
done

echo
echo "=== 8) 退出 jhy 的会话 ==="
curl -s -X POST -H "Cookie: jqt_auth=$C" http://127.0.0.1:8080/api/logout | sed 's/^/   /'
echo "   退出后 whoami: $(curl -s -H "Cookie: jqt_auth=$C" http://127.0.0.1:8080/api/whoami)"
