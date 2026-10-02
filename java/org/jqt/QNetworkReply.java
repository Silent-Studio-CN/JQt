/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * 网络应答（Qt {@code QNetworkReply} 的只读快照，纯 Java 值对象）。
 * <p>JQt 在请求结束时把状态、错误与响应体一次性搬到 Java 侧 —— 因此本对象
 * <b>没有原生句柄</b>，不会泄漏、也不需要 dispose，随时可跨线程保存。
 */
public final class QNetworkReply {

    /** 无错误（对应 Qt {@code QNetworkReply::NoError}）。 */
    public static final int NoError = 0;

    private final int statusCode;
    private final int error;
    private final String errorString;
    private final byte[] body;
    private final String url;
    private final Map<String, String> headers;

    QNetworkReply(String url, int statusCode, int error, String errorString, byte[] body,
                  Map<String, String> headers) {
        this.url = url == null ? "" : url;
        this.statusCode = statusCode;
        this.error = error;
        this.errorString = errorString == null ? "" : errorString;
        this.body = body == null ? new byte[0] : body;
        this.headers = headers == null ? Collections.emptyMap() : headers;
    }

    /** HTTP 状态码（0 表示未拿到响应，例如连接失败）。 */
    public int statusCode() { return statusCode; }

    /** 错误码（{@link #NoError} 表示成功）。 */
    public int error() { return error; }

    /** 错误描述（成功时为空串）。 */
    public String errorString() { return errorString; }

    /** 请求是否成功（无错误且有状态码）。 */
    public boolean ok() { return error == NoError && statusCode >= 200 && statusCode < 300; }

    /** 原始响应体（可能为空数组）。 */
    public byte[] body() { return body.clone(); }

    /** 响应体长度（字节）。 */
    public int size() { return body.length; }

    /** 响应体按 UTF-8 解码为文本。 */
    public String text() { return new String(body, StandardCharsets.UTF_8); }

    /** 请求 URL。 */
    public String url() { return url; }

    /** 响应头（大小写不敏感查找由调用方处理；键为 Qt 原始大小写）。 */
    public Map<String, String> headers() { return Collections.unmodifiableMap(headers); }

    /** 指定响应头（不存在返回空串）。 */
    public String header(String name) {
        String v = headers.get(name);
        if (v != null) return v;
        for (Map.Entry<String, String> e : headers.entrySet()) {
            if (e.getKey().equalsIgnoreCase(name)) return e.getValue();
        }
        return "";
    }

    @Override public String toString() {
        return "QNetworkReply(" + url + " status=" + statusCode + " error=" + error
             + (errorString.isEmpty() ? "" : " " + errorString) + " " + size() + "B)";
    }
}
