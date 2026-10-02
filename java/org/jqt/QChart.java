/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.lang.ref.Cleaner;
import java.util.ArrayList;
import java.util.List;

/**
 * 图表（Qt {@code QChart}，QtCharts 模块）。
 * <pre>
 * QChart chart = new QChart();
 * chart.setTitle("过去 7 天");
 * QLineSeries s = new QLineSeries("访问量");
 * s.append(0, 12); s.append(1, 31);
 * chart.addSeries(s);
 * chart.createDefaultAxes();          // 自动建 X/Y 轴
 * QChartView view = new QChartView(chart);
 * view.resize(640, 400);
 * byte[] png = view.toPng();          // 离屏渲染:可直接存文件/贴到界面
 * </pre>
 * <p>QtCharts 不属于 qtbase:某些平台/安装包未附带 —— 先判断
 * {@link #isAvailable()};未编译进本库时构造会抛明确异常。
 */
public class QChart {

    private static final Cleaner CLEANER = Cleaner.create();

    private final List<QLineSeries> series = new ArrayList<>();
    private long nativeHandle;
    private String title = "";
    private boolean legendVisible = true;

    /** 本库是否编译进 QtCharts 支持。 */
    public static boolean isAvailable() { return nativeAvailable(); }
    private static native boolean nativeAvailable();

    /** 创建空图表。 */
    public QChart() {
        if (!isAvailable()) {
            throw new IllegalStateException(
                "本库未编译 QtCharts 支持(缺少 qtcharts 模块);先安装该模块再构建 libjqt");
        }
        nativeHandle = nativeCreate();
        final long h = nativeHandle;
        CLEANER.register(this, () -> nativeDispose(h));
    }

    private static native long nativeCreate();
    private static native void nativeDispose(long handle);

    long nativeHandle() { return nativeHandle; }

    /** 标题。 */
    public void setTitle(String t) {
        title = t == null ? "" : t;
        nativeSetTitle(nativeHandle, title);
    }
    private static native void nativeSetTitle(long handle, String title);

    /** 当前标题。 */
    public String title() { return title; }

    /** 是否显示图例。 */
    public void setLegendVisible(boolean visible) {
        legendVisible = visible;
        nativeSetLegendVisible(nativeHandle, visible);
    }
    private static native void nativeSetLegendVisible(long handle, boolean visible);

    /** 图例是否可见。 */
    public boolean isLegendVisible() { return legendVisible; }

    /** 动画选项（0 = 关闭；见 {@link Animation}）。 */
    public static final class Animation {
        private Animation() {}
        public static final int NoAnimation = 0;
        public static final int GridAxisAnimations = 1;
        public static final int SeriesAnimations = 2;
        public static final int AllAnimations = 3;
    }

    /** 设置动画（离屏冒烟建议用 {@link Animation#NoAnimation} 以求确定）。 */
    public void setAnimationOptions(int options) { nativeSetAnimationOptions(nativeHandle, options); }
    private static native void nativeSetAnimationOptions(long handle, int options);

    /** 添加序列。 */
    public void addSeries(QLineSeries s) {
        series.add(s);
        nativeAddSeries(nativeHandle, s.nativeHandle());
    }
    private static native void nativeAddSeries(long chartHandle, long seriesHandle);

    /** 序列个数。 */
    public int seriesCount() { return nativeSeriesCount(nativeHandle); }
    private static native int nativeSeriesCount(long handle);

    /** 移除并释放全部序列。 */
    public void removeAllSeries() {
        series.clear();
        nativeRemoveAllSeries(nativeHandle);
    }
    private static native void nativeRemoveAllSeries(long handle);

    /** 依据已添加序列自动创建 X/Y 轴。 */
    public void createDefaultAxes() { nativeCreateDefaultAxes(nativeHandle); }
    private static native void nativeCreateDefaultAxes(long handle);
}
