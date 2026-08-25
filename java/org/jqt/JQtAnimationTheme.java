/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 动画主题：全局动效节奏配置（时长倍率 + 默认缓动）。
 * 通过 {@link JQtApplication#setAnimationTheme(JQtAnimationTheme)} 应用，
 * 所有 JQt 动效（hover / 入场 / pivot 指示器）统一跟随。
 */
public class JQtAnimationTheme {

    /** 标准节奏（1.0x，OutCubic）。 */
    public static final JQtAnimationTheme DEFAULT = new JQtAnimationTheme(1.0, JQtEasing.OUT_CUBIC);

    /** 轻快节奏（0.65x）——适合触摸屏/演示。 */
    public static final JQtAnimationTheme FAST = new JQtAnimationTheme(0.65, JQtEasing.OUT_CUBIC);

    /** 舒缓节奏（1.6x，OutQuint）——适合桌面沉浸感。 */
    public static final JQtAnimationTheme RELAXED = new JQtAnimationTheme(1.6, JQtEasing.OUT_QUINT);

    /** 关闭动效（瞬时完成，用于无障碍/低配环境）。 */
    public static final JQtAnimationTheme OFF = new JQtAnimationTheme(0.0, JQtEasing.LINEAR);

    /** 时长倍率（1.0 = 标准；0 = 禁用所有动效）。 */
    public final double speed;

    /** 默认缓动曲线（未显式指定的动效用它）。 */
    public final JQtEasing easing;

    /** 自定义主题。
     * @param speed 时长倍率，0 = 禁用动效
     * @param easing 默认缓动
     */
    public JQtAnimationTheme(double speed, JQtEasing easing) {
        this.speed = speed;
        this.easing = easing;
    }

    /** 动效是否启用。 */
    public boolean enabled() {
        return speed > 0;
    }

    /** 按本主题缩放时长（禁用时返回 0）。 */
    public long apply(long ms) {
        if (speed <= 0) {
            return 0;
        }
        long d = (long) (ms * speed);
        return d < 16 ? 16 : d;   // 至少 1 帧
    }
}
