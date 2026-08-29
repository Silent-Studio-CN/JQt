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

    // ---- 值对象批（手写精修：画刷/画笔/路径/图像/裁剪/变换） ----

    /** 设置画刷（填充样式）。 */
    public void setBrush(QBrush brush) {
        check();
        if (brush == null) return;
        nativeSetBrush(handle, brush.color().rgba(), brush.style().value);
    }
    private static native void nativeSetBrush(long handle, int argb, int style);

    /** 设置画笔（描边样式/宽度/颜色）。 */
    public void setPen(QPen pen) {
        check();
        if (pen == null) return;
        nativeSetPen(handle, pen.color().rgba(), pen.widthF(), pen.style().value);
    }
    private static native void nativeSetPen(long handle, int argb, double width, int style);

    /** 绘制路径。 */
    public void drawPath(QPainterPath path) {
        check();
        if (path == null || path.isEmpty()) return;
        nativeDrawPath(handle, path.flattenSegments());
    }
    private static native void nativeDrawPath(long handle, double[] segments);

    /** 绘制像素图（左上角 x,y）。 */
    public void drawPixmap(double x, double y, QPixmap pixmap) {
        check();
        if (pixmap == null || pixmap.isNull()) return;
        nativeDrawPixmap(handle, x, y, pixmap.nativeHandle());
    }
    private static native void nativeDrawPixmap(long handle, double x, double y, long pixmapHandle);

    /** 绘制像素图（目标矩形 + 源矩形裁剪）。 */
    public void drawPixmap(QRectF target, QPixmap pixmap, QRectF source) {
        check();
        if (pixmap == null || pixmap.isNull() || target == null || source == null) return;
        nativeDrawPixmapRect(handle, target.x(), target.y(), target.width(), target.height(),
                             pixmap.nativeHandle(), source.x(), source.y(), source.width(), source.height());
    }
    private static native void nativeDrawPixmapRect(long handle, double tx, double ty, double tw, double th,
                                                    long pixmapHandle, double sx, double sy, double sw, double sh);

    /** 绘制图像（左上角 x,y）。 */
    public void drawImage(double x, double y, QImage image) {
        check();
        if (image == null || image.isNull()) return;
        nativeDrawImage(handle, x, y, image.nativeHandle());
    }
    private static native void nativeDrawImage(long handle, double x, double y, long imageHandle);

    /** 绘制图像（目标矩形 + 源矩形裁剪）。 */
    public void drawImage(QRectF target, QImage image, QRectF source) {
        check();
        if (image == null || image.isNull() || target == null || source == null) return;
        nativeDrawImageRect(handle, target.x(), target.y(), target.width(), target.height(),
                            image.nativeHandle(), source.x(), source.y(), source.width(), source.height());
    }
    private static native void nativeDrawImageRect(long handle, double tx, double ty, double tw, double th,
                                                   long imageHandle, double sx, double sy, double sw, double sh);

    /** 绘制静态文本（简化：按普通文本绘制）。 */
    public void drawStaticText(double x, double y, QStaticText text) {
        check();
        if (text == null) return;
        nativeDrawText(handle, x, y, text.text());
    }

    /** 平铺绘制像素图。 */
    public void drawTiledPixmap(QRectF rect, QPixmap pixmap, QPointF offset) {
        check();
        if (pixmap == null || pixmap.isNull() || rect == null) return;
        double ox = offset != null ? offset.x() : 0;
        double oy = offset != null ? offset.y() : 0;
        nativeDrawTiledPixmap(handle, rect.x(), rect.y(), rect.width(), rect.height(),
                              pixmap.nativeHandle(), ox, oy);
    }
    private static native void nativeDrawTiledPixmap(long handle, double rx, double ry, double rw, double rh,
                                                     long pixmapHandle, double ox, double oy);

    /** 绘制图片记录（简化：无操作占位，播放引擎随 QPicture 集成）。 */
    public void drawPicture(double x, double y, QPicture picture) {
        check();
        if (picture == null || picture.isNull()) return;
        nativeDrawPicture(handle, x, y, picture.width(), picture.height());
    }
    private static native void nativeDrawPicture(long handle, double x, double y, double w, double h);

    /** 绘制字形运行（简化：无操作占位）。 */
    public void drawGlyphRun(QPointF position, QGlyphRun glyphRun) {
        check();
        if (glyphRun == null || glyphRun.isEmpty()) return;
        nativeDrawGlyphRun(handle, position != null ? position.x() : 0, position != null ? position.y() : 0);
    }
    private static native void nativeDrawGlyphRun(long handle, double x, double y);

    /** 填充路径。 */
    public void fillPath(QPainterPath path, QBrush brush) {
        check();
        if (path == null || path.isEmpty() || brush == null) return;
        nativeFillPath(handle, path.flattenSegments(), brush.color().rgba(), brush.style().value);
    }
    private static native void nativeFillPath(long handle, double[] segments, int argb, int style);

    /** 描边路径。 */
    public void strokePath(QPainterPath path, QPen pen) {
        check();
        if (path == null || path.isEmpty() || pen == null) return;
        nativeStrokePath(handle, path.flattenSegments(), pen.color().rgba(), pen.widthF(), pen.style().value);
    }
    private static native void nativeStrokePath(long handle, double[] segments, int argb, double width, int style);

    /** 设置裁剪路径。 */
    public void setClipPath(QPainterPath path) {
        check();
        if (path == null || path.isEmpty()) return;
        nativeSetClipPath(handle, path.flattenSegments());
    }
    private static native void nativeSetClipPath(long handle, double[] segments);

    /** 设置裁剪矩形。 */
    public void setClipRect(QRectF rect) {
        check();
        if (rect == null) return;
        nativeSetClipRect(handle, rect.x(), rect.y(), rect.width(), rect.height());
    }
    private static native void nativeSetClipRect(long handle, double x, double y, double w, double h);

    /** 设置裁剪区域（简化：使用外接矩形）。 */
    public void setClipRegion(QRegion region) {
        check();
        if (region == null || region.isEmpty()) return;
        QRectF b = region.boundingRect().toRectF();
        nativeSetClipRect(handle, b.x(), b.y(), b.width(), b.height());
    }

    /** 设置世界变换。 */
    public void setWorldTransform(QTransform transform) {
        check();
        if (transform == null) return;
        nativeSetWorldTransform(handle, transform.m11(), transform.m12(), transform.m13(),
                                transform.m21(), transform.m22(), transform.m23(),
                                transform.m31(), transform.m32(), transform.m33());
    }
    private static native void nativeSetWorldTransform(long handle, double m11, double m12, double m13,
                                                       double m21, double m22, double m23,
                                                       double m31, double m32, double m33);
}
