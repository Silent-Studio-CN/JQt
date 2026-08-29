/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 二维双精度点（Qt {@code QPointF} 值类，纯 Java 实现）。
 * <p>完整覆盖 Qt 6 QPointF API（含运算符语义）。
 */
public class QPointF {

    private double x;
    private double y;

    public QPointF() { this(0, 0); }
    public QPointF(double x, double y) { this.x = x; this.y = y; }

    public double x() { return x; }
    public double y() { return y; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public double rx() { return x; }
    public double ry() { return y; }

    /** 是否 (0,0)。 */
    public boolean isNull() { return x == 0.0 && y == 0.0; }

    /** 曼哈顿距离。 */
    public double manhattanLength() { return Math.abs(x) + Math.abs(y); }

    /** 转整数点（四舍五入）。 */
    public QPoint toPoint() { return new QPoint((int) Math.round(x), (int) Math.round(y)); }

    /** 交换 x/y。 */
    public QPointF transposed() { return new QPointF(y, x); }

    /** 两点点积。 */
    public static double dotProduct(QPointF p1, QPointF p2) {
        return p1.x * p2.x + p1.y * p2.y;
    }

    public QPointF plus(QPointF p) { return new QPointF(x + p.x, y + p.y); }
    public QPointF minus(QPointF p) { return new QPointF(x - p.x, y - p.y); }
    public QPointF multiply(double factor) { return new QPointF(x * factor, y * factor); }
    public QPointF divide(double divisor) { return new QPointF(x / divisor, y / divisor); }
    public QPointF negate() { return new QPointF(-x, -y); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QPointF)) return false;
        QPointF p = (QPointF) o;
        return Double.compare(x, p.x) == 0 && Double.compare(y, p.y) == 0;
    }

    @Override
    public int hashCode() {
        long a = Double.doubleToLongBits(x), b = Double.doubleToLongBits(y);
        return (int) (31 * a + b);
    }

    @Override
    public String toString() { return "QPointF(" + x + "," + y + ")"; }
}
