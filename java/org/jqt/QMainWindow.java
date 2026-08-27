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
public class QMainWindow extends QWidget {

    private final List<Runnable> onCloseHandlers = new ArrayList<>();
    private final List<BiConsumer<Integer, Integer>> onResizedHandlers = new ArrayList<>();
    private final List<BiConsumer<Integer, Integer>> onMovedHandlers = new ArrayList<>();

    /** 创建一个 800x600 的新窗口。 */
    public QMainWindow(String title) {
        this(title, 800, 600);
    }

    /** 创建一个指定大小的新窗口。 */
    public QMainWindow(String title, int width, int height) {
        nativeHandle = nativeCreate(title, width, height);
        registerCleaner();
    }

    private native long nativeCreate(String title, int width, int height);

    // show()/hide() 继承自 QWidget（基础 API）

    /** 关闭窗口（触发 onClose 回调；若为最后一个窗口，exec() 返回）。 */
    public void close() {
        nativeClose(nativeHandle);
    }
    private native void nativeClose(long handle);

    /** 修改窗口大小（会触发 onResized 回调）。 */
    public void resize(int width, int height) {
        nativeResize(nativeHandle, width, height);
    }
    private native void nativeResize(long handle, int width, int height);

    // ---- Fluent 窗口能力（无边框 / 亚克力 / 圆角 / 拖拽）----

    /**
     * 无边框模式（Fluent 风格窗口的基石）。
     * 启用后：移除系统标题栏、自动添加 DWM 阴影、边框 5px 区域可缩放、
     * 顶部 40px 区域可拖拽移动。需要自绘标题栏（最小化/关闭按钮等）。
     */
    public void setFrameless(boolean frameless) {
        nativeSetFrameless(nativeHandle, frameless);
    }
    private native void nativeSetFrameless(long handle, boolean frameless);

    /** 亚克力背景（Windows 10+，模糊 + 半透明混合色）。 */
    public void setAcrylic(boolean acrylic) {
        nativeSetAcrylic(nativeHandle, acrylic);
    }
    private native void nativeSetAcrylic(long handle, boolean acrylic);

    /** Windows 11 圆角窗口。 */
    public void setRoundedCorners(boolean rounded) {
        nativeSetRoundedCorners(nativeHandle, rounded);
    }
    private native void nativeSetRoundedCorners(long handle, boolean rounded);

    /** 是否允许顶部区域拖拽移动窗口（默认 true）。 */
    public void setDraggable(boolean draggable) {
        nativeSetDraggable(nativeHandle, draggable);
    }
    private native void nativeSetDraggable(long handle, boolean draggable);

    /** 无边框窗口的缩放热区宽度（像素，默认 5）。 */
    public void setBorderWidth(int px) {
        nativeSetBorderWidth(nativeHandle, px);
    }
    private native void nativeSetBorderWidth(long handle, int px);

    /** 最小化。 */
    public void minimize() {
        nativeMinimize(nativeHandle);
    }
    private native void nativeMinimize(long handle);

    /** 最大化。 */
    public void maximize() {
        nativeMaximize(nativeHandle);
    }
    private native void nativeMaximize(long handle);

    /** 最大化/还原切换。 */
    public void toggleMaximize() {
        nativeToggleMaximize(nativeHandle);
    }
    private native void nativeToggleMaximize(long handle);

    /** 是否已最大化。 */
    public boolean isMaximized() {
        return nativeIsMaximized(nativeHandle);
    }
    private native boolean nativeIsMaximized(long handle);

    // ---- 动画（QPropertyAnimation）----

    /** 窗口淡入（透明度 0 → 1，默认 200ms）。 */
    public void fadeIn(long ms) {
        nativeFadeIn(nativeHandle, ms);
    }
    private native void nativeFadeIn(long handle, long ms);

    /** 窗口淡出（透明度 1 → 0）。 */
    public void fadeOut(long ms) {
        nativeFadeOut(nativeHandle, ms);
    }
    private native void nativeFadeOut(long handle, long ms);

    /** 窗口淡入，可指定缓动函数。 */
    public void fadeIn(long ms, JQtEasing easing) {
        nativeFadeInEasing(nativeHandle, ms, easing.qtType);
    }
    private native void nativeFadeInEasing(long handle, long ms, int easing);

    /** 窗口淡出，可指定缓动函数。 */
    public void fadeOut(long ms, JQtEasing easing) {
        nativeFadeOutEasing(nativeHandle, ms, easing.qtType);
    }
    private native void nativeFadeOutEasing(long handle, long ms, int easing);

    /** 修改窗口标题。 */
    public void setTitle(String title) {
        nativeSetTitle(nativeHandle, title);
    }
    private native void nativeSetTitle(long handle, String title);

    /**
     * 添加子控件（未设置布局时按顺序自动摆放）。
     * 若已调用 {@link #setLayout(QLayout)}，子控件应改用 {@link QLayout#addWidget(QWidget)} 加入布局。
     */
    public void addWidget(QWidget child) {
        nativeAddWidget(nativeHandle, child.nativeHandle());
    }
    private native void nativeAddWidget(long handle, long childHandle);

    /**
     * 设置布局管理器（继承自 {@link QWidget}）。
     * 布局接管子控件的位置与大小；重复设置会替换并销毁旧布局（Qt 行为）。
     */

    /** 注册窗口关闭回调（closeEvent）。 */
    public QMainWindow onClose(Runnable handler) {
        onCloseHandlers.add(handler);
        return this;
    }

    /** 注册窗口大小变化回调（resizeEvent，参数为新的宽和高）。 */
    public QMainWindow onResized(BiConsumer<Integer, Integer> handler) {
        onResizedHandlers.add(handler);
        return this;
    }

    /** 注册窗口位置变化回调（moveEvent，参数为新的 x 和 y）。 */
    public QMainWindow onMoved(BiConsumer<Integer, Integer> handler) {
        onMovedHandlers.add(handler);
        return this;
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



