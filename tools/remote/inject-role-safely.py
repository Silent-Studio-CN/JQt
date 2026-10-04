#!/usr/bin/env python3
"""回滚 + 换一种安全做法:
  上一版用正则改函数体,破坏了页面 JS(备份 ✅ / 改后 ❌)。
  现在改为**注入一段独立脚本**:
    · 探测 /api/whoami,记录是否管理员
    · 非管理员:隐藏管理面板与"退出所有会话"
    · 非管理员:包一层 fetch,拦截管理类接口(返回 403 提示,不再让页面报错)
  完全不修改页面原有代码 -> 不可能破坏它。
"""
import io
import os
import shutil
import subprocess

PAGE = "/var/www/jqt/account/index.html"
BAK = PAGE + ".bak"

# ① 回滚
if os.path.exists(BAK):
    shutil.copy2(BAK, PAGE)
    print("① 已回滚到备份")
    r = subprocess.run(["node", "--check", "/dev/stdin"], input=io.open(PAGE, encoding="utf-8").read()
                       if False else "", capture_output=True, text=True)

# ② 注入独立脚本(放在 </body> 前)
s = io.open(PAGE, encoding="utf-8").read()
s = s.replace("<script>\n/* 角色感知", "<script>\n/* 角色感知(已废弃)")
# 去掉旧注入(若回滚不彻底)
if "JQT_ROLE_INJECTED" in s:
    print("   旧注入仍在,先移除")
    i = s.find("<!-- JQT_ROLE_INJECTED -->")
    j = s.find("<!-- /JQT_ROLE_INJECTED -->")
    if i != -1 and j != -1:
        s = s[:i] + s[j + len("<!-- /JQT_ROLE_INJECTED -->"):]

INJECT = '''<!-- JQT_ROLE_INJECTED -->
<script>
/* 角色感知(独立脚本,不改动页面原有代码):
   非管理员(silent 之外)隐藏管理面板,并拦截管理类接口请求 */
(function () {
  var ADMIN_APIS = ['/api/status', '/api/account/audit', '/api/account/service',
                    '/api/account/brand', '/api/account/logout-all'];
  var isAdmin = null;

  var origFetch = window.fetch ? window.fetch.bind(window) : null;
  if (origFetch) {
    window.fetch = function (input, init) {
      var url = (typeof input === 'string') ? input : (input && input.url) || '';
      if (isAdmin === false && ADMIN_APIS.some(function (p) { return url.indexOf(p) === 0; })) {
        return Promise.resolve(new Response(
          JSON.stringify({ ok: false, msg: '该操作仅管理员可用' }),
          { status: 403, headers: { 'Content-Type': 'application/json' } }));
      }
      return origFetch(input, init);
    };
  }

  function hideAdminUi() {
    if (isAdmin !== false) return;
    document.querySelectorAll('.card').forEach(function (c) {
      var t = c.textContent || '';
      if (t.indexOf('品牌') >= 0 || t.indexOf('服务') >= 0 || t.indexOf('审计') >= 0) {
        c.style.display = 'none';
      }
    });
    ['doLogoutAll', 'status', 'audit', 'svcList'].forEach(function (id) {
      var el = document.getElementById(id);
      if (el) el.style.display = 'none';
    });
  }

  function boot() {
    fetch('/api/whoami').then(function (r) { return r.json(); }).then(function (w) {
      isAdmin = !!(w && w.user === 'silent');
      hideAdminUi();
    }).catch(function () { isAdmin = null; });
  }
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot);
  } else {
    boot();
  }
})();
</script>
<!-- /JQT_ROLE_INJECTED -->
'''
if "JQT_ROLE_INJECTED" not in s:
    s = s.replace("</body>", INJECT + "</body>", 1)
    io.open(PAGE, "w", encoding="utf-8", newline="\n").write(s)
    print("② 已注入独立角色脚本")
else:
    print("② 注入已存在")

# ③ 校验:原脚本与注入脚本都要能通过 node --check
print("③ JS 校验:")
code = r'''
import io, re, subprocess, tempfile, os
s = io.open("/var/www/jqt/account/index.html", encoding="utf-8").read()
blocks = re.findall(r"<script>(.*?)</script>", s, re.S)
bad = 0
for i, b in enumerate(blocks, 1):
    with tempfile.NamedTemporaryFile("w", suffix=".js", delete=False, encoding="utf-8") as f:
        f.write(b); p = f.name
    r = subprocess.run(["node", "--check", p], capture_output=True, text=True)
    ok = r.returncode == 0
    print(f"   script#{i} ({len(b)} 字节): {'✅ 通过' if ok else '❌ ' + r.stderr.strip().splitlines()[0][:80]}")
    if not ok: bad += 1
    os.unlink(p)
print("   结论:", "全部通过" if bad == 0 else f"{bad} 段失败")
print("   括号配平: {}=%d ()=%d" % (s.count('{')-s.count('}'), s.count('(')-s.count(')')))
'''
r = subprocess.run(["python3", "-c", code], capture_output=True, text=True)
print(r.stdout.rstrip() or r.stderr.strip()[:300])
