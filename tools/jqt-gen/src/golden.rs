//! 金标准 diff：生成输出 vs 手写样板 → 风格收敛
//!
//! 目标：生成的代码与手写金标准"去格式后语义一致"，格式差异作为模板改进项。

use crate::model::QtClass;

/// 规范化：去掉空白/注释差异，提取语义骨架
pub fn normalize_code(code: &str) -> String {
    let mut out = String::new();
    let mut in_comment = false;
    let mut chars = code.chars().peekable();
    while let Some(c) = chars.next() {
        if in_comment {
            if c == '*' && chars.peek() == Some(&'/') {
                chars.next();
                in_comment = false;
            }
            continue;
        }
        if c == '/' && chars.peek() == Some(&'*') {
            chars.next();
            in_comment = true;
            continue;
        }
        if c == '/' && chars.peek() == Some(&'/') {
            while let Some(&n) = chars.peek() {
                if n == '\n' { break; }
                chars.next();
            }
            continue;
        }
        if !c.is_whitespace() {
            out.push(c);
        }
    }
    out
}

/// 从手写 Java 类提取指定方法的源码块
pub fn extract_method(java_source: &str, method_name: &str) -> Option<String> {
    let mut search_from = 0usize;
    while let Some(rel) = java_source[search_from..].find(&format!("{}(", method_name)) {
        let start = search_from + rel;
        let line_start = java_source[..start].rfind('\n').map(|i| i + 1).unwrap_or(0);
        let body = java_source[start..].find('{')? + start;
        let close = java_source[body..].find('}')? + body;
        let block = &java_source[line_start..close + 1];
        if block.contains('(') && block.contains('{') && !block.contains("class ") {
            return Some(block.to_string());
        }
        search_from = start + 1;
    }
    None
}

/// 对比：生成方法 vs 金标准方法（规范化后比较）
pub fn compare_method(generated: &str, golden: &str) -> bool {
    normalize_code(generated) == normalize_code(golden)
}

/// 对类的所有可生成方法做金标准对比，返回差异清单
pub fn audit_class(cls: &QtClass, golden_java: &str) -> Vec<String> {
    let mut diffs = Vec::new();
    for m in &cls.methods {
        if let Some(golden_block) = extract_method(golden_java, &m.name) {
            let generated_block = crate::generate::gen_java_methods_for(cls, &m.name);
            if !generated_block.is_empty() && !compare_method(&generated_block, &golden_block) {
                diffs.push(format!("{}: 语义/格式差异", m.name));
            }
        }
    }
    diffs
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn normalize_strips_comments_and_space() {
        let a = "public void setVisible(boolean visible) { nativeSetVisible(handle, visible); }";
        let b = "  public  void setVisible (boolean visible) {\n        nativeSetVisible(handle, visible); // 注释\n    }";
        assert_eq!(normalize_code(a), normalize_code(b));
    }

    #[test]
    fn extract_method_finds_block() {
        let src = "public class X {\n    public void setVisible(boolean v) { nativeSetVisible(handle, v); }\n}";
        let block = extract_method(src, "setVisible").expect("found");
        assert!(block.contains("setVisible"));
        assert!(block.contains("nativeSetVisible"));
    }

    #[test]
    fn generated_matches_golden_semantics() {
        let golden = "public void setEnabled(boolean enabled) { nativeSetEnabled(nativeHandle, enabled); }";
        let generated = "public void setEnabled(boolean enabled) {\n        nativeSetEnabled(nativeHandle, enabled);\n    }";
        assert!(compare_method(generated, golden));
    }
}
