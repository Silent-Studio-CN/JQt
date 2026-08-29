/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.awt.MouseInfo;
import java.awt.PointerInfo;

/**
 * 鼠标光标（Qt {@code QCursor}，纯 Java 值类：形状 + 热点）。
 */
public class QCursor {

    /** 光标形状（Qt Shape 常用）。 */
    public enum Shape {
        ArrowCursor(0), UpArrowCursor(1), CrossCursor(2), WaitCursor(3), IBeamCursor(4),
        SizeVerCursor(5), SizeHorCursor(6), SizeBDiagCursor(7), SizeFDiagCursor(8),
        SizeAllCursor(9), BlankCursor(10), SplitVCursor(11), SplitHCursor(12),
        PointingHandCursor(13), ForbiddenCursor(14), WhatsThisCursor(15), BusyCursor(16),
        OpenHandCursor(17), ClosedHandCursor(18), DragCopyCursor(19), DragMoveCursor(20),
        DragLinkCursor(21), BitmapCursor(24);
        public final int value;
        Shape(int v) { value = v; }
    }

    private final Shape shape;
    private final QPixmap pixmap;   // BitmapCursor 时的图像
    private final QPoint hotSpot;

    public QCursor() { this(Shape.ArrowCursor); }

    public QCursor(Shape shape) {
        this.shape = shape != null ? shape : Shape.ArrowCursor;
        this.pixmap = null;
        this.hotSpot = new QPoint();
    }

    public QCursor(QPixmap pixmap, int hotX, int hotY) {
        this.shape = Shape.BitmapCursor;
        this.pixmap = pixmap;
        this.hotSpot = new QPoint(hotX, hotY);
    }

    public Shape shape() { return shape; }
    public QPixmap pixmap() { return pixmap; }
    public QPoint hotSpot() { return hotSpot; }

    /** 全局鼠标位置（Qt QCursor::pos）。 */
    public static QPoint pos() {
        try {
            PointerInfo pi = MouseInfo.getPointerInfo();
            if (pi != null) return new QPoint(pi.getLocation().x, pi.getLocation().y);
        } catch (Exception e) { /* headless */ }
        return new QPoint();
    }

    public static void setPos(int x, int y) {
        try {
            java.awt.Robot robot = new java.awt.Robot();
            robot.mouseMove(x, y);
        } catch (Exception e) { /* ignored */ }
    }
    public static void setPos(QPoint p) { setPos(p.x(), p.y()); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QCursor)) return false;
        QCursor c = (QCursor) o;
        return shape == c.shape && hotSpot.equals(c.hotSpot);
    }

    @Override
    public int hashCode() { return 31 * shape.hashCode() + hotSpot.hashCode(); }

    @Override
    public String toString() { return "QCursor(" + shape + ")"; }
}
