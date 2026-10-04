#!/usr/bin/env python3
"""sudo 密码真校验(修正版):
  · ss-verify:极简 setuid 校验器,fork+exec sha256sum 并用管道喂数据(不经 shell)
  · sudo shim 整份重写(含缓存、-n、-l、真校验)
"""
import io
import os
import subprocess

LIB, BIN = "/opt/jhy-sandbox/lib", "/opt/jhy-sandbox/bin"

io.open(f"{LIB}/ss-verify.c", "w", encoding="utf-8", newline="\n").write(r'''/* SilentSafe sudo 密码校验器(极简、setuid root)
 * 只做一件事:比较 stdin 的密码与 root-only 文件中的 盐+SHA256。
 * 不执行任何用户输入构造的命令:fork + exec sha256sum,数据走管道。 */
#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/wait.h>
#include <unistd.h>

#define SALT_FILE "/etc/jhy-sandbox-passwd"

int main(void) {
    char pw[512] = {0};
    if (!fgets(pw, sizeof(pw) - 1, stdin)) return 1;
    pw[strcspn(pw, "\r\n")] = 0;
    if (!pw[0]) return 1;

    FILE *f = fopen(SALT_FILE, "r");
    if (!f) return 1;
    char salt[256] = {0}, want[256] = {0};
    if (!fgets(salt, sizeof(salt) - 1, f) || !fgets(want, sizeof(want) - 1, f)) {
        fclose(f); return 1;
    }
    fclose(f);
    salt[strcspn(salt, "\r\n")] = 0;
    want[strcspn(want, "\r\n")] = 0;

    char buf[1024];
    int n = snprintf(buf, sizeof(buf), "%s%s", salt, pw);
    if (n <= 0 || n >= (int)sizeof(buf)) return 1;

    int pin[2], pout[2];
    if (pipe(pin) != 0 || pipe(pout) != 0) return 1;
    pid_t pid = fork();
    if (pid < 0) return 1;
    if (pid == 0) {
        dup2(pin[0], 0);
        dup2(pout[1], 1);
        close(pin[0]); close(pin[1]); close(pout[0]); close(pout[1]);
        execl("/usr/bin/sha256sum", "sha256sum", (char *)NULL);
        _exit(127);
    }
    close(pin[0]); close(pout[1]);
    ssize_t wr = write(pin[1], buf, (size_t)n);
    close(pin[1]);
    char got[256] = {0};
    ssize_t rd = read(pout[0], got, sizeof(got) - 1);
    close(pout[0]);
    int st = 0;
    waitpid(pid, &st, 0);
    if (wr < 0 || rd <= 0) return 1;
    got[strcspn(got, " \r\n")] = 0;
    return strcmp(got, want) == 0 ? 0 : 1;
}
''')
p = subprocess.run(["gcc", "-O2", "-o", f"{LIB}/ss-verify", f"{LIB}/ss-verify.c"],
                   capture_output=True, text=True)
print("   编译:", "成功" if p.returncode == 0 else p.stderr[:500])
if p.returncode == 0:
    subprocess.run(["chown", "root:root", f"{LIB}/ss-verify"], capture_output=True)
    os.chmod(f"{LIB}/ss-verify", 0o4755)
    print("   权限:", oct(os.stat(f"{LIB}/ss-verify").st_mode & 0o7777))
    # 直接验一下
    for pw in ("jhy20110726", "wrongpass"):
        r = subprocess.run([f"{LIB}/ss-verify"], input=pw, capture_output=True, text=True)
        print(f"   ss-verify('{pw}') -> {'通过' if r.returncode == 0 else '拒绝'}")

# sudo shim 整份重写
SUDO = '''#!/bin/bash
# SilentSafe 沙箱 sudo:在**沙箱内**的嵌套 user namespace 里提升为 uid 0。
# 宿主上没有任何真实 root 能力;命令行中的危险操作仍会被 SilentSafe 拦截。
CACHE="$HOME/.cache/.sudo-ts"

case " $* " in
  *" -l "*|*" --list "*)
    echo "Matching Defaults entries for $(id -un) on compute-node-01:"
    echo "    env_reset, mail_badpass, secure_path=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
    echo ""
    echo "User $(id -un) may run the following commands on compute-node-01:"
    echo "    (ALL : ALL) ALL"
    exit 0 ;;
esac

NONINTERACTIVE=0; ARGS=()
for a in "$@"; do
  case "$a" in
    -n|--non-interactive) NONINTERACTIVE=1 ;;
    -S|--stdin|-E|--preserve-env|-H|--set-home|-b|--background) : ;;
    -k|--reset-timestamp) rm -f "$CACHE" ;;
    -v|--validate) mkdir -p "$(dirname "$CACHE")"; date +%s > "$CACHE"; exit 0 ;;
    --) : ;;
    *) ARGS+=("$a") ;;
  esac
done

mkdir -p "$(dirname "$CACHE")"
fresh=0
if [ -f "$CACHE" ]; then
  ts=$(cat "$CACHE" 2>/dev/null || echo 0)
  [ $(( $(date +%s) - ts )) -lt 300 ] && fresh=1
fi

if [ $fresh -eq 0 ]; then
  if [ $NONINTERACTIVE -eq 1 ]; then
    echo "sudo: a password is required" >&2; exit 1
  fi
  ok=0
  for t in 1 2 3; do
    printf "[sudo] password for %s: " "$(id -un)" >&2
    read -rs pw; echo >&2
    if printf '%s' "$pw" | /opt/jhy-sandbox/lib/ss-verify 2>/dev/null; then ok=1; break; fi
    sleep 1; echo "Sorry, try again." >&2
  done
  if [ $ok -eq 0 ]; then echo "sudo: 3 incorrect password attempts" >&2; exit 1; fi
  date +%s > "$CACHE"
fi

[ ${#ARGS[@]} -eq 0 ] && { echo "usage: sudo command"; exit 1; }
exec unshare -Ur --map-root-user -- /bin/bash -c "$(printf '%q ' "${ARGS[@]}")"
'''
io.open(f"{BIN}/sudo", "w", encoding="utf-8", newline="\n").write(SUDO)
os.chmod(f"{BIN}/sudo", 0o755)
print("   sudo shim 已重写")

# 沙箱内验证
t = subprocess.run(["sudo", "-u", "jhy", "-H", "/usr/local/bin/jhy-shell"],
                   input='rm -f ~/.cache/.sudo-ts\n'
                         'echo "正确密码 → $(echo jhy20110726 | sudo -S id 2>&1 | head -1)"\n'
                         'rm -f ~/.cache/.sudo-ts\n'
                         'echo "错误密码 → $(echo wrongpass | sudo -S id 2>&1 | tail -1)"\n'
                         'echo "错误后再用对密码 → $(echo jhy20110726 | sudo -S id 2>&1 | head -1)"\n'
                         'echo "sudo -n(无缓存时)→ $(sudo -n id 2>&1 | head -1)"\n',
                   capture_output=True, text=True, timeout=180)
print(t.stdout.rstrip()[:1500])
if t.stderr.strip():
    print("   [stderr]", t.stderr.strip()[:300])
