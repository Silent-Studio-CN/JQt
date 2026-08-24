/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * JQt 所有控件的基类。
 * <p>
 * 每个控件内部持有一个 C++ 侧 Qt 对象的内存指针（{@code nativeHandle}），
 * 由 native 方法（JNI 胶水层）创建和操作。
 */
public abstract class JQtWidget {

    /** C++ 侧 Qt 对象的内存地址（jlong 指针），0 表示尚未创建。 */
    protected long nativeHandle;

    /** 控件是否已在 C++ 侧创建。 */
    public boolean isCreated() {
        return nativeHandle != 0;
    }

    /** C++ 侧 Qt 对象指针（仅供内部 / 高级用法）。 */
    public long nativeHandle() {
        return nativeHandle;
    }
}
