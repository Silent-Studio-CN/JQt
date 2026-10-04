#!/usr/bin/env python3
"""最后一处残留:/sys/devices/system/cpu/ 目录里只有 12 个 cpuN。
造一个 512 个 cpuN 的假目录绑上去(配合已伪造的 online/possible/present)。
"""
import io
import os
import subprocess

FAKE = "/opt/jhy-sandbox/fake/sys-cpu"
THREADS = 512
WRAPPER = "/usr/local/bin/jhy-shell"

os.makedirs(FAKE, exist_ok=True)
for i in range(THREADS):
    os.makedirs(f"{FAKE}/cpu{i}/topology", exist_ok=True)
    # 每个核两个线程:同 core_id 的成对
    core = i // 2
    io.open(f"{FAKE}/cpu{i}/topology/core_id", "w").write(f"{core}\n")
    io.open(f"{FAKE}/cpu{i}/topology/physical_package_id", "w").write(f"{core // 128}\n")
    io.open(f"{FAKE}/cpu{i}/topology/thread_siblings_list", "w").write(f"{core * 2},{core * 2 + 1}\n")
    io.open(f"{FAKE}/cpu{i}/online", "w").write("1\n")

for name, val in (("online", f"0-{THREADS - 1}\n"),
                  ("possible", f"0-{THREADS - 1}\n"),
                  ("present", f"0-{THREADS - 1}\n"),
                  ("offline", "\n"),
                  ("isolated", "\n"),
                  ("kernel_max", "8191\n"),
                  ("uevent", "MODALIAS=cpu:type:x86,ven0000fam0006mod008F\n")):
    io.open(f"{FAKE}/{name}", "w").write(val)
print(f"   已造出假 CPU 目录:{len(os.listdir(FAKE))} 个条目")

# 旧的散装 cpu-* 文件仍然可用,但整目录绑定后会被覆盖;保留无妨
s = io.open(WRAPPER, encoding="utf-8").read()
if "sys-cpu" not in s:
    # 去掉散装 cpu-online/possible/present 绑定,换成整目录绑定
    for line in ("  --ro-bind /opt/jhy-sandbox/fake/cpu-online   /sys/devices/system/cpu/online\n",
                 "  --ro-bind /opt/jhy-sandbox/fake/cpu-possible /sys/devices/system/cpu/possible\n",
                 "  --ro-bind /opt/jhy-sandbox/fake/cpu-present  /sys/devices/system/cpu/present\n"):
        s = s.replace(line, "")
    s = s.replace("  --ro-bind /sys /sys\n",
                  "  --ro-bind /sys /sys\n"
                  "  --ro-bind /opt/jhy-sandbox/fake/sys-cpu /sys/devices/system/cpu\n")
    io.open(WRAPPER, "w", encoding="utf-8", newline="\n").write(s)
    os.chmod(WRAPPER, 0o755)
    print("   wrapper 已改为整目录绑定 CPU 拓扑")
p = subprocess.run(["bash", "-n", WRAPPER], capture_output=True, text=True)
print("   wrapper 语法:", "通过" if p.returncode == 0 else p.stderr[:300])
