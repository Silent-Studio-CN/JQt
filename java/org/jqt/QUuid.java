/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.UUID;

/**
 * UUID（Qt {@code QUuid}，纯 Java 实现，委托 {@link java.util.UUID}）。
 * <p>提供 {@link #toString()}（含花括号）、{@link #createUuid()}、{@link #fromString(String)}。
 */
public class QUuid {

    private final UUID uuid;
    private final boolean valid;

    public QUuid() { this.uuid = null; this.valid = false; }
    public QUuid(UUID uuid) { this.uuid = uuid; this.valid = uuid != null; }
    public QUuid(String text) { this.uuid = parse(text); this.valid = this.uuid != null; }

    private static UUID parse(String text) {
        if (text == null) return null;
        String s = text.trim();
        if (s.startsWith("{") && s.endsWith("}")) s = s.substring(1, s.length() - 1);
        try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
    }

    /** 随机 UUID。 */
    public static QUuid createUuid() { return new QUuid(UUID.randomUUID()); }

    /** 解析字符串（含可选花括号）。 */
    public static QUuid fromString(String text) { return new QUuid(text); }

    public boolean isNull() { return !valid || uuid == null; }

    public String toString() { return uuid != null ? uuid.toString() : ""; }
    /** 带花括号形式（{xxxxxxxx-...}）。 */
    public String toStringWithBraces() { return uuid != null ? "{" + uuid + "}" : ""; }

    /** 转 java.util.UUID。 */
    public UUID toUuid() { return uuid; }
    public static QUuid fromUuid(UUID u) { return new QUuid(u); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QUuid)) return false;
        QUuid q = (QUuid) o;
        if (uuid == null || q.uuid == null) return uuid == q.uuid;
        return uuid.equals(q.uuid);
    }

    @Override
    public int hashCode() { return uuid != null ? uuid.hashCode() : 0; }
}
