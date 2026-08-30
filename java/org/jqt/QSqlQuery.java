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

// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** at（Qt at）。 */
    public int at() {
        return nativeAt(nativeHandle);
    }
    private static native int nativeAt(long nativeHandle);

    /** first（Qt first）。 */
    public boolean first() {
        return nativeFirst(nativeHandle);
    }
    private static native boolean nativeFirst(long nativeHandle);

    /** last（Qt last）。 */
    public boolean last() {
        return nativeLast(nativeHandle);
    }
    private static native boolean nativeLast(long nativeHandle);

    /** lastQuery（Qt lastQuery）。 */
    public String lastQuery() {
        return nativeLastQuery(nativeHandle);
    }
    private static native String nativeLastQuery(long nativeHandle);

    /** nextResult（Qt nextResult）。 */
    public boolean nextResult() {
        return nativeNextResult(nativeHandle);
    }
    private static native boolean nativeNextResult(long nativeHandle);

    /** prepare（Qt prepare）。 */
    public boolean prepare(String arg0) {
        return nativePrepare(nativeHandle, arg0);
    }
    private static native boolean nativePrepare(long nativeHandle, String arg0);

    /** setForwardOnly（Qt setForwardOnly）。 */
    public void setForwardOnly(boolean arg0) {
        nativeSetForwardOnly(nativeHandle, arg0);
    }
    private static native void nativeSetForwardOnly(long nativeHandle, boolean arg0);

    /** setPositionalBindingEnabled（Qt setPositionalBindingEnabled）。 */
    public void setPositionalBindingEnabled(boolean arg0) {
        nativeSetPositionalBindingEnabled(nativeHandle, arg0);
    }
    private static native void nativeSetPositionalBindingEnabled(long nativeHandle, boolean arg0);

    /** size（Qt size）。 */
    public int size() {
        return nativeSize(nativeHandle);
    }
    private static native int nativeSize(long nativeHandle);

}