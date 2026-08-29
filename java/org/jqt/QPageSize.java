/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 页面尺寸（Qt {@code QPageSize}，纯 Java 值类：标准纸张 + 自定义点尺寸）。
 */
public class QPageSize {

    /** 标准纸张（Qt PageSizeId 常用）。 */
    public enum PageSizeId {
        A4(5), A3(4), A5(6), Letter(11), Legal(12), A2(3), A1(2), A0(1),
        B4(7), B5(8), Executive(13), Tabloid(14), Custom(15);
        public final int value;
        PageSizeId(int v) { value = v; }
    }

    /** 单位（Qt SizeUnit）。 */
    public enum Unit { Millimeter(0), Point(1), Inch(2), Pica(3), Didot(4), Cicero(5);
        public final int value; Unit(int v) { value = v; } }

    private final PageSizeId id;
    private final double widthPt;    // 点（1/72 英寸）
    private final double heightPt;

    public QPageSize() { this(PageSizeId.A4); }

    public QPageSize(PageSizeId id) {
        this.id = id != null ? id : PageSizeId.A4;
        double[] pt = stdSize(this.id);
        this.widthPt = pt[0];
        this.heightPt = pt[1];
    }

    public QPageSize(double width, double height, Unit unit) {
        this.id = PageSizeId.Custom;
        this.widthPt = toPoints(width, unit);
        this.heightPt = toPoints(height, unit);
    }

    private static double toPoints(double v, Unit unit) {
        switch (unit) {
            case Millimeter: return v * 72.0 / 25.4;
            case Inch: return v * 72.0;
            case Pica: return v * 12.0;
            default: return v;   // Point / Didot / Cicero 简化
        }
    }

    private static double[] stdSize(PageSizeId id) {
        // 点（1/72"）
        switch (id) {
            case A0: return new double[]{2384, 3370};
            case A1: return new double[]{1684, 2384};
            case A2: return new double[]{1191, 1684};
            case A3: return new double[]{842, 1191};
            case A5: return new double[]{420, 595};
            case Letter: return new double[]{612, 792};
            case Legal: return new double[]{612, 1008};
            case B4: return new double[]{709, 1001};
            case B5: return new double[]{499, 709};
            case Executive: return new double[]{522, 756};
            case Tabloid: return new double[]{792, 1224};
            case A4:
            default: return new double[]{595, 842};
        }
    }

    public PageSizeId id() { return id; }
    public boolean isEquivalentTo(PageSizeId other) { return id == other; }

    /** 点尺寸。 */
    public QSizeF sizePoints() { return new QSizeF(widthPt, heightPt); }
    public double widthPoints() { return widthPt; }
    public double heightPoints() { return heightPt; }

    /** 指定单位尺寸。 */
    public QSizeF size(Unit unit) {
        switch (unit) {
            case Millimeter: return new QSizeF(widthPt * 25.4 / 72.0, heightPt * 25.4 / 72.0);
            case Inch: return new QSizeF(widthPt / 72.0, heightPt / 72.0);
            case Pica: return new QSizeF(widthPt / 12.0, heightPt / 12.0);
            default: return sizePoints();
        }
    }

    public static QPageSize A4() { return new QPageSize(PageSizeId.A4); }
    public static QPageSize Letter() { return new QPageSize(PageSizeId.Letter); }

    public boolean isValid() { return widthPt > 0 && heightPt > 0; }

    @Override
    public String toString() { return "QPageSize(" + id + ")"; }
}
