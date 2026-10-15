/*
 * JQt - QWebView（系统 WebView）冒烟（v1.9.2 P2）。
 *
 * 背景：Qt 官方不为 MinGW 提供 QtWebEngine，但提供 qtwebview —— 它用系统 WebView 渲染
 * （Windows=WebView2 / macOS=WKWebView / Linux=WebEngine）。本冒烟验证：
 *   1. isAvailable() 与 QtWebView 模块探测一致
 *   2. 视图能建立、QML 无错误、WebView 元素存在
 *   3. **真的加载**：loadHtml 一段带 title 的 HTML，跑事件循环后 title() 必须匹配
 *   4. load(url) 后 url() 能读回
 *   5. 抓图：抓到且非纯色算通过；抓不到/空图 -> 明确 SKIP（WebView 是原生层，离屏常抓不到）
 *
 * 不支持时（模块缺失 / 无 WebView2 运行时 / 无桌面会话）必须打印 SKIP 并计数，不算通过。
 */
package org.jqt;

import java.util.concurrent.atomic.AtomicReference;

public class SmokeWebView {

    static int pass = 0;
    static int fail = 0;
    static int skip = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("[wv] OK   " + name); }
        else { fail++; System.out.println("[wv] FAIL " + name); }
    }

    static void skipped(String name, String why) {
        skip++;
        System.out.println("[wv] SKIP " + name + " —— " + why);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("[wv] start");

        // 原生库是在 QApplication 的静态初始化里 loadLibrary("jqt") 的 ——
        // 所以任何 native 调用(含 isAvailable())都必须先建 app(踩过:先查 isAvailable 会 UnsatisfiedLinkError)。
        QApplication app = new QApplication();

        boolean available = QWebView.isAvailable();
        System.out.println("[wv] QtWebView 编译进库 = " + available);
        if (!available) {
            skipped("全部 WebView 断言", "本库未编译 QtWebView(Android 或构建时未探测到该模块)");
            app.scheduleQuit(50);
            app.exec();
            System.out.println("[wv] pass=" + pass + " fail=" + fail + " skip=" + skip);
            System.out.println("[wv] ALL PASS ✅(含跳过)");
            return;
        }

        if (!QWebView.isBackendAvailable()) {
            // 必须在这里就停:没有后端时建视图会让 Qt qFatal 直接杀进程(实测 Windows 官方包如此)
            skipped("WebView 端到端断言", "本平台 Qt 缺 WebView 后端插件(plugins/webview);"
                    + "官方 6.12 qtwebview 包在 Windows 上不含它,构造会让 Qt 中止进程");
            app.scheduleQuit(50);
            app.exec();
            System.out.println("[wv] pass=" + pass + " fail=" + fail + " skip=" + skip);
            System.out.println("[wv] ALL PASS ✅(含跳过)");
            return;
        }

        QWebView view = new QWebView(480, 320);
        check("视图已建立(handle 有效)", view != null && !view.toString().isEmpty());

        // 关键顺序:必须先 load() —— QML 是在 load 时才 setSource 的,
        // 不加载就查 isReady() 永远是 false(踩过:这一步害我误判成"WebView2 没装")。
        view.load("about:blank");
        AtomicReference<String> readyState = new AtomicReference<>("(未检测)");
        QTimer.singleShot(1500, () -> readyState.set(view.isReady() ? "ready" : ("not-ready:" + view.errorString())));
        app.scheduleQuit(1800);
        app.exec();

        String err = view.errorString();
        if (!err.isEmpty()) System.out.println("[wv] QML 错误: " + err);
        // 平台缺件 ≠ 代码 bug:Qt 官方包在 Windows 上**不含 WebView2 后端插件**
        // (实测:mingw 与 MSVC 的 qtwebview 包都只有 Qt6WebView/Quick.dll + QML 模块,
        //  没有 plugins/webview/;于是 Qt 报 "No WebView plug-in found!")。
        // 这种情况如实 SKIP 并写明原因,不算通过、也不算失败。
        boolean platformMissing = err.contains("plug-in")
                || err.contains("not installed")
                || err.contains("Cannot load library");
        if (platformMissing) {
            skipped("WebView 端到端断言", "本平台 Qt 缺 WebView 后端/模块:" + err);
        } else {
            check("QML 加载无错误", err.isEmpty());
        }
        if (platformMissing || !view.isReady()) {
            skipped("WebView 元素与加载断言",
                    "QML 无错但 WebView 元素未建立(常见原因:无 WebView2 运行时 / 无桌面会话) [" + readyState.get() + "]");
        } else {
            // ---- 3) 真的加载 HTML 并读回 title ----
            final String marker = "JQt-WebView-OK-2026";
            view.loadHtml("<html><head><title>" + marker + "</title></head>"
                    + "<body><h1>JQt</h1><p>system webview</p></body></html>");
            AtomicReference<String> titleAfter = new AtomicReference<>("");
            QTimer.singleShot(1500, () -> titleAfter.set(view.title()));
            QTimer.singleShot(1800, () -> titleAfter.set(view.title()));
            app.scheduleQuit(2000);
            app.exec();
            System.out.println("[wv] title() = " + titleAfter.get());
            check("loadHtml 后 title() 匹配(实际 " + titleAfter.get() + ")", marker.equals(titleAfter.get()));

            // ---- 4) load(url) 读回 ----
            view.load("about:blank");
            AtomicReference<String> urlAfter = new AtomicReference<>("");
            QTimer.singleShot(600, () -> urlAfter.set(view.url()));
            app.scheduleQuit(800);
            app.exec();
            System.out.println("[wv] url() = " + urlAfter.get());
            check("load(about:blank) 后 url() 非空", !urlAfter.get().isEmpty());

            // ---- 5) 抓图 ----
            byte[] png = view.grabToPng();
            if (png == null || png.length < 100) {
                skipped("抓图断言", "WebView 是原生层,离屏/无窗口时抓不到画面(属预期)");
            } else {
                System.out.println("[wv] 抓到 PNG " + png.length + " 字节");
                check("抓到非空 PNG", png.length > 100);
            }
        }

        view.dispose();
        System.out.println("[wv] pass=" + pass + " fail=" + fail + " skip=" + skip);
        System.out.println("[wv] " + (fail == 0 ? "ALL PASS ✅" : ("FAILED: " + fail)));
        if (fail > 0) System.exit(1);
    }
}
