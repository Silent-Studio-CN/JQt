/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 布局管理器基类（Phase 3）：封装 C++ 侧的 {@code QBoxLayout}。
 * <p>
 * 用法：
 * <pre>
 * JQtVBoxLayout vbox = new JQtVBoxLayout();
 * vbox.addWidget(label);
 * vbox.addWidget(button);
 * window.setLayout(vbox);
 * </pre>
 * 布局接管子控件的位置与大小；控件加入布局后不要再调用
 * {@link JQtWindow#addWidget(JQtWidget)} 重复添加。
 */
public abstract class JQtLayout {

    /** C++ 侧布局对象指针。 */
    protected long nativeHandle;

    /** 布局是否已在 C++ 侧创建。 */
    public boolean isCreated() {
        return nativeHandle != 0;
    }

    /** C++ 侧布局对象指针（仅供内部 / 高级用法）。 */
    public long nativeHandle() {
        return nativeHandle;
    }

    /** 把子控件加入布局（Qt 会自动接管其排列）。 */
    public void addWidget(JQtWidget widget) {
        nativeAddWidget(nativeHandle, widget.nativeHandle());
    }
    protected native void nativeAddWidget(long handle, long childHandle);

    /** 设置控件之间的间距（像素）。 */
    public void setSpacing(int spacing) {
        nativeSetSpacing(nativeHandle, spacing);
    }
    protected native void nativeSetSpacing(long handle, int spacing);

    /**
     * 添加弹性空间（stretch 因子，越大占据越多剩余空间）。
     * 例如 {@code vbox.addStretch(1)} 会把控件推向顶部。
     */
    public void addStretch(int stretch) {
        nativeAddStretch(nativeHandle, stretch);
    }
    protected native void nativeAddStretch(long handle, int stretch);
}
