/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

/**
 * URL 值类（Qt {@code QUrl}，纯 Java 实现，基于 {@link java.net.URI}）。
 * <p>完整覆盖 Qt 6 QUrl 常用 API，并提供 {@link #toUri()} / {@link #fromUri(URI)} 与 Java 生态互转。
 * <p>注意：Qt 6 已移除 {@code clear()}（本类同样不提供，使用 {@code new QUrl()}）。
 */
public class QUrl {

    private URI uri;          // null = 无效
    private boolean valid;

    public QUrl() { this.uri = null; this.valid = false; }

    public QUrl(String url) {
        if (url == null || url.isEmpty()) { this.uri = null; this.valid = false; return; }
        try {
            this.uri = new URI(url);
            this.valid = true;
        } catch (URISyntaxException e) {
            this.uri = null;
            this.valid = false;
        }
    }

    public QUrl(QUrl other) { this.uri = other.uri; this.valid = other.valid; }

    /** 从 URI 构造。 */
    public static QUrl fromUri(URI uri) {
        QUrl u = new QUrl();
        u.uri = uri;
        u.valid = uri != null;
        return u;
    }

    /** 从编码字符串构造（Qt fromEncoded）。 */
    public static QUrl fromEncoded(String encoded) {
        try {
            QUrl u = new QUrl();
            u.uri = new URI(encoded);
            return u;
        } catch (URISyntaxException e) {
            return new QUrl();
        }
    }

    public boolean isValid() { return valid && uri != null; }

    public String scheme() { return uri != null ? uri.getScheme() : null; }
    public String host() { return uri != null ? uri.getHost() : null; }
    public String path() { return uri != null ? uri.getPath() : null; }
    public String query() { return uri != null ? uri.getQuery() : null; }
    public String fragment() { return uri != null ? uri.getFragment() : null; }
    public String userName() { return uri != null ? uri.getUserInfo() : null; }
    public String password() { return uri != null ? uri.getUserInfo() : null; }

    /** authority = userInfo@host:port。 */
    public String authority() { return uri != null ? uri.getAuthority() : null; }

    /** userInfo = user:password。 */
    public String userInfo() { return uri != null ? uri.getUserInfo() : null; }

    /** 端口（未指定返回 -1）。 */
    public int port() { return uri != null && uri.getPort() != -1 ? uri.getPort() : -1; }

    public boolean isRelative() { return uri != null && uri.getScheme() == null; }
    public boolean isLocalFile() { return uri != null && "file".equals(uri.getScheme()); }

    /** 从本地文件路径构造（Qt fromLocalFile：自动加 file:// 前缀）。 */
    public static QUrl fromLocalFile(String path) {
        try {
            QUrl u = new QUrl();
            u.uri = new URI("file:///" + path.replace('\\', '/'));
            return u;
        } catch (URISyntaxException e) {
            return new QUrl();
        }
    }

    /** 本地文件路径（Qt toLocalFile；非 file: 返回空串）。 */
    public String toLocalFile() {
        if (!isLocalFile()) return "";
        String p = path();
        if (p.startsWith("/")) p = p.substring(1);
        return p.replace('/', '\\');
    }

    /** 解析相对 URL（Qt resolved）。 */
    public QUrl resolved(String relative) {
        if (uri == null) return new QUrl();
        try {
            URI base = uri;
            if (base.getPath() == null) base = new URI(base.getScheme(), base.getAuthority(), "/", base.getQuery(), base.getFragment());
            QUrl r = new QUrl();
            r.uri = base.resolve(new URI(relative));
            return r;
        } catch (URISyntaxException e) {
            return new QUrl();
        }
    }

    private String schemePart() { return scheme() != null ? scheme() + ":" : ""; }
    private String authorityPart() { return host() != null ? "//" + authority() : ""; }
    private String queryPart() { return query() != null ? "?" + query() : ""; }
    private String fragmentPart() { return fragment() != null ? "#" + fragment() : ""; }
    private String pathPart() { return path() != null ? path() : ""; }

    public void setScheme(String scheme) {
        replace(scheme != null ? scheme + ":" + restAfterScheme() : restAfterScheme());
    }
    public void setHost(String host) {
        replace(schemePart() + "//" + (host != null ? host : "") + pathPart() + queryPart() + fragmentPart());
    }
    public void setPath(String path) {
        replace(schemePart() + authorityPart() + (path != null ? path : "") + queryPart() + fragmentPart());
    }
    public void setQuery(String query) {
        replace(schemePart() + authorityPart() + pathPart() + (query != null ? "?" + query : "") + fragmentPart());
    }
    public void setFragment(String fragment) {
        replace(schemePart() + authorityPart() + pathPart() + queryPart() + (fragment != null ? "#" + fragment : ""));
    }
    public void setPort(int port) {
        String auth = host() != null ? "//" + host() + (port > 0 ? ":" + port : "") : "";
        replace(schemePart() + auth + pathPart() + queryPart() + fragmentPart());
    }
    public void setUrl(String url) {
        if (url == null || url.isEmpty()) { uri = null; return; }
        try { uri = new URI(url); } catch (URISyntaxException e) { uri = null; }
    }

    private String schemeWithColon() { return scheme() != null ? scheme() + ":" : ""; }
    private String restAfterScheme() {
        String s = toString();
        int i = s.indexOf(':');
        return i >= 0 ? s.substring(i + 1) : s;
    }
    private String restAfterHost() {
        String s = toString();
        int i = s.indexOf("//");
        if (i < 0) return s;
        int j = s.indexOf('/', i + 2);
        return j >= 0 ? s.substring(j) : "";
    }
    private String authorityPrefix() {
        return host() != null ? "//" + authority() : "";
    }
    private String queryFragment() {
        return (query() != null ? "?" + query() : "") + (fragment() != null ? "#" + fragment() : "");
    }
    private void replace(String s) {
        if (s == null || s.isEmpty()) { uri = null; return; }
        try { uri = new URI(s); }
        catch (URISyntaxException e) {
            // 空 scheme-specific-part（如 "https:"）→ 补 "/" 使其合法
            try { uri = new URI(s + "/"); }
            catch (URISyntaxException e2) { uri = null; }
        }
    }

    /** 编码字符串（Qt toEncoded）。 */
    public String toEncoded() { return toString(); }

    /** 解码显示字符串（Qt toDisplayString 简化：同 toString）。 */
    public String toDisplayString() { return toString(); }

    /** 转 java.net.URI。 */
    public URI toUri() { return uri; }

    /** 文件名部分（Qt fileName）。 */
    public String fileName() {
        String p = path();
        if (p == null || p.isEmpty()) return "";
        int i = p.lastIndexOf('/');
        return i >= 0 ? p.substring(i + 1) : p;
    }

    /** 顶级域名（Qt topLevelDomain，简化：host 最后两段）。 */
    public String topLevelDomain() {
        String h = host();
        if (h == null) return "";
        String[] parts = h.split("\\.");
        if (parts.length < 2) return h;
        return "." + parts[parts.length - 2] + "." + parts[parts.length - 1];
    }

    /** 是否父级 URL（Qt isParentOf 简化：path 前缀判断）。 */
    public boolean isParentOf(QUrl child) {
        String p = path(), cp = child.path();
        if (p == null || cp == null) return false;
        return cp.startsWith(p) && !cp.equals(p);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QUrl)) return false;
        QUrl u = (QUrl) o;
        if (valid != u.valid) return false;
        if (!valid) return true;
        return uri.equals(u.uri);
    }

    @Override
    public int hashCode() { return uri != null ? uri.hashCode() : 0; }

    @Override
    public String toString() { return uri != null ? uri.toString() : ""; }
}
