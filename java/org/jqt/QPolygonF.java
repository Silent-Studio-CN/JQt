/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 双精度多边形点集（Qt {@code QPolygonF} 值类，纯 Java 实现）。
 * <p>完整覆盖 Qt 6 QPolygonF API。
 */
public class QPolygonF extends java.util.ArrayList<QPointF> {

    public QPolygonF() { super(); }
    public QPolygonF(int size) { super(size); }

    public QPolygonF(QRectF rectangle) {
        super(4);
        add(rectangle.topLeft());
        add(rectangle.topRight());
        add(rectangle.bottomRight());
        add(rectangle.bottomLeft());
    }

    public static QPolygonF fromList(java.util.List<QPointF> list) {
        QPolygonF p = new QPolygonF(list.size());
        p.addAll(list);
        return p;
    }

    public int count() { return size(); }
    public QPointF at(int i) { return get(i); }
    public void append(QPointF p) { add(p); }
    public QPointF first() { return get(0); }
    public QPointF last() { return get(size() - 1); }
    public void prepend(QPointF p) { add(0, p); }
    // remove(int)：继承 ArrayList（语义=Qt QVector::remove）
    public void insert(int i, QPointF p) { add(i, p); }
    public void replace(int i, QPointF p) { set(i, p); }
    public boolean contains(QPointF p) { return super.contains(p); }

    public void setPoint(int index, QPointF p) { set(index, p); }
    public void setPoint(int index, double x, double y) { set(index, new QPointF(x, y)); }
    public void putPoints(int index, QPointF[] points) {
        for (int i = 0; i < points.length; i++) {
            int idx = index + i;
            if (idx < size()) set(idx, points[i]); else add(points[i]);
        }
    }
    public void swap(int i, int j) {
        QPointF t = get(i); set(i, get(j)); set(j, t);
    }

    public QRectF boundingRect() {
        if (isEmpty()) return new QRectF();
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (QPointF p : this) {
            minX = Math.min(minX, p.x()); minY = Math.min(minY, p.y());
            maxX = Math.max(maxX, p.x()); maxY = Math.max(maxY, p.y());
        }
        return new QRectF(minX, minY, maxX - minX, maxY - minY);
    }

    public void translate(double dx, double dy) {
        for (int i = 0; i < size(); i++) { QPointF p = get(i); set(i, new QPointF(p.x() + dx, p.y() + dy)); }
    }
    public void translate(QPointF offset) { translate(offset.x(), offset.y()); }
    public QPolygonF translated(double dx, double dy) {
        QPolygonF p = new QPolygonF(size());
        for (QPointF pt : this) p.add(new QPointF(pt.x() + dx, pt.y() + dy));
        return p;
    }
    public QPolygonF translated(QPointF offset) { return translated(offset.x(), offset.y()); }

    public boolean containsPoint(QPointF pt) {
        boolean inside = false;
        int n = size();
        if (n < 3) return false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            QPointF a = get(i), b = get(j);
            if (((a.y() > pt.y()) != (b.y() > pt.y()))
                && (pt.x() < (b.x() - a.x()) * (pt.y() - a.y()) / (b.y() - a.y()) + a.x())) {
                inside = !inside;
            }
        }
        return inside;
    }

    public QPolygonF intersected(QPolygonF other) { return clippingOp(other, false); }
    public QPolygonF united(QPolygonF other) { return clippingOp(other, true); }
    public QPolygonF subtracted(QPolygonF other) {
        QPolygonF result = new QPolygonF();
        for (QPointF p : this) { if (!other.containsPoint(p)) result.add(p); }
        return result;
    }
    public boolean intersects(QPolygonF other) { return boundingRect().intersects(other.boundingRect()); }

    private QPolygonF clippingOp(QPolygonF other, boolean union) {
        QPolygonF result = new QPolygonF();
        for (QPointF p : this) { if (other.containsPoint(p)) result.add(p); }
        for (QPointF p : other) { if (union && containsPoint(p)) result.add(p); }
        return result;
    }

    /** 转整数多边形（四舍五入）。 */
    public QPolygon toPolygon() {
        QPolygon p = new QPolygon();
        for (QPointF pt : this) p.add(new QPoint((int) Math.round(pt.x()), (int) Math.round(pt.y())));
        return p;
    }

    @Override
    public String toString() { return "QPolygonF(" + size() + " points)"; }
}
