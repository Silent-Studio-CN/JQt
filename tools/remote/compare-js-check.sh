#!/bin/bash
# 判断 JS 报错是"检查方法问题"还是"页面被改坏":
# 对改动前的备份做同样的检查,两者结果对比即可
check() {
  local f="$1" label="$2"
  echo "--- $label ---"
  python3 - "$f" <<'PY'
import io, re, subprocess, sys, tempfile, os
s = io.open(sys.argv[1], encoding="utf-8").read()
blocks = re.findall(r"<script>(.*?)</script>", s, re.S)
print(f"  内联脚本段数: {len(blocks)}")
for i, b in enumerate(blocks, 1):
    with tempfile.NamedTemporaryFile("w", suffix=".js", delete=False, encoding="utf-8") as f:
        f.write(b); p = f.name
    r = subprocess.run(["node", "--check", p], capture_output=True, text=True)
    first = (r.stderr.strip().splitlines()[0] if r.stderr.strip() else "")
    print(f"   第 {i} 段 ({len(b)} 字节): {'✅' if r.returncode == 0 else '❌ ' + first[:90]}")
    os.unlink(p)
PY
}

check /var/www/jqt/account/index.html.bak "改动前(备份)"
check /var/www/jqt/account/index.html     "改动后(当前)"

echo
echo "=== 用浏览器等价方式看:脚本是否被 HTML 解析器接受 ==="
python3 - <<'PY'
import io, re
for path, label in (("/var/www/jqt/account/index.html.bak", "备份"),
                    ("/var/www/jqt/account/index.html", "当前")):
    s = io.open(path, encoding="utf-8").read()
    print(f"  {label}: <script>={s.count('<script>')} </script>={s.count('</script>')} "
          f"大括号配平={s.count('{')-s.count('}')} 圆括号配平={s.count('(')-s.count(')')}")
PY
