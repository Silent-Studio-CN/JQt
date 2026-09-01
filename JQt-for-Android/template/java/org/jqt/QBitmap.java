/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 单色位图（Qt {@code QBitmap}，纯 Java 值类：1-bit 像素矩阵）。
 * <p>宽度按 32 位对齐存储（Qt 语义），提供 fill/setBit/testBit/toQPixmap。
 */
public class QBitmap {

    private int width;
    private int height;
    private int[] lines;   // 每行一个 int（低 width 位有效）

    public QBitmap() { this(0, 0); }

    public QBitmap(int width, int height) {
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
        this.lines = new int[this.height];
    }

    public boolean isNull() { return width == 0 || height == 0; }

    public int width() { return width; }
    public int height() { return height; }
    public QSize size() { return new QSize(width, height); }

    /** 清空（Qt clear）。 */
    public void clear() { java.util.Arrays.fill(lines, 0); }

    /** 全部填充 1。 */
    public void fill(int color) { java.util.Arrays.fill(lines, color != 0 ? -1 : 0); }

    public void setBit(int x, int y, boolean on) {
        if (x < 0 || y < 0 || x >= width || y >= height) return;
        if (on) lines[y] |= (1 << x);
        else lines[y] &= ~(1 << x);
    }

    public boolean testBit(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) return false;
        return (lines[y] & (1 << x)) != 0;
    }

    /** 转 QPixmap（黑=不透明，白=透明）。 */
    public QPixmap toQPixmap() {
        QPixmap pm = new QPixmap(width, height);
        int[] argb = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                argb[y * width + x] = testBit(x, y) ? 0xFF000000 : 0x00FFFFFF;
            }
        }
        return QPixmap.fromArgb(argb, width, height);
    }

    public void swap(QBitmap other) {
        int t = width; width = other.width; other.width = t;
        t = height; height = other.height; other.height = t;
        int[] tl = lines; lines = other.lines; other.lines = tl;
    }

    @Override
    public String toString() { return "QBitmap(" + width + "x" + height + ")"; }
}