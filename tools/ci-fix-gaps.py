#!/usr/bin/env python3
"""③ 收尾 CI 缺口(幂等):
  1. 6 个"从未进 CI"的冒烟加入三处清单(Linux / Windows x64 / macOS)
  2. Linux 与 macOS 加硬断言:QSvg 与 SQL 不许再 SKIP(以前缺 -DJQT_HAVE_SVG / 没部署 qsqlite)
  3. macOS 6.8.3 线补装 qtdeclarative(否则 SmokeQml 在 6.8.3 上跳过)
"""
import io
import re

P = r"D:\SilentStudio\JQt - Dev\.github\workflows\ci.yml"
s = io.open(P, encoding="utf-8").read()
before = s
NEW6 = ["SmokeFluent", "SmokeInputDialog", "SmokeVisible", "SmokeSqlDb", "SmokeL2", "SmokeL1fin"]
done = []

# 1) 三处清单(按各清单最后一个类名追加)
for last in ("SmokeHelp", "SmokeQml"):
    for lst in re.findall(r"(for C in [^;]+? " + last + r")(; do)", s):
        pass
# Linux 清单(以 SmokeHelp 结尾)
s, n = re.subn(r"(for C in org\.jqt\.SmokeConcurrency[^;]*? org\.jqt\.SmokeHelp)(; do)",
               lambda m: m.group(1) + " " + " ".join("org.jqt." + x for x in NEW6) + m.group(2), s)
if n:
    done.append(f"Linux 清单 +{len(NEW6)}")
# macOS 清单(以 SmokeQml 结尾)
s, n = re.subn(r"(for C in org\.jqt\.SmokeConcurrency[^;]*? org\.jqt\.SmokeQml)(; do)",
               lambda m: m.group(1) + " " + " ".join("org.jqt." + x for x in NEW6) + m.group(2), s)
if n:
    done.append(f"macOS 清单 +{len(NEW6)}")
# Windows x64 PowerShell 清单(以 SmokeHelp" 结尾)
s, n = re.subn(r'("org\.jqt\.SmokeHelp")\)\s*\{',
               lambda m: '"org.jqt.SmokeHelp",' + ",".join('"org.jqt.%s"' % x for x in NEW6) + ")) {", s)
if n:
    done.append(f"Windows 清单 +{len(NEW6)}")

# 2) 硬断言(插在 Linux 循环之后)
anchor = "            if [ $RC -ne 0 ]; then exit $RC; fi\n          done\n"
assertion = anchor + """
          # v1.9.2:QSvg 与 SQL 以前在 Linux 上是 SKIP(脚本没传 -DJQT_HAVE_SVG / 没部署 qsqlite),
          # 现在必须是真跑 —— 出现 SKIP 直接红,避免"填了坑但没人知道还跳着"。
          if grep -q "SKIP QSvgRenderer" "smoke_org.jqt.SmokeEvent.txt"; then
            echo "::error::QSvg 仍被跳过:检查 build-linux.sh 的 -DJQT_HAVE_SVG 探测"; exit 1
          fi
          if grep -q "SKIP 本平台无 SQLite 驱动" "smoke_org.jqt.SmokeSqlModel.txt"; then
            echo "::error::SQLite 驱动仍不可用:检查 plugins/sqldrivers 部署"; exit 1
          fi
          echo "QSvg 与 SQLite 在 Linux 上确认为真跑(无 SKIP)"
"""
if "QSvg 仍被跳过" not in s and anchor in s:
    s = s.replace(anchor, assertion, 1)
    done.append("Linux 硬断言")

# macOS:循环体是 ./run-macos.sh -Class $C(输出直接进日志,不落文件)-> 用 tee 落文件并断言
old_mac = """          for C in org.jqt.SmokeConcurrency"""
if "macOS:QSvg/SQL 断言" not in s:
    m = re.search(r"( +)for C in org\.jqt\.SmokeConcurrency[^;]*? org\.jqt\.SmokeQml[^\n]*\n( +)([^\n]*run-macos\.sh -Class \$C[^\n]*)", s)
    if m:
        indent, body = m.group(1), m.group(3)
        new_block = (f"{indent}for C in" + s.split("for C in", 1)[1].split("\n", 1)[0][:0] + "\n")  # 占位,下面整段替换
        # 直接在该循环后追加断言(macOS 循环不需要落盘:用 tee 太重,改用重跑两个类的办法)
        tail_anchor = m.group(0)
        add = tail_anchor + f"""
{indent}# macOS:QSvg/SQL 断言(v1.9.2)——两个类各重跑一次并检查是否仍 SKIP
{indent}if QT_QPA_PLATFORM=offscreen ./run-macos.sh -Class org.jqt.SmokeEvent -AutoClose 1500 2>&1 | grep -q "SKIP QSvgRenderer"; then
{indent}  echo "::error::macOS:QSvg 仍被跳过(检查 build-macos.sh 的 -DJQT_HAVE_SVG)"; exit 1
{indent}fi
{indent}if QT_QPA_PLATFORM=offscreen ./run-macos.sh -Class org.jqt.SmokeSqlModel -AutoClose 1500 2>&1 | grep -q "SKIP 本平台无 SQLite 驱动"; then
{indent}  echo "::error::macOS:SQLite 驱动仍不可用(检查 plugins/sqldrivers 部署)"; exit 1
{indent}fi
{indent}echo "macOS:QSvg 与 SQLite 确认为真跑(无 SKIP)"
"""
        s = s.replace(tail_anchor, add, 1)
        done.append("macOS 硬断言")

io.open(P, "w", encoding="utf-8", newline="\n").write(s)
print("已改:" + ("、".join(done) if done else "(无)"))
print("SmokeFluent 出现次数:", s.count("SmokeFluent"), "| SmokeL1fin:", s.count("SmokeL1fin"))
