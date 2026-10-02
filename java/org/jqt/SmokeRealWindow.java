/*
 * JQt - 真窗口冒烟（v1.9.1，修 J9）。
 *
 * 为什么需要它：CI 一直只跑 QT_QPA_PLATFORM=offscreen，而"真窗口崩溃"类问题
 * （macOS 的 NSWindow 主线程断言 J1、平台插件缺失、窗口/布局原生路径异常）
 * 在 offscreen 下**根本不会触发** —— 全绿但真机崩溃。本冒烟使用真实平台插件，
 * 创建/显示/移动/缩放窗口并跑事件循环，Linux 侧在 CI 里用 Xvfb 提供显示。
 */
package org.jqt;

public class SmokeRealWindow {

    static int pass = 0;
    static int fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("[win] OK   " + name); }
        else { fail++; System.out.println("[win] FAIL " + name); }
    }

    public static void main(String[] args) {
        System.out.println("[win] start  platform=" + System.getProperty("jqt.platform", "(real)"));
        QApplication app = new QApplication();

        QMainWindow w = new QMainWindow("real-window-smoke", 420, 260);
        QVBoxLayout v = new QVBoxLayout();
        QLabel label = new QLabel("real window");
        QPushButton btn = new QPushButton("click");
        QLineEdit edit = new QLineEdit("hello");
        QStackedWidget stack = new QStackedWidget();
        stack.addWidget(new QLabel("p0"));
        stack.addWidget(new QLabel("p1"));
        v.addWidget(label);
        v.addWidget(btn);
        v.addWidget(edit);
        v.addWidget(stack);
        w.setLayout(v);            // QWidget 是抽象类,布局直接挂在窗口上(MainWindow 壳支持)

        check("显示前 isVisible=false", !w.isVisible());
        w.show();
        check("show() 后 isVisible=true", w.isVisible());

        // 真窗口路径：移动/缩放/切页/输入
        w.resize(520, 320);
        w.move(40, 40);
        stack.setCurrentIndex(1);
        edit.setText("changed");
        QTimer.singleShot(60, () -> {
            check("事件循环中窗口仍可见", w.isVisible());
            check("stacked 切到第 2 页", stack.currentIndex() == 1);
        });
        QTimer.singleShot(120, () -> {
            w.hide();
            check("hide() 后 isVisible=false", !w.isVisible());
        });

        app.scheduleQuit(300);
        app.exec();

        check("退出事件循环后仍存活(无崩溃)", true);
        System.out.println("[win] pass=" + pass + " fail=" + fail);
        System.out.println("[win] " + (fail == 0 ? "ALL PASS ✅" : ("FAILED: " + fail)));
        if (fail > 0) System.exit(1);
    }
}
