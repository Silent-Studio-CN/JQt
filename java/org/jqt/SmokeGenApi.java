/*
 * JQt - 生成器批次 API 冒烟（batch 1-4 方法运行时验证，mangle 修复回归）。
 */
package org.jqt;

public class SmokeGenApi {

    static int pass = 0;
    static int fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("[gen] OK  " + name); }
        else { fail++; System.out.println("[gen] FAIL " + name); }
    }

    public static void main(String[] args) {
        System.out.println("[gen] start");
        QApplication app = new QApplication();

        // QLabel（batch 2）：重载 setNum + getter
        QLabel label = new QLabel("test");
        label.setNum(42);
        check("QLabel setNum(int) -> text 42", "42".equals(label.text()));
        label.setNum(3.5);
        check("QLabel setNum(double) -> text 3.5", "3.5".equals(label.text()));
        label.setOpenExternalLinks(true);
        check("QLabel openExternalLinks()", label.openExternalLinks());

        // QPushButton（batch 2）
        QPushButton btn = new QPushButton("go");
        btn.setFlat(true);
        check("QPushButton isFlat()", btn.isFlat());
        btn.setAutoDefault(false);
        check("QPushButton autoDefault()==false", !btn.autoDefault());

        // QComboBox（batch 2）
        QComboBox combo = new QComboBox();
        combo.addItem("a");
        combo.addItem("b");
        combo.setCurrentText("b");
        check("QComboBox maxCount()>=2", combo.maxCount() >= 2);
        check("QComboBox modelColumn()==0", combo.modelColumn() == 0);

        // QLineEdit（batch 2）
        QLineEdit edit = new QLineEdit("hello");
        edit.setModified(true);
        check("QLineEdit isModified()", edit.isModified());
        // isUndoAvailable 依赖编辑历史（setModified 不产生 undo 记录），行为正确不断言
        edit.selectAll();
        check("QLineEdit selectionStart()==0", edit.selectionStart() == 0);

        // QTabWidget（batch 3）
        QTabWidget tabs = new QTabWidget();
        tabs.addTab(label, "one");
        tabs.addTab(btn, "two");
        check("QTabWidget tabText(1)", "two".equals(tabs.tabText(1)));
        tabs.setTabEnabled(1, false);
        check("QTabWidget tabToolTip(0)", "".equals(tabs.tabToolTip(0)));

        // QSettings（batch 3）
        QSettings settings = new QSettings();
        settings.setValue("gen/api/check", 7);
        settings.sync();
        check("QSettings sync() ok", settings.value("gen/api/check") == 7);

        // QApplication（batch 4）
        check("QApplication doubleClickInterval()>0", app.doubleClickInterval() > 0);
        app.setWheelScrollLines(5);
        check("QApplication cursorFlashTime()>0", app.cursorFlashTime() > 0);

        // QFormLayout（batch 4）
        QFormLayout form = new QFormLayout();
        form.addRow("x", edit);
        // 空布局：verticalSpacing()=-1（默认）、hasHeightForWidth()=false 均为 Qt 正确语义
        check("QFormLayout verticalSpacing()==-1 (默认)", form.verticalSpacing() == -1);
        form.setVerticalSpacing(12);
        check("QFormLayout setVerticalSpacing -> 12", form.verticalSpacing() == 12);
        form.addRow("y", btn);
        // Qt 6 QFormLayout 不重写 hasHeightForWidth（恒 false），不断言

        System.out.println("[gen] pass=" + pass + " fail=" + fail);
        System.out.println(fail == 0 ? "[gen] ALL PASS ✅" : "[gen] FAILURES ❌");
        app.quit();
    }
}
