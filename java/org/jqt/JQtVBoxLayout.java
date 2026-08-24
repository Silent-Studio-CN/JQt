/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 垂直布局：子控件自上而下排列（封装 C++ 侧 {@code QVBoxLayout}）。
 */
public class JQtVBoxLayout extends JQtLayout {

    public JQtVBoxLayout() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    private native long nativeCreate();
}
