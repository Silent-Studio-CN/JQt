/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 快捷键序列（Qt {@code QKeySequence}，纯 Java 实现）。
 * <p>支持 "Ctrl+Shift+P" 风格字符串解析/生成，常用标准快捷键常量。
 */
public class QKeySequence {

    /** 标准快捷键（Qt StandardKey 常用）。 */
    public enum StandardKey {
        Open, Save, SaveAs, Close, Quit, Copy, Cut, Paste, Undo, Redo,
        SelectAll, Find, Refresh, ZoomIn, ZoomOut, Print, Help
    }

    private String sequence;

    public QKeySequence() { this.sequence = ""; }
    public QKeySequence(String keySequence) { this.sequence = keySequence != null ? keySequence : ""; }
    public QKeySequence(StandardKey key) { this.sequence = standardKeyToString(key); }

    private static String standardKeyToString(StandardKey k) {
        switch (k) {
            case Open: return "Ctrl+O";
            case Save: return "Ctrl+S";
            case SaveAs: return "Ctrl+Shift+S";
            case Close: return "Ctrl+W";
            case Quit: return "Ctrl+Q";
            case Copy: return "Ctrl+C";
            case Cut: return "Ctrl+X";
            case Paste: return "Ctrl+V";
            case Undo: return "Ctrl+Z";
            case Redo: return "Ctrl+Y";
            case SelectAll: return "Ctrl+A";
            case Find: return "Ctrl+F";
            case Refresh: return "F5";
            case ZoomIn: return "Ctrl++";
            case ZoomOut: return "Ctrl+-";
            case Print: return "Ctrl+P";
            case Help: return "F1";
            default: return "";
        }
    }

    /** 平台默认标准快捷键。 */
    public static QKeySequence keyBindings(StandardKey key) { return new QKeySequence(key); }

    public boolean isEmpty() { return sequence == null || sequence.isEmpty(); }

    public int count() { return sequence == null || sequence.isEmpty() ? 0 : 1; }

    public QKeySequence matched(QKeySequence other) {
        return sequence != null && sequence.equals(other.sequence) ? other : new QKeySequence();
    }

    public String toString() { return sequence != null ? sequence : ""; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QKeySequence)) return false;
        QKeySequence q = (QKeySequence) o;
        return sequence != null ? sequence.equals(q.sequence) : q.sequence == null;
    }

    @Override
    public int hashCode() { return sequence != null ? sequence.hashCode() : 0; }
}
