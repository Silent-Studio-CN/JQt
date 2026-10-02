/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * SQL 表模型（Qt {@code QSqlTableModel}）：可读可写的单表模型。
 * <pre>
 * QSqlTableModel m = new QSqlTableModel();
 * m.setTable("t");
 * m.select();
 * m.setData(0, 1, "new-name");     // 改内存中的行
 * m.submitAll();                    // 写回数据库
 * </pre>
 * 适合直接接表格控件做"浏览 + 编辑 + 提交"，比手写 UPDATE 语句省事。
 */
public class QSqlTableModel extends QSqlQueryModel {

    /** 创建表模型（默认连接）。 */
    public QSqlTableModel() {
        super(nativeCreateTable());     // 必须建 QSqlTableModel 本体,否则 setTable 无效
    }
    private static native long nativeCreateTable();

    /**
     * 创建表模型并**绑定指定连接**。
     * <p>多连接场景下务必用这个构造器:{@code new QSqlTableModel(db)} ——
     * 无参构造器走 Qt 的默认连接,而 JQt 的 {@link QSqlDatabase} 句柄可能对应
     * 非默认连接,直接 select() 会报 "Unable to find table"。
     */
    public QSqlTableModel(QSqlDatabase db) {
        super(nativeCreateTableWithDb(db == null ? 0 : db.nativeHandle()));
    }
    private static native long nativeCreateTableWithDb(long dbHandle);


    /** 绑定表名（随后调用 {@link #select()} 才会取数据）。 */
    public void setTable(String table) { nativeSetTable(nativeHandle, table); }
    private native void nativeSetTable(long handle, String table);

    /** 当前表名。 */
    public String tableName() { return nativeTableName(nativeHandle); }
    private static native String nativeTableName(long handle);

    /** 取数据（{@code QSqlTableModel::select}）。 */
    public boolean select() { return nativeSelect(nativeHandle); }
    private static native boolean nativeSelect(long handle);

    /** 修改单元格（只改内存；{@link #submitAll()} 才落库）。 */
    public boolean setData(int row, int column, String value) {
        return nativeSetData(nativeHandle, row, column, value);
    }
    private static native boolean nativeSetData(long handle, int row, int column, String value);

    /** 提交所有待写改动（内部事务）。 */
    public boolean submitAll() { return nativeSubmitAll(nativeHandle); }
    private static native boolean nativeSubmitAll(long handle);

    /** 撤销所有未提交改动。 */
    public void revertAll() { nativeRevertAll(nativeHandle); }
    private static native void nativeRevertAll(long handle);

    /** 是否有未提交改动。 */
    public boolean isDirty() { return nativeIsDirty(nativeHandle); }
    private static native boolean nativeIsDirty(long handle);

    /** 插入空行（position 为插入位置，-1 追加到末尾）。 */
    public boolean insertRow(int position) { return nativeInsertRow(nativeHandle, position); }
    private static native boolean nativeInsertRow(long handle, int position);

    /** 删除行（同样需要 {@link #submitAll()} 才落库）。 */
    public boolean removeRow(int row) { return nativeRemoveRow(nativeHandle, row); }
    private static native boolean nativeRemoveRow(long handle, int row);
}
