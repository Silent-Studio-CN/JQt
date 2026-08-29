/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 进程环境变量（Qt {@code QProcessEnvironment}，纯 Java 实现）。
 */
public class QProcessEnvironment {

    private final Map<String, String> vars;

    public QProcessEnvironment() { this.vars = new LinkedHashMap<>(); }
    private QProcessEnvironment(Map<String, String> m) { this.vars = new LinkedHashMap<>(m); }

    /** 空环境。 */
    public static QProcessEnvironment systemEnvironment() {
        return new QProcessEnvironment(System.getenv());
    }

    public boolean isEmpty() { return vars.isEmpty(); }

    public boolean contains(String name) { return vars.containsKey(name); }

    public String value(String name) { return vars.get(name); }
    public String value(String name, String defaultValue) {
        return vars.containsKey(name) ? vars.get(name) : defaultValue;
    }

    public void insert(String name, String value) { vars.put(name, value); }
    public void remove(String name) { vars.remove(name); }
    public void clear() { vars.clear(); }

    public List<String> keys() { return new ArrayList<>(vars.keySet()); }

    public String toString() { return "QProcessEnvironment(" + vars.size() + " vars)"; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QProcessEnvironment)) return false;
        return vars.equals(((QProcessEnvironment) o).vars);
    }

    @Override
    public int hashCode() { return vars.hashCode(); }
}
