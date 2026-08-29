/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 时区（Qt {@code QTimeZone}，纯 Java 实现，委托 {@link java.time.ZoneId}）。
 */
public class QTimeZone {

    private final ZoneId zone;
    private final boolean valid;

    public QTimeZone() { this.zone = ZoneId.systemDefault(); this.valid = true; }
    public QTimeZone(String ianaId) {
        ZoneId z = null;
        try { z = ZoneId.of(ianaId); } catch (Exception e) { /* invalid */ }
        this.zone = z; this.valid = z != null;
    }
    public QTimeZone(ZoneId zone) { this.zone = zone; this.valid = zone != null; }

    /** 系统时区。 */
    public static QTimeZone systemTimeZone() { return new QTimeZone(ZoneId.systemDefault()); }
    public static QTimeZone utc() { return new QTimeZone(ZoneOffset.UTC); }

    public static List<String> availableTimeZoneIds() {
        Set<String> ids = ZoneId.getAvailableZoneIds();
        return new ArrayList<>(ids);
    }

    public boolean isValid() { return valid && zone != null; }

    public String id() { return zone != null ? zone.getId() : ""; }

    /** 相对 UTC 的秒偏移（当前时刻）。 */
    public int offsetFromUtc() {
        if (zone == null) return 0;
        return zone.getRules().getOffset(java.time.Instant.now()).getTotalSeconds();
    }

    /** 指定时刻的秒偏移。 */
    public int offsetFromUtc(long msecsSinceEpoch) {
        if (zone == null) return 0;
        return zone.getRules().getOffset(java.time.Instant.ofEpochMilli(msecsSinceEpoch)).getTotalSeconds();
    }

    public boolean hasDaylightTime() {
        return zone != null && !zone.getRules().isFixedOffset();
    }

    public boolean isDaylightTime(long msecsSinceEpoch) {
        if (zone == null) return false;
        return zone.getRules().isDaylightSavings(java.time.Instant.ofEpochMilli(msecsSinceEpoch));
    }

    public String displayName() { return zone != null ? zone.getId() : ""; }

    public ZoneId toZoneId() { return zone; }
    public static QTimeZone fromZoneId(ZoneId z) { return new QTimeZone(z); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QTimeZone)) return false;
        QTimeZone q = (QTimeZone) o;
        return zone != null ? zone.equals(q.zone) : q.zone == null;
    }

    @Override
    public int hashCode() { return zone != null ? zone.hashCode() : 0; }

    @Override
    public String toString() { return zone != null ? zone.getId() : ""; }
}
