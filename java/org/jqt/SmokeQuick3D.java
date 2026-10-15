/*
 * JQt - Qt Quick 3D 冒烟（v1.9.2 P2）。
 *
 * 目标:证明"用 QML 真的把一个 Quick 3D 场景加载/渲染出来了",而不只是"API 能调"。
 * 做法与 SmokeQml/SmokeCharts 一致 —— 抓图 + 逐像素校验;不同之处是
 * Quick 3D **必须有可用的 RHI**(软件后端 QT_QUICK_BACKEND=software 渲染不出 3D),
 * 所以这里对"环境渲染不出"给出明确 SKIP,绝不把跳过算成 pass:
 *
 *   1) QQuick3D.isAvailable() 用真实加载 "import QtQuick3D" 的 QML 判定;
 *   2) 场景 QML 里放了一个 2D 绿底 Rectangle(对照组)+ 一个 View3D(深蓝清屏色 + 橙色立方体),
 *      于是像素统计能区分三种情况:
 *        · 有橙色立方体/深蓝清屏色 → 3D 真的渲染出来了 → 内容检查按**真断言**执行;
 *        · 只有绿底(2D 场景图正常,View3D 没出内容) → 环境 RHI 不支持 Quick 3D → SKIP;
 *        · 纯色/空图(场景图整体没渲染) → 环境没有可用图形栈 → SKIP。
 *   3) 无论可用与否,都做一遍"负向/机制"真检查:探测结论与实际加载一致、
 *      不可用时 errorString() 必须给出原因、dispose() 后抓图返回 null 且临时文件被删。
 *
 * <p><b>必须在真窗口下跑才能把 3D 内容检查做成真断言</b>(Windows 实测):
 * <ul>
 *   <li>{@code QT_QPA_PLATFORM=offscreen} → Qt Quick 直接退到<b>软件后端</b>
 *       ({@code qt.scenegraph.general: Loading backend software}),offscreen 平台插件
 *       没有任何 GL/RHI 表面,View3D 画不出来 ⇒ 内容检查只能 SKIP(见下);</li>
 *   <li>真窗口({@code QT_QPA_PLATFORM=windows},默认 RHI=D3D11,无独显走 WARP)
 *       → 3D 真的渲染出来,内容检查按真断言执行、skip=0。</li>
 * </ul>
 * 因此"Quick 3D 真的渲染出来了"的证据请用真窗口那次运行(像素统计会打印立方体/清屏色点数)。
 *
 * <p><b>可选开关</b>(都不影响默认行为):
 * <ul>
 *   <li>{@code JQT_Q3D_PNG=<路径>} —— 把抓到的 PNG 存下来(人工看图/写报告用);</li>
 *   <li>{@code JQT_Q3D_STRICT=1} —— 严格模式:任何 SKIP 记为 FAIL(CI 里"必须真渲染"时用,
 *       避免"环境不支持 → SKIP → 退出码 0"被当成渲染通过);</li>
 *   <li>{@code JQT_Q3D_WAIT_MS=<毫秒>} —— 第一次抓图前的等待(默认 700)。慢机器(CI runner)上
 *       首帧的 Quick3D 着色器/管线创建可能晚于 700ms,于是抓到一帧"没有 3D"的画面 →
 *       走 SKIP 分支 → 严格模式下变成假阴性红。把它调大(CI 用 2100 = 3 倍)可以把
 *       "环境慢"与"真的渲染不出来"分开:后者无论等多久都没有立方体像素。
 *       第二/第三次时机按这个值等比拉开(间隔 = max(400, 值/2)),默认值下
 *       恰为 700 / 1100 / 1500 —— 与本冒烟的历史行为完全一致。</li>
 * </ul>
 */
package org.jqt;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class SmokeQuick3D {

    static int pass = 0;
    static int fail = 0;
    static int skip = 0;

    /** 严格模式(JQT_Q3D_STRICT=1):任何 SKIP 都记为 FAIL —— CI 里用它要求"必须真渲染"。 */
    static final boolean STRICT = isOn(System.getenv("JQT_Q3D_STRICT"));

    /**
     * 第一次抓图前的等待(ms):{@code JQT_Q3D_WAIT_MS},默认 700(历史行为)。
     *
     * <p>为什么要可调:慢机器上首帧(含 Quick3D 的着色器/管线创建)可能晚于 700ms,
     * 抓到的会是"没有 3D"的画面 → SKIP → 严格模式下假阴性红。调大它是把
     * "环境慢"与"真的渲染不出来"区分开 —— 不是放宽断言:像素阈值与 skip=0 一个都没动。
     */
    static final int WAIT_MS = intEnv("JQT_Q3D_WAIT_MS", 700, 100);
    /** 第二次抓图与退出时刻:间隔 = max(400, WAIT_MS/2) ⇒ 默认 700/1100/1500 保持不变。 */
    static final int GRAB_GAP_MS = Math.max(400, WAIT_MS / 2);
    static final int GRAB1_MS = WAIT_MS;
    static final int GRAB2_MS = GRAB1_MS + GRAB_GAP_MS;
    static final int QUIT_MS = GRAB2_MS + GRAB_GAP_MS;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("[q3d] OK   " + name); }
        else { fail++; System.out.println("[q3d] FAIL " + name); }
    }

    /** 环境/平台限制导致本项无法真验证:打印并计入 skip(**不算 pass**)。 */
    static void skip(String why) {
        if (STRICT) {
            fail++;
            System.out.println("[q3d] FAIL " + why + "  [JQT_Q3D_STRICT=1:跳过不算通过]");
            return;
        }
        skip++;
        System.out.println("[q3d] SKIP " + why);
    }

    static boolean isOn(String v) {
        if (v == null) return false;
        String s = v.trim().toLowerCase();
        return s.equals("1") || s.equals("true") || s.equals("yes") || s.equals("on");
    }

    /** 读一个正整数环境变量:未设置/非数字 → 默认值;小于下限 → 抬到下限(避免 0ms 这种无意义值)。 */
    static int intEnv(String name, int def, int min) {
        String v = System.getenv(name);
        if (v == null || v.trim().isEmpty()) return def;
        try {
            int n = Integer.parseInt(v.trim());
            return n < min ? min : n;
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * 冒烟场景:2D 绿底对照组 + 3D 场景(深蓝清屏色 + 橙色无光照立方体)。
     * 立方体用 PrincipledMaterial.NoLighting,不吃灯光,颜色稳定 ⇒ 像素统计可判定。
     */
    static final String SCENE_QML =
            "import QtQuick\n"
          + "import QtQuick3D\n"
          + "\n"
          + "Item {\n"
          + "    width: 320; height: 240\n"
          + "\n"
          + "    // 2D 对照组:证明抓图/场景图这条链路本身通\n"
          + "    Rectangle { anchors.fill: parent; color: \"#22c55e\" }\n"
          + "\n"
          + "    View3D {\n"
          + "        anchors.fill: parent\n"
          + "        environment: SceneEnvironment {\n"
          + "            backgroundMode: SceneEnvironment.Color\n"
          + "            clearColor: \"#1e3a8a\"\n"
          + "        }\n"
          + "        PerspectiveCamera { position: Qt.vector3d(0, 0, 400) }\n"
          + "        Model {\n"
          + "            source: \"#Cube\"\n"
          + "            scale: Qt.vector3d(1.6, 1.6, 1.6)\n"
          + "            eulerRotation: Qt.vector3d(-20, 30, 0)\n"
          + "            materials: PrincipledMaterial {\n"
          + "                baseColor: \"#f59e0b\"\n"
          + "                lighting: PrincipledMaterial.NoLighting\n"
          + "            }\n"
          + "        }\n"
          + "    }\n"
          + "}\n";

    public static void main(String[] args) throws Exception {
        System.out.println("[q3d] start  platform=" + env("QT_QPA_PLATFORM")
                         + " quickBackend=" + env("QT_QUICK_BACKEND")
                         + " rhi=" + env("QSG_RHI_BACKEND")
                         + " qml2ImportPath=" + env("QML2_IMPORT_PATH"));
        System.out.println("[q3d] 渲染环境: " + renderEnvNote());
        if (STRICT) System.out.println("[q3d] 严格模式: 开(JQT_Q3D_STRICT="
                                      + env("JQT_Q3D_STRICT") + ")—— 任何 SKIP 记为 FAIL");
        QApplication app = new QApplication();

        // ---------- 1. 版本与探测(真的加载 import QtQuick3D 的 QML) ----------
        String ver = QQuick3D.qtVersion();
        check("Qt 运行库版本可读(实际 '" + ver + "')", !ver.isEmpty());

        System.out.println("[q3d] ---- QQuick3D.status() ----");
        for (String line : QQuick3D.status().split("\n")) System.out.println("[q3d] " + line);
        System.out.println("[q3d] --------------------------");

        boolean avail = QQuick3D.isAvailable();
        check("status() 给出明确结论(" + (avail ? "可用 ✅" : "不可用 ❌") + ")",
              QQuick3D.status().contains(avail ? "可用 ✅" : "不可用 ❌"));
        check("isAvailable() 结论稳定(缓存:连调两次一致)", avail == QQuick3D.isAvailable());
        System.out.println("[q3d] 探测结论: Quick3D " + (avail ? "可用" : "不可用")
                         + ", 编译期宏 JQT_HAVE_QUICK3D=" + QQuick3D.compiledIn()
                         + ", Qt=" + ver);

        // ---------- 2. 视图 + loadQml(临时文件 → setSource) ----------
        QQuick3DView view = new QQuick3DView(320, 240);
        check("视图尺寸 320x240(实际 " + view.width() + "x" + view.height() + ")",
              view.width() == 320 && view.height() == 240);
        check("新视图未释放、状态为 Null(实际 " + QQuick3D.statusName(view.status()) + ")",
              !view.isDisposed() && view.status() == QQuickView.Status.Null);

        view.loadQml(SCENE_QML);
        String qmlPath = view.qmlFile();
        Path qmlFile = qmlPath.isEmpty() ? null : Path.of(qmlPath);
        check("QML 已写到临时文件(实际 '" + qmlPath + "')",
              qmlFile != null && Files.exists(qmlFile));
        check("view.source() 指向该临时文件(实际 '" + view.source() + "')",
              !qmlPath.isEmpty() && view.source().replace('\\', '/').endsWith(qmlFile.getFileName().toString()));
        String onDisk = (qmlFile != null && Files.exists(qmlFile))
                ? Files.readString(qmlFile, StandardCharsets.UTF_8) : "";
        check("临时文件内容与传入 QML 完全一致(实际 " + onDisk.length() + " 字符)",
              onDisk.equals(SCENE_QML));
        check("加载后状态不再是 Null(实际 " + QQuick3D.statusName(view.status()) + ")",
              view.status() != QQuickView.Status.Null);

        // ---------- 3. 事件循环里抓图(先 show/expose 才有画面) ----------
        // 两次抓图:第二次**无条件覆盖**第一次(场景是静态的,渲染出来后两帧内容一致)。
        // 这样"首帧还没画完"不会再被锁进结论里 —— 慢环境多等一会儿,像素断言照旧判定。
        final byte[][] grabbed = new byte[1][];
        System.out.println("[q3d] 抓图时机: " + GRAB1_MS + "ms / " + GRAB2_MS + "ms, 退出 "
                         + QUIT_MS + "ms(JQT_Q3D_WAIT_MS=" + WAIT_MS
                         + ", 慢机器/CI 用 2100)");
        view.show();
        QTimer.singleShot(GRAB1_MS, () -> grabbed[0] = view.grabToPng());
        QTimer.singleShot(GRAB2_MS, () -> grabbed[0] = view.grabToPng());
        app.scheduleQuit(QUIT_MS);
        app.exec();

        boolean ready = view.isReady();
        String err = view.errorString();
        if (!err.isEmpty()) {
            System.out.println("[q3d] QML 错误原文:");
            for (String line : err.split("\n")) System.out.println("[q3d]   " + line);
        }
        check("探测结论与视图实际加载结论一致(探测=" + avail + ", 视图 ready=" + ready + ")",
              avail == ready);

        byte[] png = grabbed[0];
        dumpPng(png);

        if (!avail) {
            // 环境不支持 Quick 3D:做**负向真检查**,渲染内容检查如实 SKIP
            check("不可用时视图如实未就绪(isReady()=false)", !ready);
            check("errorString() 给出失败原因(实际 " + err.length() + " 字符)", !err.isEmpty());
            boolean threw = false;
            byte[] p = null;
            try { p = view.grabToPng(); } catch (Throwable t) { threw = true; }
            check("不可用时 grabToPng() 不抛异常(返回 " + (p == null ? "null" : p.length + " 字节") + ")", !threw);
            String why = err.isEmpty() ? QQuick3D.status() : firstLine(err);
            skip("3D 内容渲染检查(画面是否真有立方体/清屏色) —— Quick3D 不可用: " + why);
            skip("清屏色 #1e3a8a 生效检查 —— Quick3D 不可用");
            skip("渲染图尺寸/设备像素比检查 —— Quick3D 不可用(空场景图,没有 3D 内容可校验)");
        } else if (png == null || png.length < 100) {
            skip("抓图/内容检查 —— Quick3D 可用但没有画面(" + (png == null ? "null" : png.length + " 字节")
                 + "):" + renderEnvNote() + renderEnvFix());
            skip("3D 内容渲染检查(立方体像素) —— 同上");
            skip("清屏色 #1e3a8a 生效检查 —— 同上");
        } else {
            // Quick3D 可用:PNG 机制做真断言,内容按像素统计判定"真渲染 / 环境渲染不出"
            boolean magic = png.length > 8 && (png[0] & 0xFF) == 0x89
                         && png[1] == 'P' && png[2] == 'N' && png[3] == 'G';
            check("PNG 魔数正确(" + png.length + " 字节)", magic);
            QImage img = new QImage();
            boolean loaded = magic && img.loadFromData(png);
            check("PNG 可被 QImage 读入", loaded);
            if (!loaded || img.width() <= 0) {
                skip("渲染图尺寸/像素内容检查 —— PNG 解不开(无法进一步分析)");
            } else {
                double scale = (double) img.width() / Math.max(1, view.width());
                boolean sizeOk = scale >= 1.0
                        && img.height() == (int) Math.round(view.height() * scale);
                check("图像尺寸 = 视图尺寸 x 设备像素比(视图 320x240,图像 " + img.width() + "x"
                      + img.height() + ", 倍率 " + scale + ")", sizeOk);
                Pixels px = analyze(img);
                System.out.println("[q3d] ---- 像素证据(共 " + px.sampled + " 抽样点, 图像 "
                                 + img.width() + "x" + img.height() + ") ----");
                System.out.println("[q3d] 像素抽样: 共 " + px.sampled + " 点, 主色 " + px.dominantHex()
                                 + " x" + px.dominantCount + ", 非主色 " + px.nonBackground
                                 + ", 暖色(立方体#f59e0b) " + px.warm
                                 + ", 深蓝(清屏#1e3a8a) " + px.bluish + ", 绿(2D对照#22c55e) " + px.greenish);
                System.out.println("[q3d] 立方体类暖色 #f59e0b : " + px.warm + " 点 ("
                                 + pct(px.warm, px.sampled) + "%)");
                System.out.println("[q3d] 清屏类深蓝 #1e3a8a : " + px.bluish + " 点 ("
                                 + pct(px.bluish, px.sampled) + "%)");
                System.out.println("[q3d] 2D 对照组绿 #22c55e : " + px.greenish + " 点 ("
                                 + pct(px.greenish, px.sampled) + "%), 主色占比 "
                                 + pct(px.dominantCount, px.sampled) + "%");
                System.out.println("[q3d] ------------------------------------------");
                boolean threeDRendered = px.warm > 0 || px.bluish > px.sampled / 20;
                if (threeDRendered) {
                    // 真渲染:以下全部按真断言执行(阈值来自需求:#f59e0b > 0、#1e3a8a 占比 > 5%)
                    check("3D 场景渲染出内容,画面非纯色(主色占比 " + pct(px.dominantCount, px.sampled)
                          + "% < 99%, 非主色像素 " + px.nonBackground + "/" + px.sampled + ")",
                          px.nonBackground > 0 && px.dominantCount < px.sampled * 99 / 100);
                    check("立方体材质色 #f59e0b 像素 > 0(实际 " + px.warm + " 点)", px.warm > 0);
                    check("清屏色 #1e3a8a 生效(深蓝像素 " + px.bluish + "/" + px.sampled + " = "
                          + pct(px.bluish, px.sampled) + "% > 5%)", px.bluish * 20 > px.sampled);
                } else if (px.greenish > px.sampled / 20) {
                    skip("3D 内容渲染检查 —— 2D 场景图正常(绿底可见)但 View3D 没出内容:" + renderEnvNote() + renderEnvFix());
                    skip("立方体像素检查 —— 同上");
                    skip("清屏色 #1e3a8a 检查 —— 同上");
                } else {
                    skip("3D 内容渲染检查 —— 抓到的图是纯色(主色 " + px.dominantHex()
                         + "):整个场景图都没渲染出来 —— " + renderEnvNote() + renderEnvFix());
                    skip("立方体像素检查 —— 同上");
                    skip("清屏色 #1e3a8a 检查 —— 同上");
                }
            }
        }

        // ---------- 4. dispose(幂等 + 释放 + 删临时文件) ----------
        view.dispose();
        check("dispose() 后 isDisposed()=true", view.isDisposed());
        check("dispose() 后 grabToPng() 返回 null(实际 "
              + (view.grabToPng() == null ? "null" : "非 null") + ")", view.grabToPng() == null);
        check("dispose() 后临时 QML 已删除(实际 exists="
              + (qmlFile != null && Files.exists(qmlFile)) + ")",
              qmlFile != null && !Files.exists(qmlFile));
        boolean again = true;
        try { view.dispose(); } catch (Throwable t) { again = false; }
        check("dispose() 幂等(重复调用不抛异常)", again);

        System.out.println("[q3d] pass=" + pass + " fail=" + fail + " skip=" + skip);
        if (skip > 0) {
            System.out.println("[q3d] 注意: 有 " + skip + " 项因环境限制被跳过(SKIP 不计入 pass)");
            System.out.println("[q3d] 真渲染验证请用真窗口跑(Windows: QT_QPA_PLATFORM=windows 或不设该变量;"
                             + "Linux: xcb + 可用 GL),或加 JQT_Q3D_STRICT=1 让跳过直接算失败");
        }
        System.out.println("[q3d] " + (fail == 0 ? "ALL PASS ✅" : ("FAILED: " + fail)));
        System.exit(fail > 0 ? 1 : 0);
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    /** 抽样统计结果。 */
    static final class Pixels {
        int sampled;
        int dominant;
        int dominantCount;
        int nonBackground;
        int warm;       // 暖色:立方体材质 #f59e0b
        int bluish;     // 深蓝:清屏色 #1e3a8a
        int greenish;   // 绿:2D 对照组 #22c55e
        String dominantHex() { return String.format("#%06x", dominant & 0xFFFFFF); }
    }

    /** 每隔 4 像素采样:统计主色(出现最多的颜色)与三类特征像素。 */
    static Pixels analyze(QImage img) {
        Pixels p = new Pixels();
        Map<Integer, Integer> counts = new HashMap<>();
        for (int x = 0; x < img.width(); x += 4) {
            for (int y = 0; y < img.height(); y += 4) {
                int rgb = img.pixel(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                p.sampled++;
                counts.merge(rgb & 0xFFFFFF, 1, Integer::sum);
                if (r > 120 && r > b + 50 && g > 60) p.warm++;
                if (b > 80 && b > r + 25 && b > g + 20) p.bluish++;
                if (g > 120 && g > r + 40 && g > b + 40) p.greenish++;
            }
        }
        for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
            if (e.getValue() > p.dominantCount) {
                p.dominantCount = e.getValue();
                p.dominant = e.getKey();
            }
        }
        p.nonBackground = p.sampled - p.dominantCount;
        return p;
    }

    static String firstLine(String s) {
        if (s == null) return "";
        int i = s.indexOf('\n');
        return i < 0 ? s : s.substring(0, i);
    }

    /** 占比(百分比,一位小数);分母为 0 时回 0.0。 */
    static String pct(int n, int total) {
        return String.format("%.1f", total <= 0 ? 0.0 : (100.0 * n / total));
    }

    /**
     * 当前渲染环境的<b>客观描述</b>(只进日志/SKIP 文本,不参与判定)。
     *
     * <p>两条 Windows 实测结论写在文案里(见 {@link QQuick3D} 类注释):
     * {@code offscreen} 平台插件没有任何 GL/RHI 表面(实测 {@code qoffscreen.dll} 只导入
     * Qt6Core/Qt6Gui,不链接 opengl32/D3D),Qt Quick 只能退到软件后端;而 Quick 3D 必须有 RHI。
     */
    static String renderEnvNote() {
        String qpa = System.getenv("QT_QPA_PLATFORM");
        String qb = System.getenv("QT_QUICK_BACKEND");
        if (qb != null && qb.toLowerCase().contains("software")) {
            return "QT_QUICK_BACKEND=software(软件光栅:Quick 3D 一定没有画面)";
        }
        if (qpa != null && qpa.toLowerCase().contains("offscreen")) {
            return "QT_QPA_PLATFORM=offscreen(该平台插件没有 GL/RHI 表面,Qt Quick 只能退到软件后端,"
                 + "View3D 一定没有画面)";
        }
        return "真窗口平台(QT_QPA_PLATFORM=" + env("QT_QPA_PLATFORM")
             + (qb == null ? ", 后端默认" : ", QT_QUICK_BACKEND=" + qb)
             + "):RHI 可用,预期 3D 能渲染出来";
    }

    /** 出不了 3D 时的补救建议(拼在 SKIP 文本后面)。 */
    static String renderEnvFix() {
        return " -> 需真窗口: QT_QPA_PLATFORM=windows(Windows,默认 D3D11/WARP)或 Linux xcb+GL";
    }

    /**
     * 把抓到的 PNG 存到 {@code JQT_Q3D_PNG} 指定的路径(人工看图/写报告用)。
     * 没设该环境变量、或没有画面时什么都不做 —— <b>这不是一项检查</b>,失败只记一行日志。
     */
    static void dumpPng(byte[] png) {
        String path = System.getenv("JQT_Q3D_PNG");
        if (path == null || path.trim().isEmpty()) return;
        if (png == null || png.length == 0) {
            System.out.println("[q3d] JQT_Q3D_PNG=" + path + " 但没有可写的画面(抓图为空)");
            return;
        }
        try {
            Path out = Path.of(path.trim()).toAbsolutePath();
            if (out.getParent() != null) Files.createDirectories(out.getParent());
            Files.write(out, png);
            System.out.println("[q3d] 渲染图已存: " + out + " (" + png.length + " 字节, "
                             + Files.size(out) + " 字节落盘)");
        } catch (Exception e) {
            System.out.println("[q3d] 渲染图写盘失败(不影响判定): " + e);
        }
    }

    static String env(String name) {
        String v = System.getenv(name);
        return (v == null || v.isEmpty()) ? "(未设置)" : v;
    }
}
