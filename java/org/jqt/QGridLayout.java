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

    // ---- v1.8.0 L2-B8：列最小宽 / 双向间距 / 原点 / 几何查询（手写直传）----

    /** 原点角（Qt::Corner）：0 左上 / 1 右上 / 2 左下 / 3 右下。 */
    public static final int CORNER_TOP_LEFT = 0;
    public static final int CORNER_TOP_RIGHT = 1;
    public static final int CORNER_BOTTOM_LEFT = 2;
    public static final int CORNER_BOTTOM_RIGHT = 3;

    /** 设置列最小宽度（像素）。 */
    public void setColumnMinimumWidth(int col, int width) {
        nativeSetColumnMinimumWidth(nativeHandle, col, width);
    }
    private static native void nativeSetColumnMinimumWidth(long handle, int col, int width);

    /** 水平间距（像素；未单独设置返回 -1，沿用全局）。 */
    public int horizontalSpacing() { return nativeHorizontalSpacing(nativeHandle); }
    private static native int nativeHorizontalSpacing(long handle);

    /** 设置水平间距（像素）。 */
    public void setHorizontalSpacing(int spacing) { nativeSetHorizontalSpacing(nativeHandle, spacing); }
    private static native void nativeSetHorizontalSpacing(long handle, int spacing);

    /** 原点角（行/列方向起点，见 CORNER_*）。 */
    public int originCorner() { return nativeOriginCorner(nativeHandle); }
    private static native int nativeOriginCorner(long handle);

    /** 设置原点角。 */
    public void setOriginCorner(int corner) { nativeSetOriginCorner(nativeHandle, corner); }
    private static native void nativeSetOriginCorner(long handle, int corner);

    /** 单元格几何 [x, y, w, h]（QGridLayout::cellRect；越界返回 0 矩形）。 */
    public int[] cellRect(int row, int col) { return nativeCellRect(nativeHandle, row, col); }
    private static native int[] nativeCellRect(long handle, int row, int col);

    /**
     * 子项网格位置 [row, col, rowSpan, colSpan]（QGridLayout::getItemPosition 行语义）。
     * index 越界返回全 -1。
     */
    public int[] getItemPosition(int index) { return nativeGetItemPosition(nativeHandle, index); }
    private static native int[] nativeGetItemPosition(long handle, int index);

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