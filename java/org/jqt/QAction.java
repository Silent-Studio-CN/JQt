/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 动作（QAction）：可独立触发/勾选的操作单元，可与菜单、按钮、快捷键关联。
 * <p>
 * L1 补全（v0.7.1）：text/icon/shortcut/toolTip/font/toggle/toggled/triggered/menu。
 */
public class QAction {

    private final List<Runnable> triggeredHandlers = new ArrayList<>();
    private final List<Consumer<Boolean>> toggledHandlers = new ArrayList<>();

    private long nativeHandle;

    /** 创建动作（默认不可勾选）。 */
    public QAction(String text) {
        nativeHandle = nativeCreate(text);
    }

    /** 设置文本（QAction::setText）。 */
    public void setText(String text) { nativeSetText(nativeHandle, text); }

    /** 当前文本。 */
    public String text() { return nativeText(nativeHandle); }

    /** 设置图标（图片文件路径）。 */
    public void setIcon(String iconPath) { nativeSetIcon(nativeHandle, iconPath); }

    /** 当前图标路径（未设置返回 null）。 */
    public String icon() { return nativeIcon(nativeHandle); }

    /** 设置快捷键（如 "Ctrl+S"）。 */
    public void setShortcut(String shortcut) { nativeSetShortcut(nativeHandle, shortcut); }

    /** 当前快捷键。 */
    public String shortcut() { return nativeShortcut(nativeHandle); }

    /** 设置提示文字（toolTip）。 */
    public void setToolTip(String tip) { nativeSetToolTip(nativeHandle, tip); }

    /** 当前提示文字。 */
    public String toolTip() { return nativeToolTip(nativeHandle); }

    /** 设置字体（简化：族名 + 字号；QFont 值类未建）。 */
    public void setFont(String family, int pointSize) { nativeSetFont(nativeHandle, family, pointSize); }

    /** 是否可勾选（checkable）。 */
    public void setCheckable(boolean checkable) { nativeSetCheckable(nativeHandle, checkable); }

    /** 是否已勾选。 */
    public boolean isChecked() { return nativeIsChecked(nativeHandle); }

    /** 设置勾选状态。 */
    public void setChecked(boolean checked) { nativeSetChecked(nativeHandle, checked); }

    /** 切换勾选状态（QAction::toggle）。 */
    public void toggle() { nativeToggle(nativeHandle); }

    /** 触发动作（QAction::trigger）。 */
    public void trigger() { nativeTrigger(nativeHandle); }

    /** 关联菜单（QAction::setMenu；action 作为子菜单入口）。 */
    public void setMenu(QMenu menu) { nativeSetMenu(nativeHandle, menu.nativeHandle()); }

    /** 触发回调（triggered 信号）。 */
    public QAction onTriggered(Runnable handler) {
        triggeredHandlers.add(handler);
        nativeConnectTriggered(nativeHandle);
        return this;
    }

    /** 勾选状态变化回调（toggled 信号，参数为当前勾选状态）。 */
    public QAction onToggled(Consumer<Boolean> handler) {
        toggledHandlers.add(handler);
        nativeConnectToggled(nativeHandle);
        return this;
    }

    void nativeHandleTriggered() {
        for (Runnable h : triggeredHandlers) h.run();
    }

    void nativeHandleToggled(boolean checked) {
        for (Consumer<Boolean> h : toggledHandlers) h.accept(checked);
    }

    private native long nativeCreate(String text);
    private native void nativeSetText(long handle, String text);
    private native String nativeText(long handle);
    private native void nativeSetIcon(long handle, String iconPath);
    private native String nativeIcon(long handle);
    private native void nativeSetShortcut(long handle, String shortcut);
    private native String nativeShortcut(long handle);
    private native void nativeSetToolTip(long handle, String tip);
    private native String nativeToolTip(long handle);
    private native void nativeSetFont(long handle, String family, int pointSize);
    private native void nativeSetCheckable(long handle, boolean checkable);
    private native boolean nativeIsChecked(long handle);
    private native void nativeSetChecked(long handle, boolean checked);
    private native void nativeToggle(long handle);
    private native void nativeTrigger(long handle);
    private native void nativeSetMenu(long handle, long menuHandle);
    private native void nativeConnectTriggered(long handle);
    private native void nativeConnectToggled(long handle);
}
