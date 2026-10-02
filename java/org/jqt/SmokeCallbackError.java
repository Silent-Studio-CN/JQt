/*
 * JQt - 回调异常可捕获性冒烟（v1.9.1，修 J3）。
 * 断言：回调里抛出的异常能被 JQtCallbackErrors 的处理器收到（而不是只打印到 stderr）。
 */
package org.jqt;

import java.util.concurrent.atomic.AtomicReference;

public class SmokeCallbackError {

    static int pass = 0;
    static int fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("[cerr] OK   " + name); }
        else { fail++; System.out.println("[cerr] FAIL " + name); }
    }

    public static void main(String[] args) {
        System.out.println("[cerr] start");
        QApplication app = new QApplication();

        AtomicReference<Throwable> caught = new AtomicReference<>();
        JQtCallbackErrors.setHandler(caught::set);

        QTimer t = new QTimer(30);
        t.setSingleShot(true);
        t.onTimeout(() -> { throw new IllegalStateException("回调里故意抛的异常"); });
        t.start();

        app.scheduleQuit(300);
        app.exec();

        check("处理器收到了回调异常", caught.get() != null);
        check("异常类型正确(" + (caught.get() == null ? "null" : caught.get().getClass().getSimpleName()) + ")",
              caught.get() instanceof IllegalStateException);
        check("异常消息正确(" + (caught.get() == null ? "null" : caught.get().getMessage()) + ")",
              caught.get() != null && "回调里故意抛的异常".equals(caught.get().getMessage()));

        // 取消注册后不应再崩
        JQtCallbackErrors.setHandler(null);
        QTimer t2 = new QTimer(20);
        t2.setSingleShot(true);
        t2.onTimeout(() -> { throw new RuntimeException("第二个异常"); });
        t2.start();
        app.scheduleQuit(200);
        app.exec();
        check("取消处理器后仍存活(回到默认打印行为)", true);

        System.out.println("[cerr] pass=" + pass + " fail=" + fail);
        System.out.println("[cerr] " + (fail == 0 ? "ALL PASS ✅" : ("FAILED: " + fail)));
        if (fail > 0) System.exit(1);
    }
}
