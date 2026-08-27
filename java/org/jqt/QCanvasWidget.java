/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 自绘画布：每次重绘时调用 {@link #onPaint(Consumer)} 回调，
 * 在回调内通过 {@link QPainter} 绘制任意 2D 内容。
 * <pre>
 * QCanvasWidget canvas = new QCanvasWidget();
 * canvas.onPaint(p -> {
 *     p.setColor(0xFF4CC2FF);
 *     p.fillRect(10, 10, 80, 40);
 *     p.setColor(0xFFFFFFFF);
 *     p.drawText(20, 38, "你好");
 * });
 * </pre>
 */
public class QCanvasWidget extends QWidget {

    private final List<Consumer<QPainter>> onPaintHandlers = new ArrayList<>();

    public QCanvasWidget() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    private native long nativeCreate();

    /** 注册绘制回调（每次重绘触发，参数为本次绘制可用的 {@link QPainter}）。 */
    public QCanvasWidget onPaint(Consumer<QPainter> handler) {
        onPaintHandlers.add(handler);
        return this;
    }

    /** 请求立即重绘（继承自 QWidget，v0.6.0 上移到基类）。 */

    /** 由 C++ 侧在 paintEvent 中回调（JNI）。 */
    void nativeHandlePaint() {
        long ptr = nativeCurrentPainter();
        QPainter painter = new QPainter(ptr);
        for (Consumer<QPainter> h : onPaintHandlers) {
            h.accept(painter);
        }
        painter.release();
    }

    /** 当前 paintEvent 期间的 QPainter 指针（JNI）。 */
    private static native long nativeCurrentPainter();
}
