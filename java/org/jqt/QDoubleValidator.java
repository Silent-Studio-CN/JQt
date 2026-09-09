/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 浮点范围校验器（Qt {@code QDoubleValidator}）。
 * <pre>
 * QLineEdit edit = new QLineEdit();
 * edit.setValidator(new QDoubleValidator(0.0, 1.0, 2));
 * </pre>
 */
public class QDoubleValidator extends QValidator {

    /** 创建浮点校验器（范围含边界，decimals 为小数位数）。 */
    public QDoubleValidator(double bottom, double top, int decimals) {
        nativeHandle = nativeCreate(bottom, top, decimals);
        registerCleaner();
    }

    private native long nativeCreate(double bottom, double top, int decimals);
}
