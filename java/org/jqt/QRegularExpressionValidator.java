/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 正则校验器（Qt {@code QRegularExpressionValidator}；整串匹配语义）。
 * <pre>
 * QLineEdit edit = new QLineEdit();
 * edit.setValidator(new QRegularExpressionValidator("[0-9]{4}"));
 * </pre>
 */
public class QRegularExpressionValidator extends QValidator {

    /** 创建正则校验器（pattern 为 Qt 正则语法，整串匹配）。 */
    public QRegularExpressionValidator(String pattern) {
        nativeHandle = nativeCreate(pattern != null ? pattern : "");
        registerCleaner();
    }

    private native long nativeCreate(String pattern);
}
