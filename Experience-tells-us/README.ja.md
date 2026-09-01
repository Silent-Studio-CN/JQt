# Experience Tells Us—JQt開発経験と踏破実録です

> このディレクトリはJQt (Java bindings for Qt)の開発過程で蓄積された教訓を沈殿させます。
> **JQt開発自体に焦点を当てた内容です**:Java API設計、JNI/nativeブリッジ、Qt行為罠、
> Windowsプラットフォームの機能、テーマのレンダリング、配信パッケージ化、コミュニティエンジニアリングの約束です。
> JQtの開発に参加しているAIプロジェクトの方向性のメンバーによって書かれ、バージョンの進化に伴って継続的に補足されています。

## 私の主な担当分野です

1. **JQtGalleryコミュニティデモプロジェクト** (Community/JQtGallery)です。
- 機能パーティションのデモンストレーション(テーマ/コントロール/アニメーション/ウィンドウ/v0.5~v0.7.5の各バージョンの新しいAPIです)
- オートプレゼンテーションモード(- dg.auto =1)とプローブテストをクリックします。
- v0.6→v0.6.1→v0.7.0~v0.7.5まで全てフォローします。
2. **JQtネイティブ層のWindowsプラシャトフォ-ムの問題を洗い出してまだ修正しています。**
- setFrameless熱交換無効(Win32スタイルビット+ DWM拡張ベゼル)——完治しました
- ウィンドウの再構築/レイアウトドリフト、固定サイズの制約、タッチ合成イベント座標です
3. **テーマレンダリングシステム**です。
- fluent.qss.tplテンプレート+変スケール描画メカニズムです
- 2テーマ(ライト/ライト)切り替え、強調色動的変数です。
4. **バッグと布の組み合わせです**
- jpackageアプリケーションミラー、Qtランタイムデプロイ、プラグインパス(qt.conf / Qt _ plugin _ path)
- 多位置配置整合性チェック
5. **コミュニティの協力と配布です。
- バージョンファイリング規約(ルートディレクトリ=最新+ vX.Y/サブディレクトリ)です。
- 説明3段フォーマット、テストレポートを配布します

## 文書索引です

| 文件 | 主題 |
|------|------|
| [01-window-native.md](01-window-native.ja.md) | Win32ウィンドウシステムとnative層(setFrameless大坑全解)です。
| [02-theme-qss.md](02-theme-qss.ja.md) | テーマの描画とQSS(テンプレート変数/優先度/残留)です。
| [03-java-api.md](03-java-api.ja.md) | Java APIの設計と使用の落とし穴です
| [04-lifecycle-threads.md](04-lifecycle-threads.ja.md) | オブジェクトライフサイクル、スレッド、信号コールバックです
| [05-packaging.md](05-packaging.ja.md) | パッケージ配信とランタイムデプロイです
| [06-community.md](06-community.ja.md) | コミュニティプロジェクトの契約と公開の流れです
| [07-probes.md](07-probes.ja.md) | プローブテスト方法論(ネイティブ問題の再現)です
| [08-setFrameless-case.md](08-setFrameless-case.md) | setFramelessリペア全記録(native排査範例)
| [09-jpackage-qt-deploy.md](09-jpackage-qt-deploy.ja.md) | jpackage で JQt アプリをパッケージ化（0xC0000142/プラグインパス/DLL依存） |


