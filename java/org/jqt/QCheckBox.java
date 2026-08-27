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
public class QCheckBox extends QWidget {

    private final List<Consumer<Boolean>> onToggledHandlers = new ArrayList<>();

    public QCheckBox(String text) {
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
    public QCheckBox onToggled(Consumer<Boolean> handler) {
        onToggledHandlers.add(handler);
        return this;
    }

    /** 由 C++ 侧回调（JNI）。 */
    void nativeHandleToggled(boolean checked) {
        for (Consumer<Boolean> h : onToggledHandlers) {
            h.accept(checked);
        }
    }

    // ---- L1 补全（v0.6.0）----

    /** 三态模式（0 未勾选 / 1 部分 / 2 勾选）。 */
    public void setTristate(boolean tristate) { nativeSetTristate(nativeHandle, tristate); }
    private static native void nativeSetTristate(long handle, boolean tristate);

    /** 是否三态模式。 */
    public boolean isTristate() { return nativeIsTristate(nativeHandle); }
    private static native boolean nativeIsTristate(long handle);

    /** 检查状态：0 未勾选 / 1 部分 / 2 勾选。 */
    public int checkState() { return nativeCheckState(nativeHandle); }
    private static native int nativeCheckState(long handle);

    /** 设置检查状态（0/1/2）。 */
    public void setCheckState(int state) { nativeSetCheckState(nativeHandle, state); }
    private static native void nativeSetCheckState(long handle, int state);

    private final List<Consumer<Integer>> onCheckStateChangedHandlers = new ArrayList<>();
    private volatile boolean stateConn;

    /** 检查状态变化回调（参数 0/1/2）。 */
    public QCheckBox onCheckStateChanged(Consumer<Integer> handler) {
        onCheckStateChangedHandlers.add(handler);
        if (!stateConn) { stateConn = true; nativeConnectCheckStateChanged(nativeHandle); }
        return this;
    }

    private native void nativeConnectCheckStateChanged(long handle);

    void nativeHandleCheckStateChanged(int state) {
        for (Consumer<Integer> h : onCheckStateChangedHandlers) h.accept(state);
    }
}