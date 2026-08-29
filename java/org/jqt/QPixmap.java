/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.awt.image.BufferedImage;
import java.lang.ref.Cleaner;

/**
 * 像素图（Qt {@code QPixmap}，native 句柄管理 + Java Cleaner 自动释放）。
 * <p>完整覆盖 Qt 6 QPixmap 常用 API，核心价值：{@link #fromBufferedImage(BufferedImage)} /
 * {@link #toBufferedImage()} 与 Java AWT 图像生态互转（Java 侧图片处理 → Qt 显示一条龙）。
 */
public class QPixmap {

    private static final Cleaner CLEANER = Cleaner.create();

    private final long nativeHandle;
    private final Cleaner.Cleanable cleanable;

    /** 缩放宽高比模式（Qt AspectRatioMode + TransformMode 简化）。 */
    public enum AspectRatioMode {
        IgnoreAspectRatio(0), KeepAspectRatio(1), KeepAspectRatioByExpanding(2);
        public final int value;
        AspectRatioMode(int v) { value = v; }
    }

    public QPixmap() {
        nativeHandle = nativeCreate();
        cleanable = CLEANER.register(this, new Disposer(nativeHandle));
    }

    public QPixmap(int width, int height) {
        nativeHandle = nativeCreateWH(width, height);
        cleanable = CLEANER.register(this, new Disposer(nativeHandle));
    }

    /** 从图片文件加载（png/jpg/bmp 等）。 */
    public QPixmap(String fileName) {
        nativeHandle = nativeCreateFromFile(fileName);
        cleanable = CLEANER.register(this, new Disposer(nativeHandle));
    }

    private static final class Disposer implements Runnable {
        private long h;
        Disposer(long h) { this.h = h; }
        @Override public void run() { if (h != 0) { nativeDispose(h); h = 0; } }
    }

    private static native long nativeCreate();
    private static native long nativeCreateWH(int width, int height);
    private static native long nativeCreateFromFile(String fileName);
    private static native void nativeDispose(long handle);

    public boolean isNull() { return nativeHandle == 0 || nativeIsNull(nativeHandle); }
    private static native boolean nativeIsNull(long handle);
    public int width() { return nativeWidth(nativeHandle); }
    private static native int nativeWidth(long handle);
    public int height() { return nativeHeight(nativeHandle); }
    private static native int nativeHeight(long handle);
    public QSize size() { return new QSize(nativeWidth(nativeHandle), nativeHeight(nativeHandle)); }
    public int depth() { return nativeDepth(nativeHandle); }
    private static native int nativeDepth(long handle);

    /** 填充颜色（0xAARRGGBB）。 */
    public void fill(int argb) { nativeFill(nativeHandle, argb); }
    private static native void nativeFill(long handle, int argb);

    /** 从文件加载，成功返回 true。 */
    public boolean load(String fileName) { return nativeLoad(nativeHandle, fileName); }
    private static native boolean nativeLoad(long handle, String fileName);

    /** 从字节数据加载。 */
    public boolean loadFromData(byte[] data) { return nativeLoadFromData(nativeHandle, data); }
    private static native boolean nativeLoadFromData(long handle, byte[] data);

    /** 保存到文件。format: "PNG"/"JPG"/"BMP"...；quality 0-100（-1 默认）。 */
    public boolean save(String fileName, String format, int quality) {
        return nativeSave(nativeHandle, fileName, format, quality);
    }
    public boolean save(String fileName) { return save(fileName, null, -1); }
    private static native boolean nativeSave(long handle, String fileName, String format, int quality);

    /** 缩放。 */
    public QPixmap scaled(int w, int h, AspectRatioMode mode) {
        return new QPixmap(nativeScaled(nativeHandle, w, h, mode.value));
    }
    private static native long nativeScaled(long handle, int w, int h, int mode);
    private QPixmap(long borrowed) {
        nativeHandle = borrowed;
        cleanable = CLEANER.register(this, new Disposer(borrowed));
    }

    public QPixmap scaledToWidth(int w, AspectRatioMode mode) {
        if (width() == 0) return new QPixmap();
        return scaled(w, height() * w / width(), mode);
    }
    public QPixmap scaledToHeight(int h, AspectRatioMode mode) {
        if (height() == 0) return new QPixmap();
        return scaled(width() * h / height(), h, mode);
    }

    /** 转 QImage。 */
    public QImage toImage() { return new QImage(nativeToImage(nativeHandle)); }
    private static native long nativeToImage(long handle);

    /** 从 QImage 构造。 */
    public static QPixmap fromImage(QImage image) { return new QPixmap(nativeFromImage(image.nativeHandle())); }
    private static native long nativeFromImage(long imageHandle);

    // ---- Java 生态桥：BufferedImage 互转 ----
    /** 从 java.awt.image.BufferedImage 构造（ARGB 像素复制）。 */
    public static QPixmap fromBufferedImage(BufferedImage bi) {
        int w = bi.getWidth(), h = bi.getHeight();
        int[] argb = new int[w * h];
        bi.getRGB(0, 0, w, h, argb, 0, w);
        return new QPixmap(nativeCreateFromArgb(argb, w, h));
    }
    private static native long nativeCreateFromArgb(int[] argb, int w, int h);

    /** 转 java.awt.image.BufferedImage（ARGB）。 */
    public BufferedImage toBufferedImage() {
        int w = width(), h = height();
        if (w <= 0 || h <= 0) return null;
        int[] argb = new int[w * h];
        nativeGetArgb(nativeHandle, argb, w, h);
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        bi.setRGB(0, 0, w, h, argb, 0, w);
        return bi;
    }
    private static native void nativeGetArgb(long handle, int[] argb, int w, int h);

    long nativeHandle() { return nativeHandle; }

    @Override
    public String toString() {
        return "QPixmap(" + width() + "x" + height() + (isNull() ? ", null" : "") + ")";
    }
}
