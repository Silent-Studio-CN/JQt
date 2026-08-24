# Contributing to JQt（贡献指南）

感谢你愿意为 JQt 贡献力量！

## ⚠️ 重要：贡献者协议（CLA）

JQt 采用**分层授权**（JSL-1.0，见 `LICENSE`）+ **商业许可**双轨模式。
为维持该模式在法律上有效，SilentStudio 必须对全部代码持有完整版权。

因此，**任何贡献（代码、文档、示例、测试）均视为你同意以下条款**：

1. 你保证所贡献的内容为你的原创作品，不侵犯任何第三方权利；
2. 你不可撤销地将贡献的著作权（包括修改与演绎作品）转让给 SilentStudio；
3. SilentStudio 有权以 JSL-1.0 及任何商业许可条款（含第 4 条营收分成
   模式）授权、再许可你的贡献；
4. 本条约定自贡献提交之时生效，不因贡献被合并或拒绝而失效。

**不接受贡献，除非 PR 描述中明确声明：**

> I agree to the JQt Contributor Agreement (CLA) described in CONTRIBUTING.md.

## 提交流程

1. Fork 本仓库，创建功能分支；
2. 遵循现有代码风格：4 空格缩进、UTF-8 编码、Java 17+ 语法；
3. Java 源码与 C++ 胶水层的修改必须保持 JNI 符号一致（方法名 ↔ JNI 导出名）；
4. 提交 PR，并在描述中附 CLA 声明；
5. 维护者审查后合并。

## 开发环境

见 `README.md` 的构建依赖与 `build.ps1`。
