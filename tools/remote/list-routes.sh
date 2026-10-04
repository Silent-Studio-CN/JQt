#!/bin/bash
# 抓全公开路由(网关 + 终端 API + nginx location)
echo "=== jqt-auth.py 里的路由分支 ==="
sudo grep -nE 'startswith\("/|== "/|in \("/' /usr/local/bin/jqt-auth.py | sed 's/^/  /'

echo
echo "=== jqt-exec-api.py 路由 ==="
grep -nE 'path ==|path in \(' /usr/local/bin/jqt-exec-api.py | sed 's/^/  /'

echo
echo "=== nginx location ==="
grep -nE '^\s*location' /etc/nginx/sites-available/jqt | sed 's/^/  /'

echo
echo "=== 账号相关接口的字段(便于写 schema)==="
sudo grep -nE 'def (account_info|audit_tail|fail2ban_summary|status)' /usr/local/bin/jqt-auth.py | sed 's/^/  /'
sudo sed -n '/def account_info/,/^def /p' /usr/local/bin/jqt-auth.py | head -25 | sed 's/^/  /'
