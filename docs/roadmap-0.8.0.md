# JQt v0.8.0 路线策划（草案 · 2026-09）

> 状态：草案待审阅。代号沿用 **-Industrial-Kit**（0.7.5 收尾记录既定：0.8.0 沿用）。
> 输入：0.7.5-Generator-Kit 发布复盘 · Android PoC 全链路验证 · GitHub 发布物安全审查与修复 · 文档全量更新

---

## 0. 现状基线

| 维度 | 状态 |
|------|------|
| 发布 | 0.7.5-Generator-Kit 三渠道（Central 0.7.5 / JitPack / GitHub 15 releases）；jar Java 17 字节码；资产已净化（无 .bak/测试类） |
| Android | 4 ABI APK（minSdk 28）+ 模拟器原生/翻译层验证通过；Java API 调用链未接入；无 CI job |
| API 覆盖 | QtWidgets 191 类：55 落地（42 实现 + 12 手写 + 1 骨架）/ 140 未规划；L1 常用 92.7% |
| 工程 | jqt-gen 生成器 + jqt-build-runner（Rust）双工具链；release 脚本已固化为 Java17 + 纯净 jar |
| 安全治理 | 泄密历史已重写清除；.signing 隔离；冷邮件/社工防护未文档化；Central groupId 为个人 namespace |
| 项目状态 | 3 stars / 0 fork；官网 jqt.silentstudio.cn 已上线 |

---

## 1. 0.8.0 目标定义

一句话：**让 JQt 成为「下载即用的跨平台桌面绑定」——Android 正式成为第四平台，发布与 CI 全自动，文档与代码零断链。**

三个验收断言：
1. `gradle 依赖 jqt:0.8.0` + 下载 zip → 三平台 Hello 在 CI 上自动编译运行通过
2. Android APK（4 ABI）由 CI 自动产出并跑通模拟器冒烟（按钮点击）
3. 全仓库文档引用零断链；release 资产三渠道一致且通过自动校验

---

## 2. 方向与条目

### A. 产品（Android 转正 + API 覆盖）

**A1 Android 转正（P0）**
- A1.1 Java API 调用链接入：JQtPocActivity 延迟创建 QApplication（等 main() attach 完成后再走 Java 侧构造）；桥侧补 runOnQtThread 工具（把 Java Runnable 投递到 Qt 线程执行），解决 UI 线程跨线程对象操作
- A1.2 触摸/生命周期/安全区适配：点击/触摸信号（onClicked 已通）、Activity 生命周期（onPause→Qt 窗口暂停、旋转→尺寸同步）、刘海屏安全区 inset 透传
- A1.3 真机验证矩阵：arm64 真机（小米/三星任一）侧载 + 截图证据；32 位 armv7 老机（可选）
- A1.4 产物正式化：Android 目录整理为可发布形态（模板 java 变体说明文档化）；随 0.8.0 发布 jqtpoc-debug.apk + 文档

**A2 API 覆盖冲刺（P1）**
- A2.1 按 qt6-classes 表推进：目标 55 → 80（QtWidgets 191 类表）；优先「手写」标记类（QAbstractItemView 系、model-view）
- A2.2 每批交付标准：javac 断言 + Smoke 冒烟（生成器既有闭环）
- A2.3 api-implemented.md / qt6-classes.md 状态随批次更新（自动）

### B. 工程完备度（P1）

**B1 CI Android job**
- Linux runner + aqt 装 4 ABI kit → build-android.ps1 移植为 bash/CI 版（或 pwsh cross）→ 出 APK 上传 release
- 模拟器冒烟：x86_64 镜像 + adb install + am start + input tap + logcat 断言 clicked

**B2 发布流水线与资产校验（把本次人工踩坑固化）**
- build-release.ps1 已含：javac --release 17 / jar 排除测试类 / .bak 排除——补 release-check.ps1：自动断言（jar 无 Smoke/Demo 类、class major=61、zip 无 .bak、VERSION 与 tag 一致、三渠道坐标同步）
- 发布 checklist 文档化（docs/release-process.md）

**B3 文档自动重生成**：api-implemented.md 由 jqt-gen 随版本导出（消除 v0.7.4 标题滞后类问题）

### C. 治理与安全收尾（P1-P2）

**C1 社工/冷邮件识别页（P1）**：docs/security-notes.md——把今天的 Mercystar 案例 + 识别清单（公开信息引用/开放钩子/身份不可验证）写成一页参考；附发布凭据隔离原则（.signing 规则）

**C2 Central groupId 决策（P2，需用户拍板）**：
- 选项 1：维持 io.github.silent-xiaomiao（现状，零迁移成本）
- 选项 2：注册 io.github.silent-studio-cn 新 groupId 重新发布（Central 不可改名；旧坐标保留但标记 deprecated）
- 附带：POM developer 字段与 README Contributors 对齐

**C3 GitHub 孤儿对象清除（P2）**：向 GitHub Support 申请清除 force-push 前历史（说明仓库 URL + 时间点）

### D. 冷启动（克制路线，P2）

- D1 README 加真实运行截图（JQtGallery 主题效果 2-3 张 + Android 模拟器截图 1 张）
- D2 官网 jqt.silentstudio.cn 与仓库对齐（版本徽章、releases 链接、docs 站开放后同步 api-implemented）
- D3 Experience-tells-us 系列继续（真实技术内容优先；不做刷星/冷邮件/SEO 推广）
- D4 社区入口：CONTRIBUTING 已存在——补「报告问题模板」与「新类贡献指引」（对齐 jqt-gen 流程）

---

## 3. 里程碑（建议顺序）

| 里程碑 | 内容 | 依赖 |
|--------|------|------|
| M1 工程底座 | B2 校验脚本 + B3 文档重生成 + C1 安全页 + 断链清零 | 无 |
| M2 Android 正式 | A1.1-A1.3 Java 链路 + 真机验证 | M1 |
| M3 CI 自动化 | B1 Android CI job + 模拟器冒烟 | M2（脚本可并行） |
| M4 0.8.0 发布 | A2 部分覆盖批次 + A1.4 产物 + D1 截图 | M2/M3 |
| M5 治理收尾 | C2 groupId 决策执行 + C3 Support 申请 | 用户拍板 |

---

## 4. 明确不做（本周期）

- Qt Quick/QML 绑定层（qt6-classes 既定范围外，QtJambi 生态占位）
- 刷星、付费推广、SEO 外包（冷邮件已验证为无效信号）
- Windows ARM64/32 位桌面（维持 x64）
- 移动端 iOS（Qt 官方无 iOS Widgets 支持路径）

---

## 5. 待用户拍板项

1. 0.8.0 代号确认（-Industrial-Kit）与发布渠道策略（是否同 0.7.5：Central 数字版 + GitHub/JitPack 代号版）
2. A2 覆盖目标幅度（80 类？还是保守 65？）
3. C2 Central groupId 选项 1 还是 2
4. M2 真机验证的机型（用户侧可提供哪台）
