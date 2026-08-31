# 04 · 對象生命週期、線程與信號回調

> JQt 的 native 對象管理與線程模型：Cleaner、句柄註冊表、Qt 父子管理、
> 回調時機、定時器。這些坑直接導致過崩潰（exit code -1）。

## 1. 句柄註冊表（registerHandle / requireHandle）

- 每個 native 對象註冊進 `g_handles`（id → 指針），返回 Java 側 long 句柄。
- **javaOwned 標記**：Java 管理（GC 回收時 dispose）vs Qt 管理（addWidget/setLayout 後）。
- `markQtOwned`：控件加入佈局後標記歸 Qt 管理，Cleaner 不再幹預。
- `QObject::destroyed` 信號自動註銷句柄（含父刪子、佈局清理、deleteLater 一切途徑）。
- `requireHandle` 對無效/已銷燬句柄拋 IllegalStateException，**JNI 回調中必須
checkJniException 清理**，否則懸掛異常污染後續 JNI 調用。

## 2. Cleaner 與 Java 8 兼容

- 首選 `java.lang.ref.Cleaner`（Java 9+），Java 8 用 CompatCleaner（PhantomReference）。
- 控件加入窗口/佈局後生命週期由 Qt 父子關係管理，**Cleaner 不再幹預**
（避免 double-free）。
- dispose 的 native 實現雙重保護：javaOwned 檢查 + `QMetaObject::invokeMethod`
QueuedConnection 在 GUI 線程 delete。

## 3. 回調觸發時機（最容易崩的坑）

**QOpenGLWidget 的 onInitialize 在 `show()` 時同步觸發**：
```java
gl.show();          // ← 这行里 onInitialize 回调已经跑了！
// 如果回调里访问的 Java 对象（如 logLabel）还没创建 → NullPointerException
```
修復：
1. `log()` 等通用函數加 null 保護（`if (logLabel != null)`）；
2. 或者把 GL 控件創建挪到所有 UI 構建完成之後。

同類問題：任何 onXxx 回調都可能在任何 UI 操作（show/click/resize）中同步觸發，
**回調體必須對"構建早期"狀態健壯**。

## 4. 定時器與退出（exit code -1 之謎）

- `app.schedule(runnable, delay)` 底層是 QTimer::singleShot。
- **退出後定時器回調**：app.quit() → exec 返回 → main 結束 → JVM 開始卸載，
但 pending 的 QTimer 回調可能仍在觸發 → "QBasicTimer::start ... destroyed"。
- Gallery 的 scheduleGeo 每秒遞歸調度自己，退出後仍會繼續調度 → 潛在崩潰。
- **修復模式**：
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

## 5. 線程模型

- 所有 UI 操作必須在 GUI 線程（exec 所在線程）。
- 後臺線程更新 UI → `QApplication.runOnUiThread(Runnable)`。
- 工作線程（ScheduledExecutorService 等）裏不要直接碰控件，
一律 schedule/runOnUiThread 包裝。
- `scheduleQuit(ms)` 用於延遲退出（自動演示收尾常用）。

## 6. 信號槽註冊表模式

JQt 的 onXxx 回調用 `List<Consumer<...>>` 累積 + native 回調分發：

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

- **鏈式 API 設計**（onClicked 返回 this）是 Q-class 的一致約定。
- nativeHandleXxx 由 C++ 側回調，必須與 native 簽名嚴格一致
（生成器保證，手寫時容易錯）。
- 全局熱鍵分發：WM_HOTKEY → jqtDispatchHotkey → 遍歷 ALL 列表匹配 hotkeyId。

## 7. 懸垂保護（dangling guard）

- dispose() 後任何方法調用拋 IllegalStateException（requireHandle 查表失敗）。
- Gallery 自動演示裏專門有一步驗證：
`new QPushButton("x"); dispose(); setText("y");` → 應拋 IllegalStateException。
- 這是 JQt 質量承諾的一部分：**每一個生成的 API 都經過編譯驗證與運行時冒煙**。

