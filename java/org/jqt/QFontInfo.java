/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 字体信息（Qt {@code QFontInfo}，纯 Java 实现：QFont 快照属性）。
 */
public class QFontInfo {

    private final String family;
    private final int pointSize;
    private final boolean bold;
    private final boolean italic;
    private final int weight;

    public QFontInfo(QFont font) {
        this.family = font != null && font.family() != null ? font.family() : "";
        this.pointSize = font != null ? font.pointSize() : -1;
        this.bold = font != null && font.bold();
        this.italic = font != null && font.italic();
        this.weight = font != null ? font.weight() : QFont.Weight.Normal.value;
    }

    public String family() { return family; }
    public int pointSize() { return pointSize; }
    public boolean bold() { return bold; }
    public boolean italic() { return italic; }
    public int weight() { return weight; }
    public boolean fixedPitch() { return false; }   // 简化：无底层字体系统查询

    public QFont toQFont() {
        QFont f = new QFont(family, Math.max(1, pointSize));
        f.setBold(bold);
        f.setItalic(italic);
        f.setWeight(weight);
        return f;
    }

    @Override
    public String toString() { return "QFontInfo(" + family + "," + pointSize + ")"; }
}
