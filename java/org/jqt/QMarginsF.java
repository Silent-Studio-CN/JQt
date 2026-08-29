/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 四边距双精度版（Qt {@code QMarginsF} 值类，纯 Java 实现）。
 */
public class QMarginsF {

    private double left;
    private double top;
    private double right;
    private double bottom;

    public QMarginsF() { this(0, 0, 0, 0); }
    public QMarginsF(double left, double top, double right, double bottom) {
        this.left = left; this.top = top; this.right = right; this.bottom = bottom;
    }
    public QMarginsF(QMargins margins) {
        this(margins.left(), margins.top(), margins.right(), margins.bottom());
    }

    public double left() { return left; }
    public double top() { return top; }
    public double right() { return right; }
    public double bottom() { return bottom; }
    public void setLeft(double left) { this.left = left; }
    public void setTop(double top) { this.top = top; }
    public void setRight(double right) { this.right = right; }
    public void setBottom(double bottom) { this.bottom = bottom; }

    public boolean isNull() { return left == 0 && top == 0 && right == 0 && bottom == 0; }

    /** 转整数边距（四舍五入）。 */
    public QMargins toMargins() {
        return new QMargins((int) Math.round(left), (int) Math.round(top),
                            (int) Math.round(right), (int) Math.round(bottom));
    }

    public QMarginsF plus(QMarginsF m) {
        return new QMarginsF(left + m.left, top + m.top, right + m.right, bottom + m.bottom);
    }
    public QMarginsF minus(QMarginsF m) {
        return new QMarginsF(left - m.left, top - m.top, right - m.right, bottom - m.bottom);
    }
    public QMarginsF multiply(double factor) {
        return new QMarginsF(left * factor, top * factor, right * factor, bottom * factor);
    }
    public QMarginsF divide(double divisor) {
        return new QMarginsF(left / divisor, top / divisor, right / divisor, bottom / divisor);
    }
    public QMarginsF negate() {
        return new QMarginsF(-left, -top, -right, -bottom);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QMarginsF)) return false;
        QMarginsF m = (QMarginsF) o;
        return Double.compare(left, m.left) == 0 && Double.compare(top, m.top) == 0
            && Double.compare(right, m.right) == 0 && Double.compare(bottom, m.bottom) == 0;
    }

    @Override
    public int hashCode() {
        long a = Double.doubleToLongBits(left), b = Double.doubleToLongBits(top);
        long c = Double.doubleToLongBits(right), d = Double.doubleToLongBits(bottom);
        int r = (int) a; r = 31 * r + (int) b; r = 31 * r + (int) c; r = 31 * r + (int) d;
        return r;
    }

    @Override
    public String toString() {
        return "QMarginsF(" + left + "," + top + "," + right + "," + bottom + ")";
    }
}
