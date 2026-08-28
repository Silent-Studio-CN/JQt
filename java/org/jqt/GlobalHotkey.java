/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局热键（Exclusive Kit，v0.6.1）：注册系统级快捷键（应用失焦也生效）。
 * <pre>
 * GlobalHotkey hk = new GlobalHotkey();
 * hk.register("Ctrl+Shift+X", () -> System.out.println("全局热键触发"));
 * hk.unregister();
 * </pre>
 * 修饰键：Ctrl / Alt / Shift / Win；主键为单个字母或 F1-F24。
 */
public class GlobalHotkey {

    private static final List<GlobalHotkey> ALL = new ArrayList<>();

    private int hotkeyId = 0;
    private boolean registered;

    /** 注册全局热键（组合如 "Ctrl+Shift+X"），触发时调用 handler。 */
    public boolean register(String combo, Runnable handler) {
        int id = nativeRegister(combo);
        if (id <= 0) {
            return false;
        }
        hotkeyId = id;
        registered = true;
        ALL.add(this);
        this.handler = handler;
        return true;
    }

    /** 注销热键。 */
    public void unregister() {
        if (registered) {
            nativeUnregister(hotkeyId);
            ALL.remove(this);
            registered = false;
        }
    }

    private static native int nativeRegister(String combo);
    private static native void nativeUnregister(int hotkeyId);

    private Runnable handler;

    /** 由 C++ 侧在热键触发时回调（JNI）。 */
    static void nativeHandleHotkey(int hotkeyId) {
        for (GlobalHotkey hk : ALL) {
            if (hk.registered && hk.hotkeyId == hotkeyId && hk.handler != null) {
                hk.handler.run();
            }
        }
    }
}
