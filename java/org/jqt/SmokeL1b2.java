/*
 * JQt - L1 收尾批 2 冒烟测试（v0.7.1：QAction/QDialog/QMenuBar/QListView）。
 */
package org.jqt;

import java.util.List;

public class SmokeL1b2 {

    public static void main(String[] args) {
        System.out.println("[l1b2] start");
        QApplication app = new QApplication();
        QMainWindow w = new QMainWindow("SmokeL1b2", 500, 400);

        // QAction
        QAction act = new QAction("保存");
        act.setShortcut("Ctrl+S");
        act.setToolTip("保存文件");
        act.setCheckable(true);
        act.setChecked(true);
        System.out.println("[l1b2] QAction text=" + act.text() + " shortcut=" + act.shortcut()
            + " toolTip=" + act.toolTip() + " checked=" + act.isChecked());
        final int[] trig = { 0 };
        final boolean[] tog = { false };
        act.onTriggered(() -> trig[0]++);
        act.onToggled(v -> tog[0] = v);
        act.toggle();
        act.trigger();
        System.out.println("[l1b2] QAction toggle->checked=" + act.isChecked() + " toggled=" + tog[0] + " triggered=" + trig[0]);
        act.setIcon("a.png");
        System.out.println("[l1b2] QAction icon=" + act.icon());

        // QMenuBar
        QMenuBar bar = new QMenuBar();
        QMenu fileMenu = bar.addMenu("文件");
        int openId = fileMenu.addItem("打开");
        int quitId = fileMenu.addItem("退出");
        final int[] triggeredId = { -1 };
        bar.onTriggered(id -> triggeredId[0] = id);
        System.out.println("[l1b2] QMenuBar addMenu ok (openId=" + openId + " quitId=" + quitId + ")");
        bar.clear();
        System.out.println("[l1b2] QMenuBar.clear ok");

        // QDialog（不阻塞 exec，用 open + accept/reject）
        QDialog dlg = new QDialog("对话框", w.nativeHandle());
        dlg.resize(300, 200);
        dlg.open();
        System.out.println("[l1b2] QDialog.open ok");
        dlg.accept();
        System.out.println("[l1b2] QDialog.accept ok");

        // QListView
        QListView lv = new QListView();
        lv.addItem("甲");
        lv.addItem("乙");
        lv.addItem("丙");
        lv.setSpacing(6);
        lv.setWordWrap(true);
        System.out.println("[l1b2] QListView count=" + lv.count() + " spacing=" + lv.spacing()
            + " wordWrap=" + lv.wordWrap() + " item(1)=" + lv.item(1));
        final int[] selCount = { 0 };
        lv.onSelectionChanged(items -> selCount[0] = items.size());
        lv.setCurrentItem("乙");
        System.out.println("[l1b2] QListView currentItem=" + lv.currentItem());
        List<String> items = new java.util.ArrayList<>();
        items.add("x1"); items.add("x2"); items.add("x3");
        lv.setItems(items);
        System.out.println("[l1b2] QListView setItems count=" + lv.count() + " item(2)=" + lv.item(2));
        lv.clear();
        System.out.println("[l1b2] QListView clear count=" + lv.count());

        w.show();
        app.scheduleQuit(1000);
        app.exec();
        System.out.println("[l1b2] PASS");
    }
}
