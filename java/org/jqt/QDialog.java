/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 对话框基类（QDialog）：模态 exec / 非模态 open。
 * <p>
 * L1 补全（v0.7.1）。
 */
public class QDialog extends QWidget {

    /** 创建对话框。 */
    public QDialog() {
        this("", 0);
    }

    /** 创建对话框（指定标题；parent 为 null 时为独立顶层窗口）。 */
    public QDialog(String title, long parentHandle) {
        nativeHandle = nativeCreate(title, parentHandle);
        registerCleaner();
    }

    private native long nativeCreate(String title, long parentHandle);

    /**
     * 模态显示（阻塞直到关闭；QDialog::exec）。
     * @return 1 = Accepted（accept 关闭）/ 0 = Rejected
     */
    public int exec() {
        return nativeExec(nativeHandle);
    }

    /** 非模态显示（QDialog::open；不阻塞，窗口置顶显示）。 */
    public void open() {
        nativeOpen(nativeHandle);
    }

    /** 以 Accepted 结果关闭（QDialog::accept）。 */
    public void accept() {
        nativeAccept(nativeHandle);
    }

    /** 以 Rejected 结果关闭（QDialog::reject）。 */
    public void reject() {
        nativeReject(nativeHandle);
    }

    private native int nativeExec(long handle);
    private native void nativeOpen(long handle);
    private native void nativeAccept(long handle);
    private native void nativeReject(long handle);
}
