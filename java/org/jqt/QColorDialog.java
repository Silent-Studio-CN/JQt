/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 颜色对话框（QColorDialog 封装）。调用会阻塞直到用户选择（模态循环）。
 */
public class QColorDialog {

    private QColorDialog() {
    }

    /** 颜色选择框，返回 0xAARRGGBB；用户取消返回 -1。argb 为初始颜色。阻塞调用。 */
    public static int getColor(QMainWindow parent, String title, int argb) {
        return nativeGetColor(parent.nativeHandle(), title, argb);
    }
    static native int nativeGetColor(long winHandle, String title, int argb);

    // ---- L1 补全（v0.8.0）：非阻塞 open() + colorSelected 信号 ----

    private static final java.util.List<java.util.function.Consumer<Integer>> colorSelectedHandlers = new java.util.ArrayList<>();
    private static volatile boolean openConnected;

    /**
     * 非阻塞打开颜色对话框（QColorDialog::open）。
     * 选择颜色后触发 {@link #onColorSelected}（参数为 0xAARRGGBB）；取消不触发。
     */
    public static void open(QMainWindow parent, String title, int argb) {
        if (!openConnected) {
            openConnected = true;
            nativeConnectColorSelected();
        }
        nativeOpen(parent.nativeHandle(), title, argb);
    }

    /** 注册颜色选择回调（colorSelected 信号）。 */
    public static void onColorSelected(java.util.function.Consumer<Integer> handler) {
        colorSelectedHandlers.add(handler);
    }

    static void nativeHandleColorSelected(int argb) {
        for (java.util.function.Consumer<Integer> h : colorSelectedHandlers) {
            h.accept(argb);
        }
    }

    static native void nativeOpen(long winHandle, String title, int argb);
    static native void nativeConnectColorSelected();
}
