/*
 * JQt - 信号投递确定性回归(v1.9.1)。
 *
 * 背景:旧实现的每个 nativeConnectXxx() 每次调用都新建一条 QObject::connect
 * 并泄漏一个 global ref —— Java 侧 onXxx() 注册 N 次 → 信号被投递 N 次,
 * 每次投递又遍历全部 handler,实机(macOS)观测到 1/1/6 这类非确定计数。
 *
 * 本冒烟用**可程序化触发的确定信号**断言投递次数:
 *   QLineEdit  textChanged / QCheckBox toggled / QSpinBox valueChanged /
 *   QComboBox  currentIndexChanged / QListWidget itemChanged(setItemIcon)
 */
package org.jqt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class SmokeSignalOnce {

    static int pass = 0;
    static int fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("[sig] OK   " + name); }
        else { fail++; System.out.println("[sig] FAIL " + name); }
    }

    public static void main(String[] args) {
        System.out.println("[sig] start");
        QApplication app = new QApplication();

        // ---------- 1) 同一信号注册两个 handler:每次发射各收到 1 次 ----------
        QLineEdit e1 = new QLineEdit("");
        AtomicInteger a1 = new AtomicInteger(), a2 = new AtomicInteger();
        e1.onTextChanged(s -> a1.incrementAndGet());
        e1.onTextChanged(s -> a2.incrementAndGet());   // 旧实现:此处再加一条 native 连接
        e1.setText("x");
        check("两次注册后 h1 恰好 1 次(实际 " + a1.get() + ")", a1.get() == 1);
        check("两次注册后 h2 恰好 1 次(实际 " + a2.get() + ")", a2.get() == 1);
        e1.setText("xy");
        check("第二次发射后 h1 累计 2(实际 " + a1.get() + ")", a1.get() == 2);
        check("第二次发射后 h2 累计 2(实际 " + a2.get() + ")", a2.get() == 2);

        // ---------- 2) 同一 handler 实例显式注册两次 → 语义上收到 2 次 ----------
        QLineEdit e2 = new QLineEdit("");
        AtomicInteger same = new AtomicInteger();
        Consumer<String> cb = s -> same.incrementAndGet();
        e2.onTextChanged(cb);
        e2.onTextChanged(cb);
        e2.setText("once");
        check("同一 handler 注册两次 → 收到 2 次(实际 " + same.get() + ")", same.get() == 2);

        // ---------- 3) 单次注册 + 多次发射:线性、顺序正确 ----------
        QLineEdit e3 = new QLineEdit("");
        List<String> seen = new ArrayList<>();
        e3.onTextChanged(seen::add);
        e3.setText("p"); e3.setText("pq"); e3.setText("pqr");
        check("textChanged 线性 3 次(实际 " + seen.size() + ")", seen.size() == 3);
        check("textChanged 顺序正确 " + seen, seen.equals(List.of("p", "pq", "pqr")));

        // ---------- 4) 不同控件类型 × 重复注册,均不得放大 ----------
        QCheckBox chk = new QCheckBox("c");
        AtomicInteger c1 = new AtomicInteger(), c2 = new AtomicInteger();
        chk.onToggled(b -> c1.incrementAndGet());
        chk.onToggled(b -> c2.incrementAndGet());
        chk.setChecked(true);
        check("QCheckBox 双注册 → 各 1 次(" + c1.get() + "/" + c2.get() + ")", c1.get() == 1 && c2.get() == 1);

        QSpinBox sp = new QSpinBox();
        AtomicInteger s1 = new AtomicInteger(), s2 = new AtomicInteger();
        sp.onValueChanged(v -> s1.incrementAndGet());
        sp.onValueChanged(v -> s2.incrementAndGet());
        sp.setValue(7);
        check("QSpinBox 双注册 → 各 1 次(" + s1.get() + "/" + s2.get() + ")", s1.get() == 1 && s2.get() == 1);

        QComboBox combo = new QComboBox();
        combo.addItem("A"); combo.addItem("B");
        AtomicInteger i1 = new AtomicInteger(), i2 = new AtomicInteger();
        combo.onCurrentIndexChanged(i -> i1.incrementAndGet());
        combo.onCurrentIndexChanged(i -> i2.incrementAndGet());
        combo.setCurrentIndex(1);
        check("QComboBox 双注册 → 各 1 次(" + i1.get() + "/" + i2.get() + ")",
              i1.get() == 1 && i2.get() == 1);

        // ---------- 5) 两个独立对象互不串扰 ----------
        QLineEdit x = new QLineEdit(""), y = new QLineEdit("");
        AtomicInteger cx = new AtomicInteger(), cy = new AtomicInteger();
        x.onTextChanged(s -> cx.incrementAndGet());
        y.onTextChanged(s -> cy.incrementAndGet());
        x.setText("only-x");
        check("对象 X 收到 1 次(实际 " + cx.get() + ")", cx.get() == 1);
        check("对象 Y 未收到(实际 " + cy.get() + ")", cy.get() == 0);

        // ---------- 6) 高频重复注册(20 次)不放大投递 ----------
        QLineEdit e6 = new QLineEdit("");
        AtomicInteger cnt = new AtomicInteger();
        for (int i = 0; i < 20; i++) e6.onTextChanged(s -> cnt.incrementAndGet());
        e6.setText("m");
        check("20 个 handler → 恰好 20 次(实际 " + cnt.get() + ")", cnt.get() == 20);

        // ---------- 7) QTreeWidget itemChanged(经 setItemText 确定触发) ----------
        QTreeWidget tree = new QTreeWidget();
        int id = tree.addTopLevelItem("node0");
        AtomicInteger t1 = new AtomicInteger(), t2 = new AtomicInteger();
        tree.onItemChanged(v -> t1.incrementAndGet());
        tree.onItemChanged(v -> t2.incrementAndGet());
        tree.setItemText(id, "node0-edited");
        check("QTreeWidget itemChanged 双注册 → 各 1 次(" + t1.get() + "/" + t2.get() + ")",
              t1.get() == 1 && t2.get() == 1);
        tree.setItemText(id, "node0-edited2");
        check("QTreeWidget 第二次发射 → 累计 2(" + t1.get() + "/" + t2.get() + ")",
              t1.get() == 2 && t2.get() == 2);

        // ---------- 8) 销毁后不残留(建/删循环不放大后续计数) ----------
        for (int i = 0; i < 5; i++) {
            QLineEdit tmp = new QLineEdit("");
            AtomicInteger t = new AtomicInteger();
            tmp.onTextChanged(s -> t.incrementAndGet());
            tmp.setText("z");
            check("循环#" + i + " 临时对象投递 1 次(实际 " + t.get() + ")", t.get() == 1);
        }

        QMainWindow w = new QMainWindow("sig", 240, 160);
        w.show();
        app.scheduleQuit(300);

        System.out.println("[sig] pass=" + pass + " fail=" + fail);
        System.out.println("[sig] " + (fail == 0 ? "ALL PASS ✅" : ("FAILED: " + fail)));
        if (fail > 0) System.exit(1);
    }
}
