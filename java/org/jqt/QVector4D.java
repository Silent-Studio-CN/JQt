/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 4D 向量（Qt {@code QVector4D}，纯 Java 实现）。
 */
public class QVector4D {

    private double x;
    private double y;
    private double z;
    private double w;

    public QVector4D() { this(0, 0, 0, 0); }
    public QVector4D(double x, double y, double z, double w) { this.x = x; this.y = y; this.z = z; this.w = w; }
    public QVector4D(QVector2D v) { this(v.x(), v.y(), 0, 0); }
    public QVector4D(QVector3D v) { this(v.x(), v.y(), v.z(), 0); }
    public QVector4D(QPoint p) { this(p.x(), p.y(), 0, 0); }
    public QVector4D(QPointF p) { this(p.x(), p.y(), 0, 0); }

    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public double w() { return w; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setZ(double z) { this.z = z; }
    public void setW(double w) { this.w = w; }

    public boolean isNull() { return x == 0 && y == 0 && z == 0 && w == 0; }
    public double length() { return Math.sqrt(x * x + y * y + z * z + w * w); }
    public double lengthSquared() { return x * x + y * y + z * z + w * w; }

    public QVector4D normalized() {
        double len = length();
        if (len == 0) return new QVector4D();
        return new QVector4D(x / len, y / len, z / len, w / len);
    }
    public void normalize() {
        double len = length();
        if (len != 0) { x /= len; y /= len; z /= len; w /= len; }
    }

    public double dotProduct(QVector4D v) { return x * v.x + y * v.y + z * v.z + w * v.w; }
    public double distanceToPoint(QVector4D p) { return minus(p).length(); }

    public QVector2D toVector2D() { return new QVector2D(x, y); }
    public QVector3D toVector3D() { return new QVector3D(x, y, z); }
    public QPoint toPoint() { return new QPoint((int) Math.round(x), (int) Math.round(y)); }
    public QPointF toPointF() { return new QPointF(x, y); }

    public QVector4D plus(QVector4D v) { return new QVector4D(x + v.x, y + v.y, z + v.z, w + v.w); }
    public QVector4D minus(QVector4D v) { return new QVector4D(x - v.x, y - v.y, z - v.z, w - v.w); }
    public QVector4D multiply(double factor) { return new QVector4D(x * factor, y * factor, z * factor, w * factor); }
    public QVector4D divide(double divisor) { return new QVector4D(x / divisor, y / divisor, z / divisor, w / divisor); }
    public QVector4D negate() { return new QVector4D(-x, -y, -z, -w); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QVector4D)) return false;
        QVector4D v = (QVector4D) o;
        return Double.compare(x, v.x) == 0 && Double.compare(y, v.y) == 0
            && Double.compare(z, v.z) == 0 && Double.compare(w, v.w) == 0;
    }

    @Override
    public int hashCode() {
        long a = Double.doubleToLongBits(x), b = Double.doubleToLongBits(y);
        long c = Double.doubleToLongBits(z), d = Double.doubleToLongBits(w);
        int h = (int) a; h = 31 * h + (int) b; h = 31 * h + (int) c; h = 31 * h + (int) d;
        return h;
    }

    @Override
    public String toString() { return "QVector4D(" + x + ", " + y + ", " + z + ", " + w + ")"; }
}
