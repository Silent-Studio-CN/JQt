/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 文本标签：封装 C++ 侧的 {@code QLabel}。
 */
public class QLabel extends QWidget {

    public QLabel(String text) {
        nativeHandle = nativeCreate(text);
        registerCleaner();
    }

    private native long nativeCreate(String text);

    /** 修改标签文字。 */
    public void setText(String text) {
        nativeSetText(nativeHandle, text);
    }
    private native void nativeSetText(long handle, String text);

    /** 读取当前文字。 */
    public String text() {
        return nativeText(nativeHandle);
    }
    private native String nativeText(long handle);
}


