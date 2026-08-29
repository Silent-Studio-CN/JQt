/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 四元数（Qt {@code QQuaternion}，纯 Java 实现）。
 * <p>完整覆盖 Qt 6 QQuaternion API：旋转表示（角度/轴、欧拉角、fromAxes）、
 * SLERP 插值、向量旋转 {@link #rotatedVector(QVector3D)} 等。
 */
public class QQuaternion {

    private final double scalar;   // w
    private final double x;
    private final double y;
    private final double z;

    public QQuaternion() { this(1, 0, 0, 0); }

    public QQuaternion(double scalar, double x, double y, double z) {
        this.scalar = scalar; this.x = x; this.y = y; this.z = z;
    }

    // ---- 构造工厂 ----
    /** 单位四元数。 */
    public static QQuaternion fromAxisAndAngle(QVector3D axis, double angle) {
        return fromAxisAndAngle(axis.x(), axis.y(), axis.z(), angle);
    }
    /** 绕轴旋转 angle 度。 */
    public static QQuaternion fromAxisAndAngle(double x, double y, double z, double angle) {
        double len = Math.sqrt(x * x + y * y + z * z);
        if (len == 0) return new QQuaternion();
        double rad = Math.toRadians(angle) / 2.0;
        double s = Math.sin(rad) / len;
        return new QQuaternion(Math.cos(rad), x * s, y * s, z * s);
    }

    /** 欧拉角（度，zxz 约定，Qt fromEulerAngles 语义）。 */
    public static QQuaternion fromEulerAngles(double pitch, double yaw, double roll) {
        return fromEulerAngles(new QVector3D(pitch, yaw, roll));
    }
    public static QQuaternion fromEulerAngles(QVector3D eulerAngles) {
        // 标准 ZYX 约定：R = Rz(roll) * Ry(yaw) * Rx(pitch)，与 toEulerAngles 对称
        double pitch = Math.toRadians(eulerAngles.x());
        double yaw = Math.toRadians(eulerAngles.y());
        double roll = Math.toRadians(eulerAngles.z());
        double cy = Math.cos(yaw / 2), sy = Math.sin(yaw / 2);
        double cp = Math.cos(pitch / 2), sp = Math.sin(pitch / 2);
        double cr = Math.cos(roll / 2), sr = Math.sin(roll / 2);
        return new QQuaternion(
            cr * cy * cp + sr * sy * sp,
            cr * cy * sp - sr * sy * cp,
            cr * sy * cp + sr * cy * sp,
            sr * cy * cp - cr * sy * sp);
    }

    /** 由三个正交轴构造（x/y/z 轴在旋转后的方向）。 */
    public static QQuaternion fromAxes(QVector3D xAxis, QVector3D yAxis, QVector3D zAxis) {
        // R 的列 = 旋转后各轴方向（xAxis/yAxis/zAxis）
        double[] col0 = {xAxis.x(), xAxis.y(), xAxis.z()};
        double[] col1 = {yAxis.x(), yAxis.y(), yAxis.z()};
        double[] col2 = {zAxis.x(), zAxis.y(), zAxis.z()};
        double trace = col0[0] + col1[1] + col2[2];
        double w, x, y, z;
        if (trace > 0) {
            double s = Math.sqrt(trace + 1.0) * 2;
            w = 0.25 * s;
            x = (col2[1] - col1[2]) / s;
            y = (col0[2] - col2[0]) / s;
            z = (col0[1] - col1[0]) / s;
        } else if (col0[0] > col1[1] && col0[0] > col2[2]) {
            double s = Math.sqrt(1.0 + col0[0] - col1[1] - col2[2]) * 2;
            w = (col2[1] - col1[2]) / s;
            x = 0.25 * s;
            y = (col0[1] + col1[0]) / s;
            z = (col0[2] + col2[0]) / s;
        } else if (col1[1] > col2[2]) {
            double s = Math.sqrt(1.0 + col1[1] - col0[0] - col2[2]) * 2;
            w = (col0[2] - col2[0]) / s;
            x = (col0[1] + col1[0]) / s;
            y = 0.25 * s;
            z = (col1[2] + col2[1]) / s;
        } else {
            double s = Math.sqrt(1.0 + col2[2] - col0[0] - col1[1]) * 2;
            w = (col1[0] - col0[1]) / s;
            x = (col0[2] + col2[0]) / s;
            y = (col1[2] + col2[1]) / s;
            z = 0.25 * s;
        }
        return new QQuaternion(w, x, y, z).normalized();
    }

    public static QQuaternion fromDirection(QVector3D direction, QVector3D up) {
        QVector3D zAxis = direction.normalized();
        QVector3D xAxis = up.crossProduct(zAxis).normalized();
        QVector3D yAxis = zAxis.crossProduct(xAxis);
        return fromAxes(xAxis, yAxis, zAxis);
    }

    public static QQuaternion fromRotationMatrix(QMatrix4x4 rotationMatrix) {
        double[][] m = rotationMatrix.data3x3();
        double trace = m[0][0] + m[1][1] + m[2][2];
        double w, x, y, z;
        if (trace > 0) {
            double s = Math.sqrt(trace + 1.0) * 2;
            w = 0.25 * s;
            x = (m[2][1] - m[1][2]) / s;
            y = (m[0][2] - m[2][0]) / s;
            z = (m[1][0] - m[0][1]) / s;
        } else if (m[0][0] > m[1][1] && m[0][0] > m[2][2]) {
            double s = Math.sqrt(1.0 + m[0][0] - m[1][1] - m[2][2]) * 2;
            w = (m[2][1] - m[1][2]) / s;
            x = 0.25 * s;
            y = (m[0][1] + m[1][0]) / s;
            z = (m[0][2] + m[2][0]) / s;
        } else if (m[1][1] > m[2][2]) {
            double s = Math.sqrt(1.0 + m[1][1] - m[0][0] - m[2][2]) * 2;
            w = (m[0][2] - m[2][0]) / s;
            x = (m[0][1] + m[1][0]) / s;
            y = 0.25 * s;
            z = (m[1][2] + m[2][1]) / s;
        } else {
            double s = Math.sqrt(1.0 + m[2][2] - m[0][0] - m[1][1]) * 2;
            w = (m[1][0] - m[0][1]) / s;
            x = (m[0][2] + m[2][0]) / s;
            y = (m[1][2] + m[2][1]) / s;
            z = 0.25 * s;
        }
        return new QQuaternion(w, x, y, z).normalized();
    }

    // ---- 分量 ----
    public double scalar() { return scalar; }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public QVector3D vector() { return new QVector3D(x, y, z); }

    public boolean isNull() { return scalar == 0 && x == 0 && y == 0 && z == 0; }
    public boolean isIdentity() { return scalar == 1 && x == 0 && y == 0 && z == 0; }

    // ---- 运算 ----
    public double length() { return Math.sqrt(scalar * scalar + x * x + y * y + z * z); }
    public double lengthSquared() { return scalar * scalar + x * x + y * y + z * z; }

    public QQuaternion normalized() {
        double len = length();
        if (len == 0) return new QQuaternion(1, 0, 0, 0);
        return new QQuaternion(scalar / len, x / len, y / len, z / len);
    }
    public void normalize() { /* 值类不可变：用 normalized() */ }

    /** 共轭（向量部分取反）。 */
    public QQuaternion conjugate() { return new QQuaternion(scalar, -x, -y, -z); }

    /** 逆（共轭除以模长平方）。 */
    public QQuaternion inverted() {
        double l2 = lengthSquared();
        if (l2 == 0) return new QQuaternion(1, 0, 0, 0);
        return new QQuaternion(scalar / l2, -x / l2, -y / l2, -z / l2);
    }

    /** 点积。 */
    public double dotProduct(QQuaternion q) {
        return scalar * q.scalar + x * q.x + y * q.y + z * q.z;
    }

    public QQuaternion plus(QQuaternion q) {
        return new QQuaternion(scalar + q.scalar, x + q.x, y + q.y, z + q.z);
    }
    public QQuaternion minus(QQuaternion q) {
        return new QQuaternion(scalar - q.scalar, x - q.x, y - q.y, z - q.z);
    }
    /** Hamilton 积（旋转合成：this 后 q）。 */
    public QQuaternion multiply(QQuaternion q) {
        return new QQuaternion(
            scalar * q.scalar - x * q.x - y * q.y - z * q.z,
            scalar * q.x + x * q.scalar + y * q.z - z * q.y,
            scalar * q.y - x * q.z + y * q.scalar + z * q.x,
            scalar * q.z + x * q.y - y * q.x + z * q.scalar);
    }
    public QQuaternion multiply(double factor) {
        return new QQuaternion(scalar * factor, x * factor, y * factor, z * factor);
    }
    public QQuaternion divide(double divisor) {
        return new QQuaternion(scalar / divisor, x / divisor, y / divisor, z / divisor);
    }
    public QQuaternion negate() { return new QQuaternion(-scalar, -x, -y, -z); }

    // ---- 旋转应用 ----
    /** 旋转向量（q * v * q⁻¹）。 */
    public QVector3D rotatedVector(QVector3D vector) {
        QQuaternion v = new QQuaternion(0, vector.x(), vector.y(), vector.z());
        QQuaternion result = multiply(v).multiply(conjugate());
        return new QVector3D(result.x(), result.y(), result.z());
    }

    /** SLERP 球面插值（t: 0=this, 1=other）。 */
    public QQuaternion slerp(QQuaternion other, double t) {
        double cosOmega = dotProduct(other);
        QQuaternion end = other;
        if (cosOmega < 0) { end = other.negate(); cosOmega = -cosOmega; }
        if (cosOmega > 0.9999) {
            return normalized().multiply(1 - t).plus(end.normalized().multiply(t)).normalized();
        }
        double omega = Math.acos(Math.min(1.0, cosOmega));
        double so = Math.sin(omega);
        double a = Math.sin((1 - t) * omega) / so;
        double b = Math.sin(t * omega) / so;
        return multiply(a).plus(end.multiply(b)).normalized();
    }

    /** 欧拉角（度：pitch/yaw/roll）。 */
    public QVector3D toEulerAngles() {
        double sp = 2 * (scalar * x + y * z);
        double cp = 1 - 2 * (x * x + y * y);
        double pitch = Math.toDegrees(Math.atan2(sp, cp));
        double sy = 2 * (scalar * y - z * x);
        double yaw = Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, sy))));
        double sr = 2 * (scalar * z + x * y);
        double cr = 1 - 2 * (y * y + z * z);
        double roll = Math.toDegrees(Math.atan2(sr, cr));
        return new QVector3D(pitch, yaw, roll);
    }

    /** 旋转轴（归一化）。 */
    public QVector3D axis() {
        QVector3D v = vector();
        double len = v.length();
        if (len == 0) return new QVector3D();
        return v.divide(len);
    }

    /** 旋转角（度，0-180）。 */
    public double angle() {
        double w = Math.max(-1, Math.min(1, scalar / length()));
        return Math.toDegrees(2 * Math.acos(w));
    }

    /** 转 4x4 旋转矩阵（列主序，左上 3x3 为旋转）。 */
    public QMatrix4x4 toRotationMatrix() {
        QMatrix4x4 mat = new QMatrix4x4();
        double s = 2 / lengthSquared();
        mat.m[0] = 1 - s * (y * y + z * z);
        mat.m[4] = s * (x * y - scalar * z);
        mat.m[8] = s * (x * z + scalar * y);
        mat.m[1] = s * (x * y + scalar * z);
        mat.m[5] = 1 - s * (x * x + z * z);
        mat.m[9] = s * (y * z - scalar * x);
        mat.m[2] = s * (x * z - scalar * y);
        mat.m[6] = s * (y * z + scalar * x);
        mat.m[10] = 1 - s * (x * x + y * y);
        // 第 4 行/列 = 齐次单位（m[15] 已为 1）
        return mat;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QQuaternion)) return false;
        QQuaternion q = (QQuaternion) o;
        return Double.compare(scalar, q.scalar) == 0 && Double.compare(x, q.x) == 0
            && Double.compare(y, q.y) == 0 && Double.compare(z, q.z) == 0;
    }

    @Override
    public int hashCode() {
        long a = Double.doubleToLongBits(scalar), b = Double.doubleToLongBits(x);
        long c = Double.doubleToLongBits(y), d = Double.doubleToLongBits(z);
        int h = (int) a; h = 31 * h + (int) b; h = 31 * h + (int) c; h = 31 * h + (int) d;
        return h;
    }

    @Override
    public String toString() {
        return "QQuaternion(" + scalar + ", " + x + ", " + y + ", " + z + ")";
    }
}
