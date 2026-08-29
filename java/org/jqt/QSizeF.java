/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 二维双精度尺寸（Qt {@code QSizeF} 值类，纯 Java 实现）。
 * <p>完整覆盖 Qt 6 QSizeF API。
 */
public class QSizeF {

    public enum AspectRatioMode {
        IgnoreAspectRatio(0), KeepAspectRatio(1), KeepAspectRatioByExpanding(2);
        final int value;
        AspectRatioMode(int v) { value = v; }
    }

    private double width;
    private double height;

    public QSizeF() { this(0, 0); }
    public QSizeF(double width, double height) { this.width = width; this.height = height; }
    public QSizeF(QSize size) { this.width = size.width(); this.height = size.height(); }

    public double width() { return width; }
    public double height() { return height; }
    public void setWidth(double width) { this.width = width; }
    public void setHeight(double height) { this.height = height; }
    public double rwidth() { return width; }
    public double rheight() { return height; }

    public boolean isEmpty() { return width == 0 || height == 0; }
    public boolean isNull() { return width == 0 && height == 0; }
    public boolean isValid() { return width > 0 && height > 0; }

    public QSizeF expandedTo(QSizeF other) {
        return new QSizeF(Math.max(width, other.width), Math.max(height, other.height));
    }
    public QSizeF boundedTo(QSizeF other) {
        return new QSizeF(Math.min(width, other.width), Math.min(height, other.height));
    }
    public QSizeF grownBy(QMarginsF m) {
        return new QSizeF(width + m.left() + m.right(), height + m.top() + m.bottom());
    }
    public QSizeF shrunkBy(QMarginsF m) {
        return new QSizeF(width - m.left() - m.right(), height - m.top() - m.bottom());
    }
    public void transpose() { double t = width; width = height; height = t; }
    public QSizeF transposed() { return new QSizeF(height, width); }

    public void scale(double w, double h, AspectRatioMode mode) {
        QSizeF s = scaled(w, h, mode);
        width = s.width; height = s.height;
    }
    public QSizeF scaled(double w, double h, AspectRatioMode mode) {
        if (mode == AspectRatioMode.IgnoreAspectRatio || width == 0 || height == 0) {
            return new QSizeF(w, h);
        }
        double r = Math.min(w / width, h / height);
        if (mode == AspectRatioMode.KeepAspectRatioByExpanding) {
            r = Math.max(w / width, h / height);
        }
        return new QSizeF(width * r, height * r);
    }

    /** 转整数尺寸（四舍五入）。 */
    public QSize toSize() { return new QSize((int) Math.round(width), (int) Math.round(height)); }

    public QSizeF plus(QSizeF s) { return new QSizeF(width + s.width, height + s.height); }
    public QSizeF minus(QSizeF s) { return new QSizeF(width - s.width, height - s.height); }
    public QSizeF multiply(double factor) { return new QSizeF(width * factor, height * factor); }
    public QSizeF divide(double divisor) { return new QSizeF(width / divisor, height / divisor); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QSizeF)) return false;
        QSizeF s = (QSizeF) o;
        return Double.compare(width, s.width) == 0 && Double.compare(height, s.height) == 0;
    }

    @Override
    public int hashCode() {
        long a = Double.doubleToLongBits(width), b = Double.doubleToLongBits(height);
        return (int) (31 * a + b);
    }

    @Override
    public String toString() { return "QSizeF(" + width + "x" + height + ")"; }
}
