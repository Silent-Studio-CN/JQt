# 07 · 探针测试方法论（复现与验证 native 问题）

> JQt native 层问题（崩溃/样式/坐标）在开发环境难复现时的系统排查方法。
> 核心思路：**最小探针 + 时间轴调度 + 外部可观测信号**。

## 为什么需要探针

- 开发环境与真实用户环境隔离（DSH 后台 vs 交互桌面），EnumWindows/
  FindWindow 可能拿不到窗口。
- 自动演示（-Dg.auto=1）只覆盖"点击路径"，覆盖不到"重建后交互"、
  "模态框关闭"、"退出清理"等边界。
- 用户现场才能复现的 bug（exit -1），需要可带回现场的诊断探针。

## 1. 最小探针模板（Java + schedule 时间轴）

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

要点：每步 System.out 打点；结束显式 quit；Start-Process 重定向 stdout/stderr
到文件；超时判断是否卡死。

## 2. 已用探针清单（可复用）

| 探针 | 验证内容 | 结论 |
|------|---------|------|
| FrameProbe | setFrameless 热切换是否生效 | 样式位 0x86CE0000（有边框）✓ |
| SizeProbe | 原生边框模式下 setFixedSize 是否约束 | resize(1200,900) 被弹回 800x500 ✓ |
| GlCrashProbe | 窗口重建后 QOpenGLWidget update/close | 正常（排除 GL 崩溃） |
| TimerProbe | scheduleGeo 递归 + close 是否崩溃 | 正常（DSH 下不复现 -1） |
| ModalProbe | QDialog.exec + reject + getText 关闭路径 | 正常退出 0 |
| FullProbe | 全分区切换 + onClose 路径 | 正常退出 0 |

教训：**全部探针都返回 0，唯独用户现场 -1** —— 说明差异在环境
（触摸屏合成事件 + 真实交互序列），此时用诊断日志（onClose/shutdown hook）
带到现场定位，而不是继续猜。

## 3. 外部观测手段

- **Win32 样式位**：GetWindowLongPtrW(GWL_STYLE) 打点（native 层 fprintf），
  验证 WS_CAPTION/WS_THICKFRAME 是否真的变化 —— 最客观。
- **窗口枚举**：EnumWindows + GetWindowThreadProcessId 按 pid 过滤，
  但**受 desktop 隔离影响**（后台进程的窗口在另一个 desktop 看不到）。
- **GetWindowRect vs GetClientRect**：原生边框窗口两者不同，无边框相同。
- **截图**：CopyFromScreen（同样受 desktop 隔离限制）。
- **JVM 诊断**：onClose 打点 + Runtime.addShutdownHook 打点，
  判断退出路径是正常还是崩溃。

## 4. 网络受限环境的下载策略（与 JQt 开发相关部分）

- GitHub release 大文件（zip 16-20MB）用 IWR 重试循环（10-12 次，
  间隔 8s），中途可能断点续传式增长。
- 只取 zip 中单个 dll 时用 Range 请求 + zlib inflate（本地解析 central
  directory），避免下载整个包。
- 本地仓库 dist/ 通常已有最新构建产物（jar/dll），优先复制本地。

## 5. 回归纪律

1. 每个修复后跑**完整自动演示**（全部分区 + 收尾切回），记 EXIT 和日志行数基线。
2. 修复不能只验证"不崩"——要验证"行为对"（样式位、渲染色值、回调计数）。
3. 部署后哈希校验三处一致性（jar/dll/源码）。
4. 发布前先 push 到本地仓库再推 GitHub（网络抖动常态，push 重试最多 8 次）。
