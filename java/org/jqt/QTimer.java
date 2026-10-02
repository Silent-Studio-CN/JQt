/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.lang.ref.Cleaner;
import java.util.ArrayList;
import java.util.List;

/**
 * 定时器（Qt {@code QTimer}）。
 * <p>重复定时器：
 * <pre>
 * QTimer t = new QTimer(1000);
 * t.onTimeout(() -&gt; System.out.println("tick"));
 * t.start();
 * </pre>
 * 单次：
 * <pre>
 * QTimer.singleShot(500, () -&gt; System.out.println("once"));
 * </pre>
 * <b>线程语义</b>：{@code QTimer} 的回调在 **Qt 主线程**执行；{@link #singleShot(int, Runnable)}
 * 因此也可以当作"从后台线程回到主线程"的编组手段（等价于 Qt 的
 * {@code QTimer::singleShot(0, ...)}），这是更新界面的推荐做法。
 */
public class QTimer {

    private static final Cleaner CLEANER = Cleaner.create();

    private final List<Runnable> timeoutHandlers = new ArrayList<>();
    private long nativeHandle;
    private boolean connected;

    /** 创建定时器（未启动；默认 interval = 0）。 */
    public QTimer() {
        nativeHandle = nativeCreate();
        final long handle = nativeHandle;
        CLEANER.register(this, () -> nativeDispose(handle));
    }

    /** 创建定时器并设置间隔（毫秒）。 */
    public QTimer(int intervalMs) {
        this();
        setInterval(intervalMs);
    }

    private native long nativeCreate();
    private native void nativeDispose(long handle);

    /** 设置间隔（毫秒）。 */
    public void setInterval(int ms) { nativeSetInterval(nativeHandle, Math.max(0, ms)); }
    private native void nativeSetInterval(long handle, int ms);

    /** 当前间隔（毫秒）。 */
    public int interval() { return nativeInterval(nativeHandle); }
    private native int nativeInterval(long handle);

    /** 是否为一次性定时器。 */
    public void setSingleShot(boolean singleShot) { nativeSetSingleShot(nativeHandle, singleShot); }
    private native void nativeSetSingleShot(long handle, boolean singleShot);

    /** 是否一次性。 */
    public boolean isSingleShot() { return nativeIsSingleShot(nativeHandle); }
    private native boolean nativeIsSingleShot(long handle);

    /** 以当前 interval 启动。 */
    public void start() { nativeStart(nativeHandle, -1); }

    /** 以指定间隔启动（毫秒）。 */
    public void start(int ms) { nativeStart(nativeHandle, Math.max(0, ms)); }
    private native void nativeStart(long handle, int ms);

    /** 停止。 */
    public void stop() { nativeStop(nativeHandle); }
    private native void nativeStop(long handle);

    /** 是否运行中。 */
    public boolean isActive() { return nativeIsActive(nativeHandle); }
    private native boolean nativeIsActive(long handle);

    /** 剩余时间（毫秒；未启动返回 -1）。 */
    public int remainingTime() { return nativeRemainingTime(nativeHandle); }
    private native int nativeRemainingTime(long handle);

    /**
     * 注册超时回调（可注册多个，各自每次触发各收到一次）。
     * <p>首次注册时建立原生连接（惰性，且原生侧去重）。
     */
    public QTimer onTimeout(Runnable handler) {
        timeoutHandlers.add(handler);
        if (!connected) {
            connected = true;
            nativeConnectTimeout(nativeHandle);
        }
        return this;
    }
    private native void nativeConnectTimeout(long handle);

    /** 由 C++ 侧在超时时回调（JNI）。 */
    void nativeHandleTimeout() {
        for (Runnable h : timeoutHandlers) {
            h.run();
        }
    }

    /**
     * 在 Qt 主线程上延迟执行一次（{@code QTimer::singleShot}）。
     * <p>可从任意线程调用；{@code ms = 0} 即"尽快在主线程执行"。
     */
    public static void singleShot(int ms, Runnable action) {
        if (action == null) return;
        nativeSingleShot(Math.max(0, ms), new OnceRunner(action));
    }
    private static native void nativeSingleShot(int ms, OnceRunner runner);

    /** JNI 跳板：保证回调发生在 Qt 主线程。 */
    static final class OnceRunner {
        private final Runnable action;
        OnceRunner(Runnable action) { this.action = action; }
        /** 由 C++ 侧在主线程调用（JNI 按名字查找）。 */
        public void run() { action.run(); }
    }

    /** 当前是否在 Qt 主线程（用于断言/防御式编程）。 */
    public static boolean isMainThread() { return nativeIsMainThread(); }
    private static native boolean nativeIsMainThread();
}
