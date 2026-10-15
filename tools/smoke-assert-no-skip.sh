#!/usr/bin/env bash
# ============================================================================
# smoke-assert-no-skip.sh - 硬断言:QSvg 与 SQLite 在 Linux/macOS 上**必须真跑**
#
# 为什么需要它:v1.9.2 之前,Linux 与 macOS 上
#   · SmokeEvent   -> "[ev] SKIP QSvgRenderer(本库未编译 QtSvg 支持)"
#   · SmokeSqlModel-> "[sqlm] SKIP 本平台无 SQLite 驱动: ..."
# 一直是"绿的 SKIP"—— 填了坑没人知道还跳着。这个脚本把"还在跳"变成 CI 红。
#
# 用法(两个平台的冒烟输出文件都必须已经落盘):
#   bash tools/smoke-assert-no-skip.sh <平台标签> <SmokeEvent 输出> <SmokeSqlModel 输出>
#
# 注意断言是**两段式**的:
#   1) 反向:出现 SKIP 行 -> exit 1;
#   2) 正向:必须看得到"真跑过"的实测行 —— 否则文件为空/内容不符时
#      grep 找不到 SKIP 会假通过,断言就变成了摆设。
# ============================================================================
set -euo pipefail

LABEL="${1:?usage: smoke-assert-no-skip.sh <label> <SmokeEvent log> <SmokeSqlModel log>}"
EVENT_LOG="${2:?missing SmokeEvent log path}"
SQL_LOG="${3:?missing SmokeSqlModel log path}"

rc=0

# ---- 0. 前置:输出文件必须存在且非空(防止"文件没生成"造成假通过) ----
for f in "$EVENT_LOG" "$SQL_LOG"; do
  if [ ! -s "$f" ]; then
    echo "::error::$LABEL 冒烟输出缺失或为空: $f"
    rc=1
  fi
done
if [ $rc -ne 0 ]; then
  exit 1
fi

# ---- 1. 反向断言:不许再 SKIP ----
if grep -q "SKIP QSvgRenderer" "$EVENT_LOG"; then
  echo "::error::$LABEL: SmokeEvent 仍跳过 QSvgRenderer —— 检查构建脚本的 -DJQT_HAVE_SVG 探测,以及 CI 是否装了 qtsvg(独立 7z)"
  grep -n "SKIP QSvgRenderer" "$EVENT_LOG" || true
  rc=1
fi

if grep -q "SKIP 本平台无 SQLite 驱动" "$SQL_LOG"; then
  echo "::error::$LABEL: SmokeSqlModel 仍无 SQLite 驱动 —— 检查构建脚本是否把 libqsqlite 部署成 <repo>/lib/sqldrivers/qsqlite.<so|dylib>"
  grep -n "SKIP 本平台无 SQLite 驱动" "$SQL_LOG" || true
  rc=1
fi

# SmokeSqlModel 还有第二条 SKIP 路径(驱动在但 :memory: 打不开),同样不该出现 ——
# 否则"SQLite 真的可用"这句话就不成立。
if grep -q "\[sqlm\] SKIP" "$SQL_LOG"; then
  echo "::error::$LABEL: SmokeSqlModel 走了 SKIP 分支(SQLite 未真正跑起来)"
  grep -n "\[sqlm\] SKIP" "$SQL_LOG" || true
  rc=1
fi

if [ $rc -ne 0 ]; then
  exit 1
fi

# ---- 2. 正向断言:确认确实跑到了实测断言 ----
#   SmokeEvent  只有非 SKIP 分支才会打印 "SVG 解析成功";注意必须锚定 "[ev] OK" ——
#               check() 失败时打的是 "[ev] FAIL SVG 解析成功(...)",同样含这几个字,
#               不锚定的话"跑了但断言失败"会被当成通过(退出码那条防线另算)。
#   SmokeSqlModel 的两条 SKIP 分支都提前 return,只有真跑完才会打印 "[sqlm] pass=N fail=0".
if ! grep -qE "\[ev\] OK +SVG 解析成功" "$EVENT_LOG"; then
  echo "::error::$LABEL: SmokeEvent 里没有通过的 QSvgRenderer 实测断言(既没 SKIP 也没真跑成功?)"
  tail -20 "$EVENT_LOG" || true
  exit 1
fi

if ! grep -qE "\[sqlm\] pass=[0-9]+ fail=0" "$SQL_LOG"; then
  echo "::error::$LABEL: SmokeSqlModel 里没有 \"[sqlm] pass=N fail=0\"(既没 SKIP 也没真跑成功?)"
  tail -20 "$SQL_LOG" || true
  exit 1
fi

echo "$LABEL: QSvgRenderer 与 SQLite 均为真跑(无 SKIP)✅"
