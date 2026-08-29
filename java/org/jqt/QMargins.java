/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 四边距（Qt {@code QMargins} 值类，纯 Java 实现）。
 * <p>完整覆盖 Qt 6 QMargins API（含运算符语义）。
 */
public class QMargins {

    private int left;
    private int top;
    private int right;
    private int bottom;

    public QMargins() { this(0, 0, 0, 0); }

    public QMargins(int left, int top, int right, int bottom) {
        this.left = left; this.top = top; this.right = right; this.bottom = bottom;
    }

    public int left() { return left; }
    public int top() { return top; }
    public int right() { return right; }
    public int bottom() { return bottom; }
    public void setLeft(int left) { this.left = left; }
    public void setTop(int top) { this.top = top; }
    public void setRight(int right) { this.right = right; }
    public void setBottom(int bottom) { this.bottom = bottom; }

    /** 四边都为 0。 */
    public boolean isNull() { return left == 0 && top == 0 && right == 0 && bottom == 0; }

    /** 转双精度边距。 */
    public QMarginsF toMarginsF() { return new QMarginsF(left, top, right, bottom); }

    public QMargins plus(QMargins m) {
        return new QMargins(left + m.left, top + m.top, right + m.right, bottom + m.bottom);
    }
    public QMargins minus(QMargins m) {
        return new QMargins(left - m.left, top - m.top, right - m.right, bottom - m.bottom);
    }
    public QMargins multiply(int factor) {
        return new QMargins(left * factor, top * factor, right * factor, bottom * factor);
    }
    public QMargins divide(int divisor) {
        return new QMargins(left / divisor, top / divisor, right / divisor, bottom / divisor);
    }
    public QMargins negate() {
        return new QMargins(-left, -top, -right, -bottom);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QMargins)) return false;
        QMargins m = (QMargins) o;
        return left == m.left && top == m.top && right == m.right && bottom == m.bottom;
    }

    @Override
    public int hashCode() {
        int r = left; r = 31 * r + top; r = 31 * r + right; r = 31 * r + bottom;
        return r;
    }

    @Override
    public String toString() {
        return "QMargins(" + left + "," + top + "," + right + "," + bottom + ")";
    }
}
