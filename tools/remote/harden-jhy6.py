#!/usr/bin/env python3
"""jhy 沙箱第六轮:
  ① uname shim 支持组合短参数(-sr / -snrvm / -a)
  ② DNS:本机 /etc/resolv.conf 是指向 /mnt/wsl/resolv.conf 的软链,
     而沙箱把 /mnt 盖成 tmpfs -> 悬空。把可用 DNS 绑到 /mnt/wsl/resolv.conf 上。
"""
import io
import os
import re
import subprocess

FAKE = "/opt/jhy-sandbox/fake"
SHIM = "/opt/jhy-sandbox/bin"
WRAPPER = "/usr/local/bin/jhy-shell"
HOST, THREADS = "compute-node-01", 512

# ① uname
io.open(f"{SHIM}/uname", "w", encoding="utf-8", newline="\n").write(f'''#!/bin/bash
# uname 直接问内核,伪造 /proc 无效 -> shim 覆盖。
# 必须支持组合短参数(如 -sr / -snrvm),否则会输出空(实测踩过)。
K="Linux"; N="{HOST}"; R="6.8.0-45-generic"
V="#45-Ubuntu SMP PREEMPT_DYNAMIC Fri Aug 30 12:02:04 UTC 2026"; M="x86_64"; P="x86_64"; O="GNU/Linux"
s=0; n=0; r=0; v=0; m=0; p=0; o=0; a=0
if [ $# -eq 0 ]; then a=1; fi
for arg in "$@"; do
  case "$arg" in
    --all) a=1; continue ;;
    --kernel-name) s=1; continue ;;
    --nodename) n=1; continue ;;
    --kernel-release) r=1; continue ;;
    --kernel-version) v=1; continue ;;
    --machine) m=1; continue ;;
    --processor) p=1; continue ;;
    --operating-system) o=1; continue ;;
    -*) for ((i=1; i<${{#arg}}; i++)); do
          case "${{arg:$i:1}}" in
            a) a=1 ;; s) s=1 ;; n) n=1 ;; r) r=1 ;; v) v=1 ;; m) m=1 ;; p) p=1 ;; o) o=1 ;;
          esac
        done ;;
  esac
done
if [ $a -eq 1 ]; then echo "$K $N $R $V $M $P $O"; exit 0; fi
out=""
[ $s -eq 1 ] && out="$K"
[ $n -eq 1 ] && out="$out $N"
[ $r -eq 1 ] && out="$out $R"
[ $v -eq 1 ] && out="$out $V"
[ $m -eq 1 ] && out="$out $M"
[ $p -eq 1 ] && out="$out $P"
[ $o -eq 1 ] && out="$out $O"
echo "${{out# }}"
''')
os.chmod(f"{SHIM}/uname", 0o755)
print("   已修 uname shim(支持组合参数)")

# ② DNS
real = ""
try:
    real = io.open("/etc/resolv.conf", encoding="utf-8", errors="replace").read()
except Exception:
    pass
ns = re.findall(r"^nameserver\s+(\S+)", real, re.M) or ["10.255.255.254", "1.1.1.1"]
io.open(f"{FAKE}/resolv.conf", "w", encoding="utf-8", newline="\n").write(
    "# 沙箱 DNS(宿主 /etc/resolv.conf 是软链,指向 /mnt/wsl/resolv.conf)\n"
    + "".join(f"nameserver {x}\n" for x in ns[:3]) + "options timeout:2 attempts:2\n")
print("   DNS 服务器:", ", ".join(ns[:3]))

s = io.open(WRAPPER, encoding="utf-8").read()
# 去掉所有旧的 resolv 绑定,重新绑到软链目标
s = re.sub(r"\n\s*--ro-bind \S*resolv\.conf \S+", "", s)
if "/mnt/wsl/resolv.conf" not in s:
    # /mnt 是 tmpfs(bwrap 可在其中创建父目录)
    s = s.replace("  --tmpfs /mnt\n",
                  f"  --tmpfs /mnt\n  --ro-bind {FAKE}/resolv.conf /mnt/wsl/resolv.conf\n")
    print("   已绑定 /mnt/wsl/resolv.conf(软链目标)")
io.open(WRAPPER, "w", encoding="utf-8", newline="\n").write(s)
os.chmod(WRAPPER, 0o755)
p = subprocess.run(["bash", "-n", WRAPPER], capture_output=True, text=True)
print("   wrapper 语法:", "通过" if p.returncode == 0 else p.stderr[:200])

# ③ lsmod 里那条 hv 是什么
out = subprocess.run(["lsmod"], capture_output=True, text=True).stdout
print("   真实 lsmod 里含 hv/hyperv 的行:",
      [l.split()[0] for l in out.splitlines() if re.search(r"hv_|hyperv", l)][:5])
