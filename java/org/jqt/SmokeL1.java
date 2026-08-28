/*
 * JQt - L1 收尾批 1 冒烟测试（v0.7.1）。
 */
package org.jqt;

import java.util.List;

public class SmokeL1 {

    public static void main(String[] args) {
        System.out.println("[l1] QApplication 创建...");
        QApplication app = new QApplication();
        QMainWindow w = new QMainWindow("SmokeL1", 500, 400);

        // placeholderText
        QLineEdit le = new QLineEdit("");
        le.setPlaceholderText("请输入");
        System.out.println("[l1] QLineEdit.placeholderText = " + le.placeholderText());

        QTextEdit te = new QTextEdit();
        te.setPlaceholderText("多行占位");
        System.out.println("[l1] QTextEdit.placeholderText = " + te.placeholderText());
        te.append("追加一行");
        System.out.println("[l1] QTextEdit.append+toPlainText = " + te.toPlainText().trim());

        // QPushButton icon/shortcut/text
        QPushButton btn = new QPushButton("按钮");
        btn.setShortcut("Ctrl+O");
        System.out.println("[l1] QPushButton.text = " + btn.text());
        System.out.println("[l1] QPushButton.shortcut = " + btn.shortcut());
        btn.setIcon("nonexistent.png");
        System.out.println("[l1] QPushButton.icon = " + btn.icon());

        // QMenu icon
        QMenu menu = new QMenu();
        menu.setIcon("nope.png");
        System.out.println("[l1] QMenu.icon = " + menu.icon());

        // QFile 实例
        QFile f = new QFile();
        boolean ok = f.open("smoke_l1_test.txt", QFile.OpenMode.WRITE_ONLY);
        System.out.println("[l1] QFile.open(write) = " + ok + ", isOpen = " + f.isOpen());
        f.write("hello L1\nsecond line\n");
        f.close();
        QFile fr = new QFile();
        fr.open("smoke_l1_test.txt", QFile.OpenMode.READ_ONLY);
        System.out.println("[l1] QFile.readAll = " + fr.readAll().trim().replace("\n", "|"));
        fr.close();

        // QClipboard pixmap
        byte[] png = new byte[] { (byte)0x89, 0x50, 0x4E, 0x47 };  // 无效 PNG，不崩溃即可
        QClipboard.setPixmap(png);
        byte[] back = QClipboard.pixmap();
        System.out.println("[l1] QClipboard.pixmap = " + (back == null ? "null" : back.length + " bytes"));

        // QSize methods
        QSize sz = new QSize(320, 240);
        System.out.println("[l1] QSize.width()=" + sz.width() + " height()=" + sz.height());

        // QListWidget itemChanged + row
        QListWidget lw = new QListWidget();
        lw.addItem("alpha");
        lw.addItem("beta");
        System.out.println("[l1] QListWidget.row(alpha) = " + lw.row("alpha") + ", row(zzz) = " + lw.row("zzz"));

        // QWidget find/layout/setWindowIcon
        w.setWindowIcon("nope.png");
        System.out.println("[l1] QWidget.setWindowIcon ok, layout() = " + w.layout());
        QVBoxLayout vb = new QVBoxLayout();
        vb.addWidget(btn);
        w.setLayout(vb);
        System.out.println("[l1] QWidget.layout() after setLayout = " + (w.layout() != 0 ? "ok" : "0"));

        // QMessageBox 实例（不弹，仅构造+set）
        QMessageBox mb = new QMessageBox();
        mb.setText("测试");
        mb.setWindowTitle("标题");
        mb.setIcon(QMessageBox.Icon.INFORMATION);
        System.out.println("[l1] QMessageBox 实例构造+set ok");

        // QColorDialog.open（非阻塞，无交互）
        QColorDialog.open(w, "选色", 0xFF0000FF);
        System.out.println("[l1] QColorDialog.open ok");

        w.show();
        app.scheduleQuit(1200);
        app.exec();
        System.out.println("[l1] PASS");
    }
}
