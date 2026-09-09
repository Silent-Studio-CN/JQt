/*
 * JQt - L2 汇总冒烟 b1（B1-B4：QSplitter/QMainWindow 状态持久化、按钮/标签、行编辑）。
 * 对应提交 B1 dd9c168 / B2 285a8f8 / B3 d728ab5 / B4 f90fae2。
 * 注：前辈 SmokeL2（prints 风格）保持不动；本文件为 check 风格汇总，fail>0 时非零退出。
 */
package org.jqt;

import java.util.Arrays;

public class SmokeL2b1 {

    static int pass = 0;
    static int fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("[l2b1] OK   " + name); }
        else { fail++; System.out.println("[l2b1] FAIL " + name); }
    }

    static boolean near(int[] x, int[] y, int tol) {
        if (x.length != y.length) return false;
        for (int i = 0; i < x.length; i++) if (Math.abs(x[i] - y[i]) > tol) return false;
        return true;
    }

    public static void main(String[] args) {
        System.out.println("[l2b1] start");
        QApplication app = new QApplication();
        QMainWindow w = new QMainWindow("smokel2b1", 700, 320);

        // ---------- B1:QSplitter saveState/restoreState ----------
        QSplitter sp = new QSplitter();
        sp.addWidget(new QLabel("a"));
        sp.addWidget(new QLabel("b"));
        sp.addWidget(new QLabel("c"));
        QVBoxLayout v = new QVBoxLayout();
        v.addWidget(sp);
        w.setLayout(v);
        w.show();
        app.scheduleQuit(500);
        app.exec();

        int avail = 0;
        for (int s : sp.sizes()) avail += s;
        check("B1 窗口 show 后 splitter 已布局", avail > 300);
        sp.setSizes(new int[]{ 120, 200, avail - 320 });
        int[] sA = sp.sizes();
        check("B1 setSizes 生效(≈120)", Math.abs(sA[0] - 120) <= 6);
        byte[] stateA = sp.saveState();
        check("B1 saveState 非空", stateA != null && stateA.length > 0);
        sp.setSizes(new int[]{ 260, 180, avail - 440 });
        check("B1 布局 B 生效(≈260)", Math.abs(sp.sizes()[0] - 260) <= 6);
        check("B1 restoreState true", sp.restoreState(stateA));
        check("B1 恢复回布局 A", near(sA, sp.sizes(), 6));
        check("B1 restoreState(null)=false", !sp.restoreState(null));

        // ---------- B2:QMainWindow 壳结构状态持久化 ----------
        QMainWindow mw = new QMainWindow("b2", 600, 300);
        mw.menuBar();
        mw.statusBar();
        QToolBar ta = new QToolBar(); ta.setObjectName("tbA"); mw.addToolBar(ta);
        QToolBar tb = new QToolBar(); tb.setObjectName("tbB"); mw.addToolBar(tb);
        QLabel d1 = new QLabel("d1"); d1.setObjectName("dock1"); mw.addDockWidget(QMainWindow.DOCK_LEFT, d1);
        QLabel d2 = new QLabel("d2"); d2.setObjectName("dock2"); mw.addDockWidget(QMainWindow.DOCK_RIGHT, d2);
        byte[] m1 = mw.saveState();
        check("B2 saveState 非空", m1 != null && m1.length > 10);
        mw.removeDockWidget(d2);
        mw.addDockWidget(QMainWindow.DOCK_LEFT, d2);
        check("B2 restoreState 成功", mw.restoreState(m1));
        check("B2 恢复后结构一致", Arrays.equals(m1, mw.saveState()));
        mw.removeDockWidget(d2);
        check("B2 缺件 restore=false", !mw.restoreState(m1));
        check("B2 null=false / 坏数据=false", !mw.restoreState(null) && !mw.restoreState(new byte[]{ 1, 2 }));

        // ---------- B3:按钮/标签 ----------
        QPushButton btn = new QPushButton("x");
        btn.setAutoRepeat(true);
        check("B3 autoRepeat 设读", btn.autoRepeat());
        btn.setAutoRepeatDelay(600);
        btn.setAutoRepeatInterval(80);
        final int[] clicks = { 0 };
        btn.onClicked(() -> clicks[0]++);
        btn.animateClick();
        app.scheduleQuit(400);
        app.exec();
        check("B3 animateClick 触发 clicked", clicks[0] >= 1);
        btn.setAutoExclusive(true);
        check("B3 autoExclusive 设读", btn.autoExclusive());
        QLabel lbl = new QLabel("<b>rich</b>");
        lbl.setTextFormat(QLabel.TEXT_RICH);
        lbl.setTextFormat(QLabel.TEXT_PLAIN);
        lbl.setTextInteractionFlags(QLabel.TEXT_BROWSER_INTERACTION);
        check("B3 QLabel 文本格式/旗标不崩", true);

        // ---------- B4:行编辑 ----------
        QLineEdit edit = new QLineEdit("hello world");
        edit.setCursorPosition(5);
        check("B4 setCursorPosition", edit.cursorPosition() == 5);
        edit.end(true);
        check("B4 end(true)", edit.cursorPosition() == 11);
        edit.home(false);
        check("B4 home(false)", edit.cursorPosition() == 0);
        edit.cursorForward(false, 3);
        edit.cursorBackward(true, 2);
        check("B4 光标前进/后退", edit.cursorPosition() == 1);
        edit.setText("abc123");
        edit.setCursorPosition(3);
        edit.backspace();
        check("B4 backspace 后文本", "ab123".equals(edit.text()));
        int at = edit.cursorPositionAt(edit.x() + 1, edit.y() + 1);
        check("B4 cursorPositionAt 不崩", at >= 0 || at == -1);
        edit.setInputMask("999");
        check("B4 inputMask 设读", "999".equals(edit.inputMask()));
        edit.setInputMask("");
        check("B4 inputMask 清除", "".equals(edit.inputMask()));
        edit.setTextMargins(3, 4, 5, 6);
        int[] m = edit.textMargins();
        check("B4 textMargins 设读", m[0] == 3 && m[3] == 6);

        System.out.println("[l2b1] pass=" + pass + " fail=" + fail);
        System.out.println(fail == 0 ? "[l2b1] ALL PASS" : "[l2b1] FAILURES");
        System.out.flush();
        app.quit();
        if (fail > 0) { System.exit(1); }
    }
}
