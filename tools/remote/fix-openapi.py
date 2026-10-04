#!/usr/bin/env python3
"""修 openapi.yaml 的 YAML 语法(plain scalar 里的 ": ")+ nginx 的 YAML Content-Type"""
import io
import re
import subprocess

YAML = "/var/www/jqt/openapi.yaml"
NGINX = "/etc/nginx/sites-available/jqt"

# ① 把所有 plain scalar 里含 ": " 的值加引号(YAML 里那是嵌套映射的分隔符)
lines = io.open(YAML, encoding="utf-8").read().splitlines()
fixed = []
changed = 0
pat = re.compile(r'^(\s*)(description|summary|msg|line|value|title):\s+(.*)$')
for ln in lines:
    m = pat.match(ln)
    if m:
        indent, key, val = m.groups()
        if ": " in val and not (val.startswith('"') or val.startswith("'") or val.startswith("|")
                                or val.startswith(">") or val.startswith("[")):
            val = '"' + val.replace('"', '\\"') + '"'
            ln = f"{indent}{key}: {val}"
            changed += 1
    fixed.append(ln)
io.open(YAML, "w", encoding="utf-8", newline="\n").write("\n".join(fixed) + "\n")
print(f"① 已给 {changed} 处含冒号的值加引号")

# ② 校验
try:
    import yaml
    doc = yaml.safe_load(io.open(YAML, encoding="utf-8"))
    paths = doc.get("paths", {})
    ops = sum(len([m for m in v if m in ("get", "post", "put", "delete")]) for v in paths.values())
    print(f"② YAML 校验通过:版本 {doc['info']['version']},路径 {len(paths)} 个,操作 {ops} 个")
    print("   路径:", ", ".join(sorted(paths)))
except Exception as e:
    print("② ⚠️ 仍有语法问题:", str(e)[:300])

# ③ nginx:types 块 + default_type
s = io.open(NGINX, encoding="utf-8").read()
old = '''    location = /openapi.yaml {
        default_type application/yaml;'''
new = '''    location = /openapi.yaml {
        # 空的 types 块让 default_type 生效(否则回落到 application/octet-stream)
        types { }
        default_type application/yaml;'''
if "types { }" not in s:
    s = s.replace(old, new, 1)
    io.open(NGINX, "w", encoding="utf-8").write(s)
    print("③ nginx 已加 types {}")
p = subprocess.run(["nginx", "-t"], capture_output=True, text=True)
print("   nginx -t:", (p.stderr or "ok").strip().splitlines()[-1])
if p.returncode == 0:
    subprocess.run(["systemctl", "reload", "nginx"], capture_output=True)
    r = subprocess.run(["curl", "-s", "-o", "/dev/null", "-w", "%{http_code} %{content_type} %{size_download}",
                        "http://127.0.0.1:8080/openapi.yaml"], capture_output=True, text=True)
    print("④ 自测:", r.stdout)
    r2 = subprocess.run(["curl", "-s", "http://127.0.0.1:8080/openapi.yaml"], capture_output=True, text=True)
    print("   首行:", (r2.stdout or "").splitlines()[0] if r2.stdout else "(空)")
