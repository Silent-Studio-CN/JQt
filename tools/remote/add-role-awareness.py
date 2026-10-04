#!/usr/bin/env python3
"""账号页角色感知:非管理员(silent 之外)自动隐藏管理面板与"退出所有会话",
并让调用管理接口的渲染函数直接跳过(否则页面上会显示 403 报错)。
"""
import io
import os
import re
import shutil
import subprocess

PAGE = "/var/www/jqt/account/index.html"
shutil.copy2(PAGE, PAGE + ".bak")
s = io.open(PAGE, encoding="utf-8").read()

# ---------------- ① 角色探测 + 隐藏 ----------------
if "JQT_IS_ADMIN" not in s:
    role_js = '''<script>
/* 角色感知:只有管理员(silent)才显示品牌/服务/审计等管理面板 */
window.JQT_IS_ADMIN = null;
function jqtApplyRole() {
  if (window.JQT_IS_ADMIN !== false) return;
  document.querySelectorAll('.card').forEach(function (c) {
    var t = c.textContent || '';
    if (t.indexOf('品牌') >= 0 || t.indexOf('服务') >= 0 || t.indexOf('审计') >= 0) {
      c.style.display = 'none';
    }
  });
  ['doLogoutAll', 'status', 'audit', 'svcList', 'brandLine'].forEach(function (id) {
    var el = document.getElementById(id);
    if (el && id !== 'brandLine') el.style.display = 'none';
  });
  var t = document.getElementById('title');
  if (t) t.textContent = '我的账号';
}
fetch('/api/whoami').then(function (r) { return r.json(); }).then(function (w) {
  window.JQT_IS_ADMIN = (w && w.user === 'silent');
  jqtApplyRole();
}).catch(function () { window.JQT_IS_ADMIN = null; });
</script>
'''
    # 插到第一个 <script> 之前(保证守卫先用上)
    i = s.find("<script>")
    s = s[:i] + role_js + s[i:] if i != -1 else s + role_js
    print("① 已插入角色探测脚本")

# ---------------- ② 管理类渲染函数加守卫 ----------------
ADMIN_HINTS = ("/api/status", "/api/account/audit", "/api/account/brand",
               "/api/account/service", "svcList")
guard = "  if (window.JQT_IS_ADMIN === false) return;\n"
count = 0
def guard_fn(m):
    global count
    head, body_start, body = m.group(1), m.group(2), m.group(3)
    if any(h in body for h in ADMIN_HINTS):
        if "JQT_IS_ADMIN === false) return" in body:
            return m.group(0)
        count += 1
        return head + body_start + guard + body
    return m.group(0)

# 匹配 async function xxx(...) {  或  function xxx(...) {  的整个块(按大括号配平近似:取到首个 \n}\n)
pattern = re.compile(r'((?:async\s+)?function\s+\w+\s*\([^)]*\)\s*\{)(\n)((?:.|\n)*?)\n\}', re.M)
s = pattern.sub(guard_fn, s)
print(f"② 已给 {count} 个管理类函数加守卫")

io.open(PAGE, "w", encoding="utf-8").write(s)
os.chmod(PAGE, 0o644)

# ---------------- ③ 语法粗检 + 关键点确认 ----------------
ok = []
ok.append(("角色脚本", "JQT_IS_ADMIN" in s))
ok.append(("隐藏逻辑", "jqtApplyRole" in s))
ok.append(("守卫注入", "JQT_IS_ADMIN === false) return" in s))
for name, v in ok:
    print(f"   {name}: {'✅' if v else '❌'}")
print("   备份:", PAGE + ".bak")
