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
JQtApplication app = new JQtApplication();

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
JQtApplication app = new JQtApplication();
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