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
 * Fluent 风格开关控件：轨道 + 滑块，切换时滑块位移动画。
 * 自定义绘制（非 QSS 子控件），颜色跟随内置配色。
 */
public class JQtSwitch extends QWidget {

    private final List<Consumer<Boolean>> onToggledHandlers = new ArrayList<>();

    /** 创建开关（默认关闭）。 */
    public JQtSwitch() {
        this(false);
    }

    /** 创建开关（指定初始状态）。 */
    public JQtSwitch(boolean checked) {
        nativeHandle = nativeCreate(checked);
        registerCleaner();
    }

    private native long nativeCreate(boolean checked);

    /** 当前是否开启。 */
    public boolean isChecked() {
        return nativeIsChecked(nativeHandle);
    }
    private native boolean nativeIsChecked(long handle);

    /** 设置状态（带动画切换，触发 onToggled）。 */
    public void setChecked(boolean checked) {
        nativeSetChecked(nativeHandle, checked);
    }
    private native void nativeSetChecked(long handle, boolean checked);

    /** 状态切换回调（参数为新状态）。 */
    public JQtSwitch onToggled(Consumer<Boolean> handler) {
        onToggledHandlers.add(handler);
        return this;
    }

    /** 由 C++ 侧回调（JNI）。 */
    void nativeHandleToggled(boolean checked) {
        for (Consumer<Boolean> h : onToggledHandlers) {
            h.accept(checked);
        }
    }
}
