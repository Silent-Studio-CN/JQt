/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * Fluent 动效库（clean-room，独立实现；视觉参数参考微软 Fluent 公开动效规范）。
 * <ul>
 *   <li><b>hover</b>：按钮悬停高亮过渡（默认开启，150ms，叠加白色 8.5% 高亮层）</li>
 *   <li><b>entrance</b>：控件入场（下方 24px 滑入）</li>
 *   <li><b>exit</b>：控件退场（下移，结束后隐藏）</li>
 * </ul>
 * 所有时长经 {@link JQtAnimationTheme} 缩放：{@link JQtApplication#setAnimationTheme}。
 */
public final class JQtAnimations {

    private JQtAnimations() {
    }

    /** 全局开关按钮悬停动画（跟随动画主题，一般无需手动调用）。 */
    public static void setHoverEnabled(boolean on) {
        nativeSetHoverEnabled(on);
    }
    static native void nativeSetHoverEnabled(boolean on);

    /** 控件入场：下方滑入（时长取主题默认 360ms）。 */
    public static void entrance(JQtWidget widget) {
        entrance(widget, 360, JQtApplication.getAnimationTheme().easing);
    }

    /** 控件入场：下方滑入。 */
    public static void entrance(JQtWidget widget, long ms, JQtEasing easing) {
        if (widget == null || !widget.isCreated()) {
            return;
        }
        long d = JQtApplication.getAnimationTheme().apply(ms);
        if (d <= 0) {
            return;
        }
        nativeEntrance(widget.nativeHandle(), 24, d, easing.qtType);
    }
    static native void nativeEntrance(long handle, int dy, long ms, int easing);

    /** 控件退场：下移，动画结束后隐藏（时长取主题默认 240ms）。 */
    public static void exit(JQtWidget widget) {
        exit(widget, 240, JQtApplication.getAnimationTheme().easing);
    }

    /** 控件退场：下移，动画结束后隐藏。 */
    public static void exit(JQtWidget widget, long ms, JQtEasing easing) {
        if (widget == null || !widget.isCreated()) {
            return;
        }
        long d = JQtApplication.getAnimationTheme().apply(ms);
        if (d <= 0) {
            return;
        }
        nativeExit(widget.nativeHandle(), 24, d, easing.qtType);
    }
    static native void nativeExit(long handle, int dy, long ms, int easing);
}
