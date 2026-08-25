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
                applyThemeFile("themes/fluent-dark.qss", false);
                break;
            case "fluent-light":
                applyThemeFile("themes/fluent-light.qss", true);
                break;
            default:
                throw new IllegalArgumentException("未知主题: " + name + "（内置: fluent-dark / fluent-light）");
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