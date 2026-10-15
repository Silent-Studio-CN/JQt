/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.lang.ref.Cleaner;

/**
 * 系统 WebView（Qt {@code Qt WebView} 模块，v1.9.2 P2）。
 *
 * <p><b>为什么用它</b>：Qt 官方**不为 MinGW 提供 QtWebEngine**（Windows 上只发 MSVC 版），
 * 所以"在 Java 里显示网页"这条路由官方 {@code qtwebview} 走 —— 它用系统自带的 WebView 渲染：
 * <ul>
 *   <li><b>Windows</b>：WebView2（Edge/Chromium，Win10/11 基本预装运行时）；</li>
 *   <li><b>macOS</b>：系统 WKWebView；</li>
 *   <li><b>Linux</b>：依赖 QtWebEngine（本绑定不保证，{@link #isAvailable()} 会如实报告）。</li>
 * </ul>
 *
 * <pre>
 * if (QWebView.isAvailable()) {
 *     QWebView view = new QWebView(640, 480);
 *     view.loadHtml("&lt;html&gt;&lt;head&gt;&lt;title&gt;你好&lt;/title&gt;&lt;/head&gt;&lt;body&gt;JQt&lt;/body&gt;&lt;/html&gt;");
 *     view.show();
 *     // 需要事件循环推进加载：QApplication.exec() 或 QTimer + processEvents
 *     System.out.println(view.title());     // → 你好
 * }
 * </pre>
 *
 * <h3>与 Qt 官方限制一致的两点（不是本绑定的缺陷）</h3>
 * <ol>
 *   <li>WebView <b>不能与其它 QML 元素重叠</b>（它是原生窗口/合成层），事件也不进 Qt 事件系统；</li>
 *   <li>{@code runJavaScript} 需要 QML 回调桥，本类暂未提供（当前提供 {@code url/title/grabToPng}）。</li>
 * </ol>
 * <b>线程</b>：所有方法必须在 Qt 主线程调用（与其它部件一致）。
 */
public class QWebView {

    private static final Cleaner CLEANER = Cleaner.create();

    private long handle;
    private final int width;
    private final int height;

    /** QtWebView 是否编译进本库（未探测到该模块时为 false）。 */
    public static boolean isAvailable() { return nativeAvailable(); }

    /**
     * 本平台的 WebView <b>后端</b>是否真的在。
     * <p><b>实测结论</b>：官方 Qt 6.12 的 qtwebview 包（MinGW 与 MSVC 都一样）只带
     * Qt6WebView / Qt6WebViewQuick 与 QML 模块，<b>没有 plugins/webview/ 后端插件</b>；
     * 这种环境下实例化 WebView 元素会让 Qt 直接 abort
     * （{@code qFatal("No WebView plug-in found!")}，进程 fail-fast）。
     * 因此本类在构造时就拒绝，避免调用方把进程崩掉。
     * <p>macOS/iOS 的系统 WebView 后端编译在 QtWebView 内部，不需要插件目录 → 返回 true。
     */
    public static boolean isBackendAvailable() { return nativeBackendAvailable(); }

    /** 创建视图（此时还没有网页内容）。 */
    public QWebView(int width, int height) {
        if (!isAvailable()) throw new IllegalStateException("本库未编译 QtWebView");
        if (!isBackendAvailable()) {
            throw new IllegalStateException("本平台 Qt 未提供 WebView 后端插件(plugins/webview):"
                    + "官方 6.12 的 qtwebview 包不含它,实例化会让 Qt 直接中止进程");
        }
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.handle = nativeCreate(this.width, this.height);
        final long h = handle;
        CLEANER.register(this, () -> nativeDispose(h));
    }

    /** 加载 URL（http/https/file/data 均可）。 */
    public void load(String url) {
        if (handle != 0) nativeLoadUrl(handle, url == null ? "about:blank" : url);
    }

    /**
     * 加载一段 HTML 文本。
     * <p>实现上会先把 HTML 写到临时文件再让 WebView 打开（避免把 HTML 拼进 QML 字符串的转义坑）。
     */
    public void loadHtml(String html) {
        if (handle != 0) nativeLoadHtml(handle, html == null ? "" : html);
    }

    /** 当前 URL（尚未加载时为 {@code ""}）。 */
    public String url() { return handle == 0 ? "" : nativeUrl(handle); }

    /** 当前页面标题（加载完成前为 {@code ""}）。 */
    public String title() { return handle == 0 ? "" : nativeTitle(handle); }

    /** QML 是否已就绪且 WebView 元素已建立（不代表页面加载完成）。 */
    public boolean isReady() { return handle != 0 && nativeIsReady(handle); }

    /** QML 错误文本（正常时为空串）。 */
    public String errorString() { return handle == 0 ? "" : nativeErrorString(handle); }

    /** 调整尺寸。 */
    public void resize(int width, int height) {
        if (handle != 0) nativeResize(handle, Math.max(1, width), Math.max(1, height));
    }

    /** 显示窗口（真实桌面下才有意义；离屏/无窗口环境请勿依赖）。 */
    public void show() { if (handle != 0) nativeShow(handle); }

    /** 抓取当前画面为 PNG（WebView 是原生层，可能抓到空图 —— 调用方需自行判断）。 */
    public byte[] grabToPng() { return handle == 0 ? null : nativeGrabToPng(handle); }

    /** 提前释放。 */
    public void dispose() {
        if (handle != 0) {
            nativeDispose(handle);
            handle = 0;
        }
    }

    @Override public String toString() {
        return "QWebView(" + width + "x" + height + " url=" + url() + " title=" + title()
                + " ready=" + isReady() + ")";
    }

    private static native boolean nativeAvailable();
    private static native boolean nativeBackendAvailable();
    private static native long nativeCreate(int width, int height);
    private static native void nativeDispose(long handle);
    private static native void nativeLoadUrl(long handle, String url);
    private static native void nativeLoadHtml(long handle, String html);
    private static native String nativeUrl(long handle);
    private static native String nativeTitle(long handle);
    private static native boolean nativeIsReady(long handle);
    private static native String nativeErrorString(long handle);
    private static native void nativeResize(long handle, int width, int height);
    private static native void nativeShow(long handle);
    private static native byte[] nativeGrabToPng(long handle);
}
