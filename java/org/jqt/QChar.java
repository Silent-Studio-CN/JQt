/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 字符（Qt {@code QChar}，纯 Java 实现）。
 */
public class QChar {

    private final char value;

    public QChar() { this((char) 0); }
    public QChar(char c) { this.value = c; }
    public QChar(int code) { this.value = (char) code; }
    public QChar(String s) { this.value = s != null && !s.isEmpty() ? s.charAt(0) : (char) 0; }

    public char toLatin1() { return value; }
    public char unicode() { return value; }
    public int unicodeValue() { return value; }

    public boolean isNull() { return value == 0; }
    public boolean isLetter() { return Character.isLetter(value); }
    public boolean isDigit() { return Character.isDigit(value); }
    public boolean isLetterOrNumber() { return Character.isLetterOrDigit(value); }
    public boolean isNumber() { return Character.isDigit(value); }
    public boolean isSpace() { return Character.isWhitespace(value); }
    public boolean isUpper() { return Character.isUpperCase(value); }
    public boolean isLower() { return Character.isLowerCase(value); }
    public boolean isPunct() { return !Character.isLetterOrDigit(value) && !Character.isWhitespace(value) && value != 0; }
    public boolean isSymbol() { return Character.isLetterOrDigit(value) ? false : Character.isDefined(value) && !Character.isWhitespace(value) && !isPunct(); }

    public QChar toUpper() { return new QChar(Character.toUpperCase(value)); }
    public QChar toLower() { return new QChar(Character.toLowerCase(value)); }

    /** 十进制数值（'0'-'9' → 0-9；其他返回 -1）。 */
    public int digitValue() {
        return Character.isDigit(value) ? Character.digit(value, 10) : -1;
    }

    public String toString() { return String.valueOf(value); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QChar)) return false;
        return value == ((QChar) o).value;
    }

    @Override
    public int hashCode() { return value; }
}
