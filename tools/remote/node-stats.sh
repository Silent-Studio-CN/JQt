#!/bin/bash
# 节点当前状态汇总(用于进度统计)
echo "== 服务 =="
for s in nginx sslh jqt-auth jqt-exec-api jqt-term-silent jqt-term-silent-legacy jqt-term-jhy fail2ban; do
  printf "   %-26s %s\n" "$s" "$(systemctl is-active $s 2>/dev/null)"
done

echo
echo "== 对外路由 =="
grep -oE 'location (=|~)? ?[^ {]+' /etc/nginx/sites-available/jqt | sed 's/location //' | sort -u | sed 's/^/   /'

echo
echo "== 接口文档规模 =="
python3 -c "
import yaml,io
d=yaml.safe_load(io.open('/var/www/jqt/openapi.yaml',encoding='utf-8'))
ops=sum(len([m for m in v if m in ('get','post','put','delete')]) for v in d['paths'].values())
print(f'   路径 {len(d[\"paths\"])} 个 / 操作 {ops} 个 / 鉴权方案 {len(d[\"components\"][\"securitySchemes\"])} 个')
print('   标签:', ', '.join(t['name'] for t in d.get('tags',[])))
"

echo
echo "== 终端 API token =="
sudo /usr/bin/python3 /usr/local/bin/jqt-exec-api.py --show-tokens | sed 's/\(.\{20\}\).*\(allow_http.*\)/   \1…  \2/'

echo
echo "== jhy 沙箱 =="
printf "   家目录配额: %s\n" "$(df -h /home/jhy | tail -1 | awk '{print $2}')"
printf "   cgroup    : mem=%s cpu=%s tasks=%s\n" \
  "$(cat /sys/fs/cgroup/user.slice/user-1001.slice/memory.max 2>/dev/null)" \
  "$(cat /sys/fs/cgroup/user.slice/user-1001.slice/cpu.max 2>/dev/null)" \
  "$(cat /sys/fs/cgroup/user.slice/user-1001.slice/pids.max 2>/dev/null)"
printf "   挂载      : %s\n" "$(mount | grep -c '/home/jhy') 处 loop 挂载"
printf "   封禁状态  : %s 秒剩余\n" "$(/usr/local/bin/ss-ban status 2>/dev/null)"
printf "   SilentSafe: %s 条规则, %s 个拦截器\n" \
  "$(grep -vc '^#' /opt/jhy-sandbox/silentsafe/rules.tsv 2>/dev/null)" \
  "$(ls /opt/jhy-sandbox/block/real 2>/dev/null | wc -l)"
printf "   shim 数量 : %s\n" "$(ls /opt/jhy-sandbox/bin 2>/dev/null | wc -l)"
printf "   伪装      : nproc=%s uname=%s virt=%s\n" \
  "$(sudo -u jhy -H /usr/local/bin/jhy-shell -c 'nproc; /usr/bin/uname -r; systemd-detect-virt' 2>/dev/null | tr '\n' ' ')"

echo
echo "== 账号 =="
for u in silent jhy; do
  printf "   %-8s shell=%s\n" "$u" "$(getent passwd $u | cut -d: -f7)"
done
