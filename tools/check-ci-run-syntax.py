#!/usr/bin/env python3
"""把 ci.yml 里每个 `run:` 体抽出来单独做语法检查。

为什么需要它:一个 workflow 里几十个 run 块,YAML 合法**不代表** shell 合法
(引号/here-doc/函数体最容易写错),而 GitHub 只有在跑到那一步时才会报错 ——
那时候已经烧掉几分钟 runner。本脚本把这件事提前到本地:

  1. yaml.safe_load(ci.yml) —— 先保证 YAML 本身能过;
  2. 逐 job / 逐 step 抽出 run 体,按 step 的 `shell`(缺省按 runs-on 猜:
     windows-* -> pwsh,其余 -> bash)落成临时文件(**LF、UTF-8 无 BOM**);
  3. .sh 用 `bash -n` 检查,.ps1 用 PowerShell 的 Parser::ParseFile 检查;
  4. 任一失败 -> exit 1,并打印出错文件与行号。

用法:
    python tools/check-ci-run-syntax.py [workflow.yml ...]
    BASH=/usr/bin/bash python tools/check-ci-run-syntax.py     # Windows 上指定 bash

注意:这里只做**语法**检查,不做语义/环境检查 —— 变量有没有定义、镜像里有没有那个
包,它一概不知道(那是 CI 自己的事)。
"""
import io
import os
import shutil
import subprocess
import sys
import tempfile

try:
    import yaml
except ImportError:  # pragma: no cover
    print("需要 pyyaml: pip install pyyaml", file=sys.stderr)
    raise SystemExit(2)

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

PS_CHECKER = r"""param([string]$Path)
$errs = $null
$toks = $null
[System.Management.Automation.Language.Parser]::ParseFile($Path, [ref]$toks, [ref]$errs) | Out-Null
if ($errs -and $errs.Count -gt 0) {
  foreach ($e in $errs) { Write-Host ("  line {0}: {1}" -f $e.Extent.StartLineNumber, $e.Message) }
  exit 1
}
exit 0
"""


def find_bash():
    """找 bash:优先 $BASH,其次 Git for Windows,最后 PATH 上的 bash。"""
    cand = []
    if os.environ.get("BASH"):
        cand.append(os.environ["BASH"])
    cand += [
        r"C:\Program Files\Git\bin\bash.exe",
        r"C:\Program Files (x86)\Git\bin\bash.exe",
        "/bin/bash",
        "bash",
    ]
    for c in cand:
        if os.path.sep in c or (os.name == "nt" and ":" in c):
            if os.path.exists(c):
                return c
        else:
            w = shutil.which(c)
            if w:
                return w
    return None


def find_pwsh():
    for c in ("pwsh", "powershell"):
        w = shutil.which(c)
        if w:
            return w
    return None


def default_shell(job, step):
    """GitHub 的缺省 shell:windows runner 是 pwsh,其余是 bash;step.shell 可覆盖。"""
    sh = step.get("shell")
    if sh:
        return sh
    runs_on = job.get("runs-on", "")
    if isinstance(runs_on, str) and "windows" in runs_on.lower():
        return "pwsh"
    return "bash"


def main(argv):
    # Windows 控制台默认是 GBK,打印 ✅/❌ 会 UnicodeEncodeError;这里降级成替换而不是崩掉
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(errors="replace")
        except Exception:
            pass
    files = argv[1:] or [".github/workflows/ci.yml"]
    bash = find_bash()
    pwsh = find_pwsh()
    print(f"bash  : {bash or '(未找到 —— .sh 检查会跳过)'}")
    print(f"pwsh  : {pwsh or '(未找到 —— .ps1 检查会跳过)'}")

    workdir = tempfile.mkdtemp(prefix="ci-run-syntax-")
    # pwsh 的检查器写成文件调用,避免 -Command 的引号地狱
    checker = os.path.join(workdir, "_ps_check.ps1")
    with io.open(checker, "w", encoding="utf-8", newline="\n") as f:
        f.write(PS_CHECKER)

    total = 0
    failed = []
    for rel in files:
        path = rel if os.path.isabs(rel) else os.path.join(ROOT, rel)
        with io.open(path, encoding="utf-8") as f:
            doc = yaml.safe_load(f)
        print(f"\n== {rel} ==")
        # on: 里的东西不是 job;有 steps 的才算
        for job_id, job in (doc.get("jobs") or {}).items():
            for i, step in enumerate(job.get("steps") or []):
                body = step.get("run")
                if body is None:
                    continue
                total += 1
                shell = default_shell(job, step)
                is_ps = shell.lower().startswith(("pwsh", "powershell"))
                ext = ".ps1" if is_ps else ".sh"
                safe = "".join(ch if ch.isalnum() or ch in "-_." else "_" for ch in str(job_id))
                out = os.path.join(workdir, f"{safe}__{i:02d}{ext}")
                # 必须是 LF + UTF-8(无 BOM):带 CRLF 的 bash 脚本会 `do\r` 语法错
                with io.open(out, "w", encoding="utf-8", newline="\n") as f:
                    f.write(body if body.endswith("\n") else body + "\n")

                name = step.get("name") or f"step#{i}"
                if is_ps:
                    if not pwsh:
                        print(f"  [skip] {job_id} / {name} (无 pwsh)")
                        continue
                    cp = subprocess.run([pwsh, "-NoProfile", "-NonInteractive", "-File", checker, out],
                                        capture_output=True, text=True, encoding="utf-8", errors="replace")
                else:
                    if not bash:
                        print(f"  [skip] {job_id} / {name} (无 bash)")
                        continue
                    cp = subprocess.run([bash, "-n", out],
                                        capture_output=True, text=True, encoding="utf-8", errors="replace")
                if cp.returncode == 0:
                    print(f"  [ok]   {job_id} / {name}  ({shell}, {len(body.splitlines())} 行)")
                else:
                    print(f"  [FAIL] {job_id} / {name}  ({shell})  -> {out}")
                    for line in (cp.stdout or "").splitlines() + (cp.stderr or "").splitlines():
                        print("       " + line)
                    failed.append(f"{rel} :: {job_id} :: {name}")

    print(f"\n共检查 {total} 个 run 块,temp: {workdir}")
    if failed:
        print(f"❌ 语法失败 {len(failed)} 个:")
        for x in failed:
            print("   - " + x)
        return 1
    print("✅ 全部 run 块语法通过")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
