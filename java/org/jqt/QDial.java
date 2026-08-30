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
 * 旋钮（表盘）控件：封装 C++ 侧的 {@code QDial}。
 * <p>信号槽：{@link #onValueChanged(Consumer)} — valueChanged 信号（参数为新值）。
 */
public class QDial extends QWidget {

    private final List<Consumer<Integer>> onValueChangedHandlers = new ArrayList<>();

    public QDial() {
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
    public QDial onValueChanged(Consumer<Integer> handler) {
        onValueChangedHandlers.add(handler);
        return this;
    }

    /** 由 C++ 侧回调（JNI）。 */
    void nativeHandleValueChanged(int value) {
        for (Consumer<Integer> h : onValueChangedHandlers) {
            h.accept(value);
        }
    }

// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** notchSize（Qt notchSize）。 */
    public int notchSize() {
        return nativeNotchSize(nativeHandle);
    }
    private static native int nativeNotchSize(long nativeHandle);

    /** notchTarget（Qt notchTarget）。 */
    public double notchTarget() {
        return nativeNotchTarget(nativeHandle);
    }
    private static native double nativeNotchTarget(long nativeHandle);

    /** notchesVisible（Qt notchesVisible）。 */
    public boolean notchesVisible() {
        return nativeNotchesVisible(nativeHandle);
    }
    private static native boolean nativeNotchesVisible(long nativeHandle);

    /** setNotchTarget（Qt setNotchTarget）。 */
    public void setNotchTarget(double arg0) {
        nativeSetNotchTarget(nativeHandle, arg0);
    }
    private static native void nativeSetNotchTarget(long nativeHandle, double arg0);

    /** setNotchesVisible（Qt setNotchesVisible）。 */
    public void setNotchesVisible(boolean arg0) {
        nativeSetNotchesVisible(nativeHandle, arg0);
    }
    private static native void nativeSetNotchesVisible(long nativeHandle, boolean arg0);

    /** setWrapping（Qt setWrapping）。 */
    public void setWrapping(boolean arg0) {
        nativeSetWrapping(nativeHandle, arg0);
    }
    private static native void nativeSetWrapping(long nativeHandle, boolean arg0);

    /** wrapping（Qt wrapping）。 */
    public boolean wrapping() {
        return nativeWrapping(nativeHandle);
    }
    private static native boolean nativeWrapping(long nativeHandle);

}