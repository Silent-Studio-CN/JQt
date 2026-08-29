/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 二维整数矩形（Qt {@code QRect} 值类，纯 Java 实现）。
 * <p>完整覆盖 Qt 6 QRect API（含运算符语义）。Qt 语义：right()=x+width-1、bottom()=y+height-1。
 */
public class QRect {

    private int x;
    private int y;
    private int width;
    private int height;

    public QRect() { this(0, 0, 0, 0); }
    public QRect(int x, int y, int width, int height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
    }
    public QRect(QPoint topLeft, QSize size) {
        this(topLeft.x(), topLeft.y(), size.width(), size.height());
    }
    public QRect(QPoint topLeft, QPoint bottomRight) {
        this(topLeft.x(), topLeft.y(),
             bottomRight.x() - topLeft.x() + 1, bottomRight.y() - topLeft.y() + 1);
    }

    // ---- 位置/尺寸 ----
    public int x() { return x; }
    public int y() { return y; }
    public int width() { return width; }
    public int height() { return height; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public void setWidth(int width) { this.width = width; }
    public void setHeight(int height) { this.height = height; }

    public QSize size() { return new QSize(width, height); }
    public void setSize(QSize size) { this.width = size.width(); this.height = size.height(); }

    // ---- 边 ----
    public int left() { return x; }
    public int right() { return x + width - 1; }
    public int top() { return y; }
    public int bottom() { return y + height - 1; }
    public void setLeft(int left) { width = right() - left + 1; x = left; }
    public void setRight(int right) { width = right - x + 1; }
    public void setTop(int top) { height = bottom() - top + 1; y = top; }
    public void setBottom(int bottom) { height = bottom - y + 1; }

    // ---- 角 ----
    public QPoint topLeft() { return new QPoint(x, y); }
    public QPoint topRight() { return new QPoint(x + width - 1, y); }
    public QPoint bottomLeft() { return new QPoint(x, y + height - 1); }
    public QPoint bottomRight() { return new QPoint(x + width - 1, y + height - 1); }
    public void setTopLeft(QPoint p) { x = p.x(); y = p.y(); }
    public void setTopRight(QPoint p) { width = p.x() - x + 1; y = p.y(); }
    public void setBottomLeft(QPoint p) { x = p.x(); height = p.y() - y + 1; }
    public void setBottomRight(QPoint p) { width = p.x() - x + 1; height = p.y() - y + 1; }

    /** 中心点。 */
    public QPoint center() { return new QPoint(x + width / 2, y + height / 2); }

    // ---- 状态 ----
    public boolean isEmpty() { return width <= 0 || height <= 0; }
    public boolean isNull() { return width == 0 && height == 0; }
    public boolean isValid() { return width > 0 && height > 0; }

    // ---- 调整 ----
    public void adjust(int dx1, int dy1, int dx2, int dy2) {
        x += dx1; y += dy1; width += dx2 - dx1; height += dy2 - dy1;
    }
    public QRect adjusted(int dx1, int dy1, int dx2, int dy2) {
        return new QRect(x + dx1, y + dy1, width + dx2 - dx1, height + dy2 - dy1);
    }

    /** 规范化（负宽高翻转为正）。 */
    public QRect normalized() {
        QRect r = new QRect(x, y, width, height);
        if (r.width < 0) { r.x += r.width; r.width = -r.width; }
        if (r.height < 0) { r.y += r.height; r.height = -r.height; }
        return r;
    }

    // ---- 移动 ----
    public void moveTo(int x, int y) { this.x = x; this.y = y; }
    public void moveTo(QPoint p) { this.x = p.x(); this.y = p.y(); }
    public void moveLeft(int left) { x = left; }
    public void moveRight(int right) { x = right - width + 1; }
    public void moveTop(int top) { y = top; }
    public void moveBottom(int bottom) { y = bottom - height + 1; }
    public void moveTopLeft(QPoint p) { x = p.x(); y = p.y(); }
    public void moveTopRight(QPoint p) { x = p.x() - width + 1; y = p.y(); }
    public void moveBottomLeft(QPoint p) { x = p.x(); y = p.y() - height + 1; }
    public void moveBottomRight(QPoint p) { x = p.x() - width + 1; y = p.y() - height + 1; }
    public void moveCenter(QPoint p) { x = p.x() - width / 2; y = p.y() - height / 2; }

    public void translate(int dx, int dy) { x += dx; y += dy; }
    public void translate(QPoint offset) { x += offset.x(); y += offset.y(); }
    public QRect translated(int dx, int dy) { return new QRect(x + dx, y + dy, width, height); }
    public QRect translated(QPoint offset) { return translated(offset.x(), offset.y()); }

    // ---- 集合运算 ----
    public boolean contains(int px, int py) {
        return px >= x && px <= right() && py >= y && py <= bottom();
    }
    public boolean contains(QPoint p) { return contains(p.x(), p.y()); }
    /** 是否完全包含另一矩形。 */
    public boolean contains(QRect r) {
        return contains(r.x, r.y) && contains(r.x + r.width - 1, r.y + r.height - 1);
    }
    /** 是否相交（含触碰）。 */
    public boolean intersects(QRect r) {
        return x <= r.right() && r.x <= right() && y <= r.bottom() && r.y <= bottom();
    }
    public QRect intersected(QRect r) {
        int nx = Math.max(x, r.x), ny = Math.max(y, r.y);
        int nr = Math.min(right(), r.right()), nb = Math.min(bottom(), r.bottom());
        if (nx > nr || ny > nb) return new QRect();
        return new QRect(nx, ny, nr - nx + 1, nb - ny + 1);
    }
    /** 并集（最小外接）。 */
    public QRect united(QRect r) {
        QRect n = normalized(); QRect m = r.normalized();
        int nx = Math.min(n.x, m.x), ny = Math.min(n.y, m.y);
        int nr = Math.max(n.right(), m.right()), nb = Math.max(n.bottom(), m.bottom());
        return new QRect(nx, ny, nr - nx + 1, nb - ny + 1);
    }
    /** 包围两矩形范围的边界（span）。 */
    public QRect span(QRect r) { return united(r); }

    /** 四边加边距。 */
    public QRect marginsAdded(QMargins m) {
        return adjusted(-m.left(), -m.top(), m.right(), m.bottom());
    }
    public QRect marginsRemoved(QMargins m) {
        return adjusted(m.left(), m.top(), -m.right(), -m.bottom());
    }

    // ---- 坐标读写 ----
    public void setRect(int x, int y, int width, int height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
    }
    public void getRect(int[] xywh) {
        if (xywh != null && xywh.length >= 4) { xywh[0] = x; xywh[1] = y; xywh[2] = width; xywh[3] = height; }
    }
    public void setCoords(int x1, int y1, int x2, int y2) {
        x = x1; y = y1; width = x2 - x1 + 1; height = y2 - y1 + 1;
    }
    public void getCoords(int[] xyxy) {
        if (xyxy != null && xyxy.length >= 4) { xyxy[0] = x; xyxy[1] = y; xyxy[2] = right(); xyxy[3] = bottom(); }
    }

    /** 转双精度矩形。 */
    public QRectF toRectF() { return new QRectF(x, y, width, height); }

    /** 转置（交换 x/y 与宽高）。 */
    public QRect transposed() { return new QRect(y, x, height, width); }

    // ---- 运算符语义（& 交集、| 并集、+ 平移） ----
    public QRect and(QRect r) { return intersected(r); }
    public QRect or(QRect r) { return united(r); }
    public QRect plus(QPoint p) { return translated(p); }
    public QRect minus(QPoint p) { return translated(-p.x(), -p.y()); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QRect)) return false;
        QRect r = (QRect) o;
        return x == r.x && y == r.y && width == r.width && height == r.height;
    }

    @Override
    public int hashCode() {
        int h = x; h = 31 * h + y; h = 31 * h + width; h = 31 * h + height;
        return h;
    }

    @Override
    public String toString() {
        return "QRect(" + x + "," + y + " " + width + "x" + height + ")";
    }
}
