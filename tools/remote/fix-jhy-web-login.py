#!/usr/bin/env python3
"""修 jhy 无法登录网页控制台:
  ① 登录壳要支持 `-c "命令"` —— 网关的密码校验走的是
     `setpriv --reuid=65534 su - jhy -c true`,su 会用**登录 shell** 执行命令;
     我的壳原来不认 -c,直接起了交互式沙箱 -> pexpect 永远等不到预期输出 -> 判成密码错误。
  ② /etc/shells 里登记该壳(su/PAM 的 pam_shells 会校验)。
"""
import io
import os
import subprocess

WRAPPER = "/usr/local/bin/jhy-shell"
SHELLS = "/etc/shells"

# ---------------- ① 支持 -c ----------------
s = io.open(WRAPPER, encoding="utf-8").read()
if '"-c"' not in s:
    anchor = 'ORIG="${SSH_ORIGINAL_COMMAND:-}"'
    add = '''# 兼容 "-c <命令>" 调用方式:su / scp / 各种工具会这样调用登录 shell。
# 少了这一段,网关的 su 密码校验(以及 scp)都会失败 —— 实测踩过。
if [ "${1:-}" = "-c" ] && [ -n "${2:-}" ]; then
  exec /usr/bin/bwrap "${SANDBOX[@]}" -- /bin/bash -c "$2"
fi

''' + anchor
    s = s.replace(anchor, add, 1)
    io.open(WRAPPER, "w", encoding="utf-8", newline="\n").write(s)
    os.chmod(WRAPPER, 0o755)
    print("① 登录壳已支持 -c")
else:
    print("① 已支持 -c")

p = subprocess.run(["bash", "-n", WRAPPER], capture_output=True, text=True)
print("   语法:", "通过" if p.returncode == 0 else p.stderr[:200])

# ---------------- ② /etc/shells ----------------
shells = io.open(SHELLS, encoding="utf-8").read() if os.path.exists(SHELLS) else ""
if WRAPPER not in shells:
    with io.open(SHELLS, "a", encoding="utf-8") as f:
        f.write(f"{WRAPPER}\n")
    print("② 已登记到 /etc/shells")
else:
    print("② 已在 /etc/shells")

# ---------------- ③ 直接验证网关的 check_password ----------------
code = '''
import importlib.util, sys
spec = importlib.util.spec_from_file_location("gw", "/usr/local/bin/jqt-auth.py")
# 只加载到类定义之前(避免启动 HTTP 服务)
src = open("/usr/local/bin/jqt-auth.py", encoding="utf-8").read()
ns = {}
exec(compile(src.split("class H(")[0], "gw", "exec"), ns)
print("   check_password(jhy, 正确密码) =", ns["check_password"]("jhy", "jhy20110726"))
print("   check_password(jhy, 错误密码) =", ns["check_password"]("jhy", "wrongpass"))
print("   check_password(silent, 任意) =", ns["check_password"]("silent", "x"))
'''
p = subprocess.run(["python3", "-c", code], capture_output=True, text=True)
print("③ 校验函数实测:")
print(p.stdout.rstrip() or p.stderr.strip()[:400])

# ---------------- ④ 直接测 su 那条链路 ----------------
print("④ su 链路实测:")
r = subprocess.run(["setpriv", "--reuid=65534", "--regid=65534", "--clear-groups",
                    "/bin/su", "-", "jhy", "-c", "true"], capture_output=True, text=True, timeout=20)
print("   su 退出码:", r.returncode, "| stderr:", (r.stderr or "").strip()[:120])
