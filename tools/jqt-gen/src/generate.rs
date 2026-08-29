//! 模板层：直传型方法 → Java 方法块 + native 函数块

use crate::model::{QtClass, QtMethod, QtParam};

fn java_type(ty: &str) -> Option<&'static str> {
    match ty.trim().trim_start_matches("const ") {
        "int" | "qint32" | "uint" | "quint32" => Some("int"),
        "bool" => Some("boolean"),
        "double" | "qreal" => Some("double"),
        "float" => Some("double"),
        "qint64" | "quint64" | "qlonglong" => Some("long"),
        "void" => Some("void"),
        "QString" => Some("String"),
        "char" => Some("char"),
        _ => None,
    }
}

fn jni_type(ty: &str) -> Option<&'static str> {
    match ty.trim().trim_start_matches("const ") {
        "int" | "qint32" | "uint" | "quint32" => Some("jint"),
        "bool" => Some("jboolean"),
        "double" | "qreal" => Some("jdouble"),
        "float" => Some("jfloat"),
        "qint64" | "quint64" | "qlonglong" => Some("jlong"),
        "void" => Some("void"),
        "QString" => Some("jstring"),
        "char" => Some("jchar"),
        _ => None,
    }
}

fn all_direct(params: &[QtParam]) -> bool {
    params.iter().all(|p| java_type(&p.ty).is_some())
}

fn is_generatable(cls: &QtClass, m: &QtMethod) -> bool {
    if m.name == cls.name { return false; }
    if m.name.ends_with("Event")
        || m.name == "operator"
        || m.name.starts_with('~')
        || m.params.iter().any(|p| p.ty.contains("..."))
    { return false; }
    let rt = if m.return_type.is_empty() { "void" } else { m.return_type.as_str() };
    all_direct(&m.params) && java_type(rt).is_some()
}

fn camel(name: &str) -> String {
    let mut chars = name.chars();
    match chars.next() {
        Some(c) => c.to_uppercase().collect::<String>() + chars.as_str(),
        None => String::new(),
    }
}

pub fn gen_java_methods(cls: &QtClass) -> String {
    let mut out = String::new();
    for m in &cls.methods {
        if !is_generatable(cls, m) { continue; }
        let rt = java_type(if m.return_type.is_empty() { "void" } else { &m.return_type }).unwrap();
        let cap = camel(&m.name);
        let mut params = Vec::new();
        let mut args = Vec::new();
        for (i, p) in m.params.iter().enumerate() {
            let jt = java_type(&p.ty).unwrap();
            let nm = if p.name.is_empty() { format!("arg{}", i) } else { p.name.clone() };
            params.push(format!("{} {}", jt, nm));
            args.push(nm);
        }
        let native_call = format!("native{}(nativeHandle{})", cap,
            args.iter().map(|a| format!(", {}", a)).collect::<String>());
        let body = if rt == "void" {
            format!("        {};", native_call)
        } else {
            format!("        return {};", native_call)
        };
        let mut nparams = String::from("long nativeHandle");
        for (i, p) in m.params.iter().enumerate() {
            let jt = jni_type(&p.ty).unwrap();
            let nm = if p.name.is_empty() { format!("arg{}", i) } else { p.name.clone() };
            nparams.push_str(&format!(", {} {}", jt, nm));
        }
        let nret = jni_type(&m.return_type).unwrap_or("void");

        out.push_str(&format!(
            "\n    /** {}（Qt {}）。 */\n    public {} {}({}) {{\n{}\n    }}\n    private static native {} native{}({});\n",
            m.name, m.name, rt, m.name, params.join(", "), body, nret, cap, nparams
        ));
    }
    out
}

pub fn gen_native_functions(cls: &QtClass, cpp_class: &str) -> String {
    let mut out = String::new();
    for m in &cls.methods {
        if !is_generatable(cls, m) { continue; }
        let cap = camel(&m.name);
        let nret = jni_type(&m.return_type).unwrap_or("void");
        let mut jparams = String::from("JNIEnv* env, jobject /*thiz*/, jlong handle");
        let mut call_args = String::new();
        for (i, p) in m.params.iter().enumerate() {
            let jt = jni_type(&p.ty).unwrap();
            let nm = if p.name.is_empty() { format!("arg{}", i) } else { p.name.clone() };
            jparams.push_str(&format!(", {} {}", jt, nm));
            if p.ty.contains("QString") {
                call_args.push_str(&format!(", QString::fromUtf8({})", nm));
            } else {
                call_args.push_str(&format!(", {}", nm));
            }
        }
        let is_void = m.return_type.trim().is_empty() || m.return_type.trim().trim_start_matches("const ") == "void";
        let call_body = if m.return_type.contains("QString") && !is_void {
            format!("        QString __jqt_ret = w->{}({});\n        return env->NewStringUTF(__jqt_ret.toUtf8().constData());",
                m.name, call_args.trim_start_matches(", "))
        } else if is_void {
            format!("        w->{}({});", m.name, call_args.trim_start_matches(", "))
        } else {
            format!("        return w->{}({});", m.name, call_args.trim_start_matches(", "))
        };
        out.push_str(&format!(
            "\nJNIEXPORT {} JNICALL Java_org_jqt_{}_native{}({}) {{\n    {}* w = static_cast<{}*>(requireHandle(env, handle));\n    if (w == nullptr) {{ {} }}\n{}\n}}\n",
            nret, cls.name, cap, jparams, cpp_class, cpp_class,
            if is_void { "return;" } else { "return 0;" },
            call_body
        ));
    }
    out
}


/// 生成单个方法的 Java 骨架（金标准审计用）
pub fn gen_java_methods_for(cls: &QtClass, method_name: &str) -> String {
    let filtered = QtClass {
        name: cls.name.clone(),
        methods: cls.methods.iter().filter(|m| m.name == method_name).cloned().collect(),
        properties: Vec::new(),
        enums: Vec::new(),
    };
    gen_java_methods(&filtered)
}
#[cfg(test)]
mod tests {
    use super::*;
    use crate::model::QtMethod;

    fn m(name: &str, params: Vec<(&str, &str)>, ret: &str) -> QtMethod {
        QtMethod {
            name: name.to_string(),
            return_type: ret.to_string(),
            params: params.into_iter().map(|(n, t)| QtParam { name: n.to_string(), ty: t.to_string() }).collect(),
            is_static: false, is_signal: false, is_ctor: false,
        }
    }

    #[test]
    fn direct_int_params() {
        assert!(java_type("int").is_some());
        assert_eq!(jni_type("int").unwrap(), "jint");
    }

    #[test]
    fn string_params() {
        assert_eq!(java_type("const QString").unwrap(), "String");
        assert_eq!(jni_type("QString").unwrap(), "jstring");
    }

    #[test]
    fn non_direct_rejected() {
        let m = m("setIcon", vec![("icon", "const QIcon &")], "void");
        let cls = QtClass::new("QWidget");
        assert!(!is_generatable(&cls, &m));
    }

    #[test]
    fn direct_accepted() {
        let m = m("setVisible", vec![("visible", "bool")], "void");
        let cls = QtClass::new("QWidget");
        assert!(is_generatable(&cls, &m));
    }

    #[test]
    fn gen_simple_java() {
        let mut cls = QtClass::new("QWidget");
        cls.methods.push(m("setVisible", vec![("visible", "bool")], "void"));
        let java = gen_java_methods(&cls);
        assert!(java.contains("public void setVisible(boolean visible)"));
        assert!(java.contains("nativeSetVisible(nativeHandle, visible)"));
        assert!(java.contains("private static native void nativeSetVisible(long nativeHandle, jboolean visible);"));
    }

    #[test]
    fn gen_simple_native() {
        let mut cls = QtClass::new("QWidget");
        cls.methods.push(m("setVisible", vec![("visible", "bool")], "void"));
        let native = gen_native_functions(&cls, "QWidget");
        assert!(native.contains("Java_org_jqt_QWidget_nativeSetVisible"));
        assert!(native.contains("w->setVisible(visible)"));
    }
}
