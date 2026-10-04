#!/usr/bin/env python3
"""封禁机制 v2(架构修正):
  沙箱内的"root"其实是命名空间 root(宿主 uid 仍是她),写不了宿主 /run。
  所以改成:沙箱内只放一个**触发文件** -> 宿主 systemd 路径单元以 root 身份
  执行封禁(写 /run/jhy-sandbox-ban + 杀掉 uid 1001 的全部进程)。
  ss-ban 退化为**宿主机上的 root 工具**(status/clear)。
"""
import io
import os
import subprocess

SB = "/opt/jhy-sandbox"
WRAPPER = "/usr/local/bin/jhy-shell"
TRIGGER = "/home/jhy/.silentsafe-trigger"
BAN_FILE = "/run/jhy-sandbox-ban"
BAN_SECONDS = 300
UID = 1001


def w(path, content, mode=0o644):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with io.open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    os.chmod(path, mode)


# ① 宿主侧封禁脚本(root 运行)
w("/usr/local/bin/jhy-silentsafe-ban.sh", f'''#!/bin/bash
# 由 systemd 路径单元以 root 触发:封禁 + 踢下线(宿主侧,不可被沙箱内绕过)
set -u
NOW=$(date +%s)
echo $((NOW + {BAN_SECONDS})) > {BAN_FILE}
chmod 644 {BAN_FILE}

# 杀掉她的全部进程(踢下线,含所有会话)
KILLED=0
for pid in $(ls /proc | grep -E '^[0-9]+$'); do
  RU=$(awk '/^Uid:/{{print $2}}' /proc/$pid/status 2>/dev/null)
  if [ "$RU" = "{UID}" ]; then
    kill -9 "$pid" 2>/dev/null && KILLED=$((KILLED+1))
  fi
done
rm -f {TRIGGER}
logger -t silentsafe "封禁 jhy {BAN_SECONDS}s(kill $KILLED 个进程,触发: $(cat /home/jhy/.silentsafe-last 2>/dev/null | tail -1))"
echo "$(date '+%Y-%m-%d %H:%M:%S') 已封禁 {BAN_SECONDS}s,终止 $KILLED 个进程" >> /home/jhy/silentsafe-audit.log
chown {UID}:{UID} /home/jhy/silentsafe-audit.log 2>/dev/null
''', 0o755)

# ② systemd 路径单元 + 服务
w("/etc/systemd/system/jhy-silentsafe-ban.service", '''[Unit]
Description=SilentSafe ban (jhy) - triggered by sandbox trigger file

[Service]
Type=oneshot
ExecStart=/usr/local/bin/jhy-silentsafe-ban.sh
''')
w("/etc/systemd/system/jhy-silentsafe-ban.path", f'''[Unit]
Description=Watch SilentSafe trigger for jhy

[Path]
PathExists={TRIGGER}
Unit=jhy-silentsafe-ban.service

[Install]
WantedBy=multi-user.target
''')
subprocess.run(["systemctl", "daemon-reload"], capture_output=True)
p = subprocess.run(["systemctl", "enable", "--now", "jhy-silentsafe-ban.path"],
                   capture_output=True, text=True)
print("② systemd 路径单元:", "已启用" if p.returncode == 0 else p.stderr[:200])

# ③ ss-ban 变成宿主 root 工具
w("/usr/local/bin/ss-ban", f'''#!/bin/bash
# 宿主侧封禁管理(root)
case "${{1:-status}}" in
  status)
    if [ -f {BAN_FILE} ]; then
      UNTIL=$(cat {BAN_FILE}); LEFT=$(( UNTIL - $(date +%s) ))
      [ $LEFT -lt 0 ] && LEFT=0
      echo "$LEFT"
    else
      echo 0
    fi ;;
  clear) [ "$(id -u)" = "0" ] || {{ echo "需要 root" >&2; exit 1; }}; rm -f {BAN_FILE}; echo "已解除封禁" ;;
  *) echo "用法: ss-ban [status|clear]" >&2; exit 1 ;;
esac
''', 0o755)
os.remove(f"{SB}/lib/ss-ban") if os.path.exists(f"{SB}/lib/ss-ban") else None

# ④ block.sh:打印 -> 2 秒 -> 放触发文件
w(f"{SB}/block/block.sh", f'''#!/bin/bash
# SilentSafe 统一拦截器
#   顺序至关重要:**先让她看清报错**,再触发封禁(宿主侧踢人与封禁)
CODE="${{SS_CODE:-SS_ERR_ID_100}}"
DESC="${{SS_DESC:-危险操作}}"
BAN="${{SS_BAN:-0}}"
TS="$(date '+%Y-%m-%d %H:%M:%S')"

echo "[SilentSafe]: 您的行为${{DESC}}根据服务器规则配置文件，已经被拦截。" >&2
echo "[SilentSafe]  ErrCode: ${{CODE}}" >&2
echo "$TS $CODE $DESC" > /home/jhy/.silentsafe-last 2>/dev/null
echo "[$TS] $CODE $DESC ban=$BAN" >> /home/jhy/silentsafe-audit.log 2>/dev/null

if [ "$BAN" = "1" ]; then
  echo "[SilentSafe]  该行为触发安全封禁:会话将在 2 秒后终止,并封禁 {BAN_SECONDS} 秒。" >&2
  echo "[SilentSafe]  封禁期内登录会被拒绝;详情见 ~/silentsafe-audit.log" >&2
  sleep 2
  touch /home/jhy/.silentsafe-trigger 2>/dev/null
  sleep 3          # 等宿主侧完成踢人(期间会话会被 kill)
else
  echo "[SilentSafe]  操作已阻止,未执行。" >&2
fi
exit 1
''', 0o755)
print("④ block.sh 已改为触发文件方式")

# ⑤ 登录壳:直接读封禁文件
s = io.open(WRAPPER, encoding="utf-8").read()
old_guard_start = s.find("# 封禁检查(SilentSafe)")
if old_guard_start != -1:
    old_guard_end = s.find("\n\n", old_guard_start) + 2
    s = s[:old_guard_start] + s[old_guard_end:]
guard = f'''# 封禁检查(SilentSafe):封禁期内一律拒绝访问
if [ -f {BAN_FILE} ]; then
  UNTIL=$(cat {BAN_FILE} 2>/dev/null || echo 0)
  LEFT=$(( UNTIL - $(date +%s) ))
  if [ "$LEFT" -gt 0 ]; then
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
print("⑤ 登录壳已改为读宿主的封禁文件")
print("   wrapper 语法:", "通过" if subprocess.run(["bash", "-n", WRAPPER],
      capture_output=True).returncode == 0 else "错误")
