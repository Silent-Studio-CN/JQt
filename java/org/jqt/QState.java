/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 复合状态（Qt {@code QState}）：可包含子状态与出边转换。
 * <pre>
 * QState s1 = new QState(machine, "s1");
 * QState s2 = new QState(machine, "s2");
 * s1.addTransition(new QEventTransition(QEvent.Type.User + 1).setTargetState(s2));
 * machine.setInitialState(s1);
 * </pre>
 * <b>子状态模式</b>：默认 {@link ChildMode#Exclusive}（进入初始子状态）；{@link ChildMode#Parallel}
 * 会同时进入全部子状态（任一子状态到达终态即视为该并行状态完成）。
 */
public class QState extends QAbstractState {

    /** 子状态模式（Qt {@code QState::ChildMode}）。 */
    public enum ChildMode { Exclusive, Parallel }

    private final List<QAbstractState> children = new ArrayList<>();
    private final List<QAbstractTransition> transitions = new ArrayList<>();
    private final List<Runnable> finishedHandlers = new ArrayList<>();

    private QAbstractState initialState;
    private QAbstractState errorState;
    private ChildMode childMode = ChildMode.Exclusive;
    private boolean finishedFlag;

    public QState() { super(); }

    public QState(String id) { super(id); }

    /** 建好即挂到父状态上（Qt 的 {@code QState(QState *parent)}）。 */
    public QState(QState parent) {
        super();
        if (parent != null) parent.addState(this);
    }

    public QState(QState parent, String id) {
        super(id);
        if (parent != null) parent.addState(this);
    }

    /** 添加子状态。 */
    public QState addState(QAbstractState child) {
        if (child == null) return this;
        if (child == this) throw new IllegalArgumentException("状态不能把自己作为子状态");
        if (children.contains(child)) return this;
        children.add(child);
        child.setParentStateInternal(this);
        child.propagateMachine(machine());
        return this;
    }

    /** 子状态列表（只读）。 */
    public List<QAbstractState> childStates() {
        return Collections.unmodifiableList(children);
    }

    /** 初始子状态（{@link ChildMode#Exclusive} 下进入该状态）。 */
    public QState setInitialState(QAbstractState state) {
        this.initialState = state;
        if (state != null && !children.contains(state)) addState(state);
        return this;
    }

    public QAbstractState initialState() { return initialState; }

    /** 子状态模式。 */
    public QState setChildMode(ChildMode mode) {
        this.childMode = mode == null ? ChildMode.Exclusive : mode;
        return this;
    }

    public ChildMode childMode() { return childMode; }

    /** 错误状态：事件无人处理且机器未处理该事件时进入（Qt {@code QState::errorState}）。 */
    public QState setErrorState(QAbstractState state) {
        this.errorState = state;
        if (state != null && !children.contains(state)) addState(state);
        return this;
    }

    public QAbstractState errorState() { return errorState; }

    /** 添加一条出边转换（转换的源状态即本状态）。 */
    public QState addTransition(QAbstractTransition transition) {
        if (transition == null) return this;
        transitions.add(transition);
        transition.setSourceStateInternal(this);
        return this;
    }

    /**
     * 无条件转换（Qt {@code QState::addTransition(QAbstractState *target)}）：
     * 任意事件都会触发，因此通常用作"完成即转"的兜底。
     */
    public QState addTransition(QAbstractState target) {
        return addTransition(new QAnyTransition().setTargetState(target));
    }

    /**
     * 便捷重载（JQt 扩展）：按事件类型转换。
     * <p>等价于 {@code addTransition(new QEventTransition(type).setTargetState(target))}。
     */
    public QState addTransition(int eventType, QAbstractState target) {
        return addTransition(new QEventTransition(eventType).setTargetState(target));
    }

    /** 出边转换列表（只读，声明顺序即优先级）。 */
    public List<QAbstractTransition> transitions() {
        return Collections.unmodifiableList(transitions);
    }

    /**
     * 本状态"完成"时的回调：子状态进入 {@link QFinalState} 时触发；
     * 若本状态是状态机的顶层子状态，机器会随之结束并停止。
     */
    public QState onFinished(Runnable handler) {
        if (handler != null) finishedHandlers.add(handler);
        return this;
    }

    @Override public QState onEntered(Runnable handler) { super.onEntered(handler); return this; }

    @Override public QState onExited(Runnable handler) { super.onExited(handler); return this; }

    // ------------------------------------------------------------------ 内部
    @Override void propagateMachine(QStateMachine m) {
        super.propagateMachine(m);
        for (QAbstractState c : children) c.propagateMachine(m);
    }

    /** 由状态机在子状态进入终态时调用。 */
    final void fireFinished() {
        finishedFlag = true;
        for (Runnable r : finishedHandlers) r.run();
    }

    /** 是否已完成（子状态进入过终态；并行语义下由状态机判定）。 */
    public boolean isFinished() { return finishedFlag; }

    /** 并行模式判定用：本状态是否已被标记完成。 */
    final boolean finishedFlag() { return finishedFlag; }
}
