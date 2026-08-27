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
 * 工具栏：封装 C++ 侧的 {@code QToolBar}。
 * <p>信号槽：{@link #onTriggered(Consumer)} — actionTriggered 信号（参数为 actionId）。
 */
public class QToolBar extends QWidget {

    private final List<Consumer<Integer>> onTriggeredHandlers = new ArrayList<>();
    private int nextActionId = 1;

    public QToolBar() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    private native long nativeCreate();

    /** 追加工具按钮（文本），返回其 actionId。 */
    public int addButton(String text) {
        return nativeAddButton(nativeHandle, nextActionId++, text);
    }
    private native int nativeAddButton(long handle, int actionId, String text);

    /** 追加任意控件（如搜索框）。 */
    public void addWidget(QWidget widget) {
        nativeAddWidget(nativeHandle, widget.nativeHandle);
    }
    private native void nativeAddWidget(long handle, long childHandle);

    /** 工具按钮触发回调（参数为 actionId）。 */
    public QToolBar onTriggered(Consumer<Integer> handler) {
        onTriggeredHandlers.add(handler);
        return this;
    }

    /** 由 C++ 侧在按钮被触发时回调（JNI）。 */
    void nativeHandleTriggered(int actionId) {
        for (Consumer<Integer> h : onTriggeredHandlers) {
            h.accept(actionId);
        }
    }

    // ---- L1 补全（v0.6.0）----

    /** 清空全部按钮。 */
    public void clear() { nativeClear(nativeHandle); }
    private static native void nativeClear(long handle);

    /** 图标尺寸（像素）。 */
    public void setIconSize(int size) { nativeSetIconSize(nativeHandle, size); }
    private static native void nativeSetIconSize(long handle, int size);

    /** 图标尺寸。 */
    public int iconSize() { return nativeIconSize(nativeHandle); }
    private static native int nativeIconSize(long handle);

    // ---- L1 补全（v0.6.0）----

    private final List<Consumer<Integer>> onIconSizeChangedHandlers = new ArrayList<>();
    private final List<Consumer<Integer>> onToolButtonStyleChangedHandlers = new ArrayList<>();
    private volatile boolean iconConn, styleConn;

    /** 图标尺寸变化回调（参数为新尺寸）。 */
    public QToolBar onIconSizeChanged(Consumer<Integer> handler) {
        onIconSizeChangedHandlers.add(handler);
        if (!iconConn) { iconConn = true; nativeConnectIconSizeChanged(nativeHandle); }
        return this;
    }

    /** 按钮样式变化回调（参数为样式值）。 */
    public QToolBar onToolButtonStyleChanged(Consumer<Integer> handler) {
        onToolButtonStyleChangedHandlers.add(handler);
        if (!styleConn) { styleConn = true; nativeConnectToolButtonStyleChanged(nativeHandle); }
        return this;
    }

    private native void nativeConnectIconSizeChanged(long handle);
    private native void nativeConnectToolButtonStyleChanged(long handle);

    void nativeHandleIconSizeChanged(int size) {
        for (Consumer<Integer> h : onIconSizeChangedHandlers) h.accept(size);
    }
    void nativeHandleToolButtonStyleChanged(int style) {
        for (Consumer<Integer> h : onToolButtonStyleChangedHandlers) h.accept(style);
    }
}