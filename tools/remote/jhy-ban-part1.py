#!/usr/bin/env python3
"""SilentSafe 封禁机制:
  · ss-ban(setuid root,无参数):写入封禁时间戳(now+300s)+ 杀掉 uid 1001 的所有进程(踢下线)
  · block.sh:SS_BAN=1 时先打印提示,再调用 ss-ban
  · rm shim:递归删除关键目录/根 -> SS_ERR_ID_100 + 封禁
  · jhy-shell:封禁期内直接拒绝访问(显示剩余秒数)
  · 规则文件里可给任意规则加 ban 标记(便于以后扩展)
"""
import io
import os
import subprocess

SB = "/opt/jhy-sandbox"
LIB, BIN, BLOCK = f"{SB}/lib", f"{SB}/bin", f"{SB}/block"
WRAPPER = "/usr/local/bin/jhy-shell"
BAN_FILE = "/run/jhy-sandbox-ban"          # tmpfs:重启自动清除
BAN_SECONDS = 300
UID = 1001

# ---------------------------------------------------------------- ① ss-ban
io.open(f"{LIB}/ss-ban.c", "w", encoding="utf-8", newline="\n").write(r'''/* SilentSafe 封禁器(setuid root,不接受任何参数 -> 无注入面)
 * 1) 写封禁时间戳(now + 300s)到 /run/jhy-sandbox-ban
 * 2) SIGKILL 掉目标 uid 的所有进程(把当前会话踢下线)
 * 用法:
 *   (无参数)        执行封禁 + 踢人
 *   status          打印剩余秒数(root 或普通用户都可读)
 *   clear           解除封禁(仅真实 uid 0 可用)
 */
#define _GNU_SOURCE
#include <dirent.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>

#define BAN_FILE "/run/jhy-sandbox-ban"
#define TARGET_UID 1001
#define BAN_SECONDS 300

static long now_sec(void) { return (long)time(NULL); }

static long read_until(void) {
    FILE *f = fopen(BAN_FILE, "r");
    if (!f) return 0;
    long v = 0;
    if (fscanf(f, "%ld", &v) != 1) v = 0;
    fclose(f);
    return v;
}

static void kill_user(pid_t self) {
    DIR *d = opendir("/proc");
    if (!d) return;
    struct dirent *e;
    while ((e = readdir(d))) {
        if (e->d_name[0] < '0' || e->d_name[0] > '9') continue;
        pid_t pid = (pid_t)atoi(e->d_name);
        if (pid == self) continue;
        char path[64], line[512];
        snprintf(path, sizeof(path), "/proc/%d/status", pid);
        FILE *f = fopen(path, "r");
        if (!f) continue;
        int matched = 0;
        while (fgets(line, sizeof(line), f)) {
            if (strncmp(line, "Uid:", 4) == 0) {
                int real = -1;
                if (sscanf(line + 4, "%d", &real) == 1 && real == TARGET_UID) matched = 1;
                break;
            }
        }
        fclose(f);
        if (matched) kill(pid, SIGKILL);
    }
    closedir(d);
}

int main(int argc, char **argv) {
    pid_t self = getpid();
    if (argc > 1 && strcmp(argv[1], "status") == 0) {
        long until = read_until(), left = until - now_sec();
        printf("%ld\n", left > 0 ? left : 0);
        return 0;
    }
    if (argc > 1 && strcmp(argv[1], "clear") == 0) {
        if (getuid() != 0) { fprintf(stderr, "clear 需要 root\n"); return 1; }
        unlink(BAN_FILE);
        return 0;
    }
    /* 默认:封禁 + 踢人 */
    FILE *f = fopen(BAN_FILE, "w");
    if (!f) return 1;
    fprintf(f, "%ld\n", now_sec() + BAN_SECONDS);
    fclose(f);
    chmod(BAN_FILE, 0644);
    kill_user(self);
    return 0;
}
''')
p = subprocess.run(["gcc", "-O2", "-o", f"{LIB}/ss-ban", f"{LIB}/ss-ban.c"], capture_output=True, text=True)
print("① ss-ban 编译:", "成功" if p.returncode == 0 else p.stderr[:400])
if p.returncode == 0:
    subprocess.run(["chown", "root:root", f"{LIB}/ss-ban"], capture_output=True)
    os.chmod(f"{LIB}/ss-ban", 0o4755)
    print("   权限:", oct(os.stat(f"{LIB}/ss-ban").st_mode & 0o7777))

# ---------------------------------------------------------------- ② block.sh
io.open(f"{BLOCK}/block.sh", "w", encoding="utf-8", newline="\n").write(f'''#!/bin/bash
# SilentSafe 统一拦截器:固定提示格式;高 severity 规则还会踢下线并封禁 {BAN_SECONDS} 秒。
# 顺序很重要:**先让她看到报错**,再终止会话 —— 否则被踢得莫名其妙。
CODE="${{SS_CODE:-SS_ERR_ID_100}}"
DESC="${{SS_DESC:-危险操作}}"
BAN="${{SS_BAN:-0}}"
TS="$(date '+%Y-%m-%d %H:%M:%S')"

echo "[SilentSafe]: 您的行为${{DESC}}根据服务器规则配置文件，已经被拦截。" >&2
echo "[SilentSafe]  ErrCode: ${{CODE}}" >&2

# 落一份审计(她的家目录,重新登录后也能看到发生了什么)
LOG="$HOME/silentsafe-audit.log"
{{
  echo "[$TS] $CODE $DESC ban=$BAN cmd=${{SSH_ORIGINAL_COMMAND:-$0 $*}}"
}} >> "$LOG" 2>/dev/null

if [ "$BAN" = "1" ]; then
  echo "[SilentSafe]  该行为触发安全封禁:本次会话将在 2 秒后终止,并封禁 {BAN_SECONDS} 秒。" >&2
  echo "[SilentSafe]  封禁期内登录会被拒绝;详情见 ~/silentsafe-audit.log" >&2
  # 给她 2 秒看清上面的信息(同时保证输出已送出),再执行封禁 + 踢下线
  sleep 2
  /opt/jhy-sandbox/lib/ss-ban >/dev/null 2>&1
else
  # 非封禁类拦截:同样记一笔,便于事后追溯
  echo "[SilentSafe]  操作已阻止,未执行。" >&2
fi
exit 1
''')
print("② block.sh 已支持封禁(先报错 2 秒再踢)")

# ---------------------------------------------------------------- ③ rm shim:递归删除触发封禁
rm_shim = io.open(f"{BIN}/rm", encoding="utf-8").read()
if "SS_BAN=1" not in rm_shim:
    rm_shim = rm_shim.replace(
        'export SS_CODE="SS_ERR_ID_100"; export SS_DESC="递归删除根目录"',
        'export SS_CODE="SS_ERR_ID_100"; export SS_DESC="递归删除根目录"; export SS_BAN=1')
    rm_shim = rm_shim.replace(
        'export SS_DESC="删除系统关键目录($c)"',
        'export SS_DESC="删除系统关键目录($c)"; export SS_BAN=1')
    rm_shim = rm_shim.replace(
        '''        "$c"/*) export SS_CODE="SS_ERR_ID_100"
                export SS_DESC="递归删除系统目录($c)"
                exec /opt/silentsafe/block.sh ;;''',
        '''        "$c"/*) export SS_CODE="SS_ERR_ID_100"
                export SS_DESC="递归删除系统目录($c)"
                export SS_BAN=1
                exec /opt/silentsafe/block.sh ;;''')
    io.open(f"{BIN}/rm", "w", encoding="utf-8", newline="\n").write(rm_shim)
    os.chmod(f"{BIN}/rm", 0o755)
    print("③ rm shim:递归删除 -> 拦截 + 封禁")

# 规则文件标注哪些规则会封禁
rules = io.open(f"{SB}/silentsafe/rules.tsv", encoding="utf-8").read().splitlines()
out = []
for line in rules:
    if line.startswith("#"):
        out.append("# SilentSafe 规则表: ErrCode\t描述\t类别\t封禁(ban=踢下线+封5分钟)")
        continue
    parts = line.split("\t")
    if len(parts) >= 3:
        ban = "ban" if parts[0] == "SS_ERR_ID_100" else "-"
        out.append("\t".join(parts[:3] + [ban]))
io.open(f"{SB}/silentsafe/rules.tsv", "w", encoding="utf-8", newline="\n").write("\n".join(out) + "\n")
print("   规则表已标注 ban 标记")

# ---------------------------------------------------------------- ④ 登录壳:封禁期内拒绝
s = io.open(WRAPPER, encoding="utf-8").read()
if "jhy-sandbox-ban" not in s:
    guard = f'''# 封禁检查(SilentSafe):封禁期内一律拒绝访问
if [ -x /opt/jhy-sandbox/lib/ss-ban ]; then
  LEFT="$(/opt/jhy-sandbox/lib/ss-ban status 2>/dev/null || echo 0)"
  if [ "${{LEFT:-0}}" -gt 0 ]; then
    echo "[SilentSafe]: 您的账号因触发安全规则,当前处于封禁状态,访问已被拒绝。" >&2
    echo "[SilentSafe]  ErrCode: SS_ERR_ID_BAN" >&2
    echo "[SilentSafe]  剩余封禁时间: ${{LEFT}} 秒" >&2
    exit 1
  fi
fi

'''
    s = s.replace("set -u\n", "set -u\n\n" + guard, 1)
    io.open(WRAPPER, "w", encoding="utf-8", newline="\n").write(s)
    os.chmod(WRAPPER, 0o755)
    print("④ 登录壳已加封禁检查")
p = subprocess.run(["bash", "-n", WRAPPER], capture_output=True, text=True)
print("   wrapper 语法:", "通过" if p.returncode == 0 else p.stderr[:200])
