/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.lang.ref.Cleaner;

/**
 * SQL 查询模型（Qt {@code QSqlQueryModel}）：把 SELECT 结果当成只读表格用。
 * <pre>
 * QSqlQueryModel m = new QSqlQueryModel();
 * m.setQuery(db, "SELECT id, name FROM t ORDER BY id");
 * for (int r = 0; r < m.rowCount(); r++)
 *     System.out.println(m.data(r, 0) + " = " + m.data(r, 1));
 * </pre>
 * 与 {@link QSqlQuery} 的区别：查询模型**一次取全部行**并提供行列访问（适合接表格控件），
 * 游标式逐行遍历仍用 {@code QSqlQuery}。
 */
public class QSqlQueryModel {

    private static final Cleaner CLEANER = Cleaner.create();

    protected long nativeHandle;

    /** 创建空模型。 */
    public QSqlQueryModel() {
        this(nativeCreate());
    }

    /** 子类用:以既有原生句柄构造(QSqlTableModel 创建的是 QSqlTableModel 对象)。 */
    protected QSqlQueryModel(long handle) {
        this.nativeHandle = handle;
        final long h = nativeHandle;
        CLEANER.register(this, () -> nativeDispose(h));
    }

    protected static native long nativeCreate();
    protected native void nativeDispose(long handle);

    /** 在默认连接上执行查询。 */
    public void setQuery(String sql) { nativeSetQuery(nativeHandle, 0, sql); }

    /** 在指定连接上执行查询（{@link QSqlDatabase} 句柄）。 */
    public boolean setQuery(QSqlDatabase db, String sql) {
        return nativeSetQuery(nativeHandle, db == null ? 0 : db.nativeHandle(), sql);
    }
    private native boolean nativeSetQuery(long handle, long dbHandle, String sql);

    /** 行数。 */
    public int rowCount() { return nativeRowCount(nativeHandle); }
    private static native int nativeRowCount(long handle);

    /** 列数。 */
    public int columnCount() { return nativeColumnCount(nativeHandle); }
    private static native int nativeColumnCount(long handle);

    /** 单元格文本（NULL 返回空串；越界返回空串）。 */
    public String data(int row, int column) { return nativeData(nativeHandle, row, column); }
    private static native String nativeData(long handle, int row, int column);

    /** 列标题（即字段名）。 */
    public String headerData(int column) { return nativeHeaderData(nativeHandle, column); }
    private static native String nativeHeaderData(long handle, int column);

    /** 最后一行的字段名列表（{@code QSqlQueryModel::record()} 的列名）。 */
    public String[] columnNames() { return nativeColumnNames(nativeHandle); }
    private static native String[] nativeColumnNames(long handle);

    /** 最近一次错误（无错误返回空串）。 */
    public String lastError() { return nativeLastError(nativeHandle); }
    private static native String nativeLastError(long handle);

    /** 刷新（重新执行当前查询）。 */
    public void refresh() { nativeRefresh(nativeHandle); }
    private static native void nativeRefresh(long handle);
}
