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

fn cmd_plan(_args: &[String]) {
    println!("plan 尚未实现（下一步：对比 JQt 现有实现输出缺口）");
}

fn fetch_url(url: &str) -> Result<String, String> {
    let resp = ureq::get(url)
        .timeout(std::time::Duration::from_secs(30))
        .call()
        .map_err(|e| e.to_string())?;
    resp.into_string().map_err(|e| e.to_string())
}
