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

    /**
     * 以**窗口模态**显示（Qt {@code QDialog::open}）。
     * <p><b>注意</b>：{@code open()} 不是"非模态"—— 它不阻塞调用者，但按 Qt 语义是
     * {@code Qt::WindowModal}：**父窗口输入被阻塞**（Windows 上通过禁用父窗口实现，
     * macOS 上渲染为 sheet，因此会有一次动画开销 ~280ms）。
     * <p>实测：调用 {@code open()} 后 {@code isModal()} 为 true，且会**覆盖**先前的
     * {@code setModal(false)}。真正的不阻塞且不限制父窗口请用 {@link #show()}。
     * <p>模态级别可用 {@link #windowModality()} 精确查询。
     */
    public void open() {
        nativeOpen(nativeHandle);
    }

    /**
     * 模态级别（Qt {@code Qt::WindowModality}）。
     * <p>{@code 0}=NonModal · {@code 1}=WindowModal · {@code 2}=ApplicationModal。
     * <p>布尔 {@link #isModal()} 无法区分"窗口模态"与"应用模态"，需要精确判断时用本方法
     * （v1.9.1 新增，见问题报告 J14；对应 Qt 的 QWidget::windowModality）。
     */
    public int modality() { return nativeModality(nativeHandle); }
    private native int nativeModality(long handle);

    /** 设置模态级别（0=NonModal 1=WindowModal 2=ApplicationModal）。 */
    public void setModality(int modality) { nativeSetModality(nativeHandle, modality); }
    private native void nativeSetModality(long handle, int modality);

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

    // ---- v1.8.0 L1-100：结果码 + accepted/rejected 信号 ----

    /** 以指定结果码关闭对话框（QDialog::done；模态 exec() 返回该码）。 */
    public void done(int result) {
        nativeDone(nativeHandle, result);
    }
    private native void nativeDone(long handle, int result);

    /** 对话框结果码（QDialog::result；默认 0 = Rejected，accept 后为 1）。 */
    public int result() {
        return nativeResult(nativeHandle);
    }
    private native int nativeResult(long handle);

    private final java.util.List<Runnable> onAcceptedHandlers = new java.util.ArrayList<>();
    private final java.util.List<Runnable> onRejectedHandlers = new java.util.ArrayList<>();
    private volatile boolean acceptedConn;
    private volatile boolean rejectedConn;

    /** 对话框以 Accepted 结果关闭回调（Qt accepted 信号；accept()/done(1) 触发）。链式。 */
    public QDialog onAccepted(Runnable handler) {
        onAcceptedHandlers.add(handler);
        if (!acceptedConn) {
            acceptedConn = true;
            nativeConnectAccepted(nativeHandle);
        }
        return this;
    }

    /** 对话框以 Rejected 结果关闭回调（Qt rejected 信号；reject()/done(0) 触发）。链式。 */
    public QDialog onRejected(Runnable handler) {
        onRejectedHandlers.add(handler);
        if (!rejectedConn) {
            rejectedConn = true;
            nativeConnectRejected(nativeHandle);
        }
        return this;
    }

    private native void nativeConnectAccepted(long handle);
    private native void nativeConnectRejected(long handle);

    /** 由 C++ 侧在 accepted 信号时回调（JNI）。 */
    void nativeHandleAccepted() {
        for (Runnable h : onAcceptedHandlers) {
            h.run();
        }
    }

    /** 由 C++ 侧在 rejected 信号时回调（JNI）。 */
    void nativeHandleRejected() {
        for (Runnable h : onRejectedHandlers) {
            h.run();
        }
    }

// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** isModal（Qt isModal）。 */
    public boolean isModal() {
        return nativeIsModal(nativeHandle);
    }
    private static native boolean nativeIsModal(long nativeHandle);

    /** isSizeGripEnabled（Qt isSizeGripEnabled）。 */
    public boolean isSizeGripEnabled() {
        return nativeIsSizeGripEnabled(nativeHandle);
    }
    private static native boolean nativeIsSizeGripEnabled(long nativeHandle);

    /** setModal（Qt setModal）。 */
    public void setModal(boolean arg0) {
        nativeSetModal(nativeHandle, arg0);
    }
    private static native void nativeSetModal(long nativeHandle, boolean arg0);

    /** setResult（Qt setResult）。 */
    public void setResult(int arg0) {
        nativeSetResult(nativeHandle, arg0);
    }
    private static native void nativeSetResult(long nativeHandle, int arg0);

    /** setSizeGripEnabled（Qt setSizeGripEnabled）。 */
    public void setSizeGripEnabled(boolean arg0) {
        nativeSetSizeGripEnabled(nativeHandle, arg0);
    }
    private static native void nativeSetSizeGripEnabled(long nativeHandle, boolean arg0);

    /** setVisible（Qt setVisible）。 */
    public void setVisible(boolean arg0) {
        nativeSetVisible(nativeHandle, arg0);
    }
    private static native void nativeSetVisible(long nativeHandle, boolean arg0);

}