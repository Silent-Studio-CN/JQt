# 06・コミュニティ・エンジニアリングの約定とリリースの流れ

> JQtコミュニティ(Community/ディレクトリ)でのエンジニアリングの約束とリリースのコラボレーションの経験です。
> JQtGalleryのバージョンファイリング、自動プレゼンテーション、キャプションフォーマットの配布、更新ポリシーに従っています。

## 1.コミュニティディレクトリ版ファイリング約定(ユーザの裁量フォーマット)

```
Community/JQtGallery/
├── JQtGallery.java        # 根目录 = 最新版本（跟随当前 release）
├── README.md
├── NordTheme.java / SolarizedTheme.java / TerminalTheme.java   # 主题包（版本无关）
└── v5.0/                  # 旧版本归档子目录
    ├── JQtGallery.java
    └── README.md
```

- **ルートディレクトリは最新版を入れて、旧版はvX.Y/子ディレクトリに入ります**(v0.6.0から実行)です。
- READMEは、ファイリングされたそれぞれのバージョンに準拠したJQtバージョンをテーブルで記録します。
- テーマ系(NordThemeなど)のバージョンは関係なく、1部でokです。

## 2.自動プレゼンモード(- dg.auto =1)です。

JQtGalleryの自動化デモです`app.schedule` 順次各パーティションをトリガしますボタンをクリックして検証します:

- 新地区ごとのボタンで登録関数(v6btn / v61btn v7btn / v74btn v75btn)リストに登録し、
autoモードが次々と現れます `bb.click()` +ログです(`自动点击: 按钮名`)です。
- **ポップアップボタンは自動リストに入りません。**(モダリティexecが壊れてしまいます):
QDialog.exec / qmessagebox.exec / QInputDialog.getText /レジストリの自己啓発を書きます。
- パーティションの切り替えも自動です:`pivot.setCurrentIndex(n)` +「パーティション切替トリガー」ログです。
- 終了切り戻しv0.6パーティション(復現ユーザー手働切替経路、防レイアウト回帰)。
- 基準をクリアします。`EXIT=0` +ログ行数安定+クリック異常なしです。

## 3.ポリシーの更新に従います(リリースごとに必ずリストを作ります)

1. GitHub releases (api.github.com、タグ名に注意)を調べます。
2. release notes (body)を読んで、新しいクラス/新しいAPIをリストアップします。
3. jar + windows-x64.zip(またはローカルリポジトリdist/生成物)をダウンロードします。
4. javapの新しいクラスは署名を確認します(コードを書く前に必ず調べます)。
5. Galleryに新しいパーティションを追加(pivot +パネル+ボタン登録+ autoクリック)します。
6. コンパイル+自働プレゼン全緑です。
7. fat jar +配備(lib/pack/runtimeの3か所+ハッシュチェック)を再構築します。
8. READMEを更新します(バージョン番号+パーティションキャプション+ビルド命令)。
9. commit + push(プッシュ失敗のリトライは最大8回、ネットワークのジッタは常時)です。

フォロー済みバージョン:v0.6 (L1 API)→v0.6.1 (Exclusive Kit)→v0.7.0~0.7.2
(universalキットカットミニはqprinter / qsql qaction / qlistview qclipboard画像)→
v0.7.3 / 0.7.4 (qopenglwidget / qserialport)→v0.7.5(60値タイプの类)。

## 4.説明書の3段フォーマットを発行します(ユーザの裁量)

説明は3つに絞り込まなければなりません。

1. **今回のアップデートで追加されました** 新しいカテゴリー/新しいAPI/新しい機能リストです
2. **修復済み(詳細)です。** バグ修正の箇条書きです
3. **地域貢献に感謝します(詳細)** 外部の貢献者です(例えばQraftLab)

構造例(v0.7.2):
```
# JQt v0.7.2-Universal-Kit — 工业模块：QPrinter + QSql
> QtPrintSupport / Qt6Sql 首次 API 化
## QPrinter（打印/PDF）
- QPrinter 类：setOutputFormat(NATIVE/PDF)、setPageSize(A4/A3/...)
- QTextEdit.print(QPrinter) / printToPdf(path)
## QSql（数据库）
- QSqlDatabase：addDatabase(SQLITE/PSQL/MYSQL)、exec、lastError
- 驱动插件 workaround + 可用性预检
## 验证
- SmokeV072 四平台 CI 全绿
## 资产
- 各平台 jar/zip/裸库列表
```

## 5.バージョン名と配信ルートです

- 名前はこうです`0.7.5` デジタルバージョン+です `-Generator-Kit` コードネームを発表します。
- 3つのチャンネルが同じコード/生成物です
- GitHub Releases(メインチャネル)です
- Maven Centralです`io.github.silent-xiaomiao:jqt:0.7.5`(デジタル版です)
- JitPackです`com.github.Silent-Studio-CN:JQt:0.7.5-Generator-Kit`
- GitHub ReleasesはただJDK26主jar +完全なパッケージをパスします;旧バージョンのjarは再生します
`jqt.silentstudio.cn/releases`(サイトはまだローンチしていません、READMEはComing Soonを表示します)。

## 6.テスト文化(JQt品質承諾)です。

- **機械が生産し,人が修理するだけです**生成されたAPIはすべて通過します
コンパイル断言(javac) +実行時発煙(Smoke*シリーズ)四コンソールCIです。
- 煙の種類の名前:SmokeL1/SmokeL1b2、SmokeExclusive、SmokeV072、です。
「SmokeV073」、「SmokeV074」、「SmokeGenApi 16/16」、「SmokeInputDialog 13/13」です。
SmokeSqlDb 10/10です。
- dllから導き出されたmangleカウントは品質指標:369→8(残りはJava宣言なしの無害孤児)です。
- クラッシュログ:jqt-crash.log (SEH handler) + hs_err (CI捕獲native frames)です。

## 7.ユーザー主導の反復の教訓です

- 「ボタンを押しても反応がありません」→調べてみる**ウィンドウサイズ/レイアウトドリフトです**とボタンの論理を疑います。
- ユーザーからのフィードバック「テーマを切り残しています」→調べる**インライン仕様/コントロールレベル仕様/ハードコーディングカラーです**三点セットです。
- ユーザーフィードバック退出コード-1→onClose + shutdown hook打点位置(04章参照)です。
- タッチスクリーン環境の自働クリックイベント:まず、プログラム論理がトリガーパス(grep onClicked)を持っているかどうかを検証します。
ハードウェアのせいにしてはいけませんユーザーは気にします

