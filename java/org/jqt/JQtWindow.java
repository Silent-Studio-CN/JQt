/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * 窗口：封装 C++ 侧的顶级 {@code QWidget}。
 * <p>
 * 事件（均可注册多个监听器）：
 * <ul>
 *   <li>{@link #onClose(Runnable)} — 窗口关闭（closeEvent）</li>
 *   <li>{@link #onResized(BiConsumer)} — 窗口大小变化（resizeEvent，参数为宽高）</li>
 *   <li>{@link #onMoved(BiConsumer)} — 窗口位置变化（moveEvent，参数为 x/y）</li>
 * </ul>
 */
public class JQtWindow extends JQtWidget {

    private final List<Runnable> onCloseHandlers = new ArrayList<>();
    private final List<BiConsumer<Integer, Integer>> onResizedHandlers = new ArrayList<>();
    private final List<BiConsumer<Integer, Integer>> onMovedHandlers = new ArrayList<>();

    /** 创建一个 800x600 的新窗口。 */
    public JQtWindow(String title) {
        this(title, 800, 600);
    }

    /** 创建一个指定大小的新窗口。 */
    public JQtWindow(String title, int width, int height) {
        nativeHandle = nativeCreate(title, width, height);
    }

    private native long nativeCreate(String title, int width, int height);

    /** 显示窗口。 */
    public void show() {
        nativeShow(nativeHandle);
    }
    private native void nativeShow(long handle);

    /** 隐藏窗口。 */
    public void hide() {
        nativeHide(nativeHandle);
    }
    private native void nativeHide(long handle);

    /** 修改窗口大小（会触发 onResized 回调）。 */
    public void resize(int width, int height) {
        nativeResize(nativeHandle, width, height);
    }
    private native void nativeResize(long handle, int width, int height);

    /** 修改窗口标题。 */
    public void setTitle(String title) {
        nativeSetTitle(nativeHandle, title);
    }
    private native void nativeSetTitle(long handle, String title);

    /**
     * 添加子控件（未设置布局时按顺序自动摆放）。
     * 若已调用 {@link #setLayout(JQtLayout)}，子控件应改用 {@link JQtLayout#addWidget(JQtWidget)} 加入布局。
     */
    public void addWidget(JQtWidget child) {
        nativeAddWidget(nativeHandle, child.nativeHandle());
    }
    private native void nativeAddWidget(long handle, long childHandle);

    /**
     * 设置布局管理器（QVBoxLayout / QHBoxLayout）。
     * 布局接管子控件的位置与大小；重复设置会替换并销毁旧布局（Qt 行为）。
     */
    public void setLayout(JQtLayout layout) {
        nativeSetLayout(nativeHandle, layout.nativeHandle());
    }
    private native void nativeSetLayout(long handle, long layoutHandle);

    /** 注册窗口关闭回调（closeEvent）。 */
    public void onClose(Runnable handler) {
        onCloseHandlers.add(handler);
    }

    /** 注册窗口大小变化回调（resizeEvent，参数为新的宽和高）。 */
    public void onResized(BiConsumer<Integer, Integer> handler) {
        onResizedHandlers.add(handler);
    }

    /** 注册窗口位置变化回调（moveEvent，参数为新的 x 和 y）。 */
    public void onMoved(BiConsumer<Integer, Integer> handler) {
        onMovedHandlers.add(handler);
    }

    /** 由 C++ 侧在窗口关闭时回调（JNI）。 */
    void nativeHandleClose() {
        for (Runnable h : onCloseHandlers) {
            h.run();
        }
    }

    /** 由 C++ 侧在窗口大小变化时回调（JNI）。 */
    void nativeHandleResized(int width, int height) {
        for (BiConsumer<Integer, Integer> h : onResizedHandlers) {
            h.accept(width, height);
        }
    }

    /** 由 C++ 侧在窗口移动时回调（JNI）。 */
    void nativeHandleMoved(int x, int y) {
        for (BiConsumer<Integer, Integer> h : onMovedHandlers) {
            h.accept(x, y);
        }
    }
}
