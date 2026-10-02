/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.lang.ref.Cleaner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 网络访问管理器（Qt {@code QNetworkAccessManager} 子集）：HTTP GET/POST。
 * <pre>
 * QNetworkAccessManager nam = new QNetworkAccessManager();
 * nam.get("https://example.com/api", reply -&gt; {
 *     if (reply.ok()) System.out.println(reply.statusCode() + " " + reply.text());
 *     else           System.out.println("失败: " + reply.errorString());
 * });
 * nam.post("https://example.com/api", "{\"a\":1}", "application/json", reply -&gt; { ... });
 * </pre>
 * <b>线程</b>：回调在 Qt 主线程执行（与 Qt 一致）；要更新界面请直接在回调里做，
 * 或从别的线程用 {@link QTimer#singleShot(int, Runnable)} 编组回主线程。
 * <p><b>生命周期</b>：回调收到的是 {@link QNetworkReply} —— 纯 Java 快照，
 * 原生应答对象在回调前已被释放，不会泄漏。
 */
public class QNetworkAccessManager {

    private static final Cleaner CLEANER = Cleaner.create();

    private final List<Consumer<QNetworkReply>> finishedHandlers = new ArrayList<>();
    private long nativeHandle;
    private String userAgent;
    private int transferTimeoutMs = 30000;

    /** 创建管理器。 */
    public QNetworkAccessManager() {
        nativeHandle = nativeCreate();
        final long h = nativeHandle;
        CLEANER.register(this, () -> nativeDispose(h));
    }

    private native long nativeCreate();
    private native void nativeDispose(long handle);

    /** 本库是否编译进 QtNetwork 支持（qtbase 自带，正常都为 true）。 */
    public static boolean isAvailable() { return nativeAvailable(); }
    private static native boolean nativeAvailable();

    /** 设置 User-Agent（空串表示不设置）。 */
    public void setUserAgent(String ua) {
        this.userAgent = ua;
        nativeSetUserAgent(nativeHandle, ua == null ? "" : ua);
    }
    private native void nativeSetUserAgent(long handle, String ua);

    /** 传输超时（毫秒；≤0 表示不限时）。 */
    public void setTransferTimeout(int ms) {
        this.transferTimeoutMs = ms;
        nativeSetTransferTimeout(nativeHandle, ms);
    }
    private native void nativeSetTransferTimeout(long handle, int ms);

    /** 当前传输超时。 */
    public int transferTimeout() { return transferTimeoutMs; }

    /** 注册"所有请求完成"回调（管理器级，Qt {@code finished} 语义）。 */
    public QNetworkAccessManager onFinished(Consumer<QNetworkReply> handler) {
        finishedHandlers.add(handler);
        return this;
    }

    /** HTTP GET；回调为该请求专属（可为 null）。 */
    public void get(String url, Consumer<QNetworkReply> handler) {
        nativeGet(nativeHandle, url, handler);
    }
    private native void nativeGet(long handle, String url, Consumer<QNetworkReply> handler);

    /** HTTP GET（无回调，仅走管理器级 onFinished）。 */
    public void get(String url) { get(url, null); }

    /** HTTP POST；contentType 为空则用 application/octet-stream。 */
    public void post(String url, byte[] body, String contentType, Consumer<QNetworkReply> handler) {
        nativePost(nativeHandle, url, body, contentType == null ? "" : contentType, handler);
    }
    private native void nativePost(long handle, String url, byte[] body, String contentType,
                                   Consumer<QNetworkReply> handler);

    /** HTTP POST（文本体，UTF-8）。 */
    public void post(String url, String body, String contentType, Consumer<QNetworkReply> handler) {
        post(url, body == null ? new byte[0] : body.getBytes(java.nio.charset.StandardCharsets.UTF_8),
             contentType, handler);
    }

    /**
     * 由 C++ 在请求完成时回调（JNI，Qt 主线程）。
     * <p>把状态/错误/响应体装配成 {@link QNetworkReply} 后分发给请求级与管理器级处理器。
     */
    void nativeHandleReply(String url, int statusCode, int error, String errorString,
                           byte[] body, String[] headerNames, String[] headerValues,
                           Consumer<QNetworkReply> handler) {
        Map<String, String> headers = new HashMap<>();
        if (headerNames != null && headerValues != null) {
            for (int i = 0; i < headerNames.length && i < headerValues.length; i++) {
                headers.put(headerNames[i], headerValues[i]);
            }
        }
        QNetworkReply reply = new QNetworkReply(url, statusCode, error, errorString, body, headers);
        if (handler != null) {
            handler.accept(reply);
        }
        for (Consumer<QNetworkReply> h : finishedHandlers) {
            h.accept(reply);
        }
    }
}
