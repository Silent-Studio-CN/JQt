/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON 值（Qt {@code QJsonValue}，纯 Java 实现，内置极简递归下降解析器）。
 * <p>类型：Null / Bool / Double / String / Array / Object / Undefined。
 * 提供 {@link #parse(String)} / {@link #fromJson(String)} 解析与 JSON 序列化 {@link #toString()}。
 */
public class QJsonValue {

    /** 值类型（Qt Type）。 */
    public enum Type { Null(0), Bool(1), Double(2), String(3), Array(4), Object(5), Undefined(6);
        public final int value;
        Type(int v) { value = v; }
    }

    private final Type type;
    private final Object data;

    public QJsonValue() { this.type = Type.Null; this.data = null; }
    public QJsonValue(Type t) { this.type = t; this.data = null; }
    public QJsonValue(boolean b) { this.type = Type.Bool; this.data = b; }
    public QJsonValue(double d) { this.type = Type.Double; this.data = d; }
    public QJsonValue(int i) { this.type = Type.Double; this.data = (double) i; }
    public QJsonValue(long l) { this.type = Type.Double; this.data = (double) l; }
    public QJsonValue(String s) { this.type = s != null ? Type.String : Type.Null; this.data = s; }
    public QJsonValue(QJsonArray a) { this.type = a != null ? Type.Array : Type.Null; this.data = a; }
    public QJsonValue(QJsonObject o) { this.type = o != null ? Type.Object : Type.Null; this.data = o; }
    QJsonValue(Type t, Object d) { this.type = t; this.data = d; }

    // ---- 类型判断 ----
    public Type type() { return type; }
    public boolean isNull() { return type == Type.Null; }
    public boolean isBool() { return type == Type.Bool; }
    public boolean isDouble() { return type == Type.Double; }
    public boolean isString() { return type == Type.String; }
    public boolean isArray() { return type == Type.Array; }
    public boolean isObject() { return type == Type.Object; }
    public boolean isUndefined() { return type == Type.Undefined; }

    // ---- 取值 ----
    public boolean toBool() { return type == Type.Bool && (Boolean) data; }
    public boolean toBool(boolean defaultValue) { return type == Type.Bool ? (Boolean) data : defaultValue; }
    public double toDouble() { return type == Type.Double ? (Double) data : 0; }
    public double toDouble(double defaultValue) { return type == Type.Double ? (Double) data : defaultValue; }
    public int toInt() { return (int) toDouble(); }
    public String toString() { return type == Type.String ? (String) data : stringify(); }
    public String toString(String defaultValue) { return type == Type.String ? (String) data : defaultValue; }
    public QJsonArray toArray() { return type == Type.Array ? (QJsonArray) data : new QJsonArray(); }
    public QJsonObject toObject() { return type == Type.Object ? (QJsonObject) data : new QJsonObject(); }

    /** 字符串形式（Qt toString 语义：字符串类型原样，其他序列化）。 */
    public String toVariant() { return toString(); }

    // ---- JSON 序列化 ----
    private String stringify() {
        switch (type) {
            case Null: return "null";
            case Bool: return (Boolean) data ? "true" : "false";
            case Double: {
                double d = (Double) data;
                if (d == Math.floor(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
                    return String.valueOf((long) d);
                }
                return String.valueOf(d);
            }
            case String: return escape((String) data);
            case Array: return data.toString();
            case Object: return data.toString();
            default: return "null";
        }
    }

    /** 序列化为 JSON 文本（非字符串类型时）。 */
    public String toJson() { return stringify(); }

    // ---- 解析 ----
    /** 解析 JSON 文本。 */
    public static QJsonValue parse(String json) { return fromJson(json); }

    /** 解析 JSON 文本（Qt fromJson 语义）。 */
    public static QJsonValue fromJson(String json) {
        if (json == null) return new QJsonValue(Type.Undefined);
        Parser p = new Parser(json);
        QJsonValue v = p.parseValue();
        p.skipWs();
        return p.pos >= json.length() ? v : new QJsonValue(Type.Undefined);
    }

    /** 字符串值的 JSON 引号形式。 */
    public static String escape(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.append('\"').toString();
    }

    /** 极简递归下降解析器。 */
    static final class Parser {
        final String s;
        int pos;

        Parser(String s) { this.s = s; }

        void skipWs() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        }

        QJsonValue parseValue() {
            skipWs();
            if (pos >= s.length()) return new QJsonValue(Type.Undefined);
            char c = s.charAt(pos);
            if (c == '{') return new QJsonValue(parseObject());
            if (c == '[') return new QJsonValue(parseArray());
            if (c == '\"') return new QJsonValue(parseString());
            if (c == 't') { expect("true"); return new QJsonValue(true); }
            if (c == 'f') { expect("false"); return new QJsonValue(false); }
            if (c == 'n') { expect("null"); return new QJsonValue(); }
            return parseNumber();
        }

        QJsonObject parseObject() {
            QJsonObject o = new QJsonObject();
            pos++;  // {
            skipWs();
            if (pos < s.length() && s.charAt(pos) == '}') { pos++; return o; }
            while (pos < s.length()) {
                skipWs();
                if (s.charAt(pos) != '\"') break;
                String key = parseString();
                skipWs();
                if (pos < s.length() && s.charAt(pos) == ':') pos++;
                QJsonValue v = parseValue();
                o.insert(key, v);
                skipWs();
                if (pos < s.length() && s.charAt(pos) == ',') { pos++; continue; }
                if (pos < s.length() && s.charAt(pos) == '}') { pos++; break; }
                break;
            }
            return o;
        }

        QJsonArray parseArray() {
            QJsonArray a = new QJsonArray();
            pos++;  // [
            skipWs();
            if (pos < s.length() && s.charAt(pos) == ']') { pos++; return a; }
            while (pos < s.length()) {
                a.append(parseValue());
                skipWs();
                if (pos < s.length() && s.charAt(pos) == ',') { pos++; continue; }
                if (pos < s.length() && s.charAt(pos) == ']') { pos++; break; }
                break;
            }
            return a;
        }

        String parseString() {
            pos++;  // "
            StringBuilder sb = new StringBuilder();
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '\"') break;
                if (c == '\\' && pos < s.length()) {
                    char e = s.charAt(pos++);
                    switch (e) {
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'u':
                            if (pos + 4 <= s.length()) {
                                try { sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16)); pos += 4; }
                                catch (NumberFormatException ex) { sb.append('?'); }
                            }
                            break;
                        default: sb.append(e);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        QJsonValue parseNumber() {
            int start = pos;
            if (pos < s.length() && (s.charAt(pos) == '-' || s.charAt(pos) == '+')) pos++;
            while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.' || s.charAt(pos) == 'e' || s.charAt(pos) == 'E' || s.charAt(pos) == '-' || s.charAt(pos) == '+')) pos++;
            try {
                return new QJsonValue(Double.parseDouble(s.substring(start, pos)));
            } catch (NumberFormatException e) {
                return new QJsonValue(Type.Undefined);
            }
        }

        void expect(String word) {
            if (pos + word.length() <= s.length() && s.startsWith(word, pos)) pos += word.length();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QJsonValue)) return false;
        QJsonValue q = (QJsonValue) o;
        if (type != q.type) return false;
        if (data == null || q.data == null) return data == q.data;
        return data.equals(q.data);
    }

    @Override
    public int hashCode() { return data != null ? data.hashCode() : type.hashCode(); }
}
