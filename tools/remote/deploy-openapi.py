#!/usr/bin/env python3
"""部署 /openapi.yaml:放到站点根目录 + nginx 指定 YAML 类型 + 校验语法"""
import io
import os
import subprocess

SRC = "/tmp/openapi.yaml"
DST = "/var/www/jqt/openapi.yaml"

# ① 安装文件
subprocess.run(["install", "-m", "0644", "-o", "root", "-g", "root", SRC, DST], check=True)
print("① 已安装:", DST, os.path.getsize(DST), "字节")

# ② YAML 语法校验(顺便统计接口数)
try:
    import yaml
    doc = yaml.safe_load(io.open(DST, encoding="utf-8"))
    paths = doc.get("paths", {})
    print(f"② YAML 校验通过:openapi={doc.get('openapi')} 版本={doc['info']['version']}")
    print(f"   路径 {len(paths)} 个:", ", ".join(sorted(paths)))
    n = sum(len([m for m in v if m in ("get", "post", "put", "delete")]) for v in paths.values())
    print(f"   操作 {n} 个;tags {len(doc.get('tags', []))} 个;schemas {len(doc['components']['schemas'])} 个")
except ImportError:
    print("② 无 pyyaml,跳过校验")
except Exception as e:
    print("② ⚠️ YAML 校验失败:", str(e)[:400])

# ③ nginx:给 .yaml 正确的 Content-Type
P = "/etc/nginx/sites-available/jqt"
s = io.open(P, encoding="utf-8").read()
if "openapi.yaml" not in s:
    anchor = "    location / { try_files $uri $uri/ =404; }\n"
    add = ("    # 接口文档(公开)\n"
           "    location = /openapi.yaml {\n"
           "        default_type application/yaml;\n"
           "        add_header Cache-Control \"no-cache\";\n"
           "    }\n\n" + anchor)
    s = s.replace(anchor, add, 1)
    io.open(P, "w", encoding="utf-8").write(s)
    print("③ nginx 已加 /openapi.yaml 类型声明")
else:
    print("③ nginx 已存在该 location")

p = subprocess.run(["nginx", "-t"], capture_output=True, text=True)
print("   nginx -t:", (p.stderr or "ok").strip().splitlines()[-1])
if p.returncode == 0:
    subprocess.run(["systemctl", "reload", "nginx"], capture_output=True)
    print("   nginx 已重载")

# ④ 自测
for url in ("http://127.0.0.1:8080/openapi.yaml",):
    r = subprocess.run(["curl", "-s", "-o", "/dev/null", "-w", "%{http_code} %{content_type} %{size_download}",
                        url], capture_output=True, text=True)
    print(f"④ {url} -> {r.stdout}")
