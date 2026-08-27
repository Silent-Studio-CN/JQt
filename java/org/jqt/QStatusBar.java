/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 状态栏：封装 C++ 侧的 {@code QStatusBar}，位于窗口底部显示提示信息。
 */
public class QStatusBar extends QWidget {

    public QStatusBar() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    private native long nativeCreate();

    /** 显示临时消息（ms 毫秒后自动清除；传 0 表示常驻直到下一次消息）。 */
    public void showMessage(String text, int ms) {
        nativeShowMessage(nativeHandle, text, ms);
    }
    private native void nativeShowMessage(long handle, String text, int ms);

    /** 清除当前消息。 */
    public void clearMessage() {
        nativeClearMessage(nativeHandle);
    }
    private native void nativeClearMessage(long handle);

    /** 当前消息文本（无消息返回空串）。 */
    public String currentMessage() {
        return nativeCurrentMessage(nativeHandle);
    }
    private native String nativeCurrentMessage(long handle);
}
