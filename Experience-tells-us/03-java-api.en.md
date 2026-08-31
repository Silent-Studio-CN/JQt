# 03 · Traps in Java API Design and Usage

> Most of the various traps in the use of JQt's Java API (Q-class system) come from JQT Gallery
> Practical verification of full-function demonstration. Involved: API gaps, naming conflicts, lambda capture, modal blocking, value types.

## First, use javap to confirm the API, and then write the code

The version iteration of JQt is fast (from v0.1 to v0.7.5), and the API is frequently added, deleted or modified.**Use javap before writing code**:

```
javap -cp jqt.jar org.jqt.QSpinBox org.jqt.QPushButton ...
```

API gaps encountered in actual combat:
- `QSpinBox.text()` **It doesn't exist** Use `cleanText()`(Compiled report "Symbol text() Not found")
- `QPushButton` **There is no text() reading method** A parallel Map is required to record the button text
(`IdentityHashMap<QPushButton,String>`Note that an IdentityHashMap must be used.
Because QPushButton may not override equals/hashCode
- `QMenu` None `addAction(QAction)` Use `addItem(String)` Return actionId +
`onTriggered(Consumer<Integer>)` Callback
- `QDialog` None `addWidget` Use `setLayout(QVBoxLayout)` + layout.addWidget
- `QRect` The x/y/width/height is **v0.7.5 Change from public field to private method**
Compile the report "x is private access in QRect ", and instead use x()/y()/width()/height()
- `QStackedWidget` The structure is `QStackedWidget(long)`(internal handle form)
It's not a parameterless construction - generators are prone to being stepped on when implemented in batches

## 2. Class name/variable name conflict (same as scope visibility

Compilation error of Java local variable and field with the same name:
- Existing fields `QSwitch sw`(Control partitioning), new code is declared again `QStackedWidget sw` "Conflict."
**Change name**(swd) rather than reuse.
- Already available `QListWidget list`The lambda parameter is called again `list` → "The variable list has been defined in main."
**Rename the lambda parameter**(sel).

## 3. lambda captures the effectively-final trap

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

Similarly: The counter is used `int[] n = {0}` Rather than int.

## 4. Modal exec will block - a big pitfall in automatic demonstration

- `QDialog.exec()` / `QMessageBox.exec()` / `QInputDialog.getText()` All of them
Modal blocking call**Clicking in the Auto Demo (-Dg.auto=1) will freeze the entire demo**.
- Strategy: The automatic demonstration list only includes non-blocking apis`QDialog.open()` Non-modal`showAbout` "Etc."
Modal buttons are not registered in the auto-click list with a regular makeBtn.
- When it is necessary to verify the modal path, write a separate probe program. The internal schedule will automatically close when rejecting/accepting at regular intervals.

## 5. API Consistency in the Generator era

- Starting from v0.7.5, 332 direct-passing methods / 60 value type classes are batch-produced by the jqt-gen generator.
- Generator semantic filtering: Signals /protected/ non-existent apis are all excluded; Overloaded JNI suffix exact matching.
- **In the C++ mode of JDK 26 jni.h, jclass≠jobject** Generate the method symbol mangle
(Runtime UnsatisfiedLinkError) -- Unified jclass + generator template correction.
- After the generator batch landed **High-frequency apis such as QWidget min/max size**(v0.7.4L2 batch)
Pay attention to the javap return type`minimumSize()` Return the long packaging encoding, which needs to be unpacked (int)(v>>32).

## 6. Destructive changes during Version Evolution (Compatibility Required)

| Version | Change | "Influence" 
|------|------|------|
| v0.4.1 | JQt-class → Q-class Renaming (breaking) | All the old code has been renamed 
| v0.7.1 | Modify the underlying QTextEdit to QPlainTextEdit | Rich text semantics → plain text 
| v0.7.5 | "QRect field → private method. | Compile-time error 
| v0.7.5 | QInputDialog Static Tool → Instance Mode | 32 generation methods are reused 

## 7. Other practical experiences

- **The built-in demo class in the jar will shadow the classpath**Built-in in jqt.jar for v0.6
`JQtGallery.class`(Official demo), if the classpath order is incorrect, it will run the old version.
- `-cp out10;v06.jar;...`(One's own output comes first).
- Enumeration (QPrinter OutputFormat. PDF, QSerialPort. OpenMode. READ_WRITE)
Ordinary Java enum, straightforward `QPrinter.OutputFormat.PDF` Use.
- Value types and Java. The awt transfers: QPixmap fromBufferedImage/toBufferedImage,
QFont. ToAwt/fromAwt, QDateTime. ToLocalDateTime - traffic with Java ecosystem key bridge.

