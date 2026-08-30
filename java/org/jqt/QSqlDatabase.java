/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 数据库连接（QSqlDatabase，Qt6Sql）。
 * <p>
 * v0.7.2 工业模块。驱动：SQLITE（内置插件）/ PSQL / MYSQL（需 Qt 对应插件）。
 * <pre>
 * QSqlDatabase db = QSqlDatabase.addDatabase("SQLITE");
 * db.setDatabaseName("app.db");
 * if (db.open()) {
 *     QSqlQuery q = db.exec("SELECT id, name FROM users");
 *     while (q.next()) { System.out.println(q.value(0) + " " + q.value(1)); }
 * }
 * </pre>
 */
public class QSqlDatabase {

    private static final java.util.List<QSqlDatabase> connections = new java.util.ArrayList<>();
    private static long nextId = 1;

    /** 默认连接名（QSqlDatabase::defaultConnection）。 */
    public static final String DEFAULT_CONNECTION = "";

    private final long nativeHandle;
    private final String connectionName;
    private boolean disposed;

    private QSqlDatabase(long handle, String name) {
        this.nativeHandle = handle;
        this.connectionName = name;
    }

    /**
     * 添加数据库连接（QSqlDatabase::addDatabase）。
     * 同一 driver 只允许一个默认连接；重复调用返回同一连接（Qt 语义）。
     * 驱动：SQLITE / PSQL / MYSQL 等。
     */
    public static QSqlDatabase addDatabase(String driver) {
        return addDatabase(driver, DEFAULT_CONNECTION);
    }

    /** 添加命名连接（connectionName 为空 = 默认连接）。 */
    public static QSqlDatabase addDatabase(String driver, String connectionName) {
        for (QSqlDatabase c : connections) {
            if (c.connectionName.equals(connectionName)) {
                return c;
            }
        }
        long h = nativeAddDatabase(driver, connectionName);
        if (h == 0) {
            throw new IllegalStateException("JQt: 数据库驱动不可用: " + driver
                + "（SQLITE 需部署 plugins/sqldrivers/qsqlite；PSQL/MYSQL 需对应 Qt 插件）");
        }
        QSqlDatabase db = new QSqlDatabase(h, connectionName);
        connections.add(db);
        return db;
    }

    private static native long nativeAddDatabase(String driver, String connectionName);

    /** 数据库文件路径（SQLITE）或库名（PSQL/MYSQL）。 */
    public void setDatabaseName(String name) {
        nativeSetDatabaseName(nativeHandle, name);
    }

    /** 用户名。 */
    public void setUserName(String user) {
        nativeSetUserName(nativeHandle, user);
    }

    /** 密码。 */
    public void setPassword(String password) {
        nativeSetPassword(nativeHandle, password);
    }

    /** 主机名（网络数据库）。 */
    public void setHostName(String host) {
        nativeSetHostName(nativeHandle, host);
    }

    /** 端口（网络数据库）。 */
    public void setPort(int port) {
        nativeSetPort(nativeHandle, port);
    }

    /** 打开连接，成功返回 true。 */
    public boolean open() {
        return nativeOpen(nativeHandle);
    }

    /** 关闭连接。 */
    public void close() {
        nativeClose(nativeHandle);
    }

    /** 是否已打开。 */
    public boolean isOpen() {
        return nativeIsOpen(nativeHandle);
    }

    /** 执行 SQL，返回结果集（SELECT）或状态查询（INSERT/UPDATE）。 */
    public QSqlQuery exec(String sql) {
        long h = nativeExec(nativeHandle, sql);
        if (h == 0) {
            throw new IllegalStateException("JQt: SQL 执行失败: " + lastError());
        }
        return new QSqlQuery(h);
    }

    /** 最近一次错误信息（无错误返回空串）。 */
    public String lastError() {
        return nativeLastError(nativeHandle);
    }

    /** 释放 C++ 连接（一般无需调用）。 */
    public void dispose() {
        if (!disposed) {
            disposed = true;
            nativeDispose(nativeHandle);
            connections.remove(this);
        }
    }

    private native void nativeSetDatabaseName(long handle, String name);
    private native void nativeSetUserName(long handle, String user);
    private native void nativeSetPassword(long handle, String password);
    private native void nativeSetHostName(long handle, String host);
    private native void nativeSetPort(long handle, int port);
    private native boolean nativeOpen(long handle);
    private native void nativeClose(long handle);
    private native boolean nativeIsOpen(long handle);
    private native long nativeExec(long handle, String sql);
    private native String nativeLastError(long handle);
    private native void nativeDispose(long handle);

    // ---- 手写批次（值类型连接句柄 → jqtSqlDb 查表，非生成器模板）----

    /** 连接是否存在（QSqlDatabase::contains）。 */
    public static boolean contains(String connectionName) {
        return nativeContains(connectionName);
    }

    /** 驱动是否可用（QSqlDatabase::isDriverAvailable）。 */
    public static boolean isDriverAvailable(String driver) {
        return nativeIsDriverAvailable(driver);
    }

    /** 驱动名（QSqlDatabase::driverName）。 */
    public String driverName() {
        return nativeDriverName(nativeHandle);
    }

    /** 打开时是否出错（QSqlDatabase::isOpenError）。 */
    public boolean isOpenError() {
        return nativeIsOpenError(nativeHandle);
    }

    /** 连接是否有效（QSqlDatabase::isValid）。 */
    public boolean isValid() {
        return nativeIsValid(nativeHandle);
    }

    /** 开启事务（QSqlDatabase::transaction）。 */
    public boolean transaction() {
        return nativeTransaction(nativeHandle);
    }

    /** 用户名（QSqlDatabase::userName）。 */
    public String userName() {
        return nativeUserName(nativeHandle);
    }

    private static native boolean nativeContains(String connectionName);
    private static native boolean nativeIsDriverAvailable(String driver);
    private native String nativeDriverName(long handle);
    private native boolean nativeIsOpenError(long handle);
    private native boolean nativeIsValid(long handle);
    private native boolean nativeTransaction(long handle);
    private native String nativeUserName(long handle);
}
