/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 二维整数线段（Qt {@code QLine} 值类，纯 Java 实现）。
 * <p>完整覆盖 Qt 6 QLine API（含运算符语义）。
 */
public class QLine {

    private int x1;
    private int y1;
    private int x2;
    private int y2;

    public QLine() { this(0, 0, 0, 0); }

    public QLine(int x1, int y1, int x2, int y2) {
        this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
    }

    public QLine(QPoint p1, QPoint p2) {
        this(p1.x(), p1.y(), p2.x(), p2.y());
    }

    public QPoint p1() { return new QPoint(x1, y1); }
    public QPoint p2() { return new QPoint(x2, y2); }
    public int x1() { return x1; }
    public int x2() { return x2; }
    public int y1() { return y1; }
    public int y2() { return y2; }
    public int dx() { return x2 - x1; }
    public int dy() { return y2 - y1; }

    /** 是否退化为点（p1==p2）。 */
    public boolean isNull() { return x1 == x2 && y1 == y2; }

    /** 中点。 */
    public QPoint center() { return new QPoint((x1 + x2) / 2, (y1 + y2) / 2); }

    public void setLine(int x1, int y1, int x2, int y2) {
        this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
    }

    public void setPoints(QPoint p1, QPoint p2) {
        this.x1 = p1.x(); this.y1 = p1.y(); this.x2 = p2.x(); this.y2 = p2.y();
    }

    public void setP1(QPoint p1) { this.x1 = p1.x(); this.y1 = p1.y(); }
    public void setP2(QPoint p2) { this.x2 = p2.x(); this.y2 = p2.y(); }

    /** 平移（就地修改）。 */
    public void translate(QPoint offset) { translate(offset.x(), offset.y()); }
    public void translate(int dx, int dy) { x1 += dx; y1 += dy; x2 += dx; y2 += dy; }

    /** 平移后的新线段。 */
    public QLine translated(QPoint offset) { return translated(offset.x(), offset.y()); }
    public QLine translated(int dx, int dy) {
        return new QLine(x1 + dx, y1 + dy, x2 + dx, y2 + dy);
    }

    /** 转双精度线段。 */
    public QLineF toLineF() { return new QLineF(x1, y1, x2, y2); }

    public QLine plus(QPoint p) { return translated(p); }
    public QLine minus(QPoint p) { return translated(-p.x(), -p.y()); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QLine)) return false;
        QLine l = (QLine) o;
        return x1 == l.x1 && y1 == l.y1 && x2 == l.x2 && y2 == l.y2;
    }

    @Override
    public int hashCode() {
        int r = x1; r = 31 * r + y1; r = 31 * r + x2; r = 31 * r + y2;
        return r;
    }

    @Override
    public String toString() { return "QLine(" + x1 + "," + y1 + "," + x2 + "," + y2 + ")"; }
}
