/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 系统剪贴板：封装 C++ 侧的 {@code QClipboard}（文本操作）。
 */
public class QClipboard {

    private QClipboard() {
    }

    /** 读取剪贴板文本（空返回空串）。 */
    public static String text() {
        return nativeText();
    }
    private static native String nativeText();

    /** 写入剪贴板文本。 */
    public static void setText(String text) {
        nativeSetText(text);
    }
    private static native void nativeSetText(String text);

    /** 清空剪贴板。 */
    public static void clear() {
        nativeClear();
    }
    private static native void nativeClear();

    private static final java.util.List<Runnable> onSelectionChangedHandlers = new java.util.ArrayList<>();
    private static volatile boolean selectionConnected;

    /** 剪贴板内容变化回调（静态注册，全局一次）。 */
    public static void onSelectionChanged(Runnable handler) {
        onSelectionChangedHandlers.add(handler);
        if (!selectionConnected) {
            selectionConnected = true;
            nativeConnectSelectionChanged();
        }
    }

    private static native void nativeConnectSelectionChanged();

    /** 由 C++ 侧在剪贴板变化时回调（JNI）。 */
    static void nativeHandleSelectionChanged() {
        for (Runnable h : onSelectionChangedHandlers) h.run();
    }
}
