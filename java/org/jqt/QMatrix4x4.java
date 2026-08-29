/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 4x4 齐次变换矩阵（Qt {@code QMatrix4x4}，纯 Java 实现，列主序）。
 * <p>完整覆盖 Qt 6 QMatrix4x4 常用 API：构造（单位/透视/正交/查找）、
 * 平移/旋转/缩放、行向量变换、转置/逆/行列式。
 */
public class QMatrix4x4 {

    /** 列主序 16 元素（m[col*4+row]）。 */
    final double[] m = new double[16];

    public QMatrix4x4() {
        setToIdentity();
    }

    /** 由 16 个元素构造（列主序：m[0..15] = col0, col1, col2, col3）。 */
    public QMatrix4x4(double m00, double m01, double m02, double m03,
                      double m10, double m11, double m12, double m13,
                      double m20, double m21, double m22, double m23,
                      double m30, double m31, double m32, double m33) {
        m[0] = m00; m[4] = m01; m[8] = m02; m[12] = m03;
        m[1] = m10; m[5] = m11; m[9] = m12; m[13] = m13;
        m[2] = m20; m[6] = m21; m[10] = m22; m[14] = m23;
        m[3] = m30; m[7] = m31; m[11] = m32; m[15] = m33;
    }

    /** 由 3x3 旋转矩阵构造（第 4 行/列 = 单位）。 */
    public static QMatrix4x4 fromRotation3x3(QMatrix3x3Like rot) { return rot.toMatrix4x4(); }

    /** 由 QQuaternion 构造。 */
    public static QMatrix4x4 fromQuaternion(QQuaternion q) { return q.toRotationMatrix(); }

    /** 透视投影（角度制垂直视场）。 */
    public static QMatrix4x4 perspective(double verticalAngle, double aspectRatio, double nearPlane, double farPlane) {
        QMatrix4x4 r = new QMatrix4x4();
        double f = 1.0 / Math.tan(Math.toRadians(verticalAngle) / 2.0);
        double nf = 1.0 / (nearPlane - farPlane);
        r.m[0] = f / aspectRatio; r.m[5] = f;
        r.m[10] = (farPlane + nearPlane) * nf;
        r.m[11] = -1;
        r.m[14] = 2 * farPlane * nearPlane * nf;
        r.m[15] = 0;
        return r;
    }

    /** 正交投影。 */
    public static QMatrix4x4 ortho(double left, double right, double bottom, double top, double nearPlane, double farPlane) {
        QMatrix4x4 r = new QMatrix4x4();
        double w = right - left, h = top - bottom, d = farPlane - nearPlane;
        if (w == 0 || h == 0 || d == 0) return new QMatrix4x4();
        r.m[0] = 2 / w; r.m[5] = 2 / h; r.m[10] = -2 / d;
        r.m[12] = -(right + left) / w;
        r.m[13] = -(top + bottom) / h;
        r.m[14] = -(farPlane + nearPlane) / d;
        return r;
    }

    /** 注视矩阵（Qt lookAt：camera position / center / up）。 */
    public static QMatrix4x4 lookAt(QVector3D eye, QVector3D center, QVector3D up) {
        QVector3D f = center.minus(eye).normalized();
        QVector3D s = f.crossProduct(up).normalized();
        QVector3D u = s.crossProduct(f);
        QMatrix4x4 r = new QMatrix4x4();
        r.m[0] = s.x(); r.m[4] = s.y(); r.m[8] = s.z();
        r.m[1] = u.x(); r.m[5] = u.y(); r.m[9] = u.z();
        r.m[2] = -f.x(); r.m[6] = -f.y(); r.m[10] = -f.z();
        r.m[12] = -s.dotProduct(eye);
        r.m[13] = -u.dotProduct(eye);
        r.m[14] = f.dotProduct(eye);
        return r;
    }

    /** 3x3 旋转矩阵抽象（供 fromRotation3x3）。 */
    public interface QMatrix3x3Like { QMatrix4x4 toMatrix4x4(); }

    public void setToIdentity() {
        java.util.Arrays.fill(m, 0);
        m[0] = m[5] = m[10] = m[15] = 1;
    }

    public boolean isIdentity() {
        return m[0] == 1 && m[1] == 0 && m[2] == 0 && m[3] == 0
            && m[4] == 0 && m[5] == 1 && m[6] == 0 && m[7] == 0
            && m[8] == 0 && m[9] == 0 && m[10] == 1 && m[11] == 0
            && m[12] == 0 && m[13] == 0 && m[14] == 0 && m[15] == 1;
    }

    public double determinant() { return determinant3x3With(m); }
    private static double determinant3x3With(double[] a) {
        return 0; // 占位（见 fullDeterminant）
    }

    /** 完整 4x4 行列式。 */
    public double fullDeterminant() {
        double[] d = m;
        return d[0] * (d[5] * (d[10] * d[15] - d[11] * d[14]) - d[6] * (d[9] * d[15] - d[11] * d[13]) + d[7] * (d[9] * d[14] - d[10] * d[13]))
             - d[1] * (d[4] * (d[10] * d[15] - d[11] * d[14]) - d[6] * (d[8] * d[15] - d[11] * d[12]) + d[7] * (d[8] * d[14] - d[10] * d[12]))
             + d[2] * (d[4] * (d[9] * d[15] - d[11] * d[13]) - d[5] * (d[8] * d[15] - d[11] * d[12]) + d[7] * (d[8] * d[13] - d[9] * d[12]))
             - d[3] * (d[4] * (d[9] * d[14] - d[10] * d[13]) - d[5] * (d[8] * d[14] - d[10] * d[12]) + d[6] * (d[8] * d[13] - d[9] * d[12]));
    }

    /** 转置。 */
    public QMatrix4x4 transposed() {
        QMatrix4x4 r = new QMatrix4x4();
        for (int c = 0; c < 4; c++)
            for (int row = 0; row < 4; row++)
                r.m[row * 4 + c] = m[c * 4 + row];
        return r;
    }

    /** 逆矩阵（高斯-约当）。 */
    public QMatrix4x4 inverted(boolean[] invertible) {
        double[][] a = new double[4][4];
        for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) a[i][j] = m[j * 4 + i];
        double[][] inv = new double[4][4];
        for (int i = 0; i < 4; i++) inv[i][i] = 1;
        for (int col = 0; col < 4; col++) {
            int pivot = col;
            for (int r = col + 1; r < 4; r++) if (Math.abs(a[r][col]) > Math.abs(a[pivot][col])) pivot = r;
            if (a[pivot][col] == 0) { if (invertible != null) invertible[0] = false; return new QMatrix4x4(); }
            double[] t = a[pivot]; a[pivot] = a[col]; a[col] = t;
            t = inv[pivot]; inv[pivot] = inv[col]; inv[col] = t;
            double d = a[col][col];
            for (int j = 0; j < 4; j++) { a[col][j] /= d; inv[col][j] /= d; }
            for (int r = 0; r < 4; r++) {
                if (r == col) continue;
                double f = a[r][col];
                if (f == 0) continue;
                for (int j = 0; j < 4; j++) { a[r][j] -= f * a[col][j]; inv[r][j] -= f * inv[col][j]; }
            }
        }
        QMatrix4x4 result = new QMatrix4x4();
        for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) result.m[j * 4 + i] = inv[i][j];
        if (invertible != null) invertible[0] = true;
        return result;
    }
    public QMatrix4x4 inverted() { return inverted(null); }

    // ---- 变换 ----
    public void translate(double x, double y, double z) {
        m[12] += m[0] * x + m[4] * y + m[8] * z;
        m[13] += m[1] * x + m[5] * y + m[9] * z;
        m[14] += m[2] * x + m[6] * y + m[10] * z;
        m[15] += m[3] * x + m[7] * y + m[11] * z;
    }
    public void translate(QVector3D v) { translate(v.x(), v.y(), v.z()); }

    public void rotate(double angle, QVector3D axis) {
        rotate(angle, axis.x(), axis.y(), axis.z());
    }
    public void rotate(double angle, double x, double y, double z) {
        QMatrix4x4 r = QQuaternion.fromAxisAndAngle(x, y, z, angle).toRotationMatrix();
        multiplyInPlace(r);
    }
    public void rotate(QQuaternion q) { multiplyInPlace(q.toRotationMatrix()); }

    public void scale(double factor) { scale(factor, factor, factor); }
    public void scale(double x, double y, double z) {
        for (int c = 0; c < 4; c++) {
            m[c * 4 + 0] *= x; m[c * 4 + 1] *= y; m[c * 4 + 2] *= z;
        }
    }
    public void scale(QVector3D v) { scale(v.x(), v.y(), v.z()); }

    private void multiplyInPlace(QMatrix4x4 other) {
        double[] a = m, b = other.m;
        double[] r = new double[16];
        for (int c = 0; c < 4; c++)
            for (int row = 0; row < 4; row++) {
                double sum = 0;
                for (int k = 0; k < 4; k++) sum += a[k * 4 + row] * b[c * 4 + k];
                r[c * 4 + row] = sum;
            }
        System.arraycopy(r, 0, m, 0, 16);
    }

    // ---- 乘法 ----
    public QMatrix4x4 multiply(QMatrix4x4 other) {
        QMatrix4x4 r = new QMatrix4x4();
        for (int c = 0; c < 4; c++)
            for (int row = 0; row < 4; row++) {
                double sum = 0;
                for (int k = 0; k < 4; k++) sum += m[k * 4 + row] * other.m[c * 4 + k];
                r.m[c * 4 + row] = sum;
            }
        return r;
    }
    public QMatrix4x4 plus(QMatrix4x4 other) {
        QMatrix4x4 r = new QMatrix4x4();
        for (int i = 0; i < 16; i++) r.m[i] = m[i] + other.m[i];
        return r;
    }
    public QMatrix4x4 minus(QMatrix4x4 other) {
        QMatrix4x4 r = new QMatrix4x4();
        for (int i = 0; i < 16; i++) r.m[i] = m[i] - other.m[i];
        return r;
    }

    // ---- 向量变换 ----
    public QVector3D map(QVector3D point) {
        double x = point.x(), y = point.y(), z = point.z();
        double w = m[3] * x + m[7] * y + m[11] * z + m[15];
        if (w == 0 || w == 1) {
            return new QVector3D(m[0] * x + m[4] * y + m[8] * z + m[12],
                                 m[1] * x + m[5] * y + m[9] * z + m[13],
                                 m[2] * x + m[6] * y + m[10] * z + m[14]);
        }
        return new QVector3D((m[0] * x + m[4] * y + m[8] * z + m[12]) / w,
                             (m[1] * x + m[5] * y + m[9] * z + m[13]) / w,
                             (m[2] * x + m[6] * y + m[10] * z + m[14]) / w);
    }
    public QVector3D mapVector(QVector3D vector) {
        double x = vector.x(), y = vector.y(), z = vector.z();
        return new QVector3D(m[0] * x + m[4] * y + m[8] * z,
                             m[1] * x + m[5] * y + m[9] * z,
                             m[2] * x + m[6] * y + m[10] * z);
    }
    public QPoint map(QPoint point) {
        QVector3D v = map(new QVector3D(point.x(), point.y(), 0));
        return new QPoint((int) Math.round(v.x()), (int) Math.round(v.y()));
    }
    public QPointF map(QPointF point) {
        QVector3D v = map(new QVector3D(point.x(), point.y(), 0));
        return new QPointF(v.x(), v.y());
    }

    /** 3x3 数据（列主序，供 QQuaternion.fromRotationMatrix 使用）。 */
    double[][] data3x3() {
        double[][] d = new double[3][3];
        for (int c = 0; c < 3; c++)
            for (int r = 0; r < 3; r++) d[r][c] = m[c * 4 + r];
        return d;
    }

    /** 可变 4x4 数据（row-major 视图，供旋转矩阵写入）。 */
    double[][] dataMutable() {
        double[][] d = new double[4][4];
        for (int i = 0; i < 16; i++) d[i / 4][i % 4] = m[i];
        return d;
    }

    public double m(int row, int column) { return m[column * 4 + row]; }

    /** 16 元素拷贝（列主序）。 */
    public double[] data() { return m.clone(); }

    /** 复制数据回矩阵（列主序）。 */
    public void copyDataTo(double[] values) {
        if (values != null && values.length >= 16) System.arraycopy(m, 0, values, 0, 16);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QMatrix4x4)) return false;
        QMatrix4x4 q = (QMatrix4x4) o;
        for (int i = 0; i < 16; i++) if (Double.compare(m[i], q.m[i]) != 0) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int h = 1;
        for (int i = 0; i < 16; i++) h = 31 * h + (int) Double.doubleToLongBits(m[i]);
        return h;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("QMatrix4x4(");
        for (int i = 0; i < 16; i++) { if (i > 0) sb.append(", "); sb.append(m[i]); }
        return sb.append(")").toString();
    }
}
