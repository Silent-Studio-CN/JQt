#!/usr/bin/env python3
"""
JQt 远程终端 API v1.0 —— 给 curl(以及任何脚本/CI)用的命令行接口。

设计取舍:
  · 面向 curl:一次请求一条命令,支持 GET/POST,token 可放 Header 或查询串;
    默认回 JSON,`format=text` 直接回 stdout(方便管道 `| jq` / `> file`)。
  · 两条执行路径:
      - 一次性:`bash -lc "<cmd>"`(默认,干净、可超时、拿得到退出码)
      - 持久会话:`session=name` 走 tmux,`cd`/环境变量在多次调用间保留
  · 安全默认:必须 token;明文 HTTP 默认拒绝(可用 token 上的 allow_http 放开);
    有审计日志与限流;命令以 `silent` 身份执行(不用 root 跑用户命令)。

token 文件 /etc/jqt/api-tokens.json:
    { "default": { "token": "<64hex>", "created": 1700000000,
                   "allow_http": false, "note": "" } }

用法示例:
    curl -s "https://<host>/api/exec?cmd=uptime&token=$TOK"
    curl -s -X POST https://<host>/api/exec -H "Authorization: Bearer $TOK" \
         --data-urlencode 'cmd=df -h' --data 'format=text'
    curl -s "https://<host>/api/exec?cmd=cd /var/log;ls&session=main&token=$TOK"
"""
import hmac
import json
import os
import re
import secrets
import shlex
import subprocess
import sys
import threading
import time
from collections import deque
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

TOKENS_FILE = "/etc/jqt/api-tokens.json"
LOG_FILE = "/var/log/jqt/exec.log"
RUN_AS = "silent"
HOME_DIR = "/home/silent"
PORT = 9100
DEFAULT_TIMEOUT, MAX_TIMEOUT = 30, 300
MAX_OUTPUT = 512 * 1024
RATE_LIMIT, RATE_WINDOW = 120, 60
SESSION_RE = re.compile(r"^[A-Za-z0-9_-]{1,32}$")
# 已知参数名:请求体里出现其中之一才按"表单"解析,否则整段请求体就是命令
KNOWN_PARAMS = {"cmd", "cwd", "timeout", "format", "session", "token"}

_lock = threading.Lock()
_rate = {}          # token 名 -> deque(时间戳)


# ------------------------------------------------------------------ token
def load_tokens():
    if not os.path.exists(TOKENS_FILE):
        tok = secrets.token_hex(32)
        data = {"default": {"token": tok, "created": int(time.time()),
                            "allow_http": False, "note": "自动生成"}}
        os.makedirs(os.path.dirname(TOKENS_FILE), exist_ok=True)
        with open(TOKENS_FILE, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        os.chmod(TOKENS_FILE, 0o600)
        print(f"[exec-api] 已生成首个 token(default): {tok}", flush=True)
        return data
    try:
        with open(TOKENS_FILE, encoding="utf-8") as f:
            return json.load(f)
    except Exception as e:
        print(f"[exec-api] token 文件解析失败: {e}", flush=True)
        return {}


def add_token(name, note=""):
    data = load_tokens()
    data[name] = {"token": secrets.token_hex(32), "created": int(time.time()),
                  "allow_http": False, "note": note}
    with open(TOKENS_FILE, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    os.chmod(TOKENS_FILE, 0o600)
    print(data[name]["token"])
    return data[name]["token"]


def audit(rec):
    try:
        os.makedirs(os.path.dirname(LOG_FILE), exist_ok=True)
        with open(LOG_FILE, "a", encoding="utf-8") as f:
            f.write(json.dumps(rec, ensure_ascii=False) + "\n")
    except Exception:
        pass


# ------------------------------------------------------------------ 执行
def sh(args, timeout, cwd=None, text=True):
    return subprocess.run(args, capture_output=True, text=text, timeout=timeout,
                          cwd=cwd or HOME_DIR)


def run_once(cmd, timeout, cwd=None):
    t0 = time.time()
    try:
        p = sh(["sudo", "-u", RUN_AS, "--", "/bin/bash", "-lc", cmd], timeout, cwd)
        return p.stdout, p.stderr, p.returncode, int((time.time() - t0) * 1000), False
    except subprocess.TimeoutExpired:
        return "", f"命令超时({timeout}s)已被终止", 124, int((time.time() - t0) * 1000), False
    except Exception as e:
        return "", f"执行失败: {e}", 125, int((time.time() - t0) * 1000), False


def tmux(args, timeout=10):
    return sh(["sudo", "-u", RUN_AS, "--", "tmux"] + args, timeout)


def tmux_sessions():
    p = tmux(["list-sessions", "-F", "#{session_name}\t#{session_created}\t#{session_windows}"])
    out = []
    for line in (p.stdout or "").splitlines():
        parts = line.split("\t")
        if len(parts) >= 2:
            out.append({"name": parts[0], "created": int(parts[1]) if parts[1].isdigit() else 0,
                        "windows": int(parts[2]) if len(parts) > 2 and parts[2].isdigit() else 1})
    return out


ANSI = re.compile(r"\x1b\[[0-9;?]*[A-Za-z]|\x1b\][^\x07]*\x07|\r")


def run_session(name, cmd, timeout):
    """在 tmux 会话里执行并回收输出(持久 cd/env)。"""
    t0 = time.time()
    if not any(s["name"] == name for s in tmux_sessions()):
        tmux(["new-session", "-d", "-s", name, "-x", "220", "-y", "50"])
        time.sleep(0.3)
    marker = f"__JQT_DONE_{secrets.token_hex(4)}__"
    line = f"{cmd} ; echo {marker}$?"
    tmux(["send-keys", "-t", name, "-l", "--", line])
    tmux(["send-keys", "-t", name, "Enter"])
    deadline = time.time() + timeout
    pane = ""
    while time.time() < deadline:
        pane = ANSI.sub("", tmux(["capture-pane", "-p", "-t", name, "-S", "-2000"]).stdout or "")
        m = re.search(re.escape(marker) + r"(\d+)", pane)
        if m:
            code = int(m.group(1))
            idx = pane.rfind(line)
            body = pane[idx + len(line):] if idx >= 0 else pane
            body = body.split(marker)[0]
            return body.strip("\n"), "", code, int((time.time() - t0) * 1000), False
        time.sleep(0.2)
    return pane[-MAX_OUTPUT:], f"会话命令超时({timeout}s)", 124, int((time.time() - t0) * 1000), False


def clip(s):
    if len(s) <= MAX_OUTPUT:
        return s, False
    return s[:MAX_OUTPUT], True


HELP = """JQt 远程终端 API(用 token 鉴权,不需要浏览器)

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

示例(TOK=你的 token,H=https://你的域名)
  # 最省事:一条命令
  curl -s "$H/api/exec?cmd=uptime&token=$TOK"

  # 推荐:命令与 token 都不进 URL
  curl -s -X POST $H/api/exec -H "Authorization: Bearer $TOK" \\
       --data-urlencode 'cmd=df -h'

  # 请求体就是命令(最贴近 "curl + 命令" 的直觉)
  curl -s -X POST $H/api/exec -H "Authorization: Bearer $TOK" --data-binary 'systemctl status nginx'

  # 只要输出,便于管道/重定向
  curl -s "$H/api/exec?cmd=ls%20-1&format=text&token=$TOK" | grep log
  curl -s -X POST $H/api/exec -H "Authorization: Bearer $TOK" --data-binary 'df -h' > /tmp/df.txt

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
"""


class Handler(BaseHTTPRequestHandler):
    server_version = "jqt-exec-api/1.0"

    def log_message(self, fmt, *args):        # 走自己的审计日志,别刷 journal
        pass

    # ---------------------------------------------------------- 工具
    def _query(self):
        q = parse_qs(urlparse(self.path).query)
        return {k: v[0] for k, v in q.items()}

    def _body(self):
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
        # curl 的 --data-binary 默认 Content-Type **就是** form-urlencoded,
        # 所以不能只看类型:先按表单解析,**只有解析出的键里出现已知参数名**才算表单,
        # 否则整段请求体就是命令(用法:curl -X POST ... --data-binary 'df -h')。
        form = {k: v[0] for k, v in parse_qs(text).items()}
        if form and (set(form) & KNOWN_PARAMS):
            return form
        if text.strip():
            return {"cmd": text}
        return {}

    def _auth(self, q):
        tok = ""
        h = self.headers.get("Authorization", "")
        if h.lower().startswith("bearer "):
            tok = h[7:].strip()
        tok = tok or self.headers.get("X-JQt-Token", "") or q.get("token", "")
        if not tok:
            return None
        for name, rec in load_tokens().items():
            if hmac.compare_digest(str(rec.get("token", "")), tok):
                return name, rec
        return None

    def _json(self, code, obj, extra=None):
        body = json.dumps(obj, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        for k, v in (extra or []):
            self.send_header(k, v)
        self.end_headers()
        self.wfile.write(body)

    def _text(self, code, s, extra=None):
        body = s.encode("utf-8", "replace")
        self.send_response(code)
        self.send_header("Content-Type", "text/plain; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        for k, v in (extra or []):
            self.send_header(k, v)
        self.end_headers()
        self.wfile.write(body)

    def _rate_ok(self, name):
        with _lock:
            dq = _rate.setdefault(name, deque())
            now = time.time()
            while dq and now - dq[0] > RATE_WINDOW:
                dq.popleft()
            if len(dq) >= RATE_LIMIT:
                return False
            dq.append(now)
            return True

    # ---------------------------------------------------------- 路由
    def do_GET(self):
        self._route()

    def do_POST(self):
        self._route()

    def _route(self):
        path = urlparse(self.path).path
        q = self._query()
        if path in ("/api/exec/help", "/help"):
            return self._text(200, HELP)

        auth = self._auth(q)
        if not auth:
            return self._json(401, {"ok": False, "msg": "缺少或无效的 token(见 /api/exec/help)"})
        name, rec = auth
        ip = (self.headers.get("X-Forwarded-For", "").split(",")[0].strip()
              or self.client_address[0])
        proto = (self.headers.get("X-Forwarded-Proto") or "https").lower()
        if proto == "http" and not rec.get("allow_http"):
            return self._json(403, {"ok": False,
                                    "msg": "明文 HTTP 被拒绝;请用 https,或在 token 上设 allow_http=true"})

        if path == "/api/exec/sessions":
            return self._json(200, {"ok": True, "sessions": tmux_sessions()})
        if path not in ("/api/exec", "/api/exec/"):
            return self._json(404, {"ok": False, "msg": "未知接口,见 /api/exec/help"})

        if not self._rate_ok(name):
            return self._json(429, {"ok": False, "msg": f"限流:每 {RATE_WINDOW}s 最多 {RATE_LIMIT} 条"})

        params = dict(q)
        if self.command == "POST":
            params.update(self._body())
        cmd = (params.get("cmd") or "").strip()
        if not cmd:
            return self._json(400, {"ok": False, "msg": "缺少 cmd;示例 ?cmd=uptime"})
        if len(cmd) > 8000:
            return self._json(400, {"ok": False, "msg": "命令过长(>8000)"})

        try:
            timeout = max(1, min(MAX_TIMEOUT, int(params.get("timeout") or DEFAULT_TIMEOUT)))
        except ValueError:
            timeout = DEFAULT_TIMEOUT
        cwd = params.get("cwd") or None
        session = params.get("session") or ""
        if session and not SESSION_RE.match(session):
            return self._json(400, {"ok": False, "msg": "session 名只允许字母数字下划线短横"})
        fmt = (params.get("format") or "json").lower()

        if session:
            out, err, code, ms, trunc = run_session(session, cmd, timeout)
        else:
            out, err, code, ms, trunc = run_once(cmd, timeout, cwd)
        out, t1 = clip(out)
        err, t2 = clip(err)
        trunc = trunc or t1 or t2

        audit({"ts": int(time.time()), "token": name, "ip": ip, "session": session or "-",
               "cmd": cmd[:500], "exit": code, "ms": ms, "truncated": trunc})
        print(f"[exec-api] {name}@{ip} exit={code} {ms}ms session={session or '-'} cmd={cmd[:120]}",
              flush=True)

        if fmt == "text":
            return self._text(200, out, [("X-JQt-Exit", str(code)), ("X-JQt-Ms", str(ms)),
                                         ("X-JQt-Truncated", "1" if trunc else "0")])
        return self._json(200, {"ok": code == 0, "exit": code, "stdout": out, "stderr": err,
                                "ms": ms, "cmd": cmd, "session": session or "",
                                "truncated": trunc})


def main():
    if len(sys.argv) > 2 and sys.argv[1] == "--add-token":
        add_token(sys.argv[2], sys.argv[3] if len(sys.argv) > 3 else "")
        return
    if len(sys.argv) > 1 and sys.argv[1] == "--show-tokens":
        for n, r in load_tokens().items():
            print(f"{n}\t{r['token']}\tallow_http={r.get('allow_http', False)}")
        return
    load_tokens()
    print(f"[exec-api] 监听 127.0.0.1:{PORT}(经 nginx 暴露在 /api/exec)", flush=True)
    ThreadingHTTPServer(("127.0.0.1", PORT), Handler).serve_forever()


if __name__ == "__main__":
    main()
