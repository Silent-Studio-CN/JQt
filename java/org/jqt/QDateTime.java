/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 日期时间值类（Qt {@code QDateTime}，纯 Java 实现，基于 {@link java.time.LocalDateTime}）。
 * <p>提供 {@link #toLocalDateTime()} / {@link #fromLocalDateTime(LocalDateTime)} 与 Java 生态互转。
 */
public class QDateTime {

    private LocalDateTime dt;   // null = 无效

    public QDateTime() { this.dt = null; }

    public QDateTime(QDate date, QTime time) {
        LocalDate d = date != null ? date.toLocalDate() : null;
        LocalTime t = time != null ? time.toLocalTime() : null;
        if (d != null && t != null) this.dt = LocalDateTime.of(d, t);
        else this.dt = null;
    }

    private QDateTime(LocalDateTime dt) { this.dt = dt; }

    public static QDateTime currentDateTime() { return new QDateTime(LocalDateTime.now()); }

    public boolean isValid() { return dt != null; }

    public QDate date() { return dt != null ? QDate.fromLocalDate(dt.toLocalDate()) : new QDate(); }
    public QTime time() { return dt != null ? QTime.fromLocalTime(dt.toLocalTime()) : new QTime(); }

    public QDateTime addDays(int ndays) { return dt != null ? new QDateTime(dt.plusDays(ndays)) : new QDateTime(); }
    public QDateTime addMonths(int nmonths) { return dt != null ? new QDateTime(dt.plusMonths(nmonths)) : new QDateTime(); }
    public QDateTime addYears(int nyears) { return dt != null ? new QDateTime(dt.plusYears(nyears)) : new QDateTime(); }
    public QDateTime addSecs(long secs) { return dt != null ? new QDateTime(dt.plusSeconds(secs)) : new QDateTime(); }
    public QDateTime addMSecs(long ms) { return dt != null ? new QDateTime(dt.plusNanos(ms * 1_000_000L)) : new QDateTime(); }

    /** 到另一时刻秒差。 */
    public long secsTo(QDateTime other) {
        return dt != null && other.dt != null ? java.time.temporal.ChronoUnit.SECONDS.between(dt, other.dt) : 0;
    }
    public long msecsTo(QDateTime other) {
        return dt != null && other.dt != null ? java.time.temporal.ChronoUnit.MILLIS.between(dt, other.dt) : 0;
    }

    /** 自 1970-01-01T00:00:00Z 的毫秒数（本地时区语义，Qt 默认 LocalTime）。 */
    public long toMSecsSinceEpoch() {
        return dt != null ? dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : Long.MIN_VALUE;
    }

    public static QDateTime fromMSecsSinceEpoch(long msecs) {
        return new QDateTime(LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(msecs), ZoneId.systemDefault()));
    }

    /** ISO-8601 格式。 */
    public String toString(String format) {
        if (dt == null) return "";
        try { return dt.format(DateTimeFormatter.ofPattern(format, Locale.ROOT)); }
        catch (Exception e) { return ""; }
    }

    public static QDateTime fromString(String s, String format) {
        try { return new QDateTime(LocalDateTime.parse(s, DateTimeFormatter.ofPattern(format, Locale.ROOT))); }
        catch (Exception e) { return new QDateTime(); }
    }

    public LocalDateTime toLocalDateTime() { return dt; }
    public static QDateTime fromLocalDateTime(LocalDateTime dt) { return new QDateTime(dt); }

    @Override
    public String toString() { return dt != null ? dt.toString() : "QDateTime(Invalid)"; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QDateTime)) return false;
        QDateTime q = (QDateTime) o;
        if (dt == null || q.dt == null) return dt == q.dt;
        return dt.equals(q.dt);
    }

    @Override
    public int hashCode() { return dt != null ? dt.hashCode() : 0; }
}
