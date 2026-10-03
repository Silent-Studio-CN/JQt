/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.lang.ref.Cleaner;

/**
 * 地理坐标（Qt {@code QGeoCoordinate}，QtPositioning 模块）。
 * <pre>
 * QGeoCoordinate beijing = new QGeoCoordinate(39.9042, 116.4074);
 * QGeoCoordinate shanghai = new QGeoCoordinate(31.2304, 121.4737);
 * double meters = beijing.distanceTo(shanghai);      // ≈ 1067 km
 * double deg    = beijing.azimuthTo(shanghai);       // ≈ 122°
 * QGeoCoordinate east = beijing.atDistanceAndAzimuth(100_000, 90);   // 正东 100km
 * </pre>
 * <b>无需任何硬件或定位服务</b>：本类是纯几何计算（大圆距离/方位角），
 * 可在无 GPS 的环境（CI/服务器）里确定性地使用与测试。
 * <p>可用性:{@link #isAvailable()}（QtPositioning 不属于 qtbase）。
 */
public class QGeoCoordinate {

    /**
     * 字符串格式（Qt {@code QGeoCoordinate::CoordinateFormat}）。
     * <p>取值与 Qt 枚举**逐一对齐**（0~5）;海拔已设置时字符串末尾会附加如 {@code 28.1m}。
     */
    public static final class Format {
        private Format() {}
        /** 0:十进制度 —— {@code -27.46758°, 153.02789°, 28.1m} */
        public static final int Degrees = 0;
        /** 1:十进制度 + 半球 —— {@code 27.46758° S, 153.02789° E} */
        public static final int DegreesWithHemisphere = 1;
        /** 2:度分 —— {@code -27° 28.054', 153° 1.673'} */
        public static final int DegreesMinutes = 2;
        /** 3:度分 + 半球 —— {@code 27° 28.054 S', 153° 1.673' E} */
        public static final int DegreesMinutesWithHemisphere = 3;
        /** 4:度分秒 —— {@code -27° 28' 3.2", 153° 1' 40.4"} */
        public static final int DegreesMinutesSeconds = 4;
        /** 5:度分秒 + 半球(toString() 的默认值) —— {@code 27° 28' 3.2" S, 153° 1' 40.4" E} */
        public static final int DegreesMinutesSecondsWithHemisphere = 5;
    }

    /** 坐标类型（Qt {@code QGeoCoordinate::CoordinateType}）。 */
    public static final class Type {
        private Type() {}
        public static final int Invalid = 0;
        public static final int Coordinate2D = 1;
        public static final int Coordinate3D = 2;
    }

    private static final Cleaner CLEANER = Cleaner.create();
    private long nativeHandle;
    private double latitude;
    private double longitude;
    private double altitude;
    private boolean threeD;

    /** 本库是否编译进 QtPositioning 支持。 */
    public static boolean isAvailable() { return nativeAvailable(); }
    private static native boolean nativeAvailable();

    /** 无效坐标（未设置经纬度）。 */
    public QGeoCoordinate() {
        this(Double.NaN, Double.NaN, Double.NaN, false);
    }

    /** 二维坐标（纬度、经度，单位度）。 */
    public QGeoCoordinate(double latitude, double longitude) {
        this(latitude, longitude, 0.0, false);
    }

    /** 三维坐标（纬度、经度、海拔米）。 */
    public QGeoCoordinate(double latitude, double longitude, double altitude) {
        this(latitude, longitude, altitude, true);
    }

    private QGeoCoordinate(double lat, double lon, double alt, boolean is3d) {
        if (!isAvailable()) {
            throw new IllegalStateException(
                "本库未编译 QtPositioning 支持(缺少 qtpositioning 模块);先安装该模块再构建 libjqt");
        }
        latitude = lat; longitude = lon; altitude = alt; threeD = is3d;
        nativeHandle = nativeCreate(lat, lon, alt, is3d);
        final long h = nativeHandle;
        CLEANER.register(this, () -> nativeDispose(h));
    }

    private static native long nativeCreate(double lat, double lon, double alt, boolean threeD);
    private static native void nativeDispose(long handle);

    long nativeHandle() { return nativeHandle; }

    /** 是否有效（经纬度均已设置且在范围内）。 */
    public boolean isValid() { return nativeIsValid(nativeHandle); }
    private static native boolean nativeIsValid(long handle);

    /** 类型（{@link Type}）。 */
    public int type() { return nativeType(nativeHandle); }
    private static native int nativeType(long handle);

    /** 纬度（度;无效为 NaN）。 */
    public double latitude() { return latitude; }

    /** 经度（度;无效为 NaN）。 */
    public double longitude() { return longitude; }

    /** 海拔（米;二维坐标为 0）。 */
    public double altitude() { return altitude; }

    /** 设置纬度（度）。 */
    public void setLatitude(double degrees) {
        latitude = degrees;
        nativeSetLatitude(nativeHandle, degrees);
    }
    private static native void nativeSetLatitude(long handle, double degrees);

    /** 设置经度（度）。 */
    public void setLongitude(double degrees) {
        longitude = degrees;
        nativeSetLongitude(nativeHandle, degrees);
    }
    private static native void nativeSetLongitude(long handle, double degrees);

    /** 设置海拔（米;设置后坐标变为三维）。 */
    public void setAltitude(double meters) {
        altitude = meters; threeD = true;
        nativeSetAltitude(nativeHandle, meters);
    }
    private static native void nativeSetAltitude(long handle, double meters);

    /**
     * 到另一坐标的大圆距离（**米**）。
     * <p>按 Qt 语义:{@code this} 或 {@code other} **无效时返回 0**（不是 -1）。
     * <p>海拔**不参与**计算 —— 同一经纬度、不同海拔的两点距离仍为 0。
     */
    public double distanceTo(QGeoCoordinate other) {
        return other == null ? -1 : nativeDistanceTo(nativeHandle, other.nativeHandle);
    }
    private static native double nativeDistanceTo(long handle, long otherHandle);

    /**
     * 到另一坐标的方位角（**度**，正北为 0、顺时针）。
     * <p>按 Qt 语义:任一坐标无效时返回 0。海拔不参与计算。
     */
    public double azimuthTo(QGeoCoordinate other) {
        return other == null ? -1 : nativeAzimuthTo(nativeHandle, other.nativeHandle);
    }
    private static native double nativeAzimuthTo(long handle, long otherHandle);

    /**
     * 沿给定方位角与距离推算新坐标（与 {@link #distanceTo}/{@link #azimuthTo} 互逆）。
     * @param distance 米
     * @param azimuth  度（正北 0、顺时针）
     */
    public QGeoCoordinate atDistanceAndAzimuth(double distance, double azimuth) {
        return nativeAtDistanceAndAzimuth(nativeHandle, distance, azimuth);
    }
    private native QGeoCoordinate nativeAtDistanceAndAzimuth(long handle, double distance, double azimuth);

    /** 按指定格式输出（{@link Format}）。 */
    public String toString(int format) { return nativeToString(nativeHandle, format); }
    private static native String nativeToString(long handle, int format);

    /** Qt 默认格式:度分秒 + 半球（{@link Format#DegreesMinutesSecondsWithHemisphere}）。 */
    @Override public String toString() { return toString(Format.DegreesMinutesSecondsWithHemisphere); }
}
