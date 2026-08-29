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
public class QProgressBar extends QWidget {

    public QProgressBar() {
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

    // ---- L1 补全（v0.6.0）----

    /** 文本对齐：1 左 / 2 右 / 4 居中。 */
    public void setAlignment(int alignment) { nativeSetAlignment(nativeHandle, alignment); }
    private static native void nativeSetAlignment(long handle, int alignment);

    /** 文本对齐。 */
    public int alignment() { return nativeAlignment(nativeHandle); }
    private static native int nativeAlignment(long handle);

    /** 当前显示文本（默认 "50%" 形式；未设置格式时）。 */
    public String text() { return nativeText(nativeHandle); }
    private static native String nativeText(long handle);

// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** format（Qt format）。 */
    public String format() {
        return nativeFormat(nativeHandle);
    }
    private static native String nativeFormat(long nativeHandle);

    /** invertedAppearance（Qt invertedAppearance）。 */
    public boolean invertedAppearance() {
        return nativeInvertedAppearance(nativeHandle);
    }
    private static native boolean nativeInvertedAppearance(long nativeHandle);

    /** isTextVisible（Qt isTextVisible）。 */
    public boolean isTextVisible() {
        return nativeIsTextVisible(nativeHandle);
    }
    private static native boolean nativeIsTextVisible(long nativeHandle);

    /** maximum（Qt maximum）。 */
    public int maximum() {
        return nativeMaximum(nativeHandle);
    }
    private static native int nativeMaximum(long nativeHandle);

    /** minimum（Qt minimum）。 */
    public int minimum() {
        return nativeMinimum(nativeHandle);
    }
    private static native int nativeMinimum(long nativeHandle);

    /** resetFormat（Qt resetFormat）。 */
    public void resetFormat() {
        nativeResetFormat(nativeHandle);
    }
    private static native void nativeResetFormat(long nativeHandle);

    /** setFormat（Qt setFormat）。 */
    public void setFormat(String arg0) {
        nativeSetFormat(nativeHandle, arg0);
    }
    private static native void nativeSetFormat(long nativeHandle, String arg0);

    /** setInvertedAppearance（Qt setInvertedAppearance）。 */
    public void setInvertedAppearance(boolean arg0) {
        nativeSetInvertedAppearance(nativeHandle, arg0);
    }
    private static native void nativeSetInvertedAppearance(long nativeHandle, boolean arg0);

    /** setMaximum（Qt setMaximum）。 */
    public void setMaximum(int arg0) {
        nativeSetMaximum(nativeHandle, arg0);
    }
    private static native void nativeSetMaximum(long nativeHandle, int arg0);

    /** setMinimum（Qt setMinimum）。 */
    public void setMinimum(int arg0) {
        nativeSetMinimum(nativeHandle, arg0);
    }
    private static native void nativeSetMinimum(long nativeHandle, int arg0);

    /** setTextVisible（Qt setTextVisible）。 */
    public void setTextVisible(boolean arg0) {
        nativeSetTextVisible(nativeHandle, arg0);
    }
    private static native void nativeSetTextVisible(long nativeHandle, boolean arg0);

}