/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 高精度计时器（Qt {@code QElapsedTimer}，纯 Java 实现，委托 {@link System#nanoTime()}）。
 */
public class QElapsedTimer {

    private long startNanos;
    private boolean started;

    public QElapsedTimer() { }

    public void start() { startNanos = System.nanoTime(); started = true; }

    /** 重启并返回距上次 start 的毫秒数。 */
    public long restart() {
        long now = System.nanoTime();
        long elapsed = started ? (now - startNanos) / 1_000_000 : 0;
        startNanos = now;
        started = true;
        return elapsed;
    }

    public long elapsed() {
        return started ? (System.nanoTime() - startNanos) / 1_000_000 : 0;
    }

    /** 是否已超过 ms 毫秒。 */
    public boolean hasExpired(long timeoutMs) {
        return elapsed() >= timeoutMs;
    }

    public boolean isValid() { return started; }

    public static QElapsedTimer create() {
        QElapsedTimer t = new QElapsedTimer();
        t.start();
        return t;
    }

    @Override
    public String toString() { return "QElapsedTimer(" + elapsed() + " ms)"; }
}
