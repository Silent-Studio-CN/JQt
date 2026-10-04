#!/usr/bin/env python3
"""jhy 沙箱第四轮:修实测发现的三个问题
  ① free -h 单位错(awk 下标从 KiB 起算错位)-> 重写 human()
  ② df ~ 不看参数 -> 支持路径参数
  ③ 沙箱里没外网:/run 是 tmpfs,resolv.conf(常指向 /run 或 /mnt/wsl)断了
     -> 生成一份可用的 resolv.conf 绑进去
"""
import io
import os
import re
import subprocess

FAKE = "/opt/jhy-sandbox/fake"
SHIM = "/opt/jhy-sandbox/bin"
WRAPPER = "/usr/local/bin/jhy-shell"
RAM_GB = 128


def write(path, content, mode=0o644):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with io.open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    os.chmod(path, mode)
    print(f"   写入 {path}")


# ① free
write(f"{SHIM}/free", f'''#!/bin/bash
# 伪装成 128 GiB;human() 直接从 KiB 起算(上一版下标错位,128GiB 被显示成 128MiB)
human() {{
  awk -v k="$1" 'BEGIN {{
    split("KiB MiB GiB TiB PiB", u, " ");
    i = 1; v = k;
    while (v >= 1024 && i < 5) {{ v /= 1024; i++ }}
    printf (i == 1 ? "%.0f%s" : "%.1f%s"), v, u[i]
  }}'
}}
unit=MiB; div=1024
for a in "$@"; do
  case "$a" in
    -g|--giga) unit="GiB"; div=1048576 ;;
    -m|--mega) unit="MiB"; div=1024 ;;
    -k|--kilo) unit="KiB"; div=1 ;;
    -h|--human) unit="auto" ;;
  esac
done
TOTAL={RAM_GB * 1024 * 1024}; USED=$((TOTAL / 5)); FREE=$((TOTAL - USED)); SWAP=$((TOTAL / 4))
printf "               total        used        free      shared  buff/cache   available\\n"
if [ "$unit" = "auto" ]; then
  printf "Mem:   %11s %11s %11s %11s %11s %11s\\n" "$(human $TOTAL)" "$(human $USED)" "$(human $FREE)" "0B" "$(human $((TOTAL/10)))" "$(human $((TOTAL*6/10)))"
  printf "Swap:  %11s %11s %11s\\n" "$(human $SWAP)" "0B" "$(human $SWAP)"
else
  printf "Mem:   %11d %11d %11d %11d %11d %11d\\n" "$((TOTAL/div))" "$((USED/div))" "$((FREE/div))" 0 "$((TOTAL/div/10))" "$((TOTAL*6/div/10))"
  printf "Swap:  %11d %11d %11d\\n" "$((SWAP/div))" 0 "$((SWAP/div))"
fi
''', mode=0o755)

# ② df(支持路径)
write(f"{SHIM}/df", '''#!/bin/bash
human=0; path=""
for a in "$@"; do
  case "$a" in
    -h|--human-readable) human=1 ;;
    -*) ;;
    *) path="$a" ;;
  esac
done
table() {
  if [ $human -eq 1 ]; then
    cat <<'EOF'
Filesystem      Size  Used Avail Use% Mounted on
/dev/nvme0n1p2  200G   38G  152G  20% /
/dev/nvme0n1p1  512M  6.1M  506M   2% /boot/efi
/dev/nvme0n1p3  3.6T  1.2T  2.3T  35% /data
/dev/loop0       98G   24G   70G  26% /home/jhy
tmpfs           128G  1.1G  127G   1% /dev/shm
EOF
  else
    cat <<'EOF'
Filesystem     1K-blocks      Used Available Use% Mounted on
/dev/nvme0n1p2 209715200  39845888 159500288  20% /
/dev/nvme0n1p1    524288      6248    518040   2% /boot/efi
/dev/nvme0n1p3 3865470566 1288490188 2477418752 35% /data
/dev/loop0     102400000  25165824  73089024  26% /home/jhy
tmpfs          134217728   1153434 133064294   1% /dev/shm
EOF
  fi
}
if [ -z "$path" ]; then table; exit 0; fi
# 按路径挑选:家目录走 /home/jhy,其余走 /
case "$path" in
  /home/jhy*|~*|/root*) pick="/home/jhy" ;;
  /data*|/srv*)         pick="/data" ;;
  /boot*)               pick="/boot/efi" ;;
  /dev/shm*)            pick="/dev/shm" ;;
  *)                    pick="/" ;;
esac
if [ $human -eq 1 ]; then
  echo "Filesystem      Size  Used Avail Use% Mounted on"
else
  echo "Filesystem     1K-blocks      Used Available Use% Mounted on"
fi
table | grep -E " $pick$"
''', mode=0o755)

# ③ resolv.conf
real = ""
try:
    real = io.open("/etc/resolv.conf", encoding="utf-8", errors="replace").read().strip()
except Exception:
    pass
nameservers = re.findall(r"^nameserver\s+(\S+)", real, re.M) or ["1.1.1.1", "8.8.8.8"]
write(f"{FAKE}/resolv.conf",
      "# 由宿主 DNS 生成;沙箱内 /run 是 tmpfs,原 resolv.conf 的软链会断\n"
      + "".join(f"nameserver {ns}\n" for ns in nameservers[:3])
      + "options timeout:2 attempts:2\n")
print("   DNS:", ", ".join(nameservers[:3]))

# 绑进沙箱
s = io.open(WRAPPER, encoding="utf-8").read()
if "/opt/jhy-sandbox/fake/resolv.conf" not in s:
    s = s.replace("  --ro-bind /sys /sys\n",
                  "  --ro-bind /sys /sys\n"
                  f"  --ro-bind {FAKE}/resolv.conf /etc/resolv.conf\n")
    io.open(WRAPPER, "w", encoding="utf-8", newline="\n").write(s)
    os.chmod(WRAPPER, 0o755)
    print("   wrapper 已绑定 resolv.conf")
p = subprocess.run(["bash", "-n", WRAPPER], capture_output=True, text=True)
print("   wrapper 语法:", "通过" if p.returncode == 0 else p.stderr[:200])

# 顺手看看上面 lsmod 里那条 hv_ 是哪来的
mods = io.open(f"{FAKE}/modules", encoding="utf-8").read().splitlines()
bad = [m for m in mods if re.match(r"^(hv_|hyperv|vmbus|msft)", m)]
print("   伪造 /proc/modules 里的 hv/hyperv 行:", bad or "无")
print("   伪造模块总数:", len(mods))
