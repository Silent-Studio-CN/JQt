/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 表格控件：封装 C++ 侧的 {@code QTableWidget}。
 * <p>
 * 信号槽：
 * <ul>
 *   <li>{@link #onCellClicked(BiConsumer)} — cellClicked 信号（参数为 行, 列）</li>
 *   <li>{@link #onCurrentRowChanged(Consumer)} — 当前行切换（参数为新行号）</li>
 * </ul>
 */
public class QTableWidget extends QWidget {

    private final List<BiConsumer<Integer, Integer>> onCellClickedHandlers = new ArrayList<>();
    private final List<Consumer<Integer>> onCurrentRowChangedHandlers = new ArrayList<>();

    /** 创建 rows × cols 的表格。 */
    public QTableWidget(int rows, int cols) {
        nativeHandle = nativeCreate(rows, cols);
        registerCleaner();
    }

    private native long nativeCreate(int rows, int cols);

    /** 设置单元格文本。 */
    public void setItemText(int row, int col, String text) {
        nativeSetItemText(nativeHandle, row, col, text);
    }
    private native void nativeSetItemText(long handle, int row, int col, String text);

    /** 读取单元格文本（空单元格返回 null）。 */
    public String itemText(int row, int col) {
        return nativeItemText(nativeHandle, row, col);
    }
    private native String nativeItemText(long handle, int row, int col);

    /** 设置列表头。 */
    public void setColumnHeaders(String[] headers) {
        nativeSetColumnHeaders(nativeHandle, headers);
    }
    private native void nativeSetColumnHeaders(long handle, String[] headers);

    public void setRowCount(int rows) {
        nativeSetRowCount(nativeHandle, rows);
    }
    private native void nativeSetRowCount(long handle, int rows);

    public void setColumnCount(int cols) {
        nativeSetColumnCount(nativeHandle, cols);
    }
    private native void nativeSetColumnCount(long handle, int cols);

    public int rowCount() {
        return nativeRowCount(nativeHandle);
    }
    private native int nativeRowCount(long handle);

    public int columnCount() {
        return nativeColumnCount(nativeHandle);
    }
    private native int nativeColumnCount(long handle);

    /** 设置列宽。 */
    public void setColumnWidth(int col, int width) {
        nativeSetColumnWidth(nativeHandle, col, width);
    }
    private native void nativeSetColumnWidth(long handle, int col, int width);

    /** 设置行高。 */
    public void setRowHeight(int row, int height) {
        nativeSetRowHeight(nativeHandle, row, height);
    }
    private native void nativeSetRowHeight(long handle, int row, int height);

    /** 列宽自适应内容。 */
    public void resizeColumnsToContents() {
        nativeResizeColumnsToContents(nativeHandle);
    }
    private native void nativeResizeColumnsToContents(long handle);

    /** 清空所有单元格内容（保留行列数）。 */
    public void clearContents() {
        nativeClearContents(nativeHandle);
    }
    private native void nativeClearContents(long handle);

    /** 当前行号（无选中时为 -1）。 */
    public int currentRow() {
        return nativeCurrentRow(nativeHandle);
    }
    private native int nativeCurrentRow(long handle);

    /** 注册单元格点击回调（cellClicked 信号，参数为 行, 列）。 */
    public QTableWidget onCellClicked(BiConsumer<Integer, Integer> handler) {
        onCellClickedHandlers.add(handler);
        return this;
    }

    /** 注册当前行切换回调（参数为新行号）。 */
    public QTableWidget onCurrentRowChanged(Consumer<Integer> handler) {
        onCurrentRowChangedHandlers.add(handler);
        return this;
    }

    /** 由 C++ 侧在点击单元格时回调（JNI）。 */
    void nativeHandleCellClicked(int row, int col) {
        for (BiConsumer<Integer, Integer> h : onCellClickedHandlers) {
            h.accept(row, col);
        }
    }

    /** 由 C++ 侧在当前行切换时回调（JNI）。 */
    void nativeHandleCurrentRowChanged(int row) {
        for (Consumer<Integer> h : onCurrentRowChangedHandlers) {
            h.accept(row);
        }
    }

// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** clear（Qt clear）。 */
    public void clear() {
        nativeClear(nativeHandle);
    }
    private static native void nativeClear(long nativeHandle);

    /** insertColumn（Qt insertColumn）。 */
    public void insertColumn(int arg0) {
        nativeInsertColumn(nativeHandle, arg0);
    }
    private static native void nativeInsertColumn(long nativeHandle, int arg0);

    /** insertRow（Qt insertRow）。 */
    public void insertRow(int arg0) {
        nativeInsertRow(nativeHandle, arg0);
    }
    private static native void nativeInsertRow(long nativeHandle, int arg0);

    /** removeCellWidget（Qt removeCellWidget）。 */
    public void removeCellWidget(int arg0, int arg1) {
        nativeRemoveCellWidget(nativeHandle, arg0, arg1);
    }
    private static native void nativeRemoveCellWidget(long nativeHandle, int arg0, int arg1);

    /** removeColumn（Qt removeColumn）。 */
    public void removeColumn(int arg0) {
        nativeRemoveColumn(nativeHandle, arg0);
    }
    private static native void nativeRemoveColumn(long nativeHandle, int arg0);

    /** removeRow（Qt removeRow）。 */
    public void removeRow(int arg0) {
        nativeRemoveRow(nativeHandle, arg0);
    }
    private static native void nativeRemoveRow(long nativeHandle, int arg0);

    /** visualColumn（Qt visualColumn）。 */
    public int visualColumn(int arg0) {
        return nativeVisualColumn(nativeHandle, arg0);
    }
    private static native int nativeVisualColumn(long nativeHandle, int arg0);

    /** visualRow（Qt visualRow）。 */
    public int visualRow(int arg0) {
        return nativeVisualRow(nativeHandle, arg0);
    }
    private static native int nativeVisualRow(long nativeHandle, int arg0);

}