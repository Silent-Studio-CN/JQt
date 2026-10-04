#!/bin/bash
# 验证登录跳转链路:
#   ① safe_next() 白名单逻辑(含开放重定向攻击用例)
#   ② 未登录访问 /silent/ 得到登录页
#   ③ 用合法 cookie 访问 /silent/ 得到真正的终端(证明"登录后目的地"是通的)
set -u
echo "== ① safe_next 白名单(直接调用网关函数)"
sudo python3 - <<'PY'
import importlib.util
spec = importlib.util.spec_from_loader("g", loader=None)
src = open("/usr/local/bin/jqt-auth.py", encoding="utf-8").read()
ns = {}
exec(compile(src.split("class Handler")[0], "jqt-auth", "exec"), ns)
safe_next = ns["safe_next"]
cases = [
    ("/silent/",              "/silent/"),
    ("/silent/",              "/console/"),
    ("/console/",             "/silent/"),
    ("/console/",             "/account/"),
    ("/console/",             "/"),            # 根路径不在白名单 -> 回落
    ("/console/",             "//evil.com/x"), # 协议相对 -> 回落
    ("/console/",             "https://evil.com"),  # 外部 -> 回落
    ("/console/",             "/api/status"),  # 非入口 -> 回落
    ("/console/",             ""),             # 空 -> 回落
]
ok = True
for user, want in cases:
    got = safe_next(user, want)
    exp = want if want in ("/silent/", "/console/", "/account/") else ns["ALLOWED"].get(user, "/console/")
    flag = "✅" if got == exp else "❌"
    if got != exp:
        ok = False
    print(f"   {flag} user={user:9s} next={want:22s} -> {got}")
print("   结论:" + ("白名单逻辑正确" if ok else "存在错误!"))
PY

echo
echo "== ② 未登录访问(应回登录页)"
for p in /silent/ /console/; do
  body=$(curl -s --max-time 8 "http://127.0.0.1:8080$p" | head -c 200)
  printf "   %-10s -> %s\n" "$p" "$(echo "$body" | grep -qi '登录\|login' && echo '登录页 ✅' || echo "非登录页 ❓")"
done

echo
echo "== ③ 伪造合法 cookie 后访问(证明登录后目的地可用)"
COOKIE=$(sudo python3 - <<'PY'
import base64, hashlib, hmac, time
sec = open("/etc/jqt-auth.secret", "rb").read()
msg = f"silent|{int(time.time()) + 600}".encode()
print(base64.urlsafe_b64encode(msg + b"|" + hmac.new(sec, msg, hashlib.sha256).digest()).decode())
PY
)
for p in /silent/ /console/; do
  code=$(curl -s -o /tmp/o.html -w '%{http_code}' --max-time 10 -H "Cookie: jqt_auth=$COOKIE" "http://127.0.0.1:8080$p")
  kind=$(grep -qi 'xterm\|ttyd' /tmp/o.html && echo '终端页面 ✅' || (grep -qi '登录' /tmp/o.html && echo '登录页(仍被拦)❌' || echo '其它页面'))
  printf "   %-10s -> HTTP %s  %s  (%s 字节)\n" "$p" "$code" "$kind" "$(wc -c < /tmp/o.html)"
done

echo
echo "== ④ 登录接口仍正常(错误口令应 401)"
curl -s --max-time 8 -X POST http://127.0.0.1:9000/api/login \
  -H 'Content-Type: application/json' -d '{"user":"silent","password":"__definitely_wrong__","next":"/silent/"}' | head -c 200; echo

echo
echo "== ⑤ login.html 关键改动"
sudo grep -n 'jqtNext\|d.next' /var/www/jqt/login.html | sed 's/^/   /'
