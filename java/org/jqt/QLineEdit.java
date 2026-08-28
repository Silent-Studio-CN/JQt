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
 * 单行文本输入框：封装 C++ 侧的 {@code QLineEdit}。
 * <p>
 * 信号槽：
 * <ul>
 *   <li>{@link #onTextChanged(Consumer)} — textChanged 信号（内容每次变化）</li>
 *   <li>{@link #onReturnPressed(Runnable)} — returnPressed 信号（回车确认）</li>
 * </ul>
 */
public class QLineEdit extends QWidget {

    private final List<Consumer<String>> onTextChangedHandlers = new ArrayList<>();
    private final List<Runnable> onReturnPressedHandlers = new ArrayList<>();

    public QLineEdit(String text) {
        nativeHandle = nativeCreate(text);
        registerCleaner();
    }

    private native long nativeCreate(String text);

    /** 当前文本内容。 */
    public String text() {
        return nativeText(nativeHandle);
    }
    private native String nativeText(long handle);

    /** 设置文本内容（会触发 onTextChanged）。 */
    public void setText(String text) {
        nativeSetText(nativeHandle, text);
    }
    private native void nativeSetText(long handle, String text);

    /** 占位提示文字（输入为空时灰色显示）。 */
    public void setPlaceholderText(String text) {
        nativeSetPlaceholderText(nativeHandle, text);
    }
    private native void nativeSetPlaceholderText(long handle, String text);

    /** 当前占位提示文本（输入为空时显示）。 */
    public String placeholderText() {
        return nativePlaceholderText(nativeHandle);
    }
    private native String nativePlaceholderText(long handle);

    /** 注册文本变化回调（textChanged 信号，参数为最新文本）。 */
    public QLineEdit onTextChanged(Consumer<String> handler) {
        onTextChangedHandlers.add(handler);
        return this;
    }

    /** 注册回车确认回调（returnPressed 信号）。 */
    public QLineEdit onReturnPressed(Runnable handler) {
        onReturnPressedHandlers.add(handler);
        return this;
    }

    /** 由 C++ 侧在文本变化时回调（JNI）。 */
    void nativeHandleTextChanged(String text) {
        for (Consumer<String> h : onTextChangedHandlers) {
            h.accept(text);
        }
    }

    /** 由 C++ 侧在回车时回调（JNI）。 */
    void nativeHandleReturnPressed() {
        for (Runnable h : onReturnPressedHandlers) {
            h.run();
        }
    }

    // ---- L1 补全（v0.6.0）----

    /** 清空文本。 */
    public void clear() { nativeClear(nativeHandle); }
    private static native void nativeClear(long handle);

    /** 复制选中文本到剪贴板。 */
    public void copy() { nativeCopy(nativeHandle); }
    private static native void nativeCopy(long handle);

    /** 剪切选中文本。 */
    public void cut() { nativeCut(nativeHandle); }
    private static native void nativeCut(long handle);

    /** 粘贴剪贴板。 */
    public void paste() { nativePaste(nativeHandle); }
    private static native void nativePaste(long handle);

    /** 撤销。 */
    public void undo() { nativeUndo(nativeHandle); }
    private static native void nativeUndo(long handle);

    /** 重做。 */
    public void redo() { nativeRedo(nativeHandle); }
    private static native void nativeRedo(long handle);

    /** 全选。 */
    public void selectAll() { nativeSelectAll(nativeHandle); }
    private static native void nativeSelectAll(long handle);

    /** 在光标处插入文本。 */
    public void insert(String text) { nativeInsert(nativeHandle, text); }
    private static native void nativeInsert(long handle, String text);

    /**
     * 显示模式：0 正常 / 1 密码（圆点）/ 2 不显示 / 3 密码（输入时短暂可见）。
     */
    public void setEchoMode(int mode) { nativeSetEchoMode(nativeHandle, mode); }
    private static native void nativeSetEchoMode(long handle, int mode);

    /** 当前显示模式。 */
    public int echoMode() { return nativeEchoMode(nativeHandle); }
    private static native int nativeEchoMode(long handle);

    /** 最大输入长度（0 = 不限）。 */
    public void setMaxLength(int max) { nativeSetMaxLength(nativeHandle, max); }
    private static native void nativeSetMaxLength(long handle, int max);

    /** 最大输入长度。 */
    public int maxLength() { return nativeMaxLength(nativeHandle); }
    private static native int nativeMaxLength(long handle);

    /** 文本对齐：1 左 / 2 右 / 4 居中。 */
    public void setAlignment(int alignment) { nativeSetAlignment(nativeHandle, alignment); }
    private static native void nativeSetAlignment(long handle, int alignment);

    /** 文本对齐。 */
    public int alignment() { return nativeAlignment(nativeHandle); }
    private static native int nativeAlignment(long handle);

    /** 只读（可选中不可编辑）。 */
    public void setReadOnly(boolean readOnly) { nativeSetReadOnly(nativeHandle, readOnly); }
    private static native void nativeSetReadOnly(long handle, boolean readOnly);

    /** 是否只读。 */
    public boolean isReadOnly() { return nativeIsReadOnly(nativeHandle); }
    private static native boolean nativeIsReadOnly(long handle);

    private final List<Runnable> onEditingFinishedHandlers = new ArrayList<>();
    private final List<Consumer<String>> onTextEditedHandlers = new ArrayList<>();
    private volatile boolean editingConnected;
    private volatile boolean textEditedConnected;

    /** 编辑完成回调（回车或失焦，参数无）。 */
    public QLineEdit onEditingFinished(Runnable handler) {
        onEditingFinishedHandlers.add(handler);
        if (!editingConnected) {
            editingConnected = true;
            nativeConnectEditingFinished(nativeHandle);
        }
        return this;
    }

    /** 用户编辑回调（textEdited 信号，仅用户输入触发，参数为当前文本）。 */
    public QLineEdit onTextEdited(Consumer<String> handler) {
        onTextEditedHandlers.add(handler);
        if (!textEditedConnected) {
            textEditedConnected = true;
            nativeConnectTextEdited(nativeHandle);
        }
        return this;
    }

    private native void nativeConnectEditingFinished(long handle);
    private native void nativeConnectTextEdited(long handle);

    /** 由 C++ 侧在编辑完成时回调（JNI）。 */
    void nativeHandleEditingFinished() {
        for (Runnable h : onEditingFinishedHandlers) {
            h.run();
        }
    }

    /** 由 C++ 侧在用户编辑时回调（JNI）。 */
    void nativeHandleTextEdited(String text) {
        for (Consumer<String> h : onTextEditedHandlers) {
            h.accept(text);
        }
    }

    private final List<Runnable> onSelectionChangedHandlers = new ArrayList<>();
    private final List<Consumer<Integer>> onCursorPositionChangedHandlers = new ArrayList<>();
    private volatile boolean selConn, curConn;

    /** 选区变化回调。 */
    public QLineEdit onSelectionChanged(Runnable handler) {
        onSelectionChangedHandlers.add(handler);
        if (!selConn) { selConn = true; nativeConnectSelectionChanged(nativeHandle); }
        return this;
    }

    /** 光标位置变化回调（参数为新位置）。 */
    public QLineEdit onCursorPositionChanged(Consumer<Integer> handler) {
        onCursorPositionChangedHandlers.add(handler);
        if (!curConn) { curConn = true; nativeConnectCursorPositionChanged(nativeHandle); }
        return this;
    }

    private native void nativeConnectSelectionChanged(long handle);
    private native void nativeConnectCursorPositionChanged(long handle);

    void nativeHandleSelectionChanged() {
        for (Runnable h : onSelectionChangedHandlers) h.run();
    }
    void nativeHandleCursorPositionChanged(int pos) {
        for (Consumer<Integer> h : onCursorPositionChangedHandlers) h.accept(pos);
    }
}