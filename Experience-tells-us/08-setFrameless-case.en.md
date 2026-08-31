# 08 · setFrameless Fix Complete Record (native Troubleshooting Practical Example)

> This is the most typical full process of bug detection and repair at the native layer in JQt development
> From user feedback to the release of the radical solution, the methodology is fully restored. It can be used as a template for subsequent investigations.

## Timeline

1. **User feedback**"Border hot update is fine, but you need to open it first, then close it, and then open it again before it shows up."
2. **Gallery Layer workaround**"hide/show Forced Reconstruction (First Attempt, Treating Symptoms)
3. **User retest**"Press several times but there's no response. You must turn it off once and then turn it on again to check for JQt issues." - Demand a radical solution
4. **Read native source code**Positioning: nativeSetFrameless only uses setWindowFlag
5. **"Repair**Win32 style bit + DWM clear + SWP_FRAMECHANGED
6. **Verification**: FrameProbe + GetWindowLongPtrW style bit dot
7. **"Publish**Compile jqt.dll → Deploy → fully green automatic demonstration → commit/push

## Source code Location (jqt_bridge.cpp)

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

## The process of troubleshooting suspicious points (elimination method)

| Hypothesis | Verification | Conclusion 
|------|------|------|
| Apis such as QSpinBox do not exist | javap | Irrelevant 
| The GL control crashed. | GlCrashProbe | Irrelevant 
| The timer recursively crashed | TimerProbe | Irrelevant 
| The window has been pulled wider | SizeProbe (resize is constrained) | Irrelevant 
| setWindowFlag does not update HWND | **GetWindowLongPtrW dot: The style bit remains unchanged** | **Confirmed diagnosis** 

## Radical repair

```cpp
// 切无边框：清除样式位
style &= ~(WS_CAPTION | WS_THICKFRAME | WS_SYSMENU | WS_MINIMIZEBOX | WS_MAXIMIZEBOX);
// 切原生边框：恢复样式位 + 清 DWM 扩展 + 强制重算
style |= WS_CAPTION | WS_THICKFRAME | WS_SYSMENU | WS_MINIMIZEBOX | WS_MAXIMIZEBOX;
DwmExtendFrameIntoClientArea(hwnd, &margins{0,0,0,0});
SetWindowPos(hwnd, nullptr, 0,0,0,0,
             SWP_FRAMECHANGED | SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE);
```

## Verify evidence (style bit log)

```
[JQt] setFrameless(1) before style=0x860B0000
[JQt] setFrameless(1) after  style=0x86000000    // 边框位清除 ✓
[JQt] setFrameless(0) before style=0x86CE0000
[JQt] setFrameless(0) after  style=0x86CF0000    // WS_CAPTION 置位 ✓ 第一次就生效
[JQt] setFrameless(1) before style=0x860A0000
[JQt] setFrameless(1) after  style=0x86000000
```

## Derivative problems and chain fixes

After fixing native, user testing also found that

1. **After cutting the native border, the button failed to click** The layout drifts after the hide/show reconstruction window
After reconstruction, reset the Fixedsize (1280,720).
2. **exited (code=-1)** The scheduleGeo recursive scheduling is triggered after exiting
→ appRunning flag + onClose/shutdown diagnostic log.
3. **Cut the black residue of the theme**(Penny Must-Earn Project) → Inline Style/Control-level style/Hard-coded three-piece set
→ All quantification + template rendering (see Chapter 02).

## Summary of Methodology

1. **Read the native source code first and then guess**-- Semantic error of setWindowFlag, confirmed after reading the code for 10 minutes.
2. **Verify with the minimum probe + objective signal (pattern bit/color value/count)**It doesn't rely on the naked eye to see.
3. **The Gallery workaround is only a temporary stoppage**When the user requests a complete cure, immediately switch to native.
4. **After fixing native, you need to synchronously recompile the dll, deploy all locations, and perform hash verification**.
5. **One bug often leads to a series of bugs**(Rebuild drift, exit timer) - After repair
Complete regression automatic demonstration. Don't just verify a single point.

