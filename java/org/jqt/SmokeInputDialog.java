/*
 * JQt - QInputDialog 实例模式冒烟（v0.8.0：配置后 exec 的自定义对话框）。
 */
package org.jqt;

public class SmokeInputDialog {

    static int pass = 0;
    static int fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("[idlg] OK  " + name); }
        else { fail++; System.out.println("[idlg] FAIL " + name); }
    }

    public static void main(String[] args) {
        System.out.println("[idlg] start");
        QApplication app = new QApplication();

        QInputDialog dlg = new QInputDialog();
        dlg.setLabelText("积分数量");
        dlg.setOkButtonText("确认");
        dlg.setCancelButtonText("取消");
        dlg.setIntRange(1, 100);
        dlg.setIntValue(50);
        dlg.setDoubleRange(0.5, 99.5);
        dlg.setDoubleValue(3.25);
        dlg.setTextValue("abc");
        dlg.setComboBoxEditable(true);

        check("labelText()", "积分数量".equals(dlg.labelText()));
        check("okButtonText()", "确认".equals(dlg.okButtonText()));
        check("cancelButtonText()", "取消".equals(dlg.cancelButtonText()));
        check("intMinimum()==1", dlg.intMinimum() == 1);
        check("intMaximum()==100", dlg.intMaximum() == 100);
        check("intValue()==50", dlg.intValue() == 50);
        check("doubleMinimum()==0.5", dlg.doubleMinimum() == 0.5);
        check("doubleMaximum()==99.5", dlg.doubleMaximum() == 99.5);
        check("doubleValue()==3.25", dlg.doubleValue() == 3.25);
        check("textValue()==abc", "abc".equals(dlg.textValue()));
        check("isComboBoxEditable()", dlg.isComboBoxEditable());

        // 非阻塞显示 + 隐藏（不 exec，避免卡自动化）
        dlg.setVisible(true);
        check("setVisible(true) ok", true);
        dlg.setVisible(false);
        check("setVisible(false) ok", true);

        dlg.dispose();
        System.out.println("[idlg] pass=" + pass + " fail=" + fail);
        System.out.println(fail == 0 ? "[idlg] ALL PASS ✅" : "[idlg] FAILURES ❌");
        app.quit();
    }
}
