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
        System.loadLibrary("jqt");
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
            String qss = Files.readString(Path.of(themeTemplatePath), StandardCharsets.UTF_8);
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
            Path p = Path.of(path);
            if (!Files.exists(p)) {
                throw new IllegalArgumentException("主题文件不存在: " + path);
            }
            String qss = Files.readString(p, StandardCharsets.UTF_8);
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
}

