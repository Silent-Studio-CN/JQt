/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 字符格式（Qt {@code QTextCharFormat}，纯 Java 值类）。
 */
public class QTextCharFormat extends QTextFormat {

    public QTextCharFormat() { super(1); }

    public void setFontFamily(String family) { setProperty(FontFamily, family); }
    public String fontFamily() { Object v = property(FontFamily); return v != null ? (String) v : ""; }

    public void setFontPointSize(double size) { setProperty(FontPointSize, size); }
    public double fontPointSize() { Object v = property(FontPointSize); return v instanceof Number ? ((Number) v).doubleValue() : 0; }

    public void setFontWeight(int weight) { setProperty(FontWeight, weight); }
    public int fontWeight() { Object v = property(FontWeight); return v instanceof Number ? ((Number) v).intValue() : QFont.Weight.Normal.value; }

    public void setFontBold(boolean bold) { setProperty(FontWeight, bold ? QFont.Weight.Bold.value : QFont.Weight.Normal.value); }
    public boolean fontBold() { return fontWeight() >= QFont.Weight.Bold.value; }

    public void setFontItalic(boolean italic) { setProperty(FontItalic, italic); }
    public boolean fontItalic() { return Boolean.TRUE.equals(property(FontItalic)); }

    public void setFontUnderline(boolean underline) { setProperty(FontUnderline, underline); }
    public boolean fontUnderline() { return Boolean.TRUE.equals(property(FontUnderline)); }

    public void setForeground(QBrush brush) { setProperty(ForegroundBrush, brush); }
    public QBrush foreground() { Object v = property(ForegroundBrush); return v instanceof QBrush ? (QBrush) v : new QBrush(); }

    public void setBackground(QBrush brush) { setProperty(BackgroundBrush, brush); }
    public QBrush background() { Object v = property(BackgroundBrush); return v instanceof QBrush ? (QBrush) v : new QBrush(); }

    public void setAnchorHref(String href) { setProperty(AnchorHref, href); }
    public String anchorHref() { Object v = property(AnchorHref); return v != null ? (String) v : ""; }

    public boolean isAnchor() { return hasProperty(AnchorHref); }
}
