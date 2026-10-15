/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.lang.ref.Cleaner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DBus 消息（Qt {@code QDBusMessage}，v1.9.2 P1 收尾）。
 * <p>这是**值类型**（不是 QObject）—— 生命周期由 {@link Cleaner} 管理，用 {@link #dispose()} 可提前释放。
 * <pre>
 * QDBusMessage call = QDBusMessage.createMethodCall(
 *         "org.freedesktop.DBus", "/org/freedesktop/DBus",
 *         "org.freedesktop.DBus", "NameHasOwner");
 * call.addArgument("org.freedesktop.DBus");
 * QDBusMessage reply = QDBusConnection.call(call);
 * if (reply != null &amp;&amp; !reply.isError()) {
 *     System.out.println("在总线上 = " + reply.boolAt(0));
 * }
 * </pre>
 * <b>参数类型子集</b>：当前支持 String / int / long / double / boolean（够做方法调用与信号）；
 * 读取回复时提供 {@code stringAt / intAt / doubleAt / boolAt}，未知类型可用
 * {@link #argumentTypeAt(int)} 查看类型名再走 {@link #stringAt(int)} 兜底。
 */
public final class QDBusMessage {

    /** 消息类型（与 Qt {@code QDBusMessage::MessageType} 数值一致）。 */
    public static final int InvalidMessage = 0;
    public static final int MethodCallMessage = 1;
    public static final int ReplyMessage = 2;
    public static final int ErrorMessage = 3;
    public static final int SignalMessage = 4;

    private static final Cleaner CLEANER = Cleaner.create();

    private long handle;

    private QDBusMessage(long handle) {
        this.handle = handle;
        final long h = handle;
        CLEANER.register(this, () -> nativeDispose(h));
    }

    /** 由原生侧返回的句柄包装（包内使用）。 */
    static QDBusMessage wrap(long handle) {
        return new QDBusMessage(handle);
    }

    /** DBus 能力是否可用（未探测到 QtDBus 时为 false）。 */
    public static boolean isAvailable() { return QDBusConnection.isAvailable(); }

    /** 构造一个方法调用消息。 */
    public static QDBusMessage createMethodCall(String service, String path, String iface, String method) {
        long h = nativeCreateMethodCall(nz(service), nz(path), nz(iface), nz(method));
        return h == 0 ? null : new QDBusMessage(h);
    }

    /** 构造一个信号消息（配合 {@link QDBusConnection#send(QDBusMessage)} 发出）。 */
    public static QDBusMessage createSignal(String path, String iface, String name) {
        long h = nativeCreateSignal(nz(path), nz(iface), nz(name));
        return h == 0 ? null : new QDBusMessage(h);
    }

    private static String nz(String s) { return s == null ? "" : s; }

    // ------------------------------------------------------------------ 参数
    /** 追加字符串参数。 */
    public QDBusMessage addArgument(String value) { nativeAddString(handle, nz(value)); return this; }
    /** 追加 32 位整型参数。 */
    public QDBusMessage addArgument(int value) { nativeAddInt(handle, value); return this; }
    /** 追加 64 位整型参数。 */
    public QDBusMessage addArgument(long value) { nativeAddLong(handle, value); return this; }
    /** 追加双精度参数。 */
    public QDBusMessage addArgument(double value) { nativeAddDouble(handle, value); return this; }
    /** 追加布尔参数。 */
    public QDBusMessage addArgument(boolean value) { nativeAddBool(handle, value); return this; }

    /** 参数个数。 */
    public int argumentCount() { return nativeArgumentCount(handle); }

    /** 第 i 个参数的类型名（如 {@code QString} / {@code int}），用于排查未知类型。 */
    public String argumentTypeAt(int index) { return nativeArgumentTypeAt(handle, index); }

    /** 第 i 个参数按字符串读取。 */
    public String stringAt(int index) { return nativeStringAt(handle, index); }

    /** 第 i 个参数按 int 读取。 */
    public int intAt(int index) { return nativeIntAt(handle, index); }

    /** 第 i 个参数按 double 读取。 */
    public double doubleAt(int index) { return nativeDoubleAt(handle, index); }

    /** 第 i 个参数按 boolean 读取。 */
    public boolean boolAt(int index) { return nativeBoolAt(handle, index); }

    /** 便捷：所有参数按字符串列出（未知类型也能看到内容）。 */
    public List<String> argumentStrings() {
        int n = argumentCount();
        List<String> out = new ArrayList<>(Math.max(0, n));
        for (int i = 0; i < n; i++) out.add(stringAt(i));
        return Collections.unmodifiableList(out);
    }

    // ------------------------------------------------------------------ 元信息
    /** 消息类型（{@link #InvalidMessage} 等）。 */
    public int type() { return nativeType(handle); }

    /** 类型名（日志用）。 */
    public String typeName() {
        switch (type()) {
            case MethodCallMessage: return "MethodCall";
            case ReplyMessage: return "Reply";
            case ErrorMessage: return "Error";
            case SignalMessage: return "Signal";
            default: return "Invalid";
        }
    }

    /** 是否为错误回复。 */
    public boolean isError() { return nativeIsError(handle); }

    /** 错误名（如 {@code org.freedesktop.DBus.Error.ServiceUnknown}）。 */
    public String errorName() { return nativeErrorName(handle); }

    /** 错误描述。 */
    public String errorMessage() { return nativeErrorMessage(handle); }

    /** 目标服务名。 */
    public String service() { return nativeService(handle); }

    /** 对象路径。 */
    public String path() { return nativePath(handle); }

    /** 接口名（Qt 里叫 {@code interface()}，Java 里改名避免关键字冲突）。 */
    public String interfaceName() { return nativeInterface(handle); }

    /** 成员名（方法名 / 信号名）。 */
    public String member() { return nativeMember(handle); }

    /** 句柄是否有效（调用返回 null 表示没有回复）。 */
    public boolean isValid() { return handle != 0; }

    /** 包内：给 {@link QDBusConnection} 用的原生句柄。 */
    long handle() { return handle; }

    /** 提前释放（否则等 Cleaner）。 */
    public void dispose() {
        if (handle != 0) {
            nativeDispose(handle);
            handle = 0;
        }
    }

    @Override public String toString() {
        return "QDBusMessage(" + typeName() + " " + service() + path()
                + " iface=" + interfaceName() + " member=" + member()
                + " args=" + argumentCount()
                + (isError() ? " error=" + errorName() + " " + errorMessage() : "") + ")";
    }

    // ------------------------------------------------------------------ native
    private static native long nativeCreateMethodCall(String service, String path, String iface, String method);
    private static native long nativeCreateSignal(String path, String iface, String name);
    private static native void nativeDispose(long handle);
    private static native void nativeAddString(long handle, String value);
    private static native void nativeAddInt(long handle, int value);
    private static native void nativeAddLong(long handle, long value);
    private static native void nativeAddDouble(long handle, double value);
    private static native void nativeAddBool(long handle, boolean value);
    private static native int nativeType(long handle);
    private static native boolean nativeIsError(long handle);
    private static native String nativeErrorName(long handle);
    private static native String nativeErrorMessage(long handle);
    private static native String nativeService(long handle);
    private static native String nativePath(long handle);
    private static native String nativeInterface(long handle);
    private static native String nativeMember(long handle);
    private static native int nativeArgumentCount(long handle);
    private static native String nativeArgumentTypeAt(long handle, int index);
    private static native String nativeStringAt(long handle, int index);
    private static native int nativeIntAt(long handle, int index);
    private static native double nativeDoubleAt(long handle, int index);
    private static native boolean nativeBoolAt(long handle, int index);
}
