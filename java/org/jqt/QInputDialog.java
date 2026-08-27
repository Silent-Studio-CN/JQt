/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 输入对话框（QInputDialog 封装）。调用会阻塞直到用户选择（模态循环）。
 * 样式由 QSS 控制。
 */
public class QInputDialog {

    private QInputDialog() {
    }

    /** 文本输入框，返回输入内容；用户取消返回 null。阻塞调用。 */
    public static String getText(QMainWindow parent, String title, String label, String text) {
        return nativeGetText(parent.nativeHandle(), title, label, text);
    }
    static native String nativeGetText(long winHandle, String title, String label, String text);

    /** 整数输入框，返回输入值；用户取消返回 value（初始值）。阻塞调用。 */
    public static int getInt(QMainWindow parent, String title, String label, int value, int min, int max, int step) {
        return nativeGetInt(parent.nativeHandle(), title, label, value, min, max, step);
    }
    static native int nativeGetInt(long winHandle, String title, String label, int value, int min, int max, int step);

    /** 下拉选择框，返回选中项；用户取消返回 null。阻塞调用。 */
    public static String getItem(QMainWindow parent, String title, String label, String[] items, int current) {
        return nativeGetItem(parent.nativeHandle(), title, label, items, current);
    }
    static native String nativeGetItem(long winHandle, String title, String label, String[] items, int current);
}
