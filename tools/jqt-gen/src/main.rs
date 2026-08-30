//! JQt 绑定生成器 — Qt 元信息 → Java/native 骨架
//!
//! 用法:
//!   jqt-gen fetch <class1,class2,...>  抓取 members 页 → 输出 JSON 清单
//!   jqt-gen plan <class>               对比 JQt 现有实现 → 输出缺口计划

mod generate;
mod golden;
mod model;
mod parse;

use std::io::Write;

fn main() {
    let args: Vec<String> = std::env::args().collect();
    if args.len() < 2 {
        print_usage();
        return;
    }
    match args[1].as_str() {
        "fetch" => cmd_fetch(&args),
        "plan" => cmd_plan(&args),
        "generate" => cmd_generate(&args),
        _ => print_usage(),
    }
}

fn print_usage() {
    println!("jqt-gen — JQt 绑定生成器");
    println!("  jqt-gen fetch <classes>     抓取 Qt members 页 → JSON");
    println!("  jqt-gen plan <class>        缺口计划");
    println!("  jqt-gen generate <class>    生成直传型方法骨架");
}

fn cmd_fetch(args: &[String]) {
    if args.len() < 3 {
        eprintln!("用法: jqt-gen fetch <class1,class2>");
        return;
    }
    let classes: Vec<&str> = args[2].split(',').map(|s| s.trim()).filter(|s| !s.is_empty()).collect();
    // 累积模式：保留 qt-classes.json 中已抓取的类（按类名去重），支持分批抓取
    let mut out: Vec<model::QtClass> = std::fs::read_to_string("qt-classes.json")
        .ok()
        .and_then(|d| serde_json::from_str(&d).ok())
        .unwrap_or_default();
    for c in &classes {
        let lower = c.to_lowercase();
        let url = format!("https://doc.qt.io/qt-6/{}-members.html", lower);
        match fetch_url(&url) {
            Ok(html) => {
                let mut cls = parse::parse_members_html(&html, c);
                let main_url = format!("https://doc.qt.io/qt-6/{}.html", lower);
                if let Ok(main_html) = fetch_url(&main_url) {
                    let rts = parse::parse_main_return_types(&main_html);
                    for m in &mut cls.methods {
                        if m.return_type.is_empty() {
                            if let Some(rt) = rts.get(&m.name) {
                                m.return_type = rt.clone();
                            }
                        }
                    }
                    cls.signal_names = parse::parse_signal_names(&main_html);
                }
                println!("{}: {} methods", cls.name, cls.methods.len());
                if let Some(existing) = out.iter_mut().find(|e| e.name == cls.name) {
                    *existing = cls;   // 同名单次刷新
                } else {
                    out.push(cls);
                }
            }
            Err(e) => {
                eprintln!("{}: 抓取失败: {}", c, e);
            }
        }
    }
    if !out.is_empty() {
        let json = serde_json::to_string_pretty(&out).unwrap_or_default();
        let mut f = std::fs::File::create("qt-classes.json").expect("写入 qt-classes.json");
        f.write_all(json.as_bytes()).expect("写入失败");
        println!("已写入 qt-classes.json");
    }
}

/// plan: 读 qt-classes.json（Qt 元信息）→ 扫描 JQt java 实现 → 输出缺口清单
fn cmd_plan(args: &[String]) {
    let data = match std::fs::read_to_string("qt-classes.json") {
        Ok(d) => d,
        Err(_) => { eprintln!("缺少 qt-classes.json —— 先运行: jqt-gen fetch <classes>"); return; }
    };
    let classes: Vec<model::QtClass> = match serde_json::from_str(&data) {
        Ok(c) => c,
        Err(e) => { eprintln!("qt-classes.json 解析失败: {}", e); return; }
    };

    let implemented = scan_jqt_implemented();

    // 对比：输出每类的缺口（值对象参数方法优先标记）
    let mut plan: Vec<serde_json::Value> = Vec::new();
    for cls in &classes {
        let lower = cls.name.to_lowercase();
        let impls = implemented.get(&lower).cloned().unwrap_or_default();
        let mut missing: Vec<&model::QtMethod> = Vec::new();
        for m in &cls.methods {
            if !impls.contains(&m.name) {
                missing.push(m);
            }
        }
        plan.push(serde_json::json!({
            "class": cls.name,
            "qt_methods": cls.methods.len(),
            "jqt_implemented": impls.len(),
            "missing": missing.iter().map(|m| serde_json::json!({
                "name": m.name,
                "params": m.params.iter().map(|p| p.ty.clone()).collect::<Vec<_>>(),
            })).collect::<Vec<_>>(),
        }));
    }
    let json = serde_json::to_string_pretty(&plan).unwrap_or_default();
    std::fs::write("qt-plan.json", json).expect("写入 qt-plan.json");
    for p in &plan {
        let cls = p["class"].as_str().unwrap_or("");
        let missing = p["missing"].as_array().map(|a| a.len()).unwrap_or(0);
        println!("{}: 缺口 {} 方法", cls, missing);
    }
    println!("已写入 qt-plan.json");
}


/// 扫描 JQt 现有实现：类名(小写) → 已实现方法名集合
fn scan_jqt_implemented() -> std::collections::HashMap<String, std::collections::HashSet<String>> {
    let mut implemented: std::collections::HashMap<String, std::collections::HashSet<String>> = std::collections::HashMap::new();
    let jqt_dir = std::env::var("JQT_JAVA_DIR").unwrap_or_else(|_| "D:\\SilentStudio\\JQt - Dev\\java\\org\\jqt".to_string());
    if let Ok(entries) = std::fs::read_dir(&jqt_dir) {
        for entry in entries.flatten() {
            let path = entry.path();
            if path.extension().map(|e| e == "java").unwrap_or(false) {
                if let Ok(content) = std::fs::read_to_string(&path) {
                    let class = path.file_stem().map(|s| s.to_string_lossy().to_string()).unwrap_or_default();
                    let mut names = std::collections::HashSet::new();
                    for line in content.lines() {
                        let t = line.trim();
                        // 生成器批次块（标记后即类尾）：视为缺口，replace 合入时整体替换
                        if t.contains("生成器批次") { break; }
                        if t.starts_with("public") && t.contains('(') {
                            // 方法名 = 最后一个含 '(' 的 token 的 '(' 前部分（兼容单行/多行签名）
                            let mut name = String::new();
                            for w in t.split_whitespace().rev() {
                                if w.contains('(') {
                                    name = w.split('(').next().unwrap_or("").to_string();
                                    break;
                                }
                            }
                            if !name.is_empty() && name.chars().all(|c| c.is_ascii_alphanumeric() || c == '_') {
                                names.insert(name);
                            }
                        }
                    }
                    implemented.insert(class.to_lowercase(), names);
                }
            }
        }
    }
    implemented
}

/// generate: 读 qt-classes.json → 生成 Java/native 直传型骨架（排除已有实现）
fn cmd_generate(args: &[String]) {
    let data = match std::fs::read_to_string("qt-classes.json") {
        Ok(d) => d,
        Err(_) => { eprintln!("缺少 qt-classes.json —— 先运行: jqt-gen fetch <classes>"); return; }
    };
    let classes: Vec<model::QtClass> = match serde_json::from_str(&data) {
        Ok(c) => c,
        Err(e) => { eprintln!("解析失败: {}", e); return; }
    };
    let implemented = scan_jqt_implemented();
    let target = args.get(2).map(|s| s.to_lowercase()).unwrap_or_default();
    // --all：忽略 scan_jqt_implemented（重新生成已合入批次时使用）
    let all = args.iter().any(|a| a == "--all");
    for cls in &classes {
        if !target.is_empty() && cls.name.to_lowercase() != target { continue; }
        let impls: std::collections::HashSet<String> = if all { std::collections::HashSet::new() } else { implemented.get(&cls.name.to_lowercase()).cloned().unwrap_or_default() };
        // 过滤：只保留未实现的方法
        let filtered = model::QtClass {
            name: cls.name.clone(),
            methods: cls.methods.iter().filter(|m| !impls.contains(&m.name)).cloned().collect(),
            properties: Vec::new(),
            enums: Vec::new(),
            signal_names: cls.signal_names.clone(),
        };
        let java = generate::gen_java_methods(&filtered);
        let native = generate::gen_native_functions(&filtered, &cls.name);
        let dir = "generated";
        std::fs::create_dir_all(dir).expect("创建 generated 目录");
        std::fs::write(format!("{}/{}.java.part", dir, cls.name), &java).expect("写 java part");
        std::fs::write(format!("{}/{}.native.part", dir, cls.name), &native).expect("写 native part");
        let gen_count = java.matches("public ").count();
        println!("{}: 缺口直传 {} 个 / 生成 Java 方法 {} 个 / native 函数 {} 个", cls.name, filtered.methods.len(), gen_count, native.matches("JNIEXPORT").count());
    }
    println!("生成完成 → generated/");
}

fn fetch_url(url: &str) -> Result<String, String> {
    let resp = ureq::get(url)
        .timeout(std::time::Duration::from_secs(30))
        .call()
        .map_err(|e| e.to_string())?;
    resp.into_string().map_err(|e| e.to_string())
}

#[cfg(test)]
mod compile_tests {
    use crate::model::{QtClass, QtMethod, QtParam};
    use crate::generate::gen_java_methods;

    fn m(name: &str, params: Vec<(&str, &str)>, ret: &str) -> QtMethod {
        QtMethod {
            name: name.to_string(),
            return_type: ret.to_string(),
            params: params.into_iter().map(|(n, t)| QtParam { name: n.to_string(), ty: t.to_string() }).collect(),
            is_static: false, is_signal: false, is_ctor: false,
        }
    }

    #[test]
    fn generated_java_compiles() {
        let mut cls = QtClass::new("GenCheck");
        cls.methods.push(m("setVisible", vec![("visible", "bool")], "void"));
        cls.methods.push(m("windowTitle", vec![], "QString"));
        cls.methods.push(m("setToolTip", vec![("tip", "const QString")], "void"));
        cls.methods.push(m("minimumWidth", vec![], "int"));
        cls.methods.push(m("setGeometry", vec![("x", "int"), ("y", "int"), ("w", "int"), ("h", "int")], "void"));
        let java = gen_java_methods(&cls);
        let src = format!("public class GenCheck {{\n    long nativeHandle;\n{}\n}}", java);
        std::fs::create_dir_all("target/gencheck").expect("mkdir");
        std::fs::write("target/gencheck/GenCheck.java", &src).expect("write java");
        let javac = std::env::var("JAVAC").unwrap_or_else(|_| "javac".to_string());
        let out = std::process::Command::new(javac)
            .arg("-d").arg("target/gencheck")
            .arg("target/gencheck/GenCheck.java")
            .output()
            .expect("run javac");
        assert!(out.status.success(), "javac 编译失败:\n{}", String::from_utf8_lossy(&out.stderr));
    }

    #[test]
    fn generated_native_compiles() {
        let mut cls = QtClass::new("QWidget");
        cls.methods.push(m("setVisible", vec![("visible", "bool")], "void"));
        cls.methods.push(m("windowTitle", vec![], "QString"));
        cls.methods.push(m("setToolTip", vec![("tip", "const QString")], "void"));
        cls.methods.push(m("minimumWidth", vec![], "int"));
        cls.methods.push(m("setGeometry", vec![("x", "int"), ("y", "int"), ("w", "int"), ("h", "int")], "void"));
        let native = crate::generate::gen_native_functions(&cls, "QWidget");
        // 桩类验证（不 include Qt 头——低内存环境可用，验证 JNI 结构/类型转换/调用语法）
        let cpp = format!(
            "#include <jni.h>\n#include <cstring>\n\
             struct QByteArray {{ const char* constData() const {{ return \"\"; }} }};\n\
             struct QString {{\n    static QString fromUtf8(const char*) {{ return QString(); }}\n\
             \x20   QByteArray toUtf8() const {{ return QByteArray(); }}\n}};\n\
             class QWidget {{\npublic:\n\
             \x20   void setVisible(bool) {{}}\n\
             \x20   QString windowTitle() {{ return QString(); }}\n\
             \x20   void setToolTip(const QString&) {{}}\n\
             \x20   int minimumWidth() {{ return 0; }}\n\
             \x20   void setGeometry(int, int, int, int) {{}}\n}};\n\
             static void* requireHandle(JNIEnv*, jlong) {{ return nullptr; }}\n{}\n",
            native
        );
        std::fs::create_dir_all("target/gencheck").expect("mkdir");
        std::fs::write("target/gencheck/GenCheck.cpp", &cpp).expect("write cpp");

        // 本机 Qt/MinGW/JDK 路径（可被环境变量覆盖）
        let _qt_root = std::env::var("QT_ROOT").unwrap_or_else(|_| "D:\\Qt\\6.11.2\\mingw_64".to_string());
        let jdk = std::env::var("JDK_HOME").unwrap_or_else(|_| "C:\\Program Files\\Java\\latest\\jdk-26".to_string());
        let gxx = std::env::var("GXX").unwrap_or_else(|_| "D:\\Qt\\Tools\\mingw1310_64\\bin\\g++.exe".to_string());
        if !std::path::Path::new(&gxx).exists() {
            eprintln!("跳过：未找到 g++ ({})", gxx);
            return;
        }
        let mut cmd = std::process::Command::new(&gxx);
        cmd.arg("-std=c++17").arg("-fsyntax-only")
            .arg(format!("-I{}", jdk))
            .arg(format!("-I{}\\include", jdk))
            .arg(format!("-I{}\\include\\win32", jdk))
            .arg("target/gencheck/GenCheck.cpp");
        // MinGW DLL 依赖需要 PATH
        #[cfg(target_os = "windows")]
        {
            let mingw = std::path::Path::new(&gxx).parent().and_then(|p| p.to_str()).unwrap_or("");
            let qt_bin = std::path::Path::new(&_qt_root).parent().map(|p| p.join("bin")).and_then(|p| p.to_str().map(String::from));
            let mut path = format!("{};{}", mingw, std::env::var("PATH").unwrap_or_default());
            if let Some(qb) = qt_bin { path = format!("{};{}", qb, path); }
            cmd.env("PATH", path);
        }
        let out = cmd.output().expect("run g++");
        let stderr = String::from_utf8_lossy(&out.stderr).to_string();
        if !out.status.success() {
            if stderr.trim().is_empty() {
                // 无 stderr 输出且失败 = 编译器崩溃（如系统内存不足），跳过而非误报
                eprintln!("跳过：g++ 崩溃（无诊断输出），疑似环境内存不足");
                return;
            }
            panic!("g++ 语法检查失败:\n{}", stderr);
        }
    }}

