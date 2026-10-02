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
 * 折线/散点数据序列（Qt {@code QLineSeries}，QtCharts 模块）。
 * <pre>
 * QLineSeries s = new QLineSeries("温度");
 * s.append(0, 20.5);
 * s.append(1, 21.0);
 * chart.addSeries(s);
 * </pre>
 * 可用性见 {@link QChart#isAvailable()}。
 */
public class QLineSeries {

    private static final Cleaner CLEANER = Cleaner.create();
    private final List<double[]> localPoints = new ArrayList<>();
    private long nativeHandle;

    /** 创建空序列。 */
    public QLineSeries() {
        this(null);
    }

    /** 创建带名字的序列（图例显示）。 */
    public QLineSeries(String name) {
        nativeHandle = nativeCreate(name == null ? "" : name);
        final long h = nativeHandle;
        CLEANER.register(this, () -> nativeDispose(h));
    }

    private static native long nativeCreate(String name);
    private static native void nativeDispose(long handle);

    long nativeHandle() { return nativeHandle; }

    /** 追加一个数据点。 */
    public void append(double x, double y) {
        localPoints.add(new double[] {x, y});
        nativeAppend(nativeHandle, x, y);
    }
    private static native void nativeAppend(long handle, double x, double y);

    /** 数据点个数。 */
    public int count() { return nativeCount(nativeHandle); }
    private static native int nativeCount(long handle);

    /** 第 index 个点的 X（越界返回 NaN）。 */
    public double x(int index) {
        if (index < 0 || index >= localPoints.size()) return Double.NaN;
        return localPoints.get(index)[0];
    }

    /** 第 index 个点的 Y（越界返回 NaN）。 */
    public double y(int index) {
        if (index < 0 || index >= localPoints.size()) return Double.NaN;
        return localPoints.get(index)[1];
    }

    /** 清空数据点。 */
    public void clear() { localPoints.clear(); nativeClear(nativeHandle); }
    private static native void nativeClear(long handle);

    /** 序列名（图例）。 */
    public String name() { return nativeName(nativeHandle); }
    private static native String nativeName(long handle);

    /** 设置序列名。 */
    public void setName(String name) { nativeSetName(nativeHandle, name == null ? "" : name); }
    private static native void nativeSetName(long handle, String name);
}
