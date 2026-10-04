#!/usr/bin/env python3
"""节点侧收尾:
  ① default token 放开明文 HTTP(隧道在服务商侧终止 TLS 时 nginx 只能看到 http;
     严格模式随时可用 allow_http=false 切回)
  ② 清掉 nginx 里重复的 sub_filter_types text/html 告警
"""
import io
import json
import os
import subprocess

# ① token
TF = "/etc/jqt/api-tokens.json"
data = json.load(io.open(TF, encoding="utf-8"))
if data.get("default", {}).get("allow_http") is not True:
    data["default"]["allow_http"] = True
    data["default"]["note"] = "自动生成;allow_http=true 以便隧道场景直接 curl(可改回 false 强制 https)"
    with io.open(TF, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    os.chmod(TF, 0o600)
    print("已放开 default token 的明文 HTTP")
else:
    print("default token 已允许明文")

# ② nginx 重复 MIME 告警
P = "/etc/nginx/sites-available/jqt"
s = io.open(P, encoding="utf-8").read()
n = s.count("        sub_filter_types text/html;\n")
if n:
    s = s.replace("        sub_filter_types text/html;\n", "")
    io.open(P, "w", encoding="utf-8").write(s)
    print(f"已清理 {n} 处重复的 sub_filter_types(text/html 本就是默认值)")
else:
    print("无需清理 sub_filter_types")

print(subprocess.run(["nginx", "-t"], capture_output=True, text=True).stderr.strip()[:400])
