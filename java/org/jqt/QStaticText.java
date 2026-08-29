/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 静态文本（Qt {@code QStaticText}，纯 Java 值类：文本 + 格式 + 性能提示）。
 * <p>用于高频重绘的静态文本优化（Qt 缓存布局；Java 侧存文本与选项）。
 */
public class QStaticText {

    /** 性能提示（Qt PerformanceHint）。 */
    public enum PerformanceHint { ModerateCaching(0), AggressiveCaching(1);
        public final int value; PerformanceHint(int v) { value = v; } }

    private String text;
    private PerformanceHint hint;
    private QSizeF size;

    public QStaticText() { this(""); }

    public QStaticText(String text) {
        this.text = text != null ? text : "";
        this.hint = PerformanceHint.ModerateCaching;
        this.size = new QSizeF();
    }

    public QStaticText(QStaticText other) {
        this.text = other.text;
        this.hint = other.hint;
        this.size = other.size;
    }

    public void setText(String text) { this.text = text != null ? text : ""; }
    public String text() { return text; }

    public void setPerformanceHint(PerformanceHint hint) { this.hint = hint != null ? hint : PerformanceHint.ModerateCaching; }
    public PerformanceHint performanceHint() { return hint; }

    /** 布局尺寸（Qt prepare/size 语义：由渲染器更新）。 */
    public QSizeF size() { return size; }
    public void setSize(QSizeF s) { this.size = s != null ? s : new QSizeF(); }

    public boolean isEmpty() { return text.isEmpty(); }

    /** 内容变化后调用（Qt prepare）。 */
    public void prepare() { /* 布局缓存由渲染层处理 */ }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QStaticText)) return false;
        QStaticText s = (QStaticText) o;
        return text.equals(s.text) && hint == s.hint;
    }

    @Override
    public int hashCode() { return 31 * text.hashCode() + hint.hashCode(); }

    @Override
    public String toString() { return "QStaticText(" + text.length() + " chars)"; }
}
