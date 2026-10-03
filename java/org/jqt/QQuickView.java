/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.lang.ref.Cleaner;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * QML 视图（Qt {@code QQuickView}，QtQuick 模块）：加载 QML 并显示/离屏渲染。
 * <pre>
 * QQuickView view = new QQuickView();
 * view.setSource("ui/main.qml");        // 或 setSourceData(qml 文本)
 * view.onStatusChanged(s -&gt; {
 *     if (s == QQuickView.Status.Ready) System.out.println("QML 就绪");
 *     if (s == QQuickView.Status.Error) System.out.println(view.errors());
 * });
 * view.resize(640, 480);
 * view.show();
 * byte[] png = view.grabToPng();        // 离屏渲染成图片(无需真窗口)
 * </pre>
 * <b>无头渲染</b>：离屏/CI 环境请设置环境变量 {@code QT_QUICK_BACKEND=software}
 * （Qt Quick 的场景图默认要 OpenGL）。
 * <p>可用性:{@link #isAvailable()}（QtQuick 不属于 qtbase）。
 */
public class QQuickView {

    /** 加载状态（Qt {@code QQuickView::Status}）。 */
    public static final class Status {
        private Status() {}
        public static final int Null = 0;
        public static final int Ready = 1;
        public static final int Loading = 2;
        public static final int Error = 3;
    }

    private static final Cleaner CLEANER = Cleaner.create();

    private final List<Consumer<Integer>> statusHandlers = new ArrayList<>();
    private long nativeHandle;
    private String source = "";
    private String errors = "";
    private int lastStatus = Status.Null;
    private int width = 0;
    private int height = 0;

    /** 本库是否编译进 QtQuick 支持。 */
    public static boolean isAvailable() { return nativeAvailable(); }
    private static native boolean nativeAvailable();

    /** 创建空视图。 */
    public QQuickView() {
        if (!isAvailable()) {
            throw new IllegalStateException(
                "本库未编译 QtQuick 支持(缺少 qtdeclarative 模块);先安装该模块再构建 libjqt");
        }
        nativeHandle = nativeCreate();
        final long h = nativeHandle;
        CLEANER.register(this, () -> nativeDispose(h));
        nativeConnectStatus(nativeHandle);
    }

    private static native long nativeCreate();
    private static native void nativeDispose(long handle);
    private native void nativeConnectStatus(long handle);   // 实例 native:回调需要 Java 对象引用

    long nativeHandle() { return nativeHandle; }

    /** 从文件加载 QML（本地路径或 qrc: 资源）。 */
    public void setSource(String url) {
        source = url == null ? "" : url;
        nativeSetSource(nativeHandle, source);
    }
    private static native void nativeSetSource(long handle, String url);

    /** 直接从 QML 文本加载（免去建文件,便于测试与内嵌界面）。 */
    public void setSourceData(String qml) {
        source = "(inline)";
        nativeSetSourceData(nativeHandle, qml == null ? "" : qml);
    }
    private static native void nativeSetSourceData(long handle, String qml);

    /** 当前 QML 源（文件路径或 "(inline)"）。 */
    public String source() { return source; }

    /** 设置尺寸（QML 根元素无固定尺寸时以此为准）。 */
    public void resize(int w, int h) {
        width = w; height = h;
        nativeResize(nativeHandle, w, h);
    }
    private static native void nativeResize(long handle, int w, int h);

    /** 当前宽。 */
    public int width() { return width; }

    /** 当前高。 */
    public int height() { return height; }

    /** 显示（真窗口环境;离屏渲染可不调用）。 */
    public void show() { nativeShow(nativeHandle); }
    private static native void nativeShow(long handle);

    /** 隐藏。 */
    public void hide() { nativeHide(nativeHandle); }
    private static native void nativeHide(long handle);

    /** 加载状态（{@link Status}）。 */
    public int status() { return lastStatus; }

    /** 加载错误（无错误为空串）。 */
    public String errors() { return errors; }

    /** 把当前内容渲染成 PNG 字节（离屏可用）。 */
    public byte[] grabToPng() { return nativeGrabToPng(nativeHandle); }
    private static native byte[] nativeGrabToPng(long handle);

    /** 状态变化回调（{@link Status}）。 */
    public QQuickView onStatusChanged(Consumer<Integer> handler) {
        statusHandlers.add(handler);
        return this;
    }

    /** 由 C++ 侧回调（JNI，Qt 主线程）。 */
    void nativeHandleStatusChanged(int status, String errs) {
        lastStatus = status;
        errors = errs == null ? "" : errs;
        for (Consumer<Integer> h : statusHandlers) h.accept(status);
    }
}
