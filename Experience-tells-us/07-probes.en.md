# 07 · Probe Testing Methodology (Reproducing and Verifying native Issues)

> System troubleshooting methods for JQt native layer issues (crashes/styles/coordinates) when they are difficult to reproduce in the development environment.
> Core idea**Minimum probe + timeline scheduling + external observable signal**.

## Why do we need probes?

- The development environment is isolated from the real user environment (DSH backend vs interactive desktop), EnumWindows/
FindWindow may not be able to get the window.
- The auto-demonstration (-Dg.auto=1) only covers the "click path" and does not cover the "post-reconstruction interaction".
Boundaries such as "Modal box closed" and "Exit Cleanup".
- Bugs (exit-1) that can only be reproduced on-site require diagnostic probes that can be brought back to the site.

## Minimum probe template (Java + schedule timeline)

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

Key point: Make dots at each System.out step. End explicit quit; Start-Process redirects stdout/stderr
To the file; Determine if it is stuck due to timeout.

## 2. List of Used Probes (Reusable

| Probe | Verification content | Conclusion 
|------|---------|------|
| FrameProbe | Whether setFrameless hot swapping takes effect | Style bit 0x86CE0000 (with border) ✓ 
| SizeProbe | Is setFixedSize restricted in the native border mode | The resize(1200,900) is bounced back to 800x500 ✓ 
| GlCrashProbe | After the window is rebuilt, QOpenGLWidget update/close | Normal (excluding GL crash) 
| TimerProbe | Does scheduleGeo recursion + close crash | Normal (does not reproduce -1 under DSH) 
| ModalProbe | Close the path by using QDialog.exec + reject + getText | Normal exit 0 
| FullProbe | Full partition switching + onClose path | Normal exit 0 

Lesson:**All probes returned 0, except for -1 at the user site** -- This indicates that the differences lie in the environment
(Touchscreen synthesis event + real interaction sequence), at this time, use the diagnostic log (onClose/shutdown hook)
Take it to the scene for positioning instead of continuing to guess.

## 3. External observation methods

- **Win32 style bit**: GetWindowLongPtrW(GWL_STYLE) dotting (native layer fprintf)
Verify whether WS_CAPTION/WS_THICKFRAME has really changed - the most objective.
- **Window enumeration**EnumWindows + GetWindowThreadProcessId filter by pid
but**Affected by desktop isolation**(The window of the background process cannot be seen on another desktop.)
- **GetWindowRect vs GetClientRect**Native border Windows are different, while borderless ones are the same.
- **Screenshot**CopyFromScreen (also subject to desktop isolation restrictions).
- **JVM Diagnosis**: onClose dot + Runtime.addShutdownHook dot
Determine whether the exit path is normal or crashed.

## 4. Download Strategies in Network-Constrained Environments (Related to JQt Development)

- For large GitHub release files (zip 16-20MB), use the IWR retry loop (10-12 times).
There is an 8-second interval between them, and the growth may resume from a breakpoint during the process.
- When only a single dll in the zip is taken, use Range request + zlib inflate (local resolution central)
Avoid downloading the entire package (directory).
- The local repository dist/ usually already has the latest build products (jar/dll), so local replication is preferred.

## 5. Return to discipline

1. Run after each repair**Complete automatic demonstration**(All partitions + end switch back), record EXIT and the baseline number of log lines.
2. Fixes cannot merely verify "no crash" - it is necessary to verify "behavior pairs" (style bits, render color values, callback counts).
3. After deployment, hash check for consistency in three places (jar/dll/ source code).
4. Before publishing, push to the local repository first and then to GitHub (network jitter is the norm, and the push retry can be up to 8 times at most).

