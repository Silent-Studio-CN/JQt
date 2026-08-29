/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 版本号（Qt {@code QVersionNumber}，纯 Java 实现）。
 */
public class QVersionNumber implements Comparable<QVersionNumber> {

    private final int[] segments;

    public QVersionNumber(int... segments) {
        this.segments = segments != null ? segments.clone() : new int[0];
    }

    public QVersionNumber(List<Integer> seg) {
        this.segments = new int[seg != null ? seg.size() : 0];
        if (seg != null) for (int i = 0; i < seg.size(); i++) this.segments[i] = seg.get(i);
    }

    public static QVersionNumber fromString(String string) {
        if (string == null) return new QVersionNumber();
        List<Integer> seg = new ArrayList<>();
        String[] parts = string.trim().split("\\.");
        for (String p : parts) {
            try { seg.add(Integer.parseInt(p.trim())); } catch (NumberFormatException e) { break; }
        }
        return new QVersionNumber(seg);
    }

    public int segmentAt(int index) {
        return index >= 0 && index < segments.length ? segments[index] : 0;
    }

    public int segmentCount() { return segments.length; }

    public boolean isNull() { return segments.length == 0; }

    public int majorVersion() { return segments.length > 0 ? segments[0] : 0; }
    public int minorVersion() { return segments.length > 1 ? segments[1] : 0; }
    public int patchVersion() { return segments.length > 2 ? segments[2] : 0; }

    public QVersionNumber normalized() {
        int n = segments.length;
        while (n > 0 && segments[n - 1] == 0) n--;
        if (n == segments.length) return this;
        return new QVersionNumber(Arrays.copyOf(segments, n));
    }

    public int[] segments() { return segments.clone(); }

    /** 比较：逐段比较，短版本缺段视为 0。 */
    @Override
    public int compareTo(QVersionNumber other) {
        int n = Math.max(segments.length, other.segments.length);
        for (int i = 0; i < n; i++) {
            int a = segmentAt(i), b = other.segmentAt(i);
            if (a != b) return Integer.compare(a, b);
        }
        return 0;
    }

    public boolean isPrefixOf(QVersionNumber other) {
        if (segments.length > other.segments.length) return false;
        for (int i = 0; i < segments.length; i++) {
            if (segments[i] != other.segments[i]) return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QVersionNumber)) return false;
        QVersionNumber q = (QVersionNumber) o;
        return compareTo(q) == 0;
    }

    @Override
    public int hashCode() { return Arrays.hashCode(normalized().segments); }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) sb.append('.');
            sb.append(segments[i]);
        }
        return sb.toString();
    }
}
