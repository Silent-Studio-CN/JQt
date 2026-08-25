/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.lang.ref.Cleaner;

/**
 * JQt 所有控件的基类。
 * <p>
 * 每个控件内部持有 C++ 侧 Qt 对象的内存句柄（{@code nativeHandle}）。
 *
 * <h3>内存管理（Phase 5）</h3>
 * <ul>
 *   <li>句柄为自增 ID（非裸指针），由 C++ 注册表管理，永不复用；
 *       对已销毁对象调用方法会抛出 {@link IllegalStateException}，不会 native 崩溃。</li>
 *   <li>Java 对象不可达时，由 {@link Cleaner} 自动回收 C++ 对象（GUI 线程安全删除）。</li>
 *   <li>控件加入窗口/布局后，生命周期由 Qt 父子关系管理，Cleaner 不再干预。</li>
 *   <li>也可调用 {@link #dispose()} 手动提前释放。</li>
 * </ul>
 */
public abstract class JQtWidget {

    private static final Cleaner CLEANER = Cleaner.create();

    /** C++ 侧 Qt 对象的句柄 ID（自增、不复用），0 表示尚未创建或已释放。 */
    protected long nativeHandle;

    private volatile boolean disposed;

    /**
     * 注册清理器：对象不可达时由 Cleaner 线程调用 native 释放 C++ 对象。
     * 子类构造器在 {@code nativeHandle} 赋值后调用。
     */
    protected final void registerCleaner() {
        final long handle = nativeHandle;
        CLEANER.register(this, () -> nativeDispose(handle));
    }

    private static native void nativeDispose(long handle);

    /**
     * 设置控件级样式表（QSS），只影响本控件及其子控件。
     * 与 {@link JQtApplication#setStyleSheet(String)} 的全局样式可叠加（控件级优先）。
     */
    public void setStyleSheet(String qss) {
        nativeSetStyleSheet(nativeHandle, qss);
    }
    private static native void nativeSetStyleSheet(long handle, String qss);

    /**
     * 手动释放 C++ 侧对象（通常无需调用——GC 时会自动释放）。
     * 释放后再次调用本控件任何方法将抛出 {@link IllegalStateException}。
     */
    public final void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        final long handle = nativeHandle;
        nativeHandle = 0;
        nativeDispose(handle);
    }

    /** 是否已释放（调用过 {@link #dispose()} 或对象已不可达）。 */
    public final boolean isDisposed() {
        return disposed;
    }

    /** 控件是否已在 C++ 侧创建且未释放。 */
    public boolean isCreated() {
        return nativeHandle != 0 && !disposed;
    }

    /** C++ 侧句柄 ID（仅供内部 / 高级用法）。 */
    public long nativeHandle() {
        return nativeHandle;
    }
}
