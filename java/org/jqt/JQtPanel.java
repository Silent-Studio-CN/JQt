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
public class JQtPanel extends JQtWidget {

    public JQtPanel() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    private native long nativeCreate();

    /** 直接添加子控件（无布局时自动摆放）。 */
    public void addWidget(JQtWidget child) {
        nativeAddWidget(nativeHandle, child.nativeHandle());
    }
    private native void nativeAddWidget(long handle, long childHandle);
}