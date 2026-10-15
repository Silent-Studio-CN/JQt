#!/usr/bin/env python3
"""回退 ③ 里有风险的两处(保留确定有效的部分):
  · build-linux.sh / build-macos.sh 回退到加 SVG/sqldrivers/WebView 之前(git checkout)
  · ci.yml 去掉 Linux/macOS 的"QSvg/SQL 不许 SKIP"硬断言(修复未生效,断言只会让 CI 红)
  保留:6 个冒烟进 CI、build.ps1 的 WebView 部署(本地已验证)、DBus/状态机等既有改动。
"""
import io
import subprocess

R = r"D:\SilentStudio\JQt - Dev"

# 回退两个 shell 脚本到上一提交
for f in ("build-linux.sh", "build-macos.sh"):
    r = subprocess.run(["git", "checkout", "da91d96^", "--", f], cwd=R,
                       capture_output=True, text=True)
    print(f"  {f}: {'已回退' if r.returncode == 0 else '回退失败 ' + r.stderr[:120]}")

# 去掉 ci.yml 里的两段断言
p = R + r"\.github\workflows\ci.yml"
s = io.open(p, encoding="utf-8").read()
before = s
start = s.find("          # v1.9.2:QSvg 与 SQL 以前在 Linux 上是 SKIP")
end = s.find("          echo \"QSvg 与 SQLite 在 Linux 上确认为真跑(无 SKIP)\"\n")
if start != -1 and end != -1:
    s = s[:start] + s[end + len("          echo \"QSvg 与 SQLite 在 Linux 上确认为真跑(无 SKIP)\"\n"):]
m_start = s.find("          # macOS:QSvg/SQL 断言(v1.9.2)")
m_end = s.find("          echo \"macOS:QSvg 与 SQLite 确认为真跑(无 SKIP)\"\n")
if m_start != -1 and m_end != -1:
    s = s[:m_start] + s[m_end + len("          echo \"macOS:QSvg 与 SQLite 确认为真跑(无 SKIP)\"\n"):]
io.open(p, "w", encoding="utf-8", newline="\n").write(s)
print(f"  ci.yml: 断言已移除 = {'QSvg 仍被跳过' not in s and 'macOS:QSvg' not in s}")
print(f"  6 个冒烟仍在 CI = {'SmokeFluent' in s and 'SmokeL1fin' in s}")
