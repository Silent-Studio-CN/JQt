# 07・プローブテスト方法論(ネイティブ問題の再現と検証)

> JQtネイティブ層問題(クラッシュ/パターン/座標)が開発環境で再現しにくい時のシステムチェック方法です。
> 核心的な考え方です:**最小プローブ+時間軸スケジュール+外部観測可能信号です**です。

## なぜプローブが必要なのですか

- 実際のユーザー環境から隔離された開発環境(DSHバックグランドvsインタラクションデスクトップ)、EnumWindows/
FindWindowではウィンドウが届かないかもしれません。
- 自働デモ(- dg.auto =1)はただ「パスをクリックします」だけをカバーして、「再構築後の対話」をカバーできません、
「モダリティ枠閉鎖」、「退出クリーンアップ」などの境界です。
- ユーザー現場でしか再現できないバグ(exit−1)は、現場に持ち帰ることができる診断プローブが必要です。

## 1.最小プローブテンプレート(Java + schedule時間軸)

```java
public class XxxProbe {
    static QApplication app;
    static QMainWindow w;
    public static void main(String[] args) {
        app = new QApplication();
        w = new QMainWindow("XxxProbe", 800, 500);
        w.setFrameless(true);
        w.setFixedSize(800, 500);
        w.show();
        System.out.println("P1 started");
        app.schedule(() -> { ... }, 2000);   // STEP1
        app.schedule(() -> { ... }, 3000);   // STEP2
        app.schedule(() -> { System.out.println("DONE"); app.quit(); }, 5000);
        app.exec();
        System.out.println("P exec returned normally");
    }
}
```

ポイント:毎歩system.out打点;明示的quitを終了します;start-processリダイレクトstdout/stderrです
書類に着きます;オーバータイムでデッドラインかどうかを判断します。

## 2.使用済みプローブリスト(多重化可能)

| プローブです | 内容を検証します | 結論です 
|------|---------|------|
| FrameProbeです | setFrameless熱交換が有効かどうかです | 仕様ビット0x86CE0000(枠有り)スケジュールです。 
| SizeProbeです | setFixedSizeが制約されているかどうかは、フレームベゼルのネイティブモードです。 | resize(1200,900)は800x500リクエストされました 
| GlCrashProbeです | ウィンドウを再構築しましたQOpenGLWidget update/close | 正常(GLクラッシュを除きます) 
| TimerProbeです | scheduleGeo再帰+ closeがクラッシュするかどうかです | 正常(DSH下では戻りません-1) 
| ModalProbeです | QDialog.exec + reject + getTextクローズパスです。 | 正常アウト0です 
| FullProbeです | 全パーティション切り替え+ onCloseパスです | 正常アウト0です 

教訓です:**全プローブが0に戻りユーザーフィールドだけ-1になります** 違いは環境にあります
(タッチスクリーン合成イベント+実際の対話シーケンス)、この時点で診断ログ(onClose/shutdown hook)を使用します。
当て続けるのではなく現場に持っていきます

## 3外部からの観測手段です

- **Win32スタイルビットです**: GetWindowLongPtrW(GWL_STYLE)打点(native層fprintf)、
WS_CAPTION/WS_THICKFRAMEが本当に変化しているかどうかを検証します
- **ウィンドウ枚挙**EnumWindows + GetWindowThreadProcessIdをpidフィルタリングします。
ですが**desktop隔離の影響です**(バックグラウンドプロセスのウィンドウは別のdesktopでは見られません)。
- **GetWindowRect vs GetClientRect**:オリジナルのフレームウィンドウとは異なり、フレームレスは同じです。
- **スクリーンショットです**CopyFromScreen(同様にdesktop隔離制限を受けます)。
- **JVM診断です**: onClose打点+ Runtime.addShutdownHook打点です,
出口経路が正常かクラッシュかを判断します。

## 4.ネット制約環境でのダウンロードポリシー(JQt開発に関連したものです)

- GitHub releaseの大型ファイル(zip 16-20MB)をIWRでリトライします(10-12回、
間隔8s)、途中で断続的に成長する可能性があります。
- zipの中の単一のdllだけを取る場合はRange要求+ zlib inflate(ローカル構文解析central
directory)、パッケージ全体のダウンロードを回避します。
- dist/ローカルリポジトリには最新のビルド(jar/dll)があり、ローカルを優先的にコピーします。

## 5.規律への回帰です

1. 修復後に走ります**完全自動プレゼンです**(すべてのパーティション+終了切回)、EXITとログ行数を記す基線です。
2. リペアは「クラッシュしない」だけでは検証できません——「アクション・ペア」(スタイルビット、色値、コールバック・カウント)を検証します。
3. 配置後、ハッシュは3つの整合性(jar/dll/ソースコード)をチェックします。
4. リリース前にローカルのリポジトリにプッシュしてからGitHubをプッシュします(プッシュの最大回数は8回)。

