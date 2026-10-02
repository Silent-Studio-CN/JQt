/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.lang.ref.Cleaner;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * WebSocket 客户端（Qt {@code QWebSocket}，QtWebSockets 模块）。
 * <pre>
 * QWebSocket ws = new QWebSocket();
 * ws.onConnected(() -&gt; ws.sendText("hello"));
 * ws.onTextMessage(msg -&gt; System.out.println("收到: " + msg));
 * ws.onDisconnected(() -&gt; System.out.println("断开"));
 * ws.open("ws://127.0.0.1:9001/chat");
 * </pre>
 * <b>线程</b>：所有回调在 Qt 主线程执行（事件循环里驱动）。
 * <p>模块可用性：QtWebSockets 不属于 qtbase，某些平台/发行包未附带 ——
 * 用 {@link #isAvailable()} 先判断；未编译进本库时构造会抛明确异常。
 */
public class QWebSocket {

    /** 连接状态（Qt {@code QAbstractSocket::SocketState} 子集）。 */
    public static final class State {
        private State() {}
        public static final int Unconnected = 0;
        public static final int Connecting = 2;
        public static final int Connected = 3;
        public static final int Closing = 4;
    }

    private static final Cleaner CLEANER = Cleaner.create();

    private final List<Runnable> connectedHandlers = new ArrayList<>();
    private final List<Runnable> disconnectedHandlers = new ArrayList<>();
    private final List<Consumer<String>> textHandlers = new ArrayList<>();
    private final List<Consumer<byte[]>> binaryHandlers = new ArrayList<>();
    private final List<Consumer<String>> errorHandlers = new ArrayList<>();

    private long nativeHandle;
    private String lastError = "";

    /** 本库是否编译进 QtWebSockets 支持。 */
    public static boolean isAvailable() { return nativeAvailable(); }
    private static native boolean nativeAvailable();

    /** 创建客户端（未连接）。 */
    public QWebSocket() {
        if (!isAvailable()) {
            throw new IllegalStateException(
                "本库未编译 QtWebSockets 支持(缺少 qtwebsockets 模块);先安装该模块再构建 libjqt");
        }
        nativeHandle = nativeCreate();
        final long h = nativeHandle;
        CLEANER.register(this, () -> nativeDispose(h));
    }

    private static native long nativeCreate();
    private static native void nativeDispose(long handle);

    /** 连接（ws:// 或 wss://）。 */
    public void open(String url) { nativeOpen(nativeHandle, url); }
    private native void nativeOpen(long handle, String url);   // 实例 native:回调需要 Java 对象引用

    /** 发送文本（未连接时返回 false）。 */
    public boolean sendText(String text) {
        return nativeSendText(nativeHandle, text == null ? "" : text);
    }
    private static native boolean nativeSendText(long handle, String text);

    /** 发送二进制（未连接时返回 false）。 */
    public boolean sendBinary(byte[] data) {
        return nativeSendBinary(nativeHandle, data == null ? new byte[0] : data);
    }
    private static native boolean nativeSendBinary(long handle, byte[] data);

    /** 主动关闭。 */
    public void close() { nativeClose(nativeHandle); }
    private static native void nativeClose(long handle);

    /** 是否处于已连接状态。 */
    public boolean isConnected() { return nativeState(nativeHandle) == State.Connected; }
    private static native int nativeState(long handle);

    /** 连接状态（{@link State}）。 */
    public int state() { return nativeState(nativeHandle); }

    /** 最近一次错误描述（成功时为空串）。 */
    public String lastError() { return lastError; }

    // ---------------- 回调 ----------------

    /** 连接建立。 */
    public QWebSocket onConnected(Runnable handler) { connectedHandlers.add(handler); return this; }

    /** 连接断开。 */
    public QWebSocket onDisconnected(Runnable handler) { disconnectedHandlers.add(handler); return this; }

    /** 收到文本消息。 */
    public QWebSocket onTextMessage(Consumer<String> handler) { textHandlers.add(handler); return this; }

    /** 收到二进制消息。 */
    public QWebSocket onBinaryMessage(Consumer<byte[]> handler) { binaryHandlers.add(handler); return this; }

    /** 发生错误（参数为描述）。 */
    public QWebSocket onError(Consumer<String> handler) { errorHandlers.add(handler); return this; }

    // ---------------- 由 C++ 侧回调（JNI，Qt 主线程）----------------

    void nativeHandleConnected() {
        for (Runnable h : connectedHandlers) h.run();
    }

    void nativeHandleDisconnected() {
        for (Runnable h : disconnectedHandlers) h.run();
    }

    void nativeHandleTextMessage(String text) {
        String t = text == null ? "" : text;
        for (Consumer<String> h : textHandlers) h.accept(t);
    }

    void nativeHandleBinaryMessage(byte[] data) {
        byte[] d = data == null ? new byte[0] : data;
        for (Consumer<byte[]> h : binaryHandlers) h.accept(d);
    }

    void nativeHandleError(String message) {
        lastError = message == null ? "" : message;
        for (Consumer<String> h : errorHandlers) h.accept(lastError);
    }

    /** 文本转字节（工具，便于二进制通道传文本）。 */
    public static byte[] utf8(String s) { return s.getBytes(StandardCharsets.UTF_8); }
}
