/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 按事件类型匹配的转换（Qt {@code QEventTransition}）。
 * <pre>
 * // 收到自定义事件 User+1 时从 s1 转到 s2
 * s1.addTransition(new QEventTransition(QEvent.Type.User + 1).setTargetState(s2));
 * machine.postEvent(new QEvent(QEvent.Type.User + 1));
 * </pre>
 * <b>与 Qt 的差异</b>：Qt 的 {@code QEventTransition(QObject*, QEvent::Type)} 还按发送者对象过滤；
 * JQt 没有 QObject 层，只按事件类型匹配（需要更细的判定就继承 {@link QAbstractTransition}
 * 覆盖 {@link #eventTest(QEvent)}）。
 */
public class QEventTransition extends QAbstractTransition {

    private final int eventType;
    private final boolean anyEvent;

    /** 匹配任意事件（等价于无条件转换；也可只靠 {@link #trigger()} 手动触发）。 */
    public QEventTransition() {
        this.eventType = 0;
        this.anyEvent = true;
    }

    /** 匹配指定事件类型（{@link QEvent.Type}）。 */
    public QEventTransition(int eventType) {
        this.eventType = eventType;
        this.anyEvent = false;
    }

    /** 匹配到的事件类型；任意事件时为 0。 */
    public int eventType() { return eventType; }

    @Override protected boolean eventTest(QEvent event) {
        if (event == null) return false;
        return anyEvent || event.type() == eventType;
    }

    @Override public String toString() {
        return "QEventTransition(" + (anyEvent ? "any" : "type=" + eventType) + " -> "
                + (targetStates().isEmpty() ? "-" : targetStates().get(0).id()) + ")";
    }
}
