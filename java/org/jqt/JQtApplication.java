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
 * JQtApplication app = new JQtApplication();
 * JQtWindow window = new JQtWindow("我的窗口");
 * window.show();
 * app.exec();   // 进入 Qt 事件循环（阻塞），最后一个窗口关闭后返回
 * </pre>
 */
public class JQtApplication {

    static {
        // 加载 native 库：jqt.dll (Windows) / libjqt.so (Linux) / libjqt.dylib (macOS)
        System.loadLibrary("jqt");
    }

    /** C++ 侧 QApplication 指针。 */
    private final long nativeHandle;

    private final List<Runnable> onAboutToQuitHandlers = new ArrayList<>();

    /** 全局动画主题（动效节奏 + 默认缓动）。 */
    private static volatile JQtAnimationTheme animationTheme = JQtAnimationTheme.DEFAULT;

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
    public JQtApplication() {
        nativeHandle = nativeCreateApp();
        if (Boolean.getBoolean("jqt.lightMode")) {
            setColorScheme(true);
        }
    }

    private native long nativeCreateApp();

    /**
     * 进入 Qt 事件循环（阻塞调用）。
     * 当最后一个窗口关闭时返回（对应 Qt 的 quitOnLastWindowClosed）。
     */
    public native void exec();

    /** 退出事件循环。 */
    public native void quit();

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
        try {
            Path p = Path.of(qssTemplatePath);
            if (!Files.exists(p)) {
                throw new IllegalArgumentException("主题文件不存在: " + qssTemplatePath);
            }
            String qss = Files.readString(p, StandardCharsets.UTF_8);
            if (variables != null) {
                for (java.util.Map.Entry<String, String> e : variables.entrySet()) {
                    qss = qss.replace("%" + e.getKey() + "%", e.getValue());
                }
            }
            setColorScheme(light);
            setStyleSheet(qss);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("读取主题失败: " + qssTemplatePath, e);
        }
    }

    /** 自定义主题：加载 QSS 文件 + 指定浅色/深色调色板。 */
    public void setTheme(String qssPath, boolean light) {
        applyThemeFile(qssPath, light);
    }

    private void applyThemeFile(String path, boolean light) {
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
    public void onAboutToQuit(Runnable handler) {
        onAboutToQuitHandlers.add(handler);
    }

    /** 由 C++ 侧在应用退出前回调（JNI）。 */
    void nativeHandleAboutToQuit() {
        for (Runnable h : onAboutToQuitHandlers) {
            h.run();
        }
    }
}