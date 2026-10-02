/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.lang.ref.Cleaner;

/**
 * 图表视图（Qt {@code QChartView}，QtCharts 模块）—— 可直接放进布局显示。
 * <pre>
 * QChartView view = new QChartView(chart);
 * view.setAntialiasing(true);
 * view.resize(640, 400);
 * byte[] png = view.toPng();      // 离屏导出(无需真窗口)
 * </pre>
 * 继承 {@link QWidget}，因此可用 JQt 的布局/样式 API（{@code setLayout}、QSS 等）。
 */
public class QChartView extends QWidget {

    /** 渲染提示（Qt {@code QPainter::RenderHint} 子集）。 */
    public static final class RenderHint {
        private RenderHint() {}
        public static final int Antialiasing = 0x01;
        public static final int TextAntialiasing = 0x02;
        public static final int SmoothPixmapTransform = 0x04;
    }

    private static final Cleaner CLEANER = Cleaner.create();
    private final QChart chart;

    /** 用给定图表创建视图。 */
    public QChartView(QChart chart) {
        if (!QChart.isAvailable()) {
            throw new IllegalStateException("本库未编译 QtCharts 支持(缺少 qtcharts 模块)");
        }
        this.chart = chart;
        nativeHandle = nativeCreate(chart.nativeHandle());
        final long h = nativeHandle;
        CLEANER.register(this, () -> nativeDispose(h));
    }

    private static native long nativeCreate(long chartHandle);
    private static native void nativeDispose(long handle);

    /** 关联的图表。 */
    public QChart chart() { return chart; }

    /** 开关渲染提示位（默认开启抗锯齿,曲线才不锯齿）。 */
    public void setRenderHint(int hint, boolean on) { nativeSetRenderHint(nativeHandle, hint, on); }
    private static native void nativeSetRenderHint(long handle, int hint, boolean on);

    /** 便捷:开/关抗锯齿。 */
    public void setAntialiasing(boolean on) { setRenderHint(RenderHint.Antialiasing, on); }

    /** 把当前图表渲染成 PNG 字节（离屏可用,不依赖窗口显示）。 */
    public byte[] toPng() { return nativeToPng(nativeHandle); }
    private static native byte[] nativeToPng(long handle);

    /** 渲染像素尺寸(宽度)。 */
    public int renderWidth() { return nativeRenderWidth(nativeHandle); }
    private static native int nativeRenderWidth(long handle);

    /** 渲染像素尺寸(高度)。 */
    public int renderHeight() { return nativeRenderHeight(nativeHandle); }
    private static native int nativeRenderHeight(long handle);

    /** 渲染并把 PNG 写入文件（返回是否成功）。 */
    public boolean savePng(String fileName) {
        byte[] png = toPng();
        if (png == null || png.length == 0) return false;
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get(fileName), png);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
