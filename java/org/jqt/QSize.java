/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 二维整数尺寸（Qt {@code QSize} 值类，纯 Java 实现）。
 * <p>完整覆盖 Qt 6 QSize API（含 AspectRatioMode 与运算符语义）。
 */
public class QSize {

    /** 宽高比模式（Qt AspectRatioMode）。 */
    public enum AspectRatioMode {
        /** 忽略比例，直接使用给定尺寸。 */
        IgnoreAspectRatio(0),
        /** 保持比例，可能小于给定尺寸。 */
        KeepAspectRatio(1),
        /** 保持比例，可能大于给定尺寸。 */
        KeepAspectRatioByExpanding(2);
        final int value;
        AspectRatioMode(int v) { value = v; }
    }

    private int width;
    private int height;

    public QSize() { this(0, 0); }
    public QSize(int width, int height) { this.width = width; this.height = height; }

    public int width() { return width; }
    public int height() { return height; }
    public void setWidth(int width) { this.width = width; }
    public void setHeight(int height) { this.height = height; }
    public int rwidth() { return width; }
    public int rheight() { return height; }

    /** 宽或高为 0（Qt 语义：width==0 || height==0）。 */
    public boolean isEmpty() { return width == 0 || height == 0; }
    /** 宽高都为 0。 */
    public boolean isNull() { return width == 0 && height == 0; }
    /** 宽高都为正。 */
    public boolean isValid() { return width > 0 && height > 0; }

    /** 各分量取两尺寸中较大者。 */
    public QSize expandedTo(QSize other) {
        return new QSize(Math.max(width, other.width), Math.max(height, other.height));
    }

    /** 各分量取两尺寸中较小者。 */
    public QSize boundedTo(QSize other) {
        return new QSize(Math.min(width, other.width), Math.min(height, other.height));
    }

    /** 各边加上边距后放大。 */
    public QSize grownBy(QMargins m) {
        return new QSize(width + m.left() + m.right(), height + m.top() + m.bottom());
    }

    /** 各边减去边距后缩小。 */
    public QSize shrunkBy(QMargins m) {
        return new QSize(width - m.left() - m.right(), height - m.top() - m.bottom());
    }

    /** 交换宽高（就地修改）。 */
    public void transpose() { int t = width; width = height; height = t; }

    /** 交换宽高的新尺寸。 */
    public QSize transposed() { return new QSize(height, width); }

    /** 缩放为给定尺寸（就地修改，按模式保持比例）。 */
    public void scale(int w, int h, AspectRatioMode mode) {
        QSize s = scaled(w, h, mode);
        width = s.width; height = s.height;
    }

    /** 按模式缩放到给定尺寸的新尺寸。 */
    public QSize scaled(int w, int h, AspectRatioMode mode) {
        if (mode == AspectRatioMode.IgnoreAspectRatio || width == 0 || height == 0) {
            return new QSize(w, h);
        }
        double r = Math.min((double) w / width, (double) h / height);
        if (mode == AspectRatioMode.KeepAspectRatioByExpanding) {
            r = Math.max((double) w / width, (double) h / height);
        }
        return new QSize((int) Math.round(width * r), (int) Math.round(height * r));
    }

    /** 转双精度尺寸。 */
    public QSizeF toSizeF() { return new QSizeF(width, height); }

    // ---- 运算符语义 ----
    public QSize plus(QSize s) { return new QSize(width + s.width, height + s.height); }
    public QSize minus(QSize s) { return new QSize(width - s.width, height - s.height); }
    public QSize multiply(int factor) { return new QSize(width * factor, height * factor); }
    public QSize divide(int divisor) { return new QSize(width / divisor, height / divisor); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QSize)) return false;
        QSize s = (QSize) o;
        return width == s.width && height == s.height;
    }

    @Override
    public int hashCode() { return 31 * width + height; }

    @Override
    public String toString() { return "QSize(" + width + "x" + height + ")"; }
}
