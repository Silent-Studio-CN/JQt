/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 分割器：封装 C++ 侧的 {@code QSplitter}，子控件之间可拖动调整大小。
 * <pre>
 * QSplitter splitter = new QSplitter();
 * splitter.setOrientation(1);   // 1 = 垂直
 * splitter.addWidget(leftPanel);
 * splitter.addWidget(rightPanel);
 * </pre>
 */
public class QSplitter extends QWidget {

    /** 水平方向。 */
    public static final int HORIZONTAL = 0;

    /** 垂直方向。 */
    public static final int VERTICAL = 1;

    public QSplitter() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    private native long nativeCreate();

    /** 设置方向（HORIZONTAL / VERTICAL）。 */
    public void setOrientation(int orientation) {
        nativeSetOrientation(nativeHandle, orientation);
    }
    private native void nativeSetOrientation(long handle, int orientation);

    /** 追加子控件。 */
    public void addWidget(QWidget widget) {
        nativeAddWidget(nativeHandle, widget.nativeHandle);
    }
    private native void nativeAddWidget(long handle, long childHandle);

    /** 设置各子控件尺寸（像素）。 */
    public void setSizes(int[] sizes) {
        nativeSetSizes(nativeHandle, sizes);
    }
    private native void nativeSetSizes(long handle, int[] sizes);

    /** 读取各子控件当前尺寸。 */
    public int[] sizes() {
        return nativeSizes(nativeHandle);
    }
    private native int[] nativeSizes(long handle);

    /** 设置分隔条宽度。 */
    public void setHandleWidth(int width) {
        nativeSetHandleWidth(nativeHandle, width);
    }
    private native void nativeSetHandleWidth(long handle, int width);

    // ---- L1 补全（v0.6.0）----

    /** 子控件数量。 */
    public int count() { return nativeCount(nativeHandle); }
    private static native int nativeCount(long handle);

// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** childrenCollapsible（Qt childrenCollapsible）。 */
    public boolean childrenCollapsible() {
        return nativeChildrenCollapsible(nativeHandle);
    }
    private static native boolean nativeChildrenCollapsible(long nativeHandle);

    /** handleWidth（Qt handleWidth）。 */
    public int handleWidth() {
        return nativeHandleWidth(nativeHandle);
    }
    private static native int nativeHandleWidth(long nativeHandle);

    /** opaqueResize（Qt opaqueResize）。 */
    public boolean opaqueResize() {
        return nativeOpaqueResize(nativeHandle);
    }
    private static native boolean nativeOpaqueResize(long nativeHandle);

    /** refresh（Qt refresh）。 */
    public void refresh() {
        nativeRefresh(nativeHandle);
    }
    private static native void nativeRefresh(long nativeHandle);

    /** setChildrenCollapsible（Qt setChildrenCollapsible）。 */
    public void setChildrenCollapsible(boolean arg0) {
        nativeSetChildrenCollapsible(nativeHandle, arg0);
    }
    private static native void nativeSetChildrenCollapsible(long nativeHandle, boolean arg0);

    /** setCollapsible（Qt setCollapsible）。 */
    public void setCollapsible(int arg0, boolean arg1) {
        nativeSetCollapsible(nativeHandle, arg0, arg1);
    }
    private static native void nativeSetCollapsible(long nativeHandle, int arg0, boolean arg1);

    /** setOpaqueResize（Qt setOpaqueResize）。 */
    public void setOpaqueResize(boolean arg0) {
        nativeSetOpaqueResize(nativeHandle, arg0);
    }
    private static native void nativeSetOpaqueResize(long nativeHandle, boolean arg0);

    /** setStretchFactor（Qt setStretchFactor）。 */
    public void setStretchFactor(int arg0, int arg1) {
        nativeSetStretchFactor(nativeHandle, arg0, arg1);
    }
    private static native void nativeSetStretchFactor(long nativeHandle, int arg0, int arg1);

}