/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正则表达式（Qt {@code QRegularExpression}，纯 Java 实现，委托 {@link java.util.regex.Pattern}）。
 * <p>提供 {@link #match(String)} / {@link #globalMatch(String)} / {@link #replace(String, String)}
 * / {@link #split(String)} / {@link #escape(String)}。
 */
public class QRegularExpression {

    /** 匹配结果（Qt QRegularExpressionMatch 语义）。 */
    public static class Match {
        private final String text;
        private final Matcher m;
        private final boolean matched;
        private final int start;
        private final int end;

        Match(Matcher m, String text, int start, int end) {
            this.m = m; this.text = text; this.matched = start >= 0;
            this.start = start; this.end = end;
        }

        public boolean hasMatch() { return matched; }
        public int capturedStart() { return start; }
        public int capturedEnd() { return end; }
        public int capturedLength() { return matched ? end - start : 0; }
        public String captured() { return matched ? text.substring(start, end) : ""; }
        public String captured(int index) { return matched && m != null && index <= m.groupCount() ? m.group(index) : ""; }
        public int lastCapturedIndex() { return m != null ? m.groupCount() : -1; }

        /** 下一次匹配（全局迭代）。 */
        public Match next() {
            if (m != null && m.find()) return new Match(m, text, m.start(), m.end());
            return new Match(m, text, -1, -1);
        }
    }

    private Pattern pattern;
    private String patternText;
    private boolean valid;

    public QRegularExpression() { this("", 0); }

    public QRegularExpression(String pattern) { this(pattern, 0); }

    /** patternOptions：1=CaseInsensitive, 2=Multiline, 4=DotMatchesEverything, 8=Extended。 */
    public QRegularExpression(String pattern, int patternOptions) {
        this.patternText = pattern != null ? pattern : "";
        int flags = 0;
        if ((patternOptions & 1) != 0) flags |= Pattern.CASE_INSENSITIVE;
        if ((patternOptions & 2) != 0) flags |= Pattern.MULTILINE;
        if ((patternOptions & 4) != 0) flags |= Pattern.DOTALL;
        if ((patternOptions & 8) != 0) flags |= Pattern.COMMENTS;
        try {
            this.pattern = Pattern.compile(this.patternText, flags);
            this.valid = true;
        } catch (Exception e) {
            this.pattern = null;
            this.valid = false;
        }
    }

    public String pattern() { return patternText; }
    public boolean isValid() { return valid; }
    public boolean isValidRegularExpression() { return valid; }

    /** 全文匹配。 */
    public boolean matches(String text) {
        return pattern != null && text != null && pattern.matcher(text).matches();
    }

    /** 首个匹配。 */
    public Match match(String text, int from) {
        if (pattern == null || text == null) return new Match(null, text, -1, -1);
        Matcher m = pattern.matcher(text);
        return m.find(from) ? new Match(m, text, m.start(), m.end()) : new Match(m, text, -1, -1);
    }
    public Match match(String text) { return match(text, 0); }

    /** 全部匹配。 */
    public List<Match> globalMatch(String text) {
        List<Match> out = new ArrayList<>();
        if (pattern == null || text == null) return out;
        Matcher m = pattern.matcher(text);
        while (m.find()) out.add(new Match(m, text, m.start(), m.end()));
        return out;
    }

    /** 全部替换。 */
    public String replace(String text, String after) {
        if (pattern == null || text == null) return text;
        return pattern.matcher(text).replaceAll(Matcher.quoteReplacement(after != null ? after : ""));
    }

    /** 拆分。 */
    public List<String> split(String text) {
        List<String> out = new ArrayList<>();
        if (pattern == null || text == null) { if (text != null) out.add(text); return out; }
        String[] parts = pattern.split(text);
        java.util.Collections.addAll(out, parts);
        return out;
    }

    /** 锚定模式（^...$）。 */
    public String anchoredPattern() { return "\\A(?:" + patternText + ")\\z"; }

    /** 转义正则特殊字符。 */
    public static String escape(String str) {
        return str != null ? Pattern.quote(str).replace("\\Q", "").replace("\\E", "") : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QRegularExpression)) return false;
        QRegularExpression q = (QRegularExpression) o;
        return patternText.equals(q.patternText) && valid == q.valid;
    }

    @Override
    public int hashCode() { return patternText.hashCode(); }

    @Override
    public String toString() { return "QRegularExpression(" + patternText + ")"; }
}
