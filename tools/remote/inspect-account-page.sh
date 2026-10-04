#!/bin/bash
# 看账号页结构,准备给非管理员隐藏管理面板
echo "=== 页面里的区块/标题 ==="
grep -nE 'id="[a-zA-Z]+"|<h2|<h3|class="card' /var/www/jqt/account/index.html | head -30

echo
echo "=== 前端调用的接口 ==="
grep -nE "j\('/api|fetch\('/api|whoami|/api/account" /var/www/jqt/account/index.html | head -14

echo
echo "=== 顶部初始化逻辑 ==="
sed -n '1,20p' /var/www/jqt/account/index.html | grep -nE 'script|whoami|user' | head -8
