#!/usr/bin/env python3
"""jhy 沙箱第三轮:清掉剩余的 WSL/虚拟机硬线索
  ① 去掉上一轮导致沙箱起不来的 DMI 逐个绑定(WSL 没有 /sys/class/dmi,无数据可泄)
  ② 伪造 /proc/cmdline、/proc/stat(512 核)、/proc/modules(滤掉 hv_*/hyperv_*)
  ③ 伪造 /sys/devices/system/cpu/{online,possible,present}(否则 cat 一下就露 12 核)
  ④ tmpfs 盖住 /sys/module 与 vmbus/hv 相关类目录
"""
import io
import os
import re
import subprocess

FAKE = "/opt/jhy-sandbox/fake"
WRAPPER = "/usr/local/bin/jhy-shell"
THREADS, RAM_GB = 512, 128


def write(path, content, mode=0o644):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with io.open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    os.chmod(path, mode)
    print(f"   写入 {path}")


# ② /proc/cmdline(不说 WSL)
write(f"{FAKE}/cmdline",
      "BOOT_IMAGE=/vmlinuz-6.8.0-45-generic root=/dev/nvme0n1p2 ro quiet splash "
      "intel_iommu=on iommu=pt transparent_hugepage=always\n")

# ② /proc/stat:512 个 cpu 行 + 汇总(这样 top/htop/vmstat 都认为有 512 核)
lines = []
for i in range(THREADS):
    base = 1000000 + i * 137
    lines.append(f"cpu{i} {base} 0 {base * 3} {base * 40} 0 0 12 0 0 0\n")
tot = sum(1000000 + i * 137 for i in range(THREADS))
lines.append(f"cpu {tot} 0 {tot * 3} {tot * 40} 0 0 {THREADS * 12} 0 0 0\n")
lines.append("intr " + " ".join(str(100000000 + i * 991) for i in range(16)) + "\n")
lines.append("ctxt 4823910482\n")
lines.append("btime 1788000000\n")
lines.append(f"processes 91827364\nprocs_running 3\nprocs_blocked 0\n")
lines.append("softirq " + " ".join(str(5000000 + i * 137) for i in range(16)) + "\n")
write(f"{FAKE}/stat", "".join(lines))

# ② /proc/modules:滤掉 hv_* / hyperv_* / vmbus(让 lsmod 看起来正常)
mods = []
try:
    import glob
    for d in sorted(glob.glob("/sys/module/*")):
        name = os.path.basename(d)
        if re.match(r"^(hv_|hyperv|vmbus|msft)", name):
            continue
        mods.append(f"{name} 16384 1 - Live 0x0000000000000000\n")
except Exception as e:
    print("   ⚠️ 读取模块列表失败:", e)
write(f"{FAKE}/modules", "".join(mods[:200]))

# ③ CPU 拓扑(for_each 常见一问一答)
for name, val in (("online", f"0-{THREADS - 1}\n"),
                  ("possible", f"0-{THREADS - 1}\n"),
                  ("present", f"0-{THREADS - 1}\n")):
    write(f"{FAKE}/cpu-{name}", val)

# ④ 更新登录壳:去掉坏掉的 DMI 绑定,补上新的伪造项
s = io.open(WRAPPER, encoding="utf-8").read()
s = re.sub(r"(\n  --ro-bind /opt/jhy-sandbox/fake/dmi/[a-z_]+ +\S+)+\n", "\n", s)
s = re.sub(r"(\n  --ro-bind \{FAKE\}/dmi/[a-z_]+ +\S+)+\n", "\n", s)

extra = """  # 剩余 WSL/虚拟机线索(这些都存在,可以绑掉)
  --ro-bind {FAKE}/cmdline /proc/cmdline
  --ro-bind {FAKE}/stat    /proc/stat
  --ro-bind {FAKE}/modules /proc/modules
  --ro-bind {FAKE}/cpu-online   /sys/devices/system/cpu/online
  --ro-bind {FAKE}/cpu-possible /sys/devices/system/cpu/possible
  --ro-bind {FAKE}/cpu-present  /sys/devices/system/cpu/present
  --tmpfs /sys/module
""".replace("{FAKE}", FAKE)
if "/proc/cmdline" not in s:
    s = s.replace("  --ro-bind /sys /sys\n", "  --ro-bind /sys /sys\n" + extra)
    print("   wrapper 已补伪造项")
io.open(WRAPPER, "w", encoding="utf-8", newline="\n").write(s)
os.chmod(WRAPPER, 0o755)

p = subprocess.run(["bash", "-n", WRAPPER], capture_output=True, text=True)
print("   wrapper 语法:", "通过" if p.returncode == 0 else p.stderr[:200])
print("   当前绑定项数:", s.count("--ro-bind") + s.count("--tmpfs") + s.count("--bind") + s.count("--proc"))
print("   仍含 dmi 绑定:", "dmi" in s)
