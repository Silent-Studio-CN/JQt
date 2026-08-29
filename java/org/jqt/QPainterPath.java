/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.ArrayList;
import java.util.List;

/**
 * 绘画路径（Qt {@code QPainterPath}，纯 Java 值类：线段/贝塞尔/矩形集合）。
 */
public class QPainterPath {

    /** 线段。 */
    public static final class Segment {
        public final int type;   // 1=moveTo 2=lineTo 3=cubicTo
        public final double x1, y1, x2, y2, x3, y3;
        Segment(int type, double x1, double y1, double x2, double y2, double x3, double y3) {
            this.type = type; this.x1 = x1; this.y1 = y1;
            this.x2 = x2; this.y2 = y2; this.x3 = x3; this.y3 = y3;
        }
    }

    private final List<Segment> segments = new ArrayList<>();
    private double curX, curY;
    private double startX, startY;
    private double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
    private double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
    private boolean hasMoved;

    public QPainterPath() { }

    public void moveTo(double x, double y) {
        segments.add(new Segment(1, x, y, 0, 0, 0, 0));
        curX = x; curY = y; startX = x; startY = y; hasMoved = true;
        updateBounds(x, y);
    }
    public void moveTo(QPointF p) { moveTo(p.x(), p.y()); }

    public void lineTo(double x, double y) {
        segments.add(new Segment(2, x, y, 0, 0, 0, 0));
        curX = x; curY = y;
        updateBounds(x, y);
    }
    public void lineTo(QPointF p) { lineTo(p.x(), p.y()); }

    /** 三次贝塞尔。 */
    public void cubicTo(double c1x, double c1y, double c2x, double c2y, double ex, double ey) {
        segments.add(new Segment(3, c1x, c1y, c2x, c2y, ex, ey));
        curX = ex; curY = ey;
        updateBounds(c1x, c1y); updateBounds(c2x, c2y); updateBounds(ex, ey);
    }

    public void closeSubpath() {
        if (hasMoved) lineTo(startX, startY);
    }

    public boolean isEmpty() { return segments.isEmpty(); }

    public int elementCount() { return segments.size(); }

    public Segment elementAt(int i) { return segments.get(i); }

    public QPointF currentPosition() { return new QPointF(curX, curY); }

    /** 外接矩形。 */
    public QRectF boundingRect() {
        if (segments.isEmpty()) return new QRectF();
        return new QRectF(minX, minY, maxX - minX, maxY - minY);
    }

    /** 控制点外接矩形（Qt controlPointRect）。 */
    public QRectF controlPointRect() { return boundingRect(); }

    public boolean contains(QPointF point) {
        // 射线法（仅对封闭路径有效）
        if (segments.size() < 3) return false;
        double px = point.x(), py = point.y();
        boolean inside = false;
        double[] xs = new double[segments.size()];
        double[] ys = new double[segments.size()];
        int n = 0;
        for (Segment s : segments) {
            if (s.type == 1 || s.type == 2) { xs[n] = s.x1; ys[n] = s.y1; n++; }
        }
        if (n < 3) return false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            if (((ys[i] > py) != (ys[j] > py))
                && (px < (xs[j] - xs[i]) * (py - ys[i]) / (ys[j] - ys[i]) + xs[i])) {
                inside = !inside;
            }
        }
        return inside;
    }

    public QPainterPath translated(double dx, double dy) {
        QPainterPath p = new QPainterPath();
        for (Segment s : segments) {
            if (s.type == 1) p.moveTo(s.x1 + dx, s.y1 + dy);
            else if (s.type == 2) p.lineTo(s.x1 + dx, s.y1 + dy);
            else p.cubicTo(s.x1 + dx, s.y1 + dy, s.x2 + dx, s.y2 + dy, s.x3 + dx, s.y3 + dy);
        }
        return p;
    }

    public void addRect(QRectF rect) {
        moveTo(rect.left(), rect.top());
        lineTo(rect.right(), rect.top());
        lineTo(rect.right(), rect.bottom());
        lineTo(rect.left(), rect.bottom());
        closeSubpath();
    }
    public void addRect(QRect rect) { addRect(rect.toRectF()); }

    public void addEllipse(QRectF rect) {
        double cx = rect.center().x(), cy = rect.center().y();
        double rx = rect.width() / 2.0, ry = rect.height() / 2.0;
        double k = 0.5522847498;
        moveTo(cx + rx, cy);
        cubicTo(cx + rx, cy + ry * k, cx + rx * k, cy + ry, cx, cy + ry);
        cubicTo(cx - rx * k, cy + ry, cx - rx, cy + ry * k, cx - rx, cy);
        cubicTo(cx - rx, cy - ry * k, cx - rx * k, cy - ry, cx, cy - ry);
        cubicTo(cx + rx * k, cy - ry, cx + rx, cy - ry * k, cx + rx, cy);
        closeSubpath();
    }

    private void updateBounds(double x, double y) {
        minX = Math.min(minX, x); minY = Math.min(minY, y);
        maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
    }

    /** 扁平段数据（每 7 个：type,x1,y1,x2,y2,x3,y3；供 native 重建路径）。 */
    double[] flattenSegments() {
        double[] out = new double[segments.size() * 7];
        int i = 0;
        for (Segment s : segments) {
            out[i++] = s.type;
            out[i++] = s.x1; out[i++] = s.y1;
            out[i++] = s.x2; out[i++] = s.y2;
            out[i++] = s.x3; out[i++] = s.y3;
        }
        return out;
    }

    @Override
    public String toString() { return "QPainterPath(" + segments.size() + " elements)"; }
}
