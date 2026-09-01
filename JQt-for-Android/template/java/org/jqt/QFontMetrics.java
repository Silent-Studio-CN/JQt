/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 字体度量（Qt {@code QFontMetrics}）。
 * Android 构建：AWT 度量桥不可用，方法返回 0（PoC 阶段占位）。
 */
public class QFontMetrics {

    private final QFont font;

    public QFontMetrics(QFont font) { this.font = font != null ? font : new QFont(); }

    public int ascent() { return 0; }
    public int descent() { return 0; }
    public int height() { return 0; }
    public int leading() { return 0; }
    public int lineSpacing() { return 0; }
    public int horizontalAdvance(String text) { return 0; }
    public int width(String text) { return 0; }
    public int averageCharWidth() { return 0; }
    public boolean inFont(String text) { return true; }
    public QFont font() { return font; }

    @Override
    public String toString() { return "QFontMetrics(android-stub)"; }
}
