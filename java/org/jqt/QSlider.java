/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fluent 滑块：轨道 + 圆钮，已填充部分用主题色，点击跳转（120ms 动画）、拖动跟手。
 * 轨道颜色跟随主题明暗。
 */
public class QSlider extends QWidget {

    private final List<Consumer<Integer>> onValueChangedHandlers = new ArrayList<>();

    /** 创建滑块（范围 min~max，初始 value）。 */
    public QSlider(int min, int max, int value) {
        nativeHandle = nativeCreate(min, max, value);
        registerCleaner();
    }

    /** 当前值。 */
    public int value() {
        return nativeValue(nativeHandle);
    }

    /** 设置值（点击跳转动画）。 */
    public void setValue(int value) {
        nativeSetValue(nativeHandle, value);
    }

    /** 设置范围。 */
    public void setRange(int min, int max) {
        nativeSetRange(nativeHandle, min, max);
    }

    /** 值变化回调（拖动高频触发）。 */
    public QSlider onValueChanged(Consumer<Integer> handler) {
        onValueChangedHandlers.add(handler);
        return this;
    }

    /** 由 C++ 侧回调（JNI）。 */
    void nativeHandleValueChanged(int value) {
        for (Consumer<Integer> h : onValueChangedHandlers) {
            h.accept(value);
        }
    }

    private native long nativeCreate(int min, int max, int value);
    private native int nativeValue(long handle);
    private native void nativeSetValue(long handle, int value);
    private native void nativeSetRange(long handle, int min, int max);

    // ---- L1 补全（v0.6.0）----

    /** 刻度间距（0 = 自动）。 */
    public void setTickInterval(int interval) { nativeSetTickInterval(nativeHandle, interval); }
    private static native void nativeSetTickInterval(long handle, int interval);

    /** 刻度间距。 */
    public int tickInterval() { return nativeTickInterval(nativeHandle); }
    private static native int nativeTickInterval(long handle);

    /** 刻度位置：0 无 / 1 上（左）/ 2 下（右）/ 3 两侧。 */
    public void setTickPosition(int position) { nativeSetTickPosition(nativeHandle, position); }
    private static native void nativeSetTickPosition(long handle, int position);

    /** 刻度位置。 */
    public int tickPosition() { return nativeTickPosition(nativeHandle); }
    private static native int nativeTickPosition(long handle);
}