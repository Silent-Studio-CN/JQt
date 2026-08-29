/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 二维双精度矩形（Qt {@code QRectF} 值类，纯 Java 实现）。
 * <p>完整覆盖 Qt 6 QRectF API。Qt 语义：right()=x+width、bottom()=y+height（浮点闭区间）。
 */
public class QRectF {

    private double x;
    private double y;
    private double width;
    private double height;

    public QRectF() { this(0, 0, 0, 0); }
    public QRectF(double x, double y, double width, double height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
    }
    public QRectF(QPointF topLeft, QSizeF size) {
        this(topLeft.x(), topLeft.y(), size.width(), size.height());
    }
    public QRectF(QPointF topLeft, QPointF bottomRight) {
        this(topLeft.x(), topLeft.y(),
             bottomRight.x() - topLeft.x(), bottomRight.y() - topLeft.y());
    }
    public QRectF(QRect rect) { this(rect.x(), rect.y(), rect.width(), rect.height()); }

    /** 以中心点+尺寸构造。 */
    public static QRectF fromCenter(QPointF center, QSizeF size) {
        return new QRectF(center.x() - size.width() / 2, center.y() - size.height() / 2,
                          size.width(), size.height());
    }

    public double x() { return x; }
    public double y() { return y; }
    public double width() { return width; }
    public double height() { return height; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setWidth(double width) { this.width = width; }
    public void setHeight(double height) { this.height = height; }

    public QSizeF size() { return new QSizeF(width, height); }
    public void setSize(QSizeF size) { this.width = size.width(); this.height = size.height(); }

    public double left() { return x; }
    public double right() { return x + width; }
    public double top() { return y; }
    public double bottom() { return y + height; }
    public void setLeft(double left) { width = right() - left; x = left; }
    public void setRight(double right) { width = right - x; }
    public void setTop(double top) { height = bottom() - top; y = top; }
    public void setBottom(double bottom) { height = bottom - y; }

    public QPointF topLeft() { return new QPointF(x, y); }
    public QPointF topRight() { return new QPointF(x + width, y); }
    public QPointF bottomLeft() { return new QPointF(x, y + height); }
    public QPointF bottomRight() { return new QPointF(x + width, y + height); }
    public void setTopLeft(QPointF p) { x = p.x(); y = p.y(); }
    public void setTopRight(QPointF p) { width = p.x() - x; y = p.y(); }
    public void setBottomLeft(QPointF p) { x = p.x(); height = p.y() - y; }
    public void setBottomRight(QPointF p) { width = p.x() - x; height = p.y() - y; }

    public QPointF center() { return new QPointF(x + width / 2.0, y + height / 2.0); }

    public boolean isEmpty() { return width <= 0 || height <= 0; }
    public boolean isNull() { return width == 0 && height == 0; }
    public boolean isValid() { return width > 0 && height > 0; }

    public void adjust(double dx1, double dy1, double dx2, double dy2) {
        x += dx1; y += dy1; width += dx2 - dx1; height += dy2 - dy1;
    }
    public QRectF adjusted(double dx1, double dy1, double dx2, double dy2) {
        return new QRectF(x + dx1, y + dy1, width + dx2 - dx1, height + dy2 - dy1);
    }

    public QRectF normalized() {
        QRectF r = new QRectF(x, y, width, height);
        if (r.width < 0) { r.x += r.width; r.width = -r.width; }
        if (r.height < 0) { r.y += r.height; r.height = -r.height; }
        return r;
    }

    public void moveTo(double x, double y) { this.x = x; this.y = y; }
    public void moveTo(QPointF p) { this.x = p.x(); this.y = p.y(); }
    public void moveLeft(double left) { x = left; }
    public void moveRight(double right) { x = right - width; }
    public void moveTop(double top) { y = top; }
    public void moveBottom(double bottom) { y = bottom - height; }
    public void moveTopLeft(QPointF p) { x = p.x(); y = p.y(); }
    public void moveTopRight(QPointF p) { x = p.x() - width; y = p.y(); }
    public void moveBottomLeft(QPointF p) { x = p.x(); y = p.y() - height; }
    public void moveBottomRight(QPointF p) { x = p.x() - width; y = p.y() - height; }
    public void moveCenter(QPointF p) { x = p.x() - width / 2.0; y = p.y() - height / 2.0; }

    public void translate(double dx, double dy) { x += dx; y += dy; }
    public void translate(QPointF offset) { x += offset.x(); y += offset.y(); }
    public QRectF translated(double dx, double dy) { return new QRectF(x + dx, y + dy, width, height); }
    public QRectF translated(QPointF offset) { return translated(offset.x(), offset.y()); }

    public boolean contains(double px, double py) {
        return px >= x && px <= right() && py >= y && py <= bottom();
    }
    public boolean contains(QPointF p) { return contains(p.x(), p.y()); }
    public boolean contains(QRectF r) {
        return contains(r.x, r.y) && contains(r.x + r.width, r.y + r.height);
    }

    /** 相交检测（浮点）。 */
    public boolean intersects(QRectF r) {
        return x <= r.right() && r.x <= right() && y <= r.bottom() && r.y <= bottom();
    }
    /** 求交（未相交返回空矩形）。 */
    public QRectF intersected(QRectF r) {
        double nx = Math.max(x, r.x), ny = Math.max(y, r.y);
        double nr = Math.min(right(), r.right()), nb = Math.min(bottom(), r.bottom());
        if (nx > nr || ny > nb) return new QRectF();
        return new QRectF(nx, ny, nr - nx, nb - ny);
    }
    public QRectF united(QRectF r) {
        QRectF n = normalized(); QRectF m = r.normalized();
        double nx = Math.min(n.x, m.x), ny = Math.min(n.y, m.y);
        double nr = Math.max(n.right(), m.right()), nb = Math.max(n.bottom(), m.bottom());
        return new QRectF(nx, ny, nr - nx, nb - ny);
    }

    public QRectF marginsAdded(QMarginsF m) {
        return adjusted(-m.left(), -m.top(), m.right(), m.bottom());
    }
    public QRectF marginsRemoved(QMarginsF m) {
        return adjusted(m.left(), m.top(), -m.right(), -m.bottom());
    }

    public void setRect(double x, double y, double width, double height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
    }
    public void getRect(double[] xywh) {
        if (xywh != null && xywh.length >= 4) { xywh[0] = x; xywh[1] = y; xywh[2] = width; xywh[3] = height; }
    }
    public void setCoords(double x1, double y1, double x2, double y2) {
        x = x1; y = y1; width = x2 - x1; height = y2 - y1;
    }
    public void getCoords(double[] xyxy) {
        if (xyxy != null && xyxy.length >= 4) { xyxy[0] = x; xyxy[1] = y; xyxy[2] = right(); xyxy[3] = bottom(); }
    }

    /** 转整数矩形（四舍五入）。 */
    public QRect toRect() {
        return new QRect((int) Math.round(x), (int) Math.round(y),
                         (int) Math.round(width), (int) Math.round(height));
    }
    /** 转整数矩形（向下取整）。 */
    public QRect toAlignedRect() {
        return new QRect((int) Math.floor(x), (int) Math.floor(y),
                         (int) Math.ceil(width), (int) Math.ceil(height));
    }

    public QRectF transposed() { return new QRectF(y, x, height, width); }

    public QRectF and(QRectF r) { return intersected(r); }
    public QRectF or(QRectF r) { return united(r); }
    public QRectF plus(QPointF p) { return translated(p); }
    public QRectF minus(QPointF p) { return translated(-p.x(), -p.y()); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QRectF)) return false;
        QRectF r = (QRectF) o;
        return Double.compare(x, r.x) == 0 && Double.compare(y, r.y) == 0
            && Double.compare(width, r.width) == 0 && Double.compare(height, r.height) == 0;
    }

    @Override
    public int hashCode() {
        long a = Double.doubleToLongBits(x), b = Double.doubleToLongBits(y);
        long c = Double.doubleToLongBits(width), d = Double.doubleToLongBits(height);
        int h = (int) a; h = 31 * h + (int) b; h = 31 * h + (int) c; h = 31 * h + (int) d;
        return h;
    }

    @Override
    public String toString() {
        return "QRectF(" + x + "," + y + " " + width + "x" + height + ")";
    }
}
