# 08・setFrameless修復全記録(native排尋実戦例)

> これはJQt開発の中で最も典型的な1回のnative層のバグチェック+修復の全プロセスです。
> ユーザーからのフィードバックから抜本的なリリースまで、方法論を完全に再現します。その後の洗い出しのテンプレートにもなります。

## タイムラインです

1. **ユーザーからのフィードバックです**: "ベゼルの更新は問題ありませんが、開いて、閉じて、開いてから表示されます。"
2. **Galleryフロアworkaroundです**: hide/show強制再建(初の試み、対症療法)です。
3. **ユーザー再測定です**:「何回押しても反応がない、必ず一回閉めてから開けて、JQt問題を調べます」——
4. **ネイティブソースを読むのです**ポジショニング:nativeSetFramelessはsetWindowFlagだけです
5. **修復します**: Win32スタイルビット+ DWMクリア+ SWP_FRAMECHANGEDです。
6. **検証します**FrameProbe + GetWindowLongPtrW仕様ビット打点です。
7. **頒布します**:編訳jqt.dll→部署→自働演演全緑→commit/push

## ソースコード位置(jqt_bridge.cpp)です

```cpp
// JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeSetFrameless(...)
win->frameless = (on == JNI_TRUE);
if (win->frameless) {
    win->setWindowFlag(Qt::FramelessWindowHint, true);
    win->applyShadow();
} else {
    win->setWindowFlag(Qt::FramelessWindowHint, false);
}
win->show();
```

## 疑問点洗い出しプロセス(消去法)です。

| 仮説です | 検証します | 結論です 
|------|------|------|
| QSpinBoxなどのAPIは存在しません | javapです | 関係ありません 
| GLコントロールクラッシュです | GlCrashProbeです | 関係ありません 
| タイマー再帰クラッシュです | TimerProbeです | 関係ありません 
| 窓が大きく開けられました | SizeProbe (resizeが拘束されます) | 関係ありません 
| setWindowFlagはHWNDを更新しません。 | **GetWindowLongPtrW打点:パターンビット不変です** | **診断します** 

## 根治修復です

```cpp
// 切无边框：清除样式位
style &= ~(WS_CAPTION | WS_THICKFRAME | WS_SYSMENU | WS_MINIMIZEBOX | WS_MAXIMIZEBOX);
// 切原生边框：恢复样式位 + 清 DWM 扩展 + 强制重算
style |= WS_CAPTION | WS_THICKFRAME | WS_SYSMENU | WS_MINIMIZEBOX | WS_MAXIMIZEBOX;
DwmExtendFrameIntoClientArea(hwnd, &margins{0,0,0,0});
SetWindowPos(hwnd, nullptr, 0,0,0,0,
             SWP_FRAMECHANGED | SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE);
```

## 証拠の検証(スタイルビットログ)です。

```
[JQt] setFrameless(1) before style=0x860B0000
[JQt] setFrameless(1) after  style=0x86000000    // 边框位清除 ✓
[JQt] setFrameless(0) before style=0x86CE0000
[JQt] setFrameless(0) after  style=0x86CF0000    // WS_CAPTION 置位 ✓ 第一次就生效
[JQt] setFrameless(1) before style=0x860A0000
[JQt] setFrameless(1) after  style=0x86000000
```

## 派生問題と連鎖修復です

ネイティブを修正した後、ユーザーテストで次のことがわかりました。

1. **枠を切った後ボタンをクリックします** →hide/show再建ウィンドウ後のレイアウトドリフトです
→再構築後再びsetFixedSize(1280,720)にします。
2. **exited (code=-1)** →scheduleGeo再帰スケジュールはログアウト後にトリガします。
→appRunningフラグ+ onClose/shutdown診断ログです。
3. **テーマを切って黒が残ります**(分幣必得アイテム)→インライン仕様/コントロールレベル仕様/ハードコードの3点セットです
→全部変量子化+テンプレート描画(02章参照)です。

## 方法論のまとめです

1. **ネイティブのソースコードを読んでから当てます**——setWindowFlagのセマンティックエラーを10分で診断します。
2. **最小プローブ+客観信号(スタイルビット/色値/カウント)で検証します。**目では見ません。
3. **Gallery workaroundは一時的な止血です**ユーザーから抜本的な治療を求められたら、すぐにネイティブに移行します。
4. **nativeを修得して同期してdllを再編成します+すべての位置を配置します+ハッシュチェック**です。
5. **一つのバグがバグの連鎖を生みます**(再建ドリフト、アウトタイマー)——修復後です。
完全回帰自動プレゼン、1点だけ検証しないことです。

