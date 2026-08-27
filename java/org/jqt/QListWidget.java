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
}


