#!/bin/bash
# 检查前端页面是否泄漏入口路径(首页不应出现 /silent、/root)
echo "== 首页/登录页/账号页 里出现的入口路径 =="
for f in /var/www/jqt/index.html /var/www/jqt/login.html /var/www/jqt/account/index.html; do
  hits=$(sudo grep -oE '/(silent|root)(/|[^a-zA-Z0-9]|$)' "$f" 2>/dev/null | sort -u | tr '\n' ' ')
  printf "   %-42s %s\n" "$(basename "$(dirname "$f")")/$(basename "$f")" "${hits:-（无）}"
done

echo
echo "== 首页里所有链接(href/src) =="
sudo grep -oE '(href|src)="[^"]+"' /var/www/jqt/index.html | sort -u | sed 's/^/   /'

echo
echo "== 首页里提到的入口按钮文字(console/终端) =="
sudo grep -oE '>[^<]*(控制台|终端|console|Console)[^<]*<' /var/www/jqt/index.html | sort -u | sed 's/^/   /' | head -8
