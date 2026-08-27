/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/** 二维点（Qt QPoint 的轻量值类）。 */
public class QPoint {

    public int x;
    public int y;

    public QPoint() {
        this(0, 0);
    }

    public QPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /** 设置 x。 */
    public void setX(int x) { this.x = x; }

    /** 设置 y。 */
    public void setY(int y) { this.y = y; }

    @Override
    public String toString() { return "QPoint(" + x + "," + y + ")"; }
}
