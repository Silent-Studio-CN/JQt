#!/usr/bin/env python3
"""收尾:① 修 /openapi.json(日期对象不可序列化)② 结构校验 ③ 确认在线可看"""
import io
import json
import os
import subprocess

WWW = "/var/www/jqt"

# ① JSON:default=str 兜住 date 之类
try:
    import yaml
    doc = yaml.safe_load(io.open(f"{WWW}/openapi.yaml", encoding="utf-8"))
    io.open(f"{WWW}/openapi.json", "w", encoding="utf-8").write(
        json.dumps(doc, ensure_ascii=False, indent=2, default=str))
    os.chmod(f"{WWW}/openapi.json", 0o644)
    print("① /openapi.json 已生成:", os.path.getsize(f"{WWW}/openapi.json"), "字节")
except Exception as e:
    print("① 失败:", str(e)[:200])

# ② 结构校验(OpenAPI 必备字段 + 每个操作要有 responses)
errs = []
doc = yaml.safe_load(io.open(f"{WWW}/openapi.yaml", encoding="utf-8"))
if not str(doc.get("openapi", "")).startswith("3."):
    errs.append("openapi 版本缺失或不是 3.x")
info = doc.get("info", {})
for k in ("title", "version"):
    if not info.get(k):
        errs.append(f"info.{k} 缺失")
paths = doc.get("paths") or {}
if not paths:
    errs.append("paths 为空")
ops = 0
for path, item in paths.items():
    if not path.startswith("/"):
        errs.append(f"路径不以 / 开头: {path}")
    for m, op in item.items():
        if m not in ("get", "post", "put", "delete", "patch"):
            continue
        ops += 1
        if not op.get("responses"):
            errs.append(f"{m.upper()} {path} 缺 responses")
        if not (op.get("summary") or op.get("description")):
            errs.append(f"{m.upper()} {path} 缺 summary/description")
# 引用的 securityScheme 都要存在
schemes = set((doc.get("components", {}).get("securitySchemes") or {}).keys())
for path, item in paths.items():
    for m, op in item.items():
        if m not in ("get", "post", "put", "delete", "patch"):
            continue
        for sec in (op.get("security") or []):
            for name in sec:
                if name not in schemes:
                    errs.append(f"{m.upper()} {path} 引用未定义的 securityScheme: {name}")
print(f"② 结构校验:{len(paths)} 路径 / {ops} 操作 / {len(schemes)} 鉴权方案 -> "
      + ("全部通过 ✅" if not errs else f"{len(errs)} 个问题"))
for e in errs[:10]:
    print("   -", e)

# ③ 在线可看性自测
print("③ 在线自测:")
for u in ("/docs/", "/openapi.yaml", "/openapi.json", "/assets/redoc.standalone.js"):
    r = subprocess.run(["curl", "-s", "-o", "/dev/null", "-w", "%{http_code} %{content_type} %{size_download}",
                        f"http://127.0.0.1:8080{u}"], capture_output=True, text=True)
    print(f"   {u:34s} -> {r.stdout}")

# 页面引用的资源是否都可取
page = subprocess.run(["curl", "-s", "http://127.0.0.1:8080/docs/"], capture_output=True, text=True).stdout
for ref in ("/assets/redoc.standalone.js", "/openapi.yaml", "/openapi.json"):
    print(f"   页面引用 {ref:34s} -> {'✅ 已引用' if ref in page else '❌ 未找到'}")
js = subprocess.run(["curl", "-s", "http://127.0.0.1:8080/assets/redoc.standalone.js"],
                    capture_output=True, text=True).stdout
print("   渲染器内容检查: 含 Redoc 标识 ->", "✅" if "Redoc" in js[:200000] else "❌")
