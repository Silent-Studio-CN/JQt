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
}