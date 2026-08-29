/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 二维双精度线段（Qt {@code QLineF} 值类，纯 Java 实现）。
 * <p>完整覆盖 Qt 6 QLineF API。
 */
public class QLineF {

    public enum IntersectType {
        NoIntersection(0), BoundedIntersection(1), UnboundedIntersection(2);
        public final int value;
        IntersectType(int v) { value = v; }
    }

    private double x1;
    private double y1;
    private double x2;
    private double y2;

    public QLineF() { this(0, 0, 0, 0); }
    public QLineF(double x1, double y1, double x2, double y2) {
        this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
    }
    public QLineF(QPointF p1, QPointF p2) {
        this(p1.x(), p1.y(), p2.x(), p2.y());
    }
    public QLineF(QLine line) {
        this(line.x1(), line.y1(), line.x2(), line.y2());
    }

    /** 极坐标构造（角度为度）。 */
    public static QLineF fromPolar(double length, double angle) {
        double rad = Math.toRadians(angle);
        return new QLineF(0, 0, length * Math.cos(rad), length * Math.sin(rad));
    }

    public QPointF p1() { return new QPointF(x1, y1); }
    public QPointF p2() { return new QPointF(x2, y2); }
    public double x1() { return x1; }
    public double x2() { return x2; }
    public double y1() { return y1; }
    public double y2() { return y2; }
    public double dx() { return x2 - x1; }
    public double dy() { return y2 - y1; }

    public boolean isNull() { return x1 == x2 && y1 == y2; }

    /** 线段长度。 */
    public double length() { return Math.hypot(dx(), dy()); }

    /** 角度（度，相对 x 轴）。 */
    public double angle() { return Math.toDegrees(Math.atan2(dy(), dx())); }

    /** 另一线段角度差（度）。 */
    public double angleTo(QLineF line) {
        double a = angle(), b = line.angle();
        double d = b - a;
        while (d > 180) d -= 360;
        while (d < -180) d += 360;
        return d;
    }

    public QPointF center() { return new QPointF((x1 + x2) / 2.0, (y1 + y2) / 2.0); }

    /** 单位方向向量。 */
    public QLineF unitVector() {
        double len = length();
        if (len == 0) return new QLineF(x1, y1, x1, y1);
        return new QLineF(x1, y1, x1 + dx() / len, y1 + dy() / len);
    }

    /** 法向量（旋转 90° 的单位向量）。 */
    public QLineF normalVector() {
        double len = length();
        if (len == 0) return new QLineF(x1, y1, x1, y1);
        return new QLineF(x1, y1, x1 - dy() / len, y1 + dx() / len);
    }

    /** 参数 t 处的点（0=起点，1=终点）。 */
    public QPointF pointAt(double t) {
        return new QPointF(x1 + dx() * t, y1 + dy() * t);
    }

    /** 与另一线段交点类型。 */
    public IntersectType intersects(QLineF line, QPointF intersectionPoint) {
        double d = dx() * line.dy() - dy() * line.dx();
        if (d == 0) return IntersectType.NoIntersection;  // 平行
        double t = ((line.x1 - x1) * line.dy() - (line.y1 - y1) * line.dx()) / d;
        double u = ((line.x1 - x1) * dy() - (line.y1 - y1) * dx()) / d;
        if (t < 0 || t > 1 || u < 0 || u > 1) return IntersectType.UnboundedIntersection;
        intersectionPoint.setX(x1 + t * dx());
        intersectionPoint.setY(y1 + t * dy());
        return IntersectType.BoundedIntersection;
    }

    public void setLine(double x1, double y1, double x2, double y2) {
        this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
    }
    public void setPoints(QPointF p1, QPointF p2) {
        this.x1 = p1.x(); this.y1 = p1.y(); this.x2 = p2.x(); this.y2 = p2.y();
    }
    public void setP1(QPointF p1) { this.x1 = p1.x(); this.y1 = p1.y(); }
    public void setP2(QPointF p2) { this.x2 = p2.x(); this.y2 = p2.y(); }

    /** 设置角度（保持长度不变，就地修改）。 */
    public void setAngle(double angle) {
        double len = length();
        double rad = Math.toRadians(angle);
        x2 = x1 + len * Math.cos(rad);
        y2 = y1 + len * Math.sin(rad);
    }

    /** 设置长度（保持角度不变，就地修改）。 */
    public void setLength(double len) {
        double rad = Math.toRadians(angle());
        x2 = x1 + len * Math.cos(rad);
        y2 = y1 + len * Math.sin(rad);
    }

    public void translate(QPointF offset) { translate(offset.x(), offset.y()); }
    public void translate(double dx, double dy) { x1 += dx; y1 += dy; x2 += dx; y2 += dy; }
    public QLineF translated(QPointF offset) { return translated(offset.x(), offset.y()); }
    public QLineF translated(double dx, double dy) {
        return new QLineF(x1 + dx, y1 + dy, x2 + dx, y2 + dy);
    }

    /** 转整数线段（四舍五入）。 */
    public QLine toLine() {
        return new QLine((int) Math.round(x1), (int) Math.round(y1),
                         (int) Math.round(x2), (int) Math.round(y2));
    }

    public QLineF plus(QPointF p) { return translated(p); }
    public QLineF minus(QPointF p) { return translated(-p.x(), -p.y()); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QLineF)) return false;
        QLineF l = (QLineF) o;
        return Double.compare(x1, l.x1) == 0 && Double.compare(y1, l.y1) == 0
            && Double.compare(x2, l.x2) == 0 && Double.compare(y2, l.y2) == 0;
    }

    @Override
    public int hashCode() {
        long a = Double.doubleToLongBits(x1), b = Double.doubleToLongBits(y1);
        long c = Double.doubleToLongBits(x2), d = Double.doubleToLongBits(y2);
        int r = (int) a; r = 31 * r + (int) b; r = 31 * r + (int) c; r = 31 * r + (int) d;
        return r;
    }

    @Override
    public String toString() {
        return "QLineF(" + x1 + "," + y1 + "," + x2 + "," + y2 + ")";
    }
}
