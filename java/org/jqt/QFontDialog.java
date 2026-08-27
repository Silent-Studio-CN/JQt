/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 字体对话框（QFontDialog 封装）。调用会阻塞直到用户选择（模态循环）。
 */
public class QFontDialog {

    private QFontDialog() {
    }

    /** 字体选择框，返回 "Family,size"；用户取消返回 null。family 为初始字体，可为 null 用默认。阻塞调用。 */
    public static String getFont(QMainWindow parent, String title, String family, int size) {
        return nativeGetFont(parent.nativeHandle(), title, family, size);
    }
    static native String nativeGetFont(long winHandle, String title, String family, int size);
}
