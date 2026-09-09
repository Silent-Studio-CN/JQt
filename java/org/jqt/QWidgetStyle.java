/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * style() 分组门面：外观样式（api-tiering §2 L2 设计）。
 * <p>缺口账：setStyle(QStyle 对象化)、palette 角色级便捷方法。
 */
public class QWidgetStyle {

    private final QWidget owner;

    QWidgetStyle(QWidget owner) {
        this.owner = owner;
    }

    /** 设置字体（值对象）。 */
    public void setFont(QFont font) { owner.setFont(font); }

    /** 设置字体（族名 + 字号）。 */
    public void setFont(String family, int pointSize) { owner.setFont(family, pointSize); }

    /** 字体族名（"Family,size" 中取族名；未单独设置返回默认）。 */
    public String fontFamily() {
        String f = owner.font();
        int i = f == null ? -1 : f.lastIndexOf(',');
        return i > 0 ? f.substring(0, i) : f;
    }

    /** 字号（"Family,size" 中取字号；解析失败返回 -1）。 */
    public int fontSize() {
        String f = owner.font();
        int i = f == null ? -1 : f.lastIndexOf(',');
        try {
            return i > 0 ? Integer.parseInt(f.substring(i + 1).trim()) : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** 背景色（0xAARRGGBB；简化 palette 查询）。 */
    public int palette() { return owner.palette(); }

    /** 设置调色板（8 角色）。 */
    public void setPalette(QPalette palette) { owner.setPalette(palette); }

    /** 控件级样式表。 */
    public String styleSheet() { return owner.styleSheet(); }

    /** 控件级样式表。 */
    public void setStyleSheet(String qss) { owner.setStyleSheet(qss); }

    /** 鼠标跟踪（无需按键即收 move 事件）。 */
    public void setMouseTracking(boolean on) { owner.setMouseTracking(on); }

    /** 是否启用鼠标跟踪。 */
    public boolean hasMouseTracking() { return owner.hasMouseTracking(); }

    /** 鼠标形状（名：arrow/ibeam/…）。 */
    public void setCursor(String shape) { owner.setCursor(shape); }

    /** 鼠标形状名。 */
    public String cursor() { return owner.cursor(); }

    /** 对象名（QSS #id 用）。 */
    public void setObjectName(String name) { owner.setObjectName(name); }

    /** 对象名。 */
    public String objectName() { return owner.objectName(); }
}
