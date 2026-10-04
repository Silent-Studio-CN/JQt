#!/bin/bash
# 干净版诊断(排除 redoc 大文件)
echo "=== 1) 退出相关路径经过 nginx 的响应 ==="
for p in /logout /api/logout /login; do
  code=$(curl -s -o /tmp/o.txt -w '%{http_code}' "http://127.0.0.1:8080$p")
  echo "   $p -> HTTP $code   $(head -c 100 /tmp/o.txt | tr '\n' ' ')"
done

echo
echo "=== 2) 直连网关(绕过 nginx)==="
for p in /logout /api/logout; do
  code=$(curl -s -o /tmp/o2.txt -w '%{http_code}' "http://127.0.0.1:9000$p")
  echo "   $p -> HTTP $code   $(head -c 100 /tmp/o2.txt | tr '\n' ' ')"
done

echo
echo "=== 3) 前端实际调用的退出地址(排除 redoc)==="
sudo grep -rn "logout" /var/www/jqt/account/index.html /var/www/jqt/index.html \
     /var/www/jqt/assets/console-mobile.js 2>/dev/null | head -10

echo
echo "=== 4) nginx 现有 location 一览 ==="
grep -nE '^\s*location' /etc/nginx/sites-available/jqt
