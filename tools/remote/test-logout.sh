#!/bin/bash
# 端到端验证退出登录(含服务端吊销)
set -u

echo "=== 1) 造一个合法会话 Cookie(用节点密钥签名,模拟已登录)==="
TOK=$(python3 - <<'PY'
import base64, hashlib, hmac, time
sec = open("/etc/jqt-auth.secret", "rb").read()
msg = f"silent|{int(time.time()) + 600}".encode()
print(base64.urlsafe_b64encode(msg + b"|" + hmac.new(sec, msg, hashlib.sha256).digest()).decode())
PY
)
echo "   token 前缀: ${TOK:0:16}…"

echo
echo "=== 2) 用该 Cookie 访问受保护接口(应成功)==="
curl -s -H "Cookie: jqt_auth=$TOK" http://127.0.0.1:8080/api/whoami | sed 's/^/   /'
curl -s -o /dev/null -w "   /account/ -> HTTP %{http_code}\n" -H "Cookie: jqt_auth=$TOK" http://127.0.0.1:8080/account/

echo
echo "=== 3) 调用前端真正使用的退出地址 /api/logout ==="
curl -s -i -X POST -H "Cookie: jqt_auth=$TOK" http://127.0.0.1:8080/api/logout \
  | grep -iE '^HTTP/|^set-cookie|ok' | sed 's/^/   /'
echo "   （GET 也试一次）"
curl -s -H "Cookie: jqt_auth=$TOK" http://127.0.0.1:8080/api/logout | sed 's/^/   /'

echo
echo "=== 4) 关键:同一个 Cookie 再用,必须已失效 ==="
curl -s -H "Cookie: jqt_auth=$TOK" http://127.0.0.1:8080/api/whoami | sed 's/^/   /'
curl -s -o /dev/null -w "   /account/ -> HTTP %{http_code}(期望回登录页)\n" \
  -H "Cookie: jqt_auth=$TOK" http://127.0.0.1:8080/account/

echo
echo "=== 5) 旧路径 /logout 也要能用 ==="
TOK2=$(python3 - <<'PY'
import base64, hashlib, hmac, time
sec = open("/etc/jqt-auth.secret", "rb").read()
msg = f"silent|{int(time.time()) + 600}".encode()
print(base64.urlsafe_b64encode(msg + b"|" + hmac.new(sec, msg, hashlib.sha256).digest()).decode())
PY
)
curl -s -o /dev/null -w "   /logout -> HTTP %{http_code}\n" -H "Cookie: jqt_auth=$TOK2" http://127.0.0.1:8080/logout
curl -s -H "Cookie: jqt_auth=$TOK2" http://127.0.0.1:8080/api/whoami | sed 's/^/   之后 whoami: /'

echo
echo "=== 6) 吊销表内容与清理 ==="
sudo cat /etc/jqt-revoked 2>/dev/null | sed 's/^/   /' || echo "   (空)"
echo "   条数: $(sudo wc -l < /etc/jqt-revoked 2>/dev/null || echo 0)"

echo
echo "=== 7) 全新登录仍可用(新会话不受影响)==="
TOK3=$(python3 - <<'PY'
import base64, hashlib, hmac, time
sec = open("/etc/jqt-auth.secret", "rb").read()
msg = f"silent|{int(time.time()) + 600}".encode()
print(base64.urlsafe_b64encode(msg + b"|" + hmac.new(sec, msg, hashlib.sha256).digest()).decode())
PY
)
curl -s -H "Cookie: jqt_auth=$TOK3" http://127.0.0.1:8080/api/whoami | sed 's/^/   /'
