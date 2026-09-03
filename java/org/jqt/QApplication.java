/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * JQt 应用入口：封装 C++ 侧的 {@code QApplication}。
 * <p>
 * 用法：
 * <pre>
 * QApplication app = new QApplication();
 * QMainWindow window = new QMainWindow("我的窗口");
 * window.show();
 * app.exec();   // 进入 Qt 事件循环（阻塞），最后一个窗口关闭后返回
 * </pre>
 */
public class QApplication {

    static {
        // 加载 native 库：jqt.dll (Windows) / libjqt.so (Linux) / libjqt.dylib (macOS)
        // Android: QtLoader dlopen 了 libjqt_arm64-v8a.so（APK 内按 ABI 后缀命名），
        // System.loadLibrary("jqt") 找不到 libjqt.so；JNI 按名匹配，无需再次加载。
        try {
            System.loadLibrary("jqt");
        } catch (UnsatisfiedLinkError e) {
            if (!isAndroidRuntime()) {
                throw e;
            }
        }
    }

    private static boolean isAndroidRuntime() {
        try {
            Class.forName("android.os.Build");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** C++ 侧 QApplication 指针。 */
    private final long nativeHandle;

    private final List<Runnable> onAboutToQuitHandlers = new ArrayList<>();

    /** 全局动画主题（动效节奏 + 默认缓动）。 */
    private static volatile JQtAnimationTheme animationTheme = JQtAnimationTheme.DEFAULT;

    /** 渲染后端（null = Qt 默认；见 {@link #rhiBackend(String)}）。 */
    private static volatile String rhiBackend = null;

    // 主题渲染状态（setAccentColor 重渲染用）
    private String themeTemplatePath;          // 非 null = 模板模式
    private java.util.Map<String, String> themeVars;
    private boolean themeLight;
    private String currentAccent;              // 自定义主题色（切换主题时保留）

    /**
     * 设置渲染后端（必须在构造 {@link QApplication} 之前调用）：
     * <ul>
     *   <li>{@code "d3d11"} — Direct3D 11（Windows 默认，最稳）</li>
     *   <li>{@code "software"} — 软件光栅（无 GPU / 远程桌面兜底）</li>
     *   <li>{@code "opengl"} / {@code "vulkan"} — 仅影响 Qt Quick；Qt Widgets
     *       在 Windows 上固定使用 D3D11（Qt 限制，请求 OpenGL/Vulkan 会被忽略并回退 D3D11）</li>
     * </ul>
     * 也可用 JVM 参数 {@code -Djqt.rhi=d3d11|opengl|vulkan|software} 设置。
     */
    public static void rhiBackend(String backend) {
        rhiBackend = (backend == null || backend.isEmpty()) ? null : backend;
    }

    /** 当前配置的渲染后端（null = 默认）。 */
    public static String rhiBackend() {
        return rhiBackend;
    }

    /**
     * 设置全局动画主题：所有 JQt 动效（hover / 入场 / 退场 / pivot 指示器）
     * 的时长按 {@link JQtAnimationTheme#speed} 缩放，默认缓动取 {@link JQtAnimationTheme#easing}。
     * <p>
     * 预设：{@link JQtAnimationTheme#DEFAULT} / {@link JQtAnimationTheme#FAST} /
     * {@link JQtAnimationTheme#RELAXED} / {@link JQtAnimationTheme#OFF}（禁用全部动效）。
     */
    public static void setAnimationTheme(JQtAnimationTheme theme) {
        animationTheme = (theme == null) ? JQtAnimationTheme.DEFAULT : theme;
        JQtAnimations.setHoverEnabled(animationTheme.enabled());
    }

    /** 当前全局动画主题。 */
    public static JQtAnimationTheme getAnimationTheme() {
        return animationTheme;
    }

    /**
     * 创建 QApplication（整个进程只能有一个）。
     * 若 JVM 参数包含 {@code -Djqt.lightMode=true}，自动切换浅色配色。
     */
    public QApplication() {
        String backend = rhiBackend;
        if (backend == null) {
            backend = System.getProperty("jqt.rhi");
        }
        instance = this;
        nativeHandle = nativeCreateApp(backend);
        if (Boolean.getBoolean("jqt.lightMode")) {
            setColorScheme(true);
        }
        // 自动应用中文字体策略：Windows 雅黑 / macOS 苹方 / Linux Noto CJK
        // 解决 Qt 默认字体（Segoe UI 等）对 CJK 字形回退不稳导致的乱码/问号
        nativeSetFont(systemFontFamily(), 13);
    }

    private native long nativeCreateApp(String rhiBackend);

    /** 跨平台中文字体族（Qt 找不到时会自动回退，不会产生问号）。 */
    private static String systemFontFamily() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return "Microsoft YaHei UI";
        }
        if (os.contains("mac")) {
            return "PingFang SC";
        }
        if (os.contains("linux")) {
            return "Noto Sans CJK SC";
        }
        return "Sans Serif";
    }

    /**
     * 设置全局字体（所有控件继承，含自绘控件如 JQtPivot/JQtSwitch）。
     * Qt 找不到指定字体族时自动回退系统字体，不会产生问号。
     */
    public void setFontFamily(String family) {
        setFontFamily(family, 13);
    }

    /** 设置全局字体（指定字号）。 */
    public void setFontFamily(String family, int pointSize) {
        nativeSetFont(family, pointSize);
    }
    private native void nativeSetFont(String family, int size);

    /** 设置全局字体（Qt API 名，等同 setFontFamily）。 */
    public void setFont(String family, int pointSize) {
        nativeSetFont(family, pointSize);
    }

    /** 当前全局字体，返回 "Family,size"。 */
    public String font() { return nativeFont(); }
    private static native String nativeFont();

    /**
     * 进入 Qt 事件循环（阻塞调用）。
     * 当最后一个窗口关闭时返回（对应 Qt 的 quitOnLastWindowClosed）。
     */
    public native void exec();

    /** 退出事件循环。 */
    public native void quit();

    /** 播放系统提示音（beep）。 */
    public static void beep() {
        nativeBeep();
    }
    private static native void nativeBeep();

    /** 提醒窗口（任务栏闪烁，ms 毫秒；0 = 无限直到窗口激活）。 */
    public static void alert(QMainWindow window, int ms) {
        nativeAlert(window.nativeHandle(), ms);
    }
    private static native void nativeAlert(long winHandle, int ms);

    /** 当前全局样式表（QSS）。 */
    public String styleSheet() {
        return nativeStyleSheet();
    }
    private static native String nativeStyleSheet();

    /** 全局背景色（0xAARRGGBB；简化形态的 palette 查询）。 */
    public static int palette() { return nativePalette(); }
    private static native int nativePalette();

    /** 全局正文颜色（QPalette::Text，0xAARRGGBB；v0.7.1 L1 补全）。 */
    public static int paletteText() { return nativePaletteText(); }
    private static native int nativePaletteText();

    /** 全局占位提示颜色（QPalette::PlaceholderText，0xAARRGGBB；v0.7.1 L1 补全）。 */
    public static int palettePlaceholderText() { return nativePalettePlaceholderText(); }
    private static native int nativePalettePlaceholderText();

    /**
     * 开机自启（跨平台统一 API）。
     * <ul>
     *   <li>Windows — 写入当前用户 Run 注册表（HKCU\...\Run）</li>
     *   <li>macOS — 生成 LaunchAgent plist（~/Library/LaunchAgents/com.silentstudio.&lt;应用名&gt;.plist）</li>
     *   <li>Linux — 生成 XDG autostart 项（~/.config/autostart/&lt;应用名&gt;.desktop）</li>
     * </ul>
     * @param exePath 应用可执行文件路径；Windows 必填，macOS/Linux 传 null 时自动取当前可执行文件
     * @return 是否成功
     */
    public static boolean setAutoStart(boolean enable, String exePath) {
        return nativeSetAutoStart(enable, exePath);
    }
    private static native boolean nativeSetAutoStart(boolean enable, String exePath);

    /**
     * 阻止系统休眠/息屏（跨平台统一 API，防息屏场景：会议、演示、直播、下载）。
     * <ul>
     *   <li>Windows — SetThreadExecutionState（阻止系统睡眠 + 关闭显示器）</li>
     *   <li>macOS — NSProcessInfo idleSystemSleepDisabled（阻止系统休眠）</li>
     *   <li>Linux — org.freedesktop.ScreenSaver Inhibit（D-Bus；GNOME/KDE 均支持，
     *       失败时回退 org.gnome.SessionManager Inhibit）</li>
     * </ul>
     * 调用 {@code preventSleep(false)} 恢复系统默认策略。
     * @return 是否成功（Linux 无 D-Bus 会话总线或桌面未实现 Inhibit 时返回 false）
     */
    public static boolean preventSleep(boolean on) {
        return nativePreventSleep(on);
    }
    private static native boolean nativePreventSleep(boolean on);

    /**
     * 发送桌面通知（跨平台统一 API）。
     * <ul>
     *   <li>Linux — org.freedesktop.Notifications（D-Bus，GNOME/KDE 通知中心）</li>
     *   <li>Windows — 托盘气泡（QSystemTrayIcon::showMessage，零依赖方案）</li>
     *   <li>macOS — NSUserNotification（通知中心；Apple 已弃用但可用，无需权限弹窗）</li>
     * </ul>
     * @param title 标题
     * @param body 正文
     * @param timeoutMs 显示时长（毫秒；≤0 用平台默认值；Linux 通知服务可能自行调整）
     * @return 是否成功送达（Linux 无通知服务时返回 false）
     */
    public static boolean showNotification(String title, String body, int timeoutMs) {
        return nativeShowNotification(title, body, timeoutMs);
    }
    private static native boolean nativeShowNotification(String title, String body, int timeoutMs);

    /** 延迟 {@code ms} 毫秒后自动退出事件循环（演示 / 自动化测试用）。 */
    public native void scheduleQuit(long ms);

    /**
     * 在 {@code delayMs} 毫秒后、于 Qt GUI 线程执行任务
     * （内部使用 Qt 定时器，线程安全，可在任意线程调用）。
     */
    public void schedule(Runnable task, long delayMs) {
        nativeSchedule(task, delayMs);
    }
    private native void nativeSchedule(Runnable task, long delayMs);

    /**
     * 在 Qt GUI 线程执行任务（立即排队；线程安全，可在任意线程调用）。
     * 用于后台线程完成后回到 UI 线程更新控件——JQt 控件非线程安全，
     * 后台线程严禁直接操作控件，必须经此回到 GUI 线程。
     * <pre>
     * executor.execute(() -> {
     *     String data = loadRemote();          // 后台线程
     *     QApplication.runOnUiThread(() -> {   // 回 UI 线程
     *         label.setText(data);
     *     });
     * });
     * </pre>
     */
    public static void runOnUiThread(Runnable task) {
        appInstance().schedule(task, 0);
    }

    /**
     * 立即在 Qt GUI 线程执行任务（静态入口，无需先创建 QApplication 实例）。
     * Android：main() 完成 QApplication attach 后可用；就绪判断用 {@link #isQtReady()}。
     */
    public static native void runOnQtThread(Runnable task);

    /** Qt 运行时是否就绪（进程级 QApplication 已创建并 attach；Android main() 之后为 true）。 */
    public static native boolean isQtReady();

    /** 单例访问（runOnUiThread 静态入口用）。 */
    private static volatile QApplication instance;

    private static QApplication appInstance() {
        if (instance == null) {
            throw new IllegalStateException("QApplication 尚未创建");
        }
        return instance;
    }

    // ==================== 主题系统 ====================

    /**
     * 切换配色方案（运行时生效，立即刷新全部控件）。
     * {@code true} 浅色 / {@code false} 深色。
     * <p>
     * ⚠️ 层级规则：QSS 样式 > 调色板 > 风格引擎。QSS 覆盖到的属性优先于调色板；
     * 未覆盖的控件/属性使用调色板。建议用 {@link #setTheme(String)} 统一应用主题
     * （QSS + 调色板一致打包），避免手动混搭冲突。
     */
    public void setColorScheme(boolean light) {
        nativeSetColorScheme(light);
    }
    private native void nativeSetColorScheme(boolean light);

    /**
     * 设置全局样式表（QSS，Qt Style Sheets）。
     * 语法详见 Qt 文档 "Qt Style Sheets Reference"。
     */
    public void setStyleSheet(String qss) {
        nativeSetStyleSheet(qss);
    }
    private native void nativeSetStyleSheet(String qss);

    /**
     * 切换控件风格（QApplication::setStyle）。
     * 常见值：{@code "Fusion"}（经典 Qt 扁平风）、{@code "Windows"}、{@code "macOS"}。
     */
    public void setStyle(String style) {
        nativeSetStyle(style);
    }
    private native void nativeSetStyle(String style);

    /**
     * 应用主题（统一入口，QSS + 调色板一致打包，避免混搭冲突）。
     * 内置主题：{@code "fluent-dark"}（Fluent 深色）、{@code "fluent-light"}（Fluent 浅色）。
     * <p>
     * 自定义主题：{@code setTheme("themes/my.qss", true)} —— 加载 QSS 文件并指定配色。
     * 注意：第三方 QSS 由使用者自行负责其许可。
     */
    public void setTheme(String name) {
        switch (name == null ? "" : name) {
            case "fluent-dark":
                setTheme("themes/fluent.qss.tpl", FLUENT_DARK, false);
                break;
            case "fluent-light":
                setTheme("themes/fluent.qss.tpl", FLUENT_LIGHT, true);
                break;
            default:
                throw new IllegalArgumentException("未知主题: " + name + "（内置: fluent-dark / fluent-light）");
        }
    }

    // ==================== QSS 模板变量系统 ====================

    /** Fluent 深色变量集（模板 themes/fluent.qss.tpl + 本变量集 = fluent-dark）。 */
    public static final java.util.Map<String, String> FLUENT_DARK = java.util.Map.ofEntries(
        java.util.Map.entry("win-bg", "#1f1f1f"),
        java.util.Map.entry("fg", "#e8e8e8"),
        java.util.Map.entry("fg-strong", "#ffffff"),
        java.util.Map.entry("fg-hint", "#9a9a9a"),
        java.util.Map.entry("fg-disabled", "#6a6a6a"),
        java.util.Map.entry("card-bg", "#2b2b2b"),
        java.util.Map.entry("card-border", "#3a3a3a"),
        java.util.Map.entry("btn-bg", "#3b3b3b"),
        java.util.Map.entry("btn-fg", "#ffffff"),
        java.util.Map.entry("btn-hover", "#484848"),
        java.util.Map.entry("btn-pressed", "#2e2e2e"),
        java.util.Map.entry("btn-disabled", "#2a2a2a"),
        java.util.Map.entry("accent", "#4cc2ff"),
        java.util.Map.entry("accent-hover", "#5acbff"),
        java.util.Map.entry("accent-fg", "#ffffff"),
        java.util.Map.entry("switch-off", "#4a4a4a"),
        java.util.Map.entry("switch-off-hover", "#555555"),
        java.util.Map.entry("nav-fg", "#d8d8d8"),
        java.util.Map.entry("nav-hover", "#2b2b2b"),
        java.util.Map.entry("nav-selected", "#333333"),
        java.util.Map.entry("input-bg", "#2b2b2b"),
        java.util.Map.entry("input-border", "#3a3a3a"),
        java.util.Map.entry("titlebar-hover", "#3a3a3a"),
        java.util.Map.entry("titlebar-pressed", "#2e2e2e")
    );

    /** Fluent 浅色变量集。 */
    public static final java.util.Map<String, String> FLUENT_LIGHT = java.util.Map.ofEntries(
        java.util.Map.entry("win-bg", "#f3f3f3"),
        java.util.Map.entry("fg", "#1f1f1f"),
        java.util.Map.entry("fg-strong", "#000000"),
        java.util.Map.entry("fg-hint", "#6a6a6a"),
        java.util.Map.entry("fg-disabled", "#9a9a9a"),
        java.util.Map.entry("card-bg", "#ffffff"),
        java.util.Map.entry("card-border", "#e0e0e0"),
        java.util.Map.entry("btn-bg", "#f0f0f0"),
        java.util.Map.entry("btn-fg", "#1f1f1f"),
        java.util.Map.entry("btn-hover", "#e5e5e5"),
        java.util.Map.entry("btn-pressed", "#d8d8d8"),
        java.util.Map.entry("btn-disabled", "#f5f5f5"),
        java.util.Map.entry("accent", "#0078d4"),
        java.util.Map.entry("accent-hover", "#1a86d8"),
        java.util.Map.entry("accent-fg", "#ffffff"),
        java.util.Map.entry("switch-off", "#c8c8c8"),
        java.util.Map.entry("switch-off-hover", "#b8b8b8"),
        java.util.Map.entry("nav-fg", "#333333"),
        java.util.Map.entry("nav-hover", "#ececec"),
        java.util.Map.entry("nav-selected", "#e0e0e0"),
        java.util.Map.entry("input-bg", "#ffffff"),
        java.util.Map.entry("input-border", "#d0d0d0"),
        java.util.Map.entry("titlebar-hover", "#e0e0e0"),
        java.util.Map.entry("titlebar-pressed", "#d0d0d0")
    );

    /**
     * 用 QSS 模板 + 变量集渲染主题（一套模板，任意主题色）。
     * 模板内 {@code %变量名%} 占位符会被替换为变量值。
     */
    public void setTheme(String qssTemplatePath, java.util.Map<String, String> variables) {
        setTheme(qssTemplatePath, variables, true);
    }

    /** 模板 + 变量集 + 配色方案（light/dark palette）。 */
    public void setTheme(String qssTemplatePath, java.util.Map<String, String> variables, boolean light) {
        themeTemplatePath = qssTemplatePath;
        themeVars = (variables == null) ? null : new java.util.HashMap<>(variables);
        themeLight = light;
        renderTheme();
    }

    /** 重新渲染当前模板主题（变量替换 + 应用；自定义主题色在主题切换后保留）。 */
    private void renderTheme() {
        if (themeTemplatePath == null || themeVars == null) {
            return;
        }
        try {
            // 重放自定义 accent（setTheme 切换深浅色后不丢失）
            if (currentAccent != null) {
                java.util.Map<String, String> vars = new java.util.HashMap<>(themeVars);
                vars.put("accent", currentAccent);
                vars.put("accent-hover", lighten(currentAccent, 0.12));
                themeVars = vars;
            }
            String qss = new String(Files.readAllBytes(Paths.get(themeTemplatePath)), StandardCharsets.UTF_8);
            for (java.util.Map.Entry<String, String> e : themeVars.entrySet()) {
                qss = qss.replace("%" + e.getKey() + "%", e.getValue());
            }
            setColorScheme(themeLight);
            setStyleSheet(qss);
            if (currentAccent != null) {
                nativeSetAccent(currentAccent);   // 调色板 Highlight 重放
            }
        } catch (java.io.IOException e) {
            // 主题模板缺失：降级到调色板模式而非崩溃（与 Qt 一致：资源缺失回退默认样式）
            System.err.println("[JQt] 警告: 主题模板缺失（" + themeTemplatePath + "），回退默认配色");
            setColorScheme(themeLight);
        }
    }

    /**
     * 切换全局主题色（强调色，如 {@code "#4cc2ff"}）。
     * <ul>
     *   <li>模板主题（setTheme 内置 fluent-* 或模板模式）：QSS 重渲染，accent/accent-hover 跟随</li>
     *   <li>所有模式：QPalette::Highlight 更新（Pivot 指示器 / 选中态 / 输入框光标跟随）</li>
     *   <li>自绘控件：JQtSwitch 轨道开色跟随</li>
     * </ul>
     * 纯 QSS 文件主题（setTheme(path, light)）只更新调色板与自绘控件，QSS 内硬编码色不受影响。
     */
    public void setAccentColor(String hex) {
        if (hex == null || !hex.matches("#[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("主题色需为 #RRGGBB 格式: " + hex);
        }
        currentAccent = hex.toLowerCase();
        nativeSetAccent(currentAccent);
        if (themeVars != null) {
            java.util.Map<String, String> vars = new java.util.HashMap<>(themeVars);
            vars.put("accent", currentAccent);
            vars.put("accent-hover", lighten(currentAccent, 0.12));
            themeVars = vars;
            renderTheme();
        }
    }
    private native void nativeSetAccent(String hex);

    /**
     * 自动跟随系统主题（Windows）：深浅色 + 系统强调色，变化时自动应用。
     * 工业场景零配置：开启后无需任何手动主题切换代码。
     * 非 Windows 平台为 no-op。
     */
    public void setAutoTheme(boolean on) {
        nativeSetAutoTheme(on);
    }
    private native void nativeSetAutoTheme(boolean on);

    /** 由 C++ 侧在系统主题变化时回调（JNI，GUI 线程）。 */
    void nativeHandleSystemTheme(boolean light, String accentHex) {
        System.out.println("[JQt] auto theme: " + (light ? "light" : "dark") + " accent=" + accentHex);
        setTheme(light ? "fluent-light" : "fluent-dark");
        if (accentHex != null && !accentHex.isEmpty()) {
            setAccentColor(accentHex);
        }
    }

    /** 颜色向白偏移（hover 亮色用）。 */
    private static String lighten(String hex, double factor) {
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);
        r = (int) (r + (255 - r) * factor);
        g = (int) (g + (255 - g) * factor);
        b = (int) (b + (255 - b) * factor);
        return String.format("#%02x%02x%02x", r, g, b);
    }

    /** 自定义主题：加载 QSS 文件 + 指定浅色/深色调色板。 */
    public void setTheme(String qssPath, boolean light) {
        applyThemeFile(qssPath, light);
    }

    private void applyThemeFile(String path, boolean light) {
        themeTemplatePath = null;   // 纯文件模式：setAccentColor 只影响调色板/自绘控件
        themeVars = null;
        try {
            Path p = Paths.get(path);
            if (!Files.exists(p)) {
                throw new IllegalArgumentException("主题文件不存在: " + path);
            }
            String qss = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
            setColorScheme(light);
            setStyleSheet(qss);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("读取主题失败: " + path, e);
        }
    }

    /**
     * 注册退出前回调（对应 Qt 的 aboutToQuit 信号）。
     * 事件循环结束前触发。
     */
    public QApplication onAboutToQuit(Runnable handler) {
        onAboutToQuitHandlers.add(handler);
        return this;
    }

    /** 由 C++ 侧在应用退出前回调（JNI）。 */
    void nativeHandleAboutToQuit() {
        for (Runnable h : onAboutToQuitHandlers) {
            h.run();
        }
    }
    // ---- 值对象批：全局光标与窗口查询 ----

    /** 设置全局覆盖光标。 */
    public static void setOverrideCursor(QCursor cursor) {
        if (cursor != null) nativeSetOverrideCursor(cursor.shape().value);
    }
    private static native void nativeSetOverrideCursor(int shape);

    /** 变更全局覆盖光标（不重复计数）。 */
    public static void changeOverrideCursor(QCursor cursor) {
        if (cursor != null) nativeChangeOverrideCursor(cursor.shape().value);
    }
    private static native void nativeChangeOverrideCursor(int shape);

    /** 恢复上一个覆盖光标。 */
    public static void restoreOverrideCursor() { nativeRestoreOverrideCursor(); }
    private static native void nativeRestoreOverrideCursor();

    /** 全局坐标处的控件（返回句柄；0=无）。 */
    public static long widgetAt(QPoint pos) {
        return nativeWidgetAt(pos != null ? pos.x() : 0, pos != null ? pos.y() : 0);
    }
    private static native long nativeWidgetAt(int x, int y);

    /** 全局坐标处的最顶层窗口（返回句柄；0=无）。 */
    public static long topLevelAt(QPoint pos) {
        return nativeTopLevelAt(pos != null ? pos.x() : 0, pos != null ? pos.y() : 0);
    }
    private static native long nativeTopLevelAt(int x, int y);

    /** 全局坐标处的屏幕（返回 QScreen 句柄；0=无）。 */
    public static long screenAt(QPoint pos) {
        return nativeScreenAt(pos != null ? pos.x() : 0, pos != null ? pos.y() : 0);
    }
    private static native long nativeScreenAt(int x, int y);

// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** autoSipEnabled（Qt autoSipEnabled）。 */
    public boolean autoSipEnabled() {
        return nativeAutoSipEnabled(nativeHandle);
    }
    private static native boolean nativeAutoSipEnabled(long nativeHandle);

    /** closeAllWindows（Qt closeAllWindows）。 */
    public void closeAllWindows() {
        nativeCloseAllWindows(nativeHandle);
    }
    private static native void nativeCloseAllWindows(long nativeHandle);

    /** cursorFlashTime（Qt cursorFlashTime）。 */
    public int cursorFlashTime() {
        return nativeCursorFlashTime(nativeHandle);
    }
    private static native int nativeCursorFlashTime(long nativeHandle);

    /** doubleClickInterval（Qt doubleClickInterval）。 */
    public int doubleClickInterval() {
        return nativeDoubleClickInterval(nativeHandle);
    }
    private static native int nativeDoubleClickInterval(long nativeHandle);

    /** keyboardInputInterval（Qt keyboardInputInterval）。 */
    public int keyboardInputInterval() {
        return nativeKeyboardInputInterval(nativeHandle);
    }
    private static native int nativeKeyboardInputInterval(long nativeHandle);

    /** setCursorFlashTime（Qt setCursorFlashTime）。 */
    public void setCursorFlashTime(int arg0) {
        nativeSetCursorFlashTime(nativeHandle, arg0);
    }
    private static native void nativeSetCursorFlashTime(long nativeHandle, int arg0);

    /** setDoubleClickInterval（Qt setDoubleClickInterval）。 */
    public void setDoubleClickInterval(int arg0) {
        nativeSetDoubleClickInterval(nativeHandle, arg0);
    }
    private static native void nativeSetDoubleClickInterval(long nativeHandle, int arg0);

    /** setKeyboardInputInterval（Qt setKeyboardInputInterval）。 */
    public void setKeyboardInputInterval(int arg0) {
        nativeSetKeyboardInputInterval(nativeHandle, arg0);
    }
    private static native void nativeSetKeyboardInputInterval(long nativeHandle, int arg0);

    /** setStartDragDistance（Qt setStartDragDistance）。 */
    public void setStartDragDistance(int arg0) {
        nativeSetStartDragDistance(nativeHandle, arg0);
    }
    private static native void nativeSetStartDragDistance(long nativeHandle, int arg0);

    /** setStartDragTime（Qt setStartDragTime）。 */
    public void setStartDragTime(int arg0) {
        nativeSetStartDragTime(nativeHandle, arg0);
    }
    private static native void nativeSetStartDragTime(long nativeHandle, int arg0);

    /** setWheelScrollLines（Qt setWheelScrollLines）。 */
    public void setWheelScrollLines(int arg0) {
        nativeSetWheelScrollLines(nativeHandle, arg0);
    }
    private static native void nativeSetWheelScrollLines(long nativeHandle, int arg0);

    /** startDragTime（Qt startDragTime）。 */
    public int startDragTime() {
        return nativeStartDragTime(nativeHandle);
    }
    private static native int nativeStartDragTime(long nativeHandle);

}