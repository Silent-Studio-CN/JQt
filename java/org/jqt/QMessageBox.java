/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 模态对话框（QMessageBox 封装）。调用会阻塞直到用户选择（模态循环）。
 * 样式由 QSS 控制。
 */
public class QMessageBox {

    private QMessageBox() {
    }

    /** 是/否 询问框，返回用户选择（true = 是）。阻塞调用。 */
    public static boolean showQuestion(QMainWindow parent, String title, String text) {
        return nativeShowQuestion(parent.nativeHandle(), title, text);
    }
    static native boolean nativeShowQuestion(long winHandle, String title, String text);

    /** 信息框（确定）。阻塞调用。 */
    public static void showInfo(QMainWindow parent, String title, String text) {
        nativeShowInfo(parent.nativeHandle(), title, text);
    }
    static native void nativeShowInfo(long winHandle, String title, String text);
}


