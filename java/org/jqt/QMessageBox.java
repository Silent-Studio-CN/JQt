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

    // ---- L1 补全（v0.8.0）：QMessageBox 实例化（Qt 风格 setText/setIcon/exec/open）----

    /** 图标类型（QMessageBox::Icon）。 */
    public enum Icon { NO_ICON, INFORMATION, WARNING, CRITICAL, QUESTION }

    private long nativeHandle;

    /** 创建消息框实例（需调用 exec() 或 open() 显示）。 */
    public QMessageBox() {
        nativeHandle = nativeCreate();
    }

    /** 设置正文文本。 */
    public void setText(String text) {
        nativeSetText(nativeHandle, text);
    }

    /** 设置标题（窗口标题）。 */
    public void setWindowTitle(String title) {
        nativeSetWindowTitle(nativeHandle, title);
    }

    /** 设置图标类型。 */
    public void setIcon(Icon icon) {
        nativeSetIcon(nativeHandle, icon.ordinal());
    }

    /** 阻塞显示并返回用户选择（1 = 确定 / 0 = 取消；QMessageBox::exec）。 */
    public int exec() {
        return nativeExec(nativeHandle);
    }

    /** 非阻塞显示（QMessageBox::open；配合 finished 语义使用 exec 返回值）。 */
    public void open() {
        nativeOpen(nativeHandle);
    }

    /** 隐藏并关闭。 */
    public void close() {
        nativeClose(nativeHandle);
    }

    private native long nativeCreate();
    private native void nativeSetText(long handle, String text);
    private native void nativeSetWindowTitle(long handle, String title);
    private native void nativeSetIcon(long handle, int icon);
    private native int nativeExec(long handle);
    private native void nativeOpen(long handle);
    private native void nativeClose(long handle);

// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** detailedText（Qt detailedText）。 */
    public String detailedText() {
        return nativeDetailedText(nativeHandle);
    }
    private static native String nativeDetailedText(long nativeHandle);

    /** informativeText（Qt informativeText）。 */
    public String informativeText() {
        return nativeInformativeText(nativeHandle);
    }
    private static native String nativeInformativeText(long nativeHandle);

    /** setDetailedText（Qt setDetailedText）。 */
    public void setDetailedText(String arg0) {
        nativeSetDetailedText(nativeHandle, arg0);
    }
    private static native void nativeSetDetailedText(long nativeHandle, String arg0);

    /** setInformativeText（Qt setInformativeText）。 */
    public void setInformativeText(String arg0) {
        nativeSetInformativeText(nativeHandle, arg0);
    }
    private static native void nativeSetInformativeText(long nativeHandle, String arg0);

    /** text（Qt text）。 */
    public String text() {
        return nativeText(nativeHandle);
    }
    private static native String nativeText(long nativeHandle);

}