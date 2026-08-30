/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 堆叠布局：封装 C++ 侧的 {@code QStackedLayout}，同一时刻只显示一页。
 * <pre>
 * QStackedLayout stack = new QStackedLayout();
 * int page1 = stack.addPage(widgetA);
 * stack.setCurrentIndex(page1);
 * window.setLayout(stack);
 * </pre>
 */
public class QStackedLayout extends QLayout {

    public QStackedLayout() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    private native long nativeCreate();

    /** 追加一页，返回其 index。 */
    public int addPage(QWidget widget) {
        return nativeAddPage(nativeHandle, widget.nativeHandle());
    }
    private native int nativeAddPage(long handle, long childHandle);

    /** 切换到指定页。 */
    public void setCurrentIndex(int index) {
        nativeSetCurrentIndex(nativeHandle, index);
    }
    private native void nativeSetCurrentIndex(long handle, int index);

    /** 当前页 index。 */
    public int currentIndex() {
        return nativeCurrentIndex(nativeHandle);
    }
    private native int nativeCurrentIndex(long handle);

    /** 切换到指定控件所在页。 */
    public void setCurrentWidget(QWidget widget) {
        nativeSetCurrentWidget(nativeHandle, widget.nativeHandle);
    }
    private native void nativeSetCurrentWidget(long handle, long childHandle);

// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** hasHeightForWidth（Qt hasHeightForWidth）。 */
    public boolean hasHeightForWidth() {
        return nativeHasHeightForWidth(nativeHandle);
    }
    private static native boolean nativeHasHeightForWidth(long nativeHandle);

    /** heightForWidth（Qt heightForWidth）。 */
    public int heightForWidth(int arg0) {
        return nativeHeightForWidth(nativeHandle, arg0);
    }
    private static native int nativeHeightForWidth(long nativeHandle, int arg0);

}