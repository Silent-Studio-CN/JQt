# JQt ↔ Qt API 映射表（v0.4.1：类名 Q 化 + 信号对齐 + 链式）

JQt 的命名策略：**有 Qt 对应物的类用 Q 前缀（文档直接可查 Qt）**，
**JQt 原创控件保留 JQt 前缀**。方法名与 Qt 对齐，信号用 onXxx（对应 Qt 信号名）。

## 类名映射

### Q 档（Qt 有对应物，查 Qt 文档）

| JQt 类 | Qt 类 |
|--------|-------|
| `QApplication` | QApplication |
| `QWidget` | QWidget |
| `QMainWindow` | QMainWindow |
| `QPushButton` | QPushButton |
| `QLabel` | QLabel |
| `QLineEdit` | QLineEdit |
| `QComboBox` | QComboBox |
| `QListWidget` | QListWidget |
| `QCheckBox` | QCheckBox |
| `QFrame`（卡片容器） | QFrame |
| `QSlider` | QSlider |
| `QProgressBar` | QProgressBar |
| `QScrollArea` | QScrollArea |
| `QMessageBox` | QMessageBox |
| `QVBoxLayout` / `QHBoxLayout` / `QLayout` | 同名 |

### JQt 档（原创控件，看 JQt 文档）

| JQt 类 | 说明 |
|--------|------|
| `JQtSwitch` | Fluent 开关（自绘；Qt 无对应） |
| `JQtPivot` | Fluent 选项卡（自绘） |
| `JQtNavigation` | Fluent 侧栏导航（自绘） |
| `JQtTitleBar` | 跨平台标题栏 |
| `JQtInfoBar` | 顶部通知条（静态工具） |
| `JQtEasing` | 缓动枚举（Qt 的 QEasingCurve 是类，语义不同故保留） |
| `JQtAnimation` / `JQtAnimations` / `JQtAnimationTheme` | 动画系统（JQt 自有 API） |

## 信号映射（onXxx ↔ Qt 信号）

| JQt | Qt 信号 |
|------|---------|
| `onClicked` | clicked |
| `onPressed` / `onReleased` | pressed / released |
| `onToggled` | toggled |
| `onTextChanged` / `onReturnPressed` | textChanged / returnPressed |
| `onCurrentIndexChanged` | currentIndexChanged |
| `onItemClicked` / `onCurrentRowChanged` | itemClicked / currentRowChanged |
| `onValueChanged` | valueChanged |
| `onClose` / `onResized` / `onMoved` | closeEvent / resizeEvent / moveEvent（事件） |
| `onAboutToQuit` | aboutToQuit |

**信号注册**：`onXxx(handler)` 注册回调（可多个，GUI 线程），**返回 this 支持链式**：

```java
button.onClicked(() -> ...).setText("OK").setFixedSize(100, 40);
```

## Java → C++ 翻译规则

1. 类名：`Qxxx` 保持（同名）；`JQtXxx` → 查上表
2. 方法名：同名（setText/setStyleSheet/resize/show... 与 Qt 一致）
3. 信号：`onXxx(handler)` → `connect(&obj, &Class::signal, handler)`
4. 其余：一行对一行
