#!/usr/bin/env python3
"""修 bwrap 的 DMI 绑定:只读 /sys 里无法逐个 --ro-bind 进去,
改为"复制真目录 + 覆盖厂商文件 + 整目录绑定 /sys/class/dmi/id"。"""
import io
import os
import shutil
import subprocess

FAKE = "/opt/jhy-sandbox/fake"
WRAPPER = "/usr/local/bin/jhy-shell"
DMI_SRC = "/sys/class/dmi/id"
DMI_FAKE = f"{FAKE}/dmi-id"

# 复制真目录(保留其它文件,看起来正常),再覆盖要改的
subprocess.run(["cp", "-a", f"{DMI_SRC}/.", DMI_FAKE], capture_output=True)
for name, val in (("sys_vendor", "Dell Inc."),
                  ("product_name", "PowerEdge R760"),
                  ("product_version", "Not Specified"),
                  ("board_vendor", "Dell Inc."),
                  ("board_name", "0P4X8N"),
                  ("bios_vendor", "Dell Inc."),
                  ("bios_version", "2.4.2"),
                  ("chassis_vendor", "Dell Inc.")):
    with io.open(f"{DMI_FAKE}/{name}", "w", encoding="utf-8") as f:
        f.write(val + "\n")
print("   已准备伪造的 DMI 目录:", sorted(os.listdir(DMI_FAKE))[:6], "...")

s = io.open(WRAPPER, encoding="utf-8").read()
old_block = s[s.find("  --ro-bind {0}/dmi/sys_vendor".format(FAKE)) if False else 0:0]
# 用正则式的字符串替换:把 8 行逐个绑定换成一整行目录绑定
import re
s2 = re.sub(r"(\s+--ro-bind \{FAKE\}/dmi/[a-z_]+ +\S+\n)+",
            "  --ro-bind {FAKE}/dmi-id /sys/class/dmi/id\n", s)
if s2 == s:
    s2 = re.sub(r"(\n  --ro-bind /opt/jhy-sandbox/fake/dmi/[a-z_]+ +\S+)+",
                "\n  --ro-bind /opt/jhy-sandbox/fake/dmi-id /sys/class/dmi/id", s)
if s2 != s:
    io.open(WRAPPER, "w", encoding="utf-8", newline="\n").write(s2)
    print("   wrapper 已改为整目录绑定 DMI")
else:
    print("   ⚠️ 未匹配到 DMI 绑定块,请人工检查 wrapper")

p = subprocess.run(["bash", "-n", WRAPPER], capture_output=True, text=True)
print("   wrapper 语法检查:", "通过" if p.returncode == 0 else p.stderr[:200])
print("   DMI 绑定行:", [l.strip() for l in io.open(WRAPPER, encoding="utf-8")
                        if "dmi" in l])
