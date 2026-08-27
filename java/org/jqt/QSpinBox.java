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
 * 整数微调框：封装 C++ 侧的 {@code QSpinBox}。
 * <p>信号槽：{@link #onValueChanged(Consumer)} — valueChanged 信号（参数为新值）。
 */
public class QSpinBox extends QWidget {

    private final List<Consumer<Integer>> onValueChangedHandlers = new ArrayList<>();

    public QSpinBox() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    private native long nativeCreate();

    /** 设置取值范围。 */
    public void setRange(int min, int max) {
        nativeSetRange(nativeHandle, min, max);
    }
    private native void nativeSetRange(long handle, int min, int max);

    /** 当前值。 */
    public int value() {
        return nativeValue(nativeHandle);
    }
    private native int nativeValue(long handle);

    /** 设置当前值（触发 onValueChanged）。 */
    public void setValue(int value) {
        nativeSetValue(nativeHandle, value);
    }
    private native void nativeSetValue(long handle, int value);

    /** 数值变化回调（参数为新值）。 */
    public QSpinBox onValueChanged(Consumer<Integer> handler) {
        onValueChangedHandlers.add(handler);
        return this;
    }

    /** 由 C++ 侧回调（JNI）。 */
    void nativeHandleValueChanged(int value) {
        for (Consumer<Integer> h : onValueChangedHandlers) {
            h.accept(value);
        }
    }

    // ---- L1 补全（v0.6.0）----

    /** 前缀（如 "$"）。 */
    public void setPrefix(String prefix) { nativeSetPrefix(nativeHandle, prefix); }
    private static native void nativeSetPrefix(long handle, String prefix);

    /** 前缀。 */
    public String prefix() { return nativePrefix(nativeHandle); }
    private static native String nativePrefix(long handle);

    /** 后缀（如 " kg"）。 */
    public void setSuffix(String suffix) { nativeSetSuffix(nativeHandle, suffix); }
    private static native void nativeSetSuffix(long handle, String suffix);

    /** 后缀。 */
    public String suffix() { return nativeSuffix(nativeHandle); }
    private static native String nativeSuffix(long handle);

    /** 步进。 */
    public void setSingleStep(int step) { nativeSetSingleStep(nativeHandle, step); }
    private static native void nativeSetSingleStep(long handle, int step);

    /** 步进。 */
    public int singleStep() { return nativeSingleStep(nativeHandle); }
    private static native int nativeSingleStep(long handle);

    /** 最小值。 */
    public int minimum() { return nativeMinimum(nativeHandle); }
    private static native int nativeMinimum(long handle);

    /** 最大值。 */
    public int maximum() { return nativeMaximum(nativeHandle); }
    private static native int nativeMaximum(long handle);

    /** 设置最小值。 */
    public void setMinimum(int min) { nativeSetMinimum(nativeHandle, min); }
    private static native void nativeSetMinimum(long handle, int min);

    /** 设置最大值。 */
    public void setMaximum(int max) { nativeSetMaximum(nativeHandle, max); }
    private static native void nativeSetMaximum(long handle, int max);

    /** 无前后缀的干净文本。 */
    public String cleanText() { return nativeCleanText(nativeHandle); }
    private static native String nativeCleanText(long handle);
}