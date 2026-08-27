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
}
