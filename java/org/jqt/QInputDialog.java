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

    private static final java.lang.ref.Cleaner CLEANER = java.lang.ref.Cleaner.create();

    /** 实例句柄（v0.8.0 实例模式：配置后 exec 的自定义输入对话框）。 */
    private long nativeHandle;

    private boolean disposed;

    /** 实例模式构造：setLabelText/setIntRange 等配置后 exec()。 */
    public QInputDialog() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    private static native long nativeCreate();
    private static native void nativeDispose(long handle);
    private static native int nativeExec(long handle);

    private void registerCleaner() {
        CLEANER.register(this, () -> {
            if (nativeHandle != 0) nativeDispose(nativeHandle);
        });
    }

    /** 阻塞执行：1=接受（OK），0=取消。 */
    public int exec() {
        return nativeExec(nativeHandle);
    }

    /** 释放 C++ 对话框（一般无需调用，Cleaner 自动回收）。 */
    public void dispose() {
        if (!disposed) {
            disposed = true;
            nativeDispose(nativeHandle);
        }
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

// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** cancelButtonText（Qt cancelButtonText）。 */
    public String cancelButtonText() {
        return nativeCancelButtonText(nativeHandle);
    }
    private static native String nativeCancelButtonText(long nativeHandle);

    /** doubleDecimals（Qt doubleDecimals）。 */
    public int doubleDecimals() {
        return nativeDoubleDecimals(nativeHandle);
    }
    private static native int nativeDoubleDecimals(long nativeHandle);

    /** doubleMaximum（Qt doubleMaximum）。 */
    public double doubleMaximum() {
        return nativeDoubleMaximum(nativeHandle);
    }
    private static native double nativeDoubleMaximum(long nativeHandle);

    /** doubleMinimum（Qt doubleMinimum）。 */
    public double doubleMinimum() {
        return nativeDoubleMinimum(nativeHandle);
    }
    private static native double nativeDoubleMinimum(long nativeHandle);

    /** doubleStep（Qt doubleStep）。 */
    public double doubleStep() {
        return nativeDoubleStep(nativeHandle);
    }
    private static native double nativeDoubleStep(long nativeHandle);

    /** doubleValue（Qt doubleValue）。 */
    public double doubleValue() {
        return nativeDoubleValue(nativeHandle);
    }
    private static native double nativeDoubleValue(long nativeHandle);

    /** intMaximum（Qt intMaximum）。 */
    public int intMaximum() {
        return nativeIntMaximum(nativeHandle);
    }
    private static native int nativeIntMaximum(long nativeHandle);

    /** intMinimum（Qt intMinimum）。 */
    public int intMinimum() {
        return nativeIntMinimum(nativeHandle);
    }
    private static native int nativeIntMinimum(long nativeHandle);

    /** intStep（Qt intStep）。 */
    public int intStep() {
        return nativeIntStep(nativeHandle);
    }
    private static native int nativeIntStep(long nativeHandle);

    /** intValue（Qt intValue）。 */
    public int intValue() {
        return nativeIntValue(nativeHandle);
    }
    private static native int nativeIntValue(long nativeHandle);

    /** isComboBoxEditable（Qt isComboBoxEditable）。 */
    public boolean isComboBoxEditable() {
        return nativeIsComboBoxEditable(nativeHandle);
    }
    private static native boolean nativeIsComboBoxEditable(long nativeHandle);

    /** labelText（Qt labelText）。 */
    public String labelText() {
        return nativeLabelText(nativeHandle);
    }
    private static native String nativeLabelText(long nativeHandle);

    /** okButtonText（Qt okButtonText）。 */
    public String okButtonText() {
        return nativeOkButtonText(nativeHandle);
    }
    private static native String nativeOkButtonText(long nativeHandle);

    /** open（Qt open）。 */
    public void open() {
        nativeOpen(nativeHandle);
    }
    private static native void nativeOpen(long nativeHandle);

    /** setCancelButtonText（Qt setCancelButtonText）。 */
    public void setCancelButtonText(String arg0) {
        nativeSetCancelButtonText(nativeHandle, arg0);
    }
    private static native void nativeSetCancelButtonText(long nativeHandle, String arg0);

    /** setComboBoxEditable（Qt setComboBoxEditable）。 */
    public void setComboBoxEditable(boolean arg0) {
        nativeSetComboBoxEditable(nativeHandle, arg0);
    }
    private static native void nativeSetComboBoxEditable(long nativeHandle, boolean arg0);

    /** setDoubleDecimals（Qt setDoubleDecimals）。 */
    public void setDoubleDecimals(int arg0) {
        nativeSetDoubleDecimals(nativeHandle, arg0);
    }
    private static native void nativeSetDoubleDecimals(long nativeHandle, int arg0);

    /** setDoubleMaximum（Qt setDoubleMaximum）。 */
    public void setDoubleMaximum(double arg0) {
        nativeSetDoubleMaximum(nativeHandle, arg0);
    }
    private static native void nativeSetDoubleMaximum(long nativeHandle, double arg0);

    /** setDoubleMinimum（Qt setDoubleMinimum）。 */
    public void setDoubleMinimum(double arg0) {
        nativeSetDoubleMinimum(nativeHandle, arg0);
    }
    private static native void nativeSetDoubleMinimum(long nativeHandle, double arg0);

    /** setDoubleRange（Qt setDoubleRange）。 */
    public void setDoubleRange(double arg0, double arg1) {
        nativeSetDoubleRange(nativeHandle, arg0, arg1);
    }
    private static native void nativeSetDoubleRange(long nativeHandle, double arg0, double arg1);

    /** setDoubleStep（Qt setDoubleStep）。 */
    public void setDoubleStep(double arg0) {
        nativeSetDoubleStep(nativeHandle, arg0);
    }
    private static native void nativeSetDoubleStep(long nativeHandle, double arg0);

    /** setDoubleValue（Qt setDoubleValue）。 */
    public void setDoubleValue(double arg0) {
        nativeSetDoubleValue(nativeHandle, arg0);
    }
    private static native void nativeSetDoubleValue(long nativeHandle, double arg0);

    /** setIntMaximum（Qt setIntMaximum）。 */
    public void setIntMaximum(int arg0) {
        nativeSetIntMaximum(nativeHandle, arg0);
    }
    private static native void nativeSetIntMaximum(long nativeHandle, int arg0);

    /** setIntMinimum（Qt setIntMinimum）。 */
    public void setIntMinimum(int arg0) {
        nativeSetIntMinimum(nativeHandle, arg0);
    }
    private static native void nativeSetIntMinimum(long nativeHandle, int arg0);

    /** setIntRange（Qt setIntRange）。 */
    public void setIntRange(int arg0, int arg1) {
        nativeSetIntRange(nativeHandle, arg0, arg1);
    }
    private static native void nativeSetIntRange(long nativeHandle, int arg0, int arg1);

    /** setIntStep（Qt setIntStep）。 */
    public void setIntStep(int arg0) {
        nativeSetIntStep(nativeHandle, arg0);
    }
    private static native void nativeSetIntStep(long nativeHandle, int arg0);

    /** setIntValue（Qt setIntValue）。 */
    public void setIntValue(int arg0) {
        nativeSetIntValue(nativeHandle, arg0);
    }
    private static native void nativeSetIntValue(long nativeHandle, int arg0);

    /** setLabelText（Qt setLabelText）。 */
    public void setLabelText(String arg0) {
        nativeSetLabelText(nativeHandle, arg0);
    }
    private static native void nativeSetLabelText(long nativeHandle, String arg0);

    /** setOkButtonText（Qt setOkButtonText）。 */
    public void setOkButtonText(String arg0) {
        nativeSetOkButtonText(nativeHandle, arg0);
    }
    private static native void nativeSetOkButtonText(long nativeHandle, String arg0);

    /** setTextValue（Qt setTextValue）。 */
    public void setTextValue(String arg0) {
        nativeSetTextValue(nativeHandle, arg0);
    }
    private static native void nativeSetTextValue(long nativeHandle, String arg0);

    /** setVisible（Qt setVisible）。 */
    public void setVisible(boolean arg0) {
        nativeSetVisible(nativeHandle, arg0);
    }
    private static native void nativeSetVisible(long nativeHandle, boolean arg0);

    /** textValue（Qt textValue）。 */
    public String textValue() {
        return nativeTextValue(nativeHandle);
    }
    private static native String nativeTextValue(long nativeHandle);

}