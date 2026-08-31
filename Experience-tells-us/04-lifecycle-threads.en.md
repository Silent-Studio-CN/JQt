# 04. Object Lifecycle, Threads, and Signal Callbacks

> native object management and thread model of JQt: Cleaner, Handle Registry, Qt parent-child management
> Callback timing, timer. These pitfalls directly led to the crash (exit code-1).

## Handle Registry (registerHandle/requireHandle)

- Each native object is registered `g_handles`(id → pointer), returns the Java side long handle.
- **javaOwned tag**Java management (dispose during GC collection) vs Qt management (after addWidget/setLayout).
- `markQtOwned`After the control is added to the layout, it is marked and managed by Qt, and the Cleaner no longer intervened.
- `QObject::destroyed` The signal automatically cancels the handle (including all methods such as parent deletion of child, layout cleaning, and deleteLater).
- `requireHandle` Throw an IllegalStateException for invalid/destroyed handles, which is mandatory in the JNI callback
checkJniException cleanup **, otherwise the suspended exception will contaminate subsequent JNI calls.

## 2. Cleaner is compatible with Java 8

- First choice `java.lang.ref.Cleaner`(Java 9+), Java 8 uses CompatCleaner (PhantomReference).
- After the control is added to the window/layout, its lifecycle is managed by the Qt parent-child relationship.**Cleaner no longer intervenes**
(Avoid double-free).
- dispose's native implementation offers dual protection: javaOwned check + `QMetaObject::invokeMethod`
QueuedConnection in the GUI thread delete.

## 3. The timing of pullbacks (The most likely pitfalls to collapse)

**The onInitialize of QOpenGLWidget is triggered synchronously during 'show()'**:
```java
gl.show();          // ← 这行里 onInitialize 回调已经跑了！
// 如果回调里访问的 Java 对象（如 logLabel）还没创建 → NullPointerException
```
Repair
1. `log()` Such general functions with null protection`if (logLabel != null)`";"
2. Or move the creation of the GL control to after all UI builds are completed.

Similar issue: Any onXxx callback may be triggered synchronously in any UI operation (show/click/resize)
**The callback body must be robust to the "early build" state**.

## 4. Timer and exit (The Mystery of Exit code-1)

- `app.schedule(runnable, delay)` The bottom layer is QTimer::singleShot.
- **The timer callback after exiting**app.quit() → exec returns → main ends → JVM starts unloading
But the pending QTimer callback might still be triggered → "QBasicTimer::start..." destroyed.
- The scheduleGeo of Gallery recursively schedules itself every second. Even after exiting, it will continue to schedule → potential crash.
- **Repair mode**:
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

## 5. Thread Model

- All UI operations must be carried out in the GUI thread (the thread where exec is located).
- The background thread updates the UI → `QApplication.runOnUiThread(Runnable)`.
- Do not directly touch controls in the worker thread (such as ScheduledExecutorService, etc.)
All are packaged in schedule/runOnUiThread.
- `scheduleQuit(ms)` Used for delayed exit (commonly used for automatic presentation closing).

## 6. Signal slot registry mode

The onXxx callback of JQt `List<Consumer<...>>` Accumulation + native callback distribution

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

- **Chain API design**(onClicked returns this) is a consistent convention for Q-class.
- nativeHandleXxx is a callback from the C++ side and must be strictly consistent with the native signature
(The generator guarantees that mistakes are prone to occur when writing by hand.)
- Global hotkey distribution: WM_HOTKEY → jqtDispatchHotkey → Traverse the ALL list to match the hotkeyId.

## 7. Overhang protection (dangling guard)

- After dispose(), any method call throws an IllegalStateException (the requireHandle lookup fails).
- There is a dedicated step for verification in the Gallery auto-demonstration:
`new QPushButton("x"); dispose(); setText("y");` An IllegalStateException should be thrown.
- This is part of JQt's quality commitment:**Every generated API has undergone compilation verification and runtime smoking**.

