# JQt 行为契约（Behavior Contract）

> 版本：v0.6.0 · 本文档是**显示规则/布局顺序/特殊控件行为**的权威说明，
> 回答社区反馈的高频问题（addWidget 规则、QStackedLayout、hide 顺序、动画清理等）。

## 1. 控件显示规则（v0.4.1+ 变更，最重要）

### 规则：布局未挂载时 addWidget 不会显示控件

```java
QPushButton b = new QPushButton("点我");
vbox.addWidget(b);          // 此时 b 不显示（布局还没挂到窗口）
window.setLayout(vbox);     // 布局挂载 → 全部子控件统一显示
window.show();
```

- **原因**：v0.4.1 起，布局未安装时 addWidget 不再 show 控件——
  避免控件成为无父顶层窗口在启动时闪现（旧版 10-15 个窗口闪屏的根因）。
- **正确模式**：先 `addWidget` 收集，再 `window.setLayout(layout)`，最后 `show()`。
- **动态添加**（窗口已显示）：`addWidget` 后控件立即显示（布局已挂载）。
- **嵌套容器**：面板（QFrame）`setLayout` 内部布局后，面板加入窗口布局即可——
  窗口 setLayout 会**递归显示**面板内层控件（含 addLayout 嵌套）。

### 规则：用户显式 hide() 的控件不会被强制重新显示

```java
QPushButton b = new QPushButton("x");
b.hide();                    // 显式隐藏
window.setLayout(vbox);      // b 保持隐藏（尊重用户意图）
b.show();                    // 手动恢复
```

## 2. QStackedLayout / QTabWidget 页面规则

### 这是特性，不是 bug

```java
QStackedLayout stack = new QStackedLayout();
int page0 = stack.addPage(pageA);   // 页默认隐藏
int page1 = stack.addPage(pageB);
stack.setCurrentIndex(page1);       // 关键：切页才显示
window.setLayout(stack);
```

- `addPage`/`addTab` **不会**显示页面（Qt 语义：由 setCurrentIndex 管理）。
- **必须调用 setCurrentIndex(i)** 才会显示对应页——这是唯一正确用法。
- 多个页同时显示 = 使用错误（直接 show 页会被忽略/堆叠保护）。

## 3. 布局 API

- `addStretch(int)`：弹性空间（占据剩余空间）
- `addSpacing(int)`：固定间距（v0.6.0 新增）
- `setSpacing(int)` / `setContentsMargins(...)`
- `QWidget.layout()` 查询未提供：布局句柄归 Qt 管理，返回包装对象会引入
  所有权歧义（Cleaner 双重释放风险）。需要查询请用 `isCreated()` 或记录引用。

## 4. 几何与坐标

- `x()/y()/width()/height()`：单值查询（v0.3 起）
- `pos()`：`int[2]` 位置（v0.6.0 新增）
- `geometry()`：`int[4]` [x,y,w,h]（v0.6.0 新增）
- 所有坐标为 **Qt 逻辑坐标**（已按 DPI 缩放归一，PerMonitorV2 感知下与物理像素一致）。
- `animateMove/animateResize` 的目标值同为逻辑坐标，可结合 `pos()/geometry()` 读取当前位置。

## 5. 主题双轨说明

- **快捷轨**：`setTheme(name)` / `setAccentColor(hex)`——内部读 `themes/*.qss` 模板并
  替换 %var% 变量。模板缺失时**降级默认配色**（v0.5.1 起，不再崩溃）。
  打包部署请随包附带 `themes/` 目录（zip 已含）。
- **自由轨**：`setStyleSheet(String)`——完全手动控制 QSS。
- **优先级**：QSS 样式 > 调色板 > 风格引擎（Qt 标准层级）。
  若 setStyleSheet 硬编码了颜色，setAccentColor 的 QSS 变量不会覆盖它（QSS 优先）。
- 两者可叠加（全局 + 控件级）。

## 6. 动画与定时

- `schedule(Runnable, ms)`：一次性定时（Qt 定时器）。
- **事件循环停止（exec 返回）后定时器不再触发**——窗口关闭即停，无后台泄漏。
- 循环动画：在回调里再次 schedule 即可；控件已销毁时回调会安全抛
  IllegalStateException（不会 native 崩溃）。
- `JQtAnimation` 动画结束自动销毁（DeleteWhenStopped）。

## 7. QSS 与动画

- **QSS 无过渡**（Qt 限制）：`:hover` 样式瞬间切换。
- JQt 自绘控件（JQtSwitch/JQtPivot/JQtSlider 等）内置 hover/值动画（120ms 级）。
- 需要普通控件 hover 过渡：用 `QCanvasWidget` 自绘或接受瞬间切换。
- `setFixedSize` 与 QSS `min/max-width`：布局尺寸取**两者中更严格者**
  （QSS min-width > fixedSize 时按 QSS；fixedSize 更大时按 fixedSize）。

## 8. 鼠标与光标

- `setCursor(String)`：arrow / ibeam / wait / crosshair / pointinghand /
  forbidden / sizeall / sizefdiag / sizebdiag / sizewe / sizens / splitv /
  splith / openhand / closedhand（v0.6.0 起）。
- 无边框窗口拖拽：系统级 WM_NCHITTEST（HTCAPTION）实现，
  由 `QMainWindow.setDraggable(true)` 控制（JQtTitleBar 按钮区已排除拖拽命中）。

## 9. 迁移速查（v0.4.0 → v0.4.1+）

| 旧 | 新 | 说明 |
|----|----|------|
| JQtApplication | QApplication | 构造前可 rhiBackend() |
| JQtWindow | QMainWindow | 窗口能力（frameless/acrylic/拖拽） |
| JQtButton | QPushButton | onClick → onClicked |
| JQtLabel/JQtLineEdit/... | QLabel/QLineEdit/... | 全部 Q 前缀 |
| JQtVBoxLayout/JQtHBoxLayout | QVBoxLayout/QHBoxLayout | 布局 |
| JQtPanel | QFrame | 容器 |
| onClick | onClicked | 信号对齐 Qt |

> 完整映射见 [qt-mapping.md](qt-mapping.md)。

(C) SilentStudio
