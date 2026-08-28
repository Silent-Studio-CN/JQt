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
 * 按钮：封装 C++ 侧的 {@code QPushButton}。
 * <p>
 * 信号槽（均可注册多个监听器，按注册顺序触发）：
 * <ul>
 *   <li>{@link #onClicked(Runnable)} — clicked 信号（按下并在按钮上释放）</li>
 *   <li>{@link #onPressed(Runnable)} — pressed 信号（按下瞬间）</li>
 *   <li>{@link #onReleased(Runnable)} — released 信号（释放瞬间）</li>
 *   <li>{@link #onToggled(Consumer)} — toggled 信号（勾选状态切换，需 {@link #setCheckable(boolean)}）</li>
 * </ul>
 */
public class QPushButton extends QWidget {

    private final List<Runnable> onClickedHandlers = new ArrayList<>();
    private final List<Runnable> onPressedHandlers = new ArrayList<>();
    private final List<Runnable> onReleasedHandlers = new ArrayList<>();
    private final List<Consumer<Boolean>> onToggledHandlers = new ArrayList<>();

    public QPushButton(String text) {
        nativeHandle = nativeCreate(text);
        registerCleaner();
    }

    private native long nativeCreate(String text);

    /** 修改按钮文字。 */
    public void setText(String text) {
        nativeSetText(nativeHandle, text);
    }
    private native void nativeSetText(long handle, String text);

    /** 是否可勾选（checkable 按钮点击时在选中/未选中间切换，发出 toggled 信号）。 */
    public void setCheckable(boolean checkable) {
        nativeSetCheckable(nativeHandle, checkable);
    }
    private native void nativeSetCheckable(long handle, boolean checkable);

    /** 设置勾选状态（仅 checkable 按钮有效，会发出 toggled 信号）。 */
    public void setChecked(boolean checked) {
        nativeSetChecked(nativeHandle, checked);
    }

    /** 当前是否勾选。 */
    public boolean isChecked() {
        return nativeIsChecked(nativeHandle);
    }
    private static native boolean nativeIsChecked(long handle);
    private native void nativeSetChecked(long handle, boolean checked);

    /** 注册点击回调（Qt clicked 信号）。 */
    public QPushButton onClicked(Runnable handler) {
        onClickedHandlers.add(handler);
        return this;
    }

    /** 注册按下回调（Qt pressed 信号，鼠标按下瞬间）。 */
    public QPushButton onPressed(Runnable handler) {
        onPressedHandlers.add(handler);
        return this;
    }

    /** 注册释放回调（Qt released 信号，鼠标释放瞬间）。 */
    public QPushButton onReleased(Runnable handler) {
        onReleasedHandlers.add(handler);
        return this;
    }

    /** 注册勾选状态切换回调（Qt toggled 信号，参数为新的勾选状态）。 */
    public QPushButton onToggled(Consumer<Boolean> handler) {
        onToggledHandlers.add(handler);
        return this;
    }

    /** 由 C++ 侧在按钮被点击时回调（JNI）。 */
    void nativeHandleClick() {
        for (Runnable h : onClickedHandlers) {
            h.run();
        }
    }

    /** 由 C++ 侧在按钮被按下时回调（JNI）。 */
    void nativeHandlePressed() {
        for (Runnable h : onPressedHandlers) {
            h.run();
        }
    }

    /** 由 C++ 侧在按钮被释放时回调（JNI）。 */
    void nativeHandleReleased() {
        for (Runnable h : onReleasedHandlers) {
            h.run();
        }
    }

    /** 由 C++ 侧在勾选状态切换时回调（JNI）。 */
    void nativeHandleToggled(boolean checked) {
        for (Consumer<Boolean> h : onToggledHandlers) {
            h.accept(checked);
        }
    }

    // ---- L1 补全（v0.6.0，QAbstractButton）----

    /** 程序化点击（触发 onClicked）。 */
    public void click() { nativeClick(nativeHandle); }
    private static native void nativeClick(long handle);

    /** 切换勾选状态（需 setCheckable(true)）。 */
    public void toggle() { nativeToggle(nativeHandle); }
    private static native void nativeToggle(long handle);

    /** 可勾选模式（已在类首部定义）。 */

    /** 关联弹出菜单（按钮右侧出现小箭头，点击弹出）。 */
    public void setMenu(QMenu menu) { nativeSetMenu(nativeHandle, menu.nativeHandle); }
    private static native void nativeSetMenu(long handle, long menuHandle);

    /** 当前文本（QAbstractButton::text）。 */
    public String text() { return nativeText(nativeHandle); }
    private static native String nativeText(long handle);

    /** 设置图标（图片文件路径，QAbstractButton::setIcon）。 */
    public void setIcon(String iconPath) {
        nativeSetIcon(nativeHandle, iconPath);
    }
    private static native void nativeSetIcon(long handle, String iconPath);

    /** 当前图标路径（未设置返回 null）。 */
    public String icon() { return nativeIcon(nativeHandle); }
    private static native String nativeIcon(long handle);

    /** 设置快捷键（QAbstractButton::setShortcut，如 "Ctrl+O"）。 */
    public void setShortcut(String shortcut) {
        nativeSetShortcut(nativeHandle, shortcut);
    }
    private static native void nativeSetShortcut(long handle, String shortcut);

    /** 当前快捷键。 */
    public String shortcut() { return nativeShortcut(nativeHandle); }
    private static native String nativeShortcut(long handle);

    /** 是否关联了菜单。 */
    public boolean menu() { return nativeHasMenu(nativeHandle); }
    private static native boolean nativeHasMenu(long handle);
}