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
 * 列表视图（QListView）：基于字符串列表的轻量视图（QStringListModel 内部实现）。
 * <p>
 * L1 补全（v0.7.1）：addItem / setItems / spacing / wordWrap / selectionChanged。
 */
public class QListView extends QWidget {

    private final List<Consumer<List<String>>> selectionChangedHandlers = new ArrayList<>();

    /** 创建列表视图。 */
    public QListView() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    private native long nativeCreate();

    /** 追加一项。 */
    public void addItem(String text) {
        nativeAddItem(nativeHandle, text);
    }

    /** 批量设置全部项（QStringListModel::setStringList）。 */
    public void setItems(List<String> items) {
        nativeSetItems(nativeHandle, items.toArray(new String[0]));
    }

    /** 指定行文本（越界返回 null）。 */
    public String item(int row) {
        return nativeItem(nativeHandle, row);
    }

    /** 项数量。 */
    public int count() {
        return nativeCount(nativeHandle);
    }

    /** 清空。 */
    public void clear() {
        nativeClear(nativeHandle);
    }

    /** 项间距（像素；QListView::setSpacing）。 */
    public void setSpacing(int spacing) {
        nativeSetSpacing(nativeHandle, spacing);
    }

    /** 项间距。 */
    public int spacing() {
        return nativeSpacing(nativeHandle);
    }

    /** 自动换行（QListView::setWordWrap）。 */
    public void setWordWrap(boolean wrap) {
        nativeSetWordWrap(nativeHandle, wrap);
    }

    /** 是否自动换行。 */
    public boolean wordWrap() {
        return nativeWordWrap(nativeHandle);
    }

    /** 当前选中项文本（无选中返回 null）。 */
    public String currentItem() {
        return nativeCurrentItem(nativeHandle);
    }

    /** 选中指定项（按文本匹配，未找到不改变）。 */
    public void setCurrentItem(String text) {
        nativeSetCurrentItem(nativeHandle, text);
    }

    /** 选中变化回调（selectionChanged 信号，参数为当前选中项列表）。 */
    public QListView onSelectionChanged(Consumer<List<String>> handler) {
        selectionChangedHandlers.add(handler);
        nativeConnectSelectionChanged(nativeHandle);
        return this;
    }

    void nativeHandleSelectionChanged(String[] selected) {
        List<String> items = new ArrayList<>();
        if (selected != null) {
            for (String s : selected) items.add(s);
        }
        for (Consumer<List<String>> h : selectionChangedHandlers) h.accept(items);
    }

    private native void nativeAddItem(long handle, String text);
    private native void nativeSetItems(long handle, String[] items);
    private native String nativeItem(long handle, int row);
    private native int nativeCount(long handle);
    private native void nativeClear(long handle);
    private native void nativeSetSpacing(long handle, int spacing);
    private native int nativeSpacing(long handle);
    private native void nativeSetWordWrap(long handle, boolean wrap);
    private native boolean nativeWordWrap(long handle);
    private native String nativeCurrentItem(long handle);
    private native void nativeSetCurrentItem(long handle, String text);
    private native void nativeConnectSelectionChanged(long handle);
}
