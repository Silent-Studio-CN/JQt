# 04 · 对象生命周期、线程与信号回调

> JQt 的 native 对象管理与线程模型：Cleaner、句柄注册表、Qt 父子管理、
> 回调时机、定时器。这些坑直接导致过崩溃（exit code -1）。

## 1. 句柄注册表（registerHandle / requireHandle）

- 每个 native 对象注册进 `g_handles`（id → 指针），返回 Java 侧 long 句柄。
- **javaOwned 标记**：Java 管理（GC 回收时 dispose）vs Qt 管理（addWidget/setLayout 后）。
- `markQtOwned`：控件加入布局后标记归 Qt 管理，Cleaner 不再干预。
- `QObject::destroyed` 信号自动注销句柄（含父删子、布局清理、deleteLater 一切途径）。
- `requireHandle` 对无效/已销毁句柄抛 IllegalStateException，**JNI 回调中必须
  checkJniException 清理**，否则悬挂异常污染后续 JNI 调用。

## 2. Cleaner 与 Java 8 兼容

- 首选 `java.lang.ref.Cleaner`（Java 9+），Java 8 用 CompatCleaner（PhantomReference）。
- 控件加入窗口/布局后生命周期由 Qt 父子关系管理，**Cleaner 不再干预**
  （避免 double-free）。
- dispose 的 native 实现双重保护：javaOwned 检查 + `QMetaObject::invokeMethod`
  QueuedConnection 在 GUI 线程 delete。

## 3. 回调触发时机（最容易崩的坑）

**QOpenGLWidget 的 onInitialize 在 `show()` 时同步触发**：
```java
gl.show();          // ← 这行里 onInitialize 回调已经跑了！
// 如果回调里访问的 Java 对象（如 logLabel）还没创建 → NullPointerException
```
修复：
1. `log()` 等通用函数加 null 保护（`if (logLabel != null)`）；
2. 或者把 GL 控件创建挪到所有 UI 构建完成之后。

同类问题：任何 onXxx 回调都可能在任何 UI 操作（show/click/resize）中同步触发，
**回调体必须对"构建早期"状态健壮**。

## 4. 定时器与退出（exit code -1 之谜）

- `app.schedule(runnable, delay)` 底层是 QTimer::singleShot。
- **退出后定时器回调**：app.quit() → exec 返回 → main 结束 → JVM 开始卸载，
  但 pending 的 QTimer 回调可能仍在触发 → "QBasicTimer::start ... destroyed"。
- Gallery 的 scheduleGeo 每秒递归调度自己，退出后仍会继续调度 → 潜在崩溃。
- **修复模式**：
  ```java
  static volatile boolean appRunning = true;
  static void scheduleGeo(long delay) {
      if (!appRunning) return;
      app.schedule(() -> {
          if (!appRunning) return;
          ...
          scheduleGeo(1000);
      }, delay);
  }
  // onClose 时：appRunning = false;
  ```

## 5. 线程模型

- 所有 UI 操作必须在 GUI 线程（exec 所在线程）。
- 后台线程更新 UI → `QApplication.runOnUiThread(Runnable)`。
- 工作线程（ScheduledExecutorService 等）里不要直接碰控件，
  一律 schedule/runOnUiThread 包装。
- `scheduleQuit(ms)` 用于延迟退出（自动演示收尾常用）。

## 6. 信号槽注册表模式

JQt 的 onXxx 回调用 `List<Consumer<...>>` 累积 + native 回调分发：

```java
private final List<Consumer<Integer>> triggeredHandlers = new ArrayList<>();
public QMenu onTriggered(Consumer<Integer> handler) {
    triggeredHandlers.add(handler);
    return this;                    // 链式调用：onXxx 返回 this
}
void nativeHandleTriggered(int id) {
    for (Consumer<Integer> h : triggeredHandlers) h.accept(id);
}
```

- **链式 API 设计**（onClicked 返回 this）是 Q-class 的一致约定。
- nativeHandleXxx 由 C++ 侧回调，必须与 native 签名严格一致
  （生成器保证，手写时容易错）。
- 全局热键分发：WM_HOTKEY → jqtDispatchHotkey → 遍历 ALL 列表匹配 hotkeyId。

## 7. 悬垂保护（dangling guard）

- dispose() 后任何方法调用抛 IllegalStateException（requireHandle 查表失败）。
- Gallery 自动演示里专门有一步验证：
  `new QPushButton("x"); dispose(); setText("y");` → 应抛 IllegalStateException。
- 这是 JQt 质量承诺的一部分：**每一个生成的 API 都经过编译验证与运行时冒烟**。
