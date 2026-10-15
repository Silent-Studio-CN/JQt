/*
 * JQt - WebView 演示入口（v1.9.3）。
 *
 * 这是给"桌面快捷方式 / 双击打开"用的**长期入口**，不是冒烟测试：
 *   - 打开一个 WebView2 窗口，页面是**自包含的 HTML**（不依赖网络），
 *     蓝色背景 + 实时走动的时钟 + 运行时版本号 —— 时钟在走就说明 JS 与渲染都是活的；
 *   - 窗口标题带 "JQt WebView"（用户要认得出来），并附上 WebView2 运行时版本；
 *   - **关窗即退出**（靠 QWebView.setQuitOnClose(true)），不用去任务管理器杀进程；
 *   - 后端不可用时**友好提示**（GUI 弹窗 + 控制台说明），不是崩溃、不是白屏。
 *
 * 启动方式见仓库根目录的 run-webview-demo.cmd / run-webview-demo.ps1。
 */
package org.jqt;

public class JQtWebViewDemo {

    /** 窗口内容：自包含、离线可用；时钟在走 = JS + 渲染都在工作。 */
    private static String pageHtml(String runtimeVersion) {
        return "<!DOCTYPE html><html><head><meta charset='utf-8'>"
             + "<title>JQt WebView - 渲染正常</title>"
             + "<style>"
             + "  html,body{margin:0;height:100%;}"
             + "  body{background:linear-gradient(160deg,#204060 0%,#123 100%);"
             + "       color:#fff;font-family:'Segoe UI',system-ui,sans-serif;"
             + "       display:flex;flex-direction:column;align-items:center;"
             + "       justify-content:center;gap:14px;}"
             + "  h1{margin:0;font-size:44px;font-weight:600;letter-spacing:1px;}"
             + "  .sub{opacity:.85;font-size:17px;}"
             + "  .clock{font-size:64px;font-variant-numeric:tabular-nums;font-weight:300;"
             + "         margin:6px 0;text-shadow:0 2px 12px rgba(0,0,0,.35);}"
             + "  .badge{display:inline-block;padding:7px 16px;border-radius:999px;"
             + "         background:rgba(255,255,255,.14);border:1px solid rgba(255,255,255,.28);"
             + "         font-size:14px;}"
             + "  .ok{color:#7CFC9B;font-weight:600;}"
             + "  .foot{position:fixed;bottom:14px;font-size:12px;opacity:.6;}"
             + "</style></head><body>"
             + "<h1>JQt WebView</h1>"
             + "<div class='sub'>Windows 自研 WebView2(COM) 宿主 &mdash; 不依赖 QtWebView 后端插件</div>"
             + "<div class='clock' id='clock'>--:--:--</div>"
             + "<div class='badge'>WebView2 运行时 <b>" + runtimeVersion + "</b></div>"
             + "<div class='badge'>渲染状态 <span class='ok' id='state'>检测中…</span></div>"
             + "<div class='foot'>这个时钟每秒跳动 —— 看到它在走就说明页面真的在渲染</div>"
             + "<script>"
             + "  var ticks = 0;"
             + "  function pad(n){return (n<10?'0':'')+n;}"
             + "  function tick(){"
             + "    var d = new Date();"
             + "    document.getElementById('clock').textContent ="
             + "        pad(d.getHours())+':'+pad(d.getMinutes())+':'+pad(d.getSeconds());"
             + "    ticks++;"
             + "    if (ticks > 1) {"
             + "      document.getElementById('state').textContent = 'JS 运行中 (第 ' + ticks + ' 次刷新)';"
             + "    }"
             + "  }"
             + "  tick(); setInterval(tick, 1000);"
             + "</script></body></html>";
    }

    /** 友好失败：GUI 弹窗（双击打开时控制台可能一闪而过）+ 控制台说明 + 非零退出码。 */
    private static void failFriendly(String title, String detail) {
        System.out.println("[demo] " + title);
        System.out.println("[demo] " + detail);
        try {
            QMainWindow holder = new QMainWindow("JQt WebView");
            QMessageBox.warning(holder, title, detail);
        } catch (Throwable t) {
            // 连 Qt 窗口都起不来就只靠控制台（启动脚本会 pause，用户仍然看得到）
            System.out.println("[demo] (弹窗失败,仅控制台输出: " + t + ")");
        }
        System.exit(1);
    }

    public static void main(String[] args) {
        QApplication app = new QApplication();

        if (!QWebView.isAvailable()) {
            failFriendly("JQt WebView 不可用",
                    "本库没有编译 WebView 支持。\n\n"
                    + "请在仓库根目录跑一次 build.ps1 重新构建。");
            return;
        }
        if (!QWebView.isBackendAvailable()) {
            failFriendly("没找到 WebView2 运行时",
                    "Windows 上的 JQt WebView 需要「Microsoft Edge WebView2 Runtime」。\n\n"
                    + "Win10/11 一般随系统或 Microsoft Edge 自带；如果确实没有，装一下即可：\n"
                    + "https://developer.microsoft.com/microsoft-edge/webview2/\n\n"
                    + "（也可以直接装 Microsoft Edge，它同样提供这个运行时。）");
            return;
        }

        final String version = QWebView.runtimeVersion();
        System.out.println("[demo] WebView2 运行时版本 = " + (version.isEmpty() ? "(未知)" : version));

        final QWebView view = new QWebView(960, 640);
        view.setWindowTitle("JQt WebView \u2014 WebView2 " + version);
        // 用户关窗 -> QApplication.exec() 返回 -> 进程自然退出(不用任务管理器杀)
        view.setQuitOnClose(true);

        view.loadHtml(pageHtml(version.isEmpty() ? "?" : version));

        // 先把控制器/WebView 建起来再显示窗口,免得用户先看到一块空白
        final long deadline = System.currentTimeMillis() + 20000;
        while (!view.isReady() && System.currentTimeMillis() < deadline) {
            app.scheduleQuit(50);
            app.exec();
        }
        if (!view.isReady()) {
            failFriendly("WebView2 初始化失败",
                    "控制器没有建立起来。\n\n" + view.errorString());
            return;
        }

        view.show();
        System.out.println("[demo] 窗口标题 = " + "JQt WebView \u2014 WebView2 " + version);
        System.out.println("[demo] 窗口已打开,看完直接关掉它即可(关窗即退出)");

        app.exec();   // 阻塞:直到用户关窗(或系统注销)

        view.dispose();
        System.out.println("[demo] 窗口已关闭,程序正常退出");
    }
}
