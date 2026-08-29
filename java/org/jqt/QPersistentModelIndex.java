/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 持久模型索引（Qt {@code QPersistentModelIndex}，纯 Java 实现）。
 * <p>与 {@link QModelIndex} 相同语义；Qt 中用于在数据变化后保持有效。
 */
public class QPersistentModelIndex {

    private final QModelIndex index;

    public QPersistentModelIndex() { this.index = new QModelIndex(); }
    public QPersistentModelIndex(QModelIndex index) { this.index = index != null ? index : new QModelIndex(); }

    public QModelIndex modelIndex() { return index; }
    public int row() { return index.row(); }
    public int column() { return index.column(); }
    public Object parent() { return index.parent(); }
    public boolean isValid() { return index.isValid(); }

    public QModelIndex toModelIndex() { return index; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QPersistentModelIndex)) return false;
        return index.equals(((QPersistentModelIndex) o).index);
    }

    @Override
    public int hashCode() { return index.hashCode(); }

    @Override
    public String toString() { return "QPersistentModelIndex(" + index + ")"; }
}
