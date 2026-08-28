/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * OpenGL 画布控件（QOpenGLWidget，Qt6OpenGLWidgets）：把 OpenGL 渲染集成进 Qt 控件树。
 * <p>
 * 通用渲染画布：2D 加速绘制、图像/视频帧、图表，以及任意 3D 渲染
 * （Java 侧可用 LWJGL 挂接当前 context —— paintGL 回调执行时 GL context 已是 current）。
 * <p>
 * 生命周期回调（与 Qt 的 initializeGL/paintGL/resizeGL 一一对应）：
 * <pre>
 * QOpenGLWidget gl = new QOpenGLWidget();
 * gl.setClearColor(0xFF101418);          // 清屏色（默认自动清屏）
 * gl.onInitialize(() -> { ... });        // GL 初始化（只回调一次）
 * gl.onPaint(() -> { ... });             // 每帧渲染（context current）
 * gl.onResized((w, h) -> { ... });       // 尺寸变化
 * layout.addWidget(gl);
 * </pre>
 * 触发重绘：{@link #update()}（继承自 {@link QWidget}，Qt 语义）。
 */
public class QOpenGLWidget extends QWidget {

    private final List<Runnable> onInitializeHandlers = new ArrayList<>();
    private final List<Runnable> onPaintHandlers = new ArrayList<>();
    private final List<BiConsumer<Integer, Integer>> onResizedHandlers = new ArrayList<>();

    /**
     * 创建 OpenGL 画布。
     * @throws UnsupportedOperationException 当前平台 Qt 不含 OpenGLWidgets 模块
     *         （Windows ARM64 官方构建裁剪了该模块）
     */
    public QOpenGLWidget() {
        nativeHandle = nativeCreate();
        if (nativeHandle == 0) {
            throw new UnsupportedOperationException(
                "QOpenGLWidget 当前平台不可用（macOS / Windows ARM64 的 Qt 构建不含 OpenGLWidgets 模块）");
        }
        registerCleaner();
    }

    private native long nativeCreate();

    /**
     * 注册 GL 初始化回调（initializeGL；GL context 首次可用时回调一次，
     * 此时可查询 GL 版本/扩展、上传纹理等）。
     */
    public QOpenGLWidget onInitialize(Runnable handler) {
        onInitializeHandlers.add(handler);
        return this;
    }

    /**
     * 注册渲染回调（paintGL；每次重绘回调，执行时 GL context 已 current）。
     * 在此绘制 2D/3D 内容；绘制完成后调用 {@link #update()} 请求下一帧。
     */
    public QOpenGLWidget onPaint(Runnable handler) {
        onPaintHandlers.add(handler);
        return this;
    }

    /**
     * 注册尺寸变化回调（resizeGL；参数为视口像素宽高，执行时 GL context 已 current，
     * 应在此更新 glViewport / 投影矩阵）。
     */
    public QOpenGLWidget onResized(BiConsumer<Integer, Integer> handler) {
        onResizedHandlers.add(handler);
        return this;
    }

    /** 由 C++ 侧在 initializeGL 时回调（JNI）。 */
    void nativeHandleInitialize() {
        for (Runnable h : onInitializeHandlers) h.run();
    }

    /** 由 C++ 侧在 paintGL 时回调（JNI）。 */
    void nativeHandlePaint() {
        for (Runnable h : onPaintHandlers) h.run();
    }

    /** 由 C++ 侧在 resizeGL 时回调（JNI）。 */
    void nativeHandleResized(int width, int height) {
        for (BiConsumer<Integer, Integer> h : onResizedHandlers) h.accept(width, height);
    }

    /**
     * 设置清屏色（0xAARRGGBB；{@link #setAutoClear(boolean)} 开启时每次渲染前自动清屏）。
     */
    public void setClearColor(int argb) {
        nativeSetClearColor(nativeHandle, argb);
    }
    private native void nativeSetClearColor(long handle, int argb);

    /**
     * 渲染前自动清屏（默认 true；关闭后清屏由 {@link #onPaint(Runnable)} 自行处理，
     * 对应 Qt 中用户自己调用 glClear 的写法）。
     */
    public void setAutoClear(boolean on) {
        nativeSetAutoClear(nativeHandle, on);
    }
    private native void nativeSetAutoClear(long handle, boolean on);

    /**
     * 使 GL context 在当前线程 current（QOpenGLWidget::makeCurrent）。
     * paintGL 回调内已 current，无需调用；GUI 线程外初始化 GL 资源时可能需要。
     */
    public void makeCurrent() {
        nativeMakeCurrent(nativeHandle);
    }
    private native void nativeMakeCurrent(long handle);

    /** 释放当前线程的 GL context（QOpenGLWidget::doneCurrent）。 */
    public void doneCurrent() {
        nativeDoneCurrent(nativeHandle);
    }
    private native void nativeDoneCurrent(long handle);
}
