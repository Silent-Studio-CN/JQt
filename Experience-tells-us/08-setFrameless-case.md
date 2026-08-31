# 08 · setFrameless 修复全记录（native 排查实战范例）

> 这是 JQt 开发中最典型的一次 native 层 bug 排查+修复全流程，
> 从用户反馈到根治发布，完整还原方法论。可作为后续排查的模板。

## 时间线

1. **用户反馈**："边框热更新没问题，但是要先点开，再关闭，再打开才显示"
2. **Gallery 层 workaround**：hide/show 强制重建（第一次尝试，治标）
3. **用户再测**："多按几次没反应，一定要关一次再开，排查JQt问题" —— 要求根治
4. **读 native 源码**定位：nativeSetFrameless 只用 setWindowFlag
5. **修复**：Win32 样式位 + DWM 清除 + SWP_FRAMECHANGED
6. **验证**：FrameProbe + GetWindowLongPtrW 样式位打点
7. **发布**：编译 jqt.dll → 部署 → 自动演示全绿 → commit/push

## 源码定位（jqt_bridge.cpp）

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

## 疑点排查过程（排除法）

| 假设 | 验证 | 结论 |
|------|------|------|
| QSpinBox 等 API 不存在 | javap | 无关 |
| GL 控件崩溃 | GlCrashProbe | 无关 |
| 定时器递归崩溃 | TimerProbe | 无关 |
| 窗口被拉大 | SizeProbe（resize 被约束） | 无关 |
| setWindowFlag 不更新 HWND | **GetWindowLongPtrW 打点：样式位不变** | **确诊** |

## 根治修复

```cpp
// 切无边框：清除样式位
style &= ~(WS_CAPTION | WS_THICKFRAME | WS_SYSMENU | WS_MINIMIZEBOX | WS_MAXIMIZEBOX);
// 切原生边框：恢复样式位 + 清 DWM 扩展 + 强制重算
style |= WS_CAPTION | WS_THICKFRAME | WS_SYSMENU | WS_MINIMIZEBOX | WS_MAXIMIZEBOX;
DwmExtendFrameIntoClientArea(hwnd, &margins{0,0,0,0});
SetWindowPos(hwnd, nullptr, 0,0,0,0,
             SWP_FRAMECHANGED | SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE);
```

## 验证证据（样式位日志）

```
[JQt] setFrameless(1) before style=0x860B0000
[JQt] setFrameless(1) after  style=0x86000000    // 边框位清除 ✓
[JQt] setFrameless(0) before style=0x86CE0000
[JQt] setFrameless(0) after  style=0x86CF0000    // WS_CAPTION 置位 ✓ 第一次就生效
[JQt] setFrameless(1) before style=0x860A0000
[JQt] setFrameless(1) after  style=0x86000000
```

## 派生问题与连环修复

修复 native 后，用户测试又发现：

1. **切原生边框后按钮点击落空** → hide/show 重建窗口后布局漂移
   → 重建后重新 setFixedSize(1280,720)。
2. **exited (code=-1)** → scheduleGeo 递归调度在退出后触发
   → appRunning 标志 + onClose/shutdown 诊断日志。
3. **切主题黑残留**（分币必赚项目）→ 内联样式/控件级样式/硬编码三件套
   → 全部变量化 + 模板渲染（见 02 章）。

## 方法论总结

1. **先读 native 源码再猜**——setWindowFlag 的语义错误，读代码 10 分钟确诊。
2. **用最小探针 + 客观信号（样式位/色值/计数）验证**，不靠肉眼看。
3. **Gallery workaround 只是临时止血**，用户要求根治时立刻转 native。
4. **修完 native 要同步重编 dll + 部署所有位置 + 哈希校验**。
5. **一个 bug 往往带出连环 bug**（重建漂移、退出定时器）——修复后
   完整回归自动演示，别只验证单点。
