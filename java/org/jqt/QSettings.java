/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.lang.ref.Cleaner;

/**
 * 应用设置（注册表/INI）：封装 C++ 侧的 {@code QSettings}。
 * <pre>
 * QSettings s = new QSettings();
 * s.setValue("window/x", 100);
 * int x = s.value("window/x");
 * </pre>
 */
public class QSettings {

    private static final Cleaner CLEANER = Cleaner.create();

    /** C++ 侧对象句柄 ID。 */
    protected long nativeHandle;

    private volatile boolean disposed;

    public QSettings() {
        nativeHandle = nativeCreate();
        CLEANER.register(this, () -> nativeDispose(nativeHandle));
    }

    private native long nativeCreate();
    private static native void nativeDispose(long handle);

    /** 读取值（整数）。 */
    public int value(String key) {
        return nativeValue(nativeHandle, key);
    }
    private native int nativeValue(long handle, String key);

    /** 写入值。 */
    public void setValue(String key, int value) {
        nativeSetValue(nativeHandle, key, value);
    }
    private native void nativeSetValue(long handle, String key, int value);

    /** 键是否存在。 */
    public boolean contains(String key) {
        return nativeContains(nativeHandle, key);
    }
    private native boolean nativeContains(long handle, String key);

    /** 删除键。 */
    public void remove(String key) {
        nativeRemove(nativeHandle, key);
    }
    private native void nativeRemove(long handle, String key);

    /** 清空全部。 */
    public void clear() {
        nativeClear(nativeHandle);
    }
    private native void nativeClear(long handle);

// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** endArray（Qt endArray）。 */
    public void endArray() {
        nativeEndArray(nativeHandle);
    }
    private static native void nativeEndArray(long nativeHandle);

    /** setArrayIndex（Qt setArrayIndex）。 */
    public void setArrayIndex(int arg0) {
        nativeSetArrayIndex(nativeHandle, arg0);
    }
    private static native void nativeSetArrayIndex(long nativeHandle, int arg0);

    /** setAtomicSyncRequired（Qt setAtomicSyncRequired）。 */
    public void setAtomicSyncRequired(boolean arg0) {
        nativeSetAtomicSyncRequired(nativeHandle, arg0);
    }
    private static native void nativeSetAtomicSyncRequired(long nativeHandle, boolean arg0);

    /** setFallbacksEnabled（Qt setFallbacksEnabled）。 */
    public void setFallbacksEnabled(boolean arg0) {
        nativeSetFallbacksEnabled(nativeHandle, arg0);
    }
    private static native void nativeSetFallbacksEnabled(long nativeHandle, boolean arg0);

    /** sync（Qt sync）。 */
    public void sync() {
        nativeSync(nativeHandle);
    }
    private static native void nativeSync(long nativeHandle);

}