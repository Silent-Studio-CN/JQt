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
 * Fluent 侧栏导航：图标 + 文字导航项，选中高亮背景 200ms 滑动动画。
 * 高亮色 = 主题色淡色，文字选中态用主题色；宽度固定 180px。
 */
public class JQtNavigation extends JQtWidget {

    private final List<Consumer<Integer>> onChangedHandlers = new ArrayList<>();

    public JQtNavigation() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    /** 追加导航项（icon 可为 emoji 或单字符）。 */
    public void addItem(String icon, String text) {
        nativeAddItem(nativeHandle, icon, text);
    }

    /** 当前选中索引。 */
    public int currentIndex() {
        return nativeCurrentIndex(nativeHandle);
    }

    /** 选中指定项（高亮滑动动画，触发 onChanged）。 */
    public void setCurrentIndex(int index) {
        nativeSetCurrentIndex(nativeHandle, index);
    }

    /** 选中项变化回调。 */
    public void onChanged(Consumer<Integer> handler) {
        onChangedHandlers.add(handler);
    }

    /** 由 C++ 侧回调（JNI）。 */
    void nativeHandleChanged(int index) {
        for (Consumer<Integer> h : onChangedHandlers) {
            h.accept(index);
        }
    }

    private native long nativeCreate();
    private native void nativeAddItem(long handle, String icon, String text);
    private native int nativeCurrentIndex(long handle);
    private native void nativeSetCurrentIndex(long handle, int index);
}
