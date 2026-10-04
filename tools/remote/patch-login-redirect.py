#!/usr/bin/env python3
"""修登录跳转:
  ① login.html 忽略服务端 next、写死跳首页 -> 记住来源入口并跳回
  ② jqt-auth.py 返回的 next 写死 /console/ -> 按来源入口返回(白名单校验,防开放重定向)
两个文件都做幂等替换,可重复执行。
"""
import io
import re

HTML = "/var/www/jqt/login.html"
AUTH = "/usr/local/bin/jqt-auth.py"

# ---------------------------------------------------------------- ① 登录页
s = io.open(HTML, encoding="utf-8").read()
changed = []

if "jqt-next" not in s:
    # 在脚本开头插入"记住来源入口"的逻辑
    anchor = "const $ = id => document.getElementById(id);\n"
    inject = anchor + """// jqt-next:记住用户是从哪个入口被弹到登录页的(/console/ 或 /silent/ 等),
// 登录成功后跳回原入口,而不是一律回首页。
const JQT_ENTRY = /^\\/(console|silent|account)\\//;
let jqtNext = new URLSearchParams(location.search).get('next') || '';
if (!jqtNext && JQT_ENTRY.test(location.pathname)) jqtNext = location.pathname;
"""
    if anchor in s:
        s = s.replace(anchor, inject, 1)
        changed.append("注入来源记录")

    old_body = "body:JSON.stringify({user:u,password:p})});"
    new_body = "body:JSON.stringify({user:u,password:p,next:jqtNext})});"
    if old_body in s:
        s = s.replace(old_body, new_body, 1)
        changed.append("登录请求带 next")

    old_go = "if(d.ok){ location.href='/' }"
    new_go = "if(d.ok){ location.href=(d.next&&d.next.startsWith('/')&&!d.next.startsWith('//'))?d.next:(jqtNext||'/') }"
    if old_go in s:
        s = s.replace(old_go, new_go, 1)
        changed.append("成功后跳回来源入口")

if changed:
    io.open(HTML, "w", encoding="utf-8").write(s)
    print("login.html 已更新:" + "、".join(changed))
else:
    print("login.html 无需改动")

# ---------------------------------------------------------------- ② 网关
a = io.open(AUTH, encoding="utf-8").read()
done = []

if "def safe_next(" not in a:
    anchor = 'ALLOWED = {"silent": "/console/"}   # 入口路径不含身份信息;root 入口不暴露'
    helper = anchor + '''
ENTRY_PREFIXES = ("/console/", "/silent/", "/account/")   # 已注册入口(登录后允许跳回)


def safe_next(user, want):
    """登录后的跳转目标只允许本站已注册入口,防开放重定向。"""
    default = ALLOWED.get(user, "/console/")
    want = (want or "").replace("\\\\", "").strip()
    if not want.startswith("/") or want.startswith("//"):
        return default
    for pre in ENTRY_PREFIXES:
        if want.startswith(pre):
            return want
    return default'''
    if anchor in a:
        a = a.replace(anchor, helper, 1)
        done.append("新增 safe_next()")
    else:
        print("⚠️ 未找到 ALLOWED 锚点")

old_ret = 'return self._json(200, {"ok": True, "next": ALLOWED[user], "user": user},'
new_ret = 'return self._json(200, {"ok": True, "next": safe_next(user, str(data.get("next", ""))), "user": user},'
if old_ret in a:
    a = a.replace(old_ret, new_ret, 1)
    done.append("登录返回按来源入口")

if done:
    io.open(AUTH, "w", encoding="utf-8").write(a)
    print("jqt-auth.py 已更新:" + "、".join(done))
else:
    print("jqt-auth.py 无需改动")

# 语法自检
import py_compile
py_compile.compile(AUTH, doraise=True)
print("jqt-auth.py 语法检查通过")
