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
 * Fluent 风格选项卡（Pivot）：文本项 + 底部滑动指示器（动画过渡）。
 * 纯自绘（clean-room 独立实现），颜色跟随 QPalette::Highlight（可被主题定制）。
 */
public class JQtPivot extends QWidget {

    private final List<Consumer<Integer>> onChangedHandlers = new ArrayList<>();

    /** 创建选项卡组（高 36px）。 */
    public JQtPivot() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    /** 追加选项卡。 */
    public void addItem(String text) {
        nativeAddItem(nativeHandle, text);
    }

    /** 当前选中索引。 */
    public int currentIndex() {
        return nativeCurrentIndex(nativeHandle);
    }

    /** 选中指定项（指示器滑动动画，触发 onChanged）。 */
    public void setCurrentIndex(int index) {
        nativeSetCurrentIndex(nativeHandle, index);
    }

    /** 选中项变化回调（参数：新索引）。 */
    public JQtPivot onChanged(Consumer<Integer> handler) {
        onChangedHandlers.add(handler);
        return this;
    }

    /** 由 C++ 侧在选中项变化时回调（JNI）。 */
    void nativeHandleChanged(int index) {
        for (Consumer<Integer> h : onChangedHandlers) {
            h.accept(index);
        }
    }

    private native long nativeCreate();
    private native void nativeAddItem(long handle, String text);
    private native int nativeCurrentIndex(long handle);
    private native void nativeSetCurrentIndex(long handle, int index);
}

