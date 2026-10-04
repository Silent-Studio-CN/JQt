#!/bin/bash
echo "=== /api/account/password ==="; sudo sed -n '333,345p' /usr/local/bin/jqt-auth.py
echo "=== /api/account/logout-all ==="; sudo sed -n '346,354p' /usr/local/bin/jqt-auth.py
echo "=== /api/account/brand ==="; sudo sed -n '355,365p' /usr/local/bin/jqt-auth.py
echo "=== /api/account/service ==="; sudo sed -n '366,392p' /usr/local/bin/jqt-auth.py
echo "=== status() 返回字段 ==="; sudo sed -n '91,140p' /usr/local/bin/jqt-auth.py | grep -E '"[a-z_]+":|return' | head -25
echo "=== brand() 返回字段 ==="; sudo sed -n '/^def brand/,/^def /p' /usr/local/bin/jqt-auth.py | grep -E 'return|"[a-z]+":' | head -5
echo "=== 版本/更新信息(若有)==="; sudo grep -nE 'VERSION|v1\.[0-9]|更新' /usr/local/bin/jqt-auth.py | head -5
