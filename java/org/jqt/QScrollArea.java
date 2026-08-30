/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 滚动区：内容超出可视区域时可滚动（QScrollArea 封装）。
 * 滚动条样式由 QSS 控制（QScrollBar 规则）。
 */
public class QScrollArea extends QWidget {

    public QScrollArea() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    /** 设置滚动内容（内容控件生命周期移交滚动区管理）。 */
    public void setWidget(QWidget widget) {
        nativeSetWidget(nativeHandle, widget.nativeHandle());
    }

    /** 内容宽度是否跟随滚动区（默认 true）。 */
    public void setWidgetResizable(boolean resizable) {
        nativeSetWidgetResizable(nativeHandle, resizable);
    }

    private native long nativeCreate();
    private native void nativeSetWidget(long handle, long childHandle);
    private native void nativeSetWidgetResizable(long handle, boolean resizable);

    // ---- L1 补全（v0.6.0）----

    /** 内容对齐：1 左 / 2 右 / 4 居中 / 8 两端。 */
    public void setAlignment(int alignment) { nativeSetAlignment(nativeHandle, alignment); }
    private static native void nativeSetAlignment(long handle, int alignment);

    /** 内容对齐。 */
    public int alignment() { return nativeAlignment(nativeHandle); }
    private static native int nativeAlignment(long handle);

// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** ensureVisible（Qt ensureVisible）。 */
    public void ensureVisible(int arg0, int arg1, int arg2, int arg3) {
        nativeEnsureVisible(nativeHandle, arg0, arg1, arg2, arg3);
    }
    private static native void nativeEnsureVisible(long nativeHandle, int arg0, int arg1, int arg2, int arg3);

    /** widgetResizable（Qt widgetResizable）。 */
    public boolean widgetResizable() {
        return nativeWidgetResizable(nativeHandle);
    }
    private static native boolean nativeWidgetResizable(long nativeHandle);

}