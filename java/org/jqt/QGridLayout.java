/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 网格布局：封装 C++ 侧的 {@code QGridLayout}，按 行/列 摆放子控件。
 * <pre>
 * QGridLayout grid = new QGridLayout();
 * grid.addWidget(label, 0, 0);
 * grid.addWidget(edit, 0, 1);
 * grid.addWidget(button, 1, 0, 1, 2);   // 跨 1 行 2 列
 * window.setLayout(grid);
 * </pre>
 */
public class QGridLayout extends QLayout {

    public QGridLayout() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    private native long nativeCreate();

    /** 把子控件放到指定 行/列。 */
    public void addWidget(QWidget widget, int row, int col) {
        nativeAddWidget(nativeHandle, widget.nativeHandle(), row, col, 1, 1);
    }

    /** 把子控件放到指定位置并跨越 rowSpan 行、colSpan 列。 */
    public void addWidget(QWidget widget, int row, int col, int rowSpan, int colSpan) {
        nativeAddWidget(nativeHandle, widget.nativeHandle(), row, col, rowSpan, colSpan);
    }
    private native void nativeAddWidget(long handle, long childHandle, int row, int col, int rowSpan, int colSpan);

    /** 子项数量/间距继承自 QLayout（v0.6.0）。 */

    /** 设置列拉伸系数。 */
    public void setColumnStretch(int col, int stretch) {
        nativeSetColumnStretch(nativeHandle, col, stretch);
    }
    private native void nativeSetColumnStretch(long handle, int col, int stretch);

    /** 设置行拉伸系数。 */
    public void setRowStretch(int row, int stretch) {
        nativeSetRowStretch(nativeHandle, row, stretch);
    }
    private native void nativeSetRowStretch(long handle, int row, int stretch);

// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** count（Qt count）。 */
    public int count() {
        return nativeCount(nativeHandle);
    }
    private static native int nativeCount(long nativeHandle);

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

    /** invalidate（Qt invalidate）。 */
    public void invalidate() {
        nativeInvalidate(nativeHandle);
    }
    private static native void nativeInvalidate(long nativeHandle);

    /** minimumHeightForWidth（Qt minimumHeightForWidth）。 */
    public int minimumHeightForWidth(int arg0) {
        return nativeMinimumHeightForWidth(nativeHandle, arg0);
    }
    private static native int nativeMinimumHeightForWidth(long nativeHandle, int arg0);

    /** rowCount（Qt rowCount）。 */
    public int rowCount() {
        return nativeRowCount(nativeHandle);
    }
    private static native int nativeRowCount(long nativeHandle);

    /** setRowMinimumHeight（Qt setRowMinimumHeight）。 */
    public void setRowMinimumHeight(int arg0, int arg1) {
        nativeSetRowMinimumHeight(nativeHandle, arg0, arg1);
    }
    private static native void nativeSetRowMinimumHeight(long nativeHandle, int arg0, int arg1);

    /** setVerticalSpacing（Qt setVerticalSpacing）。 */
    public void setVerticalSpacing(int arg0) {
        nativeSetVerticalSpacing(nativeHandle, arg0);
    }
    private static native void nativeSetVerticalSpacing(long nativeHandle, int arg0);

    /** spacing（Qt spacing）。 */
    public int spacing() {
        return nativeSpacing(nativeHandle);
    }
    private static native int nativeSpacing(long nativeHandle);

    /** verticalSpacing（Qt verticalSpacing）。 */
    public int verticalSpacing() {
        return nativeVerticalSpacing(nativeHandle);
    }
    private static native int nativeVerticalSpacing(long nativeHandle);

}