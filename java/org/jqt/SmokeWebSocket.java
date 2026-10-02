/*
 * JQt - QWebSocket 冒烟（v1.9.1 P1）。
 *
 * 不依赖外网:冒烟自己实现一个最小 WebSocket 回显服务端(握手 + 帧编解码),
 * 因此"连接 → 收欢迎语 → 文本回显 → 二进制回显 → 主动关闭"全链路可确定性复现。
 * 平台不带 QtWebSockets 时优雅 SKIP。
 */
package org.jqt;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class SmokeWebSocket {

    static int pass = 0;
    static int fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("[ws] OK   " + name); }
        else { fail++; System.out.println("[ws] FAIL " + name); }
    }

    /** 最小 WebSocket 服务端:握手 + 文本/二进制回显 + 关闭。 */
    static final class MiniWs implements AutoCloseable {
        private final ServerSocket server;
        private final Thread thread;
        volatile boolean handshaken;
        volatile String lastText = "";

        MiniWs() throws Exception {
            server = new ServerSocket(0, 4, InetAddress.getByName("127.0.0.1"));
            thread = new Thread(this::serve, "mini-ws");
            thread.setDaemon(true);
            thread.start();
        }

        int port() { return server.getLocalPort(); }

        private void serve() {
            while (!server.isClosed()) {
                try (Socket c = server.accept()) {
                    InputStream in = c.getInputStream();
                    OutputStream out = c.getOutputStream();
                    // ---- 握手 ----
                    ByteArrayOutputStream head = new ByteArrayOutputStream();
                    int cur;
                    while ((cur = in.read()) != -1) {
                        head.write(cur);
                        if (head.toString("UTF-8").endsWith("\r\n\r\n")) break;
                    }
                    String req = head.toString("UTF-8");
                    String key = "";
                    for (String line : req.split("\r\n")) {
                        if (line.toLowerCase().startsWith("sec-websocket-key:")) {
                            key = line.substring(line.indexOf(':') + 1).trim();
                        }
                    }
                    String accept = Base64.getEncoder().encodeToString(
                            MessageDigest.getInstance("SHA-1")
                                    .digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                                            .getBytes(StandardCharsets.UTF_8)));
                    out.write(("HTTP/1.1 101 Switching Protocols\r\n"
                             + "Upgrade: websocket\r\nConnection: Upgrade\r\n"
                             + "Sec-WebSocket-Accept: " + accept + "\r\n\r\n")
                             .getBytes(StandardCharsets.UTF_8));
                    out.flush();
                    handshaken = true;
                    sendFrame(out, 1, "welcome".getBytes(StandardCharsets.UTF_8));

                    // ---- 帧循环 ----
                    while (true) {
                        int b0 = in.read();
                        if (b0 < 0) break;
                        int opcode = b0 & 0x0F;
                        int b1 = in.read();
                        if (b1 < 0) break;
                        boolean masked = (b1 & 0x80) != 0;
                        long len = b1 & 0x7F;
                        if (len == 126) {
                            len = ((long) in.read() << 8) | in.read();
                        } else if (len == 127) {
                            len = 0;
                            for (int i = 0; i < 8; i++) len = (len << 8) | in.read();
                        }
                        byte[] mask = new byte[4];
                        if (masked) {
                            int got = 0;
                            while (got < 4) {
                                int n = in.read(mask, got, 4 - got);
                                if (n < 0) break;
                                got += n;
                            }
                        }
                        byte[] payload = new byte[(int) len];
                        int got = 0;
                        while (got < (int) len) {
                            int n = in.read(payload, got, (int) len - got);
                            if (n < 0) break;
                            got += n;
                        }
                        if (masked) {
                            for (int i = 0; i < payload.length; i++) payload[i] ^= mask[i % 4];
                        }
                        if (opcode == 8) {                       // close
                            sendFrame(out, 8, new byte[0]);
                            break;
                        }
                        if (opcode == 1) {
                            lastText = new String(payload, StandardCharsets.UTF_8);
                            sendFrame(out, 1, payload);          // 文本回显
                        } else if (opcode == 2) {
                            sendFrame(out, 2, payload);          // 二进制回显
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        private static void sendFrame(OutputStream out, int opcode, byte[] payload) throws Exception {
            ByteArrayOutputStream f = new ByteArrayOutputStream();
            f.write(0x80 | opcode);
            if (payload.length < 126) {
                f.write(payload.length);
            } else if (payload.length < 65536) {
                f.write(126);
                f.write((payload.length >> 8) & 0xFF);
                f.write(payload.length & 0xFF);
            } else {
                f.write(127);
                for (int i = 7; i >= 0; i--) f.write((int) (((long) payload.length >> (8 * i)) & 0xFF));
            }
            f.write(payload);
            out.write(f.toByteArray());
            out.flush();
        }

        @Override public void close() throws Exception { server.close(); }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("[ws] start");
        QApplication app = new QApplication();

        if (!QWebSocket.isAvailable()) {
            System.out.println("[ws] SKIP 本库未编译 QtWebSockets(缺 qtwebsockets 模块)");
            System.out.println("[ws] ALL PASS ✅ (跳过)");
            return;
        }

        try (MiniWs srv = new MiniWs()) {
            QWebSocket ws = new QWebSocket();
            AtomicInteger connected = new AtomicInteger();
            AtomicInteger disconnected = new AtomicInteger();
            AtomicReference<String> welcome = new AtomicReference<>();
            AtomicReference<String> echo = new AtomicReference<>();
            AtomicReference<byte[]> binEcho = new AtomicReference<>();
            StringBuilder all = new StringBuilder();

            ws.onConnected(() -> {
                connected.incrementAndGet();
                ws.sendText("ping-1");
            });
            ws.onTextMessage(msg -> {
                all.append(msg).append('|');
                if ("welcome".equals(msg)) welcome.set(msg);
                if ("ping-1".equals(msg)) echo.set(msg);
            });
            ws.onBinaryMessage(data -> binEcho.set(data));
            ws.onDisconnected(disconnected::incrementAndGet);

            check("初始状态未连接(" + ws.state() + ")", !ws.isConnected());
            ws.open("ws://127.0.0.1:" + srv.port() + "/echo");

            // 等握手 + 欢迎语;握手后发二进制
            QTimer.singleShot(600, () -> ws.sendBinary(new byte[] {1, 2, 3, 4, 5}));
            QTimer.singleShot(1200, ws::close);
            app.scheduleQuit(2000);
            app.exec();

            check("服务端完成握手", srv.handshaken);
            check("onConnected 触发 1 次(实际 " + connected.get() + ")", connected.get() == 1);
            check("收到欢迎语(收到 " + all + ")", welcome.get() != null);
            check("文本回显正确(收到 " + all + ")", echo.get() != null);
            check("服务端记录到最后文本 = ping-1(实际 " + srv.lastText + ")",
                  "ping-1".equals(srv.lastText));
            check("二进制回显 5 字节(实际 "
                  + (binEcho.get() == null ? "null" : binEcho.get().length + " 字节") + ")",
                  binEcho.get() != null && binEcho.get().length == 5);
            check("二进制内容一致(1,2,3,4,5)", binEcho.get() != null
                  && binEcho.get()[0] == 1 && binEcho.get()[4] == 5);
            check("主动 close 后已断开", disconnected.get() >= 1);
            check("断开后 isConnected=false", !ws.isConnected());
            check("断开后 sendText 返回 false", !ws.sendText("after-close"));
        }

        System.out.println("[ws] pass=" + pass + " fail=" + fail);
        System.out.println("[ws] " + (fail == 0 ? "ALL PASS ✅" : ("FAILED: " + fail)));
        if (fail > 0) System.exit(1);
    }
}
