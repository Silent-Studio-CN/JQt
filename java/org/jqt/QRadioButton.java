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
 * 单选按钮：封装 C++ 侧的 {@code QRadioButton}。
 * <p>同一父容器内的单选按钮互斥（Qt 自动分组）。
 * <p>信号槽：{@link #onToggled(Consumer)} — toggled 信号（参数为新状态）。
 */
public class QRadioButton extends QWidget {

    private final List<Consumer<Boolean>> onToggledHandlers = new ArrayList<>();

    public QRadioButton(String text) {
        nativeHandle = nativeCreate(text);
        registerCleaner();
    }

    private native long nativeCreate(String text);

    /** 修改文字。 */
    public void setText(String text) {
        nativeSetText(nativeHandle, text);
    }
    private native void nativeSetText(long handle, String text);

    /** 当前是否选中。 */
    public boolean isChecked() {
        return nativeIsChecked(nativeHandle);
    }
    private native boolean nativeIsChecked(long handle);

    /** 设置选中状态（触发 onToggled）。 */
    public void setChecked(boolean checked) {
        nativeSetChecked(nativeHandle, checked);
    }
    private native void nativeSetChecked(long handle, boolean checked);

    /** 选中状态切换回调（参数为新状态）。 */
    public QRadioButton onToggled(Consumer<Boolean> handler) {
        onToggledHandlers.add(handler);
        return this;
    }

    /** 由 C++ 侧回调（JNI）。 */
    void nativeHandleToggled(boolean checked) {
        for (Consumer<Boolean> h : onToggledHandlers) {
            h.accept(checked);
        }
    }
}
