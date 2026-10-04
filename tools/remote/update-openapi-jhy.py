#!/usr/bin/env python3
"""更新 openapi.yaml:补 /terminal/、/api/entry,并标注管理类接口的权限"""
import io
import subprocess

Y = "/var/www/jqt/openapi.yaml"
s = io.open(Y, encoding="utf-8").read()
done = []

# ① /terminal/ 路径(插在 /silent/ 之后)
if "/terminal/:" not in s:
    anchor = """  /account/:
    get:
      tags: [页面]
      summary: 账号与设置页（需登录）"""
    add = """  /terminal/:
    get:
      tags: [页面]
      summary: 普通用户（jhy）的控制台（需登录）
      description: |
        与 `/console/` 同类的网页终端，但运行在**受限用户自己的沙箱**里（独立实例）。
        每个账号只能进自己的入口：普通用户访问 `/console/`、`/silent/` 会被自动跳回本入口；
        反过来管理员访问 `/terminal/` 也会被跳回 `/console/`。
      responses:
        "200":
          description: 终端页面或登录页
          content:
            text/html:
              schema: { type: string }

  /api/entry:
    get:
      tags: [认证]
      summary: 跳转到当前账号的入口（需登录）
      description: |
        已登录时 302 到本人入口（`/console/` 或 `/terminal/`），未登录时 302 到 `/login.html`。
        nginx 在入口越权（403）时用它做自动纠正，避免死循环。
      responses:
        "302":
          description: 跳转
          headers:
            Location: { schema: { type: string } }

""" + anchor
    if anchor in s:
        s = s.replace(anchor, add, 1)
        done.append("/terminal/ 与 /api/entry")
    else:
        print("⚠️ /account/ 锚点未匹配")

# ② 管理类接口标注"仅管理员"
for path, note in (("/api/status:", "**仅管理员**"),
                   ("/api/account/audit:", "**仅管理员**"),
                   ("/api/account/brand:", "**仅管理员**"),
                   ("/api/account/service:", "**仅管理员**"),
                   ("/api/account/logout-all:", "**仅管理员**")):
    i = s.find(path)
    if i == -1:
        continue
    j = s.find("summary:", i)
    if j != -1 and note not in s[j:j + 200]:
        k = s.find("\n", j)
        s = s[:k] + f"      description: {note}（普通账号调用返回 403）" + s[k:]
        done.append(path.strip(":"))

# ③ whoami 返回里补 entry
s = s.replace("""                  ok: { type: boolean }
                  user: { type: string }
              examples:
                logged: { value: { ok: true, user: "silent" } }""",
"""                  ok: { type: boolean }
                  user: { type: string }
                  entry: { type: string, description: 该账号的入口路径 }
              examples:
                logged: { value: { ok: true, user: "silent", entry: "/console/" } }
                jhy: { value: { ok: true, user: "jhy", entry: "/terminal/" } }""")

io.open(Y, "w", encoding="utf-8", newline="\n").write(s)
try:
    import yaml
    doc = yaml.safe_load(io.open(Y, encoding="utf-8"))
    print("已更新:" + "、".join(done))
    print(f"校验通过:路径 {len(doc['paths'])} 个;含 /terminal/ = {'/terminal/' in doc['paths']};"
          f" /api/entry = {'/api/entry' in doc['paths']}")
    # 同步 JSON
    import json
    io.open("/var/www/jqt/openapi.json", "w", encoding="utf-8").write(
        json.dumps(doc, ensure_ascii=False, indent=2, default=str))
    print("已同步 /openapi.json")
except Exception as e:
    print("⚠️ YAML 校验失败:", str(e)[:200])
