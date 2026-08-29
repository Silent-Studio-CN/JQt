/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 二维整数点（Qt {@code QPoint} 值类，纯 Java 实现，无 JNI）。
 * <p>完整覆盖 Qt 6 QPoint API（含运算符语义的等价方法）。
 * <p>Qt 成员覆盖：x/y/setX/setY/rx/ry/isNull/manhattanLength/transposed/toPointF/dotProduct/运算符；toCGPoint 为 macOS 专属不提供。
 */
public class QPoint {

    private int x;
    private int y;

    public QPoint() {
        this(0, 0);
    }

    public QPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /** x 坐标。 */
    public int x() { return x; }

    /** y 坐标。 */
    public int y() { return y; }

    /** 设置 x 坐标。 */
    public void setX(int x) { this.x = x; }

    /** 设置 y 坐标。 */
    public void setY(int y) { this.y = y; }

    /** Qt {@code rx()}：返回 x 的引用语义（Java 无引用返回，修改请用 {@link #setX(int)}）。 */
    public int rx() { return x; }

    /** Qt {@code ry()}：返回 y 的引用语义（修改请用 {@link #setY(int)}）。 */
    public int ry() { return y; }

    /** 是否 (0,0)。 */
    public boolean isNull() { return x == 0 && y == 0; }

    /** 曼哈顿距离 |x|+|y|。 */
    public int manhattanLength() { return Math.abs(x) + Math.abs(y); }

    /** 交换 x/y 的新点。 */
    public QPoint transposed() { return new QPoint(y, x); }

    /** 转双精度点。 */
    public QPointF toPointF() { return new QPointF(x, y); }

    /** 两点点积。 */
    public static int dotProduct(QPoint p1, QPoint p2) {
        return p1.x * p2.x + p1.y * p2.y;
    }

    // ---- 运算符语义（Qt operator+ - * / 一元-） ----

    public QPoint plus(QPoint p) { return new QPoint(x + p.x, y + p.y); }
    public QPoint minus(QPoint p) { return new QPoint(x - p.x, y - p.y); }
    public QPoint multiply(int factor) { return new QPoint(x * factor, y * factor); }
    public QPoint divide(int divisor) { return new QPoint(x / divisor, y / divisor); }
    public QPoint negate() { return new QPoint(-x, -y); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QPoint)) return false;
        QPoint p = (QPoint) o;
        return x == p.x && y == p.y;
    }

    @Override
    public int hashCode() { return 31 * x + y; }

    @Override
    public String toString() { return "QPoint(" + x + "," + y + ")"; }
}
