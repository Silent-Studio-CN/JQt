/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 字符串列表（Qt {@code QStringList}，纯 Java 实现，继承 {@code ArrayList<String>}）。
 */
public class QStringList extends ArrayList<String> {

    public QStringList() { super(); }
    public QStringList(Collection<String> c) { super(c); }
    public QStringList(String... items) { super(items.length); for (String s : items) add(s); }

    public int count() { return size(); }
    public String at(int i) { return get(i); }
    public void append(String s) { add(s); }
    public String first() { return get(0); }
    public String last() { return get(size() - 1); }
    public void prepend(String s) { add(0, s); }
    public void removeAt(int i) { remove(i); }
    public void replace(int i, String s) { set(i, s); }

    public boolean contains(String s) { return super.contains(s); }

    /** 是否含匹配项（正则）。 */
    public boolean contains(QRegularExpression re) {
        for (String s : this) if (re.matches(s)) return true;
        return false;
    }

    /** 以 sep 连接。 */
    public String join(String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size(); i++) {
            if (i > 0) sb.append(sep);
            sb.append(get(i));
        }
        return sb.toString();
    }

    /** 过滤：保留匹配正则的项。 */
    public QStringList filter(QRegularExpression re) {
        QStringList out = new QStringList();
        for (String s : this) if (re.matches(s)) out.add(s);
        return out;
    }
    /** 过滤：保留包含 subStr 的项。 */
    public QStringList filter(String subStr) {
        QStringList out = new QStringList();
        for (String s : this) if (s != null && s.contains(subStr)) out.add(s);
        return out;
    }

    /** 去重（保持顺序）。 */
    public QStringList removeDuplicates() {
        QStringList out = new QStringList();
        for (String s : this) if (!out.contains(s)) out.add(s);
        return out;
    }

    /** 排序（自然序）。 */
    public void sort() { java.util.Collections.sort(this); }

    public QStringList plus(QStringList other) {
        QStringList r = new QStringList(this);
        r.addAll(other);
        return r;
    }
}
