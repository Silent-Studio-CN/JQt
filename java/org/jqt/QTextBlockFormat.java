/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 块格式（Qt {@code QTextBlockFormat}，纯 Java 值类）。
 */
public class QTextBlockFormat extends QTextFormat {

    /** 对齐（Qt AlignmentFlag 常用位）。 */
    public static final int AlignLeft = 1;
    public static final int AlignRight = 2;
    public static final int AlignHCenter = 4;
    public static final int AlignJustify = 8;
    public static final int AlignTop = 32;
    public static final int AlignBottom = 64;
    public static final int AlignVCenter = 128;

    public QTextBlockFormat() { super(2); }

    public void setAlignment(int alignment) { setProperty(BlockAlignment, alignment); }
    public int alignment() { Object v = property(BlockAlignment); return v instanceof Number ? ((Number) v).intValue() : AlignLeft; }

    public void setIndent(int indent) { setProperty(BlockIndent, indent); }
    public int indent() { Object v = property(BlockIndent); return v instanceof Number ? ((Number) v).intValue() : 0; }

    public void setTopMargin(double margin) { setProperty(BlockTopMargin, margin); }
    public double topMargin() { Object v = property(BlockTopMargin); return v instanceof Number ? ((Number) v).doubleValue() : 0; }
    public void setBottomMargin(double margin) { setProperty(BlockBottomMargin, margin); }
    public double bottomMargin() { Object v = property(BlockBottomMargin); return v instanceof Number ? ((Number) v).doubleValue() : 0; }
    public void setLeftMargin(double margin) { setProperty(BlockLeftMargin, margin); }
    public double leftMargin() { Object v = property(BlockLeftMargin); return v instanceof Number ? ((Number) v).doubleValue() : 0; }
    public void setRightMargin(double margin) { setProperty(BlockRightMargin, margin); }
    public double rightMargin() { Object v = property(BlockRightMargin); return v instanceof Number ? ((Number) v).doubleValue() : 0; }

    public void setNonBreakableLines(boolean b) { setProperty(BlockNonBreakableLines, b); }
    public boolean nonBreakableLines() { return Boolean.TRUE.equals(property(BlockNonBreakableLines)); }

    public void setLineHeight(double height, int heightType) { setProperty(32, height); }
}
