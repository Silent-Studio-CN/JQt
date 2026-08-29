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
 * JSON 数组（Qt {@code QJsonArray}，纯 Java 实现，基于 {@link ArrayList}）。
 */
public class QJsonArray {

    private final List<QJsonValue> list = new ArrayList<>();

    public QJsonArray() { }

    public boolean isEmpty() { return list.isEmpty(); }
    public int size() { return list.size(); }
    public int count() { return list.size(); }

    public QJsonValue at(int i) { return i >= 0 && i < list.size() ? list.get(i) : new QJsonValue(); }
    public QJsonValue first() { return list.isEmpty() ? new QJsonValue() : list.get(0); }
    public QJsonValue last() { return list.isEmpty() ? new QJsonValue() : list.get(list.size() - 1); }

    public void append(QJsonValue v) { list.add(v); }
    public void append(boolean v) { list.add(new QJsonValue(v)); }
    public void append(double v) { list.add(new QJsonValue(v)); }
    public void append(String v) { list.add(new QJsonValue(v)); }
    public void append(QJsonObject v) { list.add(new QJsonValue(v)); }
    public void append(QJsonArray v) { list.add(new QJsonValue(v)); }

    public void insertAt(int i, QJsonValue v) { list.add(Math.max(0, Math.min(i, list.size())), v); }
    public void removeAt(int i) { if (i >= 0 && i < list.size()) list.remove(i); }

    public List<QJsonValue> toList() { return new ArrayList<>(list); }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(list.get(i).toJson());
        }
        return sb.append(']').toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QJsonArray)) return false;
        return list.equals(((QJsonArray) o).list);
    }

    @Override
    public int hashCode() { return list.hashCode(); }
}
