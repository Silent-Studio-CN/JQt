//! jqt-build-runner — JQt 独立构建器（无 PowerShell 层，Agent/CI 直接调用）
//!
//! 用法:
//!   jqt-build-runner.exe --repo <JQt目录> --qt-root <Qt/6.11.2> --mingw <MinGW根> --jdk <JDK目录> [--stage all|java|native|deploy]
//!
//! 步骤（镜像 build.ps1）:
//!   1/5 javac 编译 Java + 生成 JNI 头
//!   2/5 g++ 编译 jqt_bridge.cpp → lib/jqt.dll
//!   3/5 windeployqt 部署 Qt 运行库
//!   4/5 QPA 平台插件 + SQL 驱动 + qt.conf
//!   5/5 LGPL 许可文件
//!
//! 设计: 纯 std，无依赖；Command.arg() 传参数数组（CreateProcess 直传，
//!       无 shell 解析 → 无引号/转义问题）；子进程输出 inherit 透传。

use std::path::{Path, PathBuf};
use std::process::{exit, Command, Stdio};
use std::time::Instant;

struct Cfg {
    repo: PathBuf,
    qt: PathBuf,
    mingw: PathBuf,
    jdk: PathBuf,
    stage: String,
    libstdcxx: bool,   // llvm-mingw(clang) 默认 libc++，与 Qt(libstdc++) ABI 不匹配 → --stdlib=libstdc++
}

fn usage() {
    eprintln!("jqt-build-runner 1.0 — JQt 独立构建器\n用法: jqt-build-runner.exe --repo <dir> --qt-root <Qt/6.11.2> --mingw <dir> --jdk <dir> [--stage all|java|native|deploy]");
}

fn main() {
    let args: Vec<String> = std::env::args().collect();
    let mut repo = PathBuf::from(".");
    let mut qt: Option<PathBuf> = None;
    let mut mingw: Option<PathBuf> = None;
    let mut jdk: Option<PathBuf> = None;
    let mut stage = String::from("all");
    let mut libstdcxx = false;
    let mut i = 1;
    while i < args.len() {
        match args[i].as_str() {
            "--repo" => { i += 1; if i < args.len() { repo = PathBuf::from(&args[i]); } }
            "--qt-root" => { i += 1; if i < args.len() { qt = Some(PathBuf::from(&args[i])); } }
            "--mingw" => { i += 1; if i < args.len() { mingw = Some(PathBuf::from(&args[i])); } }
            "--jdk" => { i += 1; if i < args.len() { jdk = Some(PathBuf::from(&args[i])); } }
            "--stage" => { i += 1; if i < args.len() { stage = args[i].clone(); } }
            "--libstdcxx" => libstdcxx = true,
            "--help" | "-h" => { usage(); return; }
            other => { eprintln!("未知参数: {other}"); usage(); exit(2); }
        }
        i += 1;
    }
    let qt = match qt { Some(q) => q, None => { eprintln!("缺少 --qt-root"); usage(); exit(2); } };
    let mingw = match mingw { Some(m) => m, None => { eprintln!("缺少 --mingw"); usage(); exit(2); } };
    let jdk = match jdk { Some(j) => j, None => { eprintln!("缺少 --jdk"); usage(); exit(2); } };
    let cfg = Cfg { repo, qt, mingw, jdk, stage, libstdcxx };
    run(&cfg);
}

fn run_cmd(cmd: &mut Command, what: &str) -> bool {
    match cmd.stdout(Stdio::inherit()).stderr(Stdio::inherit()).status() {
        Ok(s) if s.success() => true,
        Ok(s) => { eprintln!("[jqt-build] {what} 退出码 {}", s.code().unwrap_or(-1)); false }
        Err(e) => { eprintln!("[jqt-build] {what} 启动失败: {e}"); false }
    }
}

fn fail(what: &str) -> ! {
    eprintln!("[jqt-build] 构建失败: {what}");
    exit(1);
}

fn find_java_files(repo: &Path) -> Vec<PathBuf> {
    let mut out = Vec::new();
    let mut stack = vec![repo.join("java")];
    while let Some(dir) = stack.pop() {
        if let Ok(rd) = std::fs::read_dir(&dir) {
            for e in rd.flatten() {
                let p = e.path();
                if p.is_dir() { stack.push(p); }
                else if p.extension().map(|x| x == "java").unwrap_or(false) { out.push(p); }
            }
        }
    }
    out.sort();
    out
}

fn stage_java(cfg: &Cfg, out_dir: &Path, gen_dir: &Path) {
    let files = find_java_files(&cfg.repo);
    println!("==> [1/5] javac ({} java files)", files.len());
    let mut cmd = Command::new(cfg.jdk.join("bin").join("javac.exe"));
    cmd.arg("-encoding").arg("UTF-8").arg("-d").arg(out_dir).arg("-h").arg(gen_dir);
    for f in &files { cmd.arg(f); }
    if !run_cmd(&mut cmd, "javac") { fail("javac failed"); }
}

fn stage_native(cfg: &Cfg, lib_dir: &Path, native_dir: &Path) {
    println!("==> [2/5] g++ -> jqt.dll");
    let kit = cfg.qt.join("mingw_64");
    let mut cmd = Command::new(cfg.mingw.join("bin").join("g++.exe"));
    cmd.arg("-std=c++17").arg("-O2").arg("-shared")
        .arg("-o").arg(lib_dir.join("jqt.dll"))
        .arg("-I").arg(cfg.jdk.join("include"))
        .arg("-I").arg(cfg.jdk.join("include").join("win32"))
        .arg("-I").arg(kit.join("include"))
        .arg("-I").arg(kit.join("include").join("QtWidgets"))
        .arg("-I").arg(kit.join("include").join("QtGui"))
        .arg("-I").arg(kit.join("include").join("QtCore"))
        .arg("-I").arg(kit.join("include").join("QtPrintSupport"))
        .arg("-I").arg(kit.join("include").join("QtSql"))
        .arg("-I").arg(kit.join("include").join("QtOpenGLWidgets"))
        .arg("-I").arg(kit.join("include").join("QtSerialPort"))
        .arg("-I").arg(kit.join("include").join("QtOpenGL"))
        .arg("-I").arg(native_dir)
        .arg(native_dir.join("jqt_bridge.cpp"))
        .arg("-L").arg(kit.join("lib"))
        .arg("-lQt6Widgets").arg("-lQt6Gui").arg("-lQt6Core")
        .arg("-lQt6PrintSupport").arg("-lQt6Sql").arg("-lQt6OpenGLWidgets")
        .arg("-lQt6OpenGL").arg("-lQt6SerialPort")
        .arg("-lole32").arg("-luuid").arg("-loleaut32")
        .arg("-static-libgcc").arg("-static-libstdc++");
    if cfg.libstdcxx { cmd.arg("--stdlib=libstdc++"); }
    if !run_cmd(&mut cmd, "g++") { fail("g++ failed"); }
}

fn stage_deploy(cfg: &Cfg, lib_dir: &Path, repo: &Path) {
    let kit = cfg.qt.join("mingw_64");
    println!("==> [3/5] windeployqt -> lib");
    let mut cmd = Command::new(kit.join("bin").join("windeployqt.exe"));
    cmd.arg("--no-translations").arg("--no-system-d3d-compiler")
        .arg("--no-opengl-sw").arg("--compiler-runtime")
        .arg(lib_dir.join("jqt.dll"));
    if !run_cmd(&mut cmd, "windeployqt") {
        eprintln!("[jqt-build] windeployqt 失败，手动拷贝 Qt 运行库");
        for dll in ["Qt6Core.dll", "Qt6Gui.dll", "Qt6Widgets.dll"] {
            let _ = std::fs::copy(kit.join("bin").join(dll), lib_dir.join(dll));
        }
        let _ = std::fs::copy(cfg.mingw.join("bin").join("libwinpthread-1.dll"), lib_dir.join("libwinpthread-1.dll"));
    }
    println!("==> [4/5] QPA plugin + sqldrivers + qt.conf");
    let platforms = lib_dir.join("plugins").join("platforms");
    let sqldrivers = lib_dir.join("plugins").join("sqldrivers");
    let _ = std::fs::create_dir_all(&platforms);
    let _ = std::fs::create_dir_all(&sqldrivers);
    let _ = std::fs::copy(kit.join("plugins").join("platforms").join("qwindows.dll"), platforms.join("qwindows.dll"));
    if let Ok(rd) = std::fs::read_dir(kit.join("plugins").join("sqldrivers")) {
        for e in rd.flatten() {
            if e.file_name().to_string_lossy().contains("qsqlite") {
                let _ = std::fs::copy(e.path(), sqldrivers.join(e.file_name()));
            }
        }
    }
    let _ = std::fs::write(lib_dir.join("qt.conf"), "[Paths]\nPlugins = plugins\n");
    println!("==> [5/5] LGPL license notices");
    for f in ["LGPL-3.0.txt", "THIRD-PARTY-NOTICES.md", "LICENSE.md", "LICENSE"] {
        let src = repo.join(f);
        if src.exists() {
            let _ = std::fs::copy(&src, lib_dir.join(f));
        }
    }
}

fn run(cfg: &Cfg) {
    let t0 = Instant::now();
    let mut path = String::new();
    path.push_str(&cfg.mingw.join("bin").display().to_string());
    path.push(';');
    path.push_str(&cfg.qt.join("mingw_64").join("bin").display().to_string());
    if let Ok(p) = std::env::var("PATH") {
        path.push(';');
        path.push_str(&p);
    }
    std::env::set_var("PATH", path);

    let out_dir = cfg.repo.join("out");
    let lib_dir = cfg.repo.join("lib");
    let gen_dir = cfg.repo.join("native").join("generated");
    for d in [&out_dir, &lib_dir, &gen_dir] {
        let _ = std::fs::create_dir_all(d);
    }

    match cfg.stage.as_str() {
        "java" => stage_java(cfg, &out_dir, &gen_dir),
        _ => {
            stage_java(cfg, &out_dir, &gen_dir);
            stage_native(cfg, &lib_dir, &cfg.repo.join("native"));
            stage_deploy(cfg, &lib_dir, &cfg.repo);
        }
    }
    println!("\nBuild OK in {:.1}s", t0.elapsed().as_secs_f64());
    println!("  Dynamic lib : {}", lib_dir.join("jqt.dll").display());
}
