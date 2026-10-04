#!/usr/bin/env python3
"""节点侧安装并校验 openapi.yaml(避免在 SSH 里套复杂引号)"""
import io
import json
import subprocess

subprocess.run(["install", "-m", "0644", "-o", "root", "-g", "root",
                "/tmp/openapi.yaml", "/var/www/jqt/openapi.yaml"], check=True)
try:
    import yaml
    doc = yaml.safe_load(io.open("/var/www/jqt/openapi.yaml", encoding="utf-8"))
    paths = doc["paths"]
    io.open("/var/www/jqt/openapi.json", "w", encoding="utf-8").write(
        json.dumps(doc, ensure_ascii=False, indent=2, default=str))
    print("节点校验通过:路径", len(paths), "| 含 /terminal/:", "/terminal/" in paths,
          "| 含 /api/entry:", "/api/entry" in paths)
except Exception as e:
    print("❌ 校验失败:", str(e)[:250])
for u in ("/openapi.yaml", "/openapi.json"):
    r = subprocess.run(["curl", "-s", "-o", "/dev/null", "-w", "%{http_code} %{content_type} %{size_download}",
                        f"http://127.0.0.1:8080{u}"], capture_output=True, text=True)
    print(f"  {u:16s} -> {r.stdout}")
