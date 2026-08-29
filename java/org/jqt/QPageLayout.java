/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 页面布局（Qt {@code QPageLayout}，纯 Java 值类：页面尺寸 + 边距 + 方向）。
 */
public class QPageLayout {

    public enum Orientation { Portrait(0), Landscape(1);
        public final int value; Orientation(int v) { value = v; } }
    public enum Unit { Millimeter(0), Point(1), Inch(2), Pica(3);
        public final int value; Unit(int v) { value = v; } }
    /** 边距模式。 */
    public enum Mode { StandardMode(0), FullPageMode(1);
        public final int value; Mode(int v) { value = v; } }

    private QPageSize pageSize;
    private Orientation orientation;
    private double left, top, right, bottom;   // 点
    private Mode mode;

    public QPageLayout() {
        this(new QPageSize(), Orientation.Portrait, new QMarginsF(56.7, 56.7, 56.7, 56.7));  // 2cm
    }

    public QPageLayout(QPageSize pageSize, Orientation orientation, QMarginsF margins) {
        this.pageSize = pageSize != null ? pageSize : new QPageSize();
        this.orientation = orientation != null ? orientation : Orientation.Portrait;
        QMarginsF m = margins != null ? margins : new QMarginsF();
        this.left = m.left(); this.top = m.top(); this.right = m.right(); this.bottom = m.bottom();
        this.mode = Mode.StandardMode;
    }

    public QPageSize pageSize() { return pageSize; }
    public void setPageSize(QPageSize size) { this.pageSize = size != null ? size : new QPageSize(); }

    public Orientation orientation() { return orientation; }
    public void setOrientation(Orientation o) { this.orientation = o != null ? o : Orientation.Portrait; }

    /** 边距（点）。 */
    public QMarginsF margins() { return new QMarginsF(left, top, right, bottom); }
    public void setMargins(QMarginsF m) {
        if (m != null) { this.left = m.left(); this.top = m.top(); this.right = m.right(); this.bottom = m.bottom(); }
    }

    public QMarginsF margins(Unit unit) {
        double f = 1;
        if (unit == Unit.Millimeter) f = 25.4 / 72.0;
        else if (unit == Unit.Inch) f = 1.0 / 72.0;
        else if (unit == Unit.Pica) f = 1.0 / 12.0;
        return new QMarginsF(left * f, top * f, right * f, bottom * f);
    }

    /** 可打印区域（页面减边距）。 */
    public QRectF paintRectPoints() {
        QSizeF s = pageSize.sizePoints();
        return new QRectF(left, top, s.width() - left - right, s.height() - top - bottom);
    }

    /** 完整页面区域。 */
    public QRectF fullRectPoints() {
        QSizeF s = pageSize.sizePoints();
        return new QRectF(0, 0, s.width(), s.height());
    }

    public Mode mode() { return mode; }
    public void setMode(Mode m) { this.mode = m != null ? m : Mode.StandardMode; }

    public boolean isValid() { return pageSize.isValid(); }

    @Override
    public String toString() { return "QPageLayout(" + pageSize + "," + orientation + ")"; }
}
