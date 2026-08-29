//! Qt 元信息模型（type-safe 表示，非法状态不可构造）

use serde::{Deserialize, Serialize};

/// 方法参数
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct QtParam {
    pub name: String,
    pub ty: String,
}

/// Qt 方法
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct QtMethod {
    /// 方法名（camelCase，如 "setIcon"）
    pub name: String,
    /// 返回类型（如 "void"、"QIcon"、"int"）
    pub return_type: String,
    /// 参数列表
    pub params: Vec<QtParam>,
    /// 是否静态
    pub is_static: bool,
    /// 是否信号
    pub is_signal: bool,
    /// 是否构造函数
    pub is_ctor: bool,
}

/// Qt 属性（getter/setter 对）
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct QtProperty {
    pub name: String,
    pub ty: String,
    pub read: String,
    pub write: Option<String>,
}

/// Qt 枚举
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct QtEnum {
    pub name: String,
    pub values: Vec<(String, i64)>,
}

/// Qt 类
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct QtClass {
    /// 类名（如 "QWidget"）
    pub name: String,
    pub methods: Vec<QtMethod>,
    pub properties: Vec<QtProperty>,
    pub enums: Vec<QtEnum>,
}

impl QtClass {
    pub fn new(name: &str) -> Self {
        Self { name: name.to_string(), methods: Vec::new(), properties: Vec::new(), enums: Vec::new() }
    }

    /// 按方法名查方法（含重载）
    pub fn methods_named(&self, name: &str) -> Vec<&QtMethod> {
        self.methods.iter().filter(|m| m.name == name).collect()
    }
}

/// 生成配置：一个类的绑定方案
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ClassPlan {
    pub class: String,
    /// 手写/生成标记：HandWritten | Generated
    pub mode: String,
    /// 要生成的方法名（白名单；空 = 全部缺口）
    pub include: Vec<String>,
    /// 排除的方法名
    pub exclude: Vec<String>,
}
