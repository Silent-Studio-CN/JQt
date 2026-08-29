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
 * 文本格式（Qt {@code QTextFormat}，纯 Java 值类：属性表）。
 */
public class QTextFormat {

    /** 对象类型。 */
    public enum ObjectType { NoObject(0), Image(1), Table(2), TableCell(3), UserObject(0x1000);
        public final int value; ObjectType(int v) { value = v; } }

    private final Map<Integer, Object> properties = new LinkedHashMap<>();
    private int objectType = ObjectType.NoObject.value;

    public QTextFormat() { }

    public QTextFormat(int type) { this.objectType = type; }

    public int objectType() { return objectType; }
    public void setObjectType(int type) { this.objectType = type; }

    public boolean isEmpty() { return properties.isEmpty(); }

    public boolean hasProperty(int propertyId) { return properties.containsKey(propertyId); }

    public Object property(int propertyId) { return properties.get(propertyId); }

    public void setProperty(int propertyId, Object value) {
        if (value == null) properties.remove(propertyId);
        else properties.put(propertyId, value);
    }

    public void clearProperty(int propertyId) { properties.remove(propertyId); }

    public Map<Integer, Object> properties() { return new LinkedHashMap<>(properties); }

    public void merge(QTextFormat other) {
        if (other != null) properties.putAll(other.properties);
    }

    /** 常用属性键（Qt TextFormat 属性）。 */
    public static final int FontFamily = 1;
    public static final int FontPointSize = 2;
    public static final int FontWeight = 3;
    public static final int FontItalic = 4;
    public static final int FontUnderline = 5;
    public static final int ForegroundBrush = 6;
    public static final int BackgroundBrush = 7;
    public static final int TextOutline = 8;
    public static final int BlockAlignment = 9;
    public static final int BlockIndent = 10;
    public static final int BlockTopMargin = 11;
    public static final int BlockBottomMargin = 12;
    public static final int BlockLeftMargin = 13;
    public static final int BlockRightMargin = 14;
    public static final int BlockNonBreakableLines = 15;
    public static final int ListStyle = 16;
    public static final int ListIndent = 17;
    public static final int AnchorName = 18;
    public static final int AnchorHref = 19;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QTextFormat)) return false;
        QTextFormat f = (QTextFormat) o;
        return objectType == f.objectType && properties.equals(f.properties);
    }

    @Override
    public int hashCode() { return 31 * objectType + properties.hashCode(); }

    @Override
    public String toString() { return "QTextFormat(type=" + objectType + ", props=" + properties.size() + ")"; }
}
