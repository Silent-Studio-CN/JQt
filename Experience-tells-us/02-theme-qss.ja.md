# 02・テーマレンダリングとQSS

> JQtのテーマ体系は公式setTheme(path, vars)テンプレートのレンダリングとコミュニティカスタムの2つです。
> 本章では、テンプレートの仕組み、スタイルの優先度、残留問題のすべての経験を記録します。

## 1. fluent.qss.tplテンプレート描画メカニズム

- 公式テンプレートはあります **22個の%var%プレースホルダーです**(win-bg/fg/accent/btn-*/card-*/input-*/nav-*…です)です。
- 描画方法:テンプレートを読む→です。 `replace("%"+key+"%", value)` →です `app.setStyleSheet(qss)`です。
- **レンダリング式(render-based)です。**: setThemeのファイルパス検索に頼らず、exeパッケージ版で安全です。
- テンプレート読み込み優先度:ファイルシステムthemes/fluent.qss.tpl優先、jarリソースポケット内です。
- fluent.qss.tpl(ベース)+ qraft-styles.qss(立体ボタン)+ sck-extra.qss(専用コントロール)です。

## 2パターン優先度(このカタログで踏みやすい穴)

優先順位は上から下です:
1. **コントロールレベルsetStyleSheetです**(です)`widget.setStyleSheet(...)`——最高、永遠に全局を覆います
2. **オブジェクト名選択器です**(です)`QPushButton#themeBtn`——次の高いです
3. **appレベル/ winレベルsetStyleSheetです** 全般です

### ピット1:コントロールレベル暗色上書き釘付けテーマです
```java
// 错误示范：启动时对每个控件单独 setStyleSheet 暗色
applyDarkStyles();   // 每个控件 setStyleSheet 暗色
// 之后无论怎么切全局主题，控件级样式优先级更高 → 永远暗色
```
**修復します**:コントロールレベルのロットを削除して上書きして、すべて全局のQSSテンプレートに渡してレンダリングします;
箇別コントロールで特別仕様が必要な場合は、objectNameセレクタでテンプレートに入れます。

### ピット2:インライン仕様ハードコーディングカラー(黒残り)です。
```java
topBar.setStyleSheet("QFrame#topBar { background-color: #1f1f1f; }");  // 硬编码
```
インライン仕様が一番優先度が高く、テンプレートのレンダリングではカバーできません→薄くカットしてもトップ欄は黒のままです。
**修復します**:インラインスタイルを削除し、テンプレートルール+変数に変更しました(`%topbar-bg%`)です。

### 穴3:QSSファイルの中で硬い符号化濃い色です
```css
QTextEdit, QPlainTextEdit { background: #1a1a1a; }   // 日志区永远黑
```
**修復します**変量化です `%terminal-bg%` / `%terminal-fg%`薄い色は底の深い字を告白します。
ダークフォームは黒地を保持します。

## 3.ダブルテーブルデザイン(パステル/ダークスケール)です。

- 2つの同型変化スケールです`lightVars()` / `darkVars()`キーの集合が完全に一致しなければなりません
そうでないと、あるテーマのレンダリング後に%var%が残ってQSS解析に失敗します。
- **トークンをデザインします**border/border-deep/radius/card-radius/ picl-radius /fontsizeに統一されています。
- 強調色(accent)働的変数:テーマカラーを切り替えるときlighten/darken/withAlphaで生成します。
accent-hover/pressed/deep/ghost-hover/ghost-pressedです。
- **applyTheme(name)はnameパラメータを使用しなければなりません**ハードコードの歴史があります
QSSパラメータを完全に無視したバージョン(パステルカットが効かない最大の元凶)です。

## 4. QSS解析失敗の洗い出し

- Qt新聞です `Could not parse application stylesheet` 時です:
1. 結果があるかどうか確認します**未置換の%var%です**(正則です `%[a-z-]+%` スキャンします);
2. カラーフォーマットをチェックします(rgba()と#RRGGBB混用);
3. セレクタ構文をチェックします(`QLabel#detailPanel QLabel` この子孫+IDの組み合わせ)です。
- %var% (filled by setTheme()のような説明文)は無害です。

## 5.テーマボタンのインタラクションデザインです

- テーマ切替ボタンのアイコンには「クリック後の行方」が表示されます。ダークモードではお好みの色に戻ります。
薄いモードと☾(时それは濃い色の移籍し)。
- 切り替えの後、同期更新ボタン文字、そうでなければユーザーは現在の状態を知りません。

## 6検証方法論です

- **プローブを染めます**:直接調renderThemeQss +変テーブル、断言出力を含む/含まないキーカラー値です:
`LIGHT has #f3f3f3: true`です。`LIGHT terminal dark: false`です。
- **実行履歴です**:です。`[SCK] theme=fluent-light qss len=11085`stderrにはparseエラーがありません。
- 注意:前景文字色(color:)と背景色(background:)の両方をチェックします。
「残留色値」を検索するときはfgかbgかを確認します。

