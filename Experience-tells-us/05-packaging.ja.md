# 05・パッケージ配信とランタイムデプロイ

> JQtアプリケーションのパッケージ化(jpackage / JDKモード)とQtランタイムデプロイテーションの経験です。
> カバー:JREディレクトリ、Qt DLLロケーション、プラグインパス、SQLドライバ、ロケーションコンセンサスです。

## 1. jpackageアプリケーションミラーリング構造(Windows)

```
JQtGallery-Pack/
├── JQtGallery/                 # 免安装版（整个文件夹一起分发）
│   ├── JQtGallery.exe          # jpackage 启动器
│   ├── jqt.dll                 # JQt native 库（必须在 exe 目录！）
│   ├── Qt6*.dll                # Qt 运行时（Core/Gui/Widgets/...）
│   ├── platforms/qwindows.dll  # QPA 平台插件
│   ├── sqldrivers/qsqlite.dll  # SQL 驱动插件
│   ├── app/
│   │   ├── gallery.jar         # 应用 fat jar
│   │   └── JQtGallery.cfg      # jpackage 配置
│   └── src/                    # 源码副本（分发用）
├── lib/gallery.jar             # JDK 模式主程序
└── runtime/                    # JDK 模式 Qt 运行时
```

## 2クリティカルパス(すべて踏んでいます)

| ルールです | 原因です 
|------|------|
| **JREはruntime/サブディレクトリに落ちます**(jpackage自働です) | Qtを手動で入れてはいけません 
| **Qt DLLはexeディレクトリにある必要があります** | Windowsローダーはexeディレクトリで検索します。 
| **jqt.dllはexeディレクトリにある必要があります** | 同上です 
| `-Djava.library.path=$APPDIR/..` | $APPDIR解析app/, jqt.dllは1つ上のレベルです 
| qt.confです `Plugins = .` | プラグインルートディレクトリ= exeディレクトリです 
| **JDKモードにQT_PLUGIN_PATHを設定します。** | コマンドラインjavaのアプリケーションディレクトリはjava.exeであり、qt.confは有効ではありません。 

## 3.プラグインの経路の2つのメカニズム(大きな穴)

JQtのSQLドライブ検索です:`QCoreApplication::libraryPaths()` + `QLibraryInfo::PluginsPath`です。

- **exeモードです**libraryPathsはexeディレクトリ+ qt.confのPluginsパスを含んでいます。
`plugins/sqldrivers/qsqlite.dll` とあります `sqldrivers/qsqlite.dll` **両方置きます**一番安定しています。
- **JDKモデル(start-gallery.bat)です**アプリケーションディレクトリはjava.exeです。`C:\...\javapath`)です
qt.confは有効になりません→ **`QT_PLUGIN_PATH=%RT%\plugins`環境変数を設定する必要があります。**です。
そうでなければQSqlDatabaseです。addDatabase("QSQLITE")ドロップIllegalStateException
"駆動用不可(sqlite pluginsは見つかっていない/ sqldrivers qsqlite)」。

```bat
set "RT=%~dp0runtime"
set "QT_QPA_PLATFORM_PLUGIN_PATH=%RT%platforms"
set "QT_PLUGIN_PATH=%RT%plugins"
set "PATH=%RT%;%PATH%"
```

## 4. fat jarビルドです

- `jar --update --file gallery.jar -C workdir JQtGallery.class` 公式jarにあるものを入れ替えます
デモクラス(公式jarはJQtGallery.クラスを持っています。
- テーマテンプレートはjarに入ります:`jar --update --file gallery.jar -C . themes/fluent.qss.tpl`です。
プログラム読みjarリソースポケット→exe版は外部テンプレートファイルが不要です。
- fat jarを実行します **classpath順序です**:自分が先に出力します(out10;v06.jar;…)です。
そうでなければ、クラスshadowの公式内蔵クラスです。

## 5.バージョンの整合性チェック(配置後必ず行います)

PowerShellのハッシュはすべての配布場所を比較します:

```
gallery.jar 三处一致（lib / pack-lib / pack-app）
jqt.dll 三处一致（runtime / pack / repo-lib）
源码四处一致（本地 / pack-src / staging / repo）
```

**落とし穴**PowerShellです `Copy-Item` 復数のソースのファイルは1つのディレクトリに時々静かに失敗します
(最初の1つだけコピーします)——**copy-itemに対応します** 命令を信じてはいけません

## 6.バージョンライブラリがセットになっています。

- リリース:JQt毎版リリース:JQt - x.ar (Java API) + windows-x64.zip(フルランタイム)+ヌードライブラリです。
(jqt-windows-6.8.3/6.11.2.dll、arm64、linx.so、macos .dylib)です。
- **ダブルQtバージョンです**(6.8.3 LTS + 6.11.2): CI四平台×双版全緑才頒布します。
- Qt 9+新しいAPI用バージョンガード(accessibleIdentifierなどのCIデュアルバージョンコンパイルアサーション)です。
- 発表plugins含まバッグ/ sqldrivers qsqlite . dll (sqlite開票>)。

