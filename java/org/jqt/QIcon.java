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
 * 图标（Qt {@code QIcon}，纯 Java 值类：多尺寸 QPixmap 集合）。
 */
public class QIcon {

    private final List<QPixmap> pixmaps;
    private final List<Integer> sizes;

    public QIcon() { this.pixmaps = new ArrayList<>(); this.sizes = new ArrayList<>(); }

    public QIcon(QPixmap pixmap) {
        this();
        if (pixmap != null && !pixmap.isNull()) {
            pixmaps.add(pixmap);
            sizes.add(pixmap.width());
        }
    }

    public QIcon(String fileName) {
        this(new QPixmap(fileName));
    }

    public boolean isNull() { return pixmaps.isEmpty(); }

    public void addPixmap(QPixmap pixmap) {
        if (pixmap != null && !pixmap.isNull()) {
            pixmaps.add(pixmap);
            sizes.add(pixmap.width());
        }
    }

    /** 指定尺寸的像素图（无精确匹配取最接近）。 */
    public QPixmap pixmap(int size) {
        if (pixmaps.isEmpty()) return new QPixmap();
        int best = 0, bestDiff = Integer.MAX_VALUE;
        for (int i = 0; i < sizes.size(); i++) {
            int d = Math.abs(sizes.get(i) - size);
            if (d < bestDiff) { bestDiff = d; best = i; }
        }
        return pixmaps.get(best);
    }

    public List<QSize> availableSizes() {
        List<QSize> out = new ArrayList<>();
        for (int s : sizes) out.add(new QSize(s, s));
        return out;
    }

    public QPixmap pixmap() { return pixmaps.isEmpty() ? new QPixmap() : pixmaps.get(0); }

    public void swap(QIcon other) {
        java.util.Collections.swap(pixmaps, 0, 0);
    }

    @Override
    public String toString() { return "QIcon(" + sizes.size() + " pixmaps)"; }
}
