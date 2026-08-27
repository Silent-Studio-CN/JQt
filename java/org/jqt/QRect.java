/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/** 矩形（Qt QRect 的轻量值类）。 */
public class QRect {

    public int x;
    public int y;
    public int width;
    public int height;

    public QRect() {
        this(0, 0, 0, 0);
    }

    public QRect(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /** 高度。 */
    public int height() { return height; }

    /** 宽度。 */
    public int width() { return width; }

    /** 尺寸。 */
    public QSize size() { return new QSize(width, height); }

    /** 设置 x。 */
    public void setX(int x) { this.x = x; }

    /** 设置 y。 */
    public void setY(int y) { this.y = y; }

    @Override
    public String toString() { return "QRect(" + x + "," + y + " " + width + "x" + height + ")"; }
}
