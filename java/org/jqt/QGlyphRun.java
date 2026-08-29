/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.ArrayList;
import java.util.List;

/**
 * 字形运行（Qt {@code QGlyphRun}，纯 Java 值类：字形索引 + 位置）。
 */
public class QGlyphRun {

    private final List<Integer> glyphIndexes = new ArrayList<>();
    private final List<QPointF> positions = new ArrayList<>();
    private boolean overline;
    private boolean underline;
    private boolean strikeOut;
    private boolean rightToLeft;

    public QGlyphRun() { }

    public void setGlyphIndexes(List<Integer> indexes) {
        glyphIndexes.clear();
        if (indexes != null) glyphIndexes.addAll(indexes);
    }
    public List<Integer> glyphIndexes() { return new ArrayList<>(glyphIndexes); }

    public void setPositions(List<QPointF> pos) {
        positions.clear();
        if (pos != null) positions.addAll(pos);
    }
    public List<QPointF> positions() { return new ArrayList<>(positions); }

    public void setOverline(boolean b) { overline = b; }
    public boolean overline() { return overline; }
    public void setUnderline(boolean b) { underline = b; }
    public boolean underline() { return underline; }
    public void setStrikeOut(boolean b) { strikeOut = b; }
    public boolean strikeOut() { return strikeOut; }
    public void setRightToLeft(boolean b) { rightToLeft = b; }
    public boolean isRightToLeft() { return rightToLeft; }

    public boolean isEmpty() { return glyphIndexes.isEmpty(); }
    public int size() { return glyphIndexes.size(); }

    public void clear() { glyphIndexes.clear(); positions.clear(); }

    @Override
    public String toString() { return "QGlyphRun(" + glyphIndexes.size() + " glyphs)"; }
}
