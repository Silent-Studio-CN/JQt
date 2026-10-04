# 远程节点:终端接口(curl 直连)与入口路径

> 这些脚本**部署到节点**;节点上的密钥/token 不进仓库。
> 节点:`silent@192.168.211.7`(nginx `:8080` → 公网入口由隧道/sslh 转发)

## 部署了什么

| 组件 | 位置 | 说明 |
|---|---|---|
| `jqt-exec-api.py` | `/usr/local/bin/`(root,0755) | 终端 API 服务,监听 `127.0.0.1:9100` |
| `jqt-exec-api.service` | systemd | API 服务,`Restart=always` |
| `jqt-term-silent-legacy.service` | systemd | ttyd 实例,**`-b /silent`**,端口 7683(旧入口,仍在用) |
| `jqt-term-silent.service` | systemd(原有) | ttyd 实例,**`-b /console`**,端口 7682(首页入口) |
| nginx `location /api/exec` | `/etc/nginx/sites-available/jqt` | 转发到 `127.0.0.1:9100`,**自带 token 鉴权**(不走 cookie 登录) |
| nginx `location /silent/` | 同上 | 转发到 7683,与 `/console/` 同样需要登录 |

**入口可见性**:首页/登录页/账号页**不出现** `/silent`、`/root` 任何路径
(只链 `/console/`、`/account/`、`/login.html`);但 `/silent/` **路由保持可用**。

## curl 用法(核心)

```bash
TOK=$(ssh ... "sudo /usr/bin/python3 /usr/local/bin/jqt-exec-api.py --show-tokens | awk '{print \$2}'")

# 一次性命令(GET,最省事)
curl -s "https://<你的域名>/api/exec?cmd=uptime&token=$TOK"

# POST + Bearer(推荐:命令与 token 都不进 URL)
curl -s -X POST https://<你的域名>/api/exec \
     -H "Authorization: Bearer $TOK" --data-urlencode 'cmd=df -h'

# 只要输出(便于管道/重定向),退出码在响应头 X-JQt-Exit
curl -s "https://<你的域名>/api/exec?cmd=ls%20-1&format=text&token=$TOK"
curl -s "...&format=text&token=$TOK" | grep log

# 持久会话:cd / 环境变量跨调用保留(tmux 后端)
curl -s "https://<你的域名>/api/exec?cmd=cd%20/var/log&session=main&token=$TOK"
curl -s "https://<你的域名>/api/exec?cmd=pwd;tail%20-5%20syslog&session=main&token=$TOK"
curl -s "https://<你的域名>/api/exec/sessions?token=$TOK"

# 指定工作目录 / 超时(默认 30s,上限 300s)
curl -s -X POST https://<你的域名>/api/exec -H "X-JQt-Token: $TOK" \
     --data-urlencode 'cmd=git status' --data 'cwd=/home/silent/proj' --data 'timeout=60'
```

**参数**:`cmd`(必填)、`cwd`、`timeout`、`format`(`json`|`text`)、`session`、`token`
**鉴权**:`?token=` / `Authorization: Bearer` / `X-JQt-Token` 三选一
**返回**:`{"ok":bool,"exit":int,"stdout":str,"stderr":str,"ms":int,"truncated":bool}`
**帮助**:`curl -s https://<域名>/api/exec/help`

## token 管理(在节点上)

```bash
sudo python3 /usr/local/bin/jqt-exec-api.py --show-tokens          # 列出
sudo python3 /usr/local/bin/jqt-exec-api.py --add-token ci "给 CI 用"  # 新增(打印明文,只显示这一次)
sudo /etc/jqt/api-tokens.json 中删掉对应条目即吊销
```

token 文件 `/etc/jqt/api-tokens.json`(0600,root)。每个 token 有 `allow_http` 开关:

| allow_http | 行为 |
|---|---|
| `false` | **仅 https**;经明文 http 访问一律 403(带提示) |
| `true` | 允许明文(适用于"隧道在服务商侧终止 TLS、nginx 只能看到 http"的场景)|

## 安全与限制

- 命令以 **`silent`** 身份执行(不是 root);`sudo` 是否可用取决于该账号权限
- 限流:每 token 每 60s 最多 **120** 条
- 输出上限 **512 KiB**(超出置 `truncated=true`),单命令超时上限 **300s**
- 审计:`/var/log/jqt/exec.log`(JSON 行:时间、token 名、IP、会话、命令前 500 字、退出码、耗时)
- 明文 HTTP 的请求会被记录(便于发现误用);token 一旦进 URL,注意别留在 shell 历史/代理日志里

## 重新部署 / 更新

```bash
python tools/remote/deploy-exec-api.py     # 上传脚本 + 单元 + 打补丁 + 重启
bash tools/remote/selftest.sh             # 端到端自测(入口状态码、鉴权、会话、超时、审计)
python tools/remote/check-paths.sh        # 页面是否泄漏入口路径
```
