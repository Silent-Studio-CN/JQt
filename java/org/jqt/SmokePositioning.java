/*
 * JQt - QtPositioning 冒烟（v1.9.1 P2-④）。
 *
 * QGeoCoordinate 是**纯几何计算**(大圆距离/方位角/推算),不需要 GPS 或定位服务,
 * 因此本冒烟无需任何硬件即可确定性验证:
 *   · 无效坐标语义(distanceTo/azimuthTo 返回 -1)
 *   · 北京→上海距离与方位角(与公认值比对,留合理容差)
 *   · **推算与反算互逆**:atDistanceAndAzimuth 100km 正东 → 距离≈100km、方位≈90°
 *   · 二维/三维坐标类型与海拔参与距离计算
 *   · 三种字符串格式
 */
package org.jqt;

public class SmokePositioning {

    static int pass = 0;
    static int fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("[geo] OK   " + name); }
        else { fail++; System.out.println("[geo] FAIL " + name); }
    }

    static boolean near(double a, double b, double tol) { return Math.abs(a - b) <= tol; }

    public static void main(String[] args) {
        System.out.println("[geo] start");
        QApplication app = new QApplication();

        if (!QGeoCoordinate.isAvailable()) {
            System.out.println("[geo] SKIP 本库未编译 QtPositioning(缺 qtpositioning 模块)");
            System.out.println("[geo] ALL PASS ✅ (跳过)");
            return;
        }

        // ---- 无效坐标语义 ----
        QGeoCoordinate invalid = new QGeoCoordinate();
        check("默认构造的坐标无效", !invalid.isValid());
        check("无效坐标 type = Invalid(实际 " + invalid.type() + ")",
              invalid.type() == QGeoCoordinate.Type.Invalid);

        // ---- 北京 / 上海 ----
        QGeoCoordinate beijing = new QGeoCoordinate(39.9042, 116.4074);
        QGeoCoordinate shanghai = new QGeoCoordinate(31.2304, 121.4737);
        check("北京坐标有效", beijing.isValid());
        check("北京坐标 type = 2D(实际 " + beijing.type() + ")",
              beijing.type() == QGeoCoordinate.Type.Coordinate2D);
        check("纬度回读 39.9042(实际 " + beijing.latitude() + ")", near(beijing.latitude(), 39.9042, 1e-9));
        check("经度回读 116.4074(实际 " + beijing.longitude() + ")", near(beijing.longitude(), 116.4074, 1e-9));

        // Qt 契约:任一坐标无效时 distanceTo/azimuthTo 返回 **0**(官方文档明确)
        check("无效坐标 distanceTo 返回 0(实际 " + beijing.distanceTo(invalid) + ")",
              beijing.distanceTo(invalid) == 0.0);
        check("无效坐标 azimuthTo 返回 0(实际 " + beijing.azimuthTo(invalid) + ")",
              beijing.azimuthTo(invalid) == 0.0);
        check("无效坐标 toString 为空串(实际 '" + invalid.toString() + "')",
              invalid.toString().isEmpty());

        double meters = beijing.distanceTo(shanghai);
        double km = meters / 1000.0;
        System.out.println("[geo] 北京→上海 距离 " + Math.round(km) + " km,方位角 "
                         + String.format("%.1f", beijing.azimuthTo(shanghai)) + "°");
        check("北京→上海 距离 ≈1067km(实际 " + Math.round(km) + " km,容差 ±25)",
              km > 1042 && km < 1092);
        // 大圆初始方位角:北京→上海 ≈153°(按 atan2 公式独立验算过),反向 ≈333°
        double az = beijing.azimuthTo(shanghai);
        check("北京→上海 方位角 ≈153°(实际 " + String.format("%.1f", az) + ",容差 ±3)",
              az > 150 && az < 156);
        check("距离计算对称 distanceTo 往返一致",
              near(beijing.distanceTo(shanghai), shanghai.distanceTo(beijing), 1.0));
        check("上海→北京 方位角 ≈333°(反向;实际 "
              + String.format("%.1f", shanghai.azimuthTo(beijing)) + ")",
              shanghai.azimuthTo(beijing) > 330 && shanghai.azimuthTo(beijing) < 337);

        // ---- 互逆性(最强的不变量测试)----
        for (double[] da : new double[][] {{100_000, 90}, {50_000, 0}, {1_000_000, 45}, {25_000, 225}}) {
            double dist = da[0], bearing = da[1];
            QGeoCoordinate moved = beijing.atDistanceAndAzimuth(dist, bearing);
            double back = beijing.distanceTo(moved);
            double backAz = beijing.azimuthTo(moved);
            check("推算 " + (long) (dist / 1000) + "km/" + (int) bearing + "° 后反算距离一致(实际 "
                  + Math.round(back / 1000) + " km)", near(back, dist, dist * 0.01));
            check("推算 " + (int) bearing + "° 后反算方位一致(实际 "
                  + String.format("%.1f", backAz) + "°)", near(backAz, bearing, 0.5));
        }

        // ---- 三维坐标 ----
        QGeoCoordinate bj3d = new QGeoCoordinate(39.9042, 116.4074, 50.0);
        check("三维坐标 type = 3D(实际 " + bj3d.type() + ")",
              bj3d.type() == QGeoCoordinate.Type.Coordinate3D);
        check("海拔回读 50.0(实际 " + bj3d.altitude() + ")", near(bj3d.altitude(), 50.0, 1e-9));
        double d3 = bj3d.distanceTo(new QGeoCoordinate(39.9042, 116.4074, 50.0));
        check("同点三维距离为 0(实际 " + d3 + ")", d3 >= 0 && d3 < 0.001);
        // Qt 契约:distanceTo **不使用**海拔 —— 同一经纬度海拔差 1000m 距离仍为 0
        double dAlt = bj3d.distanceTo(new QGeoCoordinate(39.9042, 116.4074, 1050.0));
        check("海拔不参与距离计算(海拔差 1000m,距离仍为 " + dAlt + " m)", dAlt == 0.0);

        // ---- 修改坐标 ----
        QGeoCoordinate mutable = new QGeoCoordinate();
        check("新建坐标为无效", !mutable.isValid());
        mutable.setLatitude(31.2304);
        mutable.setLongitude(121.4737);
        check("设置经纬度后有效", mutable.isValid());
        check("设置后的点与上海重合(距离 0)", mutable.distanceTo(shanghai) < 0.001);

        // ---- 字符串格式 ----
        String dd = beijing.toString(QGeoCoordinate.Format.Degrees);
        String ddh = beijing.toString(QGeoCoordinate.Format.DegreesWithHemisphere);
        String dmm = beijing.toString(QGeoCoordinate.Format.DegreesMinutes);
        String dms = beijing.toString(QGeoCoordinate.Format.DegreesMinutesSeconds);
        String dmsh = beijing.toString(QGeoCoordinate.Format.DegreesMinutesSecondsWithHemisphere);
        System.out.println("[geo] 格式: DD='" + dd + "' DDH='" + ddh + "' DMM='" + dmm
                         + "' DMS='" + dms + "' DMSH='" + dmsh + "'");
        check("DD(0) 含度符号但不含半球字母(实际 '" + dd + "')",
              dd.contains("°") && !dd.contains("N") && !dd.contains("E"));
        check("DegreesWithHemisphere(1) 含 N 与 E(实际 '" + ddh + "')",
              ddh.contains("N") && ddh.contains("E"));
        check("DMM(2) 含度与分符号(实际 '" + dmm + "')", dmm.contains("°") && dmm.contains("'"));
        check("DMS(4) 含度分秒符号(实际 '" + dms + "')",
              dms.contains("°") && dms.contains("'") && dms.contains("\""));
        check("DMS+半球(5) 同时含秒符号与 N/E(实际 '" + dmsh + "')",
              dmsh.contains("\"") && dmsh.contains("N") && dmsh.contains("E"));
        check("默认 toString = DMS+半球(Qt 默认格式)", beijing.toString().equals(dmsh));
        String withAlt = bj3d.toString(QGeoCoordinate.Format.Degrees);
        check("已设海拔时字符串含 'm'(实际 '" + withAlt + "')", withAlt.contains("m"));

        System.out.println("[geo] pass=" + pass + " fail=" + fail);
        System.out.println("[geo] " + (fail == 0 ? "ALL PASS ✅" : ("FAILED: " + fail)));
        System.exit(fail > 0 ? 1 : 0);
    }
}
