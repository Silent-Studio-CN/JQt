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
 * 选项卡控件：封装 C++ 侧的 {@code QTabWidget}。
 * <p>信号槽：{@link #onCurrentChanged(Consumer)} — currentChanged 信号（参数为新页 index）。
 */
public class QTabWidget extends QWidget {

    private final List<Consumer<Integer>> onCurrentChangedHandlers = new ArrayList<>();

    public QTabWidget() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    private native long nativeCreate();

    /** 追加一页，返回其 index。 */
    public int addTab(QWidget widget, String title) {
        return nativeAddTab(nativeHandle, widget.nativeHandle, title);
    }
    private native int nativeAddTab(long handle, long childHandle, String title);

    /** 切换到指定页。 */
    public void setCurrentIndex(int index) {
        nativeSetCurrentIndex(nativeHandle, index);
    }
    private native void nativeSetCurrentIndex(long handle, int index);

    /** 当前页 index。 */
    public int currentIndex() {
        return nativeCurrentIndex(nativeHandle);
    }
    private native int nativeCurrentIndex(long handle);

    /** 修改页标题。 */
    public void setTabText(int index, String title) {
        nativeSetTabText(nativeHandle, index, title);
    }
    private native void nativeSetTabText(long handle, int index, String title);

    /** 注册页切换回调（参数为新页 index）。 */
    public QTabWidget onCurrentChanged(Consumer<Integer> handler) {
        onCurrentChangedHandlers.add(handler);
        return this;
    }

    /** 由 C++ 侧在页切换时回调（JNI）。 */
    void nativeHandleCurrentChanged(int index) {
        for (Consumer<Integer> h : onCurrentChangedHandlers) {
            h.accept(index);
        }
    }

    // ---- L1 补全（v0.6.0）----

    /** 清空全部页。 */
    public void clear() { nativeClear(nativeHandle); }
    private static native void nativeClear(long handle);

    /** 页数量。 */
    public int count() { return nativeCount(nativeHandle); }
    private static native int nativeCount(long handle);

    // ---- 值对象批：选项卡图标 ----

    /** 设置选项卡图标。 */
    public void setTabIcon(int index, QIcon icon) {
        if (icon == null || icon.isNull()) return;
        long pm = icon.pixmap().nativeHandle();
        if (pm != 0) nativeSetTabIcon(nativeHandle, index, pm);
    }
    private native void nativeSetTabIcon(long handle, int index, long pixmapHandle);

// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** documentMode（Qt documentMode）。 */
    public boolean documentMode() {
        return nativeDocumentMode(nativeHandle);
    }
    private static native boolean nativeDocumentMode(long nativeHandle);

    /** hasHeightForWidth（Qt hasHeightForWidth）。 */
    public boolean hasHeightForWidth() {
        return nativeHasHeightForWidth(nativeHandle);
    }
    private static native boolean nativeHasHeightForWidth(long nativeHandle);

    /** heightForWidth（Qt heightForWidth）。 */
    public int heightForWidth(int arg0) {
        return nativeHeightForWidth(nativeHandle, arg0);
    }
    private static native int nativeHeightForWidth(long nativeHandle, int arg0);

    /** isMovable（Qt isMovable）。 */
    public boolean isMovable() {
        return nativeIsMovable(nativeHandle);
    }
    private static native boolean nativeIsMovable(long nativeHandle);

    /** removeTab（Qt removeTab）。 */
    public void removeTab(int arg0) {
        nativeRemoveTab(nativeHandle, arg0);
    }
    private static native void nativeRemoveTab(long nativeHandle, int arg0);

    /** setDocumentMode（Qt setDocumentMode）。 */
    public void setDocumentMode(boolean arg0) {
        nativeSetDocumentMode(nativeHandle, arg0);
    }
    private static native void nativeSetDocumentMode(long nativeHandle, boolean arg0);

    /** setMovable（Qt setMovable）。 */
    public void setMovable(boolean arg0) {
        nativeSetMovable(nativeHandle, arg0);
    }
    private static native void nativeSetMovable(long nativeHandle, boolean arg0);

    /** setTabBarAutoHide（Qt setTabBarAutoHide）。 */
    public void setTabBarAutoHide(boolean arg0) {
        nativeSetTabBarAutoHide(nativeHandle, arg0);
    }
    private static native void nativeSetTabBarAutoHide(long nativeHandle, boolean arg0);

    /** setTabEnabled（Qt setTabEnabled）。 */
    public void setTabEnabled(int arg0, boolean arg1) {
        nativeSetTabEnabled(nativeHandle, arg0, arg1);
    }
    private static native void nativeSetTabEnabled(long nativeHandle, int arg0, boolean arg1);

    /** setTabVisible（Qt setTabVisible）。 */
    public void setTabVisible(int arg0, boolean arg1) {
        nativeSetTabVisible(nativeHandle, arg0, arg1);
    }
    private static native void nativeSetTabVisible(long nativeHandle, int arg0, boolean arg1);

    /** setUsesScrollButtons（Qt setUsesScrollButtons）。 */
    public void setUsesScrollButtons(boolean arg0) {
        nativeSetUsesScrollButtons(nativeHandle, arg0);
    }
    private static native void nativeSetUsesScrollButtons(long nativeHandle, boolean arg0);

    /** tabBarAutoHide（Qt tabBarAutoHide）。 */
    public boolean tabBarAutoHide() {
        return nativeTabBarAutoHide(nativeHandle);
    }
    private static native boolean nativeTabBarAutoHide(long nativeHandle);

    /** tabText（Qt tabText）。 */
    public String tabText(int arg0) {
        return nativeTabText(nativeHandle, arg0);
    }
    private static native String nativeTabText(long nativeHandle, int arg0);

    /** tabToolTip（Qt tabToolTip）。 */
    public String tabToolTip(int arg0) {
        return nativeTabToolTip(nativeHandle, arg0);
    }
    private static native String nativeTabToolTip(long nativeHandle, int arg0);

    /** tabWhatsThis（Qt tabWhatsThis）。 */
    public String tabWhatsThis(int arg0) {
        return nativeTabWhatsThis(nativeHandle, arg0);
    }
    private static native String nativeTabWhatsThis(long nativeHandle, int arg0);

    /** usesScrollButtons（Qt usesScrollButtons）。 */
    public boolean usesScrollButtons() {
        return nativeUsesScrollButtons(nativeHandle);
    }
    private static native boolean nativeUsesScrollButtons(long nativeHandle);

}