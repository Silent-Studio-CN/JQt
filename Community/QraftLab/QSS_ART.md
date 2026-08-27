# QSS 美术经验（QraftLab 实战总结）

> 来源：Qraft / QraftLab / FluentButtonDemo 多轮实战踩坑
> 目标：用纯 QSS 做出"有质感"的现代 UI，理解 Qt 样式引擎的行为边界

---

## 一、立体感三要素（qf 按钮的秘密）

qf（qfluentwidgets）按钮看起来"鼓起来"，核心只有三点：

```css
QPushButton {
    background: %accent%;
    border: 1px solid %accent-pressed%;   /* ① 同色系深 1px 边框 */
    border-bottom: 1px solid %accent-deep%; /* ② 底边再深 25% = 立体感来源 */
    border-radius: 6px;
    padding: 10px 24px;
}
QPushButton:hover {
    background: %accent-hover%;            /* ③ hover 提亮 12% */
}
QPushButton:pressed {
    background: %accent-pressed%;
    border-bottom: 1px solid %accent-pressed%; /* 底边压平 = 按下物理感 */
    padding-top: 11px; padding-bottom: 9px;    /* 内容下沉 1px */
}
```

**原理**：光从上方来 → 上浅下深 → 底边深色模拟投影；按下时底边变浅 = 按钮被"压扁"。
这是 Windows 11 / Fluent / Material 按钮立体感的通用公式。

### 派生色计算（Java 侧）

```java
accent-hover  = lighten(accent, 0.12)   // 提亮 12%
accent-pressed = darken(accent, 0.12)   // 压暗 12%
accent-deep   = darken(accent, 0.25)    // 底边深 25%
accent-light  = lighten(accent, 0.35)   // 渐变另一端
```

---

## 二、圆角的坑（Qt QSS 行为边界）

### 2.1 圆角可能"悄悄不生效"

**现象**：`border-radius: 20px` 的胶囊按钮显示成方形。
**疑因**：Qt 样式引擎在 **`border-bottom` 与其他边不一致**（宽度/颜色不同）时，
圆角绘制可能被钳制或异常；且 `border-radius > 高度/2` 时行为不可控。

**结论**：
- 想要可靠圆角：四边边框一致，radius ≤ 高度一半
- 想要立体边 + 圆角：圆角小（4-8px），立体边深色差异别太大
- 超大半圆角（胶囊）与立体边组合：容易失效 → 二者择一

### 2.2 意外之美：方角 + 立体边 = MC 风格

Pill 圆角失效后意外获得的效果：
- **小圆角/方角 + 深色底边** → 立体方块
- 质感接近 Minecraft 按钮（方形 + 描边 + 有厚度），但比 Swing 的四方生硬高级
- 这是"错误"带来的风格，值得作为**刻意风格**保留：
  ```css
  QPushButton#mcStyle {
      background: %accent%;
      border: 2px solid %accent-deep%;      /* 粗边框 = MC 描边感 */
      border-bottom: 3px solid %accent-deep%; /* 更厚底边 = 厚度感 */
      border-radius: 2px;                    /* 极小圆角或 0 */
      padding: 10px 26px;
  }
  QPushButton#mcStyle:pressed {
      border-bottom: 2px solid %accent-deep%; /* 按下变薄 = 按进去 */
      padding-top: 11px;
  }
  ```

---

## 三、质感系按钮设计（5 种形态）

| 形态 | 关键 QSS | 适用 |
|------|---------|------|
| **实心 Solid** | 立体边三要素 | 主操作 |
| **幽灵 Ghost** | `background: transparent` + accent 文字 + hover 淡背景 10% | 次级/工具 |
| **描边 Outline** | 1px accent 边框 + hover 淡填充 | 次级操作 |
| **胶囊 Pill** | 大圆角（注意 §2.1 的坑） | CTA/移动端 |
| **渐变 Gradient** | `qlineargradient(x1:0,y1:0,x2:1,y2:0)` accent→accent-light | 主视觉 |

### 幽灵按钮的淡背景（rgba）

QSS 变量系统里用 `hexWithAlpha()` 生成：
```java
%accent-ghost-hover%   -> rgba(r, g, b, 26)   // 10% alpha
%accent-ghost-pressed% -> rgba(r, g, b, 46)   // 18% alpha
```

---

## 四、主题变量体系（美术的"设计令牌"）

### 4.1 官方 22 变量（QApplication.FLUENT_DARK/LIGHT）

```
win-bg fg fg-strong fg-hint fg-disabled card-bg card-border
btn-bg btn-hover btn-pressed btn-disabled accent accent-fg
switch-off switch-off-hover nav-fg nav-hover nav-selected
input-bg input-border titlebar-hover titlebar-pressed
```

### 4.2 派生变量（渲染时计算）

```
accent-hover / accent-pressed / accent-deep / accent-light
accent-ghost-hover / accent-ghost-pressed
```

### 4.3 渲染式主题（唯一入口 setStyleSheet）

```java
// 官方 JQtGallery 模式：模板 + 变量表 → 整体替换样式表
String tpl = readThemeTemplate();          // themes/fluent.qss.tpl（文件→jar 双回退）
tpl += customStyles();                      // 追加自己的 %变量% 样式
for (var e : vars.entrySet()) tpl = tpl.replace("%"+e.getKey()+"%", e.getValue());
app.setStyleSheet(tpl);
// 切强调色：vars.put("accent", hex) → 重新渲染（不要用 setAccentColor 单独调）
```

**美术工作流**：改模板 → 存盘 → 重渲染，热更新 UI。QSS 就是主题皮肤，和代码解耦。

---

## 五、布局美术（QSS 之外的视觉规则）

1. **间距体系**：卡片内容 14-16px、控件间 8-10px、页边距 14px（qf 一致）
2. **圆角体系**：卡片 8-14px、按钮 4-6px、标签 4px——层级越高圆角越大
3. **阴影**：`setDropShadow(x, y, blur, alpha)`——卡片 4-6px 偏移、18-24 blur
4. **字重**：标题 bold、正文 regular、辅助 11-12px + fg-hint
5. **状态一致性**：每个可交互元素必须有 hover/pressed/disabled 三态

---

## 六、经验清单（踩坑记忆）

- [ ] `border-radius` 与 `border-bottom` 不一致 → 圆角可能失效（要圆角就别搞复杂边框）
- [ ] 大圆角（>半高）在 Qt QSS 不可控 → 胶囊用"小圆角+立体边"代替，或接受方形
- [ ] pressed 立体感 = 底边压平 + padding 下沉 1px（缺一不可）
- [ ] 幽灵按钮需要 rgba 淡背景，用 Java 生成 `hexWithAlpha`
- [ ] 主题切换走渲染式（FLUENT_DARK/LIGHT + 自定义模板追加），别用 setTheme/setAccentColor
- [ ] 模板变量是设计令牌，派生色（hover/pressed/deep）统一在渲染时算
- [ ] 按钮四态（normal/hover/pressed/disabled）是"质感"的底线，缺态 = 假按钮
- [ ] MC 风格（方角立体边）是圆角失效的意外产物，但可以当刻意的风格用

---

> 核心一句话：**质感 = 光影（立体边）+ 响应（四态）+ 一致（设计令牌）**
