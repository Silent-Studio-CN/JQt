/*
 * JQt - QNetworkAccessManager 冒烟（v1.9.1 P0-⑤）。
 *
 * 不依赖外网:冒烟自己起一个 Java ServerSocket 当 HTTP 服务端,
 * 因此 GET/POST/404/连接失败 四种路径都可在 CI 里确定性复现。
 */
package org.jqt;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class SmokeNetwork {

    static int pass = 0;
    static int fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("[net] OK   " + name); }
        else { fail++; System.out.println("[net] FAIL " + name); }
    }

    /** 极简 HTTP 服务端:按请求行返回 200 或 404。 */
    static final class MiniHttp implements AutoCloseable {
        private final ServerSocket server;
        private final Thread thread;
        volatile String lastRequestLine = "";
        volatile String lastBody = "";
        volatile String lastHeaders = "";
        volatile String getHeaders = "";      // 第一条 GET 的头(避免被后续请求覆盖)

        MiniHttp() throws Exception {
            server = new ServerSocket(0, 4, java.net.InetAddress.getByName("127.0.0.1"));
            thread = new Thread(() -> {
                while (!server.isClosed()) {
                    try (Socket c = server.accept()) {
                        InputStream in = c.getInputStream();
                        ByteArrayOutputStream head = new ByteArrayOutputStream();
                        int cur;
                        while ((cur = in.read()) != -1) {          // 读到 \r\n\r\n(HTTP 头结束)
                            head.write(cur);
                            String so_far = head.toString("UTF-8");
                            if (so_far.endsWith("\r\n\r\n") || so_far.endsWith("\n\n")) break;
                        }
                        String req = head.toString("UTF-8");
                        lastRequestLine = req.lines().findFirst().orElse("");
                        lastHeaders = req;
                        if (lastRequestLine.startsWith("GET") && getHeaders.isEmpty()) getHeaders = req;
                        int len = 0;
                        for (String line : req.split("\r\n")) {
                            if (line.toLowerCase().startsWith("content-length:")) {
                                len = Integer.parseInt(line.split(":")[1].trim());
                            }
                        }
                        if (len > 0) {
                            byte[] b = new byte[len];
                            int got = 0;
                            while (got < len) {
                                int n = in.read(b, got, len - got);
                                if (n < 0) break;
                                got += n;
                            }
                            lastBody = new String(b, 0, got, StandardCharsets.UTF_8);
                        }
                        OutputStream out = c.getOutputStream();
                        if (lastRequestLine.contains("/missing")) {
                            String body = "nope";
                            out.write(("HTTP/1.1 404 Not Found\r\nContent-Length: " + body.length()
                                     + "\r\nConnection: close\r\n\r\n" + body).getBytes(StandardCharsets.UTF_8));
                        } else if (lastRequestLine.startsWith("POST")) {
                            String body = "echo:" + lastBody;
                            out.write(("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: "
                                     + body.getBytes(StandardCharsets.UTF_8).length
                                     + "\r\nConnection: close\r\n\r\n" + body).getBytes(StandardCharsets.UTF_8));
                        } else {
                            String body = "hello-jqt";
                            out.write(("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: "
                                     + body.length() + "\r\nConnection: close\r\n\r\n" + body)
                                     .getBytes(StandardCharsets.UTF_8));
                        }
                        out.flush();
                    } catch (Exception ignored) {
                    }
                }
            }, "mini-http");
            thread.setDaemon(true);
            thread.start();
        }

        int port() { return server.getLocalPort(); }

        @Override public void close() throws Exception {
            server.close();
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("[net] start");
        QApplication app = new QApplication();

        if (!QNetworkAccessManager.isAvailable()) {
            System.out.println("[net] SKIP 本库未编译 QtNetwork");
            return;
        }

        try (MiniHttp http = new MiniHttp()) {
            String base = "http://127.0.0.1:" + http.port();
            QNetworkAccessManager nam = new QNetworkAccessManager();
            nam.setUserAgent("JQt-Smoke/1.0");
            nam.setTransferTimeout(5000);

            AtomicReference<QNetworkReply> ok = new AtomicReference<>();
            AtomicReference<QNetworkReply> missing = new AtomicReference<>();
            AtomicReference<QNetworkReply> refused = new AtomicReference<>();
            AtomicReference<QNetworkReply> posted = new AtomicReference<>();
            AtomicInteger managerLevel = new AtomicInteger();
            nam.onFinished(r -> managerLevel.incrementAndGet());

            nam.get(base + "/ok", ok::set);
            nam.get(base + "/missing", missing::set);
            nam.post(base + "/echo", "PING", "text/plain", posted::set);
            // 端口无人监听 → 连接失败(错误码非 0)。
            // 用一个"刚刚关闭"的端口,确保是即时 RST 而不是等到超时。
            int deadPort;
            try (java.net.ServerSocket tmp = new java.net.ServerSocket(0,
                    4, java.net.InetAddress.getByName("127.0.0.1"))) {
                deadPort = tmp.getLocalPort();
            }
            nam.get("http://127.0.0.1:" + deadPort + "/refused", refused::set);

            QTimer.singleShot(2500, () -> {});
            app.scheduleQuit(2500);
            app.exec();

            // 连接失败路径在 Windows 上可能比 RST 更晚才回到 Qt;
            // 若首个窗口内没回来,再给一个窗口(不拖慢正常路径)。
            if (refused.get() == null) {
                app.scheduleQuit(3000);
                app.exec();
            }

            check("GET 200 状态码(实际 " + (ok.get() == null ? "-" : ok.get().statusCode()) + ")",
                  ok.get() != null && ok.get().statusCode() == 200);
            check("GET 响应体正确(实际 " + (ok.get() == null ? "-" : ok.get().text()) + ")",
                  ok.get() != null && "hello-jqt".equals(ok.get().text()));
            check("GET 无错误(实际 " + (ok.get() == null ? "-" : ok.get().error()) + ")",
                  ok.get() != null && ok.get().error() == QNetworkReply.NoError);
            check("GET ok() == true", ok.get() != null && ok.get().ok());
            check("响应头可用(Content-Type 实际 " + (ok.get() == null ? "-" : ok.get().header("Content-Type")) + ")",
                  ok.get() != null && ok.get().header("Content-Type").startsWith("text/plain"));
            check("User-Agent 已随请求发出",
                  http.getHeaders.toLowerCase().contains("user-agent: jqt-smoke/1.0"));

            check("404 状态码(实际 " + (missing.get() == null ? "-" : missing.get().statusCode()) + ")",
                  missing.get() != null && missing.get().statusCode() == 404);
            check("404 时 ok() == false", missing.get() != null && !missing.get().ok());
            check("404 响应体(实际 " + (missing.get() == null ? "-" : missing.get().text()) + ")",
                  missing.get() != null && "nope".equals(missing.get().text()));

            check("POST 200(实际 " + (posted.get() == null ? "-" : posted.get().statusCode()) + ")",
                  posted.get() != null && posted.get().statusCode() == 200);
            check("POST 服务端收到请求体(实际 " + http.lastBody + ")",
                  posted.get() != null && "echo:PING".equals(posted.get().text()));

            check("连接失败时错误码非 0(实际 "
                  + (refused.get() == null ? "-" : refused.get().error()) + ")",
                  refused.get() != null && refused.get().error() != QNetworkReply.NoError);
            check("连接失败时错误描述非空", refused.get() != null && !refused.get().errorString().isEmpty());

            check("管理器级 onFinished 收到全部 4 个应答(实际 " + managerLevel.get() + ")",
                  managerLevel.get() == 4);
            check("QNetworkReply 是纯 Java 快照(可跨线程保存)", ok.get() != null && ok.get().size() == 9);
        }

        System.out.println("[net] pass=" + pass + " fail=" + fail);
        System.out.println("[net] " + (fail == 0 ? "ALL PASS ✅" : ("FAILED: " + fail)));
        if (fail > 0) System.exit(1);
    }
}
