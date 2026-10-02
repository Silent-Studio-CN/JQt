/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.lang.ref.Cleaner;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程（Qt {@code QThread} 子集）：在独立 Qt 线程里跑一段 Java 代码。
 * <pre>
 * QThread t = new QThread(() -&gt; heavyWork());
 * t.start();
 * t.wait(5000);          // 毫秒；返回是否已结束
 * </pre>
 * <b>界面更新</b>：Qt 的控件只能在主线程碰。后台线程里请用
 * {@link QTimer#singleShot(int, Runnable)} 把更新动作编组回主线程：
 * <pre>
 * new QThread(() -&gt; {
 *     String data = fetch();                       // 后台
 *     QTimer.singleShot(0, () -&gt; label.setText(data));   // 主线程
 * }).start();
 * </pre>
 */
public class QThread {

    private static final Cleaner CLEANER = Cleaner.create();
    private static final AtomicInteger SEQ = new AtomicInteger();

    private long nativeHandle;
    private final String name;

    /** 创建一个线程对象（尚未启动）；任务在 {@link #start()} 后于新线程执行。 */
    public QThread(Runnable task) {
        this(task, "jqt-thread-" + SEQ.incrementAndGet());
    }

    /** 创建线程并指定名字（便于日志/断言）。 */
    public QThread(Runnable task, String name) {
        if (task == null) throw new NullPointerException("task");
        this.name = name;
        nativeHandle = nativeCreate(new TaskRunner(task, name));
        final long handle = nativeHandle;
        CLEANER.register(this, () -> nativeDispose(handle));
    }

    private native long nativeCreate(TaskRunner runner);
    private native void nativeDispose(long handle);

    /** 启动线程。 */
    public void start() { nativeStart(nativeHandle); }
    private native void nativeStart(long handle);

    /** 是否运行中。 */
    public boolean isRunning() { return nativeIsRunning(nativeHandle); }
    private native boolean nativeIsRunning(long handle);

    /** 是否已结束。 */
    public boolean isFinished() { return nativeIsFinished(nativeHandle); }
    private native boolean nativeIsFinished(long handle);

    /** 等待结束（毫秒；返回是否已结束）。 */
    public boolean wait(int ms) { return nativeWait(nativeHandle, ms); }
    private native boolean nativeWait(long handle, int ms);

    /** 请求中断（Java 侧需自行检查 {@link Thread#interrupted()} 或业务标志）。 */
    public void requestInterruption() { nativeRequestInterruption(nativeHandle); }
    private native void nativeRequestInterruption(long handle);

    /** 线程名。 */
    public String name() { return name; }

    @Override public String toString() { return "QThread(" + name + ")"; }

    /** JNI 跳板：在新 Qt 线程里执行 Java 任务。 */
    static final class TaskRunner {
        private final Runnable task;
        private final String name;
        TaskRunner(Runnable task, String name) { this.task = task; this.name = name; }
        /** 由 C++ 侧在线程体里调用。 */
        public void run() {
            Thread.currentThread().setName(name);   // 让日志/断言能看到线程名
            task.run();
        }
        /** 由 C++ 侧设置线程名。 */
        public String threadName() { return name; }
    }
}
