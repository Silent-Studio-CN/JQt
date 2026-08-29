/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 截止时间（Qt {@code QDeadlineTimer}，纯 Java 实现）。
 * <p>语义：剩余毫秒数；{@code Forever(-1)} 表示永不超时。
 */
public class QDeadlineTimer {

    /** 永不超时。 */
    public static final int Forever = -1;

    private final long deadlineNanos;   // -1 = forever

    public QDeadlineTimer() { this(Forever); }

    /** 从当前起 msRemaining 毫秒后截止；Forever(-1) 永不超时。 */
    public QDeadlineTimer(long msRemaining) {
        if (msRemaining == Forever) this.deadlineNanos = -1;
        else this.deadlineNanos = System.nanoTime() + msRemaining * 1_000_000;
    }

    /** 从当前起（即时启动）。 */
    public static QDeadlineTimer current() { return new QDeadlineTimer(0); }

    public boolean isForever() { return deadlineNanos == -1; }

    /** 剩余毫秒数（≤0 表示已过期）。 */
    public long remainingTime() {
        if (deadlineNanos == -1) return Forever;
        long rem = (deadlineNanos - System.nanoTime()) / 1_000_000;
        return Math.max(0, rem);
    }

    /** 是否已过期。 */
    public boolean hasExpired() {
        return deadlineNanos != -1 && System.nanoTime() >= deadlineNanos;
    }

    public void setRemainingTime(long msRemaining) {
        if (msRemaining == Forever) { /* immutable */ }
    }

    @Override
    public String toString() {
        return isForever() ? "QDeadlineTimer(Forever)" : "QDeadlineTimer(" + remainingTime() + " ms left)";
    }
}
