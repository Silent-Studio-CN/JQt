/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 终态（Qt {@code QFinalState}）：进入后其父状态视为"完成"。
 * <pre>
 * QState work = new QState(machine, "work");
 * QFinalState done = new QFinalState(work);
 * work.setInitialState(done);
 * machine.onFinished(() -&gt; System.out.println("机器结束"));
 * </pre>
 * 若终态的父状态是状态机的顶层子状态，状态机会发出 finished 并自动停止。
 */
public class QFinalState extends QAbstractState {

    public QFinalState() { super(); }

    public QFinalState(String id) { super(id); }

    public QFinalState(QState parent) {
        super();
        if (parent != null) parent.addState(this);
    }

    public QFinalState(QState parent, String id) {
        super(id);
        if (parent != null) parent.addState(this);
    }

    /** 便捷：批量创建多个终态。 */
    public static List<QFinalState> of(QState parent, String... ids) {
        List<QFinalState> out = new ArrayList<>();
        for (String id : ids) out.add(new QFinalState(parent, id));
        return Collections.unmodifiableList(out);
    }
}
