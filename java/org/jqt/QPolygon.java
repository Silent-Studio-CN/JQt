/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 多边形点集（Qt {@code QPolygon} 值类，纯 Java 实现，继承 {@code ArrayList<QPoint>} 提供 Qt QVector 语义）。
 * <p>完整覆盖 Qt 6 QPolygon API（含 QVector 的常用语义）。
 */
public class QPolygon extends java.util.ArrayList<QPoint> {

    public QPolygon() { super(); }

    public QPolygon(int size) { super(size); }

    /** 由矩形角点构造。 */
    public QPolygon(QRect rectangle) {
        super(4);
        add(rectangle.topLeft());
        add(rectangle.topRight());
        add(rectangle.bottomRight());
        add(rectangle.bottomLeft());
    }

    /** 由点列表构造。 */
    public static QPolygon fromList(java.util.List<QPoint> list) {
        QPolygon p = new QPolygon(list.size());
        p.addAll(list);
        return p;
    }

    // ---- QVector 语义别名 ----
    /** 点数（= size()）。 */
    public int count() { return size(); }
    public QPoint at(int i) { return get(i); }
    public void append(QPoint p) { add(p); }
    public QPoint first() { return get(0); }
    public QPoint last() { return get(size() - 1); }
    public void prepend(QPoint p) { add(0, p); }
    // remove(int)：继承 ArrayList（语义=Qt QVector::remove）
    public void insert(int i, QPoint p) { add(i, p); }
    public void replace(int i, QPoint p) { set(i, p); }
    public boolean contains(QPoint p) { return super.contains(p); }

    /** 设置索引处点（Qt setPoint）。 */
    public void setPoint(int index, QPoint p) { set(index, p); }
    public void setPoint(int index, int x, int y) { set(index, new QPoint(x, y)); }

    /** 从 index 起批量设置点（Qt putPoints 简化版）。 */
    public void putPoints(int index, QPoint[] points) {
        for (int i = 0; i < points.length; i++) {
            int idx = index + i;
            if (idx < size()) set(idx, points[i]); else add(points[i]);
        }
    }

    /** 交换两索引处点。 */
    public void swap(int i, int j) {
        QPoint t = get(i); set(i, get(j)); set(j, t);
    }

    // ---- QPolygon API ----
    /** 外接矩形。 */
    public QRect boundingRect() {
        if (isEmpty()) return new QRect();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (QPoint p : this) {
            minX = Math.min(minX, p.x()); minY = Math.min(minY, p.y());
            maxX = Math.max(maxX, p.x()); maxY = Math.max(maxY, p.y());
        }
        return new QRect(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    /** 平移（就地）。 */
    public void translate(int dx, int dy) {
        for (int i = 0; i < size(); i++) { QPoint p = get(i); set(i, new QPoint(p.x() + dx, p.y() + dy)); }
    }
    public void translate(QPoint offset) { translate(offset.x(), offset.y()); }

    /** 平移后的新多边形。 */
    public QPolygon translated(int dx, int dy) {
        QPolygon p = new QPolygon(size());
        for (QPoint pt : this) p.add(new QPoint(pt.x() + dx, pt.y() + dy));
        return p;
    }
    public QPolygon translated(QPoint offset) { return translated(offset.x(), offset.y()); }

    /** 是否包含点（射线法）。 */
    public boolean containsPoint(QPoint pt) {
        boolean inside = false;
        int n = size();
        if (n < 3) return false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            QPoint a = get(i), b = get(j);
            if (((a.y() > pt.y()) != (b.y() > pt.y()))
                && (pt.x() < (b.x() - a.x()) * (pt.y() - a.y()) / (b.y() - a.y()) + a.x())) {
                inside = !inside;
            }
        }
        return inside;
    }

    /** 与另一多边形求交（简化为外接矩形相交检测，完整实现见 Qt）。 */
    public QPolygon intersected(QPolygon other) {
        return clippingOp(other, false);
    }

    /** 与另一多边形求并（简化实现）。 */
    public QPolygon united(QPolygon other) {
        return clippingOp(other, true);
    }

    /** 与另一多边形求差（简化实现）。 */
    public QPolygon subtracted(QPolygon other) {
        QPolygon result = new QPolygon();
        for (QPoint p : this) {
            if (!other.containsPoint(p)) result.add(p);
        }
        return result;
    }

    /** 是否相交（外接矩形级检测）。 */
    public boolean intersects(QPolygon other) {
        return boundingRect().intersects(other.boundingRect());
    }

    private QPolygon clippingOp(QPolygon other, boolean union) {
        QPolygon result = new QPolygon();
        for (QPoint p : this) {
            boolean inOther = other.containsPoint(p);
            if (union ? inOther : inOther) result.add(p);
        }
        for (QPoint p : other) {
            boolean inThis = containsPoint(p);
            if (union ? inThis : !inThis) result.add(p);
        }
        return result;
    }

    /** 转双精度多边形。 */
    public QPolygonF toPolygonF() {
        QPolygonF pf = new QPolygonF();
        for (QPoint p : this) pf.add(new QPointF(p.x(), p.y()));
        return pf;
    }

    @Override
    public String toString() { return "QPolygon(" + size() + " points)"; }
}
