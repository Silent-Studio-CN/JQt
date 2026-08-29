/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 列表格式（Qt {@code QTextListFormat}，纯 Java 值类）。
 */
public class QTextListFormat extends QTextFormat {

    /** 列表样式（Qt Style 常用）。 */
    public enum Style {
        ListDisc(1), ListCircle(2), ListSquare(3), ListDecimal(4),
        ListLowerAlpha(5), ListUpperAlpha(6), ListLowerRoman(7), ListUpperRoman(8);
        public final int value; Style(int v) { value = v; }
    }

    public QTextListFormat() { super(3); }

    public void setStyle(Style style) { setProperty(ListStyle, style != null ? style.value : 1); }
    public Style style() {
        Object v = property(ListStyle);
        int s = v instanceof Number ? ((Number) v).intValue() : 1;
        for (Style st : Style.values()) if (st.value == s) return st;
        return Style.ListDisc;
    }

    public void setIndent(int indent) { setProperty(ListIndent, indent); }
    public int indent() { Object v = property(ListIndent); return v instanceof Number ? ((Number) v).intValue() : 0; }

    /** 列表项前缀（Qt itemPrefix）。 */
    public String itemPrefix() {
        switch (style()) {
            case ListDecimal: return "1.";
            case ListLowerAlpha: return "a.";
            case ListUpperAlpha: return "A.";
            case ListLowerRoman: return "i.";
            case ListUpperRoman: return "I.";
            default: return "•";
        }
    }
}
