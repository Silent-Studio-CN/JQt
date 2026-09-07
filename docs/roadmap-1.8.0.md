# JQt v1.8.0-Emerge-Kit 路线策划(修订稿 · 2026-09-03)

> 状态:修订稿待审阅。版本语义:0.x = 测试线;**1.x.x = 第一正式版线**。
> **1.8.0 名称由来(2026-09-03 确认)**:0.7.5 发布之后推进 JQt-for-Android(0.8 功能序列),
> 故 0.8.0 以第一正式版线身份晋升正式名字 **1.8.0**(代号 Emerge-Kit,寓意展露头角)。
> 正文沿革:0.8.0 草案全稿(2026-09 起草)→ 曾于 M1 提交误伤为 13 行 stub → **2026-09-03 自 git 历史恢复并改写**,
> 基线/条目/里程碑已刷新至当日实况(恢复记录见本地记忆库 README/2026-09-03-恢复0.8工作计划.md)。
> 输入:0.7.5-Generator-Kit 发布复盘 · Android PoC M2 全链路验证 · 五轮代码研读(2026-09-03)· 发布物安全审查 · 文档全量更新

---

## 0. 现状基线(2026-09-03 实测)

| 维度 | 状态 |
|------|------|
| 发布 | 0.7.5-Generator-Kit 三渠道已核实:Central 0.7.5(纯数字)/ JitPack 0.7.5-Generator-Kit / GitHub 15 releases;jar Java 17 字节码 |
| Android | 4 ABI APK(minSdk 28,~109MB);模拟器 x86_64 原生 + ARM 翻译层 + MuMu15 通过;**M2 Java→JNI→Qt→signal→Java 全链路完成**(2026-09-03,logcat 点击可证);遗留:QMainWindow 在 QPA android 渲染黑屏待查;无 CI job;Activity 仍 PoC 形态 |
| API 覆盖 | 源 145 类(含 13 Smoke);L1 常用 92.7%;完整度矩阵(2026-08-29)已失真需重审计(QColor 实 106 public 方法 ≥ Qt 102,矩阵记 3;QFont 同步完成);QtWidgets 落地类数待随矩阵刷新 |
| 工程 | jqt-gen + jqt-build-runner(Rust)双工具链;**M1 工程底座已完成**(2026-09-03:security-notes / release-check.ps1 / release-process.md);发布脚本固化 Java17 + 纯净 jar |
| 代码质量(研读 2026-09-03) | 零 native 崩溃机制实证(注册表自愈/requireHandle 抛异常/GUI 线程排队删除);**已发现待修项**:① 信号回调 JNI 全局引用无释放点(交互控件 Java wrapper 被钉住,动态 UI 内存只增不减)② QMainWindow onIconSizeChanged/onToolButtonStyleChanged 为 no-op 死 API ③ 生成批参数 arg0/中文 javadoc 缺失(根因 jqt-gen 模板)④ AWT-free 变体 7 文件靠手工同步(漂移风险) |
| 安全治理 | 泄密历史已重写清除;.signing 隔离;security-notes 已文档化;Central groupId 为个人 namespace(待决策) |
| 项目状态 | 3 stars;官网 jqt.silentstudio.cn + docs 站;main 无分支保护(建议启用);issue 1/2 为工程笔记 |

---

## 1. 1.8.0 目标定义

一句话:**让 JQt 成为「下载即用的跨平台桌面绑定」——Android 正式成为第四平台,发布与 CI 全自动,文档与代码零断链,API 在人性化与覆盖之间守住质量基线。**

三条验收断言:
1. `gradle 依赖 jqt:1.8.0` + 下载 zip → 三平台 Hello 在 CI 上自动编译运行通过
2. Android APK(4 ABI)由 CI 自动产出并跑通模拟器冒烟(按钮点击;logcat 断言 clicked)
3. 全仓库文档引用零断链;release 资产三渠道一致且通过自动校验;完整度矩阵/API 清单与源码同步(自动重生成)

---

## 2. 方向与条目

### A. 产品(Android 转正 + API 覆盖 + 人性化)

**A1 Android 转正(P0)**
- A1.1 ✅ Java API 调用链接入(已完成,M2):runOnQtThread/isQtReady + Activity 轮询就绪后 Qt 线程构建 UI
- A1.2 平台适配:触摸/点击已通;Activity 生命周期(onPause→窗口暂停、旋转→尺寸同步)、安全区 inset 透传;**QMainWindow QPA android 黑屏排查**
- A1.3 真机验证矩阵:arm64 真机(机型见拍板项 3)侧载 + 截图证据;armv7 老机(可选)
- A1.4 产物正式化:Android 目录整理为可发布形态(template AWT-free 变体机制文档化);APK 由 CI 产出随版发布

**A2 API 覆盖冲刺(P1)**
- A2.0 **完整度矩阵重审计(前置)**:jqt-gen plan 重跑 → 刷新 api-completeness.md(QColor/QFont 实况写回)与 qt6-classes 表;生成脚本接线(发版检查项含矩阵重出,防静默过期)
- A2.1 按 qt6-classes 表推进:目标落地类数见拍板项 1;优先「手写」标记类(QAbstractItemView 系、model-view)
- A2.2 每批交付标准:javac 断言 + Smoke 冒烟(生成器既有闭环)
- A2.3 api-implemented.md 状态随批次自动导出(B3)

**A3 API 人性化精修(候选,2026-09-03 研读提出;是否并入 1.8.0 首批见拍板项 1)**
- A3.1 生成模板升级:参数名抓取(main 页签名表)+ 中文描述 → 消灭 arg0 与空泛 javadoc(根因已定位 generate.rs)
- A3.2 直传方法分级落位:L1 才进主类;L2 走分组对象(window()/style()/drag()/focus(),设计已有未落地);L3 进 .native() 入口——恢复"IDE 第一屏纯净"
- A3.3 死 API/卫生清理:QMainWindow toolbar 信号 no-op(删或 @deprecated)、QPushButton.menu()→hasMenu、重复 import、版本注释残影统一
- A3.4 值对象批次继续纯 Java 化(QPoint/QRect/QSize/QDir/QFile/QSettings/QPainter 族):先例 QColor 106 方法零 native;顺带评估消灭 AWT-free 双写(值对象全去 AWT,toAwt 移工具类)

### B. 工程完备度(P1)

**B1 CI Android job**:Linux runner + Qt 4 ABI kit → build-android.ps1 移植为 CI 版 → 出 APK 上传 release;模拟器冒烟:x86_64 镜像 + adb install + am start + input tap + logcat 断言 clicked

**B2 发布流水线与资产校验** ✅ 已固化(release-check.ps1:jar 无 Smoke/Demo 类、class major=61、zip 无 .bak、VERSION 与 tag 一致)+ release-process.md 文档;补:三渠道坐标同步断言入 release-check

**B3 文档自动重生成**:api-implemented.md 由 jqt-gen 随版本导出;完整度矩阵同源(A2.0)——消除"矩阵失真/标题滞后"类问题

**B4 内存契约兑现(候选,研读 03 提出)**:回调 JNI 全局引用生命周期修复(handle→gRef 表 + destroyed/dispose 释放,或 jweak 化);javadoc 内存模型补注(交互控件 dispose/靠 Qt 父级);**压力 Smoke**(创建/释放数万控件后断言堆稳定)纳入冒烟链

### C. 治理与安全收尾(P1-P2)

- C1 ✅ security-notes.md 已文档化(2026-09-03,M1)
- C2 Central groupId 决策(**需用户拍板**):选项 1 维持 io.github.silent-xiaomiao(零迁移)/ 选项 2 注册 io.github.silent-studio-cn 重新发布(旧坐标 deprecated)
- C3 GitHub 孤儿对象清除:向 GitHub Support 申请清除 force-push 前历史
- C4 main 分支保护(新增建议):CI 四平台全绿后启用 required status checks(治理,需用户拍板)

### D. 冷启动(克制路线,P2)

- D1 README 加真实运行截图(JQtGallery 主题效果 2-3 张 + Android 模拟器 1 张)
- D2 官网 jqt.silentstudio.cn 与仓库对齐(版本徽章、releases 链接、docs 站同步 api-implemented)
- D3 Experience-tells-us 系列继续(真实技术内容优先;不做刷星/冷邮件/SEO 推广)
- D4 社区入口:CONTRIBUTING 已存在——补「报告问题模板」与「新类贡献指引」(对齐 jqt-gen 流程)

---

## 3. 里程碑(修订稿重排;✅=2026-09-03 已完成)

| 里程碑 | 内容 | 依赖 |
|--------|------|------|
| M1 工程底座 ✅ | security-notes + release-check.ps1 + release-process.md + 断链清零 | 无 |
| M2 度量与人性化基线 | A2.0 矩阵重审计 + A3 首批(模板精修/死 API 清理/值对象批次)+ B3 接线 | M1 |
| M3 Android 正式(P0) | A1.2 生命周期/安全区/黑屏 + A1.3 真机 + A1.4 产物正式化 | M1 |
| M4 CI 自动化 | B1 Android CI job + 模拟器冒烟 + B4 压力 Smoke | M3(脚本可与 M2 并行) |
| M5 1.8.0 发布 | A2 覆盖批次 + D1 截图 + B2 三渠道断言 + D2 官网对齐 | M2/M3/M4 |
| M6 治理收尾 | C2 groupId 决策执行 + C3 Support 申请 + C4 分支保护 | 用户拍板 |

---

## 4. 明确不做(本周期)

- Qt Quick/QML 绑定层(qt6-classes 既定范围外,QtJambi 生态占位)
- 刷星、付费推广、SEO 外包(冷邮件已验证为无效信号)
- Windows ARM64/32 位桌面(维持 x64;CI 的 windows-arm64 产物继续保留)
- 移动端 iOS(Qt 官方无 iOS Widgets 支持路径)

---

## 5. 待用户拍板项

1. A2 覆盖目标幅度(80 类?保守 65?)与 **A3 人性化精修是否并入 1.8.0 首批**(研读建议:并入;A3.1/A3.3 成本低收益直接)
2. C2 Central groupId:选项 1 还是 2
3. M3 真机验证机型(用户侧可提供哪台)
4. C4 main 分支保护是否启用(建议 CI 全绿后启用 required checks)
5. A2.0 矩阵重审计是否立即执行(纯工具重跑 + 文档更新,建议先做)

---

## 修订记录

- 2026-09-03:自 git 历史(8165308 引入稿)恢复被 e757820 stub 化的正文;更名 1.8.0-Emerge-Kit;基线刷新(A1.1/M1/C1 完成态、矩阵失真注记);新增 A3/B4/C4 与 M2 里程碑(源自当日五轮代码研读);恢复稿与证据链存于本地记忆库 README/(不提交仓库)。
