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
public class JQtComboBox extends JQtWidget {

    private final List<Consumer<Integer>> onCurrentIndexChangedHandlers = new ArrayList<>();

    public JQtComboBox() {
        nativeHandle = nativeCreate();
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
    public void onCurrentIndexChanged(Consumer<Integer> handler) {
        onCurrentIndexChangedHandlers.add(handler);
    }

    /** 由 C++ 侧在选项切换时回调（JNI）。 */
    void nativeHandleCurrentIndexChanged(int index) {
        for (Consumer<Integer> h : onCurrentIndexChangedHandlers) {
            h.accept(index);
        }
    }
}
