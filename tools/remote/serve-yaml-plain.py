#!/usr/bin/env python3
"""把 /openapi.yaml 改成"浏览器直接显示纯文本"(不下载、不渲染):
  · text/plain; charset=utf-8  -> 浏览器内联显示,中文不乱码
  · Content-Disposition: inline -> 明确不当作附件下载
  · /openapi.json 保持 application/json(浏览器本来就是内联纯文本显示)
"""
import io
import re
import subprocess

P = "/etc/nginx/sites-available/jqt"
s = io.open(P, encoding="utf-8").read()

# 找到现有的 openapi location(可能有两种写法),整体替换成新的
pat = re.compile(r"\n\s*# 接口文档\(公开\)\n\s*location[^\n]*openapi[^\n]*\{.*?\n\s*\}\n", re.S)
new_block = '''
    # 接口文档(公开)。注意用 text/plain + inline:
    # application/yaml 会让浏览器**下载**而不是显示,而用户要的是"点开就是纯文本"。
    location = /openapi.yaml {
        types { }
        default_type "text/plain; charset=utf-8";
        charset utf-8;
        add_header Content-Disposition "inline";
        add_header Cache-Control "no-cache";
    }
    location = /openapi.json {
        types { }
        default_type "application/json; charset=utf-8";
        charset utf-8;
        add_header Content-Disposition "inline";
        add_header Cache-Control "no-cache";
    }
'''
if pat.search(s):
    s = pat.sub(new_block, s, count=1)
    print("已替换原有 openapi location")
else:
    # 兜底:插到 location / 之前
    anchor = "    location / { try_files $uri $uri/ =404; }\n"
    s = s.replace(anchor, new_block + "\n" + anchor, 1)
    print("已插入新的 openapi location")

io.open(P, "w", encoding="utf-8").write(s)
p = subprocess.run(["nginx", "-t"], capture_output=True, text=True)
print("nginx -t:", (p.stderr or "ok").strip().splitlines()[-1])
if p.returncode == 0:
    subprocess.run(["systemctl", "reload", "nginx"], capture_output=True)
    print("已重载")

# 自测:看 Content-Type / Disposition;并模拟浏览器请求(带 Accept: text/html)
for u in ("/openapi.yaml", "/openapi.json"):
    r = subprocess.run(["curl", "-s", "-D", "-", "-o", "/dev/null", "-H",
                        "Accept: text/html,application/xhtml+xml", f"http://127.0.0.1:8080{u}"],
                       capture_output=True, text=True)
    head = [l.strip() for l in r.stdout.splitlines()
            if l.lower().startswith(("http/", "content-type", "content-disposition", "content-length"))]
    print(f"  {u}")
    for h in head:
        print("    " + h)

# 内容抽样:确认是原始 YAML 文本 + 中文可读
body = subprocess.run(["curl", "-s", "http://127.0.0.1:8080/openapi.yaml"],
                      capture_output=True, text=True).stdout
print("  首行:", body.splitlines()[0])
print("  中文抽样:", next((l.strip()[:60] for l in body.splitlines() if "更新内容" in l), "(未找到)"))
