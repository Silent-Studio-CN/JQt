/*
 * JQt - SQL 模型冒烟（v1.9.1 P1）：QSqlQueryModel + QSqlTableModel。
 * 用内存 SQLite（与 SmokeV072 同一套驱动），全部断言确定可复现。
 */
package org.jqt;

public class SmokeSqlModel {

    static int pass = 0;
    static int fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("[sqlm] OK   " + name); }
        else { fail++; System.out.println("[sqlm] FAIL " + name); }
    }

    public static void main(String[] args) {
        System.out.println("[sqlm] start");
        QApplication app = new QApplication();

        // SQLite 驱动是否随 Qt 提供因平台而异(CI 的 Linux/macOS 包就没有 qsqlite 插件),
        // 拿不到就优雅 SKIP —— 与 SmokeV072 的容错策略一致。
        QSqlDatabase db;
        try {
            db = QSqlDatabase.addDatabase("SQLITE");
        } catch (IllegalStateException e) {
            System.out.println("[sqlm] SKIP 本平台无 SQLite 驱动: " + e.getMessage());
            System.out.println("[sqlm] ALL PASS ✅ (跳过)");
            return;
        }
        db.setDatabaseName(":memory:");
        if (!db.open()) {
            System.out.println("[sqlm] SKIP SQLite 打开失败: " + db.lastError());
            System.out.println("[sqlm] ALL PASS ✅ (跳过)");
            return;
        }
        db.exec("CREATE TABLE t (id INTEGER PRIMARY KEY, name TEXT)");
        db.exec("INSERT INTO t (name) VALUES ('甲')");
        db.exec("INSERT INTO t (name) VALUES ('乙')");
        db.exec("INSERT INTO t (name) VALUES ('丙')");

        // ---------------- QSqlQueryModel:只读查询 ----------------
        QSqlQueryModel qm = new QSqlQueryModel();
        check("setQuery 成功", qm.setQuery(db, "SELECT id, name FROM t ORDER BY id"));
        check("行数 = 3(实际 " + qm.rowCount() + ")", qm.rowCount() == 3);
        check("列数 = 2(实际 " + qm.columnCount() + ")", qm.columnCount() == 2);
        check("首行首列 = 1(实际 " + qm.data(0, 0) + ")", "1".equals(qm.data(0, 0)));
        check("首行次列 = 甲(实际 " + qm.data(0, 1) + ")", "甲".equals(qm.data(0, 1)));
        check("末行次列 = 丙(实际 " + qm.data(2, 1) + ")", "丙".equals(qm.data(2, 1)));
        check("列标题 = id(实际 " + qm.headerData(0) + ")", "id".equals(qm.headerData(0)));
        check("列标题 = name(实际 " + qm.headerData(1) + ")", "name".equals(qm.headerData(1)));
        String[] cols = qm.columnNames();
        check("columnNames = [id, name](实际 " + String.join(",", cols) + ")",
              cols.length == 2 && "id".equals(cols[0]) && "name".equals(cols[1]));
        check("越界访问返回空串", "".equals(qm.data(99, 0)) && "".equals(qm.data(0, 99)));
        check("无错误(lastError 为空)", qm.lastError().isEmpty());

        // 刷新:插一行后 refresh 应看到 4 行
        db.exec("INSERT INTO t (name) VALUES ('丁')");
        qm.refresh();
        check("refresh 后行数 = 4(实际 " + qm.rowCount() + ")", qm.rowCount() == 4);

        // ---------------- QSqlTableModel:可写 ----------------
        QSqlTableModel tm = new QSqlTableModel(db);   // 显式绑定连接
        tm.setTable("t");
        check("setTable 生效(实际 " + tm.tableName() + ")", "t".equals(tm.tableName()));
        boolean sel = tm.select();
        check("select() 成功(失败原因: '" + tm.lastError() + "')", sel);
        check("表模型行数 = 4(实际 " + tm.rowCount() + ")", tm.rowCount() == 4);
        check("初始 isDirty = false", !tm.isDirty());

        check("setData 改内存", tm.setData(0, 1, "甲改了"));
        check("改后 isDirty = true", tm.isDirty());
        check("内存中已变(实际 " + tm.data(0, 1) + ")", "甲改了".equals(tm.data(0, 1)));

        // revertAll:撤销
        tm.revertAll();
        check("revertAll 后回到原值(实际 " + tm.data(0, 1) + ")", "甲".equals(tm.data(0, 1)));

        // 改 + submitAll:落库
        tm.setData(0, 1, "甲提交");
        check("submitAll 成功", tm.submitAll());
        check("提交后 isDirty = false", !tm.isDirty());
        QSqlQueryModel verify = new QSqlQueryModel();
        verify.setQuery(db, "SELECT name FROM t WHERE id = 1");
        check("数据库里确实变了(实际 " + verify.data(0, 0) + ")", "甲提交".equals(verify.data(0, 0)));

        // 插入 + 提交
        int before = tm.rowCount();
        check("insertRow 成功(追加到末尾)", tm.insertRow(tm.rowCount()));
        check("插入后行数 +1(实际 " + tm.rowCount() + " vs " + before + ")", tm.rowCount() == before + 1);
        tm.revertAll();     // 撤回这次插入,避免影响后续断言

        // 删除 + 提交
        check("removeRow 成功", tm.removeRow(3));
        check("删除后 submitAll 成功", tm.submitAll());
        QSqlQueryModel afterDel = new QSqlQueryModel();
        afterDel.setQuery(db, "SELECT COUNT(*) FROM t");
        check("删除已落库(COUNT 实际 " + afterDel.data(0, 0) + ")", "3".equals(afterDel.data(0, 0)));

        db.close();
        System.out.println("[sqlm] pass=" + pass + " fail=" + fail);
        System.out.println("[sqlm] " + (fail == 0 ? "ALL PASS ✅" : ("FAILED: " + fail)));
        if (fail > 0) System.exit(1);
    }
}
