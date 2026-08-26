/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 进度条（QProgressBar 封装）。Fluent 风格：QSS 里 QProgressBar::chunk 圆角 + accent 色。
 */
public class JQtProgressBar extends JQtWidget {

    public JQtProgressBar() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    /** 当前进度值。 */
    public int value() {
        return nativeValue(nativeHandle);
    }

    /** 设置进度值（范围 0~100，可 setRange 修改）。 */
    public void setValue(int value) {
        nativeSetValue(nativeHandle, value);
    }

    /** 设置范围（默认 0~100）。 */
    public void setRange(int min, int max) {
        nativeSetRange(nativeHandle, min, max);
    }

    private native long nativeCreate();
    private native int nativeValue(long handle);
    private native void nativeSetValue(long handle, int value);
    private native void nativeSetRange(long handle, int min, int max);
}
