/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 画刷（Qt {@code QBrush}，纯 Java 值类：样式 + 颜色）。
 * <p>与 QPainter 集成时以 {@link #color()} 的 0xAARRGGBB 传递。
 */
public class QBrush {

    /** 画刷样式（Qt BrushStyle 常用）。 */
    public enum Style {
        NoBrush(0), SolidPattern(1), Dense1Pattern(2), Dense2Pattern(3), Dense3Pattern(4),
        Dense4Pattern(5), Dense5Pattern(6), Dense6Pattern(7), Dense7Pattern(8),
        HorPattern(9), VerPattern(10), CrossPattern(11), BDiagPattern(12),
        FDiagPattern(13), DiagCrossPattern(14);
        public final int value;
        Style(int v) { value = v; }
    }

    private Style style;
    private QColor color;

    public QBrush() { this(QColor.Black, Style.SolidPattern); }

    public QBrush(QColor color) { this(color, Style.SolidPattern); }

    public QBrush(QColor color, Style style) {
        this.color = color != null ? color : QColor.Black;
        this.style = style != null ? style : Style.NoBrush;
    }

    public QBrush(Style style) { this(QColor.Black, style); }

    public Style style() { return style; }
    public void setStyle(Style style) { this.style = style != null ? style : Style.NoBrush; }

    public QColor color() { return color; }
    public void setColor(QColor color) { this.color = color != null ? color : QColor.Black; }

    public boolean isOpaque() { return style != Style.NoBrush && color.alpha() == 255; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QBrush)) return false;
        QBrush b = (QBrush) o;
        return style == b.style && color.equals(b.color);
    }

    @Override
    public int hashCode() { return 31 * style.hashCode() + color.hashCode(); }

    @Override
    public String toString() { return "QBrush(" + style + "," + color + ")"; }
}
