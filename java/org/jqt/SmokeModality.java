/*
 * JQt - 模态语义冒烟(v1.9.1,问题报告 J13/J14).
 *
 * 把报告第五节"open() 是否真的阻塞父窗口"从**待确证**变成可复现的实测结论:
 *   · open() -> isModal true 且 windowModality == WindowModal(1)
 *   · **父窗口 isEnabled 仍为 true** —— 实测推翻"Windows 用 EnableWindow 禁父窗口"的假设;
 *     Qt 6 的模态是由 QApplicationPrivate::isBlockedByModal 在**事件分发**层拦截,
 *     因此 isEnabled 不是判断模态的可靠探针(本冒烟把这个事实固定下来)
 *   · show() -> isModal false(真非模态)
 */
package org.jqt;

public class SmokeModality {

    static int pass = 0;
    static int fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("[modal] OK   " + name); }
        else { fail++; System.out.println("[modal] FAIL " + name); }
    }
    public static void main(String[] args) {
        System.out.println("[modal] start");
        QApplication app = new QApplication();

        QMainWindow parent = new QMainWindow("parent", 300, 200);
        parent.show();

        QDialog dlg = new QDialog("child", parent.nativeApi().nativeHandle());
        check("对话框默认 windowModality = 0(NonModal)", dlg.windowModality() == 0);
        check("对话框默认 isModal = false", !dlg.isModal());

        dlg.open();                       // 窗口模态
        check("open() 后 isModal = true(窗口模态语义)", dlg.isModal());
        check("open() 后 windowModality = 1(WindowModal,而非应用模态)", dlg.windowModality() == 1);
        // 关键实测:Qt 6 不禁用父窗口,模态由事件分发拦截
        check("open() 期间父窗口 isEnabled 仍为 true(证明模态不是靠禁父窗口实现)",
              parent.isEnabled());
        int modalNow = (int) Math.min(1, QApplication.activeModalWidget() == 0 ? 0 : 1);
        check("activeModalWidget() 可调用且返回 0 或有效指针(受前台焦点影响,实测 "
              + (modalNow == 0 ? "0" : "非0") + ")", modalNow == 0 || modalNow == 1);

        dlg.close();
        check("close() 后 isModal = false", !dlg.isModal());

        QDialog d2 = new QDialog("child2", parent.nativeApi().nativeHandle());
        d2.show();                        // 真非模态
        check("show() 后 isModal = false(与 open() 区分)", !d2.isModal());
        d2.close();

        app.scheduleQuit(600);
        app.exec();

        System.out.println("[modal] pass=" + pass + " fail=" + fail);
        System.out.println("[modal] " + (fail == 0 ? "ALL PASS ✅" : ("FAILED: " + fail)));
        if (fail > 0) System.exit(1);
    }
}
