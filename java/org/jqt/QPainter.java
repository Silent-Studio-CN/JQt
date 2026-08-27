/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 2D 画笔：封装 C++ 侧的 {@code QPainter}，仅可在 {@link QCanvasWidget#onPaint}
 * 回调期间使用（回调返回后句柄失效，继续调用将抛出 {@link IllegalStateException}）。
 * <p>所有颜色为 0xAARRGGBB。
 */
public final class QPainter {

    private long handle;

    QPainter(long handle) {
        this.handle = handle;
    }

    private void check() {
        if (handle == 0) {
            throw new IllegalStateException("QPainter 只能在 onPaint 回调内使用");
        }
    }

    void release() {
        handle = 0;
    }

    /** 设置画笔颜色（也用作填充色）。 */
    public void setColor(int argb) {
        check();
        nativeSetColor(handle, argb);
    }
    private static native void nativeSetColor(long handle, int argb);

    /** 设置描边宽度（像素）。 */
    public void setStrokeWidth(double width) {
        check();
        nativeSetStrokeWidth(handle, width);
    }
    private static native void nativeSetStrokeWidth(long handle, double width);

    /** 画线段。 */
    public void drawLine(double x1, double y1, double x2, double y2) {
        check();
        nativeDrawLine(handle, x1, y1, x2, y2);
    }
    private static native void nativeDrawLine(long handle, double x1, double y1, double x2, double y2);

    /** 画矩形边框。 */
    public void drawRect(double x, double y, double w, double h) {
        check();
        nativeDrawRect(handle, x, y, w, h);
    }
    private static native void nativeDrawRect(long handle, double x, double y, double w, double h);

    /** 填充矩形。 */
    public void fillRect(double x, double y, double w, double h) {
        check();
        nativeFillRect(handle, x, y, w, h);
    }
    private static native void nativeFillRect(long handle, double x, double y, double w, double h);

    /** 画圆边框（cx, cy 为圆心）。 */
    public void drawCircle(double cx, double cy, double r) {
        check();
        nativeDrawEllipse(handle, cx - r, cy - r, 2 * r, 2 * r);
    }
    private static native void nativeDrawEllipse(long handle, double x, double y, double w, double h);

    /** 填充圆。 */
    public void fillCircle(double cx, double cy, double r) {
        check();
        nativeFillEllipse(handle, cx - r, cy - r, 2 * r, 2 * r);
    }
    private static native void nativeFillEllipse(long handle, double x, double y, double w, double h);

    /** 画圆角矩形边框。 */
    public void drawRoundRect(double x, double y, double w, double h, double radius) {
        check();
        nativeDrawRoundRect(handle, x, y, w, h, radius);
    }
    private static native void nativeDrawRoundRect(long handle, double x, double y, double w, double h, double radius);

    /** 绘制文本（y 为基线）。 */
    public void drawText(double x, double y, String text) {
        check();
        nativeDrawText(handle, x, y, text);
    }
    private static native void nativeDrawText(long handle, double x, double y, String text);

    /** 设置字体（点大小）。 */
    public void setFont(String family, int pointSize) {
        check();
        nativeSetFont(handle, family, pointSize);
    }
    private static native void nativeSetFont(long handle, String family, int pointSize);

    /** 平移坐标系。 */
    public void translate(double dx, double dy) {
        check();
        nativeTranslate(handle, dx, dy);
    }
    private static native void nativeTranslate(long handle, double dx, double dy);

    /** 旋转坐标系（度，顺时针）。 */
    public void rotate(double degrees) {
        check();
        nativeRotate(handle, degrees);
    }
    private static native void nativeRotate(long handle, double degrees);
}
