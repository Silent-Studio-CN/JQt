/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * event() 分组门面：事件系统（api-tiering §2 L2 设计，预留组）。
 * <p>缺口账：setInputMethodHints（输入法，直传级待补）。当前无成员——组存在但方法随缺口补齐，
 * 避免无 native 支撑的空壳 API。
 */
public class QWidgetEvent {

    private final QWidget owner;

    QWidgetEvent(QWidget owner) {
        this.owner = owner;
    }

    /** 所属控件（供链式使用/调试）。 */
    public QWidget owner() {
        return owner;
    }
}
