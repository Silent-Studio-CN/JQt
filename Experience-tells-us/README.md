# Experience Tells Us — JQt 开发经验与踩坑实录

**🌐 语言 / Language / 言語 / Sprache:**
[简体中文](README.md) · [繁體中文](README.zh-Hant.md) · [English](README.en.md) · [日本語](README.ja.md) · [Deutsch](README.de.md)

> 本目录沉淀 JQt（Java bindings for Qt）开发过程中积累的经验教训。
> 内容聚焦** JQt 开发本身**：Java API 设计、JNI/native 桥接、Qt 行为陷阱、
> Windows 平台特性、主题渲染、打包分发、社区工程约定。
> 由参与 JQt 开发的 AI 工程方向成员撰写，随版本演进持续补充。

## 我的主要负责方向

1. **JQtGallery 社区演示工程**（Community/JQtGallery）
   - 功能分区演示（主题/控件/动画/窗口/v0.5~v0.7.5 各版本新 API）
   - 自动演示模式（-Dg.auto=1 逐个点击验证）与探针测试
   - 跟随每个 release 更新：v0.6 → v0.6.1 → v0.7.0~v0.7.5 全部跟进
2. **JQt native 层 Windows 平台问题排查与修复**
   - setFrameless 热切换失效（Win32 样式位 + DWM 扩展边框）—— 已根治
   - 窗口重建/布局漂移、固定尺寸约束、触摸合成事件坐标
3. **主题渲染系统**
   - fluent.qss.tpl 模板 + 变量表渲染机制
   - 双主题（浅/深）切换、强调色动态变量
4. **打包与分发**
   - jpackage 应用镜像、Qt 运行时部署、插件路径（qt.conf / QT_PLUGIN_PATH）
   - 多位置部署一致性校验
5. **社区协作与发布**
   - 版本归档约定（根目录=最新 + vX.Y/ 子目录）
   - 发布说明三段式格式、测试报告

## 文档索引

| 文件 | 主题 |
|------|------|
| [01-window-native.md](01-window-native.md) | Win32 窗口系统与 native 层（setFrameless 大坑全解） |
| [02-theme-qss.md](02-theme-qss.md) | 主题渲染与 QSS（模板变量/优先级/残留） |
| [03-java-api.md](03-java-api.md) | Java API 设计与使用陷阱 |
| [04-lifecycle-threads.md](04-lifecycle-threads.md) | 对象生命周期、线程、信号回调 |
| [05-packaging.md](05-packaging.md) | 打包分发与运行时部署 |
| [06-community.md](06-community.md) | 社区工程约定与发布流程 |
| [07-probes.md](07-probes.md) | 探针测试方法论（复现 native 问题） |
| [08-setFrameless-case.md](08-setFrameless-case.md) | setFrameless 修复全记录（native 排查范例） |
