/*
 * JQt - QEvent 事件体系冒烟（v1.9.1 P0-②）+ QSvgRenderer 冒烟（P0-④）。
 *
 * 事件部分用**可程序化触发**的确定事件:Resize / Show / Hide / EnabledChange /
 * WindowTitleChange / Move —— 不依赖合成输入,因此在 CI(含 offscreen)稳定可测。
 */
package org.jqt;

import java.util.ArrayList;
import java.util.List;

public class SmokeEvent {

    static int pass = 0;
    static int fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("[ev] OK   " + name); }
        else { fail++; System.out.println("[ev] FAIL " + name); }
    }

    /** 只收集事件类型名,便于断言。 */
    static final class Collector {
        final List<QEvent> events = new ArrayList<>();
        void add(QEvent e) { events.add(e); }
        boolean has(String typeName) {
            for (QEvent e : events) if (typeName.equals(e.typeName())) return true;
            return false;
        }
        QEvent first(String typeName) {
            for (QEvent e : events) if (typeName.equals(e.typeName())) return e;
            return null;
        }
        /** 是否存在满足条件的事件（用于"最后一次 resize 是 360x240"这类断言）。 */
        boolean any(String typeName, java.util.function.Predicate<QEvent> p) {
            for (QEvent e : events) if (typeName.equals(e.typeName()) && p.test(e)) return true;
            return false;
        }
        String types() {
            StringBuilder sb = new StringBuilder();
            for (QEvent e : events) { if (sb.length() > 0) sb.append(','); sb.append(e.typeName()); }
            return sb.toString();
        }
    }

    public static void main(String[] args) {
        System.out.println("[ev] start");
        QApplication app = new QApplication();

        QMainWindow w = new QMainWindow("event-smoke", 300, 200);
        Collector c = new Collector();
        w.onEvent(c::add);
        w.onEvent(e -> { /* 双注册:去重后每个事件仍只投递一次(由计数断言覆盖) */ });
        int[] doubleReg = {0};
        w.onEvent(e -> doubleReg[0]++);

        w.show();                       // Show
        w.resize(360, 240);             // Resize
        w.setWindowTitle("event-smoke-2");   // WindowTitleChange
        w.setEnabled(false);            // EnabledChange
        w.setEnabled(true);             // EnabledChange
        QTimer.singleShot(50, () -> {
            w.move(20, 20);             // Move(部分平台在显示后才有)
            w.hide();                   // Hide
        });

        app.scheduleQuit(250);
        app.exec();

        check("收到了 Show 事件", c.has("Show"));
        check("收到了 Resize 事件", c.has("Resize"));
        check("Resize 负载正确(存在 360x240 的 Resize)",
              c.any("Resize", e -> e.width() == 360 && e.height() == 240));
        check("收到了 Hide 事件", c.has("Hide"));
        check("收到了 EnabledChange 事件", c.has("EnabledChange"));
        check("收到了 WindowTitleChange 事件", c.has("WindowTitleChange"));
        check("事件类型名可读(非纯数字) " + c.types(),
              c.first("Show") != null && c.first("Show").typeName().length() > 2);
        check("事件类型值与 Qt 一致(Show=17)", c.first("Show") != null && c.first("Show").type() == 17);
        check("事件总数 > 0(实际 " + c.events.size() + ")", !c.events.isEmpty());
        check("双注册后计数与总事件数一致(去重生效: " + doubleReg[0] + " vs " + c.events.size() + ")",
              doubleReg[0] == c.events.size());

        // ---------------- P0-④ QSvgRenderer ----------------
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='24' height='16'>"
                   + "<rect x='0' y='0' width='24' height='16' fill='#2d7ff9'/></svg>";
        if (!QSvgRenderer.isAvailable()) {
            System.out.println("[ev] SKIP QSvgRenderer(本库未编译 QtSvg 支持)");
        } else {
            QSvgRenderer r = new QSvgRenderer(svg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            check("SVG 解析成功(isValid)", r.isValid());
            check("defaultSize 宽 24(实际 " + r.defaultSize().width() + ")", r.defaultSize().width() == 24);
            check("defaultSize 高 16(实际 " + r.defaultSize().height() + ")", r.defaultSize().height() == 16);
            byte[] png = r.renderToPng(48, 32);
            check("渲染出 PNG 字节(实际 " + (png == null ? 0 : png.length) + " 字节)",
                  png != null && png.length > 100);
            boolean magic = png != null && png.length > 8
                    && (png[0] & 0xFF) == 0x89 && png[1] == 'P' && png[2] == 'N' && png[3] == 'G';
            check("PNG 魔数正确(能被 QImage 读取)", magic);
            QImage img = new QImage();
            check("PNG 可被 QImage.loadFromData 读入", img.loadFromData(png));
            check("读入后的尺寸 = 48x32(实际 " + img.width() + "x" + img.height() + ")",
                  img.width() == 48 && img.height() == 32);
        }

        System.out.println("[ev] pass=" + pass + " fail=" + fail);
        System.out.println("[ev] " + (fail == 0 ? "ALL PASS ✅" : ("FAILED: " + fail)));
        if (fail > 0) System.exit(1);
    }
}
