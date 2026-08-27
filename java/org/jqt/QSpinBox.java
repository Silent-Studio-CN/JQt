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
}
