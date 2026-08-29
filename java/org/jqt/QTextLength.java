/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 文本长度（Qt {@code QTextLength}，纯 Java 值类：可变/固定/百分比）。
 */
public class QTextLength {

    /** 类型（Qt Type）。 */
    public enum Type { VariableLength(0), FixedLength(1), PercentageLength(2);
        public final int value; Type(int v) { value = v; } }

    private final Type type;
    private final double value;

    public QTextLength() { this(Type.VariableLength, 0); }
    public QTextLength(Type type, double value) {
        this.type = type != null ? type : Type.VariableLength;
        this.value = value;
    }

    public static QTextLength fixed(double length) { return new QTextLength(Type.FixedLength, length); }
    public static QTextLength percentage(double length) { return new QTextLength(Type.PercentageLength, length); }

    public Type type() { return type; }
    public double value() { return value; }

    /** 实际长度（Qt rawValue：固定=值，百分比=值/100）。 */
    public double rawValue() {
        return type == Type.PercentageLength ? value / 100.0 : value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QTextLength)) return false;
        QTextLength l = (QTextLength) o;
        return type == l.type && Double.compare(value, l.value) == 0;
    }

    @Override
    public int hashCode() { return 31 * type.hashCode() + (int) value; }

    @Override
    public String toString() { return "QTextLength(" + type + "," + value + ")"; }
}
