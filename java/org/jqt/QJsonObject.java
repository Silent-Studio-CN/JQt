/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON 对象（Qt {@code QJsonObject}，纯 Java 实现，基于 {@link LinkedHashMap}）。
 */
public class QJsonObject {

    private final Map<String, QJsonValue> map = new LinkedHashMap<>();

    public QJsonObject() { }

    public boolean isEmpty() { return map.isEmpty(); }
    public int size() { return map.size(); }
    public int count() { return map.size(); }
    public boolean contains(String key) { return map.containsKey(key); }

    public QJsonValue value(String key) {
        QJsonValue v = map.get(key);
        return v != null ? v : new QJsonValue();
    }
    public QJsonValue value(String key, QJsonValue defaultValue) {
        QJsonValue v = map.get(key);
        return v != null ? v : defaultValue;
    }

    public void insert(String key, QJsonValue value) { map.put(key, value); }
    public void insert(String key, boolean value) { map.put(key, new QJsonValue(value)); }
    public void insert(String key, double value) { map.put(key, new QJsonValue(value)); }
    public void insert(String key, String value) { map.put(key, new QJsonValue(value)); }
    public void insert(String key, QJsonArray value) { map.put(key, new QJsonValue(value)); }
    public void insert(String key, QJsonObject value) { map.put(key, new QJsonValue(value)); }

    public void remove(String key) { map.remove(key); }

    public java.util.List<String> keys() { return new java.util.ArrayList<>(map.keySet()); }

    public Map<String, QJsonValue> toMap() { return new LinkedHashMap<>(map); }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, QJsonValue> e : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append(QJsonValue.escape(e.getKey())).append(':').append(e.getValue().toJson());
        }
        return sb.append('}').toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QJsonObject)) return false;
        return map.equals(((QJsonObject) o).map);
    }

    @Override
    public int hashCode() { return map.hashCode(); }
}
