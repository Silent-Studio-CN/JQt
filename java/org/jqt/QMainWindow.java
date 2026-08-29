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

    // show()/hide()/close()/resize() 继承自 QWidget（基础 API，v0.6.0 上移到基类）

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

    // ---- L1 补全（v0.8.0）：iconSizeChanged / toolButtonStyleChanged 信号 ----

    private final List<Consumer<Integer>> onIconSizeChangedHandlers = new ArrayList<>();
    private final List<Consumer<Integer>> onToolButtonStyleChangedHandlers = new ArrayList<>();

    /** 工具栏图标尺寸变化回调（iconSizeChanged 信号，参数为图标边长像素）。 */
    public QMainWindow onIconSizeChanged(Consumer<Integer> handler) {
        onIconSizeChangedHandlers.add(handler);
        nativeConnectIconSizeChanged(nativeHandle);
        return this;
    }
    private native void nativeConnectIconSizeChanged(long handle);

    /** 工具栏按钮样式变化回调（toolButtonStyleChanged 信号，参数为 Qt::ToolButtonStyle：0 仅图标 / 1 仅文字 / 2 文字在图标旁 / 3 文字在图标下）。 */
    public QMainWindow onToolButtonStyleChanged(Consumer<Integer> handler) {
        onToolButtonStyleChangedHandlers.add(handler);
        nativeConnectToolButtonStyleChanged(nativeHandle);
        return this;
    }
    private native void nativeConnectToolButtonStyleChanged(long handle);

    void nativeHandleIconSizeChanged(int size) {
        for (Consumer<Integer> h : onIconSizeChangedHandlers) h.accept(size);
    }

    void nativeHandleToolButtonStyleChanged(int style) {
        for (Consumer<Integer> h : onToolButtonStyleChangedHandlers) h.accept(style);
    }
    // ---- Exclusive Kit（跨平台独家能力，Qt 官方未封装）----
    //   v0.6.1 : Windows —— DWM 边框/标题栏/文字颜色 + 深色标题栏 + Mica + 任务栏进度
    //   v0.7.0 : macOS —— Dock 徽章 + 透明标题栏 + 全尺寸内容视图（对齐 Windows 任务栏进度/DWM 能力）
    //   v0.7.0 : Linux —— XDG 开机自启 + D-Bus 防息屏/通知（见 QApplication）

    /**
     * 设置原生窗口边框颜色（0xAARRGGBB）。
     * 依赖 Windows 11 22H2+ 的 DWM 属性；旧系统静默忽略。
     */
    public void setNativeBorderColor(int argb) {
        nativeSetDwmAttribute(nativeHandle, 1, argb);
    }

    /** 设置原生标题栏颜色（0xAARRGGBB；Win11 22H2+，旧系统忽略）。 */
    public void setNativeCaptionColor(int argb) {
        nativeSetDwmAttribute(nativeHandle, 2, argb);
    }

    /** 设置原生标题栏文字颜色（0xAARRGGBB；Win11 22H2+，旧系统忽略）。 */
    public void setNativeCaptionTextColor(int argb) {
        nativeSetDwmAttribute(nativeHandle, 3, argb);
    }

    /** 深色标题栏（Win10 1809+ 支持）。 */
    public void setNativeDarkTitleBar(boolean dark) {
        nativeSetDwmAttribute(nativeHandle, 4, dark ? 1 : 0);
    }

    /** Mica 背景材质（Win11 22H2+；开启后窗口背景跟随系统 Mica 质感）。 */
    public void setMicaBackground(boolean on) {
        nativeSetDwmAttribute(nativeHandle, 5, on ? 1 : 0);
    }

    /** 任务栏图标进度（value/max，如 30/100；Win10+；max ≤ 0 或 value < 0 清除）。 */
    public void setTaskbarProgress(int value, int max) {
        nativeTaskbarProgress(nativeHandle, value, max);
    }

    /** 清除任务栏进度。 */
    public void clearTaskbarProgress() {
        nativeTaskbarProgress(nativeHandle, -1, 0);
    }

    private static native void nativeSetDwmAttribute(long handle, int kind, int argb);
    private static native void nativeTaskbarProgress(long handle, int value, int max);

    // ---- macOS 独家能力（v0.7.0；Windows/Linux 上为无操作）----

    /**
     * Dock 图标徽章（macOS 通知角标，如未读消息数）。
     * 对齐 Windows 任务栏进度：macOS 没有任务栏进度概念，用 Dock 角标呈现应用状态。
     * null 或空串清除；非 macOS 平台无操作。
     */
    public void setDockBadge(String badge) {
        nativeSetDockBadge(nativeHandle, badge);
    }

    /** 清除 Dock 徽章（macOS）。 */
    public void clearDockBadge() {
        nativeSetDockBadge(nativeHandle, null);
    }

    /**
     * 透明标题栏（macOS：保留红黄绿窗口按钮，标题栏区域透明，内容可延伸至顶部）。
     * Qt 官方只能"全有或全无"（无边框 = 连窗口按钮一起去掉）；
     * 此 API 保留原生窗口按钮的同时实现沉浸式布局。
     * 建议在窗口 show() 之前调用；非 macOS 平台无操作。
     */
    public void setMacTitlebarTransparent(boolean transparent) {
        nativeSetMacWindowAttribute(nativeHandle, 1, transparent);
    }

    /**
     * 全尺寸内容视图（macOS：内容视图延伸到标题栏区域，配合
     * setMacTitlebarTransparent 实现无边框观感但保留红黄绿按钮）。
     * 非 macOS 平台无操作。
     */
    public void setMacFullSizeContentView(boolean on) {
        nativeSetMacWindowAttribute(nativeHandle, 2, on);
    }

    private static native void nativeSetDockBadge(long handle, String badge);
    private static native void nativeSetMacWindowAttribute(long handle, int kind, boolean value);

    // ---- End Exclusive Kit ----

    // L1：toolbar 相关信号由 QToolBar 提供（JQtWindowShell 非 QMainWindow 类型）

// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** documentMode（Qt documentMode）。 */
    public boolean documentMode() {
        return nativeDocumentMode(nativeHandle);
    }
    private static native boolean nativeDocumentMode(long nativeHandle);

    /** isAnimated（Qt isAnimated）。 */
    public boolean isAnimated() {
        return nativeIsAnimated(nativeHandle);
    }
    private static native boolean nativeIsAnimated(long nativeHandle);

    /** isDockNestingEnabled（Qt isDockNestingEnabled）。 */
    public boolean isDockNestingEnabled() {
        return nativeIsDockNestingEnabled(nativeHandle);
    }
    private static native boolean nativeIsDockNestingEnabled(long nativeHandle);

    /** setAnimated（Qt setAnimated）。 */
    public void setAnimated(boolean arg0) {
        nativeSetAnimated(nativeHandle, arg0);
    }
    private static native void nativeSetAnimated(long nativeHandle, boolean arg0);

    /** setDockNestingEnabled（Qt setDockNestingEnabled）。 */
    public void setDockNestingEnabled(boolean arg0) {
        nativeSetDockNestingEnabled(nativeHandle, arg0);
    }
    private static native void nativeSetDockNestingEnabled(long nativeHandle, boolean arg0);

    /** setDocumentMode（Qt setDocumentMode）。 */
    public void setDocumentMode(boolean arg0) {
        nativeSetDocumentMode(nativeHandle, arg0);
    }
    private static native void nativeSetDocumentMode(long nativeHandle, boolean arg0);

    /** setUnifiedTitleAndToolBarOnMac（Qt setUnifiedTitleAndToolBarOnMac）。 */
    public void setUnifiedTitleAndToolBarOnMac(boolean arg0) {
        nativeSetUnifiedTitleAndToolBarOnMac(nativeHandle, arg0);
    }
    private static native void nativeSetUnifiedTitleAndToolBarOnMac(long nativeHandle, boolean arg0);

    /** unifiedTitleAndToolBarOnMac（Qt unifiedTitleAndToolBarOnMac）。 */
    public boolean unifiedTitleAndToolBarOnMac() {
        return nativeUnifiedTitleAndToolBarOnMac(nativeHandle);
    }
    private static native boolean nativeUnifiedTitleAndToolBarOnMac(long nativeHandle);

}