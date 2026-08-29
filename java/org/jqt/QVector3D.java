/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 3D 向量（Qt {@code QVector3D}，纯 Java 实现）。
 */
public class QVector3D {

    private double x;
    private double y;
    private double z;

    public QVector3D() { this(0, 0, 0); }
    public QVector3D(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
    public QVector3D(QPoint point) { this(point.x(), point.y(), 0); }
    public QVector3D(QPointF point) { this(point.x(), point.y(), 0); }
    public QVector3D(QVector2D v) { this(v.x(), v.y(), 0); }

    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setZ(double z) { this.z = z; }

    public boolean isNull() { return x == 0 && y == 0 && z == 0; }
    public double length() { return Math.sqrt(x * x + y * y + z * z); }
    public double lengthSquared() { return x * x + y * y + z * z; }

    public QVector3D normalized() {
        double len = length();
        if (len == 0) return new QVector3D();
        return new QVector3D(x / len, y / len, z / len);
    }
    public void normalize() {
        double len = length();
        if (len != 0) { x /= len; y /= len; z /= len; }
    }

    public double distanceToPoint(QVector3D p) { return minus(p).length(); }
    public double distanceToLine(QVector3D point, QVector3D direction) {
        QVector3D v = point.minus(this);
        double d = direction.lengthSquared();
        if (d == 0) return v.length();
        double t = v.dotProduct(direction) / d;
        return v.minus(direction.multiply(t)).length();
    }
    public double distanceToPlane(QVector3D plane, QVector3D normal) {
        return dotProduct(normal) - plane.dotProduct(normal);
    }
    public double distanceToPlane(QVector3D p1, QVector3D p2, QVector3D p3) {
        QVector3D n = p2.minus(p1).crossProduct(p3.minus(p1)).normalized();
        return distanceToPlane(p1, n);
    }

    public double dotProduct(QVector3D v) { return x * v.x + y * v.y + z * v.z; }
    public QVector3D crossProduct(QVector3D v) {
        return new QVector3D(y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y * v.x);
    }
    public QVector3D normal(QVector3D v) { return crossProduct(v).normalized(); }

    public QVector2D toVector2D() { return new QVector2D(x, y); }
    public QVector4D toVector4D() { return new QVector4D(x, y, z, 0); }
    public QPoint toPoint() { return new QPoint((int) Math.round(x), (int) Math.round(y)); }
    public QPointF toPointF() { return new QPointF(x, y); }

    public QVector3D plus(QVector3D v) { return new QVector3D(x + v.x, y + v.y, z + v.z); }
    public QVector3D minus(QVector3D v) { return new QVector3D(x - v.x, y - v.y, z - v.z); }
    public QVector3D multiply(double factor) { return new QVector3D(x * factor, y * factor, z * factor); }
    public QVector3D divide(double divisor) { return new QVector3D(x / divisor, y / divisor, z / divisor); }
    public QVector3D negate() { return new QVector3D(-x, -y, -z); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QVector3D)) return false;
        QVector3D v = (QVector3D) o;
        return Double.compare(x, v.x) == 0 && Double.compare(y, v.y) == 0 && Double.compare(z, v.z) == 0;
    }

    @Override
    public int hashCode() {
        long a = Double.doubleToLongBits(x), b = Double.doubleToLongBits(y), c = Double.doubleToLongBits(z);
        int h = (int) a; h = 31 * h + (int) b; h = 31 * h + (int) c;
        return h;
    }

    @Override
    public String toString() { return "QVector3D(" + x + ", " + y + ", " + z + ")"; }
}
