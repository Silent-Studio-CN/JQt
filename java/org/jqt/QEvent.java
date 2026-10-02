/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 事件（v1.9.1 P0-②）：控件收到的事件描述。
 * <p>用法：
 * <pre>
 * win.onEvent(e -&gt; {
 *     if (e.type() == QEvent.Type.KeyPress) System.out.println("键:" + e.key() + " 文本:" + e.text());
 *     if (e.type() == QEvent.Type.MouseButtonPress) System.out.println("点击 " + e.x() + "," + e.y());
 * });
 * </pre>
 * <b>线程</b>：事件在 Qt 主线程投递，回调也在主线程执行。
 * <p>典型事件类型见 {@link Type}；未覆盖的类型只保证 {@link #type()} 与 {@link #typeName()} 可用。
 */
public final class QEvent {

    /** 事件类型（Qt {@code QEvent::Type} 子集，与 Qt 数值一致）。 */
    public static final class Type {
        private Type() {}
        public static final int None = 0;
        public static final int Timer = 1;
        public static final int MouseButtonPress = 2;
        public static final int MouseButtonRelease = 3;
        public static final int MouseButtonDblClick = 4;
        public static final int MouseMove = 5;
        public static final int KeyPress = 6;
        public static final int KeyRelease = 7;
        public static final int FocusIn = 8;
        public static final int FocusOut = 9;
        public static final int Enter = 10;
        public static final int Leave = 11;
        public static final int Paint = 12;
        public static final int Move = 13;
        public static final int Resize = 14;
        public static final int Show = 17;
        public static final int Hide = 18;
        public static final int Close = 19;
        public static final int Wheel = 31;
        public static final int EnabledChange = 98;
        public static final int WindowTitleChange = 105;
        public static final int WindowIconChange = 101;
    }

    private final int type;
    private final String typeName;
    private final int a, b, c, d;          // 类型相关负载(坐标/按键/尺寸/滚轮)
    private final String text;

    QEvent(int type, String typeName, int a, int b, int c, int d, String text) {
        this.type = type;
        this.typeName = typeName;
        this.a = a; this.b = b; this.c = c; this.d = d;
        this.text = text == null ? "" : text;
    }

    /** 事件类型数值（{@link Type}）。 */
    public int type() { return type; }

    /** Qt 事件类型名（如 {@code "KeyPress"}），便于日志与未知类型排查。 */
    public String typeName() { return typeName; }

    /** 鼠标/滚轮:相对控件的 X；键事件:X 无意义。 */
    public int x() { return a; }
    /** 鼠标/滚轮:相对控件的 Y。 */
    public int y() { return b; }

    /** 键盘按键码（{@link QKeySequence} / Qt::Key 数值）。 */
    public int key() { return b; }
    /** 键盘修饰键（Qt::KeyboardModifier 位组合）。 */
    public int modifiers() { return c; }
    /** 键盘输入文本（可打印键或输入法提交的文本）。 */
    public String text() { return text; }

    /** 缩放/resize:新宽度（{@link Type#Resize}）或滚轮角度增量（{@link Type#Wheel}）。 */
    public int width() { return a; }
    /** resize:新高度。 */
    public int height() { return b; }
    /** 滚轮:垂直增量（1/8 度为单位，正数向前）。 */
    public int wheelDelta() { return b; }
    /** 鼠标:按键位组合（Qt::MouseButton）。 */
    public int buttons() { return c; }

    @Override public String toString() {
        return "QEvent(" + typeName + " a=" + a + " b=" + b + " c=" + c + " d=" + d
             + (text.isEmpty() ? "" : " text=" + text) + ")";
    }
}
