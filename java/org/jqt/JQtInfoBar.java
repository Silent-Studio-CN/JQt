/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 顶部通知条：滑入 → 停留 → 滑出，自动清理。
 * 样式走 QSS 模板：QFrame#infoBar（背景/边框）、QLabel#infoBarLabel（文字）。
 */
public class JQtInfoBar {

    private JQtInfoBar() {
    }

    /** 在窗口顶部显示通知（durationMs 毫秒后自动消失）。 */
    public static void show(JQtWindow window, String text, long durationMs) {
        nativeShowInfo(window.nativeHandle(), text, durationMs);
    }
    static native void nativeShowInfo(long winHandle, String text, long durationMs);
}
