/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 分组框：封装 C++ 侧的 {@code QGroupBox}，带标题边框的容器。
 * 内部可 {@code setLayout} 摆放子控件。
 */
public class QGroupBox extends QWidget {

    /** 创建带标题的分组框。 */
    public QGroupBox(String title) {
        nativeHandle = nativeCreate(title);
        registerCleaner();
    }

    private native long nativeCreate(String title);

    /** 设置标题。 */
    public void setTitle(String title) {
        nativeSetTitle(nativeHandle, title);
    }
    private native void nativeSetTitle(long handle, String title);

    /** 读取标题。 */
    public String title() {
        return nativeTitle(nativeHandle);
    }
    private native String nativeTitle(long handle);

// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** isCheckable（Qt isCheckable）。 */
    public boolean isCheckable() {
        return nativeIsCheckable(nativeHandle);
    }
    private static native boolean nativeIsCheckable(long nativeHandle);

    /** isChecked（Qt isChecked）。 */
    public boolean isChecked() {
        return nativeIsChecked(nativeHandle);
    }
    private static native boolean nativeIsChecked(long nativeHandle);

    /** isFlat（Qt isFlat）。 */
    public boolean isFlat() {
        return nativeIsFlat(nativeHandle);
    }
    private static native boolean nativeIsFlat(long nativeHandle);

    /** setAlignment（Qt setAlignment）。 */
    public void setAlignment(int arg0) {
        nativeSetAlignment(nativeHandle, arg0);
    }
    private static native void nativeSetAlignment(long nativeHandle, int arg0);

    /** setCheckable（Qt setCheckable）。 */
    public void setCheckable(boolean arg0) {
        nativeSetCheckable(nativeHandle, arg0);
    }
    private static native void nativeSetCheckable(long nativeHandle, boolean arg0);

    /** setFlat（Qt setFlat）。 */
    public void setFlat(boolean arg0) {
        nativeSetFlat(nativeHandle, arg0);
    }
    private static native void nativeSetFlat(long nativeHandle, boolean arg0);

}