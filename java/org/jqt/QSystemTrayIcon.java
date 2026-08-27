/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.lang.ref.Cleaner;

/**
 * 系统托盘图标：封装 C++ 侧的 {@code QSystemTrayIcon}（QObject，非 QWidget）。
 * 图标使用系统标准信息图标；气泡消息、提示文本可用。
 */
public class QSystemTrayIcon {

    private static final Cleaner CLEANER = Cleaner.create();

    /** C++ 侧对象句柄 ID。 */
    protected long nativeHandle;

    private volatile boolean disposed;

    public QSystemTrayIcon() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    private native long nativeCreate();

    private final void registerCleaner() {
        final long handle = nativeHandle;
        CLEANER.register(this, () -> nativeDispose(handle));
    }

    /** 显示托盘图标。 */
    public void show() {
        nativeShow(nativeHandle);
    }
    private native void nativeShow(long handle);

    /** 隐藏托盘图标。 */
    public void hide() {
        nativeHide(nativeHandle);
    }
    private native void nativeHide(long handle);

    /** 是否可见。 */
    public boolean isVisible() {
        return nativeIsVisible(nativeHandle);
    }
    private native boolean nativeIsVisible(long handle);

    /** 悬停提示文本。 */
    public void setToolTip(String tip) {
        nativeSetToolTip(nativeHandle, tip);
    }
    private native void nativeSetToolTip(long handle, String tip);

    /** 显示气泡消息（ms 毫秒；约 10 秒内，系统可能忽略超长显示）。 */
    public void showMessage(String title, String message, int ms) {
        nativeShowMessage(nativeHandle, title, message, ms);
    }
    private native void nativeShowMessage(long handle, String title, String message, int ms);

    /** 手动释放（通常无需调用）。 */
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        final long handle = nativeHandle;
        nativeHandle = 0;
        nativeDispose(handle);
    }

    private static native void nativeDispose(long handle);
}
