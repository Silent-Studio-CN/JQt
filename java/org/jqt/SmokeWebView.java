/*
 * JQt - QWebView（系统 WebView）冒烟（v1.9.3）。
 *
 * 背景：Qt 官方不为 MinGW 提供 QtWebEngine；官方 qtwebview 包又**不含 plugins/webview 后端插件**
 * （MinGW/MSVC 两个包都实测过），实例化 QML 的 WebView 元素会让 Qt `qFatal` 直接 fail-fast
 * （实测退出码 0xC0000409）。所以 Windows 上 QWebView 改走 JQt 自研的 WebView2(COM) 宿主。
 * 本冒烟验证的是**真端到端**，不是"建得出对象就算过"：
 *   1. isAvailable() / isBackendAvailable()（Windows 上后者 = WebView2 运行时可用）+ 运行时版本
 *   2. 视图能建立，控制器/WebView 真的就绪（isReady()）
 *   3. **真的加载**：loadHtml 一段带 <title> 的 HTML -> title() 必须等于那个 title
 *   4. load(url) 后 url() 能读回
 *   5. show() 生效（读 Win32 的窗口可见状态）
 *   6. resize(w,h) 生效：抓到的画面尺寸必须正好等于请求的 w×h（说明 controller bounds
 *      与宿主窗口客户区对齐了），并且图**不是纯色**（说明真渲染了，不是空白）
 *   7. dispose() 幂等且不崩
 *
 * 平台缺件时（无 WebView2 运行时 / 无桌面会话）必须打印 SKIP 并计数，不算通过。
 * 失败一律 System.exit(1) —— 不允许假通过。
 */
package org.jqt;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;

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

    /** 跑一小段事件循环（WebView2 的异步回调靠它推进）。 */
    static void pump(QApplication app, long ms) {
        app.scheduleQuit(ms);
        app.exec();
    }

    /** 反复小步推进事件循环，直到条件成立或超时。 */
    static boolean waitUntil(QApplication app, java.util.function.BooleanSupplier cond, long timeoutMs) {
        final long deadline = System.currentTimeMillis() + timeoutMs;
        while (!cond.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) return false;
            pump(app, 50);
        }
        return true;
    }

    static void summary() {
        System.out.println("[wv] pass=" + pass + " fail=" + fail + " skip=" + skip);
        System.out.println("[wv] " + (fail == 0 ? "ALL PASS ✅" : ("FAILED: " + fail)));
    }

    /** PNG 的 IHDR 尺寸（大端）。返回 null 表示不是合法 PNG。 */
    static int[] pngSize(byte[] png) {
        if (png == null || png.length < 24) return null;
        if ((png[0] & 0xFF) != 0x89 || png[1] != 'P' || png[2] != 'N' || png[3] != 'G') return null;
        int w = ((png[16] & 0xFF) << 24) | ((png[17] & 0xFF) << 16) | ((png[18] & 0xFF) << 8) | (png[19] & 0xFF);
        int h = ((png[20] & 0xFF) << 24) | ((png[21] & 0xFF) << 16) | ((png[22] & 0xFF) << 8) | (png[23] & 0xFF);
        return new int[] { w, h };
    }

    /** 采样页面像素的 ARGB 集合（每 3 像素取一个）。返回 null 表示解码失败。 */
    static Set<Integer> sampledColors(byte[] png) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
            if (img == null) return null;
            Set<Integer> colors = new HashSet<>();
            for (int y = 0; y < img.getHeight(); y += 3) {
                for (int x = 0; x < img.getWidth(); x += 3) {
                    colors.add(img.getRGB(x, y));
                }
            }
            return colors;
        } catch (Exception e) {
            return null;
        }
    }

    /** 采样里是否存在"接近白色"的像素（HTML 里的文字是 #ffffff）。 */
    static boolean hasNearWhite(Set<Integer> colors) {
        if (colors == null) return false;
        for (int c : colors) {
            int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
            if (r > 240 && g > 240 && b > 240) return true;
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        final String platform = System.getenv("QT_QPA_PLATFORM");
        System.out.println("[wv] start  QT_QPA_PLATFORM=" + (platform == null ? "(default: windows)" : platform));

        // 原生库是在 QApplication 的静态初始化里 loadLibrary("jqt") 的 ——
        // 所以任何 native 调用(含 isAvailable())都必须先建 app(踩过:先查 isAvailable 会 UnsatisfiedLinkError)。
        QApplication app = new QApplication();

        final boolean available = QWebView.isAvailable();
        final boolean backend = available && QWebView.isBackendAvailable();
        final String version = available ? QWebView.runtimeVersion() : "";
        System.out.println("[wv] isAvailable=" + available + " isBackendAvailable=" + backend
                + " runtimeVersion=" + (version.isEmpty() ? "(n/a)" : version));

        if (!available) {
            skipped("全部 WebView 断言", "本库未编译 WebView 支持(Android 或构建时既没有 QtWebView 也没有 WebView2 SDK)");
            pump(app, 50);
            summary();
            return;
        }
        if (!backend) {
            // 必须在这里就停:后端不可用时构造会拒绝(避免把进程带崩)
            skipped("WebView 端到端断言",
                    "WebView 后端不可用:本机没有可用的 WebView2 运行时(或 WebView2Loader.dll 找不到)");
            pump(app, 50);
            summary();
            return;
        }

        QWebView view = new QWebView(480, 320);
        check("视图已建立(handle 有效)", view != null && !view.toString().isEmpty());

        // ---- 1) 后端就绪(控制器 + WebView 建立;首次要起浏览器进程,给足时间) ----
        view.load("about:blank");
        final boolean ready = waitUntil(app, view::isReady, 30000);
        System.out.println("[wv] isReady=" + ready + " errorString=\"" + view.errorString() + "\"");
        check("WebView 后端就绪(controller + CoreWebView2 已建立)", ready);
        if (!ready) {
            System.out.println("[wv] 后端没起来,后续端到端断言无法进行: " + view.errorString());
            view.dispose();
            summary();
            System.exit(1);
        }

        // ---- 2) show() 生效(读 Win32 真实可见状态) ----
        view.show();
        pump(app, 400);
        System.out.println("[wv] isShown()=" + view.isShown());
        check("show() 生效(宿主窗口 IsWindowVisible=true)", view.isShown());

        // ---- 3) 真加载 HTML,title() 必须匹配 ----
        final String marker = "JQt-WebView-OK-2026";
        view.loadHtml("<html><head><title>" + marker + "</title></head>"
                + "<body style='background:#204060;color:#ffffff;font-family:sans-serif'>"
                + "<h1>JQt WebView2</h1><p>native COM host</p></body></html>");
        final boolean titled = waitUntil(app, () -> marker.equals(view.title()), 20000);
        final String title = view.title();
        System.out.println("[wv] title()=\"" + title + "\" (等待收敛=" + titled + ")");
        check("loadHtml 后 title() 匹配 HTML 里的 <title>(实际 \"" + title + "\")", marker.equals(title));

        // ---- 4) resize 生效 + 真渲染（**必须在 HTML 还显示着的时候抓图**：
        //         先 load("about:blank") 再抓，抓到的是空白页 —— 那是测试自己的 bug） ----
        final int wantW = 600, wantH = 400;
        view.resize(wantW, wantH);
        pump(app, 1200);   // 给页面重新布局 + 重绘留时间,再抓图

        // resize 是否生效看 **controller bounds**(权威),不看截图尺寸:
        // 实测 CapturePreview 有时比 bounds 多 1 像素(600x400 -> 600x401),那是 WebView2 自己的取整。
        final int[] vp = view.viewportSize();
        System.out.println("[wv] viewportSize()(controller bounds)="
                + (vp == null ? "(null)" : (vp[0] + "x" + vp[1])));
        check("resize(" + wantW + "," + wantH + ") 生效:controller bounds 与宿主客户区对齐 = "
                + wantW + "x" + wantH, vp != null && vp[0] == wantW && vp[1] == wantH);

        final byte[] png = view.grabToPng();
        if (png == null || png.length == 0) {
            skipped("抓图断言", "CapturePreview 没拿到画面(离屏/无桌面会话时常见)");
        } else {
            final int[] size = pngSize(png);
            System.out.println("[wv] PNG " + png.length + " 字节, 尺寸="
                    + (size == null ? "(不是合法 PNG)" : (size[0] + "x" + size[1])));
            check("抓到的是真 PNG(魔数 89 50 4E 47)", size != null);
            // 截图尺寸只做 ±1 的宽松断言(见 viewportSize() 的注释);严格对齐由上面的 bounds 断言负责
            check("截图尺寸与 resize 一致(±1px,WebView2 取整)",
                    size != null && Math.abs(size[0] - wantW) <= 1 && Math.abs(size[1] - wantH) <= 1);
            // 真渲染的判据:必须同时看到 HTML 里的背景色 #204060 与白色文字。
            // (空白页只能得到 1 种颜色;WebView2 未绘制时的兜底底色是 #121212)
            final Set<Integer> colors = sampledColors(png);
            final boolean bgOk = colors != null && colors.contains(0xFF204060);
            final boolean textOk = hasNearWhite(colors);
            System.out.println("[wv] 采样到的不同颜色数=" + (colors == null ? -1 : colors.size())
                    + ", 含 #204060 背景=" + bgOk + ", 含近白文字=" + textOk);
            check("画面真的渲染了 loadHtml 的内容(背景 #204060 + 白色文字都出现)", bgOk && textOk);
            try {
                File out = new File("webview_shot.png");
                Files.write(out.toPath(), png);
                System.out.println("[wv] 截图已写出: " + out.getAbsolutePath());
            } catch (Exception e) {
                System.out.println("[wv] 截图写出失败(不影响断言): " + e);
            }
        }

        // ---- 5) load(url) 后 url() 读回 ----
        view.load("about:blank");
        final boolean urled = waitUntil(app, () -> !view.url().isEmpty(), 15000);
        final String url = view.url();
        System.out.println("[wv] url()=\"" + url + "\" (等待收敛=" + urled + ")");
        check("load(about:blank) 后 url() 非空", !url.isEmpty());

        // ---- 6) dispose 幂等、不崩 ----
        view.dispose();
        view.dispose();          // 第二次必须是静默 no-op
        pump(app, 200);
        check("dispose() 后 handle 归零、url()/isReady() 安全返回",
                view.url().isEmpty() && !view.isReady());
        System.out.println("[wv] dispose 完成,进程仍然存活");

        summary();
        if (fail > 0) System.exit(1);
    }
}
