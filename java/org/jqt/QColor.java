/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 颜色值类（Qt {@code QColor}，纯 Java 实现，完整覆盖 Qt 6 QColor API）。
 * <p>支持 RGB / HSV / HSL / CMYK 四色彩空间、命名色（148 CSS 色名）、#RGB/#RRGGBB/#AARRGGBB 解析，
 * 并提供 {@link #toAwt()} / {@link #fromAwt(java.awt.Color)} 与 Java AWT 生态互转。
 */
public class QColor {

    /** 色彩空间（Qt ColorSpec）。 */
    public enum Spec { Rgb(0), Hsv(1), Cmyk(2), Hsl(3), Invalid(4);
        public final int value;
        Spec(int v) { value = v; }
    }

    // ---- 内部存储：以 RGBA 为准，另存当前 spec ----
    private int r, g, b, a;          // 0-255
    private Spec spec = Spec.Rgb;

    // ---- 构造 ----
    public QColor() { setRgb(0, 0, 0, 255); }

    public QColor(int r, int g, int b) { setRgb(r, g, b, 255); }

    public QColor(int r, int g, int b, int a) { setRgb(r, g, b, a); }

    /** 0xAARRGGBB。 */
    public QColor(int rgba) { setRgba(rgba); }

    /** 命名色（#RGB/#RRGGBB/#AARRGGBB 或 CSS 色名）。 */
    public QColor(String name) { setNamedColor(name); }

    public QColor(QColor c) { this.r = c.r; this.g = c.g; this.b = c.b; this.a = c.a; this.spec = c.spec; }

    // ---- 静态工厂 ----
    public static QColor fromRgb(int r, int g, int b) { return new QColor(r, g, b, 255); }
    public static QColor fromRgb(int r, int g, int b, int a) { return new QColor(r, g, b, a); }
    public static QColor fromRgba(int rgba) { return new QColor(rgba); }
    public static QColor fromRgbF(double r, double g, double b, double a) {
        return new QColor((int) (r * 255 + 0.5), (int) (g * 255 + 0.5), (int) (b * 255 + 0.5), (int) (a * 255 + 0.5));
    }
    public static QColor fromHsv(int h, int s, int v) { return fromHsv(h, s, v, 255); }
    public static QColor fromHsv(int h, int s, int v, int a) {
        QColor c = new QColor(); c.setHsv(h, s, v, a); return c;
    }
    public static QColor fromHsvF(double h, double s, double v, double a) {
        return fromHsv((int) (h * 360), (int) (s * 255), (int) (v * 255), (int) (a * 255));
    }
    public static QColor fromHsl(int h, int s, int l) { return fromHsl(h, s, l, 255); }
    public static QColor fromHsl(int h, int s, int l, int a) {
        QColor c = new QColor(); c.setHsl(h, s, l, a); return c;
    }
    public static QColor fromHslF(double h, double s, double l, double a) {
        return fromHsl((int) (h * 360), (int) (s * 255), (int) (l * 255), (int) (a * 255));
    }
    public static QColor fromCmyk(int c, int m, int y, int k) { return fromCmyk(c, m, y, k, 255); }
    public static QColor fromCmyk(int c, int m, int y, int k, int a) {
        QColor col = new QColor(); col.setCmyk(c, m, y, k, a); return col;
    }
    public static QColor fromCmykF(double c, double m, double y, double k, double a) {
        return fromCmyk((int) (c * 255), (int) (m * 255), (int) (y * 255), (int) (k * 255), (int) (a * 255));
    }
    public static QColor fromString(String name) {
        QColor c = new QColor();
        return c.setNamedColor(name) ? c : new QColor();
    }

    /** 全部 CSS 色名（小写）。 */
    public static java.util.List<String> colorNames() {
        return java.util.ArrayList.class.cast(new java.util.ArrayList<>(NAMED.keySet()));
    }

    /** 是否为合法 CSS 色名。 */
    public static boolean isValidColorName(String name) {
        if (name == null) return false;
        return NAMED.containsKey(name.toLowerCase());
    }

    // ---- RGB getter ----
    public int red() { return r; }
    public int green() { return g; }
    public int blue() { return b; }
    public int alpha() { return a; }
    public double redF() { return r / 255.0; }
    public double greenF() { return g / 255.0; }
    public double blueF() { return b / 255.0; }
    public double alphaF() { return a / 255.0; }

    /** 0xFFRRGGBB（Qt rgb()）。 */
    public int rgb() { return (r << 16) | (g << 8) | b; }
    /** 0xAARRGGBB（Qt rgba()）。 */
    public int rgba() { return (a << 24) | (r << 16) | (g << 8) | b; }

    public String name() { return name(NameFormat.HexRgb); }
    /** 命名格式。 */
    public enum NameFormat { HexRgb(0), HexArgb(1);
        public final int value;
        NameFormat(int v) { value = v; }
    }
    /** #RRGGBB 或 #AARRGGBB。 */
    public String name(NameFormat format) {
        String rs = hex2(r), gs = hex2(g), bs = hex2(b);
        if (format == NameFormat.HexArgb) {
            return "#" + hex2(a) + rs + gs + bs;
        }
        return "#" + rs + gs + bs;
    }
    private static String hex2(int v) {
        String s = Integer.toHexString(v & 0xFF).toUpperCase();
        return s.length() < 2 ? "0" + s : s;
    }

    public Spec spec() { return spec; }
    public boolean isValid() { return spec != Spec.Invalid; }

    // ---- HSV（Qt hue/saturation/value 为 0-359/0-255/0-255） ----
    public int hue() { return hsvHue(); }
    public int saturation() { return hsvSaturation(); }
    public int value() { return hsvValue(); }
    public int hsvHue() {
        double[] hsv = toHsvArray();
        return (int) (hsv[0] * 359.999);
    }
    public int hsvSaturation() { return (int) (toHsvArray()[1] * 255); }
    public int hsvValue() { return (int) (toHsvArray()[2] * 255); }
    private double[] toHsvArray() {
        double rd = r / 255.0, gd = g / 255.0, bd = b / 255.0;
        double max = Math.max(rd, Math.max(gd, bd)), min = Math.min(rd, Math.min(gd, bd));
        double d = max - min, h = 0, s = 0, v = max;
        if (d != 0) {
            s = d / max;
            if (max == rd) h = ((gd - bd) / d) % 6;
            else if (max == gd) h = (bd - rd) / d + 2;
            else h = (rd - gd) / d + 4;
            if (h < 0) h += 6;
            h /= 6.0;
        }
        return new double[]{h, s, v};
    }

    // ---- HSL ----
    public int hslHue() {
        double[] hsl = toHslArray();
        return (int) (hsl[0] * 359.999);
    }
    public int hslSaturation() { return (int) (toHslArray()[1] * 255); }
    public int hslLightness() { return (int) (toHslArray()[2] * 255); }
    private double[] toHslArray() {
        double rd = r / 255.0, gd = g / 255.0, bd = b / 255.0;
        double max = Math.max(rd, Math.max(gd, bd)), min = Math.min(rd, Math.min(gd, bd));
        double l = (max + min) / 2, d = max - min, h = 0, s = 0;
        if (d != 0) {
            s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
            if (max == rd) h = ((gd - bd) / d) % 6;
            else if (max == gd) h = (bd - rd) / d + 2;
            else h = (rd - gd) / d + 4;
            if (h < 0) h += 6;
            h /= 6.0;
        }
        return new double[]{h, s, l};
    }

    // ---- CMYK ----
    public int cyan() { return (int) (toCmykArray()[0] * 255); }
    public int magenta() { return (int) (toCmykArray()[1] * 255); }
    public int yellow() { return (int) (toCmykArray()[2] * 255); }
    public int black() { return (int) (toCmykArray()[3] * 255); }
    private double[] toCmykArray() {
        double rd = r / 255.0, gd = g / 255.0, bd = b / 255.0;
        double k = 1 - Math.max(rd, Math.max(gd, bd));
        if (k >= 1) return new double[]{0, 0, 0, 1};
        double c = (1 - rd - k) / (1 - k);
        double m = (1 - gd - k) / (1 - k);
        double y = (1 - bd - k) / (1 - k);
        return new double[]{c, m, y, k};
    }

    // ---- setter ----
    public void setRed(int red) { r = clamp(red); }
    public void setGreen(int green) { g = clamp(green); }
    public void setBlue(int blue) { b = clamp(blue); }
    public void setAlpha(int alpha) { a = clamp(alpha); }

    public void setRgb(int r, int g, int b) { setRgb(r, g, b, 255); }
    public void setRgb(int r, int g, int b, int a) {
        this.r = clamp(r); this.g = clamp(g); this.b = clamp(b); this.a = clamp(a);
        this.spec = Spec.Rgb;
    }
    public void setRgba(int rgba) {
        setRgb((rgba >> 16) & 0xFF, (rgba >> 8) & 0xFF, rgba & 0xFF, (rgba >> 24) & 0xFF);
    }
    public void setRgbF(double r, double g, double b, double a) {
        setRgb((int) (r * 255 + 0.5), (int) (g * 255 + 0.5), (int) (b * 255 + 0.5), (int) (a * 255 + 0.5));
    }

    public void setHsv(int h, int s, int v) { setHsv(h, s, v, 255); }
    public void setHsv(int h, int s, int v, int a) {
        int[] rgb = hsvToRgb(normHue(h), clamp(s) / 255.0, clamp(v) / 255.0);
        this.r = rgb[0]; this.g = rgb[1]; this.b = rgb[2]; this.a = clamp(a);
        this.spec = Spec.Hsv;
    }
    public void setHsvF(double h, double s, double v, double a) {
        setHsv((int) (h * 360), (int) (s * 255), (int) (v * 255), (int) (a * 255));
    }

    public void setHsl(int h, int s, int l) { setHsl(h, s, l, 255); }
    public void setHsl(int h, int s, int l, int a) {
        int[] rgb = hslToRgb(normHue(h), clamp(s) / 255.0, clamp(l) / 255.0);
        this.r = rgb[0]; this.g = rgb[1]; this.b = rgb[2]; this.a = clamp(a);
        this.spec = Spec.Hsl;
    }
    public void setHslF(double h, double s, double l, double a) {
        setHsl((int) (h * 360), (int) (s * 255), (int) (l * 255), (int) (a * 255));
    }

    public void setCmyk(int c, int m, int y, int k) { setCmyk(c, m, y, k, 255); }
    public void setCmyk(int c, int m, int y, int k, int a) {
        double cc = clamp(c) / 255.0, mm = clamp(m) / 255.0, yy = clamp(y) / 255.0, kk = clamp(k) / 255.0;
        this.r = (int) (255 * (1 - cc) * (1 - kk) + 0.5);
        this.g = (int) (255 * (1 - mm) * (1 - kk) + 0.5);
        this.b = (int) (255 * (1 - yy) * (1 - kk) + 0.5);
        this.a = clamp(a);
        this.spec = Spec.Cmyk;
    }
    public void setCmykF(double c, double m, double y, double k, double a) {
        setCmyk((int) (c * 255), (int) (m * 255), (int) (y * 255), (int) (k * 255), (int) (a * 255));
    }

    /** 解析命名色；成功返回 true，失败保持无效。 */
    public boolean setNamedColor(String name) {
        if (name == null) { spec = Spec.Invalid; return false; }
        String s = name.trim();
        if (s.startsWith("#")) {
            try {
                if (s.length() == 4) {  // #RGB
                    setRgb(Integer.parseInt(s.substring(1, 2) + s.substring(1, 2), 16),
                           Integer.parseInt(s.substring(2, 3) + s.substring(2, 3), 16),
                           Integer.parseInt(s.substring(3, 4) + s.substring(3, 4), 16), 255);
                    return true;
                }
                if (s.length() == 7) {  // #RRGGBB
                    setRgb(Integer.parseInt(s.substring(1, 3), 16),
                           Integer.parseInt(s.substring(3, 5), 16),
                           Integer.parseInt(s.substring(5, 7), 16), 255);
                    return true;
                }
                if (s.length() == 9) {  // #AARRGGBB
                    setRgb(Integer.parseInt(s.substring(3, 5), 16),
                           Integer.parseInt(s.substring(5, 7), 16),
                           Integer.parseInt(s.substring(7, 9), 16),
                           Integer.parseInt(s.substring(1, 3), 16));
                    return true;
                }
            } catch (NumberFormatException e) { /* fallthrough */ }
            spec = Spec.Invalid;
            return false;
        }
        Integer v = NAMED.get(s.toLowerCase());
        if (v != null) { setRgba(v); return true; }
        spec = Spec.Invalid;
        return false;
    }

    // ---- 转换 ----
    public QColor toRgb() { QColor c = new QColor(this); c.spec = Spec.Rgb; return c; }
    public QColor toHsv() { QColor c = new QColor(); c.setHsv(hsvHue(), hsvSaturation(), hsvValue(), a); return c; }
    public QColor toHsl() { QColor c = new QColor(); c.setHsl(hslHue(), hslSaturation(), hslLightness(), a); return c; }
    public QColor toCmyk() { QColor c = new QColor(); c.setCmyk(cyan(), magenta(), yellow(), black(), a); return c; }

    public void getRgb(int[] rgba) {
        if (rgba != null && rgba.length >= 4) { rgba[0] = r; rgba[1] = g; rgba[2] = b; rgba[3] = a; }
    }
    public void getHsv(int[] hsva) {
        if (hsva != null && hsva.length >= 4) { hsva[0] = hsvHue(); hsva[1] = hsvSaturation(); hsva[2] = hsvValue(); hsva[3] = a; }
    }
    public void getHsl(int[] hsla) {
        if (hsla != null && hsla.length >= 4) { hsla[0] = hslHue(); hsla[1] = hslSaturation(); hsla[2] = hslLightness(); hsla[3] = a; }
    }
    public void getCmyk(int[] cmyk) {
        if (cmyk != null && cmyk.length >= 4) { cmyk[0] = cyan(); cmyk[1] = magenta(); cmyk[2] = yellow(); cmyk[3] = black(); }
    }

    // ---- 明暗 ----
    /** 变亮（Qt lighter：factor=150 即 *1.5，封顶 255；100=不变）。 */
    public QColor lighter(int factor) {
        if (factor == 100 || factor <= 0) return new QColor(this);
        return new QColor(Math.min(255, (int) (r * factor / 100.0 + 0.5)),
                          Math.min(255, (int) (g * factor / 100.0 + 0.5)),
                          Math.min(255, (int) (b * factor / 100.0 + 0.5)), a);
    }

    /** 变暗（Qt darker：factor=150 即 *2/3；100=不变）。 */
    public QColor darker(int factor) {
        if (factor == 100 || factor <= 0) return new QColor(this);
        return new QColor((int) (r * 100.0 / factor + 0.5),
                          (int) (g * 100.0 / factor + 0.5),
                          (int) (b * 100.0 / factor + 0.5), a);
    }

    // ---- Java 生态桥 ----
    /** 转 java.awt.Color。 */
    public java.awt.Color toAwt() { return new java.awt.Color(r, g, b, a); }
    /** 从 java.awt.Color 构造。 */
    public static QColor fromAwt(java.awt.Color c) {
        return new QColor(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }

    // ---- 兼容：v0.7.1 静态工具 ----
    public static int value(String hex) { return new QColor(hex).hsvValue(); }
    public static int hue(String hex) { return new QColor(hex).hsvHue(); }
    public static int saturation(String hex) { return new QColor(hex).hsvSaturation(); }

    // ---- 工具 ----
    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
    private static int normHue(int h) { int n = h % 360; return n < 0 ? n + 360 : n; }

    private static int[] hsvToRgb(int h, double s, double v) {
        double c = v * s, x = c * (1 - Math.abs((h / 60.0) % 2 - 1)), m = v - c;
        double rd = 0, gd = 0, bd = 0;
        if (h < 60) { rd = c; gd = x; }
        else if (h < 120) { rd = x; gd = c; }
        else if (h < 180) { gd = c; bd = x; }
        else if (h < 240) { gd = x; bd = c; }
        else if (h < 300) { rd = x; bd = c; }
        else { rd = c; bd = x; }
        return new int[]{ (int) ((rd + m) * 255 + 0.5), (int) ((gd + m) * 255 + 0.5), (int) ((bd + m) * 255 + 0.5) };
    }

    private static int[] hslToRgb(int h, double s, double l) {
        double c = (1 - Math.abs(2 * l - 1)) * s, x = c * (1 - Math.abs((h / 60.0) % 2 - 1)), m = l - c / 2;
        double rd = 0, gd = 0, bd = 0;
        if (h < 60) { rd = c; gd = x; }
        else if (h < 120) { rd = x; gd = c; }
        else if (h < 180) { gd = c; bd = x; }
        else if (h < 240) { gd = x; bd = c; }
        else if (h < 300) { rd = x; bd = c; }
        else { rd = c; bd = x; }
        return new int[]{ (int) ((rd + m) * 255 + 0.5), (int) ((gd + m) * 255 + 0.5), (int) ((bd + m) * 255 + 0.5) };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QColor)) return false;
        QColor c = (QColor) o;
        return r == c.r && g == c.g && b == c.b && a == c.a;
    }

    @Override
    public int hashCode() {
        int h = r; h = 31 * h + g; h = 31 * h + b; h = 31 * h + a;
        return h;
    }

    @Override
    public String toString() { return name(); }

    // ---- 148 CSS 命名色（0xAARRGGBB） ----
    private static final Map<String, Integer> NAMED;
    static {
        Map<String, Integer> m = new HashMap<>(160);
        m.put("aliceblue", 0xFFF0F8FF); m.put("antiquewhite", 0xFFFAEBD7); m.put("aqua", 0xFF00FFFF);
        m.put("aquamarine", 0xFF7FFFD4); m.put("azure", 0xFFF0FFFF); m.put("beige", 0xFFF5F5DC);
        m.put("bisque", 0xFFFFE4C4); m.put("black", 0xFF000000); m.put("blanchedalmond", 0xFFFFEBCD);
        m.put("blue", 0xFF0000FF); m.put("blueviolet", 0xFF8A2BE2); m.put("brown", 0xFFA52A2A);
        m.put("burlywood", 0xFFDEB887); m.put("cadetblue", 0xFF5F9EA0); m.put("chartreuse", 0xFF7FFF00);
        m.put("chocolate", 0xFFD2691E); m.put("coral", 0xFFFF7F50); m.put("cornflowerblue", 0xFF6495ED);
        m.put("cornsilk", 0xFFFFF8DC); m.put("crimson", 0xFFDC143C); m.put("cyan", 0xFF00FFFF);
        m.put("darkblue", 0xFF00008B); m.put("darkcyan", 0xFF008B8B); m.put("darkgoldenrod", 0xFFB8860B);
        m.put("darkgray", 0xFFA9A9A9); m.put("darkgreen", 0xFF006400); m.put("darkgrey", 0xFFA9A9A9);
        m.put("darkkhaki", 0xFFBDB76B); m.put("darkmagenta", 0xFF8B008B); m.put("darkolivegreen", 0xFF556B2F);
        m.put("darkorange", 0xFFFF8C00); m.put("darkorchid", 0xFF9932CC); m.put("darkred", 0xFF8B0000);
        m.put("darksalmon", 0xFFE9967A); m.put("darkseagreen", 0xFF8FBC8F); m.put("darkslateblue", 0xFF483D8B);
        m.put("darkslategray", 0xFF2F4F4F); m.put("darkslategrey", 0xFF2F4F4F); m.put("darkturquoise", 0xFF00CED1);
        m.put("darkviolet", 0xFF9400D3); m.put("deeppink", 0xFFFF1493); m.put("deepskyblue", 0xFF00BFFF);
        m.put("dimgray", 0xFF696969); m.put("dimgrey", 0xFF696969); m.put("dodgerblue", 0xFF1E90FF);
        m.put("firebrick", 0xFFB22222); m.put("floralwhite", 0xFFFFFAF0); m.put("forestgreen", 0xFF228B22);
        m.put("fuchsia", 0xFFFF00FF); m.put("gainsboro", 0xFFDCDCDC); m.put("ghostwhite", 0xFFF8F8FF);
        m.put("gold", 0xFFFFD700); m.put("goldenrod", 0xFFDAA520); m.put("gray", 0xFF808080);
        m.put("green", 0xFF008000); m.put("greenyellow", 0xFFADFF2F); m.put("grey", 0xFF808080);
        m.put("honeydew", 0xFFF0FFF0); m.put("hotpink", 0xFFFF69B4); m.put("indianred", 0xFFCD5C5C);
        m.put("indigo", 0xFF4B0082); m.put("ivory", 0xFFFFFFF0); m.put("khaki", 0xFFF0E68C);
        m.put("lavender", 0xFFE6E6FA); m.put("lavenderblush", 0xFFFFF0F5); m.put("lawngreen", 0xFF7CFC00);
        m.put("lemonchiffon", 0xFFFFFACD); m.put("lightblue", 0xFFADD8E6); m.put("lightcoral", 0xFFF08080);
        m.put("lightcyan", 0xFFE0FFFF); m.put("lightgoldenrodyellow", 0xFFFAFAD2); m.put("lightgray", 0xFFD3D3D3);
        m.put("lightgreen", 0xFF90EE90); m.put("lightgrey", 0xFFD3D3D3); m.put("lightpink", 0xFFFFB6C1);
        m.put("lightsalmon", 0xFFFFA07A); m.put("lightseagreen", 0xFF20B2AA); m.put("lightskyblue", 0xFF87CEFA);
        m.put("lightslategray", 0xFF778899); m.put("lightslategrey", 0xFF778899); m.put("lightsteelblue", 0xFFB0C4DE);
        m.put("lightyellow", 0xFFFFFFE0); m.put("lime", 0xFF00FF00); m.put("limegreen", 0xFF32CD32);
        m.put("linen", 0xFFFAF0E6); m.put("magenta", 0xFFFF00FF); m.put("maroon", 0xFF800000);
        m.put("mediumaquamarine", 0xFF66CDAA); m.put("mediumblue", 0xFF0000CD); m.put("mediumorchid", 0xFFBA55D3);
        m.put("mediumpurple", 0xFF9370DB); m.put("mediumseagreen", 0xFF3CB371); m.put("mediumslateblue", 0xFF7B68EE);
        m.put("mediumspringgreen", 0xFF00FA9A); m.put("mediumturquoise", 0xFF48D1CC); m.put("mediumvioletred", 0xFFC71585);
        m.put("midnightblue", 0xFF191970); m.put("mintcream", 0xFFF5FFFA); m.put("mistyrose", 0xFFFFE4E1);
        m.put("moccasin", 0xFFFFE4B5); m.put("navajowhite", 0xFFFFDEAD); m.put("navy", 0xFF000080);
        m.put("oldlace", 0xFFFDF5E6); m.put("olive", 0xFF808000); m.put("olivedrab", 0xFF6B8E23);
        m.put("orange", 0xFFFFA500); m.put("orangered", 0xFFFF4500); m.put("orchid", 0xFFDA70D6);
        m.put("palegoldenrod", 0xFFEEE8AA); m.put("palegreen", 0xFF98FB98); m.put("paleturquoise", 0xFFAFEEEE);
        m.put("palevioletred", 0xFFDB7093); m.put("papayawhip", 0xFFFFEFD5); m.put("peachpuff", 0xFFFFDAB9);
        m.put("peru", 0xFFCD853F); m.put("pink", 0xFFFFC0CB); m.put("plum", 0xFFDDA0DD);
        m.put("powderblue", 0xFFB0E0E6); m.put("purple", 0xFF800080); m.put("rebeccapurple", 0xFF663399);
        m.put("red", 0xFFFF0000); m.put("rosybrown", 0xFFBC8F8F); m.put("royalblue", 0xFF4169E1);
        m.put("saddlebrown", 0xFF8B4513); m.put("salmon", 0xFFFA8072); m.put("sandybrown", 0xFFF4A460);
        m.put("seagreen", 0xFF2E8B57); m.put("seashell", 0xFFFFF5EE); m.put("sienna", 0xFFA0522D);
        m.put("silver", 0xFFC0C0C0); m.put("skyblue", 0xFF87CEEB); m.put("slateblue", 0xFF6A5ACD);
        m.put("slategray", 0xFF708090); m.put("slategrey", 0xFF708090); m.put("snow", 0xFFFFFAFA);
        m.put("springgreen", 0xFF00FF7F); m.put("steelblue", 0xFF4682B4); m.put("tan", 0xFFD2B48C);
        m.put("teal", 0xFF008080); m.put("thistle", 0xFFD8BFD8); m.put("tomato", 0xFFFF6347);
        m.put("turquoise", 0xFF40E0D0); m.put("violet", 0xFFEE82EE); m.put("wheat", 0xFFF5DEB3);
        m.put("white", 0xFFFFFFFF); m.put("whitesmoke", 0xFFF5F5F5); m.put("yellow", 0xFFFFFF00);
        m.put("yellowgreen", 0xFF9ACD32);
        NAMED = Collections.unmodifiableMap(m);
    }

    /** Qt::GlobalColor 常用常量。 */
    public static final QColor White = new QColor(255, 255, 255);
    public static final QColor Black = new QColor(0, 0, 0);
    public static final QColor Red = new QColor(255, 0, 0);
    public static final QColor Green = new QColor(0, 128, 0);
    public static final QColor Blue = new QColor(0, 0, 255);
    public static final QColor Cyan = new QColor(0, 255, 255);
    public static final QColor Magenta = new QColor(255, 0, 255);
    public static final QColor Yellow = new QColor(255, 255, 0);
    public static final QColor Gray = new QColor(128, 128, 128);
    public static final QColor DarkRed = new QColor(128, 0, 0);
    public static final QColor DarkGreen = new QColor(0, 128, 0);
    public static final QColor DarkBlue = new QColor(0, 0, 128);
    public static final QColor DarkCyan = new QColor(0, 128, 128);
    public static final QColor DarkMagenta = new QColor(128, 0, 128);
    public static final QColor DarkYellow = new QColor(128, 128, 0);
    public static final QColor Transparent = new QColor(0, 0, 0, 0);
}
