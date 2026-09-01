/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.lang.ref.Cleaner;

/**
 * 字体（Qt {@code QFont}，native 句柄管理 + Java Cleaner 自动释放）。
 * <p>完整覆盖 Qt 6 QFont 常用 API。(Android 构建：AWT 桥已裁剪。)
 */
public class QFont {

    private static final Cleaner CLEANER = Cleaner.create();

    /** 字重（Qt 6 Weight：100-900，CSS 风格）。 */
    public enum Weight {
        Thin(100), ExtraLight(200), Light(300), Normal(400), Medium(500),
        DemiBold(600), Bold(700), ExtraBold(800), Black(900);
        public final int value;
        Weight(int v) { value = v; }
    }

    private final long nativeHandle;
    private final Cleaner.Cleanable cleanable;

    public QFont() {
        nativeHandle = nativeCreate();
        cleanable = CLEANER.register(this, new Disposer(nativeHandle));
    }

    public QFont(String family) {
        nativeHandle = nativeCreateFamily(family);
        cleanable = CLEANER.register(this, new Disposer(nativeHandle));
    }

    public QFont(String family, int pointSize) {
        nativeHandle = nativeCreateFamilySize(family, pointSize);
        cleanable = CLEANER.register(this, new Disposer(nativeHandle));
    }

    public QFont(String family, int pointSize, int weight, boolean italic) {
        nativeHandle = nativeCreateFull(family, pointSize, weight, italic);
        cleanable = CLEANER.register(this, new Disposer(nativeHandle));
    }

    private static final class Disposer implements Runnable {
        private long h;
        Disposer(long h) { this.h = h; }
        @Override public void run() { if (h != 0) { nativeDispose(h); h = 0; } }
    }

    private static native long nativeCreate();
    private static native long nativeCreateFamily(String family);
    private static native long nativeCreateFamilySize(String family, int pointSize);
    private static native long nativeCreateFull(String family, int pointSize, int weight, boolean italic);
    private static native void nativeDispose(long handle);

    public String family() { return nativeFamily(nativeHandle); }
    private static native String nativeFamily(long handle);
    public void setFamily(String family) { nativeSetFamily(nativeHandle, family); }
    private static native void nativeSetFamily(long handle, String family);

    public int pointSize() { return nativePointSize(nativeHandle); }
    private static native int nativePointSize(long handle);
    public void setPointSize(int size) { nativeSetPointSize(nativeHandle, size); }
    private static native void nativeSetPointSize(long handle, int size);
    public double pointSizeF() { return nativePointSizeF(nativeHandle); }
    private static native double nativePointSizeF(long handle);
    public int pixelSize() { return nativePixelSize(nativeHandle); }
    private static native int nativePixelSize(long handle);
    public void setPixelSize(int size) { nativeSetPixelSize(nativeHandle, size); }
    private static native void nativeSetPixelSize(long handle, int size);

    public boolean bold() { return nativeBold(nativeHandle); }
    private static native boolean nativeBold(long handle);
    public void setBold(boolean bold) { nativeSetBold(nativeHandle, bold); }
    private static native void nativeSetBold(long handle, boolean bold);
    public boolean italic() { return nativeItalic(nativeHandle); }
    private static native boolean nativeItalic(long handle);
    public void setItalic(boolean italic) { nativeSetItalic(nativeHandle, italic); }
    private static native void nativeSetItalic(long handle, boolean italic);
    public boolean underline() { return nativeUnderline(nativeHandle); }
    private static native boolean nativeUnderline(long handle);
    public void setUnderline(boolean u) { nativeSetUnderline(nativeHandle, u); }
    private static native void nativeSetUnderline(long handle, boolean u);
    public boolean strikeOut() { return nativeStrikeOut(nativeHandle); }
    private static native boolean nativeStrikeOut(long handle);
    public void setStrikeOut(boolean s) { nativeSetStrikeOut(nativeHandle, s); }
    private static native void nativeSetStrikeOut(long handle, boolean s);

    public int weight() { return nativeWeight(nativeHandle); }
    private static native int nativeWeight(long handle);
    public void setWeight(int weight) { nativeSetWeight(nativeHandle, weight); }
    private static native void nativeSetWeight(long handle, int weight);

    /** Qt toString（family,size,...）。 */
    public String toString2() { return nativeToString(nativeHandle); }
    private static native String nativeToString(long handle);

    @Override
    public String toString() { return toString2(); }
}