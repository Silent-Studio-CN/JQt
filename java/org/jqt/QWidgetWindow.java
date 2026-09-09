/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * window() 分组门面：窗口级状态与几何（api-tiering §2 L2 设计）。
 * <p>缺口账（待后续批）：setModal（仅 QDialog 有）、setWindowFlag(s)（直传级）。
 */
public class QWidgetWindow {

    private final QWidget owner;

    QWidgetWindow(QWidget owner) {
        this.owner = owner;
    }

    /** 进入全屏（等同 showFullScreen）。 */
    public void fullScreen() { owner.showFullScreen(); }

    /** 全屏显示。 */
    public void showFullScreen() { owner.showFullScreen(); }

    /** 最大化显示。 */
    public void maximize() { owner.showMaximized(); }

    /** 最小化显示。 */
    public void minimize() { owner.showMinimized(); }

    /** 恢复正常大小。 */
    public void normal() { owner.showNormal(); }

    /** 是否全屏。 */
    public boolean isFullScreen() { return owner.isFullScreen(); }

    /** 是否最大化。 */
    public boolean isMaximized() { return owner.isMaximized(); }

    /** 是否最小化。 */
    public boolean isMinimized() { return owner.isMinimized(); }

    /** 窗口不透明度（0.0-1.0；等同 setWindowOpacity）。 */
    public void setOpacity(double opacity) { owner.setWindowOpacity(opacity); }

    /** 窗口不透明度。 */
    public double opacity() { return owner.windowOpacity(); }

    /** 是否模态（QWidget::isModal）。 */
    public boolean isModal() { return owner.isModal(); }

    /** 窗口状态（位值：0 正常 / 1 最小化 / 2 最大化 / 4 全屏）。 */
    public void setWindowState(int state) { owner.setWindowState(state); }

    /** 窗口状态。 */
    public int windowState() { return owner.windowState(); }

    /** 保存几何（配合 restoreGeometry）。 */
    public QByteArray saveGeometry() { return owner.saveGeometry(); }

    /** 恢复几何。 */
    public boolean restoreGeometry(QByteArray data) { return owner.restoreGeometry(data); }

    /** 激活窗口（等同 activateWindow）。 */
    public void activate() { owner.activateWindow(); }

    /** 是否为活动窗口（等同 isActiveWindow）。 */
    public boolean isActive() { return owner.isActiveWindow(); }
}
