# 03 · Java API 设计与使用陷阱

> JQt 的 Java API（Q-class 体系）使用中的各类陷阱，多数来自 JQtGallery
> 全功能演示的实战验证。涉及：API 缺口、命名冲突、lambda 捕获、模态阻塞、值类型。

## 1. 先 javap 确认 API，再写代码

JQt 版本迭代快（v0.1 → v0.7.5），API 经常增删改。**写代码前先 javap**：

```
javap -cp jqt.jar org.jqt.QSpinBox org.jqt.QPushButton ...
```

实战踩过的 API 缺口：
- `QSpinBox.text()` **不存在** → 用 `cleanText()`（编译报"找不到符号 text()"）
- `QPushButton` **没有 text() 读取方法** → 需并行 Map 记录按钮文字
  （`IdentityHashMap<QPushButton,String>`，注意必须 IdentityHashMap，
  因为 QPushButton 可能不重写 equals/hashCode）
- `QMenu` 没有 `addAction(QAction)` → 用 `addItem(String)` 返回 actionId +
  `onTriggered(Consumer<Integer>)` 回调
- `QDialog` 没有 `addWidget` → 用 `setLayout(QVBoxLayout)` + layout.addWidget
- `QRect` 的 x/y/width/height 在 **v0.7.5 从公开字段改为 private 方法**
  → 编译报"x 在 QRect 中是 private 访问"，改用 x()/y()/width()/height()
- `QStackedWidget` 构造是 `QStackedWidget(long)`（internal handle 形态），
  不是无参构造——生成器批量落地时容易踩

## 2. 类名/变量名冲突（同 scope 可见性）

Java 局部变量与字段同名编译错误：
- 已有字段 `QSwitch sw`（控件分区），新代码再声明 `QStackedWidget sw` → 冲突。
  **改名**（swd）而不是复用。
- 已有 `QListWidget list`，lambda 参数再叫 `list` → "已在 main 中定义变量 list"。
  **lambda 参数改名**（sel）。

## 3. lambda 捕获 effectively-final 陷阱

```java
// 错误：局部变量被 try/catch 赋值，不是 effectively final
QOpenGLWidget glw;
try { glw = new QOpenGLWidget(); } catch (...) { glw = null; }
glw.onInitialize(() -> { ... });   // 编译错

// 正确：final 数组引用
final QOpenGLWidget[] glwRef = new QOpenGLWidget[1];
try { glwRef[0] = new QOpenGLWidget(); } catch (...) { glwRef[0] = null; }
glwRef[0].onInitialize(() -> { ... });
```

同理：计数器用 `int[] n = {0}` 而非 int。

## 4. 模态 exec 会阻塞 —— 自动演示的大坑

- `QDialog.exec()` / `QMessageBox.exec()` / `QInputDialog.getText()` 都是
  模态阻塞调用，**在自动演示（-Dg.auto=1）里点击会卡死整个演示**。
- 策略：自动演示列表只放非阻塞 API（`QDialog.open()` 非模态、`showAbout` 等）；
  模态按钮用普通 makeBtn 不注册进自动点击列表。
- 需要验证模态路径时，单独写探针程序，内部 schedule 定时 reject/accept 自动关闭。

## 5. 生成器（Generator）时代的 API 一致性

- v0.7.5 起 332 个直传型方法 / 60 个值类型类由 jqt-gen 生成器批量产出。
- 生成器语义筛选：信号/protected/不存在 API 全部剔除；重载 JNI 后缀精确匹配。
- **JDK 26 jni.h C++ 模式下 jclass≠jobject** → 生成方法符号 mangle
  （运行时 UnsatisfiedLinkError）—— 统一 jclass + 生成器模板修正。
- 生成器批次落地后 **QWidget min/max size 等高频 API**（v0.7.4 L2 batch）
  注意 javap 返回类型（`minimumSize()` 返回 long 打包编码，需 (int)(v>>32) 解包）。

## 6. 版本演进中的破坏性变更（要兼容）

| 版本 | 变更 | 影响 |
|------|------|------|
| v0.4.1 | JQt-class → Q-class 重命名（breaking） | 老代码全量改名 |
| v0.7.1 | QTextEdit 底层改 QPlainTextEdit | 富文本语义 → 纯文本 |
| v0.7.5 | QRect 字段 → private 方法 | 编译期错误 |
| v0.7.5 | QInputDialog 静态工具 → 实例模式 | 32 个生成方法复用 |

## 7. 其他实用经验

- **jar 内自带 demo class 会 shadow classpath**：v0.6 的 jqt.jar 内置
  `JQtGallery.class`（官方 demo），classpath 顺序不对会跑旧版。
  → `-cp out10;v06.jar;...`（自己的输出在前）。
- 枚举（QPrinter.OutputFormat.PDF、QSerialPort.OpenMode.READ_WRITE）都是
  普通 Java enum，直接 `QPrinter.OutputFormat.PDF` 使用。
- 值类型与 java.awt 互转：QPixmap.fromBufferedImage/toBufferedImage、
  QFont.toAwt/fromAwt、QDateTime.toLocalDateTime —— 与 Java 生态互通的关键桥梁。
