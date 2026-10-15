/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.lang.ref.Cleaner;

/**
 * 系统 WebView（Windows 走 JQt 自研 WebView2 宿主；其它平台走 Qt {@code Qt WebView} 模块）。
 *
 * <h3>Windows：为什么不再用 QtWebView</h3>
 * Qt 官方**不为 MinGW 提供 QtWebEngine**，所以"在 Java 里显示网页"原本走官方 {@code qtwebview}
 * （它用系统 WebView 渲染）。但实测（MinGW 与 MSVC 两个包的 7z 文件清单都查过）：
 * 官方 6.12 的 {@code qtwebview} 包**只含** {@code Qt6WebView.dll} / {@code Qt6WebViewQuick.dll}
 * / 头文件 / QML 模块，**没有 {@code plugins/webview/} 后端插件**。后果是实例化 QML 的
 * {@code WebView} 元素时 Qt 直接 {@code qFatal("No WebView plug-in found!")}，
 * 进程 fail-fast（实测退出码 {@code 0xC0000409}），连降级的机会都没有。
 *
 * <p>因此 Windows 上改为 **JQt 自研的 WebView2(COM) 宿主**（native 侧 {@code jqtwv2} 命名空间）：
 * 自己 {@code CreateWindowExW} 建宿主窗口，调 {@code ICoreWebView2Environment} /
 * {@code ICoreWebView2Controller} / {@code ICoreWebView2} 驱动微软 WebView2 运行时。
 * 其它平台继续走 QtWebView（macOS/iOS 的系统 WebView 后端编译在 QtWebView 内部，不需要插件目录）。
 *
 * <pre>
 * if (QWebView.isAvailable() &amp;&amp; QWebView.isBackendAvailable()) {
 *     QWebView view = new QWebView(640, 480);
 *     view.loadHtml("&lt;html&gt;&lt;head&gt;&lt;title&gt;你好&lt;/title&gt;&lt;/head&gt;&lt;body&gt;JQt&lt;/body&gt;&lt;/html&gt;");
 *     view.show();
 *     // 需要事件循环推进加载：QApplication.exec() 或 QTimer + processEvents
 *     System.out.println(view.title());     // → 你好
 *     view.dispose();
 * }
 * </pre>
 *
 * <h3>限制（与"系统 WebView 是原生层"这件事有关，不是本绑定的缺陷）</h3>
 * <ol>
 *   <li>Windows 上 WebView 是**独立的原生顶层窗口**，不参与 Qt 的布局/层叠 —— 不能与其它
 *       QWidget 重叠摆放（QtWebView 在其它平台也有同样的限制）；</li>
 *   <li>{@code runJavaScript} 需要异步回调桥，本类暂未提供（当前提供 {@code url/title/grabToPng}）；</li>
 *   <li>构造**不会**自动显示窗口，需要显式 {@link #show()}（否则构造就弹窗，API 不礼貌）。</li>
 * </ol>
 * <b>线程</b>：所有方法必须在 Qt 主线程调用（与其它部件一致）。
 */
public class QWebView {

    private static final Cleaner CLEANER = Cleaner.create();

    private long handle;
    private final int width;
    private final int height;

    /** QtWebView 是否编译进本库，**或** Windows 上自研 WebView2 宿主是否编译进本库。 */
    public static boolean isAvailable() { return nativeAvailable(); }

    /**
     * 本平台的 WebView <b>后端</b>是否真的能用。
     *
     * <p><b>Windows（v1.9.3 起）</b>：判据变成"**WebView2 运行时是否可用**" ——
     * 动态加载 {@code WebView2Loader.dll} 并调 {@code GetAvailableCoreWebView2BrowserVersionString}
     * 问出运行时版本号，拿得到才算可用。与"Qt 有没有 plugins/webview 目录"**完全无关**
     * （后者在官方包上恒为 false，正是它让实例化必崩）。
     * 运行时缺失时这里返回 {@code false}，构造会拒绝，绝不把进程带崩。
     *
     * <p><b>其它平台</b>：macOS/iOS 的系统 WebView 后端编译在 QtWebView 内部，不需要插件目录
     * → 返回 true；Linux 看有没有 {@code plugins/webview}。
     */
    public static boolean isBackendAvailable() { return nativeBackendAvailable(); }

    /**
     * 后端版本号（诊断/冒烟用）。
     * <p>Windows 上是 WebView2 运行时版本（例如 {@code "155.0.4283.45"}）；其它平台返回空串。
     */
    public static String runtimeVersion() { return nativeRuntimeVersion(); }

    /** 创建视图（此时还没有网页内容，窗口也不可见 —— 要显示请调 {@link #show()}）。 */
    public QWebView(int width, int height) {
        if (!isAvailable()) throw new IllegalStateException("本库未编译 WebView 支持");
        if (!isBackendAvailable()) {
            throw new IllegalStateException("本平台 WebView 后端不可用:"
                    + (isWindows()
                       ? "本机没有可用的 WebView2 运行时(或 WebView2Loader.dll 找不到)"
                       : "Qt 未提供 WebView 后端插件(plugins/webview),实例化会让 Qt 直接中止进程"));
        }
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.handle = nativeCreate(this.width, this.height);
        final long h = handle;
        CLEANER.register(this, () -> nativeDispose(h));
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /** 加载 URL（http/https/file/data/about 均可）。 */
    public void load(String url) {
        if (handle != 0) nativeLoadUrl(handle, url == null ? "about:blank" : url);
    }

    /**
     * 加载一段 HTML 文本。
     * <p>Windows 自研宿主走 WebView2 的 {@code NavigateToString}（不落临时文件、也没有转义问题）；
     * 其它平台的 QtWebView 分支仍是"写临时文件再导航"。
     */
    public void loadHtml(String html) {
        if (handle != 0) nativeLoadHtml(handle, html == null ? "" : html);
    }

    /** 当前 URL（尚未加载时为 {@code ""}）。 */
    public String url() { return handle == 0 ? "" : nativeUrl(handle); }

    /** 当前页面标题（加载完成前为 {@code ""}）。 */
    public String title() { return handle == 0 ? "" : nativeTitle(handle); }

    /** 后端是否已就绪（不代表页面加载完成）。Windows 上 = WebView2 控制器与 WebView 已建立。 */
    public boolean isReady() { return handle != 0 && nativeIsReady(handle); }

    /** 错误文本（正常时为空串）。 */
    public String errorString() { return handle == 0 ? "" : nativeErrorString(handle); }

    /**
     * 调整尺寸（客户区正好 {@code width×height}）。
     * <p>Windows 上会把宿主窗口外框换算成对应尺寸，再把 WebView2 控制器的 bounds
     * 对齐到客户区。
     */
    public void resize(int width, int height) {
        if (handle != 0) nativeResize(handle, Math.max(1, width), Math.max(1, height));
    }

    /** 显示窗口（Windows：真正把宿主窗口 {@code ShowWindow}）。 */
    public void show() { if (handle != 0) nativeShow(handle); }

    /** 宿主窗口当前是否可见（Windows 上直接读 {@code IsWindowVisible}）。 */
    public boolean isShown() { return handle != 0 && nativeIsShown(handle); }

    /**
     * 当前 WebView 视口尺寸 {@code {宽, 高}}（物理像素；拿不到返回 {@code null}）。
     * <p>Windows 上就是 WebView2 controller 的 {@code get_Bounds} ——
     * <b>验证 {@link #resize} 是否真的生效要看它</b>：实测 {@code CapturePreview} 截出来的
     * 图片有时比 bounds 多 1 个像素（600×400 的 bounds 截出 600×401），那是 WebView2 自己的
     * 取整行为，不能拿图片尺寸反过来判定 bounds 没对齐。
     */
    public int[] viewportSize() { return handle == 0 ? null : nativeViewportSize(handle); }

    /** 抓取当前画面为 PNG（Windows 走 WebView2 的 {@code CapturePreview}；失败返回 {@code null}）。 */
    public byte[] grabToPng() { return handle == 0 ? null : nativeGrabToPng(handle); }

    /** 提前释放（幂等）。 */
    public void dispose() {
        if (handle != 0) {
            nativeDispose(handle);
            handle = 0;
        }
    }

    @Override public String toString() {
        return "QWebView(" + width + "x" + height + " url=" + url() + " title=" + title()
                + " ready=" + isReady() + " shown=" + isShown() + ")";
    }

    private static native boolean nativeAvailable();
    private static native boolean nativeBackendAvailable();
    private static native String nativeRuntimeVersion();
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
    private static native boolean nativeIsShown(long handle);
    private static native int[] nativeViewportSize(long handle);
    /**
     * 设置宿主窗口标题（Windows：{@code SetWindowTextW}）。
     * <p>注意别和页面标题（{@link #title()}）混淆：这是**窗口**标题栏的文字。
     */
    public void setWindowTitle(String title) {
        if (handle != 0 && title != null) nativeSetWindowTitle(handle, title);
    }

    /**
     * 用户点宿主窗口的关闭按钮时，是否顺便退出 {@code QApplication}。
     *
     * <p><b>默认 false</b>：只隐藏窗口，本对象继续可用（库该有的礼貌行为，别的窗口不受影响）。
     * 独立应用/演示程序设成 {@code true}，就能"用户关窗 → {@code QApplication.exec()} 返回"，
     * 进程自然退出，不用去任务管理器里杀。
     */
    public void setQuitOnClose(boolean on) {
        if (handle != 0) nativeSetQuitOnClose(handle, on);
    }

    private static native void nativeSetWindowTitle(long handle, String title);
    private static native void nativeSetQuitOnClose(long handle, boolean on);
    private static native byte[] nativeGrabToPng(long handle);
}
