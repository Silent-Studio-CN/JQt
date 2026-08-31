# 01・Win32ウィンドウシステムとネイティブ層

> この章はJQt Windowsプラットフォームのウィンドウシステムの上で踏んだ穴と修復を記録します。
> コアオブジェクト:JQtWindowShell (QWidgetサブクラス)、nativeSetFrameless、DWM、WM_*メッセージです。

## 1. setFrameless熱い交換失効——この目次の最も深い一課です

### 現象です
ウィンドウが表示されたら呼び出します。 `w.setFrameless(false)` 元の枠に戻します**最初のクリックは無効です**です。
先にしなければなりません `setFrameless(true)` またです `setFrameless(false)` 有効になります(「閉めてから開けて」)。

### 原因(二重になっています)

**第1層:setWindowFlagはQt層フラグだけを変更します**
```cpp
// 原实现（错误）：
win->setWindowFlag(Qt::FramelessWindowHint, false);
win->show();   // 窗口已显示，show() 是空操作
```
Qtのです `setWindowFlag` QWidget内部windowFlagsのみ更新し、**はHWNDを再構築しません。
Win32スタイルビット(WS_CAPTION/WS_THICKFRAME)の更新もありません**。窓の外観は変わりません。

**第二層:DWM枠の残留を拡張します**
ベゼルレスモードが起動しました `DwmExtendFrameIntoClientArea(hwnd, margins{1,1,1,1})` 影を作ります。
額縁を元に戻します**取り除かないことです**このDWM拡張はオリジナル枠を食べてしまいます

「閉めてから開けて」がうまくいったのは `setFrameless(true)` applyShadow()を呼び出すのです
タイミング良くDWMをトリガーしました,2回目 `setFrameless(false)` 効果があるのです

### リペア(ネイティブ層根治、配信済みです)

```cpp
win->frameless = (on == JNI_TRUE);
win->setWindowFlag(Qt::FramelessWindowHint, win->frameless);
#ifdef _WIN32
HWND hwnd = reinterpret_cast<HWND>(win->winId());
if (win->frameless) {
    LONG_PTR style = GetWindowLongPtrW(hwnd, GWL_STYLE);
    style &= ~(WS_CAPTION | WS_THICKFRAME | WS_SYSMENU | WS_MINIMIZEBOX | WS_MAXIMIZEBOX);
    SetWindowLongPtrW(hwnd, GWL_STYLE, style);
    win->applyShadow();
} else {
    LONG_PTR style = GetWindowLongPtrW(hwnd, GWL_STYLE);
    style |= WS_CAPTION | WS_THICKFRAME | WS_SYSMENU | WS_MINIMIZEBOX | WS_MAXIMIZEBOX;
    SetWindowLongPtrW(hwnd, GWL_STYLE, style);
    // 清除 DWM 扩展边框（margins=0）
    DwmExtendFrameIntoClientArea(hwnd, &margins{0,0,0,0});
}
// 强制非客户区立即重算
SetWindowPos(hwnd, nullptr, 0, 0, 0, 0,
             SWP_FRAMECHANGED | SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE);
#endif
```

### 教訓です
- **ウィンドウスタイルを変更するには、Win32スタイルビットを直接操作する必要があります。**QtのsetWindowFlagは表示されたウィンドウに対して信頼性がありません。
- **DWM拡張フレームは「状態あり」です**——設定後は明示的にクリア(margins=0)しないと永久に残ります。
- 修正後検証方式:GetWindowLongPtrW(GWL_STYLE)ポイント、パターンビット変化を確認します。
(です)`0x86CE0000` WS_CAPTIONがあります。`0x86000000` ありません)。

## 2.ウィンドウの再構築後のドリフトレイアウトです

### 現象です
「枠開き」(hide/show再建ウィンドウ)を切った後、ボタンをクリックして「無反応」です——
実際にはボタン位置全体がドリフトしているのですが、ユーザーはやはり旧位置に位置しています。

### 原因です
hide/showの再建後です**お客様エリアのサイズバリエーションです**(ネイティブ枠は非顧客領域を占有します)、
レイアウト管理机の再配置、すべてのコントロール位置下/右移働します。

### 修復します
```java
w.setFrameless(false);
w.hide(); w.show();
w.setFixedSize(1280, 720);   // 重建后强制恢复固定尺寸
```

### 教訓です
- hide/showやsetWindowFlagを再構築します**固定寸法の制約を再適用する必要があります**です。
- ユーザーがタッチスクリーンを操作するシーンでは、座標換算(DPR)により、「点不中」がより隠蔽されます。ウィンドウのサイズを確認してからボタンを疑います。

## 3. WM_NCHITTEST / WM_NCCALCSIZE / WM_GETMINMAXINFOの3点セットです

フレームレスのウィンドウはホットゾーンをカスタマイズしてキーメッセージをスケーリングします(JQtが実装され、設計上のポイントが記録されます):

- **WM_NCHITTEST**:手働でスケーリングホットゾーン(qframelesswindow)を実現します。タイトル欄の空白です
(トップ40論理px、右ボタンエリアを避けて~150px)戻るHTCAPTION→システムオンリードラッグを通します。
DPIに注意します:**メッセージ座標は物理ピクセル、Qt座標は論理ピクセル、必要/dpr換算です**です。
- **WM_NCCALCSIZE**:枠がない場合は0に戻り、お客様エリアは敷き詰められます(枠が位置を取らないようにシステム)。
- **WM_GETMINMAXINFO**:ディスプレイの作業領域に制約を最大化します(ベゼルレスウィンドウはデフォルトでタスクバーを覆う)。
注意します `_WIN32_WINNT 0x0A00` 後GCC/llvm-mingwの宣言は一致します(UINTに戻ります)。

## 4.タッチ→マウス合成(JQtPointerFilter)です。

Windowsタッチ(WM_POINTER*) WM_LBUTTONDOWN/UP/MOUSEMOVEを合成する必要がありますQtに与える:

- 全般qabstractnativeeventfilter覆われ、すべてqt最上層の窓口(qcombobox弾層を含む)。
- POINTERDOWN→PostMessage(WM_LBUTTONDOWN)です。POINTERUP→WM_LBUTTONUPです。
UPDATEキー付きステータス(g_pointerPressed)です。
- タイトルバー領域をタッチして押す→合成しないで、システムに処理させます(HTCAPTION原生ドラッグチェーンと手)。
- 座標はScreenToClient物理画素で、Qt内部はDPRで換算します——**物理・論理座標の混用に注意して検証します。**です。

## 5.スクリーンキーボード(TabTip)です。

Qt framelessウィンドウの既知の欠陥:フレームレスのウィンドウはフォーカス時に自動的にスクリーンキーボードを弾きません。
JQtはフォーカス/非フォーカス時にToggle TabTipを明示します。`jqtToggleTabTip`)、タッチデバイス必須です。

## 6.クラッシュログと異常コードです

- Windows SEH未処理例外→ `SetUnhandledExceptionFilter` 書きます `jqt-crash.log`
(時間/例外コード/アドレス/スレッド)をシステムに渡し続けます。
- **アウトプットコード-1排査です**: JVM通常のログアウトは0です。-1通常はアウトクリーンアップ段階でネイティブクラッシュです
(QApplicationデストラクション後にタイマーバックがトリガされます)。診断手段です。
1. onClose打点は正常な退出経路を行ったことを確認します;
2. JVM shutdown hook打点確認main正常終了します;
3. 2行欠けた場合→mainが戻る前にJVMがクラッシュします。
- Galleryの教訓:scheduleGeoは1秒間にスケジューリングを再帰します。
"QBasicTimer destroyed"→使います `volatile boolean appRunning` onCloseで再帰を停止します。

## 7.プラットフォームの違い(クロスプラットフォーム設計)です。

- **macOS / Windows ARM64**Qt公式ビルドOpenGLWidgetsモジュールを含みません
(AppleがOpenGLを放棄します)→コンストラクションドロップ `UnsupportedOperationException`APIは存在しますがダウングレードします。
- **macOS**: setdockbadge / cleardockbadge (nsdocktile)、setmactitlebartransparent、
setmacfullsizecontentview。show()の前に呼び出すことをお勧めします。
- **Linux**: xdg autostart、d−バスinhibit (org . freedesktop . screensaver)。
- グローバルホットキー:Windows WM_HOTKEY配布(jqtDispatchHotkey);LinuxはlibX11に依存しています
(ウェイランド限定)→v0.7.x候補です。

