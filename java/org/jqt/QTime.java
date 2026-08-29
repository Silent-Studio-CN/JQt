/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 时间值类（Qt {@code QTime}，纯 Java 实现，基于 {@link java.time.LocalTime}）。
 */
public class QTime {

    private LocalTime time;   // null = 无效

    public QTime() { this.time = null; }
    public QTime(int h, int m) { this(h, m, 0, 0); }
    public QTime(int h, int m, int s) { this(h, m, s, 0); }
    public QTime(int h, int m, int s, int ms) {
        try { this.time = LocalTime.of(h, m, s, ms * 1_000_000); }
        catch (Exception e) { this.time = null; }
    }
    private QTime(LocalTime t) { this.time = t; }

    /** 当前时间。 */
    public static QTime currentTime() { return new QTime(LocalTime.now()); }

    public boolean isValid() { return time != null; }
    public int hour() { return time != null ? time.getHour() : 0; }
    public int minute() { return time != null ? time.getMinute() : 0; }
    public int second() { return time != null ? time.getSecond() : 0; }
    public int msec() { return time != null ? time.getNano() / 1_000_000 : 0; }

    public QTime addSecs(int secs) { return time != null ? new QTime(time.plusSeconds(secs)) : new QTime(); }
    public QTime addMSecs(int ms) { return time != null ? new QTime(time.plusNanos(ms * 1_000_000L)) : new QTime(); }

    /** 到另一时间秒差（Qt 语义：忽略毫秒，按 h:m:s 整数差）。 */
    public int secsTo(QTime other) {
        return time != null && other.time != null
                ? (other.time.toSecondOfDay() - time.toSecondOfDay()) : 0;
    }
    public int msecsTo(QTime other) {
        return time != null && other.time != null ? (int) java.time.temporal.ChronoUnit.MILLIS.between(time, other.time) : 0;
    }

    /** 当天秒数。 */
    public int msecsSinceStartOfDay() {
        return time != null ? time.toSecondOfDay() * 1000 + time.getNano() / 1_000_000 : 0;
    }

    /** HH:mm:ss 或 HH:mm:ss.zzz。 */
    public String toString(String format) {
        if (time == null) return "";
        try { return time.format(DateTimeFormatter.ofPattern(format, Locale.ROOT)); }
        catch (Exception e) { return ""; }
    }

    public static QTime fromString(String s, String format) {
        try { return new QTime(LocalTime.parse(s, DateTimeFormatter.ofPattern(format, Locale.ROOT))); }
        catch (Exception e) { return new QTime(); }
    }

    public LocalTime toLocalTime() { return time; }
    public static QTime fromLocalTime(LocalTime t) { return new QTime(t); }

    @Override
    public String toString() {
        return time != null ? time.toString() : "QTime(Invalid)";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QTime)) return false;
        QTime t = (QTime) o;
        if (time == null || t.time == null) return time == t.time;
        return time.equals(t.time);
    }

    @Override
    public int hashCode() { return time != null ? time.hashCode() : 0; }
}
