//! members HTML 解析：提取方法签名

use crate::model::{QtClass, QtMethod, QtParam};
use std::collections::HashSet;

/// 从 members 页 HTML 提取方法列表
/// members 页签名形如："setWindowIcon(const QIcon &amp;)" 或 "void setIcon(const QIcon &icon)"
pub fn parse_members_html(html: &str, class_name: &str) -> QtClass {
    // 从页面标题提取规范类名（"List of All Members for QWidget | ..."），fallback 到参数
    let canonical = extract_class_name(html).unwrap_or_else(|| class_name.to_string());
    let mut cls = QtClass::new(&canonical);
    let text = strip_tags(html);
    let mut seen: HashSet<String> = HashSet::new();

    let b = text.as_bytes();
    let mut i = 0usize;
    while i < b.len() {
        if b[i] == b'(' {
            // 回找紧贴 ( 的标识符（方法名，可能带 Class:: 前缀）
            let mut j = i;
            while j > 0 && (b[j - 1].is_ascii_alphanumeric() || b[j - 1] == b'_' || b[j - 1] == b':') {
                j -= 1;
            }
            let raw = &text[j..i];
            let name = raw.rsplit("::").next().unwrap_or(raw).trim();
            if !name.is_empty() && name.chars().next().map(|c| c.is_ascii_alphabetic()).unwrap_or(false) {
                if let Some(k) = text[i..].find(')') {
                    let close = i + k;
                    let args = &text[i + 1..close];
                    if let Some(m) = build_method(name, args) {
                        let key = format!("{}({:?})", m.name, m.params);
                        if seen.insert(key) {
                            cls.methods.push(m);
                        }
                    }
                    i = close + 1;
                    continue;
                }
            }
        }
        i += 1;
    }

    cls.methods.sort_by(|a, b| a.name.cmp(&b.name));
    cls
}

/// 由方法名 + 参数字符串构造 QtMethod
fn build_method(name: &str, args: &str) -> Option<QtMethod> {
    if name.is_empty() || name == "Q" || name == "operator" || name.starts_with('~') {
        return None;
    }
    let mut params = Vec::new();
    if !args.trim().is_empty() {
        for part in split_top_level(args) {
            let p = part.trim();
            if p.is_empty() { continue; }
            let words: Vec<String> = p.split_whitespace().map(String::from).collect();
            let mut ty = String::new();
            let mut pname = String::new();
            if words.len() > 1 {
                let lastw = words.last().unwrap().clone();
                let stripped = lastw.trim_start_matches(['&', '*']);
                let looks_name = !stripped.is_empty()
                    && (stripped.chars().next().unwrap().is_ascii_alphabetic() || stripped == "...");
                if looks_name {
                    pname = stripped.to_string();
                    let ty_words: Vec<String> = words[..words.len() - 1]
                        .iter()
                        .map(|w| w.trim_matches(['&', '*']).to_string())
                        .filter(|w| !w.is_empty())
                        .collect();
                    ty = ty_words.join(" ");
                    if ty.is_empty() { ty = stripped.to_string(); }
                } else {
                    ty = words.join(" ").replace('&', "").replace('*', "").trim().to_string();
                }
            } else {
                ty = words.join(" ").replace('&', "").replace('*', "").trim().to_string();
            }
            params.push(QtParam { name: pname, ty });
        }
    }
    Some(QtMethod {
        name: name.to_string(),
        return_type: String::new(),
        params,
        is_static: false,
        is_signal: false,
        is_ctor: false,
    })
}


/// 从主页面 HTML 提取 方法名 → 返回类型 映射
/// 主页面签名形如："virtual void setVisible (bool visible)" → { setVisible: "void" }
pub fn parse_main_return_types(html: &str) -> std::collections::HashMap<String, String> {
    let mut out = std::collections::HashMap::new();
    let text = strip_tags(html);
    let b = text.as_bytes();
    let mut i = 0usize;
    while i < b.len() {
        if b[i] == b'(' {
            let mut j = i;
            while j > 0 && (b[j - 1].is_ascii_alphanumeric() || b[j - 1] == b'_' || b[j - 1] == b':') {
                j -= 1;
            }
            let raw = &text[j..i];
            let name = raw.rsplit("::").next().unwrap_or(raw).trim();
            if !name.is_empty() && name.chars().next().map(|c| c.is_ascii_alphabetic()).unwrap_or(false) {
                let mut k = j;
                while k > 0 && (b[k - 1] as char).is_whitespace() { k -= 1; }
                let mut end = k;
                while end > 0 && (b[end - 1].is_ascii_alphanumeric() || b[end - 1] == b'_' || b[end - 1] == b'*' || b[end - 1] == b'&') {
                    end -= 1;
                }
                let token = text[end..k].trim();
                let is_modifier = matches!(token, "virtual" | "static" | "const" | "inline" | "override" | "Q_DECL_OVERRIDE" | "signal" | "slot");
                if !is_modifier && !token.is_empty() && token != name {
                    let mut rt = token.to_string();
                    let mut k2 = end;
                    while k2 > 0 && (b[k2 - 1] as char).is_whitespace() { k2 -= 1; }
                    let mut e2 = k2;
                    while e2 > 0 && (b[e2 - 1].is_ascii_alphanumeric() || b[e2 - 1] == b'_') { e2 -= 1; }
                    let prev = text[e2..k2].trim();
                    if matches!(prev, "virtual" | "static" | "inline" | "signal" | "slot") {
                        rt = format!("{} {}", prev, rt);
                    }
                    out.insert(name.to_string(), rt);
                }
            }
        }
        i += 1;
    }
    out
}
/// 从页面标题提取规范类名："List of All Members for QWidget | Qt Widgets" → "QWidget"
fn extract_class_name(html: &str) -> Option<String> {
    let marker = "List of All Members for ";
    let i = html.find(marker)? + marker.len();
    let rest = &html[i..];
    let end = rest.find('|').unwrap_or(rest.len());
    let name = rest[..end].trim();
    if !name.is_empty() {
        Some(name.to_string())
    } else {
        None
    }
}

fn strip_tags(html: &str) -> String {
    let mut out = String::with_capacity(html.len());
    let mut in_tag = false;
    for c in html.chars() {
        match c {
            '<' => in_tag = true,
            '>' => in_tag = false,
            _ if !in_tag => out.push(c),
            _ => {}
        }
    }
    out.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&#39;", "'")
}

/// 顶层逗号切分（忽略括号内嵌套）
fn split_top_level(s: &str) -> Vec<String> {
    let mut out = Vec::new();
    let mut depth = 0i32;
    let mut cur = String::new();
    for c in s.chars() {
        match c {
            '(' | '<' => { depth += 1; cur.push(c); }
            ')' | '>' => { depth -= 1; cur.push(c); }
            ',' if depth <= 0 => { out.push(cur.clone()); cur.clear(); }
            _ => cur.push(c),
        }
    }
    if !cur.trim().is_empty() { out.push(cur); }
    out
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parse_basic_signature() {
        let m = build_method("setIcon", "const QIcon &icon").expect("parse");
        assert_eq!(m.name, "setIcon");
        assert_eq!(m.params.len(), 1);
        assert_eq!(m.params[0].ty, "const QIcon");
    }

    #[test]
    fn parse_multi_params() {
        let m = build_method("setGeometry", "int x, int y, int w, int h").expect("parse");
        assert_eq!(m.name, "setGeometry");
        assert_eq!(m.params.len(), 4);
        assert_eq!(m.params[3].ty, "int");
    }

    #[test]
    fn parse_pointer_param() {
        let m = build_method("addAction", "const QIcon &, const QString &").expect("parse");
        assert_eq!(m.name, "addAction");
        assert_eq!(m.params.len(), 2);
    }

    #[test]
    fn strip_tags_works() {
        let html = "<li class=\"fn\"><span class=\"name\">setIcon</span>(const QIcon &amp;icon)</li>";
        let text = strip_tags(html);
        assert!(text.contains("setIcon"));
        assert!(!text.contains('<'));
    }
}
