/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.lang.ref.Cleaner;

/**
 * 输入校验器基类（Qt {@code QValidator}，非控件）。
 * <p>
 * 内存管理：与 {@link QLayout} 同款——句柄 + {@link Cleaner} 回收；
 * 校验器不随输入控件销毁，Java 侧引用保活（{@link QLineEdit#setValidator} / {@link QComboBox#setValidator}）。
 * <p>
 * 状态常量：{@link #INVALID} / {@link #INTERMEDIATE} / {@link #ACCEPTABLE}（Qt QValidator::State）。
 */
public abstract class QValidator {

    private static final Cleaner CLEANER = Cleaner.create();

    /** 无效（Qt QValidator::Invalid）。 */
    public static final int INVALID = 0;
    /** 部分输入、暂不可接受（如数字输到一半）。 */
    public static final int INTERMEDIATE = 1;
    /** 可接受。 */
    public static final int ACCEPTABLE = 2;

    /** C++ 侧 QValidator 句柄 ID。 */
    protected long nativeHandle;

    private volatile boolean disposed;

    /** 子类构造器在 nativeHandle 赋值后调用。 */
    protected final void registerCleaner() {
        final long handle = nativeHandle;
        CLEANER.register(this, () -> nativeDispose(handle));
    }

    private static native void nativeDispose(long handle);

    /**
     * 校验文本状态（QValidator::validate）。
     * @return {@link #INVALID} / {@link #INTERMEDIATE} / {@link #ACCEPTABLE}
     */
    public int validate(String text) {
        return nativeValidate(nativeHandle, text != null ? text : "");
    }
    private static native int nativeValidate(long handle, String text);

    /** 手动释放 C++ 对象（通常无需调用，GC 自动回收；已设到输入控件上的校验器请先置 null 解绑）。 */
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
}
