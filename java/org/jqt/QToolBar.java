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
}
