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
 * 选项卡控件：封装 C++ 侧的 {@code QTabWidget}。
 * <p>信号槽：{@link #onCurrentChanged(Consumer)} — currentChanged 信号（参数为新页 index）。
 */
public class QTabWidget extends QWidget {

    private final List<Consumer<Integer>> onCurrentChangedHandlers = new ArrayList<>();

    public QTabWidget() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    private native long nativeCreate();

    /** 追加一页，返回其 index。 */
    public int addTab(QWidget widget, String title) {
        return nativeAddTab(nativeHandle, widget.nativeHandle, title);
    }
    private native int nativeAddTab(long handle, long childHandle, String title);

    /** 切换到指定页。 */
    public void setCurrentIndex(int index) {
        nativeSetCurrentIndex(nativeHandle, index);
    }
    private native void nativeSetCurrentIndex(long handle, int index);

    /** 当前页 index。 */
    public int currentIndex() {
        return nativeCurrentIndex(nativeHandle);
    }
    private native int nativeCurrentIndex(long handle);

    /** 修改页标题。 */
    public void setTabText(int index, String title) {
        nativeSetTabText(nativeHandle, index, title);
    }
    private native void nativeSetTabText(long handle, int index, String title);

    /** 注册页切换回调（参数为新页 index）。 */
    public QTabWidget onCurrentChanged(Consumer<Integer> handler) {
        onCurrentChangedHandlers.add(handler);
        return this;
    }

    /** 由 C++ 侧在页切换时回调（JNI）。 */
    void nativeHandleCurrentChanged(int index) {
        for (Consumer<Integer> h : onCurrentChangedHandlers) {
            h.accept(index);
        }
    }

    // ---- L1 补全（v0.6.0）----

    /** 清空全部页。 */
    public void clear() { nativeClear(nativeHandle); }
    private static native void nativeClear(long handle);

    /** 页数量。 */
    public int count() { return nativeCount(nativeHandle); }
    private static native int nativeCount(long handle);

    // ---- 值对象批：选项卡图标 ----

    /** 设置选项卡图标。 */
    public void setTabIcon(int index, QIcon icon) {
        if (icon == null || icon.isNull()) return;
        long pm = icon.pixmap().nativeHandle();
        if (pm != 0) nativeSetTabIcon(nativeHandle, index, pm);
    }
    private native void nativeSetTabIcon(long handle, int index, long pixmapHandle);
}
