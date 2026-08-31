# 02 · 主题渲染与 QSS

> JQt 的主题体系：官方 setTheme(path, vars) 模板渲染 + 社区自定义双主题。
> 本章记录模板机制、样式优先级、残留问题的全部经验。

## 1. fluent.qss.tpl 模板渲染机制

- 官方模板有 **22 个 %var% 占位符**（win-bg/fg/accent/btn-*/card-*/input-*/nav-*...）。
- 渲染方式：读模板 → `replace("%"+key+"%", value)` → `app.setStyleSheet(qss)`。
- **渲染式（render-based）**：不依赖 setTheme 的文件路径查找，exe 打包版安全。
- 模板读取优先级：文件系统 themes/fluent.qss.tpl 优先，jar 内资源兜底。
- 社区版常用三段拼接：fluent.qss.tpl（基础）+ qraft-styles.qss（立体按钮）+ sck-extra.qss（专属控件）。

## 2. 样式优先级（本目录最容易踩的坑）

优先级从高到低：
1. **控件级 setStyleSheet**（`widget.setStyleSheet(...)`）—— 最高，永远盖住全局
2. **对象名选择器**（`QPushButton#themeBtn`）—— 次高
3. **app 级 / win 级 setStyleSheet** —— 全局

### 坑 1：控件级暗色覆盖钉死主题
```java
// 错误示范：启动时对每个控件单独 setStyleSheet 暗色
applyDarkStyles();   // 每个控件 setStyleSheet 暗色
// 之后无论怎么切全局主题，控件级样式优先级更高 → 永远暗色
```
**修复**：删除控件级批量覆盖，全部交给全局 QSS 模板渲染；
个别控件需要特殊样式时，用 objectName 选择器放进模板。

### 坑 2：内联样式硬编码颜色（黑残留）
```java
topBar.setStyleSheet("QFrame#topBar { background-color: #1f1f1f; }");  // 硬编码
```
内联样式优先级最高，模板渲染无法覆盖 → 切浅色后顶栏还是黑的。
**修复**：移除内联样式，改为模板规则 + 变量（`%topbar-bg%`）。

### 坑 3：QSS 文件里硬编码深色
```css
QTextEdit, QPlainTextEdit { background: #1a1a1a; }   // 日志区永远黑
```
**修复**：变量化 —— `%terminal-bg%` / `%terminal-fg%`，浅色表白底深字、
深色表保持终端黑底。

## 3. 双主题设计（浅色/深色变量表）

- 两个同构变量表：`lightVars()` / `darkVars()`，键集合必须完全一致，
  否则某主题渲染后残留 %var% 导致 QSS 解析失败。
- **设计令牌**统一进变量表：border/border-deep/radius/card-radius/pill-radius/fontsize。
- 强调色（accent）动态变量：切换主题色时用 lighten/darken/withAlpha 生成
  accent-hover/pressed/deep/ghost-hover/ghost-pressed。
- **applyTheme(name) 必须真正使用 name 参数**——历史上出现过硬编码暗色
  QSS 完全忽略参数的版本（切浅色无效的最大元凶）。

## 4. QSS 解析失败的排查

- Qt 报 `Could not parse application stylesheet` 时：
  1. 检查渲染结果是否有**未替换的 %var%**（正则 `%[a-z-]+%` 扫描）；
  2. 检查颜色格式（rgba() 与 #RRGGBB 混用）；
  3. 检查选择器语法（`QLabel#detailPanel QLabel` 这种后代+ID 组合）。
- 模板注释里的字面 %var%（如 "filled by setTheme()" 说明文字）无害，不用管。

## 5. 主题按钮交互设计

- 主题切换按钮图标应显示"点击后的去向"：深色模式显示 ☀（点它回浅色）、
  浅色模式显示 ☾（点它转深色）。
- 切换后同步更新按钮文字，否则用户不知道当前状态。

## 6. 验证方法论

- **渲染探针**：直接调 renderThemeQss + 变量表，断言输出包含/不包含关键色值：
  `LIGHT has #f3f3f3: true`、`LIGHT terminal dark: false`。
- **运行日志**：`[SCK] theme=fluent-light qss len=11085`，stderr 无 parse 错误。
- 注意：前景文字色（color:）和背景色（background:）都要检查，
  搜索"残留色值"时先确认是 fg 还是 bg。
