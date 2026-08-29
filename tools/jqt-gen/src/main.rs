//! JQt 绑定生成器 — Qt 元信息 → Java/native 骨架
//!
//! 用法:
//!   jqt-gen fetch <class1,class2,...>  抓取 members 页 → 输出 JSON 清单
//!   jqt-gen plan <class>               对比 JQt 现有实现 → 输出缺口计划

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
        _ => print_usage(),
    }
}

fn print_usage() {
    println!("jqt-gen — JQt 绑定生成器");
    println!("  jqt-gen fetch <classes>     抓取 Qt members 页 → JSON");
    println!("  jqt-gen plan <class>        缺口计划");
}

fn cmd_fetch(args: &[String]) {
    if args.len() < 3 {
        eprintln!("用法: jqt-gen fetch <class1,class2>");
        return;
    }
    let classes: Vec<&str> = args[2].split(',').map(|s| s.trim()).filter(|s| !s.is_empty()).collect();
    let mut out = Vec::new();
    for c in &classes {
        let lower = c.to_lowercase();
        let url = format!("https://doc.qt.io/qt-6/{}-members.html", lower);
        match fetch_url(&url) {
            Ok(html) => {
                let cls = parse::parse_members_html(&html, c);
                println!("{}: {} methods", c, cls.methods.len());
                out.push(cls);
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

    // 扫描 JQt 现有实现
    let jqt_dir = std::env::var("JQT_JAVA_DIR").unwrap_or_else(|_| "D:\\SilentStudio\\JQt - Dev\\java\\org\\jqt".to_string());
    let mut implemented: std::collections::HashMap<String, std::collections::HashSet<String>> = std::collections::HashMap::new();
    if let Ok(entries) = std::fs::read_dir(&jqt_dir) {
        for entry in entries.flatten() {
            let path = entry.path();
            if path.extension().map(|e| e == "java").unwrap_or(false) {
                if let Ok(content) = std::fs::read_to_string(&path) {
                    let class = path.file_stem().map(|s| s.to_string_lossy().to_string()).unwrap_or_default();
                    let mut names = std::collections::HashSet::new();
                    for line in content.lines() {
                        let t = line.trim();
                        // public 方法名提取
                        if t.starts_with("public") && t.contains('(') {
                            if let Some(open) = t.find('(') {
                                let head = &t[..open];
                                let name = head.split_whitespace().next_back().unwrap_or("").trim_end_matches(')').trim();
                                if name.chars().all(|c| c.is_ascii_alphanumeric() || c == '_') && !name.is_empty() {
                                    names.insert(name.to_string());
                                }
                            }
                        }
                    }
                    implemented.insert(class.to_lowercase(), names);
                }
            }
        }
    }

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

fn fetch_url(url: &str) -> Result<String, String> {
    let resp = ureq::get(url)
        .timeout(std::time::Duration::from_secs(30))
        .call()
        .map_err(|e| e.to_string())?;
    resp.into_string().map_err(|e| e.to_string())
}
