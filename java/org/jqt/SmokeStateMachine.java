/*
 * JQt - QStateMachine 冒烟（v1.9.2 P1 补齐）。
 *
 * 背景：Qt 官方未分发 QtStateMachine 模块（6.8.3/6.11/6.12 各平台官方模块清单里都查不到），
 * 因此这一族是按 Qt 文档语义自研的纯 Java 实现。本冒烟逐条验证语义，不需要事件循环。
 *
 * 断言点：
 *   1. 基本转换：start → 初始状态；postEvent → 退出源/进入目标；未运行时投递被忽略
 *   2. 顶层终态 → finished + stopped + 机器自动停止
 *   3. 嵌套：进入顺序父先子后；退出顺序子先父后
 *   4. 离开复合状态：先退内层再退外层，再进目标
 *   5. 嵌套终态：父状态 finished，机器仍在运行；无条件转换随后收尾
 *   6. 并行状态：两个区域同时进入；区域未全完成不算完成；全部完成才算
 *   7. 历史状态：恢复上次活动的子状态；从未进入时走默认状态
 *   8. 错误状态：事件无人处理 → 进 errorState；clearError() 复位
 *   9. JQt 扩展 trigger()：接任意回调当信号源（含真实 QTimer 信号）
 *  10. 跨线程 postEvent 安全
 */
package org.jqt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class SmokeStateMachine {

    static int pass = 0;
    static int fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("[sm] OK   " + name); }
        else { fail++; System.out.println("[sm] FAIL " + name); }
    }

    /** 当前活动状态（诊断用）。 */
    static String ids(QStateMachine m) {
        StringBuilder sb = new StringBuilder();
        for (QAbstractState s : m.configuration()) {
            if (sb.length() > 0) sb.append(",");
            sb.append(s.id());
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("[sm] start");

        // ---------------------------------------------------------- 1) 基本转换
        QStateMachine m1 = new QStateMachine("m1");
        QState idle = new QState(m1, "idle");
        QState busy = new QState(m1, "busy");
        QFinalState doneTop = new QFinalState(m1, "done");
        List<String> log = Collections.synchronizedList(new ArrayList<>());
        idle.onEntered(() -> log.add("+idle")).onExited(() -> log.add("-idle"));
        busy.onEntered(() -> log.add("+busy"));
        idle.addTransition(new QEventTransition(QEvent.Type.User + 1).setTargetState(busy));
        busy.addTransition(QEvent.Type.User + 2, doneTop);      // JQt 便捷重载
        m1.setInitialState(idle);
        AtomicInteger started = new AtomicInteger(), finished = new AtomicInteger(), stopped = new AtomicInteger();
        m1.onStarted(started::incrementAndGet).onFinished(finished::incrementAndGet).onStopped(stopped::incrementAndGet);

        check("start 前 isRunning=false", !m1.isRunning());
        m1.start();
        check("start 后 isRunning=true", m1.isRunning());
        check("初始状态已进入(" + log + ")", log.equals(List.of("+idle")));
        check("started 信号 1 次", started.get() == 1);
        check("configuration 含 idle", m1.configuration().contains(idle));

        m1.postEvent(new QEvent(QEvent.Type.User + 1, "go"));
        check("转换后 idle 退出/busy 进入(" + log + ")", log.equals(List.of("+idle", "-idle", "+busy")));
        check("busy.isActive()", busy.isActive() && !idle.isActive());

        m1.postEvent(new QEvent(QEvent.Type.User + 2));
        check("顶层终态 → finished 1 次", finished.get() == 1);
        check("顶层终态 → stopped 1 次", stopped.get() == 1);
        check("结束后 isRunning=false", !m1.isRunning());
        check("结束后活动状态已退出(空配置)", m1.configuration().isEmpty());

        int before = log.size();
        m1.postEvent(new QEvent(QEvent.Type.User + 1));
        check("未运行时投递被忽略", log.size() == before);

        // ---------------------------------------------------------- 3) 嵌套顺序
        QStateMachine m2 = new QStateMachine("m2");
        QState outer = new QState(m2, "outer");
        QState inner1 = new QState(outer, "inner1");
        QState inner2 = new QState(outer, "inner2");
        QState other = new QState(m2, "other");
        List<String> seq = Collections.synchronizedList(new ArrayList<>());
        outer.onEntered(() -> seq.add("+outer")).onExited(() -> seq.add("-outer"));
        inner1.onEntered(() -> seq.add("+inner1")).onExited(() -> seq.add("-inner1"));
        inner2.onEntered(() -> seq.add("+inner2")).onExited(() -> seq.add("-inner2"));
        other.onEntered(() -> seq.add("+other"));
        outer.setInitialState(inner1);
        inner1.addTransition(QEvent.Type.User + 1, inner2);
        inner2.addTransition(QEvent.Type.User + 2, other);      // 离开复合状态
        m2.setInitialState(outer);
        m2.start();
        check("进入顺序:父先子后(" + seq + ")", seq.equals(List.of("+outer", "+inner1")));

        m2.postEvent(new QEvent(QEvent.Type.User + 1));
        check("兄弟转换:先退后进(" + seq + ")",
                seq.subList(2, seq.size()).equals(List.of("-inner1", "+inner2")));

        // ---------------------------------------------------------- 4) 退出复合
        m2.postEvent(new QEvent(QEvent.Type.User + 2));
        check("离开复合状态:子先父后,再进目标(" + seq + ")",
                seq.subList(4, seq.size()).equals(List.of("-inner2", "-outer", "+other")));
        check("other.isActive()", other.isActive() && !outer.isActive());

        // ---------------------------------------------------------- 5) 嵌套终态
        QStateMachine m3 = new QStateMachine("m3");
        QState work = new QState(m3, "work");
        QState step = new QState(work, "step");
        QFinalState workDone = new QFinalState(work, "workDone");
        QFinalState allDone = new QFinalState(m3, "allDone");
        AtomicInteger workFinished = new AtomicInteger();
        work.onFinished(workFinished::incrementAndGet);
        work.setInitialState(step);
        step.addTransition(QEvent.Type.User + 1, workDone);
        work.addTransition(allDone);                            // 无条件转换
        m3.setInitialState(work);
        m3.start();
        m3.postEvent(new QEvent(QEvent.Type.User + 1));
        check("子终态 → 父状态 finished(" + workFinished.get() + ")", workFinished.get() == 1);
        check("嵌套完成时机器仍在运行", m3.isRunning());
        m3.postEvent(new QEvent(QEvent.Type.User + 42));
        check("无条件转换收尾:机器已结束", !m3.isRunning() && allDone.isActive() == false);

        // ---------------------------------------------------------- 6) 并行状态
        QStateMachine m4 = new QStateMachine("m4");
        QState par = new QState(m4, "par").setChildMode(QState.ChildMode.Parallel);
        QState ra = new QState(par, "regionA");
        QState ra1 = new QState(ra, "a1");
        QFinalState raDone = new QFinalState(ra, "aDone");
        QState rb = new QState(par, "regionB");
        QState rb1 = new QState(rb, "b1");
        QFinalState rbDone = new QFinalState(rb, "bDone");
        QFinalState parAll = new QFinalState(m4, "parAll");
        ra.setInitialState(ra1);
        rb.setInitialState(rb1);
        ra1.addTransition(QEvent.Type.User + 1, raDone);
        rb1.addTransition(QEvent.Type.User + 2, rbDone);
        m4.setInitialState(par);
        m4.start();
        check("并行进入两个区域", ra1.isActive() && rb1.isActive());
        m4.postEvent(new QEvent(QEvent.Type.User + 1));
        check("区域 A 完成、B 未完成 → 并行未完成(机器仍运行)", ra.isFinished() && !par.isFinished() && m4.isRunning());
        m4.postEvent(new QEvent(QEvent.Type.User + 2));
        check("两区域都完成 → 并行状态 finished", par.isFinished());
        par.addTransition(parAll);                              // 并行完成后再收尾
        m4.postEvent(new QEvent(QEvent.Type.User + 3));
        check("并行收尾:机器结束", !m4.isRunning());

        // ---------------------------------------------------------- 7) 历史状态
        QStateMachine m5 = new QStateMachine("m5");
        QState run = new QState(m5, "run");
        QState r1 = new QState(run, "r1");
        QState r2 = new QState(run, "r2");
        QState paused = new QState(m5, "paused");
        QHistoryState hist = new QHistoryState(run, "hist");
        run.setInitialState(r1);
        hist.setDefaultState(r1);
        r1.addTransition(QEvent.Type.User + 1, r2);
        run.addTransition(QEvent.Type.User + 2, paused);        // 整个 run 切到 paused
        paused.addTransition(QEvent.Type.User + 3, hist);       // 回来时用历史
        m5.setInitialState(run);
        m5.start();
        m5.postEvent(new QEvent(QEvent.Type.User + 1));
        check("run 内已切到 r2", r2.isActive());
        m5.postEvent(new QEvent(QEvent.Type.User + 2));
        check("切到 paused", paused.isActive() && !run.isActive());
        m5.postEvent(new QEvent(QEvent.Type.User + 3));
        check("历史恢复:回到 r2 而不是 r1(当前配置 " + ids(m5) + ")", r2.isActive() && !r1.isActive());

        QStateMachine m6 = new QStateMachine("m6");
        QState g = new QState(m6, "g");
        QState g1 = new QState(g, "g1");
        QState g2 = new QState(g, "g2");
        QHistoryState h6 = new QHistoryState(g, "h6");
        g.setInitialState(g1);
        h6.setDefaultState(g2);
        m6.setInitialState(h6);                                 // 从未进入过 → 走默认状态
        m6.start();
        check("历史无记录 → 默认状态 g2", g2.isActive() && !g1.isActive());

        // ---------------------------------------------------------- 8) 错误状态
        QStateMachine m7 = new QStateMachine("m7");
        QState s7 = new QState(m7, "s7");
        QState err = new QState(m7, "err");
        AtomicInteger errFired = new AtomicInteger();
        m7.setInitialState(s7);
        m7.setErrorState(err);
        m7.onError(errFired::incrementAndGet);
        m7.start();
        m7.postEvent(new QEvent(QEvent.Type.User + 99));        // 无人处理
        check("未处理事件 → 进入 errorState", err.isActive() && m7.hasError());
        check("error 信号 1 次", errFired.get() == 1);
        m7.clearError();
        check("clearError() 复位", !m7.hasError() && !err.isActive());

        // ---------------------------------------------------------- 9) trigger() 扩展
        QStateMachine m8 = new QStateMachine("m8");
        QState t1 = new QState(m8, "t1");
        QState t2 = new QState(m8, "t2");
        AtomicInteger triggered = new AtomicInteger();
        QEventTransition tr = new QEventTransition();
        tr.setTargetState(t2);
        tr.onTriggered(triggered::incrementAndGet);
        t1.addTransition(tr);
        m8.setInitialState(t1);
        m8.start();
        tr.trigger();                                           // 手动触发(接任意回调)
        check("trigger() 触发转换", t2.isActive() && triggered.get() == 1);

        // 真实 Qt 信号 → 状态机(用 QTimer 当信号源)
        QApplication app = new QApplication();
        QStateMachine m9 = new QStateMachine("m9");
        QState u1 = new QState(m9, "u1");
        QState u2 = new QState(m9, "u2");
        QEventTransition tr9 = new QEventTransition();
        tr9.setTargetState(u2);
        u1.addTransition(tr9);
        m9.setInitialState(u1);
        m9.start();
        AtomicReference<String> afterSignal = new AtomicReference<>("(未转换)");
        QTimer.singleShot(30, tr9::trigger);                    // QTimer 信号 → 转换
        QTimer.singleShot(120, () -> afterSignal.set(u2.isActive() ? "u2" : "u1"));
        QMainWindow w = new QMainWindow("sm", 200, 120);
        w.show();
        app.scheduleQuit(200);
        app.exec();
        check("QTimer 信号驱动转换(实际 " + afterSignal.get() + ")", "u2".equals(afterSignal.get()));

        // ---------------------------------------------------------- 10) 跨线程
        QStateMachine m10 = new QStateMachine("m10");
        QState x1 = new QState(m10, "x1");
        QState x2 = new QState(m10, "x2");
        x1.addTransition(QEvent.Type.User + 1, x2);
        m10.setInitialState(x1);
        m10.start();
        Thread th = new Thread(() -> m10.postEvent(new QEvent(QEvent.Type.User + 1)), "sm-worker");
        th.start();
        th.join(2000);
        check("跨线程 postEvent 生效", x2.isActive());

        System.out.println("[sm] pass=" + pass + " fail=" + fail);
        System.out.println("[sm] " + (fail == 0 ? "ALL PASS ✅" : ("FAILED: " + fail)));
        if (fail > 0) System.exit(1);
    }
}
