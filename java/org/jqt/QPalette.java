/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.EnumMap;
import java.util.Map;

/**
 * 调色板（Qt {@code QPalette}，纯 Java 值类：角色 → 颜色）。
 */
public class QPalette {

    /** 颜色角色（Qt ColorRole 常用）。 */
    public enum ColorRole {
        Window(0), WindowText(1), Base(2), AlternateBase(3), ToolTipBase(4), ToolTipText(5),
        Text(6), Button(7), ButtonText(8), BrightText(9), Link(10), Highlight(11),
        HighlightedText(12), PlaceholderText(13), Disabled(14);
        public final int value;
        ColorRole(int v) { value = v; }
    }

    /** 组（Qt ColorGroup 简化：Active/Inactive 同）。 */
    public enum ColorGroup { Active(0), Inactive(1), Disabled(2);
        public final int value;
        ColorGroup(int v) { value = v; }
    }

    private final Map<ColorRole, QColor> active = new EnumMap<>(ColorRole.class);
    private final Map<ColorRole, QColor> disabled = new EnumMap<>(ColorRole.class);

    public QPalette() { }

    public QPalette(QColor button) {
        setColor(ColorRole.Button, button);
        setColor(ColorRole.Window, button);
    }

    public QColor color(ColorRole role) {
        QColor c = active.get(role);
        return c != null ? c : QColor.Black;
    }
    public QColor color(ColorGroup group, ColorRole role) {
        Map<ColorRole, QColor> m = group == ColorGroup.Disabled ? disabled : active;
        QColor c = m.get(role);
        return c != null ? c : color(role);
    }

    public void setColor(ColorRole role, QColor color) {
        active.put(role, color != null ? color : QColor.Black);
    }
    public void setColor(ColorGroup group, ColorRole role, QColor color) {
        (group == ColorGroup.Disabled ? disabled : active).put(role, color != null ? color : QColor.Black);
    }

    public QColor window() { return color(ColorRole.Window); }
    public QColor windowText() { return color(ColorRole.WindowText); }
    public QColor base() { return color(ColorRole.Base); }
    public QColor text() { return color(ColorRole.Text); }
    public QColor button() { return color(ColorRole.Button); }
    public QColor buttonText() { return color(ColorRole.ButtonText); }
    public QColor highlight() { return color(ColorRole.Highlight); }
    public QColor highlightedText() { return color(ColorRole.HighlightedText); }
    public QColor toolTipBase() { return color(ColorRole.ToolTipBase); }
    public QColor toolTipText() { return color(ColorRole.ToolTipText); }
    public QColor placeholderText() { return color(ColorRole.PlaceholderText); }
    public QColor link() { return color(ColorRole.Link); }
    public QColor brightText() { return color(ColorRole.BrightText); }

    public void setWindow(QColor c) { setColor(ColorRole.Window, c); }
    public void setWindowText(QColor c) { setColor(ColorRole.WindowText, c); }
    public void setBase(QColor c) { setColor(ColorRole.Base, c); }
    public void setText(QColor c) { setColor(ColorRole.Text, c); }
    public void setButton(QColor c) { setColor(ColorRole.Button, c); }
    public void setButtonText(QColor c) { setColor(ColorRole.ButtonText, c); }
    public void setHighlight(QColor c) { setColor(ColorRole.Highlight, c); }
    public void setHighlightedText(QColor c) { setColor(ColorRole.HighlightedText, c); }
    public void setPlaceholderText(QColor c) { setColor(ColorRole.PlaceholderText, c); }
    public void setLink(QColor c) { setColor(ColorRole.Link, c); }
    public void setBrightText(QColor c) { setColor(ColorRole.BrightText, c); }

    public boolean isEqual(ColorGroup group, ColorRole role) {
        return color(group, role).equals(color(role));
    }

    @Override
    public String toString() { return "QPalette(roles=" + active.size() + ")"; }
}
