/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 模型索引（Qt {@code QModelIndex}，纯 Java 实现）。
 * <p>描述 model/view 中的单元格位置（row/column + parent 链）。
 */
public class QModelIndex {

    private final int row;
    private final int column;
    private final Object internalPtr;   // parent 或 null（顶层）
    private final long modelId;

    public QModelIndex() { this(-1, -1, null, 0); }
    public QModelIndex(int row, int column) { this(row, column, null, 0); }
    public QModelIndex(int row, int column, Object parent) { this(row, column, parent, 0); }
    public QModelIndex(int row, int column, Object parent, long modelId) {
        this.row = row; this.column = column; this.internalPtr = parent; this.modelId = modelId;
    }

    public int row() { return row; }
    public int column() { return column; }
    public Object parent() { return internalPtr; }
    public long modelId() { return modelId; }

    public boolean isValid() { return row >= 0 && column >= 0; }

    /** 子索引。 */
    public QModelIndex child(int row, int column) {
        return new QModelIndex(row, column, this, modelId);
    }

    /** 父索引（由 parent() 构造）。 */
    public QModelIndex parentIndex() {
        if (internalPtr instanceof QModelIndex) return (QModelIndex) internalPtr;
        return new QModelIndex();
    }

    /** 同模型同位置的兄弟索引。 */
    public QModelIndex sibling(int row, int column) {
        return new QModelIndex(row, column, internalPtr, modelId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QModelIndex)) return false;
        QModelIndex q = (QModelIndex) o;
        return row == q.row && column == q.column && modelId == q.modelId
            && (internalPtr != null ? internalPtr.equals(q.internalPtr) : q.internalPtr == null);
    }

    @Override
    public int hashCode() {
        int h = row; h = 31 * h + column; h = 31 * h + (int) (modelId ^ (modelId >>> 32));
        h = 31 * h + (internalPtr != null ? internalPtr.hashCode() : 0);
        return h;
    }

    @Override
    public String toString() { return "QModelIndex(" + row + "," + column + ")"; }
}
