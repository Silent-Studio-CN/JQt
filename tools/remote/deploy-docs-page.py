#!/usr/bin/env python3
"""部署在线接口文档页 /docs/(Redoc 渲染器自托管,不依赖外网 CDN)
   · 渲染器资源下载到 /var/www/jqt/assets/redoc.standalone.js(多镜像回退)
   · /docs/index.html 加载本地渲染器 + /openapi.yaml
   · 附加:顶部工具栏(原始 YAML / JSON 视图 / 刷新)、加载失败时的降级提示
"""
import io
import os
import subprocess

WWW = "/var/www/jqt"
ASSET = f"{WWW}/assets/redoc.standalone.js"
DOCS = f"{WWW}/docs/index.html"

MIRRORS = [
    "https://cdn.jsdelivr.net/npm/redoc@2.1.5/bundles/redoc.standalone.js",
    "https://unpkg.com/redoc@2.1.5/bundles/redoc.standalone.js",
    "https://registry.npmmirror.com/redoc/-/redoc-2.1.5.tgz",
]

os.makedirs(f"{WWW}/assets", exist_ok=True)
os.makedirs(f"{WWW}/docs", exist_ok=True)

# ① 下载渲染器(多镜像)
ok = False
if os.path.exists(ASSET) and os.path.getsize(ASSET) > 200000:
    print("① 渲染器已存在:", os.path.getsize(ASSET), "字节")
    ok = True
else:
    for url in MIRRORS:
        print("   尝试:", url)
        r = subprocess.run(["curl", "-fsSL", "--max-time", "60", "-o", ASSET, url],
                           capture_output=True, text=True)
        size = os.path.getsize(ASSET) if os.path.exists(ASSET) else 0
        if r.returncode == 0 and size > 200000:
            print(f"   ✅ 已下载 {size} 字节")
            ok = True
            break
        print("   失败:", (r.stderr or "").strip()[:120])
if not ok:
    print("   ⚠️ 未取得本地渲染器 -> 页面将回退到 CDN")

# ② 文档页
io.open(DOCS, "w", encoding="utf-8", newline="\n").write('''<!doctype html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>接口文档 · SilentRemoveKit</title>
<style>
 :root{--bg:#0b1220;--card:#151f33;--line:#24324d;--fg:#e6edf7;--dim:#8fa3bf;--accent:#38bdf8}
 *{box-sizing:border-box}
 body{margin:0;background:var(--bg);color:var(--fg);font-family:system-ui,"Microsoft YaHei",sans-serif}
 header{position:sticky;top:0;z-index:9;display:flex;gap:14px;align-items:center;flex-wrap:wrap;
        padding:12px 18px;background:#101a2ee6;backdrop-filter:blur(6px);border-bottom:1px solid var(--line)}
 header b{font-size:15px}
 header .sp{flex:1}
 header a,header button{color:var(--accent);background:none;border:1px solid var(--line);
        border-radius:8px;padding:6px 11px;font-size:13px;text-decoration:none;cursor:pointer}
 header a:hover,header button:hover{border-color:var(--accent)}
 #note{padding:10px 18px;color:var(--dim);font-size:12.5px;border-bottom:1px solid var(--line)}
 #redoc{min-height:60vh}
 #fail{display:none;padding:24px 18px;line-height:1.9}
 #fail code{background:#0f1a2c;border:1px solid var(--line);border-radius:6px;padding:2px 6px}
</style>
</head>
<body>
<header>
  <b>SilentRemoveKit · 接口文档</b>
  <span class="sp"></span>
  <a href="/openapi.yaml" target="_blank">原始 YAML</a>
  <button id="json">JSON 视图</button>
  <button id="reload">重新加载</button>
  <a href="/">返回首页</a>
</header>
<div id="note">本页由 Redoc 渲染 <code>/openapi.yaml</code>;渲染器与文档都在这台服务器上,不依赖外部 CDN。</div>
<div id="redoc"></div>
<div id="fail">
  <h3>渲染器未能加载(或文档解析失败)</h3>
  <p>可以直接查看原始文件:<a href="/openapi.yaml">/openapi.yaml</a></p>
  <p>或用任意 OpenAPI 工具打开(Postman / Swagger Editor / Apifox)。</p>
  <p id="failmsg" style="color:#fca5a5"></p>
</div>

<script src="/assets/redoc.standalone.js"></script>
<script>
(function () {
  var SPEC = '/openapi.yaml';
  function showFail(msg) {
    document.getElementById('fail').style.display = 'block';
    document.getElementById('failmsg').textContent = msg || '';
    document.getElementById('redoc').style.display = 'none';
  }
  function render() {
    if (!window.Redoc) { showFail('Redoc 资源未加载(/assets/redoc.standalone.js)'); return; }
    document.getElementById('redoc').innerHTML = '';
    try {
      Redoc.init(SPEC, {
        hideDownloadButton: false,
        expandResponses: '200,400,401,403,429',
        pathInMiddlePanel: true,
        theme: { colors: { primary: { main: '#38bdf8' } }, typography: { fontSize: '14px' } }
      }, document.getElementById('redoc'), function (err) {
        if (err) showFail(String(err));
      });
    } catch (e) { showFail(String(e)); }
  }
  document.getElementById('reload').onclick = function () { location.reload(); };
  document.getElementById('json').onclick = function () { window.open(SPEC.replace('.yaml', '.json'), '_blank'); };
  render();
})();
</script>
</body>
</html>
''')
os.chmod(f"{WWW}/docs/index.html", 0o644)
print("② 已写入", DOCS)

# ③ 顺手提供 JSON 视图(/openapi.json 由 YAML 转换)
try:
    import yaml, json
    doc = yaml.safe_load(io.open(f"{WWW}/openapi.yaml", encoding="utf-8"))
    io.open(f"{WWW}/openapi.json", "w", encoding="utf-8").write(
        json.dumps(doc, ensure_ascii=False, indent=2))
    os.chmod(f"{WWW}/openapi.json", 0o644)
    print("③ 已生成 /openapi.json:", os.path.getsize(f"{WWW}/openapi.json"), "字节")
except Exception as e:
    print("③ JSON 生成失败:", str(e)[:200])

# ④ nginx:yaml/json 类型 + /docs/ 直达
P = "/etc/nginx/sites-available/jqt"
s = io.open(P, encoding="utf-8").read()
if "/openapi.json" not in s:
    old = '''    location = /openapi.yaml {
        # 空的 types 块让 default_type 生效(否则回落到 application/octet-stream)
        types { }
        default_type application/yaml;'''
    new = '''    location = /docs/ {
        types { }
        default_type text/html;
        try_files $uri $uri/ /docs/index.html;
    }
    location ~ ^/openapi\\.(yaml|json)$ {
        types { }
        default_type application/yaml;
        add_header Cache-Control "no-cache";'''
    if old in s:
        s = s.replace(old, new, 1)
        s = s.replace('        add_header Cache-Control "no-cache";\n    }\n\n    location / {',
                      '    }\n\n    location / {', 1)
        io.open(P, "w", encoding="utf-8").write(s)
        print("④ nginx 已加 /docs/ 与 /openapi.*")
if "application/json json" not in s:
    s = io.open(P, encoding="utf-8").read()
    # JSON 用默认类型即可,但显式声明更稳
    s = s.replace("    location ~ ^/openapi\\.(yaml|json)$ {\n        types { }\n        default_type application/yaml;",
                  "    location ~ ^/openapi\\.(yaml|json)$ {\n        types { application/yaml yaml; application/json json; }\n        default_type application/yaml;", 1)
    io.open(P, "w", encoding="utf-8").write(s)

p = subprocess.run(["nginx", "-t"], capture_output=True, text=True)
print("   nginx -t:", (p.stderr or "ok").strip().splitlines()[-1])
if p.returncode == 0:
    subprocess.run(["systemctl", "reload", "nginx"], capture_output=True)
    print("   nginx 已重载")

for u in ("/docs/", "/openapi.yaml", "/openapi.json", "/assets/redoc.standalone.js"):
    r = subprocess.run(["curl", "-s", "-o", "/dev/null", "-w", "%{http_code} %{content_type} %{size_download}",
                        f"http://127.0.0.1:8080{u}"], capture_output=True, text=True)
    print(f"   自测 {u:34s} -> {r.stdout}")
