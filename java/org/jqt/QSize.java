/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/** 尺寸（Qt QSize 的轻量值类）。 */
public class QSize {

    public int width;
    public int height;

    /** 宽度（与字段 width 等价，Qt 风格方法）。 */
    public int width() { return width; }

    /** 高度（与字段 height 等价，Qt 风格方法）。 */
    public int height() { return height; }

    public QSize() {
        this(0, 0);
    }

    public QSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /** 按比例缩放（keepRatio=true 保持宽高比）。 */
    public QSize scaled(int w, int h, boolean keepRatio) {
        if (keepRatio && width > 0 && height > 0) {
            double r = Math.min((double) w / width, (double) h / height);
            return new QSize((int) Math.round(width * r), (int) Math.round(height * r));
        }
        return new QSize(w, h);
    }

    @Override
    public String toString() { return "QSize(" + width + "x" + height + ")"; }
}
