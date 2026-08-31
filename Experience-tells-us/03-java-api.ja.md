# 03・Java APIの設計と利用の落とし穴

> JQtのJava API (Qクラスシステム)で使われている様々な落とし穴の多くはJQtGalleryから来ています
> 全機能プレゼンの実戦検証です。関連します:APIギャップ、名前衝突、ラムダキャプチャ、モダリティブロッキング、値のタイプ

## 1. javapでAPIを確認してからコードを書きます

JQtのバージョンは反復が速く(v0.1→v0.7.5)、APIは頻繁に変更されます。**コードを書く前にjavapを使います**:です。

```
javap -cp jqt.jar org.jqt.QSpinBox org.jqt.QPushButton ...
```

実際にAPIの切れ目を踏みました
- `QSpinBox.text()` **存在しません** →使います `cleanText()`(コンパイル新聞"記号が見つかりませんtext()")
- `QPushButton` **text()読み取り方法はありません。** →ボタン文字を並列Map記録する必要があります
(です)`IdentityHashMap<QPushButton,String>`注意しなければなりませんIdentityHashMap,
QPushButtonはequals/hashCodeを書き換えないかもしれないからです。
- `QMenu` ありません `addAction(QAction)` →使います `addItem(String)` actionId +を返します
`onTriggered(Consumer<Integer>)` 戻します
- `QDialog` ありません `addWidget` →使います `setLayout(QVBoxLayout)` + layout.addwidgetです
- `QRect` x/y/width/heightです **v0.7.5は公開フィールドからprivateメソッドに変更されます**
→「xはQRectでprivateアクセスです」をコンパイルし、x()/y()/width()/height()
- `QStackedWidget` 構造はこうです `QStackedWidget(long)`(internal handle形態)です。
無参構造ではありません——生成器は大量着地時に踏みやすい

## 2.クラス名/変数名の競合(scope可視性と同じです)

Javaローカル変数とフィールドは同じ名前のコンパイルエラーです:
- 既にフィールドがあります `QSwitch sw`(コントロールパーティション),新しいコードを宣言します `QStackedWidget sw` 衝突します。
**改名します**(swd)ではありません
- あります `QListWidget list`ラムダパラメータを呼びます `list` →" mainに変数listが定義されています。"
**ラムダパラメータの改名です**(sel)です。

## 3.ラムダ捕獲effectively-final罠です

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

カウンター用も同様です `int[] n = {0}` intではありません。

## 4モダリティexecが滞る——自動プレゼンの大きな穴

- `QDialog.exec()` / `QMessageBox.exec()` / `QInputDialog.getText()` どちらもです
モダリティブロック呼び出しです**自動プレゼン(- dg.auto =1)をクリックするとプレゼン全体が停止します。**です。
- ポリシー:自動プレゼンリストは非ブロッキングAPIのみを置きます(`QDialog.open()` 非モダリティです`showAbout` などです);
モダリティボタンは通常のmakeBtnでは自働クリックリストに登録しません。
- モダリティパスの検証が必要な場合、プローブプログラムを箇別に書き、内部scheduleタイミングreject/acceptが自働的にオフになります。

## 5.ジェネレータ時代のAPI一貫性

- v0.7.5から332個の直送型メソッド/ 60個の値型クラスがjqt-genジェネレータによって生成されます。
- 生成器の意味スクリーニング:信号/protected/存在しないAPIはすべて除外します;オーバーロードJNI接尾辞は正確に一致します。
- **JDK 26 jni.h C++モードでjclass≠jobjectです** →生成方法記号mangleです
(ランタイムUnsatisfiedLinkError)——統一jclass +ジェネレータテンプレート修正です。
- 生成器のロットが落ちました **QWidget min/max sizeなどの高週波APIです**(v0.7.4 L2 batchです)
javap returnタイプに注意してください。`minimumSize()` longパケットコードを返すには(int)(v>>32)が必要です。

## 6.バージョンアップ中の破壊的な変更(互換性があります)

| バージョンです | 変更します | 影響します 
|------|------|------|
| v0.4.1です | jqt-class→q-classへの改名(breaking)です。 | 古いコードを全量改名しました 
| v0.7.1です | QTextEditはQPlainTextEditになります | テキストリッチセマンティック→プレーンテキストです 
| v0.7.5です | QRectフィールド→privateメソッドです。 | コンパイル期間エラーです 
| v0.7.5です | QInputDialog静的ツール→インスタンスモードです | 32の生成方法を多重化します 

## 7.その他の実用的な経験

- **jar内にデモクラスを持参しますshadow classpath**v0.6のjqt.jar内蔵です
`JQtGallery.class`(公式デモ)、classpathの順番が間違っている場合は旧版を走ります。
→です `-cp out10;v06.jar;...`(自分のアウトプットが先です)。
- 色々ある(qprinter . outputformat . pdf、qserialport . openmode . read_write)は
通常のJava enumです `QPrinter.OutputFormat.PDF` 使います。
- 値タイプとjava . awt互いにトランス:qpixmap . frombufferedimage / tobufferedimage、
qfont . toawt / fromawt、qdatetime . tolocaldatetime——java生態ウィーピーのカギ橋。

