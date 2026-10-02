/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 线程池（Qt {@code QThreadPool} 子集）：把任务丢到后台跑，别卡住界面。
 * <pre>
 * QThreadPool.runAsync(() -&gt; {
 *     String data = fetch();                              // 后台线程
 *     QTimer.singleShot(0, () -&gt; label.setText(data));    // 回到主线程更新界面
 * });
 * QThreadPool.waitForDone(3000);
 * </pre>
 * 与 {@link QThread} 的分工：一次性小任务用线程池；需要独立线程标识、等待或中断语义时用 {@code QThread}。
 */
public final class QThreadPool {

    private QThreadPool() {}

    /** 提交任务到全局线程池（不阻塞调用方）。 */
    public static void runAsync(Runnable task) {
        if (task == null) return;
        nativeRunAsync(new Runner(task));
    }
    private static native void nativeRunAsync(Runner runner);

    /** 线程池最大并发数。 */
    public static int maxThreadCount() { return nativeMaxThreadCount(); }
    private static native int nativeMaxThreadCount();

    /** 设置最大并发数（>0 生效）。 */
    public static void setMaxThreadCount(int n) { nativeSetMaxThreadCount(n); }
    private static native void nativeSetMaxThreadCount(int n);

    /** 当前活跃线程数。 */
    public static int activeThreadCount() { return nativeActiveThreadCount(); }
    private static native int nativeActiveThreadCount();

    /** 等待全部任务结束（毫秒；返回是否已全部结束）。 */
    public static boolean waitForDone(int ms) { return nativeWaitForDone(ms); }
    private static native boolean nativeWaitForDone(int ms);

    /** JNI 跳板（public run 便于 JNI 按名查找）。 */
    static final class Runner {
        private final Runnable task;
        Runner(Runnable task) { this.task = task; }
        public void run() { task.run(); }
    }
}
