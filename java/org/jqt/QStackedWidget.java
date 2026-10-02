/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 堆叠控件（Qt {@code QStackedWidget}）：一叠页面，同一时刻只显示一个。
 * <pre>
 * QStackedWidget stack = new QStackedWidget();     // v1.9.1:补上 native 创建入口(J5)
 * stack.addWidget(pageA);
 * stack.addWidget(pageB);
 * stack.setCurrentIndex(1);
 * </pre>
 * 之前该类只能由原生侧句柄构造（{@code QStackedWidget(long)}），Java 侧无法直接建，
 * 现已提供无参构造器（保留旧构造器以兼容既有代码）。
 */
public class QStackedWidget extends QWidget {

    /** 创建空的堆叠控件。 */
    public QStackedWidget() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    /** 由既有原生句柄构造（兼容路径，例如从布局里取回控件）。 */
    public QStackedWidget(long nativeHandle) {
        this.nativeHandle = nativeHandle;
    }

    private native long nativeCreate();

    /** 追加一页。 */
    public void addWidget(QWidget w) { nativeAddWidget(nativeHandle, w.nativeHandle); }
    private static native void nativeAddWidget(long handle, long widgetHandle);

    /** 移除一页（不销毁控件）。 */
    public void removeWidget(QWidget w) { nativeRemoveWidget(nativeHandle, w.nativeHandle); }
    private static native void nativeRemoveWidget(long handle, long widgetHandle);

    /** 页数。 */
    public int count() { return nativeCount(nativeHandle); }
    private static native int nativeCount(long nativeHandle);

    /** 当前页索引（无页返回 -1）。 */
    public int currentIndex() { return nativeCurrentIndex(nativeHandle); }
    private static native int nativeCurrentIndex(long handle);

    /** 切换当前页。 */
    public void setCurrentIndex(int index) { nativeSetCurrentIndex(nativeHandle, index); }
    private static native void nativeSetCurrentIndex(long handle, int index);

    /** 指定控件的页索引（不在其中返回 -1）。 */
    public int indexOf(QWidget w) { return nativeIndexOf(nativeHandle, w.nativeHandle); }
    private static native int nativeIndexOf(long handle, long widgetHandle);

    /** 指定索引处的控件句柄（越界返回 0）。 */
    public long widgetHandleAt(int index) { return nativeWidgetAt(nativeHandle, index); }
    private static native long nativeWidgetAt(long handle, int index);
}
