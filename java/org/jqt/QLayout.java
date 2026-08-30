/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.lang.ref.Cleaner;

/**
 * 布局管理器基类（Phase 3）：封装 C++ 侧的 {@code QBoxLayout}。
 * <p>
 * 用法：
 * <pre>
 * QVBoxLayout vbox = new QVBoxLayout();
 * vbox.addWidget(label);
 * vbox.addWidget(button);
 * window.setLayout(vbox);
 * </pre>
 * 布局接管子控件的位置与大小；控件加入布局后不要再调用
 * {@link QMainWindow#addWidget(QWidget)} 重复添加。
 * <p>
 * 内存管理（Phase 5）：与 {@link QWidget} 相同——未安装到窗口前由
 * {@link Cleaner} 回收；{@code setLayout} 后归窗口管理。
 */
public abstract class QLayout {

    private static final Cleaner CLEANER = Cleaner.create();

    /** C++ 侧布局对象句柄 ID。 */
    protected long nativeHandle;

    private volatile boolean disposed;

    /** 子类构造器在 {@code nativeHandle} 赋值后调用。 */
    protected final void registerCleaner() {
        final long handle = nativeHandle;
        CLEANER.register(this, () -> nativeDispose(handle));
    }

    private static native void nativeDispose(long handle);

    /** 手动释放（通常无需调用）。释放后任何方法调用将抛出 {@link IllegalStateException}。 */
    public final void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        final long handle = nativeHandle;
        nativeHandle = 0;
        nativeDispose(handle);
    }

    /** 是否已释放。 */
    public final boolean isDisposed() {
        return disposed;
    }

    /** 布局是否已在 C++ 侧创建且未释放。 */
    public boolean isCreated() {
        return nativeHandle != 0 && !disposed;
    }

    /** C++ 侧句柄 ID（仅供内部 / 高级用法）。 */
    public long nativeHandle() {
        return nativeHandle;
    }

    /** 把子控件加入布局（Qt 会自动接管其排列）。 */
    public void addWidget(QWidget widget) {
        nativeAddWidget(nativeHandle, widget.nativeHandle());
    }

    /** 加入固定间距（像素，如 addSpacing(12) 在控件间留白）。 */
    public void addSpacing(int spacing) {
        nativeAddSpacing(nativeHandle, spacing);
    }
    protected native void nativeAddSpacing(long handle, int spacing);
    protected native void nativeAddWidget(long handle, long childHandle);

    /** 嵌套子布局（如 VBox 中嵌入 HBox 作为标题栏/工具行）。 */
    public void addLayout(QLayout child) {
        nativeAddLayout(nativeHandle, child.nativeHandle());
    }
    protected native void nativeAddLayout(long handle, long childLayoutHandle);

    /** 设置控件之间的间距（像素）。 */
    public void setSpacing(int spacing) {
        nativeSetSpacing(nativeHandle, spacing);
    }
    protected native void nativeSetSpacing(long handle, int spacing);

    /** 设置布局四周留白（外边距，像素；四边相同）。 */
    public void setContentsMargins(int all) {
        setContentsMargins(all, all, all, all);
    }

    /** 设置布局四周留白（外边距，像素；左/上/右/下）。 */
    public void setContentsMargins(int left, int top, int right, int bottom) {
        nativeSetContentsMargins(nativeHandle, left, top, right, bottom);
    }
    protected native void nativeSetContentsMargins(long handle, int left, int top, int right, int bottom);

    /**
     * 添加弹性空间（stretch 因子，越大占据越多剩余空间）。
     * 例如 {@code vbox.addStretch(1)} 会把控件推向顶部。
     */
    public void addStretch(int stretch) {
        nativeAddStretch(nativeHandle, stretch);
    }
    protected native void nativeAddStretch(long handle, int stretch);

    // ---- 查询（v0.6.0）----

    /** 子项数量（控件 + 间距 + 嵌套布局）。 */
    public int count() { return nativeCount(nativeHandle); }
    private static native int nativeCount(long handle);

    /** 控件间距（像素）。 */
    public int spacing() { return nativeSpacing(nativeHandle); }
    private static native int nativeSpacing(long handle);

// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** isEmpty（Qt isEmpty）。 */
    public boolean isEmpty() {
        return nativeIsEmpty(nativeHandle);
    }
    private static native boolean nativeIsEmpty(long nativeHandle);

    /** setEnabled（Qt setEnabled）。 */
    public void setEnabled(boolean arg0) {
        nativeSetEnabled(nativeHandle, arg0);
    }
    private static native void nativeSetEnabled(long nativeHandle, boolean arg0);

    /** unsetContentsMargins（Qt unsetContentsMargins）。 */
    public void unsetContentsMargins() {
        nativeUnsetContentsMargins(nativeHandle);
    }
    private static native void nativeUnsetContentsMargins(long nativeHandle);

    /** update（Qt update）。 */
    public void update() {
        nativeUpdate(nativeHandle);
    }
    private static native void nativeUpdate(long nativeHandle);

}