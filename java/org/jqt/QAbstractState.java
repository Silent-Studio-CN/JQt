/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.ArrayList;
import java.util.List;

/**
 * 状态基类（Qt {@code QAbstractState}）。
 * <p><b>实现说明</b>：Qt 官方**未分发** {@code QtStateMachine} 模块（6.8.3/6.11/6.12 在
 * mingw/MSVC/Linux/macOS 的官方模块清单里都查无此模块，见 docs/compare-qtjambi.md），
 * 因此 JQt 这一族类是按 Qt 文档语义**自研实现**的纯 Java 版本：零原生依赖、五平台一致、
 * 便于确定性测试。与 Qt 的差异见 {@link QStateMachine} 与 {@link QAbstractTransition} 的说明。
 */
public abstract class QAbstractState {

    private final String id;
    private QState parent;
    private QStateMachine machine;
    private boolean active;

    private final List<Runnable> enteredHandlers = new ArrayList<>();
    private final List<Runnable> exitedHandlers = new ArrayList<>();

    protected QAbstractState() { this(null); }

    protected QAbstractState(String id) {
        this.id = (id == null || id.isEmpty()) ? null : id;
    }

    /** 状态标识；未指定时给一个可读的默认值（日志/断言用）。 */
    public String id() {
        return id != null ? id
                : getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(this));
    }

    /** 父状态（顶层状态返回 {@code null}）。 */
    public QState parentState() { return parent; }

    /** 所属状态机（未加入任何状态机时返回 {@code null}）。 */
    public QStateMachine machine() { return machine; }

    /** 当前是否处于活动状态。 */
    public boolean isActive() { return active; }

    /** 进入该状态时的回调（可注册多个）。 */
    public QAbstractState onEntered(Runnable handler) {
        if (handler != null) enteredHandlers.add(handler);
        return this;
    }

    /** 退出该状态时的回调（可注册多个）。 */
    public QAbstractState onExited(Runnable handler) {
        if (handler != null) exitedHandlers.add(handler);
        return this;
    }

    /** 子类可覆盖：进入钩子（在 onEntered 回调之前执行）。 */
    protected void onEntry() {}

    /** 子类可覆盖：退出钩子（在 onExited 回调之前执行）。 */
    protected void onExit() {}

    // ------------------------------------------------------------------ 内部
    final void setParentStateInternal(QState p) { this.parent = p; }

    /** 把状态机引用向下传播（QState 覆盖以递归到子状态）。 */
    void propagateMachine(QStateMachine m) { this.machine = m; }

    final void markActive(boolean a) { this.active = a; }

    final void fireEntered() {
        onEntry();
        for (Runnable r : enteredHandlers) r.run();
    }

    final void fireExited() {
        onExit();
        for (Runnable r : exitedHandlers) r.run();
    }

    /** 深度：顶层状态为 0（状态机自身不计入）。 */
    final int depth() {
        int d = 0;
        QState p = parent;
        while (p != null) {
            if (!(p instanceof QStateMachine)) d++;
            p = p.parentState();
        }
        return d;
    }

    @Override public String toString() { return id(); }
}
