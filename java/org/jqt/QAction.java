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

// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** autoRepeat（Qt autoRepeat）。 */
    public boolean autoRepeat() {
        return nativeAutoRepeat(nativeHandle);
    }
    private static native boolean nativeAutoRepeat(long nativeHandle);

    /** hover（Qt hover）。 */
    public void hover() {
        nativeHover(nativeHandle);
    }
    private static native void nativeHover(long nativeHandle);

    /** iconText（Qt iconText）。 */
    public String iconText() {
        return nativeIconText(nativeHandle);
    }
    private static native String nativeIconText(long nativeHandle);

    /** isCheckable（Qt isCheckable）。 */
    public boolean isCheckable() {
        return nativeIsCheckable(nativeHandle);
    }
    private static native boolean nativeIsCheckable(long nativeHandle);

    /** isEnabled（Qt isEnabled）。 */
    public boolean isEnabled() {
        return nativeIsEnabled(nativeHandle);
    }
    private static native boolean nativeIsEnabled(long nativeHandle);

    /** isIconVisibleInMenu（Qt isIconVisibleInMenu）。 */
    public boolean isIconVisibleInMenu() {
        return nativeIsIconVisibleInMenu(nativeHandle);
    }
    private static native boolean nativeIsIconVisibleInMenu(long nativeHandle);

    /** isShortcutVisibleInContextMenu（Qt isShortcutVisibleInContextMenu）。 */
    public boolean isShortcutVisibleInContextMenu() {
        return nativeIsShortcutVisibleInContextMenu(nativeHandle);
    }
    private static native boolean nativeIsShortcutVisibleInContextMenu(long nativeHandle);

    /** isVisible（Qt isVisible）。 */
    public boolean isVisible() {
        return nativeIsVisible(nativeHandle);
    }
    private static native boolean nativeIsVisible(long nativeHandle);

    /** resetEnabled（Qt resetEnabled）。 */
    public void resetEnabled() {
        nativeResetEnabled(nativeHandle);
    }
    private static native void nativeResetEnabled(long nativeHandle);

    /** setAutoRepeat（Qt setAutoRepeat）。 */
    public void setAutoRepeat(boolean arg0) {
        nativeSetAutoRepeat(nativeHandle, arg0);
    }
    private static native void nativeSetAutoRepeat(long nativeHandle, boolean arg0);

    /** setDisabled（Qt setDisabled）。 */
    public void setDisabled(boolean arg0) {
        nativeSetDisabled(nativeHandle, arg0);
    }
    private static native void nativeSetDisabled(long nativeHandle, boolean arg0);

    /** setEnabled（Qt setEnabled）。 */
    public void setEnabled(boolean arg0) {
        nativeSetEnabled(nativeHandle, arg0);
    }
    private static native void nativeSetEnabled(long nativeHandle, boolean arg0);

    /** setIconVisibleInMenu（Qt setIconVisibleInMenu）。 */
    public void setIconVisibleInMenu(boolean arg0) {
        nativeSetIconVisibleInMenu(nativeHandle, arg0);
    }
    private static native void nativeSetIconVisibleInMenu(long nativeHandle, boolean arg0);

    /** setSeparator（Qt setSeparator）。 */
    public void setSeparator(boolean arg0) {
        nativeSetSeparator(nativeHandle, arg0);
    }
    private static native void nativeSetSeparator(long nativeHandle, boolean arg0);

    /** setShortcutVisibleInContextMenu（Qt setShortcutVisibleInContextMenu）。 */
    public void setShortcutVisibleInContextMenu(boolean arg0) {
        nativeSetShortcutVisibleInContextMenu(nativeHandle, arg0);
    }
    private static native void nativeSetShortcutVisibleInContextMenu(long nativeHandle, boolean arg0);

    /** setVisible（Qt setVisible）。 */
    public void setVisible(boolean arg0) {
        nativeSetVisible(nativeHandle, arg0);
    }
    private static native void nativeSetVisible(long nativeHandle, boolean arg0);

    /** setWhatsThis（Qt setWhatsThis）。 */
    public void setWhatsThis(String arg0) {
        nativeSetWhatsThis(nativeHandle, arg0);
    }
    private static native void nativeSetWhatsThis(long nativeHandle, String arg0);

    /** statusTip（Qt statusTip）。 */
    public String statusTip() {
        return nativeStatusTip(nativeHandle);
    }
    private static native String nativeStatusTip(long nativeHandle);

    /** whatsThis（Qt whatsThis）。 */
    public String whatsThis() {
        return nativeWhatsThis(nativeHandle);
    }
    private static native String nativeWhatsThis(long nativeHandle);

}