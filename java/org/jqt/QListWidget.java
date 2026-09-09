/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 列表控件：封装 C++ 侧的 {@code QListWidget}。
 * <p>
 * 信号槽：
 * <ul>
 *   <li>{@link #onItemClicked(Consumer)} — itemClicked 信号（点击某一项，参数为行号）</li>
 *   <li>{@link #onCurrentRowChanged(Consumer)} — currentRowChanged 信号（当前行切换）</li>
 * </ul>
 */
public class QListWidget extends QWidget {

    private final List<Consumer<Integer>> onItemClickedHandlers = new ArrayList<>();
    private final List<Consumer<Integer>> onCurrentRowChangedHandlers = new ArrayList<>();
    private final List<Consumer<List<String>>> onItemChangedHandlers = new ArrayList<>();

    public QListWidget() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    private native long nativeCreate();

    /** 追加一个列表项。 */
    public void addItem(String text) {
        nativeAddItem(nativeHandle, text);
    }
    private native void nativeAddItem(long handle, String text);

    /** 当前行号（无选中时为 -1）。 */
    public int currentRow() {
        return nativeCurrentRow(nativeHandle);
    }
    private native int nativeCurrentRow(long handle);

    /** 选中指定行（触发 onCurrentRowChanged）。 */
    public void setCurrentRow(int row) {
        nativeSetCurrentRow(nativeHandle, row);
    }
    private native void nativeSetCurrentRow(long handle, int row);

    /** 注册点击回调（itemClicked 信号，参数为行号）。 */
    public QListWidget onItemClicked(Consumer<Integer> handler) {
        onItemClickedHandlers.add(handler);
        return this;
    }

    /** 注册当前行切换回调（currentRowChanged 信号，参数为新行号）。 */
    public QListWidget onCurrentRowChanged(Consumer<Integer> handler) {
        onCurrentRowChangedHandlers.add(handler);
        return this;
    }

    /**
     * 注册列表项文本修改回调（itemChanged 信号，参数为 [行号, 新文本]）。
     * 通过 setItem(row, text) 修改时触发（QListWidget::itemChanged 语义）。
     */
    public QListWidget onItemChanged(Consumer<List<String>> handler) {
        onItemChangedHandlers.add(handler);
        nativeConnectItemChanged(nativeHandle);
        return this;
    }
    private native void nativeConnectItemChanged(long handle);

    /** 由 C++ 侧在列表项文本修改时回调（JNI，v0.8.0）。 */
    void nativeHandleItemChanged(int row, String text) {
        for (Consumer<List<String>> h : onItemChangedHandlers) {
            List<String> ev = new java.util.ArrayList<>();
            ev.add(String.valueOf(row));
            ev.add(text);
            h.accept(ev);
        }
    }

    /** 返回指定文本项所在行号（未找到返回 -1；QListWidget::row 简化版）。 */
    public int row(String itemText) {
        return nativeRow(nativeHandle, itemText);
    }
    private static native int nativeRow(long handle, String itemText);

    /** 由 C++ 侧在点击列表项时回调（JNI）。 */
    void nativeHandleItemClicked(int row) {
        for (Consumer<Integer> h : onItemClickedHandlers) {
            h.accept(row);
        }
    }

    /** 由 C++ 侧在当前行切换时回调（JNI）。 */
    void nativeHandleCurrentRowChanged(int row) {
        for (Consumer<Integer> h : onCurrentRowChangedHandlers) {
            h.accept(row);
        }
    }

    // ---- L1 补全（v0.6.0）----

    /** 清空全部项。 */
    public void clear() { nativeClear(nativeHandle); }
    private static native void nativeClear(long handle);

    /** 项数量。 */
    public int count() { return nativeCount(nativeHandle); }
    private static native int nativeCount(long handle);

    /** 指定行文本（越界返回 null）。 */
    public String item(int row) { return nativeItem(nativeHandle, row); }
    private static native String nativeItem(long handle, int row);

    // ---- L1 补全（v0.6.0）----

    /** 当前选中文本（无选中返回空串）。 */
    public String currentText() { return nativeCurrentText(nativeHandle); }
    private static native String nativeCurrentText(long handle);

    private final List<Consumer<Integer>> onItemDoubleClickedHandlers = new ArrayList<>();
    private final List<Consumer<Integer>> onItemActivatedHandlers = new ArrayList<>();
    private final List<Consumer<String>> onCurrentTextChangedHandlers = new ArrayList<>();
    private volatile boolean dblConn, actConn, curTxtConn;

    /** 双击回调（参数为行号）。 */
    public QListWidget onItemDoubleClicked(Consumer<Integer> handler) {
        onItemDoubleClickedHandlers.add(handler);
        if (!dblConn) { dblConn = true; nativeConnectItemDoubleClicked(nativeHandle); }
        return this;
    }

    /** 激活回调（双击/回车，参数为行号）。 */
    public QListWidget onItemActivated(Consumer<Integer> handler) {
        onItemActivatedHandlers.add(handler);
        if (!actConn) { actConn = true; nativeConnectItemActivated(nativeHandle); }
        return this;
    }

    /** 当前文本变化回调（参数为新文本）。 */
    public QListWidget onCurrentTextChanged(Consumer<String> handler) {
        onCurrentTextChangedHandlers.add(handler);
        if (!curTxtConn) { curTxtConn = true; nativeConnectCurrentTextChanged(nativeHandle); }
        return this;
    }

    private native void nativeConnectItemDoubleClicked(long handle);
    private native void nativeConnectItemActivated(long handle);
    private native void nativeConnectCurrentTextChanged(long handle);

    void nativeHandleItemDoubleClicked(int row) {
        for (Consumer<Integer> h : onItemDoubleClickedHandlers) h.accept(row);
    }
    void nativeHandleItemActivated(int row) {
        for (Consumer<Integer> h : onItemActivatedHandlers) h.accept(row);
    }
    void nativeHandleCurrentTextChanged(String text) {
        for (Consumer<String> h : onCurrentTextChangedHandlers) h.accept(text);
    }

    private final List<Consumer<Integer>> onItemPressedHandlers = new ArrayList<>();
    private final List<Runnable> onItemSelectionChangedHandlers = new ArrayList<>();
    private volatile boolean pressConn, selConn;

    /** 按下回调（参数为行号）。 */
    public QListWidget onItemPressed(Consumer<Integer> handler) {
        onItemPressedHandlers.add(handler);
        if (!pressConn) { pressConn = true; nativeConnectItemPressed(nativeHandle); }
        return this;
    }

    /** 选区变化回调。 */
    public QListWidget onItemSelectionChanged(Runnable handler) {
        onItemSelectionChangedHandlers.add(handler);
        if (!selConn) { selConn = true; nativeConnectItemSelectionChanged(nativeHandle); }
        return this;
    }

    private native void nativeConnectItemPressed(long handle);
    private native void nativeConnectItemSelectionChanged(long handle);

    void nativeHandleItemPressed(int row) {
        for (Consumer<Integer> h : onItemPressedHandlers) h.accept(row);
    }
    void nativeHandleItemSelectionChanged() {
        for (Runnable h : onItemSelectionChangedHandlers) h.run();
    }

    /** 当前项行号（无选中返回 -1）。 */
    public int currentItem() { return nativeCurrentItem(nativeHandle); }
    private static native int nativeCurrentItem(long handle);

    private final List<Consumer<Integer>> onCurrentItemChangedHandlers = new ArrayList<>();
    private volatile boolean curItemConn;

    /** 当前项变化回调（参数为新行号，取消选中为 -1）。 */
    public QListWidget onCurrentItemChanged(Consumer<Integer> handler) {
        onCurrentItemChangedHandlers.add(handler);
        if (!curItemConn) { curItemConn = true; nativeConnectCurrentItemChanged(nativeHandle); }
        return this;
    }

    private native void nativeConnectCurrentItemChanged(long handle);

    void nativeHandleCurrentItemChanged(int row) {
        for (Consumer<Integer> h : onCurrentItemChangedHandlers) h.accept(row);
    }

    private final List<Consumer<Integer>> onItemEnteredHandlers = new ArrayList<>();
    private volatile boolean entConn;

    /** 悬停项回调（参数为行号）。 */
    public QListWidget onItemEntered(Consumer<Integer> handler) {
        onItemEnteredHandlers.add(handler);
        if (!entConn) { entConn = true; nativeConnectItemEntered(nativeHandle); }
        return this;
    }

    private native void nativeConnectItemEntered(long handle);

    void nativeHandleItemEntered(int row) {
        for (Consumer<Integer> h : onItemEnteredHandlers) h.accept(row);
    
    }
    // ---- 值对象批：图标 ----

    /** 设置行的图标。 */
    public void setItemIcon(int row, QIcon icon) {
        if (icon == null || icon.isNull()) return;
        long pm = icon.pixmap().nativeHandle();
        if (pm != 0) nativeSetItemIcon(nativeHandle, row, pm);
    }
    private native void nativeSetItemIcon(long handle, int row, long pixmapHandle);

    // ---- v1.8.0 L1-100：列表项批量/删除/选区（行语义）----

    /** 批量追加项（addItems；内部逐条 {@link #addItem(String)}，null 元素跳过）。 */
    public void addItems(List<String> items) {
        if (items == null) {
            return;
        }
        for (String t : items) {
            if (t != null) {
                addItem(t);
            }
        }
    }

    /** 在指定行插入项（QListWidget::insertItem(int, String)）。 */
    public void insertItem(int row, String text) {
        nativeInsertItem(nativeHandle, row, text);
    }
    private static native void nativeInsertItem(long handle, int row, String text);

    /** 删除指定行（行语义 removeItem：以 takeItem + 销毁实现；越界无操作）。 */
    public void removeItem(int row) {
        nativeRemoveItem(nativeHandle, row);
    }
    private static native void nativeRemoveItem(long handle, int row);

    /**
     * 摘除指定行并返回其文本（行语义 takeItem：项随之销毁，无句柄暴露；越界返回 null）。
     * 与 {@link #removeItem(int)} 行为等价，保留以对齐 Qt 命名。
     */
    public String takeItem(int row) {
        return nativeTakeItem(nativeHandle, row);
    }
    private static native String nativeTakeItem(long handle, int row);

    /** 选中指定行（setCurrentItem 行语义，等同 {@link #setCurrentRow(int)}）。 */
    public void setCurrentItem(int row) {
        setCurrentRow(row);
    }

    /** 全部选中行号（selectedItems 行语义；无选区返回空列表）。 */
    public List<Integer> selectedItems() {
        int[] rows = nativeSelectedItems(nativeHandle);
        List<Integer> out = new ArrayList<>(rows.length);
        for (int r : rows) {
            out.add(r);
        }
        return out;
    }
    private static native int[] nativeSelectedItems(long handle);

    /** 按文本排序（QListWidget::sortItems，升序）。 */
    public void sortItems() {
        nativeSortItems(nativeHandle);
    }
    private static native void nativeSortItems(long handle);

    // ---- v1.8.0 L2-B7：查找 / 命中 / 选择模式 / 滚动与编辑器（行语义）----

    /** 选择模式（QAbstractItemView::SelectionMode）：0 禁止 / 1 单选 / 2 多选 / 3 扩展 / 4 连续。 */
    public static final int NO_SELECTION = 0;
    public static final int SINGLE_SELECTION = 1;
    public static final int MULTI_SELECTION = 2;
    public static final int EXTENDED_SELECTION = 3;
    public static final int CONTIGUOUS_SELECTION = 4;

    /** 查找文本完全匹配的行（区分大小写；QListWidget::findItems 简化）。 */
    public java.util.List<Integer> findItems(String text) {
        return findItems(text, true);
    }

    /**
     * 查找文本匹配的行（QListWidget::findItems 简化；完全匹配语义）。
     * @param caseSensitive 是否区分大小写
     */
    public java.util.List<Integer> findItems(String text, boolean caseSensitive) {
        java.util.List<Integer> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        int[] rows = nativeFindItems(nativeHandle, text, caseSensitive);
        for (int r : rows) {
            out.add(r);
        }
        return out;
    }
    private static native int[] nativeFindItems(long handle, String text, boolean caseSensitive);

    /** 坐标处的行号（QListWidget::itemAt(QPoint) 行语义；无项返回 -1）。 */
    public int itemAt(int x, int y) {
        return nativeItemAt(nativeHandle, x, y);
    }
    private static native int nativeItemAt(long handle, int x, int y);

    /** 设置选择模式（见本类 *_SELECTION 常量）。 */
    public void setSelectionMode(int mode) {
        nativeSetSelectionMode(nativeHandle, mode);
    }
    private static native void nativeSetSelectionMode(long handle, int mode);

    /** 滚动到指定行可见（QListWidget::scrollToItem 行语义，EnsureVisible）。 */
    public void scrollToItem(int row) {
        nativeScrollToItem(nativeHandle, row);
    }
    private static native void nativeScrollToItem(long handle, int row);

    /** 指定行的可视区域 [x, y, w, h]（QListWidget::visualItemRect 行语义；不可见行返回 0 矩形）。 */
    public int[] visualItemRect(int row) {
        return nativeVisualItemRect(nativeHandle, row);
    }
    private static native int[] nativeVisualItemRect(long handle, int row);

    /** 为指定行挂自定义编辑控件（QListWidget::setItemWidget 行语义）。 */
    public void setItemWidget(int row, QWidget widget) {
        if (widget == null) {
            return;
        }
        nativeSetItemWidget(nativeHandle, row, widget.nativeHandle());
    }
    private static native void nativeSetItemWidget(long handle, int row, long widgetHandle);

    /** 移除指定行的编辑控件（保留行与文本；QListWidget::removeItemWidget 行语义）。 */
    public void removeItemWidget(int row) {
        nativeRemoveItemWidget(nativeHandle, row);
    }
    private static native void nativeRemoveItemWidget(long handle, int row);

    /** 就地编辑模式开启（QListWidget::openPersistentEditor 行语义）。 */
    public void openPersistentEditor(int row) {
        nativeOpenPersistentEditor(nativeHandle, row);
    }
    private static native void nativeOpenPersistentEditor(long handle, int row);

    /** 就地编辑模式关闭。 */
    public void closePersistentEditor(int row) {
        nativeClosePersistentEditor(nativeHandle, row);
    }
    private static native void nativeClosePersistentEditor(long handle, int row);

// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** isSortingEnabled（Qt isSortingEnabled）。 */
    public boolean isSortingEnabled() {
        return nativeIsSortingEnabled(nativeHandle);
    }
    private static native boolean nativeIsSortingEnabled(long nativeHandle);

    /** setSortingEnabled（Qt setSortingEnabled）。 */
    public void setSortingEnabled(boolean arg0) {
        nativeSetSortingEnabled(nativeHandle, arg0);
    }
    private static native void nativeSetSortingEnabled(long nativeHandle, boolean arg0);

}