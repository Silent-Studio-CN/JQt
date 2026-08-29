/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 2D 向量（Qt {@code QVector2D}，纯 Java 实现）。
 */
public class QVector2D {

    private double x;
    private double y;

    public QVector2D() { this(0, 0); }
    public QVector2D(double x, double y) { this.x = x; this.y = y; }
    public QVector2D(QPoint point) { this(point.x(), point.y()); }
    public QVector2D(QPointF point) { this(point.x(), point.y()); }

    public double x() { return x; }
    public double y() { return y; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }

    public boolean isNull() { return x == 0 && y == 0; }
    public double length() { return Math.hypot(x, y); }
    public double lengthSquared() { return x * x + y * y; }

    public QVector2D normalized() {
        double len = length();
        if (len == 0) return new QVector2D();
        return new QVector2D(x / len, y / len);
    }
    public void normalize() {
        double len = length();
        if (len != 0) { x /= len; y /= len; }
    }

    public double distanceToPoint(QVector2D p) { return minus(p).length(); }
    public double distanceToLine(QVector2D point, QVector2D direction) {
        QVector2D v = point.minus(this);
        double t = v.x * direction.x + v.y * direction.y;
        if (direction.lengthSquared() == 0) return v.length();
        QVector2D proj = direction.multiply(t / direction.lengthSquared());
        return v.minus(proj).length();
    }

    public double dotProduct(QVector2D v) { return x * v.x + y * v.y; }
    public QVector3D toVector3D() { return new QVector3D(x, y, 0); }
    public QVector4D toVector4D() { return new QVector4D(x, y, 0, 0); }
    public QPoint toPoint() { return new QPoint((int) Math.round(x), (int) Math.round(y)); }
    public QPointF toPointF() { return new QPointF(x, y); }

    public QVector2D plus(QVector2D v) { return new QVector2D(x + v.x, y + v.y); }
    public QVector2D minus(QVector2D v) { return new QVector2D(x - v.x, y - v.y); }
    public QVector2D multiply(double factor) { return new QVector2D(x * factor, y * factor); }
    public QVector2D divide(double divisor) { return new QVector2D(x / divisor, y / divisor); }
    public QVector2D negate() { return new QVector2D(-x, -y); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QVector2D)) return false;
        QVector2D v = (QVector2D) o;
        return Double.compare(x, v.x) == 0 && Double.compare(y, v.y) == 0;
    }

    @Override
    public int hashCode() {
        long a = Double.doubleToLongBits(x), b = Double.doubleToLongBits(y);
        return (int) (31 * a + b);
    }

    @Override
    public String toString() { return "QVector2D(" + x + ", " + y + ")"; }
}
