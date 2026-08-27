/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 文件对话框（QFileDialog 封装）。调用会阻塞直到用户选择（模态循环）。
 */
public class QFileDialog {

    private QFileDialog() {
    }

    /** 打开文件选择框，返回完整路径；用户取消返回 null。filter 如 "Images (*.png *.jpg)"。阻塞调用。 */
    public static String getOpenFileName(QMainWindow parent, String title, String dir, String filter) {
        return nativeGetOpenFileName(parent.nativeHandle(), title, dir, filter);
    }
    static native String nativeGetOpenFileName(long winHandle, String title, String dir, String filter);

    /** 保存文件选择框，返回完整路径；用户取消返回 null。阻塞调用。 */
    public static String getSaveFileName(QMainWindow parent, String title, String dir, String filter) {
        return nativeGetSaveFileName(parent.nativeHandle(), title, dir, filter);
    }
    static native String nativeGetSaveFileName(long winHandle, String title, String dir, String filter);

    /** 目录选择框，返回目录路径；用户取消返回 null。阻塞调用。 */
    public static String getExistingDirectory(QMainWindow parent, String title, String dir) {
        return nativeGetExistingDirectory(parent.nativeHandle(), title, dir);
    }
    static native String nativeGetExistingDirectory(long winHandle, String title, String dir);
}
