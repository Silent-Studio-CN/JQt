# JQt 主题与动画系统

<details>
<summary>🌐 语言 / Language</summary>

- [中文版](#zh) · [English Version](#en)

</details>

---

<a id="zh"></a>
## 中文版

### 1. 绘制层级（为什么会有冲突）

Qt 的外观由三层决定，**优先级从高到低**：

```
QSS 样式表（setStyleSheet）  ← 最高：覆盖到的属性优先
调色板（setColorScheme）     ← 中间：QSS 未覆盖的控件/属性使用
风格引擎（setStyle）          ← 最低：默认绘制
```

**冲突场景**（PySide/Qt 生态的经典大坑，JQt 已内置规避方案）：

| 场景 | 问题 | JQt 方案 |
|------|------|---------|
| QSS 全局规则覆盖 palette | `* { color: #fff }` 后 setColorScheme 改色无效 | 用 `setTheme()` 统一打包，不手动混搭 |
| 主题切换 QSS 不跟随 | QSS 硬编码颜色不随深浅色变化 | 内置 fluent-dark/light 两套配套 QSS |
| QSS 部分覆盖 | 部分控件用 QSS、部分用 palette，视觉不统一 | 主题 = QSS + 调色板一致设计 |

### 2. 主题 API（统一入口）

```java
QApplication app = new QApplication();

// 内置主题（QSS + 调色板一致打包）
app.setTheme("fluent-dark");    // Fluent 深色
app.setTheme("fluent-light");   // Fluent 浅色

// 自定义主题：QSS 文件 + 指定配色
app.setTheme("themes/my-theme.qss", true);   // true=浅色调色板

// 底层 API（高级用法，注意层级规则）
app.setStyleSheet("QPushButton { ... }");   // 样式表
app.setColorScheme(true);                    // 浅色调色板
app.setStyle("Fusion");                      // 风格引擎
```

> 第三方 QSS（含 GPL 许可的皮肤）由使用者自行负责其许可——JQt 仅是 QSS 渲染引擎，用户导入的样式数据与 JQt 无关。

### 3. 动画系统

QSS 不支持 CSS transition，动画必须走属性动画 API（QPropertyAnimation）。

```java
window.fadeIn(300);                 // 窗口淡入
window.fadeOut(300);                // 窗口淡出

card.animateMove(100, 100, 400);    // 平滑移动（OutCubic）
card.animateResize(300, 200, 400);  // 平滑缩放
card.fadeIn(200);                   // 控件淡入（透明度效果）
card.fadeOut(200);                  // 控件淡出
```

内置动画全部走 GUI 线程，动画对象自动清理（DeleteWhenStopped），无需手动管理。


### 4. 文字、字体与编码（中文乱码/问号排查）

JQt 的字符串管道全程 UTF-8（JNI 用 GetStringUTFChars + QString::fromUtf8），
Java 源码与编译均 UTF-8。出现 `?`/乱码时按以下顺序排查：

| 症状 | 原因 | 解决 |
|------|------|------|
| 控件内中文显示为 `?` 或方块 | Qt 全局字体不含 CJK 字形或回退失败 | JQt 构造时自动应用系统中文字体（Windows 雅黑 / macOS 苹方 / Linux Noto CJK）；也可手动 `app.setFontFamily("Microsoft YaHei UI", 13)` |
| 控制台 `System.out.println` 中文为 `?` | 终端代码页（GBK）与 Java UTF-8 输出不匹配 | 用 `run.ps1`/`run-fluent.ps1` 启动（已设 UTF-8）；或 `chcp 65001` |
| 输入框无法输入中文 | 无边框窗口的 IME（输入法）候选框未正确挂接 | 确认聚焦后输入法正常弹出；无边框窗口已知限制见 docs/user-guide.md |
| 自己写的 Java 文件中文乱码 | 源文件为 GBK 编码而 javac 按 UTF-8 编译 | 源码统一 UTF-8（无 BOM）；编译加 `-encoding UTF-8` |

**跨平台字体策略**（QApplication 构造时自动）：

```
Windows: Microsoft YaHei UI / macOS: PingFang SC / Linux: Noto Sans CJK SC
```

Qt 找不到指定字体族时会自动回退系统字体，不会产生问号。
QSS 模板中的 `font-family` 建议带 CJK 回退链：`"Segoe UI", "Microsoft YaHei UI"`。

---
<a id="en"></a>
## English Version

### 1. Paint Hierarchy (why conflicts happen)

Qt appearance has three layers, priority high to low:

```
QSS stylesheet (setStyleSheet)  <- highest: properties it covers win
Palette (setColorScheme)         <- middle: used by widgets/properties QSS does not cover
Style engine (setStyle)          <- lowest: default drawing
```

**Conflict scenarios** (classic Qt/PySide pitfalls, JQt has built-in mitigations):

| Scenario | Problem | JQt solution |
|----------|---------|--------------|
| global QSS rules override palette | after `* { color: #fff }`, setColorScheme has no effect | use setTheme() instead of hand-mixing |
| QSS does not follow theme switching | hard-coded QSS colors do not change with light/dark | bundled fluent-dark/light paired QSS |
| partial QSS coverage | some widgets QSS, some palette - inconsistent look | themes package QSS + palette consistently |

### 2. Theme API (single entry)

```java
QApplication app = new QApplication();
app.setTheme("fluent-dark");    // built-in
app.setTheme("fluent-light");
app.setTheme("themes/my.qss", true);  // custom QSS + light palette
```

> Third-party QSS (including GPL skins) is the user's own responsibility - JQt is only a QSS rendering engine.

### 3. Animation API

QSS has no CSS transition; animations use QPropertyAnimation:

```java
window.fadeIn(300);  window.fadeOut(300);
card.animateMove(100, 100, 400);  card.animateResize(300, 200, 400);
card.fadeIn(200);  card.fadeOut(200);
```

All animations run on the GUI thread and self-clean (DeleteWhenStopped).


### 4. Text, Fonts & Encoding (CJK / question-mark troubleshooting)

JQt's string pipeline is UTF-8 end to end (JNI GetStringUTFChars + QString::fromUtf8);
Java sources and compilation are UTF-8. If you see `?` or mojibake:

| Symptom | Cause | Fix |
|---------|-------|-----|
| `?`/boxes for CJK in widgets | Qt global font lacks CJK glyphs or fallback fails | JQt auto-applies a system CJK font at construction (YaHei / PingFang / Noto CJK); or `app.setFontFamily("Microsoft YaHei UI", 13)` |
| `?` in console `System.out.println` | terminal codepage (GBK) vs Java UTF-8 output | use run.ps1/run-fluent.ps1 (UTF-8 preset); or `chcp 65001` |
| cannot type CJK in inputs | frameless-window IME candidate window not attached | check IME pops on focus; frameless IME limits documented in docs/user-guide.md |
| mojibake in your own .java files | source saved as GBK while javac compiles UTF-8 | save sources UTF-8 (no BOM); compile with `-encoding UTF-8` |

**Cross-platform font strategy** (auto at QApplication construction):

```
Windows: Microsoft YaHei UI / macOS: PingFang SC / Linux: Noto Sans CJK SC
```

Qt falls back to a system font when the requested family is missing - never `?`.
Use CJK fallback chains in QSS: `"Segoe UI", "Microsoft YaHei UI"`.

---
