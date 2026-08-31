# 03 · Java API 設計與使用陷阱

> JQt 的 Java API（Q-class 體系）使用中的各類陷阱，多數來自 JQtGallery
> 全功能演示的實戰驗證。涉及：API 缺口、命名衝突、lambda 捕獲、模態阻塞、值類型。

## 1. 先 javap 確認 API，再寫代碼

JQt 版本迭代快（v0.1 → v0.7.5），API 經常增刪改。**寫代碼前先 javap**：

```
javap -cp jqt.jar org.jqt.QSpinBox org.jqt.QPushButton ...
```

實戰踩過的 API 缺口：
- `QSpinBox.text()` **不存在** → 用 `cleanText()`（編譯報"找不到符號 text()"）
- `QPushButton` **沒有 text() 讀取方法** → 需並行 Map 記錄按鈕文字
（`IdentityHashMap<QPushButton,String>`，注意必須 IdentityHashMap，
因爲 QPushButton 可能不重寫 equals/hashCode）
- `QMenu` 沒有 `addAction(QAction)` → 用 `addItem(String)` 返回 actionId +
`onTriggered(Consumer<Integer>)` 回調
- `QDialog` 沒有 `addWidget` → 用 `setLayout(QVBoxLayout)` + layout.addWidget
- `QRect` 的 x/y/width/height 在 **v0.7.5 從公開字段改爲 private 方法**
→ 編譯報"x 在 QRect 中是 private 訪問"，改用 x()/y()/width()/height()
- `QStackedWidget` 構造是 `QStackedWidget(long)`（internal handle 形態），
不是無參構造——生成器批量落地時容易踩

## 2. 類名/變量名衝突（同 scope 可見性）

Java 局部變量與字段同名編譯錯誤：
- 已有字段 `QSwitch sw`（控件分區），新代碼再聲明 `QStackedWidget sw` → 衝突。
**改名**（swd）而不是複用。
- 已有 `QListWidget list`，lambda 參數再叫 `list` → "已在 main 中定義變量 list"。
**lambda 參數改名**（sel）。

## 3. lambda 捕獲 effectively-final 陷阱

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

同理：計數器用 `int[] n = {0}` 而非 int。

## 4. 模態 exec 會阻塞 —— 自動演示的大坑

- `QDialog.exec()` / `QMessageBox.exec()` / `QInputDialog.getText()` 都是
模態阻塞調用，**在自動演示（-Dg.auto=1）裏點擊會卡死整個演示**。
- 策略：自動演示列表只放非阻塞 API（`QDialog.open()` 非模態、`showAbout` 等）；
模態按鈕用普通 makeBtn 不註冊進自動點擊列表。
- 需要驗證模態路徑時，單獨寫探針程序，內部 schedule 定時 reject/accept 自動關閉。

## 5. 生成器（Generator）時代的 API 一致性

- v0.7.5 起 332 個直傳型方法 / 60 個值類型類由 jqt-gen 生成器批量產出。
- 生成器語義篩選：信號/protected/不存在 API 全部剔除；重載 JNI 後綴精確匹配。
- **JDK 26 jni.h C++ 模式下 jclass≠jobject** → 生成方法符號 mangle
（運行時 UnsatisfiedLinkError）—— 統一 jclass + 生成器模板修正。
- 生成器批次落地後 **QWidget min/max size 等高頻 API**（v0.7.4 L2 batch）
注意 javap 返回類型（`minimumSize()` 返回 long 打包編碼，需 (int)(v>>32) 解包）。

## 6. 版本演進中的破壞性變更（要兼容）

| 版本 | 變更 | 影響 
|------|------|------|
| v0.4.1 | JQt-class → Q-class 重命名（breaking） | 老代碼全量改名 
| v0.7.1 | QTextEdit 底層改 QPlainTextEdit | 富文本語義 → 純文本 
| v0.7.5 | QRect 字段 → private 方法 | 編譯期錯誤 
| v0.7.5 | QInputDialog 靜態工具 → 實例模式 | 32 個生成方法複用 

## 7. 其他實用經驗

- **jar 內自帶 demo class 會 shadow classpath**：v0.6 的 jqt.jar 內置
`JQtGallery.class`（官方 demo），classpath 順序不對會跑舊版。
→ `-cp out10;v06.jar;...`（自己的輸出在前）。
- 枚舉（QPrinter.OutputFormat.PDF、QSerialPort.OpenMode.READ_WRITE）都是
普通 Java enum，直接 `QPrinter.OutputFormat.PDF` 使用。
- 值類型與 java.awt 互轉：QPixmap.fromBufferedImage/toBufferedImage、
QFont.toAwt/fromAwt、QDateTime.toLocalDateTime —— 與 Java 生態互通的關鍵橋樑。

