#!/bin/bash
# 收尾校验:页面 JS 语法 + 完整流程复测
echo "=== 1) 页面可访问 & 体积 ==="
for f in / /login.html /account/; do
  printf "   %-14s HTTP %s\n" "$f" "$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 http://127.0.0.1:8080$f)"
done
ls -l /var/www/jqt/account/index.html | awk '{print "   account 页:", $5, "字节"}'

echo
echo "=== 2) 内联 JS 语法检查 ==="
if command -v node >/dev/null 2>&1; then
  python3 - <<'PY'
import io, re, subprocess, tempfile, os
s = io.open("/var/www/jqt/account/index.html", encoding="utf-8").read()
blocks = re.findall(r"<script>(.*?)</script>", s, re.S)
print(f"   找到 {len(blocks)} 段内联脚本")
bad = 0
for i, b in enumerate(blocks):
    if "src=" in b[:60]:
        continue
    with tempfile.NamedTemporaryFile("w", suffix=".js", delete=False, encoding="utf-8") as f:
        f.write(b); path = f.name
    r = subprocess.run(["node", "--check", path], capture_output=True, text=True)
    print(f"   第 {i+1} 段: {'✅ 语法通过' if r.returncode == 0 else '❌ ' + r.stderr.strip()[:160]}")
    if r.returncode != 0: bad += 1
    os.unlink(path)
print("   结论:", "全部通过" if bad == 0 else f"{bad} 段有问题")
PY
else
  echo "   （无 node,跳过;用括号配平粗检)"
  python3 -c "
import io
s=io.open('/var/www/jqt/account/index.html',encoding='utf-8').read()
print('   { } 配平:', s.count('{')-s.count('}'), '  ( ) 配平:', s.count('(')-s.count(')'))"
fi

echo
echo "=== 3) jhy 完整流程(登录 -> 终端 -> 退出)==="
J='jhy20110726'
C=$(curl -s -i -X POST http://127.0.0.1:8080/api/login -H 'Content-Type: application/json' \
     -d "{\"user\":\"jhy\",\"password\":\"$J\"}" | grep -i '^set-cookie' \
     | sed 's/.*jqt_auth=\([^;]*\).*/\1/' | tr -d '\r')
echo "   登录: $(curl -s -H "Cookie: jqt_auth=$C" http://127.0.0.1:8080/api/whoami)"
echo "   终端: HTTP $(curl -s -o /dev/null -w '%{http_code}' -H "Cookie: jqt_auth=$C" http://127.0.0.1:8080/terminal/)"
echo "   误闯 /console/: $(curl -s -o /dev/null -w '%{http_code} -> %{redirect_url}' -H "Cookie: jqt_auth=$C" http://127.0.0.1:8080/console/)"
echo "   管理接口: $(curl -s -H "Cookie: jqt_auth=$C" http://127.0.0.1:8080/api/status | head -c 60)"
echo "   自己的账号: $(curl -s -H "Cookie: jqt_auth=$C" http://127.0.0.1:8080/api/account | head -c 120)"
echo "   退出: $(curl -s -X POST -H "Cookie: jqt_auth=$C" http://127.0.0.1:8080/api/logout)"
echo "   退出后: $(curl -s -H "Cookie: jqt_auth=$C" http://127.0.0.1:8080/api/whoami)"

echo
echo "=== 4) 服务状态 ==="
systemctl is-active nginx jqt-auth jqt-term-silent jqt-term-silent-legacy jqt-term-jhy | tr '\n' ' '; echo
