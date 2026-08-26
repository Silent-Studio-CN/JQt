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
public class JQtSlider extends JQtWidget {

    private final List<Consumer<Integer>> onValueChangedHandlers = new ArrayList<>();

    /** 创建滑块（范围 min~max，初始 value）。 */
    public JQtSlider(int min, int max, int value) {
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
    public void onValueChanged(Consumer<Integer> handler) {
        onValueChangedHandlers.add(handler);
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
}
