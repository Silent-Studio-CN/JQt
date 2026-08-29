/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 面板/卡片容器（封装 QFrame）：Fluent 卡片、分组框、内容容器的基座。
 * 配合 QSS 使用（如 #card { background: ...; border-radius: 8px; }）。
 * 内部可 setLayout 或 addWidget 摆放子控件。
 */
public class QFrame extends QWidget {

    public QFrame() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    private native long nativeCreate();

    /** 直接添加子控件（无布局时自动摆放）。 */
    public void addWidget(QWidget child) {
        nativeAddWidget(nativeHandle, child.nativeHandle());
    }
    private native void nativeAddWidget(long handle, long childHandle);

// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** frameWidth（Qt frameWidth）。 */
    public int frameWidth() {
        return nativeFrameWidth(nativeHandle);
    }
    private static native int nativeFrameWidth(long nativeHandle);

    /** lineWidth（Qt lineWidth）。 */
    public int lineWidth() {
        return nativeLineWidth(nativeHandle);
    }
    private static native int nativeLineWidth(long nativeHandle);

    /** midLineWidth（Qt midLineWidth）。 */
    public int midLineWidth() {
        return nativeMidLineWidth(nativeHandle);
    }
    private static native int nativeMidLineWidth(long nativeHandle);

    /** setFrameStyle（Qt setFrameStyle）。 */
    public void setFrameStyle(int arg0) {
        nativeSetFrameStyle(nativeHandle, arg0);
    }
    private static native void nativeSetFrameStyle(long nativeHandle, int arg0);

    /** setLineWidth（Qt setLineWidth）。 */
    public void setLineWidth(int arg0) {
        nativeSetLineWidth(nativeHandle, arg0);
    }
    private static native void nativeSetLineWidth(long nativeHandle, int arg0);

    /** setMidLineWidth（Qt setMidLineWidth）。 */
    public void setMidLineWidth(int arg0) {
        nativeSetMidLineWidth(nativeHandle, arg0);
    }
    private static native void nativeSetMidLineWidth(long nativeHandle, int arg0);

}