/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 窗口：封装 C++ 侧的顶级 {@code QWidget}。
 */
public class JQtWindow extends JQtWidget {

    private Runnable onCloseHandler;

    /** 创建一个 800x600 的新窗口。 */
    public JQtWindow(String title) {
        this(title, 800, 600);
    }

    /** 创建一个指定大小的新窗口。 */
    public JQtWindow(String title, int width, int height) {
        nativeHandle = nativeCreate(title, width, height);
    }

    private native long nativeCreate(String title, int width, int height);

    /** 显示窗口。 */
    public void show() {
        nativeShow(nativeHandle);
    }
    private native void nativeShow(long handle);

    /** 隐藏窗口。 */
    public void hide() {
        nativeHide(nativeHandle);
    }
    private native void nativeHide(long handle);

    /** 修改窗口标题。 */
    public void setTitle(String title) {
        nativeSetTitle(nativeHandle, title);
    }
    private native void nativeSetTitle(long handle, String title);

    /**
     * 把子控件添加到窗口中。
     * C++ 侧建立 Qt 父子关系（父窗口销毁时自动销毁子控件）。
     */
    public void addWidget(JQtWidget child) {
        nativeAddWidget(nativeHandle, child.nativeHandle());
    }
    private native void nativeAddWidget(long handle, long childHandle);

    /** 注册窗口关闭回调（对应 Qt 的 closeEvent）。 */
    public void onClose(Runnable handler) {
        this.onCloseHandler = handler;
    }

    /** 由 C++ 侧在窗口关闭时回调（JNI）。 */
    void nativeHandleClose() {
        if (onCloseHandler != null) {
            onCloseHandler.run();
        }
    }
}
