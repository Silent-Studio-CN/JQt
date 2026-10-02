/*
 * JQt - QThread / QThreadPool 冒烟（v1.9.1 P0-③）。
 *
 * 断言点：
 *   1. QThread 在新线程执行 Java 任务；start/wait/isRunning/isFinished 语义
 *   2. 任务内可感知线程名（Thread.currentThread().setName 生效）
 *   3. 多个线程各自独立执行，计数正确
 *   4. QThreadPool.runAsync 并发执行多个任务；waitForDone 后全部完成
 *   5. 后台线程 → QTimer.singleShot 编组回主线程更新（"不卡界面"的正确姿势）
 */
package org.jqt;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class SmokeConcurrency {

    static int pass = 0;
    static int fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("[conc] OK   " + name); }
        else { fail++; System.out.println("[conc] FAIL " + name); }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("[conc] start");
        QApplication app = new QApplication();

        // ---- 1) QThread ----
        AtomicInteger ran = new AtomicInteger();
        ConcurrentLinkedQueue<String> threadNames = new ConcurrentLinkedQueue<>();
        QThread t1 = new QThread(() -> {
            threadNames.add(Thread.currentThread().getName());
            ran.incrementAndGet();
        }, "jqt-worker-A");
        QThread t2 = new QThread(() -> {
            threadNames.add(Thread.currentThread().getName());
            ran.incrementAndGet();
        }, "jqt-worker-B");

        check("启动前 isRunning=false", !t1.isRunning());
        check("启动前 isFinished=false", !t1.isFinished());
        t1.start();
        t2.start();
        check("wait 能等到结束", t1.wait(5000) && t2.wait(5000));
        check("两个任务都执行了(实际 " + ran.get() + ")", ran.get() == 2);
        check("结束后 isRunning=false", !t1.isRunning());
        check("结束后 isFinished=true", t1.isFinished());
        check("线程名已生效 " + threadNames, threadNames.contains("jqt-worker-A") && threadNames.contains("jqt-worker-B"));

        // ---- 2) QThreadPool ----
        int n = 6;
        AtomicInteger done = new AtomicInteger();
        AtomicInteger onMain = new AtomicInteger();
        for (int i = 0; i < n; i++) {
            QThreadPool.runAsync(() -> {
                if (QTimer.isMainThread()) onMain.incrementAndGet();
                done.incrementAndGet();
            });
        }
        check("waitForDone 全部结束", QThreadPool.waitForDone(5000));
        check("线程池执行了 " + n + " 个任务(实际 " + done.get() + ")", done.get() == n);
        check("线程池任务不在主线程执行(实际主线程次数 " + onMain.get() + ")", onMain.get() == 0);
        check("maxThreadCount>0(" + QThreadPool.maxThreadCount() + ")", QThreadPool.maxThreadCount() > 0);

        // ---- 3) 后台 → 主线程编组(界面更新的正确姿势) ----
        AtomicInteger uiUpdates = new AtomicInteger();
        AtomicInteger uiOnMain = new AtomicInteger();
        for (int i = 0; i < 5; i++) {
            QThreadPool.runAsync(() -> QTimer.singleShot(0, () -> {
                if (QTimer.isMainThread()) uiOnMain.incrementAndGet();
                uiUpdates.incrementAndGet();
            }));
        }
        QThreadPool.waitForDone(5000);
        // 编组动作要等事件循环跑起来才会执行
        QTimer.singleShot(200, () -> {});
        app.scheduleQuit(400);
        app.exec();

        check("编组回主线程执行 5 次(实际 " + uiUpdates.get() + ")", uiUpdates.get() == 5);
        check("且全部在主线程(实际 " + uiOnMain.get() + ")", uiOnMain.get() == 5);

        System.out.println("[conc] pass=" + pass + " fail=" + fail);
        System.out.println("[conc] " + (fail == 0 ? "ALL PASS ✅" : ("FAILED: " + fail)));
        if (fail > 0) System.exit(1);
    }
}
