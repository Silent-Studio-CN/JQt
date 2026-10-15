/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 转换基类（Qt {@code QAbstractTransition}）。
 * <p><b>判定与动作</b>（与 Qt 同名同义）：
 * <ul>
 *   <li>{@link #eventTest(QEvent)} —— 这个事件是否触发本转换（也可当"守卫"用）；</li>
 *   <li>{@link #onTransition(QEvent)} —— 转换真正发生时的动作。</li>
 * </ul>
 * <b>JQt 扩展</b>：{@link #trigger()} 可手动触发一次转换 ——
 * Java 侧没有 Qt 的"信号名字符串"机制，所以信号驱动推荐这样接：
 * <pre>
 * QAbstractTransition t = new QEventTransition().setTargetState(next);
 * timer.onTimeout(t::trigger);          // 任意已有回调都能当信号源
 * </pre>
 */
public abstract class QAbstractTransition {

    private QState sourceState;
    private final List<QAbstractState> targets = new ArrayList<>();
    private final List<Runnable> triggeredHandlers = new ArrayList<>();

    /** 源状态（由 {@link QState#addTransition} 自动设置）。 */
    public QState sourceState() { return sourceState; }

    /** 目标状态列表（只读）。 */
    public List<QAbstractState> targetStates() {
        return Collections.unmodifiableList(targets);
    }

    /** 设置唯一目标状态。 */
    public QAbstractTransition setTargetState(QAbstractState target) {
        targets.clear();
        if (target != null) targets.add(target);
        return this;
    }

    /** 设置多个目标状态（并行进入）。 */
    public QAbstractTransition setTargetStates(QAbstractState... more) {
        targets.clear();
        if (more != null) {
            for (QAbstractState s : more) if (s != null) targets.add(s);
        }
        return this;
    }

    /** 追加目标状态。 */
    public QAbstractTransition addTargetState(QAbstractState target) {
        if (target != null) targets.add(target);
        return this;
    }

    /** 转换发生时的回调（可注册多个；在 {@link #onTransition(QEvent)} 之后执行）。 */
    public QAbstractTransition onTriggered(Runnable handler) {
        if (handler != null) triggeredHandlers.add(handler);
        return this;
    }

    /**
     * 手动触发一次转换（JQt 扩展）。
     * <p>等价于"向状态机投递一个必然匹配本转换的事件"：仅当源状态当前处于活动状态时才生效。
     * 需要携带事件时用 {@link QStateMachine#postEvent(QEvent)}。
     */
    public void trigger() {
        if (sourceState != null && sourceState.machine() != null) {
            sourceState.machine().postEvent(new QEvent(QEvent.Type.StateMachineSignal, "StateMachineSignal",
                    0, 0, 0, 0, ""));
            return;
        }
        // 没挂到状态机上：退化为直接执行（便于单元级使用）
        if (!targets.isEmpty()) runTransition(null);
    }

    /** 子类覆盖：事件是否触发本转换（默认不触发）。 */
    protected boolean eventTest(QEvent event) { return false; }

    /** 子类覆盖：转换动作（默认什么都不做）。 */
    protected void onTransition(QEvent event) {}

    // ------------------------------------------------------------------ 内部
    final void setSourceStateInternal(QState s) { this.sourceState = s; }

    final boolean matches(QEvent event) { return eventTest(event); }

    final void runTransition(QEvent event) {
        onTransition(event);
        for (Runnable r : triggeredHandlers) r.run();
    }

    @Override public String toString() {
        return getClass().getSimpleName() + "(" + (sourceState == null ? "-" : sourceState.id())
                + " -> " + (targets.isEmpty() ? "-" : Arrays.toString(targets.stream().map(QAbstractState::id).toArray())) + ")";
    }
}

/**
 * 无条件转换（包内可见）：任意事件都触发 —— 对应 Qt 的
 * {@code QState::addTransition(QAbstractState *target)}。
 */
final class QAnyTransition extends QAbstractTransition {
    @Override protected boolean eventTest(QEvent event) { return event != null; }
}
