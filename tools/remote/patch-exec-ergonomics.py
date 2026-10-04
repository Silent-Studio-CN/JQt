#!/usr/bin/env python3
"""给终端 API 增加 curl 友好写法:`--data-binary '<命令>'` 时请求体本身就是命令。
同时更新 /api/exec/help 的帮助文本(含 token 管理)。
"""
import io
import py_compile

P = "/usr/local/bin/jqt-exec-api.py"
s = io.open(P, encoding="utf-8").read()
done = []

# ---------------- ① 请求体直接当命令 ----------------
OLD = '''    def _body(self):
        n = int(self.headers.get("Content-Length") or 0)
        raw = self.rfile.read(n) if n else b""
        ctype = (self.headers.get("Content-Type") or "").lower()
        if "json" in ctype:
            try:
                d = json.loads(raw.decode("utf-8") or "{}")
                return {k: str(v) for k, v in d.items()}
            except Exception:
                return {}
        return {k: v[0] for k, v in parse_qs(raw.decode("utf-8", "replace")).items()}'''
NEW = '''    def _body(self):
        n = int(self.headers.get("Content-Length") or 0)
        raw = self.rfile.read(n) if n else b""
        text = raw.decode("utf-8", "replace")
        ctype = (self.headers.get("Content-Type") or "").lower()
        if "json" in ctype:
            try:
                d = json.loads(text or "{}")
                return {k: str(v) for k, v in d.items()}
            except Exception:
                return {}
        # 表单:cmd=...&cwd=...
        if "form-urlencoded" in ctype or (text and "=" in text.split("\\n")[0] and "cmd=" in text):
            return {k: v[0] for k, v in parse_qs(text).items()}
        # 其余情况(含 text/plain、空 Content-Type):整个请求体就是命令
        # 用法:curl -X POST ... --data-binary 'df -h'
        if text.strip():
            return {"cmd": text}
        return {}'''
if OLD in s:
    s = s.replace(OLD, NEW, 1)
    done.append("请求体可直接作命令")
else:
    print("⚠️ _body 未匹配(可能已改)")

# ---------------- ② 帮助文本 ----------------
old_help_start = s.find('HELP = """')
old_help_end = s.find('"""', s.find('"""', old_help_start) + 3) + 3
NEW_HELP = '''HELP = """JQt 远程终端 API(用 token 鉴权,不需要浏览器)

接口
  GET  /api/exec?cmd=<命令>[&cwd=<目录>][&timeout=<秒>][&format=text][&session=<名>][&token=<token>]
  POST /api/exec                    参数同上(表单 cmd=... 或 JSON {"cmd":"..."})
  POST /api/exec  请求体直接是命令   curl ... --data-binary 'df -h'
  GET  /api/exec/sessions           列出持久会话
  GET  /api/exec/help               本说明

鉴权(任选其一)
  ?token=<token>
  -H "Authorization: Bearer <token>"
  -H "X-JQt-Token: <token>"

返回
  默认 JSON: {"ok":bool,"exit":int,"stdout":str,"stderr":str,"ms":int,"cmd":str,
              "session":str,"truncated":bool}
  format=text: 直接返回 stdout,退出码在响应头 X-JQt-Exit,耗时 X-JQt-Ms

示例
  TOK=<你的 token>
  H=https://<你的域名>

  # 最省事:一条命令
  curl -s "$H/api/exec?cmd=uptime&token=$TOK"

  # 推荐:命令与 token 都不进 URL
  curl -s -X POST $H/api/exec -H "Authorization: Bearer $TOK" \\
       --data-urlencode 'cmd=df -h'

  # 请求体就是命令(最贴近"curl + 命令"的直觉)
  curl -s -X POST $H/api/exec -H "Authorization: Bearer $TOK" --data-binary 'systemctl status nginx'

  # 只要输出,便于管道/重定向
  curl -s "$H/api/exec?cmd=ls%20-1&format=text&token=$TOK" | grep log
  curl -s -X POST $H/api/exec -H "Bearer: $TOK" --data-binary 'df -h' > /tmp/df.txt

  # 持久会话:cd / 环境变量跨调用保留
  curl -s "$H/api/exec?cmd=cd%20/var/log&session=main&token=$TOK"
  curl -s "$H/api/exec?cmd=pwd;tail%20-5%20syslog&session=main&token=$TOK"
  curl -s "$H/api/exec/sessions?token=$TOK"

  # 指定目录 / 超时
  curl -s -X POST $H/api/exec -H "X-JQt-Token: $TOK" \\
       --data-urlencode 'cmd=git status' --data 'cwd=/home/silent/proj' --data 'timeout=60'

参数
  cmd      要执行的命令(必填;上限 8000 字符)
  cwd      工作目录(仅一次性模式)
  timeout  秒,默认 30,上限 300;超时返回 exit=124
  format   json(默认)| text
  session  会话名([A-Za-z0-9_-]{1,32});同名会话复用,cd/env 保留
  token    token(也可用 Header)

限制与审计
  执行身份 silent(非 root);限流 每 token 120 条/60 秒
  输出上限 512 KiB(超出 truncated=true)
  审计日志 /var/log/jqt/exec.log(时间/token 名/IP/会话/命令/退出码/耗时)

token 管理(在节点上执行)
  sudo python3 /usr/local/bin/jqt-exec-api.py --show-tokens
  sudo python3 /usr/local/bin/jqt-exec-api.py --add-token ci "给 CI 用"
  文件 /etc/jqt/api-tokens.json(0600);每条含 allow_http 开关(明文 HTTP 是否放行)
"""'''
if old_help_start != -1 and old_help_end > old_help_start:
    s = s[:old_help_start] + NEW_HELP + s[old_help_end:]
    done.append("帮助文本已更新")
else:
    print("⚠️ HELP 未匹配")

io.open(P, "w", encoding="utf-8").write(s)
py_compile.compile(P, doraise=True)
print("已更新:" + "、".join(done) + ";语法检查通过")
