/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/** 缓动函数（映射 QEasingCurve::Type）。 */
public enum JQtEasing {
    LINEAR(0),
    IN_QUAD(1), OUT_QUAD(2), IN_OUT_QUAD(3), OUT_IN_QUAD(4),
    IN_CUBIC(5), OUT_CUBIC(6), IN_OUT_CUBIC(7), OUT_IN_CUBIC(8),
    IN_QUART(9), OUT_QUART(10), IN_OUT_QUART(11), OUT_IN_QUART(12),
    IN_QUINT(13), OUT_QUINT(14), IN_OUT_QUINT(15), OUT_IN_QUINT(16),
    IN_SINE(17), OUT_SINE(18), IN_OUT_SINE(19), OUT_IN_SINE(20),
    IN_EXPO(21), OUT_EXPO(22), IN_OUT_EXPO(23), OUT_IN_EXPO(24),
    IN_CIRC(25), OUT_CIRC(26), IN_OUT_CIRC(27), OUT_IN_CIRC(28),
    IN_ELASTIC(29), OUT_ELASTIC(30), IN_OUT_ELASTIC(31), OUT_IN_ELASTIC(32),
    IN_BACK(33), OUT_BACK(34), IN_OUT_BACK(35), OUT_IN_BACK(36),
    IN_BOUNCE(37), OUT_BOUNCE(38), IN_OUT_BOUNCE(39), OUT_IN_BOUNCE(40)
    ;

    final int qtType;

    JQtEasing(int qtType) {
        this.qtType = qtType;
    }
}