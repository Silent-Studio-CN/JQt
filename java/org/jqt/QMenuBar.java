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
 * 菜单栏（QMenuBar）：窗口顶部的菜单容器。
 * <p>
 * L1 补全（v0.7.1）：addMenu / clear / triggered。
 */
public class QMenuBar extends QWidget {

    private final List<Consumer<Integer>> triggeredHandlers = new ArrayList<>();

    /** 创建菜单栏。 */
    public QMenuBar() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    private native long nativeCreate();

    /** 添加子菜单（返回菜单对象；QMenuBar::addMenu）。 */
    public QMenu addMenu(String title) {
        QMenu menu = new QMenu();
        menu.setTitle(title);
        nativeAddMenu(nativeHandle, menu.nativeHandle());
        return menu;
    }

    /** 添加已有菜单（QMenuBar::addMenu）。 */
    public void addMenu(QMenu menu) {
        nativeAddMenu(nativeHandle, menu.nativeHandle());
    }

    /** 清空全部菜单（QMenuBar::clear）。 */
    public void clear() {
        nativeClear(nativeHandle);
    }

    /** 菜单项触发回调（triggered 信号，参数为 actionId）。 */
    public QMenuBar onTriggered(Consumer<Integer> handler) {
        triggeredHandlers.add(handler);
        return this;
    }

    void nativeHandleTriggered(int actionId) {
        for (Consumer<Integer> h : triggeredHandlers) h.accept(actionId);
    }

    private native void nativeAddMenu(long handle, long menuHandle);
    private native void nativeClear(long handle);
}
