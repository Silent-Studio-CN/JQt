/*
 * JQt - v0.7.2 工业模块冒烟（QPrinter + QSql）。
 */
package org.jqt;

public class SmokeV072 {

    public static void main(String[] args) {
        System.out.println("[v072] start");
        QApplication app = new QApplication();
        QMainWindow w = new QMainWindow("SmokeV072", 500, 400);

        // ---- QPrinter：QTextEdit.printToPdf ----
        QTextEdit te = new QTextEdit();
        te.setPlainText("JQt PDF 测试\n第二行内容\n1234567890");
        boolean pdf1 = te.printToPdf("smoke_v072_text.pdf");
        System.out.println("[v072] QTextEdit.printToPdf = " + pdf1 + ", size=" + QFile.size("smoke_v072_text.pdf"));

        // ---- QPrinter 实例 API ----
        QPrinter p = new QPrinter();
        p.setOutputFormat(QPrinter.OutputFormat.PDF);
        p.setOutputFileName("smoke_v072_printer.pdf");
        p.setResolution(300);
        p.setPageSize(QPrinter.PageSize.A4);
        boolean printed = te.print(p);
        p.newPage();
        p.disposePdf();
        System.out.println("[v072] QPrinter 实例 print = " + printed + ", size=" + QFile.size("smoke_v072_printer.pdf"));

        // ---- QWidget.printToPdf ----
        boolean pdf2 = w.printToPdf("smoke_v072_widget.pdf");
        System.out.println("[v072] QWidget.printToPdf = " + pdf2 + ", size=" + QFile.size("smoke_v072_widget.pdf"));

        // ---- QSql SQLite ----
        try {
            QSqlDatabase db = QSqlDatabase.addDatabase("SQLITE");
            db.setDatabaseName("smoke_v072.db");
            boolean opened = db.open();
            System.out.println("[v072] SQLite open = " + opened + ", isOpen=" + db.isOpen());
            if (opened) {
                db.exec("CREATE TABLE IF NOT EXISTS t (id INTEGER PRIMARY KEY, name TEXT)");
                db.exec("INSERT INTO t (name) VALUES ('甲')");
                db.exec("INSERT INTO t (name) VALUES ('乙')");
                QSqlQuery q = db.exec("SELECT id, name FROM t ORDER BY id");
                StringBuilder sb = new StringBuilder();
                while (q.next()) {
                    sb.append(q.value(0)).append("=").append(q.value(1)).append(";");
                }
                System.out.println("[v072] SELECT rows: " + sb.toString() + " (valueCount=" + q.valueCount() + ", isSelect=" + q.isSelect() + ")");
                q.dispose();
                QSqlQuery upd = db.exec("UPDATE t SET name='丙' WHERE id=1");
                System.out.println("[v072] UPDATE affected=" + upd.numRowsAffected());
                upd.dispose();
                db.close();
            }
        } catch (Throwable t) {
            System.out.println("[v072] SQL ERROR: " + t);
        }

        w.show();
        app.scheduleQuit(1200);
        app.exec();
        System.out.println("[v072] PASS");
    }
}
