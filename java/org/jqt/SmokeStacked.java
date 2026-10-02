/*
 * JQt - QStackedWidget 冒烟（v1.9.1，修 J5：补上 native 创建入口）。
 */
package org.jqt;

public class SmokeStacked {

    static int pass = 0;
    static int fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("[stack] OK   " + name); }
        else { fail++; System.out.println("[stack] FAIL " + name); }
    }

    public static void main(String[] args) {
        System.out.println("[stack] start");
        QApplication app = new QApplication();

        // J5 核心:Java 侧能直接 new 了
        QStackedWidget stack = new QStackedWidget();
        check("无参构造器可用(句柄非 0)", stack.nativeApi().nativeHandle() != 0);
        check("初始页数 = 0(实际 " + stack.count() + ")", stack.count() == 0);
        check("空栈 currentIndex = -1(实际 " + stack.currentIndex() + ")", stack.currentIndex() == -1);

        QLabel a = new QLabel("page-A");
        QLabel b = new QLabel("page-B");
        QLabel c = new QLabel("page-C");
        stack.addWidget(a);
        stack.addWidget(b);
        stack.addWidget(c);
        check("addWidget 后页数 = 3(实际 " + stack.count() + ")", stack.count() == 3);
        check("currentIndex 默认 0(实际 " + stack.currentIndex() + ")", stack.currentIndex() == 0);

        stack.setCurrentIndex(2);
        check("setCurrentIndex(2) 生效(实际 " + stack.currentIndex() + ")", stack.currentIndex() == 2);

        check("indexOf(b) = 1(实际 " + stack.indexOf(b) + ")", stack.indexOf(b) == 1);
        check("widgetHandleAt(1) 与 b 的句柄一致",
              stack.widgetHandleAt(1) == b.nativeApi().nativeHandle());

        stack.removeWidget(b);
        check("removeWidget 后页数 = 2(实际 " + stack.count() + ")", stack.count() == 2);

        QMainWindow w = new QMainWindow("stack", 240, 160);
        w.show();
        app.scheduleQuit(200);
        app.exec();

        System.out.println("[stack] pass=" + pass + " fail=" + fail);
        System.out.println("[stack] " + (fail == 0 ? "ALL PASS ✅" : ("FAILED: " + fail)));
        if (fail > 0) System.exit(1);
    }
}
