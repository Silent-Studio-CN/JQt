/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 历史状态（Qt {@code QHistoryState}）：重新进入复合状态时，恢复到上次退出时的子状态。
 * <pre>
 * QState paused = new QState(machine, "paused");
 * QHistoryState h = new QHistoryState(paused);      // 记住 paused 里最后活动的子状态
 * h.setDefaultState(pausedInitial);                 // 从未进入过时用哪个
 * </pre>
 * {@link HistoryType#Shallow} 只恢复直接子状态；{@link HistoryType#Deep} 递归恢复整棵子树。
 */
public class QHistoryState extends QAbstractState {

    /** 历史类型（Qt {@code QHistoryState::HistoryType}）。 */
    public enum HistoryType { Shallow, Deep }

    private HistoryType historyType = HistoryType.Shallow;
    private QAbstractState defaultState;

    public QHistoryState() { super(); }

    public QHistoryState(String id) { super(id); }

    /** 建好即挂到父状态（历史状态必须是复合状态的子状态）。 */
    public QHistoryState(QState parent) {
        super();
        if (parent != null) parent.addState(this);
    }

    public QHistoryState(QState parent, String id) {
        super(id);
        if (parent != null) parent.addState(this);
    }

    public QHistoryState setHistoryType(HistoryType type) {
        this.historyType = type == null ? HistoryType.Shallow : type;
        return this;
    }

    public HistoryType historyType() { return historyType; }

    /** 从未进入过父状态时使用的默认子状态。 */
    public QHistoryState setDefaultState(QAbstractState state) {
        this.defaultState = state;
        if (state != null && parentState() != null && !parentState().childStates().contains(state)) {
            parentState().addState(state);
        }
        return this;
    }

    public QAbstractState defaultState() { return defaultState; }

    @Override public QHistoryState onEntered(Runnable handler) { super.onEntered(handler); return this; }

    @Override public QHistoryState onExited(Runnable handler) { super.onExited(handler); return this; }
}
