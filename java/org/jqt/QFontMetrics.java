/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;

/**
 * 字体度量（Qt {@code QFontMetrics}，纯 Java 实现，委托 {@link java.awt.FontMetrics}）。
 */
public class QFontMetrics {

    private final FontMetrics fm;

    public QFontMetrics(QFont font) {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        Font awt = font != null ? font.toAwt() : new Font("Dialog", Font.PLAIN, 12);
        this.fm = g.getFontMetrics(awt);
        g.dispose();
    }

    public int ascent() { return fm.getAscent(); }
    public int descent() { return fm.getDescent(); }
    public int height() { return fm.getHeight(); }
    public int leading() { return fm.getLeading(); }
    public int lineSpacing() { return fm.getHeight(); }

    /** 文本宽度。 */
    public int horizontalAdvance(String text) { return fm.stringWidth(text != null ? text : ""); }
    public int width(String text) { return horizontalAdvance(text); }

    /** 平均字符宽度。 */
    public int averageCharWidth() { return fm.charWidth('M'); }

    /** 是否可显示全部字符。 */
    public boolean inFont(String text) {
        if (text == null) return true;
        return fm.getFont().canDisplayUpTo(text) == -1;
    }

    public QFont font() { return new QFont(fm.getFont().getFamily(), fm.getFont().getSize()); }

    @Override
    public String toString() { return "QFontMetrics(" + fm.getFont().getFamily() + ")"; }
}
