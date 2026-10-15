/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 状态机（Qt {@code QStateMachine}，P1 补齐项）。
 *
 * <pre>
 * QStateMachine m = new QStateMachine();
 * QState idle = new QState(m, "idle");
 * QState busy = new QState(m, "busy");
 * QFinalState done = new QFinalState(m, "done");
 *
 * idle.addTransition(new QEventTransition(QEvent.Type.User + 1).setTargetState(busy));
 * busy.addTransition(new QEventTransition(QEvent.Type.User + 2).setTargetState(done));
 * m.setInitialState(idle);
 * m.onFinished(() -&gt; System.out.println("结束"));
 * m.start();
 * m.postEvent(new QEvent(QEvent.Type.User + 1));
 * </pre>
 *
 * <h3>与 Qt 的差异（三处，都是刻意的）</h3>
 * <ol>
 *   <li><b>自研纯 Java 实现</b>：Qt 官方未分发 {@code QtStateMachine} 模块
 *       （6.8.3 / 6.11 / 6.12 在 mingw、MSVC、Linux、macOS 的官方模块清单里都没有）。
 *       本实现按 Qt 文档语义编写，零原生依赖，五平台行为一致。</li>
 *   <li><b>事件同步处理</b>：{@link #postEvent(QEvent)} 立即在当前线程完成微步，
 *       不依赖事件循环（Qt 需要 {@code exec()} 才会处理队列）。确定性更好，
 *       代价是"投递即执行"——不要在一个回调里递归投递大量事件。</li>
 *   <li><b>信号驱动方式</b>：Java 侧没有 Qt 的"信号名字符串"，改用
 *       {@link QAbstractTransition#trigger()} 接任意回调（见该类说明）。</li>
 * </ol>
 * <b>线程</b>：{@link #postEvent(QEvent)} 内部加锁，可从任意线程调用（与 Qt 一致）；
 * 其余方法（{@code start}/{@code stop}/配置）请在单线程内完成。
 */
public class QStateMachine extends QState {

    private final Object lock = new Object();
    private final ArrayDeque<QEvent> queue = new ArrayDeque<>();
    private final Set<QAbstractState> configuration = new LinkedHashSet<>();
    private final Map<QState, Set<QAbstractState>> exitSnapshot = new IdentityHashMap<>();

    private final List<Runnable> startedHandlers = new ArrayList<>();
    private final List<Runnable> stoppedHandlers = new ArrayList<>();
    private final List<Runnable> errorHandlers = new ArrayList<>();

    private boolean running;
    private boolean errorFlag;
    private boolean finishedPending;

    public QStateMachine() {
        super();
        propagateMachine(this);
    }

    public QStateMachine(String id) {
        super(id);
        propagateMachine(this);
    }

    // ------------------------------------------------------------------ 配置
    @Override public QStateMachine addState(QAbstractState child) {
        super.addState(child);
        return this;
    }

    @Override public QStateMachine setInitialState(QAbstractState state) {
        super.setInitialState(state);
        return this;
    }

    @Override public QStateMachine setChildMode(ChildMode mode) {
        super.setChildMode(mode);
        return this;
    }

    /** 机器是否运行中。 */
    public boolean isRunning() { return running; }

    /** 当前活动状态集合（进入顺序；不包含状态机自身）。 */
    public List<QAbstractState> configuration() {
        return Collections.unmodifiableList(new ArrayList<>(configuration));
    }

    /** 是否处于错误状态（事件无人处理且配置了 errorState）。 */
    public boolean hasError() { return errorFlag; }

    /** 清除错误标记（并退出错误状态，如果它还在活动集合里）。 */
    public void clearError() {
        errorFlag = false;
        QAbstractState es = errorState();
        if (es != null && es.isActive()) exitState(es);
    }

    // ------------------------------------------------------------------ 生命周期
    /** 启动：进入初始配置，然后发出 started。 */
    public void start() {
        if (running) return;
        running = true;
        errorFlag = false;
        synchronized (lock) {
            enterChildrenOf(this, true);
            fireStarted();
            if (finishedPending) { finishedPending = false; finishNow(); }
        }
    }

    /** 停止：退出所有活动状态，然后发出 stopped。 */
    public void stop() {
        if (!running) return;
        synchronized (lock) {
            exitAll();
            running = false;
        }
        fireStopped();
    }

    /**
     * 投递事件并立即处理（投递即执行，见类说明 2）。
     * <p>机器未运行时忽略该事件。
     */
    public void postEvent(QEvent event) {
        if (event == null) return;
        synchronized (lock) {
            if (!running) return;
            queue.addLast(event);
            while (!queue.isEmpty()) {
                QEvent e = queue.pollFirst();
                boolean handled = microstep(e);
                if (!handled) {
                    QAbstractState es = errorState();
                    if (es != null) {
                        errorFlag = true;
                        enterState(es, true);
                        fireError();
                    }
                }
                if (finishedPending) {
                    finishedPending = false;
                    finishNow();
                    return;
                }
            }
        }
    }

    // ------------------------------------------------------------------ 信号
    public QStateMachine onStarted(Runnable handler) {
        if (handler != null) startedHandlers.add(handler);
        return this;
    }

    public QStateMachine onStopped(Runnable handler) {
        if (handler != null) stoppedHandlers.add(handler);
        return this;
    }

    /** 机器结束（顶层终态被进入）时的回调。 */
    @Override public QStateMachine onFinished(Runnable handler) {
        super.onFinished(handler);
        return this;
    }

    public QStateMachine onError(Runnable handler) {
        if (handler != null) errorHandlers.add(handler);
        return this;
    }

    @Override public QStateMachine onEntered(Runnable handler) { super.onEntered(handler); return this; }

    @Override public QStateMachine onExited(Runnable handler) { super.onExited(handler); return this; }

    private void fireStarted() { for (Runnable r : startedHandlers) r.run(); }

    private void fireStopped() { for (Runnable r : stoppedHandlers) r.run(); }

    private void fireError() { for (Runnable r : errorHandlers) r.run(); }

    /** 顶层终态：先发 finished，再停止（Qt 的 finished -> stopped 顺序）。 */
    private void finishNow() {
        fireFinished();
        stop();
    }

    // ------------------------------------------------------------------ 微步
    private boolean microstep(QEvent event) {
        QAbstractTransition t = selectTransition(event);
        if (t == null) return false;

        QState source = t.sourceState() != null ? t.sourceState() : this;
        List<QAbstractState> targets = t.targetStates();

        Set<QAbstractState> keep = new LinkedHashSet<>();
        keep.add(this);
        for (QAbstractState tg : targets) {
            keep.add(tg);
            QState p = tg.parentState();
            while (p != null) { keep.add(p); p = p.parentState(); }
        }
        // 自转换（目标就是源）：源自身也要退出后重进，Qt 会重新发 entered
        if (targets.contains(source)) {
            keep.removeIf(a -> isSelfOrDescendantOf(a, source));
        }

        List<QAbstractState> exits = new ArrayList<>();
        for (QAbstractState a : configuration) {
            // 需要退出的是:源状态本身、源的后代,以及**源的祖先**（当目标不在该祖先链上时）
            boolean involvesSource = isSelfOrDescendantOf(a, source) || isSelfOrDescendantOf(source, a);
            if (involvesSource && !keep.contains(a)) exits.add(a);
        }
        // 历史记录必须在退出**之前**取快照:记的是"父状态退出那一刻的活动配置",
        // 而不是"曾经活动过的所有子状态"(否则浅历史恢复会把新旧状态一起进)。
        for (QAbstractState ex : exits) {
            if (!(ex instanceof QState)) continue;
            Set<QAbstractState> snap = new LinkedHashSet<>();
            for (QAbstractState a : configuration) {
                if (a != ex && isSelfOrDescendantOf(a, ex)) snap.add(a);
            }
            exitSnapshot.put((QState) ex, snap);
        }
        exits.sort((x, y) -> y.depth() - x.depth());
        for (QAbstractState a : exits) exitState(a);

        t.runTransition(event);

        for (QAbstractState tg : targets) enterState(tg, true);
        return true;
    }

    /** 选转换：活动状态（深的优先）→ 其祖先 → 状态机自身；同一状态内按声明顺序。 */
    private QAbstractTransition selectTransition(QEvent event) {
        List<QAbstractState> candidates = new ArrayList<>(configuration);
        for (QAbstractState a : configuration) {
            QState p = a.parentState();
            while (p != null) {
                if (!candidates.contains(p)) candidates.add(p);
                p = p.parentState();
            }
        }
        if (!candidates.contains(this)) candidates.add(this);
        candidates.sort((x, y) -> y.depth() - x.depth());
        for (QAbstractState s : candidates) {
            if (!(s instanceof QState)) continue;
            for (QAbstractTransition t : ((QState) s).transitions()) {
                if (t.matches(event)) return t;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ 进入/退出
    private void enterState(QAbstractState s, boolean initChildren) {
        if (s == null) return;
        if (s instanceof QHistoryState) {
            resolveHistory((QHistoryState) s);
            return;
        }
        if (s.isActive()) return;
        for (QAbstractState a : ancestorsFromRoot(s)) {
            if (!a.isActive()) enterSingle(a);
        }
        enterSingle(s);
        if (initChildren && s instanceof QState) enterChildrenOf((QState) s, false);
    }

    private void enterChildrenOf(QState q, boolean initialEntry) {
        if (q.childMode() == ChildMode.Parallel) {
            for (QAbstractState c : q.childStates()) enterState(c, true);
        } else if (q.initialState() != null) {
            enterState(q.initialState(), true);
        } else if (initialEntry && !q.childStates().isEmpty()) {
            // 未显式设置初始状态：进入第一个子状态（比静默什么都不进更好排查）
            enterState(q.childStates().get(0), true);
        }
    }

    private void enterSingle(QAbstractState s) {
        configuration.add(s);
        s.markActive(true);
        s.fireEntered();
        if (s instanceof QFinalState) onFinalEntered((QFinalState) s);
    }

    private void exitState(QAbstractState s) {
        if (!s.isActive()) return;
        configuration.remove(s);
        s.markActive(false);
        s.fireExited();
    }

    private void exitAll() {
        List<QAbstractState> all = new ArrayList<>(configuration);
        all.sort((x, y) -> y.depth() - x.depth());
        for (QAbstractState a : all) exitState(a);
        configuration.clear();
    }

    /** 终态被进入：父状态发出 finished；顶层终态则整个机器结束。并行状态需"所有子区域都完成"。 */
    private void onFinalEntered(QFinalState f) {
        QState p = f.parentState();
        if (p == null) { finishedPending = true; return; }
        if (p.childMode() == ChildMode.Parallel && !allRegionsFinished(p)) {
            return;                       // 并行状态:还有区域没到终态
        }
        if (p == this) { finishedPending = true; return; }
        p.fireFinished();
        propagateCompletion(p);
    }

    /**
     * 完成事件向上传播：并行父状态要等所有区域完成才完成，之后继续向上；
     * 独占父状态不会因子状态完成而完成（Qt 语义）。
     */
    private void propagateCompletion(QState s) {
        QState p = s.parentState();
        while (p != null) {
            if (p.childMode() != ChildMode.Parallel) return;
            if (!allRegionsFinished(p)) return;
            if (p == this) { finishedPending = true; return; }
            if (p.finishedFlag()) return;
            p.fireFinished();
            p = p.parentState();
        }
    }

    /** 并行状态是否所有子区域都已完成（子状态本身是终态，或它自己已 finished）。 */
    private boolean allRegionsFinished(QState parallel) {
        for (QAbstractState c : parallel.childStates()) {
            if (c instanceof QFinalState) {
                if (!c.isActive()) return false;
            } else if (c instanceof QState) {
                if (!((QState) c).finishedFlag()) return false;
            } else {
                return false;
            }
        }
        return true;
    }

    private List<QAbstractState> ancestorsFromRoot(QAbstractState s) {
        List<QAbstractState> chain = new ArrayList<>();
        QState p = s.parentState();
        while (p != null) {
            if (!(p instanceof QStateMachine)) chain.add(0, p);
            p = p.parentState();
        }
        return chain;
    }

    private boolean isSelfOrDescendantOf(QAbstractState a, QAbstractState ancestor) {
        QAbstractState cur = a;
        while (cur != null) {
            if (cur == ancestor) return true;
            cur = cur.parentState();
        }
        return false;
    }

    // ------------------------------------------------------------------ 历史
    private void resolveHistory(QHistoryState h) {
        QState parent = h.parentState();
        if (parent == null) return;
        Set<QAbstractState> snapshot = exitSnapshot.get(parent);
        List<QAbstractState> restore = new ArrayList<>();
        if (snapshot != null) {
            if (h.historyType() == QHistoryState.HistoryType.Shallow) {
                for (QAbstractState a : snapshot) if (a.parentState() == parent) restore.add(a);
            } else {
                for (QAbstractState a : snapshot) if (isSelfOrDescendantOf(a, parent)) restore.add(a);
            }
        }
        if (restore.isEmpty()) {
            QAbstractState def = h.defaultState() != null ? h.defaultState() : parent.initialState();
            if (def != null && def != h) enterState(def, true);
            return;
        }
        restore.sort((x, y) -> x.depth() - y.depth());
        for (QAbstractState a : restore) {
            if (!(a instanceof QHistoryState)) enterState(a, true);
        }
    }
}
