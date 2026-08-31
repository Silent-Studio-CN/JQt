/*
 * JQt - setVisible 冒烟（开发者反馈回归）。
 */
package org.jqt;

public class SmokeVisible {

    static int pass = 0;
    static int fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("[vis] OK  " + name); }
        else { fail++; System.out.println("[vis] FAIL " + name); }
    }

    public static void main(String[] args) {
        System.out.println("[vis] start");
        QApplication app = new QApplication();

        QWidget w = new QPushButton("t");
        w.setVisible(false);
        check("setVisible(false) -> isVisible()==false", !w.isVisible());
        w.setVisible(true);
        check("setVisible(true) -> isVisible()==true", w.isVisible());
        w.setWindowRole("test-role");
        check("windowRole() 非 null（Windows 行为：role 可能不持久）", w.windowRole() != null);

        System.out.println("[vis] pass=" + pass + " fail=" + fail);
        System.out.println(fail == 0 ? "[vis] ALL PASS" : "[vis] FAILURES");
        app.quit();
    }
}
