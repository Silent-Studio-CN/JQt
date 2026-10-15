/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * DBus 连接（Qt {@code QDBusConnection}，v1.9.2 P1 收尾）。
 *
 * <p>桥内部其实早就在用 QtDBus（Linux 桌面集成：屏保抑制、桌面通知），这个类把**通用面**
 * 开放给 Java —— 会话总线 / 系统总线的连接状态、方法调用与信号发送。
 *
 * <pre>
 * if (QDBusConnection.isAvailable()) {
 *     System.out.println("会话总线已连接 = " + QDBusConnection.isSessionBusConnected());
 *     System.out.println("本连接名 = " + QDBusConnection.sessionBusName());
 *
 *     QDBusMessage reply = QDBusConnection.call(
 *             "org.freedesktop.DBus", "/org/freedesktop/DBus",
 *             "org.freedesktop.DBus", "ListNames");
 *     if (reply != null &amp;&amp; !reply.isError()) {
 *         System.out.println(reply.stringAt(0));       // 总线上的服务名数组按字符串读
 *     }
 * }
 * </pre>
 *
 * <b>平台差异（如实说明，不含糊）</b>：
 * <ul>
 *   <li>Linux：有会话总线时功能完整（CI 上通过 {@code dbus-run-session} 起一个真实总线做端到端验证）；</li>
 *   <li>macOS / Windows：QtDBus 模块在，但默认**没有会话总线**（Qt 官方不随系统提供 dbus-daemon）——
 *       此时 {@link #isSessionBusConnected()} 为 false，调用会返回无效消息，属预期而非失败；</li>
 *   <li>Android：无 QtDBus 模块 → {@link #isAvailable()} 为 false。</li>
 * </ul>
 */
public final class QDBusConnection {

    private QDBusConnection() {}

    /** DBus 能力是否可用（编译时是否探测到 QtDBus）。 */
    public static boolean isAvailable() { return nativeAvailable(); }

    /** 会话总线是否已连接。 */
    public static boolean isSessionBusConnected() { return nativeIsConnected(false); }

    /** 系统总线是否已连接。 */
    public static boolean isSystemBusConnected() { return nativeIsConnected(true); }

    /** 本进程在会话总线上的唯一连接名（如 {@code :1.42}；未连接时为空串）。 */
    public static String sessionBusName() { return nativeName(false); }

    /** 本进程在系统总线上的唯一连接名（未连接时为空串）。 */
    public static String systemBusName() { return nativeName(true); }

    // ------------------------------------------------------------------ 调用
    /** 在会话总线上同步调用（等价 Qt {@code QDBusConnection::call}）。 */
    public static QDBusMessage call(QDBusMessage message) { return call(message, false); }

    /** 在指定总线上同步调用；返回 {@code null} 表示调用失败（消息无效）。 */
    public static QDBusMessage call(QDBusMessage message, boolean systemBus) {
        if (message == null || !message.isValid()) return null;
        long h = nativeCall(systemBus, message.handle());
        return h == 0 ? null : QDBusMessage.wrap(h);
    }

    /** 便捷：直接构造并调用一个无参方法。 */
    public static QDBusMessage call(String service, String path, String iface, String method) {
        return call(QDBusMessage.createMethodCall(service, path, iface, method));
    }

    /** 发送信号（不等待回复）。 */
    public static boolean send(QDBusMessage message) { return send(message, false); }

    /** 在指定总线上发送信号。 */
    public static boolean send(QDBusMessage message, boolean systemBus) {
        if (message == null || !message.isValid()) return false;
        return nativeSend(systemBus, message.handle());
    }

    // ------------------------------------------------------------------ native
    private static native boolean nativeAvailable();
    private static native boolean nativeIsConnected(boolean systemBus);
    private static native String nativeName(boolean systemBus);
    private static native long nativeCall(boolean systemBus, long messageHandle);
    private static native boolean nativeSend(boolean systemBus, long messageHandle);
}
