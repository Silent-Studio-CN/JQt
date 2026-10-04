#!/bin/bash
# 最终确认:全新会话(不同 exp)必须可用;吊销表去重
set -u

mk() {  # $1 = 过期秒数偏移
python3 - "$1" <<'PY'
import base64, hashlib, hmac, sys, time
sec = open("/etc/jqt-auth.secret", "rb").read()
msg = f"silent|{int(time.time()) + int(sys.argv[1])}".encode()
print(base64.urlsafe_b64encode(msg + b"|" + hmac.new(sec, msg, hashlib.sha256).digest()).decode())
PY
}

echo "=== A) 吊销表去重 ==="
sudo python3 - <<'PY'
import io
p = "/etc/jqt-revoked"
try:
    seen, out = set(), []
    for line in io.open(p, encoding="utf-8"):
        h = line.split()[0] if line.split() else ""
        if h and h not in seen:
            seen.add(h); out.append(line.rstrip())
    io.open(p, "w", encoding="utf-8").write("\n".join(out) + ("\n" if out else ""))
    print(f"   去重后 {len(out)} 条")
except FileNotFoundError:
    print("   （无吊销表）")
PY

echo
echo "=== B) 全新会话(exp +601)应当可用 ==="
T1=$(mk 601)
curl -s -H "Cookie: jqt_auth=$T1" http://127.0.0.1:8080/api/whoami | sed 's/^/   /'

echo
echo "=== C) 退出它 ==="
curl -s -X POST -H "Cookie: jqt_auth=$T1" http://127.0.0.1:8080/api/logout | sed 's/^/   /'

echo
echo "=== D) 退出后同一 Cookie 必须失效 ==="
curl -s -H "Cookie: jqt_auth=$T1" http://127.0.0.1:8080/api/whoami | sed 's/^/   /'

echo
echo "=== E) 另一个全新会话(exp +602)不受影响 ==="
T2=$(mk 602)
curl -s -H "Cookie: jqt_auth=$T2" http://127.0.0.1:8080/api/whoami | sed 's/^/   /'

echo
echo "=== F) 前端退出按钮的实际调用链(模拟浏览器)==="
T3=$(mk 603)
echo "   1) 登录态: $(curl -s -H "Cookie: jqt_auth=$T3" http://127.0.0.1:8080/api/whoami)"
echo "   2) fetch('/api/logout'): $(curl -s -H "Cookie: jqt_auth=$T3" http://127.0.0.1:8080/api/logout)"
echo "   3) 刷新状态: $(curl -s -H "Cookie: jqt_auth=$T3" http://127.0.0.1:8080/api/whoami)"

echo
echo "=== G) 账号页的\"注销全部会话\"仍可用 ==="
T4=$(mk 604)
curl -s -X POST -H "Cookie: jqt_auth=$T4" http://127.0.0.1:8080/api/account/logout-all | sed 's/^/   /'
echo "   之后旧 Cookie: $(curl -s -H "Cookie: jqt_auth=$T4" http://127.0.0.1:8080/api/whoami)"
echo "   网关状态: $(systemctl is-active jqt-auth)  nginx: $(systemctl is-active nginx)"
