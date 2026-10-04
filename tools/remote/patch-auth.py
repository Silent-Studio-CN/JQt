#!/usr/bin/env python3
"""在节点上执行:让登录网关的路径校验放行 /silent/ 前缀(旧入口仍在使用)。"""
import io

P = "/usr/local/bin/jqt-auth.py"
s = io.open(P, encoding="utf-8").read()
OLD = ('if not (uri.startswith(ALLOWED.get(user, "/console/")) or uri.startswith("/api/") '
       'or uri.startswith("/account/")):')
NEW = ('if not (uri.startswith(ALLOWED.get(user, "/console/")) or uri.startswith("/silent/") '
       'or uri.startswith("/api/") or uri.startswith("/account/")):')
if 'uri.startswith("/silent/")' in s:
    print("已放行 /silent/(无需改动)")
elif OLD in s:
    io.open(P, "w", encoding="utf-8").write(s.replace(OLD, NEW, 1))
    print("已放行 /silent/ 前缀")
else:
    print("⚠️ 未找到校验语句,需人工确认")
