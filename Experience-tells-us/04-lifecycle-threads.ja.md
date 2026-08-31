# 04・対象ライフサイクル、スレッド、信号コールバック

> JQtのネイティブオブジェクト管理とスレッドモデル:Cleaner、ビントレジストリ、Qt親子管理、
> 折り返しタイミング、タイマーです。この穴はオーバークラッシュ(exit code-1)につながります。

## 1. registerHandle / requireHandleです。

- 各ネイティブオブジェクトを登録します `g_handles`(id→ポインタ)、Javaサイドのlongハンドルを返します。
- **javaOwnedタグです**Java管理(GC回収時dispose) vs Qt管理(addWidget/setLayout後)です。
- `markQtOwned`:コントロールはレイアウトに加入した後にマークはQt管理に帰して、Cleanerはもう介入しません。
- `QObject::destroyed` 信号は自働的にハンドルを削除します(親を含んでパンクチャリングして、レイアウトを整理して、deleteLaterすべての経路)。
- `requireHandle` 無効/破棄済みハンドルドロップIllegalStateExceptionに対して、**JNI回調中でなければなりません
checkJniExceptionクリーンアップ**、そうでなければハングオーバー異常汚染後のJNI呼び出しです。

## 2. CleanerはJava 8互換です

- 第一選択です `java.lang.ref.Cleaner`(Java 9+), Java 8用CompatCleaner (PhantomReference)です。
- コントロールはウィンドウに加入します/レイアウト後のライフサイクルはQt親子関系で管理して、**クリーナーは関与しません**
(ダブルフリーは避けます)。
- disposeのnativeは二重の保護を実現します:javaOwnedチェック+ `QMetaObject::invokeMethod`
QueuedConnectionはGUIスレッドdeleteにあります。

## 3.リフトトリガーのタイミング(最も崩れやすい穴)です

**QOpenGLWidgetのonInitializeは`show()`で同期します**:です。
```java
gl.show();          // ← 这行里 onInitialize 回调已经跑了！
// 如果回调里访问的 Java 对象（如 logLabel）还没创建 → NullPointerException
```
修復します:
1. `log()` などの汎用関数にnull保護を加えます(`if (logLabel != null)`)です;
2. あるいは、GLコントロールの作成を全てのUI構築が完了した後にずらします。

同様の問題:任意のonXxxコールバックは任意のUI操作(show/click/resize)で同期トリガすることができます。
**復調体は構築初期の状態に対して頑健でなければなりません**です。

## 4.タイマーとエグジット(exit code-1の謎)です

- `app.schedule(runnable, delay)` 底辺はQTimer::singleShotです
- **ログアウト後にタイマーが戻ります。**: app.quit()→exec return→main終了→JVMアンインストール開始です。
pendingのQTimerコールバックがまだトリガー→"QBasicTimer::start…destroyed"です。
- GalleryのscheduleGeoは自分自身を毎秒再帰的にスケジューリングし、ログアウト後もスケジューリング→潜在的にクラッシュします。
- **修復モードです**:です。
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

## 5スレッドモデルです

- すべてのUI操作はGUIスレッド(execが存在するスレッド)で行う必要があります。
- バックエンドスレッド更新UI→です `QApplication.runOnUiThread(Runnable)`です。
- ワークスレッド(ScheduledExecutorServiceなど)では直接コントロールに触れないことです。
すべてschedule/runOnUiThreadパッケージです。
- `scheduleQuit(ms)` エグジット(自動プレゼン終了)を遅らせるときに使います。

## 6.信号スロットレジストリモードです

JQtのonXxxコールバックです `List<Consumer<...>>` 累積+ nativeコールバック配布です:

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

- **リンクAPI設計です**(onClicked return this)はq-classの一致約束事です。
- nativeHandleXxxはC++でバックコールされます。native署名と厳密に一致する必要があります。
(生成器保証、手書きは間違いやすいです)。
- グローバルホットキー配信:WM_HOTKEY→jqtDispatchHotkey→トラバースALLリストはhotkeyIdに一致します。

## 7.ダンングリングガード(dangling guard)です

- dispose()の後の任意のメソッド呼び出しIllegalStateException (requireHandle査表失敗)をドロップします。
- Galleryの自動プレゼンでは次のような検証を行います
`new QPushButton("x"); dispose(); setText("y");` →IllegalStateExceptionをドロップすべきです。
- これはJQt品質約束の一環です**生成されたAPIはすべてコンパイルされ検証され実行されます**です。

