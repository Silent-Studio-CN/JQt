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

    // ---- Qt 静态工厂名别名（v1.8.0 L1-100；与 Qt info/warning/critical/question 命名对齐）----

    /** 信息框（Qt 名，等同 {@link #showInfo}）。阻塞调用。 */
    public static void info(QMainWindow parent, String title, String text) {
        showInfo(parent, title, text);
    }

    /** 警告框（Qt 名，等同 {@link #showWarning}）。阻塞调用。 */
    public static void warning(QMainWindow parent, String title, String text) {
        showWarning(parent, title, text);
    }

    /** 错误框（Qt 名，等同 {@link #showCritical}）。阻塞调用。 */
    public static void critical(QMainWindow parent, String title, String text) {
        showCritical(parent, title, text);
    }

    /** 是/否询问框（Qt 名，等同 {@link #showQuestion}）。阻塞调用，true = 是。 */
    public static boolean question(QMainWindow parent, String title, String text) {
        return showQuestion(parent, title, text);
    }

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

    // ---- v1.8.0 L1-100：标准按钮 ----

    /** 标准按钮（值 = Qt::StandardButton 位值；自定义按钮后 exec() 返回同编码）。 */
    public enum StandardButton {
        NO_BUTTON(0x00000000), OK(0x00000400), SAVE(0x00000800), OPEN(0x00002000),
        YES(0x00004000), NO(0x00008000), YES_TO_ALL(0x00010000), NO_TO_ALL(0x00020000),
        ABORT(0x00040000), RETRY(0x00080000), IGNORE(0x00100000), CLOSE(0x00200000),
        HELP(0x01000000), APPLY(0x02000000), RESET(0x04000000);
        public final int value;
        StandardButton(int v) { value = v; }
    }

    /** 追加标准按钮（QMessageBox::addButton(StandardButton)）；此后 exec() 返回该按钮位值。 */
    public void addButton(StandardButton button) {
        if (button == null) {
            return;
        }
        nativeAddButton(nativeHandle, button.value);
    }

    /** 设置默认按钮（QMessageBox::setDefaultButton(StandardButton)；回车触发）。 */
    public void setDefaultButton(StandardButton button) {
        if (button == null) {
            return;
        }
        nativeSetDefaultButton(nativeHandle, button.value);
    }

    private static native void nativeAddButton(long handle, int standardButton);
    private static native void nativeSetDefaultButton(long handle, int standardButton);

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