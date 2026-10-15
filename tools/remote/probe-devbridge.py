#!/usr/bin/env python3
"""探测 devbridge 两台开发环境的可用工具链(JQt 后续要用)"""
import json
import subprocess
import urllib.request

TOKEN = "ea28e78759b9b81bde5d4ae0babb2149"
BASE = "http://192.168.200.125:7788"


def post(path, payload, timeout=120):
    req = urllib.request.Request(
        f"{BASE}{path}?token={TOKEN}",
        data=json.dumps(payload).encode(),
        headers={"Content-Type": "application/json", "X-Token": TOKEN})
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return json.loads(r.read().decode())


MAC = r'''
echo "== macOS =="; sw_vers; uname -m
echo "-- 编译器/工具 --"; clang++ --version | head -2; cmake --version | head -1; git --version
echo "-- JDK --"; (java -version 2>&1 | head -2); echo "JAVA_HOME=$JAVA_HOME"
echo "-- Qt 安装 --"; ls -d ~/Qt/*/  2>/dev/null | head -10; ls -d /opt/Qt/*/ 2>/dev/null | head
echo "-- Qt 版本目录 --"; for d in ~/Qt/6*/macos; do [ -d "$d" ] && echo "  $d"; done
echo "-- qmake/clang_64 --"; ls ~/Qt/6*/macos/bin/qmake 2>/dev/null; ls ~/Qt/6*/macos/lib/QtWidgets.framework 2>/dev/null | head -2
echo "-- 关键 Qt 模块 --"; for m in QtWebView QtWebEngineCore QtCharts QtMultimedia QtQuick3D QtWebSockets QtPositioning; do
  [ -e ~/Qt/6*/macos/lib/$m.framework ] && echo "  有 $m"; done
echo "-- JQt 仓库 --"; ls -d ~/jqt ~/Desktop/jqt 2>/dev/null; ls ~/jqt 2>/dev/null | head -8
'''

WIN = r'''
Write-Output "== Windows VM =="
(Get-CimInstance Win32_OperatingSystem).Caption
$env:PROCESSOR_ARCHITECTURE
Write-Output "-- MSVC --"
$vswhere = "${env:ProgramFiles(x86)}\Microsoft Visual Studio\Installer\vswhere.exe"
if (Test-Path $vswhere) { & $vswhere -latest -property displayName, installationVersion } else { Write-Output "  vswhere 不在" }
Get-ChildItem "C:\Program Files\Microsoft Visual Studio" -Directory -ErrorAction SilentlyContinue | ForEach-Object { "  " + $_.Name }
Write-Output "-- Qt --"
foreach ($d in @("C:\Qt","D:\Qt","$env:USERPROFILE\Qt")) { if (Test-Path $d) { Get-ChildItem $d -Directory | ForEach-Object { "  $d\$($_.Name)" } } }
Write-Output "-- 其他 --"
(git --version) 2>&1 | Select-Object -First 1
(java -version 2>&1) | Select-Object -First 1
'''

for path, label, cmd in (("/run", "macOS arm64", MAC), ("/win", "Windows 11 ARM64", WIN)):
    print(f"════ {label} ({path}) ════")
    try:
        r = post(path, {"cmd": cmd, "timeout": 180})
        out = r.get("stdout") or r.get("output") or json.dumps(r, ensure_ascii=False)
        print("\n".join("   " + l for l in out.strip().splitlines()[:40]))
        if r.get("stderr"):
            print("   [stderr]", str(r["stderr"])[:200])
        if r.get("exit") not in (0, None):
            print("   [exit]", r.get("exit"))
    except Exception as e:
        print("   ❌ 失败:", str(e)[:200])
    print()
