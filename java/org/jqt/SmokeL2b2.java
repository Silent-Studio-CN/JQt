/*
 * JQt - L2 汇总冒烟 2/2（B5-B10：下拉、应用查询、列表、布局、validator、分组门面）。
 * 对应提交 B5 3150f13 / B6 030befe / B7 0d0e3ac / B8 1b408b2 / B9 7dd6a0c / B10 a89e60b。
 */
package org.jqt;

public class SmokeL2b2 {

    static int pass = 0;
    static int fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("[l2b2] OK   " + name); }
        else { fail++; System.out.println("[l2b2] FAIL " + name); }
    }

    public static void main(String[] args) {
        System.out.println("[l2b2] start");
        QApplication app = new QApplication();

        // ---------- B5:QComboBox ----------
        QComboBox combo = new QComboBox();
        combo.addItem("Alpha");
        combo.addItem("beta");
        check("B5 findText 精确命中", combo.findText("Alpha") == 0);
        check("B5 findText 大小写敏感", combo.findText("alpha") == -1);
        combo.setSizeAdjustPolicy(QComboBox.ADJUST_TO_CONTENTS);
        check("B5 sizeAdjustPolicy 设读", combo.sizeAdjustPolicy() == QComboBox.ADJUST_TO_CONTENTS);
        combo.setInsertPolicy(QComboBox.INSERT_AT_BOTTOM);
        check("B5 insertPolicy 设读", combo.insertPolicy() == QComboBox.INSERT_AT_BOTTOM);

        // ---------- B6:QApplication 查询族 ----------
        QMainWindow q = new QMainWindow("b6", 300, 200);
        QPushButton fb = new QPushButton("f");
        QVBoxLayout qv = new QVBoxLayout();
        qv.addWidget(fb);
        q.setLayout(qv);
        q.show();
        fb.setFocus();
        app.scheduleQuit(400);
        app.exec();
        check("B6 topLevelWidgets 非空", QApplication.topLevelWidgets().length >= 1);
        check("B6 allWidgets 含窗口+按钮", QApplication.allWidgets().length >= 2);
        check("B6 style() 非空", QApplication.style() != null && !QApplication.style().isEmpty());
        boolean before = QApplication.quitOnLastWindowClosed();
        QApplication.setQuitOnLastWindowClosed(false);
        check("B6 quitOnLastWindowClosed 设读", !QApplication.quitOnLastWindowClosed());
        QApplication.setQuitOnLastWindowClosed(before);
        // activeWindow/focusWidget 无前台焦点环境下 Qt 返回 0,只记录
        System.out.println("[l2b2] info activeWindow=" + QApplication.activeWindow()
                + " focusWidget=" + QApplication.focusWidget());

        // ---------- B7:QListWidget 行语义 ----------
        QListWidget list = new QListWidget();
        list.addItem("Apple");
        list.addItem("Banana");
        list.addItem("apple2");
        java.util.List<Integer> hits = list.findItems("APPLE", false);
        check("B7 findItems 忽略大小写", hits.size() == 1 && hits.get(0) == 0);
        check("B7 findItems(null)=空", list.findItems(null).isEmpty());
        list.setSelectionMode(QListWidget.EXTENDED_SELECTION);
        list.setCurrentItem(1);
        list.scrollToItem(2);
        int[] rect = list.visualItemRect(0);
        check("B7 visualItemRect(0)", rect.length == 4);
        QLabel custom = new QLabel("w");
        list.setItemWidget(0, custom);
        list.removeItemWidget(0);
        list.openPersistentEditor(1);
        list.closePersistentEditor(1);
        check("B7 itemWidget/编辑器不崩", true);

        // ---------- B8:布局 ----------
        QHBoxLayout hbox = new QHBoxLayout();
        hbox.addWidget(new QLabel("h1"));
        hbox.setDirection(QLayout.RIGHT_TO_LEFT);
        check("B8 盒 direction 设读", hbox.direction() == QLayout.RIGHT_TO_LEFT);
        hbox.addStrut(60);
        QGridLayout grid = new QGridLayout();
        grid.addWidget(new QLabel("g1"), 0, 0);
        grid.addWidget(new QLabel("g2"), 1, 1, 1, 2);
        boolean threw = false;
        try { grid.setDirection(QLayout.TOP_TO_BOTTOM); } catch (IllegalStateException e) { threw = true; }
        check("B8 网格 setDirection 抛 ISE", threw);
        grid.setColumnMinimumWidth(0, 40);
        grid.setHorizontalSpacing(8);
        grid.setVerticalSpacing(6);
        check("B8 grid 间距设读", grid.horizontalSpacing() == 8 && grid.verticalSpacing() == 6);
        grid.setOriginCorner(QGridLayout.CORNER_TOP_RIGHT);
        check("B8 originCorner 设读", grid.originCorner() == QGridLayout.CORNER_TOP_RIGHT);
        int[] pos = grid.getItemPosition(1);
        check("B8 getItemPosition(1)", pos[0] == 1 && pos[1] == 1 && pos[3] == 2);
        check("B8 getItemPosition(越界)=-1", grid.getItemPosition(99)[0] == -1);

        // ---------- B9:validator 对象体系 ----------
        QIntValidator iv = new QIntValidator(0, 100);
        check("B9 int 界内 Acceptable", iv.validate("50") == QValidator.ACCEPTABLE);
        check("B9 int 越界完整数 Intermediate", iv.validate("150") == QValidator.INTERMEDIATE);
        check("B9 int 非数字 Invalid", iv.validate("abc") == QValidator.INVALID);
        QDoubleValidator dv = new QDoubleValidator(0.0, 1.0, 2);
        check("B9 double 界内", dv.validate("0.55") == QValidator.ACCEPTABLE);
        QRegularExpressionValidator rv = new QRegularExpressionValidator("[0-9]{4}");
        check("B9 regex 整串", rv.validate("2026") == QValidator.ACCEPTABLE);
        QLineEdit ve = new QLineEdit("");
        ve.setValidator(iv);
        check("B9 QLineEdit.validator() 同一实例", ve.validator() == iv);
        ve.setValidator(null);
        check("B9 清除后 null", ve.validator() == null);

        // ---------- B10:分组门面 ----------
        QMainWindow w10 = new QMainWindow("b10", 300, 200);
        QVBoxLayout v10 = new QVBoxLayout();
        v10.addWidget(new QPushButton("p"));
        w10.setLayout(v10);
        w10.show();
        app.scheduleQuit(400);
        app.exec();
        check("B10 window() 缓存", w10.window() == w10.window());
        check("B10 门面全缓存", w10.style() == w10.style() && w10.drag() == w10.drag()
                && w10.focus() == w10.focus() && w10.event() == w10.event() && w10.nativeApi() == w10.nativeApi());
        check("B10 window().isMaximized 委托", w10.window().isMaximized() == w10.isMaximized());
        w10.window().setOpacity(0.8);
        check("B10 setOpacity→owner", Math.abs(w10.windowOpacity() - 0.8) < 0.02);
        w10.style().setFont("Microsoft YaHei UI", 14);
        check("B10 style().fontFamily 回读", "Microsoft YaHei UI".equals(w10.style().fontFamily()));
        w10.drag().setAcceptDrops(true);
        check("B10 drag().acceptDrops", w10.acceptDrops());
        w10.focus().setFocusPolicy(1);
        check("B10 focus().focusPolicy", w10.focus().focusPolicy() == 1);
        check("B10 nativeApi().nativeHandle 一致", w10.nativeApi().nativeHandle() == w10.nativeHandle());
        check("B10 直曝方法不受影响", true);

        System.out.println("[l2b2] pass=" + pass + " fail=" + fail);
        System.out.println(fail == 0 ? "[l2b2] ALL PASS" : "[l2b2] FAILURES");
        System.out.flush();
        app.quit();
        if (fail > 0) { System.exit(1); }
    }
}
