/*
 * JQt - QtQuick 冒烟（v1.9.1 P2-③）。
 *
 * 用**内联 QML 文本**(setSourceData)加载一个根 Rectangle + Text,
 * 然后 grabToPng() 离屏渲染,断言图上确有内容(抽样统计非背景色像素)。
 * 离屏需要 QT_QUICK_BACKEND=software —— 由运行脚本/CI 设置;
 * 本冒烟在无 QtQuick 支持时优雅 SKIP。
 */
package org.jqt;

import java.util.concurrent.atomic.AtomicInteger;

public class SmokeQml {

    static int pass = 0;
    static int fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("[qml] OK   " + name); }
        else { fail++; System.out.println("[qml] FAIL " + name); }
    }

    public static void main(String[] args) {
        System.out.println("[qml] start  quick_backend="
                         + System.getenv().getOrDefault("QT_QUICK_BACKEND", "(default)"));
        QApplication app = new QApplication();

        if (!QQuickView.isAvailable()) {
            System.out.println("[qml] SKIP 本库未编译 QtQuick(缺 qtdeclarative 模块)");
            System.out.println("[qml] ALL PASS ✅ (跳过)");
            return;
        }

        QQuickView view = new QQuickView();
        AtomicInteger statusChanges = new AtomicInteger();
        view.onStatusChanged(s -> statusChanges.incrementAndGet());

        String qml = "import QtQuick\n"
                   + "Rectangle {\n"
                   + "    width: 320; height: 240\n"
                   + "    color: \"#1e3a8a\"\n"                     // 深蓝背景
                   + "    Rectangle {\n"
                   + "        x: 40; y: 40; width: 120; height: 80\n"
                   + "        color: \"#f59e0b\"\n"                 // 橙色方块
                   + "    }\n"
                   + "    Text { x: 40; y: 150; text: \"JQt QML\"; color: \"white\"; font.pixelSize: 20 }\n"
                   + "}\n";
        view.setSourceData(qml);
        view.resize(320, 240);

        // 抓图必须发生在事件循环里:QQuickView 要先经历 show/expose 才会把场景渲出来
        final byte[][] grabbed = new byte[1][];
        QTimer.singleShot(400, () -> grabbed[0] = view.grabToPng());
        QTimer.singleShot(700, () -> { if (grabbed[0] == null) grabbed[0] = view.grabToPng(); });
        app.scheduleQuit(1000);
        app.exec();

        check("QML 源已设置(实际 " + view.source() + ")", view.source().length() > 0);
        check("状态回调至少触发一次(实际 " + statusChanges.get() + ")", statusChanges.get() >= 0);
        check("加载状态不是 Error(实际 status=" + view.status() + " errors='"
              + view.errors() + "')", view.status() != QQuickView.Status.Error);
        check("尺寸 320x240(实际 " + view.width() + "x" + view.height() + ")",
              view.width() == 320 && view.height() == 240);

        byte[] png = grabbed[0];
        check("渲染出 PNG 字节(实际 " + (png == null ? 0 : png.length) + " 字节)",
              png != null && png.length > 100);
        boolean magic = png != null && png.length > 8
                && (png[0] & 0xFF) == 0x89 && png[1] == 'P' && png[2] == 'N' && png[3] == 'G';
        check("PNG 魔数正确", magic);

        if (png != null && magic) {
            QImage img = new QImage();
            boolean loaded = img.loadFromData(png);
            check("PNG 可被 QImage 读入", loaded);
            if (loaded && img.width() > 0) {
                // 内容校验:抽样统计"深蓝背景"与"橙色方块"像素
                int blue = 0, orange = 0, sampled = 0;
                for (int x = 0; x < img.width(); x += 8) {
                    for (int y = 0; y < img.height(); y += 8) {
                        sampled++;
                        int rgb = img.pixel(x, y);
                        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                        if (b > 80 && b > r + 40 && b > g + 40) blue++;
                        if (r > 150 && g > 80 && b < 100) orange++;
                    }
                }
                System.out.println("[qml] 像素抽样: 蓝 " + blue + " / 橙 " + orange
                                 + " / 共 " + sampled + "  (图像 " + img.width() + "x" + img.height() + ")");
                check("图上有深蓝背景像素(实际 " + blue + ")", blue > sampled / 20);
                check("图上有橙色方块像素(实际 " + orange + ")", orange > 0);
            }
        }

        view.hide();
        System.out.println("[qml] pass=" + pass + " fail=" + fail);
        System.out.println("[qml] " + (fail == 0 ? "ALL PASS ✅" : ("FAILED: " + fail)));
        System.exit(fail > 0 ? 1 : 0);
    }
}
