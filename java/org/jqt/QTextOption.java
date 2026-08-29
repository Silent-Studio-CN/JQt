/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 文本选项（Qt {@code QTextOption}，纯 Java 值类）。
 */
public class QTextOption {

    public enum WrapMode { NoWrap(0), WordWrap(1), ManualWrap(2), WrapAnywhere(3), WrapAtWordBoundaryOrAnywhere(4);
        public final int value; WrapMode(int v) { value = v; } }
    public enum Alignment { Left(1), Right(2), Center(4), Justify(8);
        public final int value; Alignment(int v) { value = v; } }

    private WrapMode wrapMode;
    private Alignment alignment;
    private boolean useDesignMetrics;

    public QTextOption() {
        this.wrapMode = WrapMode.WordWrap;
        this.alignment = Alignment.Left;
        this.useDesignMetrics = false;
    }

    public QTextOption(Alignment alignment) {
        this();
        this.alignment = alignment;
    }

    public WrapMode wrapMode() { return wrapMode; }
    public void setWrapMode(WrapMode mode) { this.wrapMode = mode != null ? mode : WrapMode.WordWrap; }

    public Alignment alignment() { return alignment; }
    public void setAlignment(Alignment a) { this.alignment = a != null ? a : Alignment.Left; }

    public boolean useDesignMetrics() { return useDesignMetrics; }
    public void setUseDesignMetrics(boolean b) { this.useDesignMetrics = b; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QTextOption)) return false;
        QTextOption q = (QTextOption) o;
        return wrapMode == q.wrapMode && alignment == q.alignment && useDesignMetrics == q.useDesignMetrics;
    }

    @Override
    public int hashCode() { return 31 * wrapMode.hashCode() + alignment.hashCode(); }

    @Override
    public String toString() { return "QTextOption(" + wrapMode + "," + alignment + ")"; }
}
