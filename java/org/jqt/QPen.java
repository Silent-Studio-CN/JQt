/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 画笔（Qt {@code QPen}，纯 Java 值类：样式/宽度/颜色/端点/连接）。
 */
public class QPen {

    public enum Style { NoPen(0), SolidLine(1), DashLine(2), DotLine(3), DashDotLine(4), DashDotDotLine(5);
        public final int value; Style(int v) { value = v; } }
    public enum CapStyle { FlatCap(0), SquareCap(1), RoundCap(2);
        public final int value; CapStyle(int v) { value = v; } }
    public enum JoinStyle { MiterJoin(0), BevelJoin(1), RoundJoin(2);
        public final int value; JoinStyle(int v) { value = v; } }

    private Style style;
    private double width;
    private QColor color;
    private CapStyle cap;
    private JoinStyle join;

    public QPen() { this(QColor.Black, 1.0, Style.SolidLine); }

    public QPen(QColor color) { this(color, 1.0, Style.SolidLine); }

    public QPen(QColor color, double width) { this(color, width, Style.SolidLine); }

    public QPen(QColor color, double width, Style style) {
        this.color = color != null ? color : QColor.Black;
        this.width = width;
        this.style = style != null ? style : Style.NoPen;
        this.cap = CapStyle.SquareCap;
        this.join = JoinStyle.BevelJoin;
    }

    public Style style() { return style; }
    public void setStyle(Style style) { this.style = style != null ? style : Style.NoPen; }
    public double widthF() { return width; }
    public double width() { return width; }
    public void setWidth(double width) { this.width = width; }
    public void setWidthF(double width) { this.width = width; }
    public QColor color() { return color; }
    public void setColor(QColor color) { this.color = color != null ? color : QColor.Black; }
    public CapStyle capStyle() { return cap; }
    public void setCapStyle(CapStyle cap) { this.cap = cap != null ? cap : CapStyle.SquareCap; }
    public JoinStyle joinStyle() { return join; }
    public void setJoinStyle(JoinStyle join) { this.join = join != null ? join : JoinStyle.BevelJoin; }

    public boolean isSolid() { return style == Style.SolidLine; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QPen)) return false;
        QPen p = (QPen) o;
        return style == p.style && Double.compare(width, p.width) == 0 && color.equals(p.color)
            && cap == p.cap && join == p.join;
    }

    @Override
    public int hashCode() {
        int h = style.hashCode(); h = 31 * h + (int) width; h = 31 * h + color.hashCode();
        return h;
    }

    @Override
    public String toString() { return "QPen(" + color + ", " + width + ", " + style + ")"; }
}
