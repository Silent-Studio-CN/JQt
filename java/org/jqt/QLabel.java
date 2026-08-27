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
 * 文本标签：封装 C++ 侧的 {@code QLabel}。
 */
public class QLabel extends QWidget {

    public QLabel(String text) {
        nativeHandle = nativeCreate(text);
        registerCleaner();
    }

    private native long nativeCreate(String text);

    /** 修改标签文字。 */
    public void setText(String text) {
        nativeSetText(nativeHandle, text);
    }
    private native void nativeSetText(long handle, String text);

    /** 读取当前文字。 */
    public String text() {
        return nativeText(nativeHandle);
    }
    private native String nativeText(long handle);

    // ---- L1 补全（v0.6.0）----

    /** 清空文本。 */
    public void clear() { nativeClear(nativeHandle); }
    private static native void nativeClear(long handle);

    /** 文本对齐：1 左 / 2 右 / 4 居中 / 8 两端。 */
    public void setAlignment(int alignment) { nativeSetAlignment(nativeHandle, alignment); }
    private static native void nativeSetAlignment(long handle, int alignment);

    /** 文本对齐。 */
    public int alignment() { return nativeAlignment(nativeHandle); }
    private static native int nativeAlignment(long handle);

    /** 自动换行。 */
    public void setWordWrap(boolean wrap) { nativeSetWordWrap(nativeHandle, wrap); }
    private static native void nativeSetWordWrap(long handle, boolean wrap);

    /** 是否自动换行。 */
    public boolean wordWrap() { return nativeWordWrap(nativeHandle); }
    private static native boolean nativeWordWrap(long handle);

    /** 外边距（像素）。 */
    public void setMargin(int margin) { nativeSetMargin(nativeHandle, margin); }
    private static native void nativeSetMargin(long handle, int margin);

    /** 外边距。 */
    public int margin() { return nativeMargin(nativeHandle); }
    private static native int nativeMargin(long handle);

    /** 缩进（像素）。 */
    public void setIndent(int indent) { nativeSetIndent(nativeHandle, indent); }
    private static native void nativeSetIndent(long handle, int indent);

    /** 缩进。 */
    public int indent() { return nativeIndent(nativeHandle); }
    private static native int nativeIndent(long handle);

    private final java.util.List<Consumer<String>> onLinkActivatedHandlers = new java.util.ArrayList<>();
    private volatile boolean linkConnected;

    /** 链接激活回调（linkActivated 信号，参数为链接 URL；文本需含 <a href=...>）。 */
    public QLabel onLinkActivated(Consumer<String> handler) {
        onLinkActivatedHandlers.add(handler);
        if (!linkConnected) {
            linkConnected = true;
            nativeConnectLinkActivated(nativeHandle);
        }
        return this;
    }

    private native void nativeConnectLinkActivated(long handle);

    /** 由 C++ 侧在链接激活时回调（JNI）。 */
    void nativeHandleLinkActivated(String url) {
        for (Consumer<String> h : onLinkActivatedHandlers) {
            h.accept(url);
        }
    }
}