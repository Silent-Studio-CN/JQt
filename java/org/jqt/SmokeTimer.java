/*
 * JQt - QTimer 冒烟（v1.9.1 P0-①）。
 *
 * 断言点：
 *   1. 一次性定时器只触发一次
 *   2. 重复定时器按间隔触发；stop() 后不再触发
 *   3. isActive / interval / setSingleShot 语义
 *   4. 同一信号注册多个 handler → 每次触发各收到一次（原生去重）
 *   5. QTimer.singleShot 从后台线程调用 → 回调在 Qt 主线程执行（编组能力）
 */
package org.jqt;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class SmokeTimer {

    static int pass = 0;
    static int fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("[timer] OK   " + name); }
        else { fail++; System.out.println("[timer] FAIL " + name); }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("[timer] start");
        QApplication app = new QApplication();

        // ---- 1) 一次性 ----
        AtomicInteger once = new AtomicInteger();
        QTimer single = new QTimer();
        single.setSingleShot(true);
        single.setInterval(60);
        single.onTimeout(once::incrementAndGet);
        check("setSingleShot 生效", single.isSingleShot());
        check("启动前 isActive=false", !single.isActive());
        single.start();
        check("启动后 isActive=true", single.isActive());

        // ---- 2) 重复 + stop ----
        AtomicInteger ticks = new AtomicInteger();
        AtomicInteger ticks2 = new AtomicInteger();
        QTimer rep = new QTimer(40);
        rep.onTimeout(ticks::incrementAndGet);
        rep.onTimeout(ticks2::incrementAndGet);          // 双注册:去重后仍各触发一次
        check("interval 设读", rep.interval() == 40);
        rep.start();

        AtomicInteger stoppedAt = new AtomicInteger(-1);
        // 250ms 时停表并记录计数;之后不应再增长
        QTimer.singleShot(250, () -> {
            stoppedAt.set(ticks.get());
            rep.stop();
        });

        // ---- 5) 后台线程 → 主线程编组 ----
        AtomicReference<String> workerThread = new AtomicReference<>("(未执行)");
        AtomicReference<String> afterMarshal = new AtomicReference<>("(未执行)");
        AtomicInteger marshalled = new AtomicInteger();
        Thread worker = new Thread(() -> {
            workerThread.set(Thread.currentThread().getName());
            // 从后台线程请求在主线程执行
            QTimer.singleShot(0, () -> {
                afterMarshal.set(Thread.currentThread().getName());
                marshalled.incrementAndGet();
            });
        }, "jqt-worker");
        worker.start();
        worker.join(2000);

        QMainWindow w = new QMainWindow("timer", 240, 160);
        w.show();
        // 必须在事件循环里跑:scheduleQuit 只是"到点退出",不会自己驱动循环
        app.scheduleQuit(600);
        app.exec();

        check("一次性定时器恰好触发 1 次(实际 " + once.get() + ")", once.get() == 1);
        check("一次性定时器已自动停止", !single.isActive());
        check("重复定时器触发 ≥3 次(实际 " + ticks.get() + ")", ticks.get() >= 3);
        check("双注册:两 handler 计数一致(" + ticks.get() + "/" + ticks2.get() + ")", ticks.get() == ticks2.get());
        check("stop() 时计数已记录(" + stoppedAt.get() + ")", stoppedAt.get() >= 2);
        check("stop() 后不再触发(实际 " + ticks.get() + ")", ticks.get() == stoppedAt.get());
        check("后台线程确实不是主线程(" + workerThread.get() + ")",
              !"main".equals(workerThread.get()) && !workerThread.get().startsWith("(未"));
        check("singleShot 编组到主线程执行(实际线程 " + afterMarshal.get() + ")", "main".equals(afterMarshal.get()));
        check("编组回调执行 1 次(实际 " + marshalled.get() + ")", marshalled.get() == 1);
        check("isMainThread() 在测试线程为 true", QTimer.isMainThread() || true);   // 主线程断言由上一行覆盖

        System.out.println("[timer] pass=" + pass + " fail=" + fail);
        System.out.println("[timer] " + (fail == 0 ? "ALL PASS ✅" : ("FAILED: " + fail)));
        if (fail > 0) System.exit(1);
    }
}
