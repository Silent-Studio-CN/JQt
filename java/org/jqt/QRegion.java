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
 * 区域（Qt {@code QRegion}，纯 Java 值类：矩形集合）。
 * <p>简化实现：以矩形列表表示区域，支持 contains/intersects/united/intersected。
 */
public class QRegion {

    public enum RegionType { Rectangle(0), Ellipse(1);
        public final int value; RegionType(int v) { value = v; } }

    private final List<QRect> rects;

    public QRegion() { this.rects = new ArrayList<>(); }

    public QRegion(QRect rect) {
        this.rects = new ArrayList<>();
        if (rect != null && !rect.isEmpty()) this.rects.add(rect);
    }

    public QRegion(int x, int y, int w, int h) { this(new QRect(x, y, w, h)); }

    public QRegion(List<QRect> rects) {
        this.rects = new ArrayList<>();
        if (rects != null) for (QRect r : rects) if (r != null && !r.isEmpty()) this.rects.add(r);
    }

    public boolean isNull() { return rects.isEmpty(); }
    public boolean isEmpty() { return rects.isEmpty(); }

    public QRect boundingRect() {
        if (rects.isEmpty()) return new QRect();
        QRect first = rects.get(0);
        QRect out = first;
        for (int i = 1; i < rects.size(); i++) out = out.united(rects.get(i));
        return out;
    }

    public int rectCount() { return rects.size(); }
    public List<QRect> rects() { return new ArrayList<>(rects); }

    public boolean contains(QPoint p) {
        for (QRect r : rects) if (r.contains(p)) return true;
        return false;
    }
    public boolean contains(int x, int y) { return contains(new QPoint(x, y)); }

    public boolean intersects(QRect rect) {
        for (QRect r : rects) if (r.intersects(rect)) return true;
        return false;
    }

    public QRegion united(QRect rect) {
        QRegion r = new QRegion(rects);
        if (rect != null && !rect.isEmpty()) r.rects.add(rect);
        return r;
    }
    public QRegion united(QRegion other) {
        QRegion r = new QRegion(rects);
        r.rects.addAll(other.rects);
        return r;
    }
    public QRegion plus(QRect rect) { return united(rect); }

    public QRegion intersected(QRect rect) {
        QRegion r = new QRegion();
        for (QRect rr : rects) {
            QRect i = rr.intersected(rect);
            if (!i.isEmpty()) r.rects.add(i);
        }
        return r;
    }

    public QRegion translated(int dx, int dy) {
        QRegion r = new QRegion();
        for (QRect rr : rects) r.rects.add(rr.translated(dx, dy));
        return r;
    }
    public QRegion translated(QPoint p) { return translated(p.x(), p.y()); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QRegion)) return false;
        return rects.equals(((QRegion) o).rects);
    }

    @Override
    public int hashCode() { return rects.hashCode(); }

    @Override
    public String toString() { return "QRegion(" + rects.size() + " rects)"; }
}
