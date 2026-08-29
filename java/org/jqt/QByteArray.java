/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 字节数组（Qt {@code QByteArray}，纯 Java 实现）。
 */
public class QByteArray {

    private byte[] data;

    public QByteArray() { this.data = new byte[0]; }
    public QByteArray(byte[] data) { this.data = data != null ? data.clone() : new byte[0]; }
    public QByteArray(String s) { this.data = s != null ? s.getBytes(StandardCharsets.UTF_8) : new byte[0]; }

    public static QByteArray fromHex(String hex) {
        if (hex == null) return new QByteArray();
        String h = hex.trim();
        if (h.length() % 2 != 0) h = "0" + h;
        byte[] out = new byte[h.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(h.substring(i * 2, i * 2 + 2), 16);
        }
        return new QByteArray(out);
    }

    public static QByteArray fromBase64(String base64) {
        return new QByteArray(java.util.Base64.getDecoder().decode(base64 != null ? base64 : ""));
    }

    public boolean isEmpty() { return data.length == 0; }
    public int size() { return data.length; }
    public int count() { return data.length; }
    public byte at(int i) { return data[i]; }
    public byte[] toByteArray() { return data.clone(); }

    public void append(byte b) {
        byte[] n = Arrays.copyOf(data, data.length + 1);
        n[data.length] = b;
        data = n;
    }
    public void append(QByteArray other) {
        byte[] n = Arrays.copyOf(data, data.length + other.data.length);
        System.arraycopy(other.data, 0, n, data.length, other.data.length);
        data = n;
    }
    public QByteArray plus(QByteArray other) {
        QByteArray r = new QByteArray(this.data);
        r.append(other);
        return r;
    }

    public QByteArray mid(int index, int len) {
        int start = Math.max(0, Math.min(index, data.length));
        int end = Math.max(start, Math.min(data.length, index + len));
        return new QByteArray(Arrays.copyOfRange(data, start, end));
    }

    public QByteArray left(int len) { return mid(0, len); }
    public QByteArray right(int len) {
        if (len <= 0 || data.length == 0) return new QByteArray();
        int start = Math.max(0, data.length - len);
        return new QByteArray(Arrays.copyOfRange(data, start, data.length));
    }

    public boolean startsWith(QByteArray other) {
        if (other.data.length > data.length) return false;
        for (int i = 0; i < other.data.length; i++) if (data[i] != other.data[i]) return false;
        return true;
    }
    public boolean endsWith(QByteArray other) {
        if (other.data.length > data.length) return false;
        int off = data.length - other.data.length;
        for (int i = 0; i < other.data.length; i++) if (data[off + i] != other.data[i]) return false;
        return true;
    }
    public boolean contains(QByteArray other) {
        if (other.data.length == 0) return true;
        outer:
        for (int i = 0; i <= data.length - other.data.length; i++) {
            for (int j = 0; j < other.data.length; j++) {
                if (data[i + j] != other.data[j]) continue outer;
            }
            return true;
        }
        return false;
    }

    public byte[] data() { return data; }

    public String toHex() {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) sb.append(String.format("%02x", b & 0xFF));
        return sb.toString();
    }
    public String toBase64() { return java.util.Base64.getEncoder().encodeToString(data); }

    @Override
    public String toString() { return new String(data, StandardCharsets.UTF_8); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QByteArray)) return false;
        return Arrays.equals(data, ((QByteArray) o).data);
    }

    @Override
    public int hashCode() { return Arrays.hashCode(data); }
}
