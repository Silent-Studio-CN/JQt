/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * drag() 分组门面：拖拽、鼠标与键盘捕获（api-tiering §2 L2 设计）。
 * <p>缺口账：setDragEnabled（QAbstractItemView 域）、drop 事件回调（onXxx 模式后续）。
 */
public class QWidgetDrag {

    private final QWidget owner;

    QWidgetDrag(QWidget owner) {
        this.owner = owner;
    }

    /** 接受拖放。 */
    public void setAcceptDrops(boolean on) { owner.setAcceptDrops(on); }

    /** 是否接受拖放。 */
    public boolean acceptDrops() { return owner.acceptDrops(); }

    /** 独占鼠标（全部事件发往本控件）。 */
    public void grabMouse() { owner.grabMouse(); }

    /** 释放鼠标独占。 */
    public void releaseMouse() { owner.releaseMouse(); }

    /** 独占键盘。 */
    public void grabKeyboard() { owner.grabKeyboard(); }

    /** 释放键盘独占。 */
    public void releaseKeyboard() { owner.releaseKeyboard(); }
}
