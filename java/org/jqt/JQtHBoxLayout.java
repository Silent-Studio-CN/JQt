/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 水平布局：子控件自左向右排列（封装 C++ 侧 {@code QHBoxLayout}）。
 */
public class JQtHBoxLayout extends JQtLayout {

    public JQtHBoxLayout() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    private native long nativeCreate();
}
