/*
 * JQt - QtCharts 冒烟（v1.9.1 P2-①）。
 *
 * 图表的关键难点是"没有屏幕怎么验证"——答案:QChartView::grab() 离屏渲染成图片。
 * 本冒烟据此断言:序列数据、系列计数、标题/图例、以及**渲染出的 PNG 真的非空且有内容**
 * (不只是字节数 > 0:还校验像素确有非背景色,证明曲线被画出来了)。
 */
package org.jqt;

public class SmokeCharts {

    static int pass = 0;
    static int fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("[chart] OK   " + name); }
        else { fail++; System.out.println("[chart] FAIL " + name); }
    }

    public static void main(String[] args) {
        System.out.println("[chart] start");
        QApplication app = new QApplication();

        if (!QChart.isAvailable()) {
            System.out.println("[chart] SKIP 本库未编译 QtCharts(缺 qtcharts 模块)");
            System.out.println("[chart] ALL PASS ✅ (跳过)");
            return;
        }

        // ---- 数据序列 ----
        QLineSeries s1 = new QLineSeries("温度");
        s1.append(0, 20.0);
        s1.append(1, 22.5);
        s1.append(2, 19.0);
        s1.append(3, 25.0);
        check("序列名(实际 " + s1.name() + ")", "温度".equals(s1.name()));
        check("数据点 4 个(原生计数 " + s1.count() + ")", s1.count() == 4);
        check("Java 侧点坐标可读(0,20.0)", s1.x(0) == 0.0 && s1.y(0) == 20.0);
        check("Java 侧点坐标可读(3,25.0)", s1.x(3) == 3.0 && s1.y(3) == 25.0);
        check("越界读点返回 NaN", Double.isNaN(s1.y(99)));
        s1.setName("气温");
        check("改名生效(实际 " + s1.name() + ")", "气温".equals(s1.name()));

        QLineSeries s2 = new QLineSeries("湿度");
        s2.append(0, 40.0);
        s2.append(3, 60.0);

        // ---- 图表 ----
        QChart chart = new QChart();
        chart.setTitle("过去 4 小时");
        chart.setAnimationOptions(QChart.Animation.NoAnimation);   // 离屏求确定
        chart.addSeries(s1);
        chart.addSeries(s2);
        chart.createDefaultAxes();
        check("系列计数 2(实际 " + chart.seriesCount() + ")", chart.seriesCount() == 2);
        check("标题已设置(实际 " + chart.title() + ")", "过去 4 小时".equals(chart.title()));
        check("图例默认可见", chart.isLegendVisible());

        // ---- 视图 + 离屏渲染 ----
        QChartView view = new QChartView(chart);
        view.setAntialiasing(true);
        view.resize(640, 400);
        check("视图尺寸 640x400(实际 " + view.renderWidth() + "x" + view.renderHeight() + ")",
              view.renderWidth() == 640 && view.renderHeight() == 400);
        check("视图绑定的图表就是入参", view.chart() == chart);

        byte[] png = view.toPng();
        check("渲染出 PNG 字节(实际 " + (png == null ? 0 : png.length) + " 字节)",
              png != null && png.length > 1000);
        boolean magic = png != null && png.length > 8
                && (png[0] & 0xFF) == 0x89 && png[1] == 'P' && png[2] == 'N' && png[3] == 'G';
        check("PNG 魔数正确", magic);

        QImage img = new QImage();
        check("PNG 可被 QImage 读入", img.loadFromData(png));
        // 高 DPI 下 grab() 给出的是物理像素(例如 1.5x -> 960x600),
        // 因此按"整数倍缩放 + 宽高比"断言,做到 DPI 无关。
        double scale = (double) img.width() / Math.max(1, view.renderWidth());
        boolean scaled = scale >= 1.0
                && img.height() == (int) Math.round(view.renderHeight() * scale);
        check("图像尺寸 = 视图尺寸 x 设备像素比(视图 640x400,图像 " + img.width() + "x" + img.height()
              + ",倍率 " + scale + ")", scaled);

        // 内容校验:统计非纯白像素比例,证明确实画了东西(而不是空白图)
        int nonWhite = 0, sampled = 0;
        for (int x = 0; x < img.width(); x += 8) {
            for (int y = 0; y < img.height(); y += 8) {
                sampled++;
                int rgb = img.pixel(x, y);
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                if (!(r > 245 && g > 245 && b > 245)) nonWhite++;
            }
        }
        check("图上有绘制内容(非纯白像素 " + nonWhite + "/" + sampled + ")", nonWhite > sampled / 100);

        // 移除序列
        chart.removeAllSeries();
        check("removeAllSeries 后计数 0(实际 " + chart.seriesCount() + ")", chart.seriesCount() == 0);

        QMainWindow w = new QMainWindow("chart", 300, 200);
        w.show();
        app.scheduleQuit(200);
        app.exec();

        System.out.println("[chart] pass=" + pass + " fail=" + fail);
        System.out.println("[chart] " + (fail == 0 ? "ALL PASS ✅" : ("FAILED: " + fail)));
        if (fail > 0) System.exit(1);
    }
}
