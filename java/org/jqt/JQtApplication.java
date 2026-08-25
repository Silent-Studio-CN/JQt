/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

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
     */
    public JQtApplication() {
        nativeHandle = nativeCreateApp();
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

    /**
     * 设置全局样式表（QSS，Qt Style Sheets）。
     * <p>
     * 示例：
     * <pre>
     * app.setStyleSheet("QPushButton { background: #3c3f41; color: white; }");
     * </pre>
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
