/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 2D 仿射变换（Qt {@code QTransform}，纯 Java 值类）。
 * <p>矩阵（row-major）：[m11 m12 m13; m21 m22 m23; m31 m32 m33]，m13/m23 为平移。
 */
public class QTransform {

    private double m11, m12, m13;
    private double m21, m22, m23;
    private double m31, m32, m33;

    public QTransform() { this(1, 0, 0, 0, 1, 0, 0, 0, 1); }

    public QTransform(double m11, double m12, double m13,
                      double m21, double m22, double m23,
                      double m31, double m32, double m33) {
        this.m11 = m11; this.m12 = m12; this.m13 = m13;
        this.m21 = m21; this.m22 = m22; this.m23 = m23;
        this.m31 = m31; this.m32 = m32; this.m33 = m33;
    }

    public static QTransform fromTranslate(double tx, double ty) {
        return new QTransform(1, 0, tx, 0, 1, ty, 0, 0, 1);
    }
    public static QTransform fromScale(double sx, double sy) {
        return new QTransform(sx, 0, 0, 0, sy, 0, 0, 0, 1);
    }
    public static QTransform fromRotate(double angle) {
        double rad = Math.toRadians(angle);
        double c = Math.cos(rad), s = Math.sin(rad);
        return new QTransform(c, s, 0, -s, c, 0, 0, 0, 1);
    }

    public boolean isIdentity() {
        return m11 == 1 && m12 == 0 && m13 == 0 && m21 == 0 && m22 == 1 && m23 == 0 && m31 == 0 && m32 == 0 && m33 == 1;
    }
    public boolean isInvertible() { return determinant() != 0; }

    public double m11() { return m11; } public double m12() { return m12; } public double m13() { return m13; }
    public double m21() { return m21; } public double m22() { return m22; } public double m23() { return m23; }
    public double m31() { return m31; } public double m32() { return m32; } public double m33() { return m33; }

    public double determinant() {
        return m11 * (m22 * m33 - m23 * m32)
             - m12 * (m21 * m33 - m23 * m31)
             + m13 * (m21 * m32 - m22 * m31);
    }

    public double dx() { return m13; }
    public double dy() { return m23; }

    /** 复合：this × other（先 other 后 this）。 */
    public QTransform multiply(QTransform o) {
        return new QTransform(
            m11 * o.m11 + m12 * o.m21 + m13 * o.m31,
            m11 * o.m12 + m12 * o.m22 + m13 * o.m32,
            m11 * o.m13 + m12 * o.m23 + m13 * o.m33,
            m21 * o.m11 + m22 * o.m21 + m23 * o.m31,
            m21 * o.m12 + m22 * o.m22 + m23 * o.m32,
            m21 * o.m13 + m22 * o.m23 + m23 * o.m33,
            m31 * o.m11 + m32 * o.m21 + m33 * o.m31,
            m31 * o.m12 + m32 * o.m22 + m33 * o.m32,
            m31 * o.m13 + m32 * o.m23 + m33 * o.m33);
    }

    public QTransform translate(double dx, double dy) { return multiply(fromTranslate(dx, dy)); }
    public QTransform scale(double sx, double sy) { return multiply(fromScale(sx, sy)); }
    public QTransform rotate(double angle) { return multiply(fromRotate(angle)); }

    public QTransform inverted(boolean[] invertible) {
        double det = determinant();
        if (det == 0) { if (invertible != null) invertible[0] = false; return new QTransform(); }
        double id = 1.0 / det;
        QTransform r = new QTransform(
            (m22 * m33 - m23 * m32) * id,
            (m13 * m32 - m12 * m33) * id,
            (m12 * m23 - m13 * m22) * id,
            (m23 * m31 - m21 * m33) * id,
            (m11 * m33 - m13 * m31) * id,
            (m13 * m21 - m11 * m23) * id,
            (m21 * m32 - m22 * m31) * id,
            (m12 * m31 - m11 * m32) * id,
            (m11 * m22 - m12 * m21) * id);
        if (invertible != null) invertible[0] = true;
        return r;
    }
    public QTransform inverted() { return inverted(null); }

    /** 点变换（齐次）。 */
    public QPointF map(QPointF p) {
        double x = p.x(), y = p.y();
        double w = m31 * x + m32 * y + m33;
        if (w == 0 || w == 1) {
            return new QPointF(m11 * x + m12 * y + m13, m21 * x + m22 * y + m23);
        }
        return new QPointF((m11 * x + m12 * y + m13) / w, (m21 * x + m22 * y + m23) / w);
    }
    public QPoint map(QPoint p) {
        QPointF f = map(new QPointF(p.x(), p.y()));
        return new QPoint((int) Math.round(f.x()), (int) Math.round(f.y()));
    }

    /** 矩形变换（角点映射后取外接）。 */
    public QRectF mapRect(QRectF rect) {
        QPointF tl = map(rect.topLeft());
        QPointF tr = map(rect.topRight());
        QPointF bl = map(rect.bottomLeft());
        QPointF br = map(rect.bottomRight());
        double minX = Math.min(Math.min(tl.x(), tr.x()), Math.min(bl.x(), br.x()));
        double minY = Math.min(Math.min(tl.y(), tr.y()), Math.min(bl.y(), br.y()));
        double maxX = Math.max(Math.max(tl.x(), tr.x()), Math.max(bl.x(), br.x()));
        double maxY = Math.max(Math.max(tl.y(), tr.y()), Math.max(bl.y(), br.y()));
        return new QRectF(minX, minY, maxX - minX, maxY - minY);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QTransform)) return false;
        QTransform t = (QTransform) o;
        return m11 == t.m11 && m12 == t.m12 && m13 == t.m13
            && m21 == t.m21 && m22 == t.m22 && m23 == t.m23
            && m31 == t.m31 && m32 == t.m32 && m33 == t.m33;
    }

    @Override
    public int hashCode() {
        long a = Double.doubleToLongBits(m11), b = Double.doubleToLongBits(m22), c = Double.doubleToLongBits(m13);
        int h = (int) a; h = 31 * h + (int) b; h = 31 * h + (int) c;
        return h;
    }

    @Override
    public String toString() {
        return "QTransform(" + m11 + "," + m12 + "," + m13 + " " + m21 + "," + m22 + "," + m23 + ")";
    }
}
