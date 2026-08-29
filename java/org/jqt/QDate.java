/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 日期值类（Qt {@code QDate}，纯 Java 实现，基于 {@link java.time.LocalDate}）。
 */
public class QDate {

    private LocalDate date;   // null = 无效

    public QDate() { this.date = null; }
    public QDate(int y, int m, int d) {
        try { this.date = LocalDate.of(y, m, d); }
        catch (Exception e) { this.date = null; }
    }
    private QDate(LocalDate d) { this.date = d; }

    /** 当前日期。 */
    public static QDate currentDate() { return new QDate(LocalDate.now()); }

    public boolean isValid() { return date != null; }
    public int year() { return date != null ? date.getYear() : 0; }
    public int month() { return date != null ? date.getMonthValue() : 0; }
    public int day() { return date != null ? date.getDayOfMonth() : 0; }

    /** 星期（Qt：1=周一 ... 7=周日）。 */
    public int dayOfWeek() { return date != null ? date.getDayOfWeek().getValue() : 0; }
    /** 年内第几天。 */
    public int dayOfYear() { return date != null ? date.getDayOfYear() : 0; }
    /** 当月天数。 */
    public int daysInMonth() { return date != null ? date.lengthOfMonth() : 0; }
    /** 当年天数。 */
    public int daysInYear() { return date != null ? date.lengthOfYear() : 0; }

    public QDate addDays(int ndays) { return date != null ? new QDate(date.plusDays(ndays)) : new QDate(); }
    public QDate addMonths(int nmonths) { return date != null ? new QDate(date.plusMonths(nmonths)) : new QDate(); }
    public QDate addYears(int nyears) { return date != null ? new QDate(date.plusYears(nyears)) : new QDate(); }

    /** 到另一日期天数差。 */
    public long daysTo(QDate other) {
        return date != null && other.date != null ? java.time.temporal.ChronoUnit.DAYS.between(date, other.date) : 0;
    }

    /** 是否早于。 */
    public boolean isBefore(QDate other) { return date != null && other.date != null && date.isBefore(other.date); }
    public boolean isAfter(QDate other) { return date != null && other.date != null && date.isAfter(other.date); }

    /** ISO 格式 yyyy-MM-dd。 */
    public String toString(String format) {
        if (date == null) return "";
        try { return date.format(DateTimeFormatter.ofPattern(format, Locale.ROOT)); }
        catch (Exception e) { return ""; }
    }

    /** 解析（Qt fromString：支持 yyyy-MM-dd / yyyy/M/d / d.M.yyyy）。 */
    public static QDate fromString(String s, String format) {
        try { return new QDate(LocalDate.parse(s, DateTimeFormatter.ofPattern(format, Locale.ROOT))); }
        catch (Exception e) { return new QDate(); }
    }

    /** 转 java.time.LocalDate。 */
    public LocalDate toLocalDate() { return date; }
    public static QDate fromLocalDate(LocalDate d) { return new QDate(d); }

    @Override
    public String toString() { return date != null ? date.toString() : "QDate(Invalid)"; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QDate)) return false;
        QDate d = (QDate) o;
        if (date == null || d.date == null) return date == d.date;
        return date.equals(d.date);
    }

    @Override
    public int hashCode() { return date != null ? date.hashCode() : 0; }
}
