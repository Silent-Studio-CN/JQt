/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 整数范围校验器（Qt {@code QIntValidator}）。
 * <pre>
 * QLineEdit edit = new QLineEdit();
 * edit.setValidator(new QIntValidator(0, 100));
 * </pre>
 */
public class QIntValidator extends QValidator {

    /** 创建整数校验器（含边界）。 */
    public QIntValidator(int bottom, int top) {
        nativeHandle = nativeCreate(bottom, top);
        registerCleaner();
    }

    private native long nativeCreate(int bottom, int top);
}
