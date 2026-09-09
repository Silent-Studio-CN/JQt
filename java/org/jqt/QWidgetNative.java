/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * native() 分组门面：底层/高级入口（api-tiering §2 L3 语义的收纳点）。
 * <p>缺口账：winId/effectiveWinId/render（需新 native，后续高级批次）。
 */
public class QWidgetNative {

    private final QWidget owner;

    QWidgetNative(QWidget owner) {
        this.owner = owner;
    }

    /** C++ 句柄 ID（高级用途）。 */
    public long nativeHandle() {
        return owner.nativeHandle();
    }

    /** 是否为独立窗口（QWidget::isWindow）。 */
    public boolean isWindow() {
        return owner.isWindow();
    }

    /** 是否已创建且未释放。 */
    public boolean isCreated() {
        return owner.isCreated();
    }

    /** 是否已释放。 */
    public boolean isDisposed() {
        return owner.isDisposed();
    }

    /** 按原生窗口句柄查找控件（QWidget::find 静态透传；未找到返回 0）。 */
    public static long find(long winId) {
        return QWidget.find(winId);
    }
}
