/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

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
}
