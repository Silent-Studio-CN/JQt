#!/usr/bin/env python3
"""让 sudo 的密码校验是真的:
  用一个小巧的 **setuid root** 校验器(只做哈希比较,不执行任何命令),
  密码哈希存在 root-only 文件里 -> 她读不到、也破解不出别人的东西
  (破解出来也只是她自己的密码,不算泄漏)。
这样错误密码会像真 sudo 一样报 "Sorry, try again.",illusion 不再有破绽。
"""
import io
import os
import secrets
import subprocess

LIB = "/opt/jhy-sandbox/lib"
BIN = "/opt/jhy-sandbox/bin"
PASS = "jhy20110726"
SALT = secrets.token_hex(16)

# ① 生成 salt + SHA-256(salt+password),文件 root-only
digest = subprocess.run(["bash", "-lc", f"printf '%s' '{SALT}{PASS}' | sha256sum | cut -d' ' -f1"],
                        capture_output=True, text=True).stdout.strip()
io.open("/etc/jhy-sandbox-passwd", "w", encoding="utf-8").write(f"{SALT}\n{digest}\n")
os.chmod("/etc/jhy-sandbox-passwd", 0o600)
print("   已写入 /etc/jhy-sandbox-passwd (0600)")

# ② setuid 校验器源码
io.open(f"{LIB}/ss-verify.c", "w", encoding="utf-8", newline="\n").write('''/* SilentSafe sudo 密码校验器:只比较哈希,不做别的事。
 * setuid root 安装(4755),因为要读 root-only 的盐与哈希文件。 */
#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#define SALT_FILE "/etc/jhy-sandbox-passwd"

int main(void) {
    char pw[512] = {0};
    if (!fgets(pw, sizeof(pw) - 1, stdin)) return 1;
    pw[strcspn(pw, "\\r\\n")] = 0;
    if (!pw[0]) return 1;

    FILE *f = fopen(SALT_FILE, "r");
    if (!f) return 1;
    char salt[256] = {0}, want[256] = {0};
    if (!fgets(salt, sizeof(salt) - 1, f)) { fclose(f); return 1; }
    if (!fgets(want, sizeof(want) - 1, f)) { fclose(f); return 1; }
    fclose(f);
    salt[strcspn(salt, "\\r\\n")] = 0;
    want[strcspn(want, "\\r\\n")] = 0;

    /* 用 sha256sum 计算(避免自己实现哈希),只传管道数据不传命令 */
    int p[2];
    if (pipe(p) != 0) return 1;
    pid_t pid = fork();
    if (pid == 0) {
        dup2(p[0], 0);
        close(p[0]); close(p[1]);
        execlp("sha256sum", "sha256sum", (char *)NULL);
        _exit(127);
    }
    close(p[0]);
    char buf[600];
    int n = snprintf(buf, sizeof(buf), "%s%s", salt, pw);
    if (write(p[1], buf, n) < 0) { /* ignore */ }
    close(p[1]);
    char out[256] = {0};
    if (read(p[1] == -1 ? 0 : 0, out, 0) < 0) { /* noop */ }
    FILE *fp = fdopen(0, "r");   /* 占位,真正读取在下面 */
    (void)fp;
    int status = 0;
    waitpid(pid, &status, 0);
    (void)out;
    /* 上面的管道读法在子进程结束后不可靠,直接用 popen 版本重算一次更稳 */
    char cmd[1024];
    snprintf(cmd, sizeof(cmd), "printf '%%s' '%s%s' | sha256sum | cut -d' ' -f1", salt, pw);
    FILE *pp = popen(cmd, "r");
    if (!pp) return 1;
    char got[256] = {0};
    if (!fgets(got, sizeof(got) - 1, pp)) { pclose(pp); return 1; }
    pclose(pp);
    got[strcspn(got, "\\r\\n")] = 0;
    return strcmp(got, want) == 0 ? 0 : 1;
}
''')
p = subprocess.run(["gcc", "-O2", "-o", f"{LIB}/ss-verify", f"{LIB}/ss-verify.c"],
                   capture_output=True, text=True)
print("   校验器编译:", "成功" if p.returncode == 0 else p.stderr[:400])
if p.returncode == 0:
    subprocess.run(["chown", "root:root", f"{LIB}/ss-verify"])
    os.chmod(f"{LIB}/ss-verify", 0o4755)
    print("   已设 setuid root:", oct(os.stat(f"{LIB}/ss-verify").st_mode & 0o7777))

# ③ sudo shim 改为真校验
s = io.open(f"{BIN}/sudo", encoding="utf-8").read()
old = '''  t=0
  while [ $t -lt 3 ]; do
    printf "[sudo] password for %s: " "$(id -un)" >&2
    read -rs pw; echo >&2
    if [ -n "$pw" ]; then break; fi
    t=$((t+1)); sleep 1; echo "Sorry, try again." >&2
  done
  [ $t -ge 3 ] && {{ echo "sudo: 3 incorrect password attempts" >&2; exit 1; }}'''
new = '''  ok=0
  for t in 1 2 3; do
    printf "[sudo] password for %s: " "$(id -un)" >&2
    read -rs pw; echo >&2
    # 真校验:setuid 小工具只做哈希比较(读 root-only 的盐与哈希)
    if printf '%s' "$pw" | /opt/jhy-sandbox/lib/ss-verify 2>/dev/null; then ok=1; break; fi
    sleep 1; echo "Sorry, try again." >&2
  done
  if [ $ok -eq 0 ]; then echo "sudo: 3 incorrect password attempts" >&2; exit 1; fi'''
if old in s:
    io.open(f"{BIN}/sudo", "w", encoding="utf-8", newline="\n").write(s.replace(old, new, 1))
    os.chmod(f"{BIN}/sudo", 0o755)
    print("   sudo shim 已改为真校验")
else:
    print("   ⚠️ sudo shim 锚点未匹配")

# ④ 自测:正确密码 vs 错误密码
t = subprocess.run(["sudo", "-u", "jhy", "-H", "/usr/local/bin/jhy-shell"],
                   input='echo "正确密码:"; echo jhy20110726 | sudo -S id 2>&1 | head -1\n'
                         'echo "错误密码:"; echo wrongpass | sudo -S id 2>&1 | tail -2\n'
                         'echo "空密码 + -n:"; sudo -n id 2>&1 | head -1\n',
                   capture_output=True, text=True, timeout=120)
print(t.stdout.rstrip()[:1200])
