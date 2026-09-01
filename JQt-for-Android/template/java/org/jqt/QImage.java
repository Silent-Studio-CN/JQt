/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.lang.ref.Cleaner;

/**
 * 图像（Qt {@code QImage}，native 句柄管理 + Java Cleaner 自动释放）。
 * <p>完整覆盖 Qt 6 QImage 常用 API。(Android 构建：AWT 桥已裁剪。)
 */
public class QImage {

    private static final Cleaner CLEANER = Cleaner.create();

    /** 像素格式（Qt QImage::Format 常用值）。 */
    public enum Format {
        ARGB32(5), RGB32(4), RGB888(13), ARGB32_Premultiplied(6), RGB555(11), RGB666(12),
        Grayscale8(24), Indexed8(3), Format_Invalid(0);
        public final int value;
        Format(int v) { value = v; }
    }

    private final long nativeHandle;
    private final Cleaner.Cleanable cleanable;

    public QImage() {
        nativeHandle = nativeCreate();
        cleanable = CLEANER.register(this, new Disposer(nativeHandle));
    }

    public QImage(int width, int height, Format format) {
        nativeHandle = nativeCreateWHF(width, height, format.value);
        cleanable = CLEANER.register(this, new Disposer(nativeHandle));
    }

    public QImage(String fileName) {
        nativeHandle = nativeCreateFromFile(fileName);
        cleanable = CLEANER.register(this, new Disposer(nativeHandle));
    }

    /** 借用手柄（QPixmap.toImage 等跨类使用，同包可见）。 */
    QImage(long borrowed) {
        nativeHandle = borrowed;
        cleanable = CLEANER.register(this, new Disposer(borrowed));
    }

    private static final class Disposer implements Runnable {
        private long h;
        Disposer(long h) { this.h = h; }
        @Override public void run() { if (h != 0) { nativeDispose(h); h = 0; } }
    }

    private static native long nativeCreate();
    private static native long nativeCreateWHF(int width, int height, int format);
    private static native long nativeCreateFromFile(String fileName);
    private static native void nativeDispose(long handle);

    public boolean isNull() { return nativeHandle == 0 || nativeIsNull(nativeHandle); }
    private static native boolean nativeIsNull(long handle);
    public int width() { return nativeWidth(nativeHandle); }
    private static native int nativeWidth(long handle);
    public int height() { return nativeHeight(nativeHandle); }
    private static native int nativeHeight(long handle);
    public QSize size() { return new QSize(nativeWidth(nativeHandle), nativeHeight(nativeHandle)); }
    public Format format() { return Format.values()[nativeFormat(nativeHandle) == 0 ? 0 : 1]; }
    private static native int nativeFormat(long handle);

    /** 填充颜色（0xAARRGGBB）。 */
    public void fill(int argb) { nativeFill(nativeHandle, argb); }
    private static native void nativeFill(long handle, int argb);

    public boolean load(String fileName) { return nativeLoad(nativeHandle, fileName); }
    private static native boolean nativeLoad(long handle, String fileName);
    public boolean loadFromData(byte[] data) { return nativeLoadFromData(nativeHandle, data); }
    private static native boolean nativeLoadFromData(long handle, byte[] data);

    public boolean save(String fileName, String format, int quality) {
        return nativeSave(nativeHandle, fileName, format, quality);
    }
    public boolean save(String fileName) { return save(fileName, null, -1); }
    private static native boolean nativeSave(long handle, String fileName, String format, int quality);

    /** 像素颜色（0xFFRRGGBB）。 */
    public int pixel(int x, int y) { return nativePixel(nativeHandle, x, y); }
    private static native int nativePixel(long handle, int x, int y);
    public void setPixel(int x, int y, int rgb) { nativeSetPixel(nativeHandle, x, y, rgb); }
    private static native void nativeSetPixel(long handle, int x, int y, int rgb);

    /** 像素颜色（QColor，含 alpha）。 */
    public QColor pixelColor(int x, int y) {
        return new QColor(nativePixelArgb(nativeHandle, x, y));
    }
    private static native int nativePixelArgb(long handle, int x, int y);

    /** 转换格式。 */
    public QImage convertToFormat(Format format) {
        return new QImage(nativeConvertToFormat(nativeHandle, format.value));
    }
    private static native long nativeConvertToFormat(long handle, int format);

    /** 缩放。 */
    public QImage scaled(int w, int h, QPixmap.AspectRatioMode mode) {
        return new QImage(nativeScaled(nativeHandle, w, h, mode.value));
    }
    private static native long nativeScaled(long handle, int w, int h, int mode);

    /** 转 QPixmap。 */
    public QPixmap toPixmap() { return QPixmap.fromImage(this); }

    // ---- 像素桥（Android 构建：AWT 桥替换为 ARGB 数组） ----
    public static QImage fromArgb(int[] argb, int w, int h) {
        return new QImage(nativeCreateFromArgb(argb, w, h));
    }
    private static native long nativeCreateFromArgb(int[] argb, int w, int h);

    long nativeHandle() { return nativeHandle; }

    @Override
    public String toString() {
        return "QImage(" + width() + "x" + height() + (isNull() ? ", null" : "") + ")";
    }
}