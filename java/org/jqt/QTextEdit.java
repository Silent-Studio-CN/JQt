/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.ArrayList;
import java.util.List;

/**
 * 多行文本编辑器：封装 C++ 侧的 {@code QTextEdit}（富文本/纯文本均可）。
 * <p>信号槽：{@link #onTextChanged(Runnable)} — textChanged 信号（内容变化）。
 */
public class QTextEdit extends QWidget {

    private final List<Runnable> onTextChangedHandlers = new ArrayList<>();

    public QTextEdit() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    private native long nativeCreate();

    /** 设置纯文本（替换全部内容）。 */
    public void setPlainText(String text) {
        nativeSetPlainText(nativeHandle, text);
    }
    private native void nativeSetPlainText(long handle, String text);

    /** 读取全部文本（纯文本形式）。 */
    public String toPlainText() {
        return nativeToPlainText(nativeHandle);
    }
    private native String nativeToPlainText(long handle);

    /** 追加一行（自动换行）。 */
    public void append(String text) {
        nativeAppend(nativeHandle, text);
    }
    private native void nativeAppend(long handle, String text);

    /** 设置只读。 */
    public void setReadOnly(boolean readOnly) {
        nativeSetReadOnly(nativeHandle, readOnly);
    }
    private native void nativeSetReadOnly(long handle, boolean readOnly);

    /** 是否只读。 */
    public boolean isReadOnly() {
        return nativeIsReadOnly(nativeHandle);
    }
    private native boolean nativeIsReadOnly(long handle);

    /** 内容变化回调。 */
    public QTextEdit onTextChanged(Runnable handler) {
        onTextChangedHandlers.add(handler);
        return this;
    }

    /** 由 C++ 侧在文本变化时回调（JNI）。 */
    void nativeHandleTextChanged() {
        for (Runnable h : onTextChangedHandlers) {
            h.run();
        }
    }

    // ---- L1 补全（v0.6.0）----

    /** 清空内容。 */
    public void clear() { nativeClear(nativeHandle); }
    private static native void nativeClear(long handle);

    /** 复制选中内容。 */
    public void copy() { nativeCopy(nativeHandle); }
    private static native void nativeCopy(long handle);

    /** 剪切选中内容。 */
    public void cut() { nativeCut(nativeHandle); }
    private static native void nativeCut(long handle);

    /** 粘贴。 */
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

    /** 查找文本（高亮第一处匹配，返回是否找到）。 */
    public boolean find(String text) { return nativeFind(nativeHandle, text); }
    private static native boolean nativeFind(long handle, String text);
}