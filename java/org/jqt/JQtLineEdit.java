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
public class JQtLineEdit extends JQtWidget {

    private final List<Consumer<String>> onTextChangedHandlers = new ArrayList<>();
    private final List<Runnable> onReturnPressedHandlers = new ArrayList<>();

    public JQtLineEdit(String text) {
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

    /** 注册文本变化回调（textChanged 信号，参数为最新文本）。 */
    public void onTextChanged(Consumer<String> handler) {
        onTextChangedHandlers.add(handler);
    }

    /** 注册回车确认回调（returnPressed 信号）。 */
    public void onReturnPressed(Runnable handler) {
        onReturnPressedHandlers.add(handler);
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
}
