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
public class JQtScrollArea extends JQtWidget {

    public JQtScrollArea() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    /** 设置滚动内容（内容控件生命周期移交滚动区管理）。 */
    public void setWidget(JQtWidget widget) {
        nativeSetWidget(nativeHandle, widget.nativeHandle());
    }

    /** 内容宽度是否跟随滚动区（默认 true）。 */
    public void setWidgetResizable(boolean resizable) {
        nativeSetWidgetResizable(nativeHandle, resizable);
    }

    private native long nativeCreate();
    private native void nativeSetWidget(long handle, long childHandle);
    private native void nativeSetWidgetResizable(long handle, boolean resizable);
}
