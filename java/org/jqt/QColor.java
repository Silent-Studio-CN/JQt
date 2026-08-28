/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 颜色（QColor 轻量工具类；L1 补全 v0.7.1）。
 * <p>
 * 当前以静态工具形式提供 HSV 分量查询；完整值类随 L2 值类批次推进。
 */
public class QColor {

    private QColor() {
    }

    /** HSV 明度（value，0-255；QColor::value）。hex 为 #RRGGBB。 */
    public static int value(String hex) {
        return nativeValue(hex);
    }

    /** HSV 色相（hue，0-359；无效颜色返回 -1）。 */
    public static int hue(String hex) {
        return nativeHue(hex);
    }

    /** HSV 饱和度（saturation，0-255）。 */
    public static int saturation(String hex) {
        return nativeSaturation(hex);
    }

    private static native int nativeValue(String hex);
    private static native int nativeHue(String hex);
    private static native int nativeSaturation(String hex);
}
