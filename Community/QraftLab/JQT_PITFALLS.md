# JQt v0.4.1+ 避坑指南（Pitfalls）

> 基于 Qraft 系列项目实战踩坑总结 · 对照 JQt v0.5.1-TEST
> 铁律：**官方 JQtGallery（Community/JQtGallery）是唯一行为规范，照它写就能跑通**

---

## 一、破坏性变更（v0.4.1 起）

| 旧 API | 新 API | 说明 |
|--------|--------|------|
| JQtApplication | **QApplication** | 有 Qt 对应物的类全部 Q 化 |
| JQtWindow | **QMainWindow** | |
| JQtButton / JQtLabel / JQtPanel | **QPushButton / QLabel / QFrame** | |
| JQtVBoxLayout / JQtHBoxLayout | **QVBoxLayout / QHBoxLayout** | |
| onClick | **onClicked** | 信号对齐 Qt；onXxx 均返回 this（链式） |
| — | JQtSwitch / JQtPivot / JQtTitleBar / JQtEasing | JQt 原创控件保留 JQt 前缀 |

> README 还没同步（仍是 JQtButton 示例），以 CHANGELOG.md 和仓库 docs/qt-mapping.md 为准。

---

## 二、显示/布局铁律（v0.5.1 行为，不遵守就"控件悄悄消失"）

### 1. addWidget 不再自动 show
"布局未安装时 addWidget 不再 show"（防窗口闪现）。
**后果**：控件添加后不显示，且无任何报错。

### 2. 必须遵循的构建顺序（官方 JQtGallery 模式）
```java
// 正确：页面直接进 root，不包中间层
QVBoxLayout root = new QVBoxLayout();
root.addWidget(title);
root.addWidget(pivot);
root.addWidget(panel1);   // 页面 QFrame 直接进 root
root.addWidget(panel2);
w.setLayout(root);        // ← 先 setLayout

// 然后才 hide 非当前页（setLayout 递归显示之后）
panel2.hide();
pivot.onChanged(i -> { panel1.setVisible(i==0); ... });
```

### 3. 布局嵌套用 addLayout，不要包中间 QFrame
```java
// 正确
QVBoxLayout box = new QVBoxLayout();
QHBoxLayout row = new QHBoxLayout();
row.addWidget(btn1); row.addWidget(btn2);
box.addLayout(row);           // 布局直接嵌布局

// 错误：包中间 QFrame 会断掉递归显示链
QFrame rowHost = new QFrame();
rowHost.setLayout(row);
box.addWidget(rowHost);       // 可能不显示
```

### 4. 页面切换用 show/hide，不要用 QStackedLayout
QStackedLayout 在 v0.5.1 页面不显示（官方 CHANGELOG 自述"页跳过防堆叠"，疑似 bug/未明）。
**官方 JQtGallery 自己也不用 QStackedLayout**，就是 pivot.onChanged + show/hide。

---

## 三、主题系统（官方渲染式，别用 setTheme/setAccentColor）

### 1. 不要用 app.setTheme("fluent-dark")
它读文件系统 `themes/fluent.qss.tpl`，exe 打包 cwd 不可控会炸。
官方注释原话：`不要用 app.setTheme（它读文件系统模板，exe 版会炸），一律走渲染式 applyTheme()`

### 2. setAccentColor 与 setStyleSheet 交互不透明
官方 JQtGallery 绕开它，改 `currentVars.put("accent", hex)` 后整体重渲染。

### 3. 官方渲染式架构（照抄）
```java
static String readThemeTemplate() {
    // 1. 文件系统 themes/fluent.qss.tpl
    // 2. 兜底：classpath /themes/fluent.qss.tpl（打包 exe 必需）
    // 3. 都没有 → 回退 app.setTheme("fluent-dark")
}

static void renderTheme(String name, Map<String,String> vars, boolean light) {
    String tpl = readThemeTemplate();
    tpl = tpl + "\n" + myCustomStyles();   // 追加自定义样式（%accent% 等变量）
    for (var e : vars.entrySet()) tpl = tpl.replace("%"+e.getKey()+"%", e.getValue());
    app.setStyleSheet(tpl);                 // ← 唯一入口
}

applyTheme("fluent-dark") → renderTheme(..., QApplication.FLUENT_DARK, false)
applyTheme("fluent-light") → renderTheme(..., QApplication.FLUENT_LIGHT, true)
setAccent(hex) → vars.put("accent", hex); vars.put("accent-fg", light?"#FFFFFF":"#000000");
                 renderTheme(themeName + " + 强调色", vars, light);
```

### 4. 官方 FLUENT 变量表（22 个 %var%）
```
win-bg fg fg-strong fg-hint fg-disabled card-bg card-border
btn-bg btn-hover btn-pressed btn-disabled accent accent-fg
switch-off switch-off-hover nav-fg nav-hover nav-selected
input-bg input-border titlebar-hover titlebar-pressed
```
派生色自己算：accent-hover = lighten(accent, 0.12)、accent-pressed = darken 等。

---

## 四、其他坑

| 问题 | 说明 |
|------|------|
| JQtTitleBar 拖不动 | 内部全按钮吃鼠标事件；无边框拖动靠事件冒泡到窗口 startSystemMove，可拖区域≈0。改用 QLabel 容器自绘标题栏 |
| QLayout 无 addSpacing | 只有 addStretch；分隔用空 QFrame 或 spacing |
| QWidget 无 pos()/geometry() | animateMove 需绝对坐标但拿不到当前位置，动画前先记坐标 |
| setCursor(String) 不存在 | 手型光标需 QSS cursor: pointinghand？未验证 |
| QSlider 构造 | 必须三参 new QSlider(min, max, value)，无默认构造 |
| QSS :hover 无过渡 | 瞬间切换；qf 的 120ms 背景过渡 JQt 未全量支持 |
| 循环动画 | app.schedule 循环在窗口关闭后行为未文档化，用 boolean 标志控制 |
| 版本渠道 | GitHub 7 个 release（alpha/TEST 混用），官网 downloads.json 停 v0.2；以 GitHub 为准 |

---

## 五、快速自检清单

- [ ] 所有类名 Q 前缀（QPushButton/QFrame/...），信号 onClicked
- [ ] 页面直接进 root，布局嵌套用 addLayout
- [ ] w.setLayout(root) 之后才 hide() 非当前页
- [ ] 切页用 show/hide（不用 QStackedLayout）
- [ ] 主题走渲染式：readThemeTemplate + renderTheme + FLUENT_DARK/LIGHT
- [ ] 自定义样式用 %变量% 追加进模板，不用 setAccentColor
- [ ] themes/fluent.qss.tpl 文件在运行目录（或打进 jar）
