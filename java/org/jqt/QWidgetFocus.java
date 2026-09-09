/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * focus() 分组门面：焦点管理（api-tiering §2 L2 设计）。
 * <p>缺口账：setFocusProxy/focusProxy/nextInFocusChain/previousInFocusChain（焦点链族，直传级，待补）。
 */
public class QWidgetFocus {

    private final QWidget owner;

    QWidgetFocus(QWidget owner) {
        this.owner = owner;
    }

    /** 焦点策略（0 NoFocus / 1 Tab / 2 Click / 4 Strong / 8 Wheel）。 */
    public void setFocusPolicy(int policy) { owner.setFocusPolicy(policy); }

    /** 焦点策略。 */
    public int focusPolicy() { return owner.focusPolicy(); }

    /** 获取键盘焦点。 */
    public void setFocus() { owner.setFocus(); }

    /** 是否有焦点。 */
    public boolean hasFocus() { return owner.hasFocus(); }

    /** 清除焦点。 */
    public void clearFocus() { owner.clearFocus(); }
}
