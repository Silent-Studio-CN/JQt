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
 * 复选框（封装 QCheckBox）。配合 QSS 可呈现 Fluent 开关外观
 * （如 QCheckBox::indicator 胶囊滑块）。
 */
public class JQtCheckBox extends JQtWidget {

    private final List<Consumer<Boolean>> onToggledHandlers = new ArrayList<>();

    public JQtCheckBox(String text) {
        nativeHandle = nativeCreate(text);
        registerCleaner();
    }

    private native long nativeCreate(String text);

    /** 修改文字。 */
    public void setText(String text) {
        nativeSetText(nativeHandle, text);
    }
    private native void nativeSetText(long handle, String text);

    /** 当前是否勾选。 */
    public boolean isChecked() {
        return nativeIsChecked(nativeHandle);
    }
    private native boolean nativeIsChecked(long handle);

    /** 设置勾选状态（触发 onToggled）。 */
    public void setChecked(boolean checked) {
        nativeSetChecked(nativeHandle, checked);
    }
    private native void nativeSetChecked(long handle, boolean checked);

    /** 勾选状态切换回调（参数为新状态）。 */
    public void onToggled(Consumer<Boolean> handler) {
        onToggledHandlers.add(handler);
    }

    /** 由 C++ 侧回调（JNI）。 */
    void nativeHandleToggled(boolean checked) {
        for (Consumer<Boolean> h : onToggledHandlers) {
            h.accept(checked);
        }
    }
}