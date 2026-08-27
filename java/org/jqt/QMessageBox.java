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

    /** 警告框（确定）。阻塞调用。 */
    public static void showWarning(QMainWindow parent, String title, String text) {
        nativeShowWarning(parent.nativeHandle(), title, text);
    }
    static native void nativeShowWarning(long winHandle, String title, String text);

    /** 错误框（确定）。阻塞调用。 */
    public static void showCritical(QMainWindow parent, String title, String text) {
        nativeShowCritical(parent.nativeHandle(), title, text);
    }
    static native void nativeShowCritical(long winHandle, String title, String text);

    /** 确定/取消 询问框，返回用户选择（true = 确定）。阻塞调用。 */
    public static boolean showOkCancel(QMainWindow parent, String title, String text) {
        return nativeShowOkCancel(parent.nativeHandle(), title, text);
    }
    static native boolean nativeShowOkCancel(long winHandle, String title, String text);

    /** 关于框（确定）。阻塞调用。 */
    public static void showAbout(QMainWindow parent, String title, String text) {
        nativeShowAbout(parent.nativeHandle(), title, text);
    }
    static native void nativeShowAbout(long winHandle, String title, String text);
}