/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * SQL 查询结果（QSqlQuery，Qt6Sql）。
 * <p>
 * v0.7.2 工业模块。迭代结果集：
 * <pre>
 * QSqlQuery q = db.exec("SELECT id, name FROM users");
 * while (q.next()) {
 *     System.out.println(q.value(0) + " / " + q.value(1));
 * }
 * </pre>
 */
public class QSqlQuery {

    private long nativeHandle;
    private boolean disposed;

    QSqlQuery(long handle) {
        this.nativeHandle = handle;
    }

    /** 移到下一行（无更多行返回 false）。 */
    public boolean next() {
        return nativeNext(nativeHandle);
    }

    /** 当前行列数。 */
    public int valueCount() {
        return nativeValueCount(nativeHandle);
    }

    /** 当前行指定列的值（字符串形式；NULL 返回 null）。 */
    public String value(int index) {
        return nativeValue(nativeHandle, index);
    }

    /** 是否为 SELECT 查询。 */
    public boolean isSelect() {
        return nativeIsSelect(nativeHandle);
    }

    /** 影响行数（INSERT/UPDATE/DELETE；SELECT 返回 -1）。 */
    public int numRowsAffected() {
        return nativeNumRowsAffected(nativeHandle);
    }

    /** 最近一次错误信息。 */
    public String lastError() {
        return nativeLastError(nativeHandle);
    }

    /** 释放 C++ 查询对象。 */
    public void dispose() {
        if (!disposed) {
            disposed = true;
            nativeDispose(nativeHandle);
            nativeHandle = 0;
        }
    }

    private native boolean nativeNext(long handle);
    private native int nativeValueCount(long handle);
    private native String nativeValue(long handle, int index);
    private native boolean nativeIsSelect(long handle);
    private native int nativeNumRowsAffected(long handle);
    private native String nativeLastError(long handle);
    private native void nativeDispose(long handle);
}
