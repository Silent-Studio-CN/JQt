/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 图片播放记录（Qt {@code QPicture}，纯 Java 值类：边界 + 尺寸元数据）。
 * <p>完整播放引擎随 QPainter 集成推进；当前提供元数据 API。
 */
public class QPicture {

    private QRect boundingRect;
    private int width;
    private int height;

    public QPicture() {
        this.boundingRect = new QRect();
        this.width = 0;
        this.height = 0;
    }

    public QPicture(QRect boundingRect) { this(); setBoundingRect(boundingRect); }

    public boolean isNull() { return width == 0 && height == 0; }

    public QRect boundingRect() { return boundingRect; }

    public void setBoundingRect(QRect r) {
        this.boundingRect = r != null ? r : new QRect();
        this.width = this.boundingRect.width();
        this.height = this.boundingRect.height();
    }

    public int width() { return width; }
    public int height() { return height; }

    public void setRect(QRect r) { setBoundingRect(r); }

    public QSize size() { return new QSize(width, height); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QPicture)) return false;
        QPicture p = (QPicture) o;
        return boundingRect.equals(p.boundingRect);
    }

    @Override
    public int hashCode() { return boundingRect.hashCode(); }

    @Override
    public String toString() { return "QPicture(" + width + "x" + height + ")"; }
}
