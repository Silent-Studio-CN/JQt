# Contributing to JQt（贡献政策）

<details>
<summary>🌐 语言 / Language</summary>

- [中文版](#zh) · [English Version](#en)

</details>

---

<a id="zh"></a>
## 中文版

### ⛔ 提交政策：仅限 SilentStudio 成员

JQt **不接受外部贡献者的代码提交**——外部 pull request 不会被合并。
只有 **SilentStudio 组织成员**可以直接提交代码。

这一政策是为了保证 JQt 分层授权模式（JSL-1.0，见 LICENSE.md）的
版权完整性：SilentStudio 必须对全部代码持有完整版权，才能维持
开源 + 商业许可双轨运作。

### ✅ 外部人员如何参与

| 需求 | 渠道 |
|------|------|
| 报告 Bug / 建议功能 | GitHub **Issues** |
| 提问 / 讨论 | GitHub Issues 或 Discussion |
| 安全漏洞 | 不要公开提交——私信联系 SilentStudio |

> Email: SilentStudio@Home.email.cn

### 📌 内部成员提交规范

- 遵循现有代码风格：4 空格缩进、UTF-8 编码、Java 17+ 语法；
- 修改 Java 源码与 C++ 胶水层时，必须保持 JNI 符号一致
  （Java 方法名 ↔ JNI 导出名）；
- **注释语言**：源码注释以中文为主；若添加英文注释，
  **两者出现歧义时以中文为准**；
- 提交信息用中文或英文均可，建议附带简短说明。

### 📌 说明

- 本政策随项目发展阶段可能调整；
- 若将来开放外部贡献，贡献者须先签署 CLA
  （贡献版权转让给 SilentStudio）才能被合并。

---

<a id="en"></a>
## English Version

### ⛔ Commit Policy: SilentStudio Members Only

JQt **does not accept code contributions from external contributors** — external pull requests will not be merged. Only **members of the SilentStudio organization** may commit code directly.

This policy ensures the copyright integrity required by JQt's layered licensing model (JSL-1.0, see LICENSE.md): SilentStudio must hold full copyright over all code in order to maintain the dual-track operation of open source + commercial licensing.

### ✅ How External People Can Participate

| Need | Channel |
|------|---------|
| Report bugs / suggest features | GitHub **Issues** |
| Questions / discussion | GitHub Issues or Discussion |
| Security vulnerabilities | Do not post publicly — contact SilentStudio privately |

> Email: SilentStudio@Home.email.cn

### 📌 Internal Member Guidelines

- Follow the existing code style: 4-space indentation, UTF-8 encoding, Java 17+ syntax;
- When modifying the Java sources and the C++ bridge, keep JNI symbols consistent
  (Java method name ↔ JNI exported symbol);
- **Comment language**: source comments are primarily in Chinese; if English comments
  are added, **the Chinese version prevails in case of ambiguity**;
- Commit messages may be in Chinese or English; a brief description is recommended.

### 📌 Notes

- This policy may be adjusted as the project evolves;
- If external contributions are opened in the future, contributors must first sign
  a CLA (assigning contribution copyright to SilentStudio) before their work can be merged.
