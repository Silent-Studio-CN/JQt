/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.function.Consumer;

/**
 * 回调异常处理器（v1.9.1，修 J3）。
 * <p><b>背景</b>：由 Qt 触发的回调（信号、定时器、事件）是从原生代码进入 Java 的，
 * 你在回调体里抛出的异常没有"外层 Java 调用者"能接住 —— 旧版本把它打印后清除，
 * 于是表现为"stderr 刷了一行，但我的 try/catch 什么都接不到"。
 * <p>现在可以显式接管：
 * <pre>
 * JQtCallbackErrors.setHandler(ex -&gt; {
 *     ex.printStackTrace();
 *     myLogger.error("Qt 回调异常", ex);
 * });
 * </pre>
 * 未注册处理器时保持旧行为（打印堆栈），不会被静默丢弃。
 */
public final class JQtCallbackErrors {

    private JQtCallbackErrors() {}

    /** 注册处理器；传 {@code null} 取消注册。 */
    public static void setHandler(Consumer<Throwable> handler) {
        nativeSetHandler(handler);
    }
    private static native void nativeSetHandler(Consumer<Throwable> handler);
}
