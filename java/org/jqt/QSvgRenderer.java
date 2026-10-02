/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.lang.ref.Cleaner;

/**
 * SVG 渲染器（Qt {@code QSvgRenderer}，QtSvg 模块）。
 * <pre>
 * QSvgRenderer svg = new QSvgRenderer("icons/home.svg");
 * if (svg.isValid()) {
 *     QSize s = svg.defaultSize();
 *     byte[] png = svg.renderToPng(64, 64);      // 栅格化成 PNG 字节
 *     QImage img = new QImage();
 *     img.loadFromData(png);                     // 交给 QImage 继续用
 * }
 * </pre>
 * 也支持直接给 SVG 文本:{@link #QSvgRenderer(byte[])} 传 UTF-8 字节。
 */
public class QSvgRenderer {

    private static final Cleaner CLEANER = Cleaner.create();

    private long nativeHandle;

    /** 从文件加载（路径或 Qt 资源路径）。 */
    public QSvgRenderer(String fileName) {
        requireAvailable();
        nativeHandle = nativeCreateFromFile(fileName);
        final long h = nativeHandle;
        CLEANER.register(this, () -> nativeDispose(h));
    }

    /** 从内存字节加载（UTF-8 编码的 SVG 文本）。 */
    public QSvgRenderer(byte[] svgData) {
        requireAvailable();
        nativeHandle = nativeCreateFromData(svgData);
        final long h = nativeHandle;
        CLEANER.register(this, () -> nativeDispose(h));
    }

    private static native long nativeCreateFromFile(String fileName);
    private static native long nativeCreateFromData(byte[] data);
    private static native void nativeDispose(long handle);

    /**
     * 本库是否编译进了 QtSvg 支持。
     * <p>某些平台未安装 qtsvg 模块时，构建会跳过这部分（特性探测），
     * 此时构造 {@link QSvgRenderer} 会抛出明确异常而不是让 JNI 找不到符号。
     */
    public static boolean isAvailable() { return nativeAvailable(); }
    private static native boolean nativeAvailable();

    private static void requireAvailable() {
        if (!isAvailable()) {
            throw new IllegalStateException(
                "本库未编译 QtSvg 支持(缺少 qtsvg 模块);请安装 Qt 的 qtsvg 后重新构建 libjqt");
        }
    }

    /** SVG 是否解析成功。 */
    public boolean isValid() { return nativeIsValid(nativeHandle); }
    private static native boolean nativeIsValid(long handle);

    /** 默认尺寸（SVG 里声明的 width/height，或 viewBox 推算）。 */
    public QSize defaultSize() {
        int[] wh = nativeDefaultSize(nativeHandle);
        return new QSize(wh[0], wh[1]);
    }
    private static native int[] nativeDefaultSize(long handle);

    /**
     * 栅格化为 PNG 字节（按给定像素尺寸等比填充）。
     * <p>比 renderToImage 更省事:不需要在 Java 侧管理额外句柄。
     */
    public byte[] renderToPng(int width, int height) {
        return nativeRenderToPng(nativeHandle, Math.max(1, width), Math.max(1, height));
    }
    private static native byte[] nativeRenderToPng(long handle, int width, int height);
}
