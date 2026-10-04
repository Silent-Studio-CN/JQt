#!/usr/bin/env python3
"""修正请求体解析:含已知参数名才当表单,否则整段当命令。
(curl 的 --data-binary 默认 Content-Type 是 form-urlencoded,不能只看 CT。)
"""
import io
import py_compile

P = "/usr/local/bin/jqt-exec-api.py"
s = io.open(P, encoding="utf-8").read()
OLD = '''        # 表单:cmd=...&cwd=...
        if "form-urlencoded" in ctype or (text and "=" in text.split("\\n")[0] and "cmd=" in text):
            return {k: v[0] for k, v in parse_qs(text).items()}
        # 其余情况(含 text/plain、空 Content-Type):整个请求体就是命令
        # 用法:curl -X POST ... --data-binary 'df -h'
        if text.strip():
            return {"cmd": text}
        return {}'''
NEW = '''        # curl 的 --data-binary 默认 Content-Type 就是 form-urlencoded,
        # 因此不能只看类型:先按表单解析,**只有当解析出的键里出现已知参数名**才当表单,
        # 否则整段请求体就是命令(用法:curl -X POST ... --data-binary 'df -h')。
        form = {k: v[0] for k, v in parse_qs(text).items()}
        if form and (set(form) & KNOWN_PARAMS):
            return form
        if text.strip():
            return {"cmd": text}
        return {}'''
if OLD in s:
    s = s.replace(OLD, NEW, 1)
    # 定义已知参数名
    anchor = 'SESSION_RE = re.compile(r"^[A-Za-z0-9_-]{1,32}$")'
    if "KNOWN_PARAMS" not in s:
        s = s.replace(anchor, anchor + '\nKNOWN_PARAMS = {"cmd", "cwd", "timeout", "format", "session", "token"}',
                      1)
    io.open(P, "w", encoding="utf-8").write(s)
    py_compile.compile(P, doraise=True)
    print("已修正请求体解析;语法检查通过")
else:
    print("⚠️ 未匹配(可能已改)")
