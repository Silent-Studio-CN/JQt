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
 * 下拉选择框：封装 C++ 侧的 {@code QComboBox}。
 * <p>
 * 信号槽：
 * <ul>
 *   <li>{@link #onCurrentIndexChanged(Consumer)} — currentIndexChanged 信号（选项切换）</li>
 * </ul>
 */
public class QComboBox extends QWidget {

    private final List<Consumer<Integer>> onCurrentIndexChangedHandlers = new ArrayList<>();

    public QComboBox() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    private native long nativeCreate();

    /** 追加一个选项。 */
    public void addItem(String text) {
        nativeAddItem(nativeHandle, text);
    }
    private native void nativeAddItem(long handle, String text);

    /** 当前选项的索引（无选中时为 -1）。 */
    public int currentIndex() {
        return nativeCurrentIndex(nativeHandle);
    }
    private native int nativeCurrentIndex(long handle);

    /** 当前选项的文本。 */
    public String currentText() {
        return nativeCurrentText(nativeHandle);
    }
    private native String nativeCurrentText(long handle);

    /** 选中指定索引的选项（会触发 onCurrentIndexChanged）。 */
    public void setCurrentIndex(int index) {
        nativeSetCurrentIndex(nativeHandle, index);
    }
    private native void nativeSetCurrentIndex(long handle, int index);

    /** 注册选项切换回调（currentIndexChanged 信号，参数为新索引）。 */
    public QComboBox onCurrentIndexChanged(Consumer<Integer> handler) {
        onCurrentIndexChangedHandlers.add(handler);
        return this;
    }

    /** 由 C++ 侧在选项切换时回调（JNI）。 */
    void nativeHandleCurrentIndexChanged(int index) {
        for (Consumer<Integer> h : onCurrentIndexChangedHandlers) {
            h.accept(index);
        }
    }

    // ---- L1 补全（v0.6.0）----

    /** 清空全部项。 */
    public void clear() { nativeClear(nativeHandle); }
    private static native void nativeClear(long handle);

    /** 项数量。 */
    public int count() { return nativeCount(nativeHandle); }
    private static native int nativeCount(long handle);

    /** 占位提示（未选择时显示）。 */
    public void setPlaceholderText(String text) { nativeSetPlaceholderText(nativeHandle, text); }
    private static native void nativeSetPlaceholderText(long handle, String text);

    /** 占位提示文本。 */
    public String placeholderText() { return nativePlaceholderText(nativeHandle); }
    private static native String nativePlaceholderText(long handle);

    /** 可编辑模式（允许输入自定义文本）。 */
    public void setEditable(boolean editable) { nativeSetEditable(nativeHandle, editable); }
    private static native void nativeSetEditable(long handle, boolean editable);

    /** 是否可编辑。 */
    public boolean isEditable() { return nativeIsEditable(nativeHandle); }
    private static native boolean nativeIsEditable(long handle);

    private final java.util.List<Consumer<Integer>> onActivatedHandlers = new java.util.ArrayList<>();
    private final java.util.List<Consumer<String>> onCurrentTextChangedHandlers = new java.util.ArrayList<>();
    private volatile boolean activatedConnected;
    private volatile boolean currentTextConnected;

    /** 下拉选择回调（activated 信号，参数为选中 index；仅用户操作触发）。 */
    public QComboBox onActivated(Consumer<Integer> handler) {
        onActivatedHandlers.add(handler);
        if (!activatedConnected) {
            activatedConnected = true;
            nativeConnectActivated(nativeHandle);
        }
        return this;
    }

    /** 当前文本变化回调（currentTextChanged 信号，参数为新文本）。 */
    public QComboBox onCurrentTextChanged(Consumer<String> handler) {
        onCurrentTextChangedHandlers.add(handler);
        if (!currentTextConnected) {
            currentTextConnected = true;
            nativeConnectCurrentTextChanged(nativeHandle);
        }
        return this;
    }

    private native void nativeConnectActivated(long handle);
    private native void nativeConnectCurrentTextChanged(long handle);

    /** 由 C++ 侧在用户选择时回调（JNI）。 */
    void nativeHandleActivated(int index) {
        for (Consumer<Integer> h : onActivatedHandlers) {
            h.accept(index);
        }
    }

    /** 由 C++ 侧在当前文本变化时回调（JNI）。 */
    void nativeHandleCurrentTextChanged(String text) {
        for (Consumer<String> h : onCurrentTextChangedHandlers) {
            h.accept(text);
        }
    }

    private final List<Consumer<String>> onEditTextChangedHandlers = new java.util.ArrayList<>();
    private final List<Consumer<Integer>> onHighlightedHandlers = new java.util.ArrayList<>();
    private volatile boolean editConn, hlConn;

    /** 编辑文本变化回调（可编辑模式下输入，参数为新文本）。 */
    public QComboBox onEditTextChanged(Consumer<String> handler) {
        onEditTextChangedHandlers.add(handler);
        if (!editConn) { editConn = true; nativeConnectEditTextChanged(nativeHandle); }
        return this;
    }

    /** 高亮项变化回调（键盘/悬停，参数为 index）。 */
    public QComboBox onHighlighted(Consumer<Integer> handler) {
        onHighlightedHandlers.add(handler);
        if (!hlConn) { hlConn = true; nativeConnectHighlighted(nativeHandle); }
        return this;
    }

    private native void nativeConnectEditTextChanged(long handle);
    private native void nativeConnectHighlighted(long handle);

    void nativeHandleEditTextChanged(String text) {
        for (Consumer<String> h : onEditTextChangedHandlers) h.accept(text);
    }
    void nativeHandleHighlighted(int index) {
        for (Consumer<Integer> h : onHighlightedHandlers) h.accept(index);
    }

// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** clearEditText（Qt clearEditText）。 */
    public void clearEditText() {
        nativeClearEditText(nativeHandle);
    }
    private static native void nativeClearEditText(long nativeHandle);

    /** duplicatesEnabled（Qt duplicatesEnabled）。 */
    public boolean duplicatesEnabled() {
        return nativeDuplicatesEnabled(nativeHandle);
    }
    private static native boolean nativeDuplicatesEnabled(long nativeHandle);

    /** hasFrame（Qt hasFrame）。 */
    public boolean hasFrame() {
        return nativeHasFrame(nativeHandle);
    }
    private static native boolean nativeHasFrame(long nativeHandle);

    /** insertSeparator（Qt insertSeparator）。 */
    public void insertSeparator(int arg0) {
        nativeInsertSeparator(nativeHandle, arg0);
    }
    private static native void nativeInsertSeparator(long nativeHandle, int arg0);

    /** maxCount（Qt maxCount）。 */
    public int maxCount() {
        return nativeMaxCount(nativeHandle);
    }
    private static native int nativeMaxCount(long nativeHandle);

    /** maxVisibleItems（Qt maxVisibleItems）。 */
    public int maxVisibleItems() {
        return nativeMaxVisibleItems(nativeHandle);
    }
    private static native int nativeMaxVisibleItems(long nativeHandle);

    /** minimumContentsLength（Qt minimumContentsLength）。 */
    public int minimumContentsLength() {
        return nativeMinimumContentsLength(nativeHandle);
    }
    private static native int nativeMinimumContentsLength(long nativeHandle);

    /** modelColumn（Qt modelColumn）。 */
    public int modelColumn() {
        return nativeModelColumn(nativeHandle);
    }
    private static native int nativeModelColumn(long nativeHandle);

    /** removeItem（Qt removeItem）。 */
    public void removeItem(int arg0) {
        nativeRemoveItem(nativeHandle, arg0);
    }
    private static native void nativeRemoveItem(long nativeHandle, int arg0);

    /** setCurrentText（Qt setCurrentText）。 */
    public void setCurrentText(String arg0) {
        nativeSetCurrentText(nativeHandle, arg0);
    }
    private static native void nativeSetCurrentText(long nativeHandle, String arg0);

    /** setDuplicatesEnabled（Qt setDuplicatesEnabled）。 */
    public void setDuplicatesEnabled(boolean arg0) {
        nativeSetDuplicatesEnabled(nativeHandle, arg0);
    }
    private static native void nativeSetDuplicatesEnabled(long nativeHandle, boolean arg0);

    /** setEditText（Qt setEditText）。 */
    public void setEditText(String arg0) {
        nativeSetEditText(nativeHandle, arg0);
    }
    private static native void nativeSetEditText(long nativeHandle, String arg0);

    /** setFrame（Qt setFrame）。 */
    public void setFrame(boolean arg0) {
        nativeSetFrame(nativeHandle, arg0);
    }
    private static native void nativeSetFrame(long nativeHandle, boolean arg0);

    /** setItemText（Qt setItemText）。 */
    public void setItemText(int arg0, String arg1) {
        nativeSetItemText(nativeHandle, arg0, arg1);
    }
    private static native void nativeSetItemText(long nativeHandle, int arg0, String arg1);

    /** setMaxCount（Qt setMaxCount）。 */
    public void setMaxCount(int arg0) {
        nativeSetMaxCount(nativeHandle, arg0);
    }
    private static native void nativeSetMaxCount(long nativeHandle, int arg0);

    /** setMaxVisibleItems（Qt setMaxVisibleItems）。 */
    public void setMaxVisibleItems(int arg0) {
        nativeSetMaxVisibleItems(nativeHandle, arg0);
    }
    private static native void nativeSetMaxVisibleItems(long nativeHandle, int arg0);

    /** setMinimumContentsLength（Qt setMinimumContentsLength）。 */
    public void setMinimumContentsLength(int arg0) {
        nativeSetMinimumContentsLength(nativeHandle, arg0);
    }
    private static native void nativeSetMinimumContentsLength(long nativeHandle, int arg0);

    /** setModelColumn（Qt setModelColumn）。 */
    public void setModelColumn(int arg0) {
        nativeSetModelColumn(nativeHandle, arg0);
    }
    private static native void nativeSetModelColumn(long nativeHandle, int arg0);

    /** showPopup（Qt showPopup）。 */
    public void showPopup() {
        nativeShowPopup(nativeHandle);
    }
    private static native void nativeShowPopup(long nativeHandle);

}