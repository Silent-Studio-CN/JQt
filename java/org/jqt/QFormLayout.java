/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 表单布局：封装 C++ 侧的 {@code QFormLayout}，标签 + 输入控件两列排布。
 * <pre>
 * QFormLayout form = new QFormLayout();
 * form.addRow("姓名：", nameEdit);
 * form.addRow("邮箱：", mailEdit);
 * window.setLayout(form);
 * </pre>
 */
public class QFormLayout extends QLayout {

    public QFormLayout() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    private native long nativeCreate();

    /** 添加一行（文本标签 + 输入控件）。 */
    public void addRow(String label, QWidget field) {
        nativeAddRowString(nativeHandle, label, field.nativeHandle());
    }
    private native void nativeAddRowString(long handle, String label, long fieldHandle);

    /** 添加一行（控件标签 + 输入控件）。 */
    public void addRow(QWidget label, QWidget field) {
        nativeAddRowWidget(nativeHandle, label.nativeHandle(), field.nativeHandle());
    }
    private native void nativeAddRowWidget(long handle, long labelHandle, long fieldHandle);

// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** hasHeightForWidth（Qt hasHeightForWidth）。 */
    public boolean hasHeightForWidth() {
        return nativeHasHeightForWidth(nativeHandle);
    }
    private static native boolean nativeHasHeightForWidth(long nativeHandle);

    /** heightForWidth（Qt heightForWidth）。 */
    public int heightForWidth(int arg0) {
        return nativeHeightForWidth(nativeHandle, arg0);
    }
    private static native int nativeHeightForWidth(long nativeHandle, int arg0);

    /** invalidate（Qt invalidate）。 */
    public void invalidate() {
        nativeInvalidate(nativeHandle);
    }
    private static native void nativeInvalidate(long nativeHandle);

    /** setRowVisible（Qt setRowVisible）。 */
    public void setRowVisible(int arg0, boolean arg1) {
        nativeSetRowVisible(nativeHandle, arg0, arg1);
    }
    private static native void nativeSetRowVisible(long nativeHandle, int arg0, boolean arg1);

    /** setVerticalSpacing（Qt setVerticalSpacing）。 */
    public void setVerticalSpacing(int arg0) {
        nativeSetVerticalSpacing(nativeHandle, arg0);
    }
    private static native void nativeSetVerticalSpacing(long nativeHandle, int arg0);

    /** spacing（Qt spacing）。 */
    public int spacing() {
        return nativeSpacing(nativeHandle);
    }
    private static native int nativeSpacing(long nativeHandle);

    /** verticalSpacing（Qt verticalSpacing）。 */
    public int verticalSpacing() {
        return nativeVerticalSpacing(nativeHandle);
    }
    private static native int nativeVerticalSpacing(long nativeHandle);

}