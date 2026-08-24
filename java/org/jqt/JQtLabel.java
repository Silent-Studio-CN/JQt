/*
 * JQt - Java bindings for Qt.
 * Copyright (c) 2025 SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.
 */
package org.jqt;

/**
 * 文本标签：封装 C++ 侧的 {@code QLabel}。
 */
public class JQtLabel extends JQtWidget {

    public JQtLabel(String text) {
        nativeHandle = nativeCreate(text);
    }

    private native long nativeCreate(String text);

    /** 修改标签文字。 */
    public void setText(String text) {
        nativeSetText(nativeHandle, text);
    }
    private native void nativeSetText(long handle, String text);
}
