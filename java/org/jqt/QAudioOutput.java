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
import java.util.function.Consumer;

/**
 * 音频输出（Qt {@code QAudioOutput}，QtMultimedia 模块）。
 * <p>{@code QMediaPlayer} 需要显式接一个输出才会出声（Qt 6 的语义）。
 */
public class QAudioOutput {

    private static final Cleaner CLEANER = Cleaner.create();
    private long nativeHandle;
    private double volume = 1.0;
    private boolean muted;

    /** 创建默认音频输出。 */
    public QAudioOutput() {
        if (!QMediaPlayer.isAvailable()) {
            throw new IllegalStateException("本库未编译 QtMultimedia 支持(缺少 qtmultimedia 模块)");
        }
        nativeHandle = nativeCreate();
        final long h = nativeHandle;
        CLEANER.register(this, () -> nativeDispose(h));
    }

    private static native long nativeCreate();
    private static native void nativeDispose(long handle);

    long nativeHandle() { return nativeHandle; }

    /** 音量（0.0 ~ 1.0；线性）。 */
    public void setVolume(double v) {
        volume = Math.max(0.0, Math.min(1.0, v));
        nativeSetVolume(nativeHandle, volume);
    }
    private static native void nativeSetVolume(long handle, double volume);

    /** 当前音量。 */
    public double volume() { return volume; }

    /** 静音开关。 */
    public void setMuted(boolean m) {
        muted = m;
        nativeSetMuted(nativeHandle, m);
    }
    private static native void nativeSetMuted(long handle, boolean muted);

    /** 是否静音。 */
    public boolean isMuted() { return muted; }
}
