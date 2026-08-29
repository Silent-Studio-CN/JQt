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
 * 页面范围（Qt {@code QPageRanges}，纯 Java 值类：区间列表）。
 */
public class QPageRanges {

    private final List<int[]> ranges = new ArrayList<>();   // {from, to} 含端点

    public QPageRanges() { }

    public void addRange(int from, int to) {
        if (from <= to) ranges.add(new int[]{Math.max(1, from), Math.max(1, to)});
    }

    /** 单页。 */
    public void addPage(int page) { addRange(page, page); }

    public boolean isEmpty() { return ranges.isEmpty(); }

    public boolean contains(int page) {
        for (int[] r : ranges) if (page >= r[0] && page <= r[1]) return true;
        return false;
    }

    public int rangeCount() { return ranges.size(); }

    /** 全部页（展开）。 */
    public List<Integer> pages() {
        List<Integer> out = new ArrayList<>();
        for (int[] r : ranges) for (int p = r[0]; p <= r[1]; p++) out.add(p);
        return out;
    }

    /** 解析 "1-3,5,7-9" 格式。 */
    public static QPageRanges fromString(String s) {
        QPageRanges r = new QPageRanges();
        if (s == null) return r;
        for (String part : s.split(",")) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            int dash = p.indexOf('-');
            if (dash > 0) {
                try { r.addRange(Integer.parseInt(p.substring(0, dash).trim()), Integer.parseInt(p.substring(dash + 1).trim())); }
                catch (NumberFormatException e) { }
            } else {
                try { r.addPage(Integer.parseInt(p)); } catch (NumberFormatException e) { }
            }
        }
        return r;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ranges.size(); i++) {
            if (i > 0) sb.append(',');
            int[] r = ranges.get(i);
            sb.append(r[0]).append(r[0] == r[1] ? "" : "-" + r[1]);
        }
        return sb.toString();
    }
}
