/*
 * JQt - QSqlDatabase 手写批次冒烟（值类型连接句柄）。
 */
package org.jqt;

public class SmokeSqlDb {

    static int pass = 0;
    static int fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("[sql] OK  " + name); }
        else { fail++; System.out.println("[sql] FAIL " + name); }
    }

    public static void main(String[] args) {
        System.out.println("[sql] start");
        QApplication app = new QApplication();

        // 静态：连接存在性（驱动可用性在 addDatabase 之后验证——插件由 workaround 加载）
        check("isDriverAvailable 未加载时为 false（预期）", !QSqlDatabase.isDriverAvailable("SQLITE"));
        check("contains(nonexistent) == false", !QSqlDatabase.contains("nonexistent"));

        // 建连接（内存库）
        QSqlDatabase db = QSqlDatabase.addDatabase("SQLITE", "smoke");
        db.setDatabaseName(":memory:");
        check("isValid() == true", db.isValid());
        check("isOpenError() == false", !db.isOpenError());
        System.out.println("[sql] driverName() = " + db.driverName());
        check("isDriverAvailable(SQLITE) after addDatabase", QSqlDatabase.isDriverAvailable("SQLITE"));
        check("userName() 空（未设置）", "".equals(db.userName()));
        check("contains(smoke) == true", QSqlDatabase.contains("smoke"));

        boolean opened = db.open();
        check("open() == true", opened);
        check("transaction() == true", db.transaction());

        // 事务内执行 SQL
        QSqlQuery q = db.exec("CREATE TABLE t (id INTEGER)");
        check("exec create == true", q != null);

        db.dispose();
        System.out.println("[sql] pass=" + pass + " fail=" + fail);
        System.out.println(fail == 0 ? "[sql] ALL PASS ✅" : "[sql] FAILURES ❌");
        app.quit();
    }
}
