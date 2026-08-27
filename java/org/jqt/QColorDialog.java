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
}
