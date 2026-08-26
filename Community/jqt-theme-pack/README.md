# JQt Theme Pack

三个原创 QSS 主题（基于 JQt 官方 fluent.qss.tpl 模板 + 变量 Map 机制）。

## 主题

| 主题 | 风格 | 亮/暗 | 主色 |
|------|------|-------|------|
| [NordTheme.java](NordTheme.java) | 北极蓝 · 程序员经典 | 暗 | #2E3440 / #88C0D0 |
| [SolarizedTheme.java](SolarizedTheme.java) | 米黄护眼 · 低对比 | 亮 | #FDF6E3 / #268BD2 |
| [TerminalTheme.java](TerminalTheme.java) | 黑底荧光绿 · 赛博终端 | 暗 | #0A0F0A / #00FF44 |

## 使用

```java
JQtApplication app = new JQtApplication();

// 暗色主题
app.setTheme("themes/fluent.qss.tpl", NordTheme.vars(), false);
app.setTheme("themes/fluent.qss.tpl", TerminalTheme.vars(), false);

// 亮色主题
app.setTheme("themes/fluent.qss.tpl", SolarizedTheme.vars(), true);
```

要求：JQt v0.2.0-alpha 及以上（setTheme 模板变量机制）。

## 实现原理

官方 fluent.qss.tpl 定义 22 个 `%var%` 占位符（win-bg / fg / card-bg / btn-* / accent / input-* / nav-* / switch-*），
本包仅提供不同配色的变量 Map —— 一套模板，无限主题。

## 版权

(C) SilentStudio
All rights reserved.
SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
