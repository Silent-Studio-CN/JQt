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
 * 弹出菜单：封装 C++ 侧的 {@code QMenu}。
 * <pre>
 * QMenu menu = new QMenu();
 * int openId = menu.addItem("打开");
 * int quitId = menu.addItem("退出");
 * menu.onTriggered(id -> { if (id == quitId) app.quit(); });
 * menu.popup(button);   // 在按钮下方弹出
 * </pre>
 * <p>信号槽：{@link #onTriggered(Consumer)} — triggered 信号（参数为 actionId）。
 */
public class QMenu extends QWidget {

    private final List<Consumer<Integer>> onTriggeredHandlers = new ArrayList<>();
    private int nextActionId = 1;

    public QMenu() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    private native long nativeCreate();

    /** 追加菜单项，返回其 actionId。 */
    public int addItem(String text) {
        return nativeAddItem(nativeHandle, nextActionId++, text);
    }
    private native int nativeAddItem(long handle, int actionId, String text);

    /** 在指定坐标（屏幕坐标）弹出菜单。 */
    public void popup(int x, int y) {
        nativePopupAt(nativeHandle, x, y);
    }
    private native void nativePopupAt(long handle, int x, int y);

    /** 在锚点控件下方弹出菜单。 */
    public void popup(QWidget anchor) {
        nativePopupAnchor(nativeHandle, anchor.nativeHandle);
    }
    private native void nativePopupAnchor(long handle, long anchorHandle);

    /** 菜单项触发回调（参数为 actionId）。 */
    public QMenu onTriggered(Consumer<Integer> handler) {
        onTriggeredHandlers.add(handler);
        return this;
    }

    /** 由 C++ 侧在菜单项被触发时回调（JNI）。 */
    void nativeHandleTriggered(int actionId) {
        for (Consumer<Integer> h : onTriggeredHandlers) {
            h.accept(actionId);
        }
    }

    // ---- L1 补全（v0.6.0）----

    /** 清空全部菜单项。 */
    public void clear() { nativeClear(nativeHandle); }
    private static native void nativeClear(long handle);

    /** 菜单标题。 */
    public String title() { return nativeTitle(nativeHandle); }
    private static native String nativeTitle(long handle);

    /** 设置菜单图标（图片文件路径；QIcon 以路径字符串简化传递）。 */
    public void setIcon(String iconPath) {
        nativeSetIcon(nativeHandle, iconPath);
    }
    private static native void nativeSetIcon(long handle, String iconPath);

    /** 当前菜单图标路径（未设置返回 null）。 */
    public String icon() { return nativeIcon(nativeHandle); }
    private static native String nativeIcon(long handle);

    /** 设置菜单标题。 */
    public void setTitle(String title) { nativeSetTitle(nativeHandle, title); }
    private static native void nativeSetTitle(long handle, String title);

    /** 模态弹出菜单（阻塞直到选择/关闭），返回选中项 actionId（取消返回 -1）。 */
    public int exec(int x, int y) { return nativeExec(nativeHandle, x, y); }
    private static native int nativeExec(long handle, int x, int y);

    /** 模态弹出菜单（在锚点控件下方），返回选中项 actionId（取消返回 -1）。 */
    public int exec(QWidget anchor) { return nativeExecAnchor(nativeHandle, anchor.nativeHandle); }
    private static native int nativeExecAnchor(long handle, long anchorHandle);

// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** isEmpty（Qt isEmpty）。 */
    public boolean isEmpty() {
        return nativeIsEmpty(nativeHandle);
    }
    private static native boolean nativeIsEmpty(long nativeHandle);

    /** isTearOffMenuVisible（Qt isTearOffMenuVisible）。 */
    public boolean isTearOffMenuVisible() {
        return nativeIsTearOffMenuVisible(nativeHandle);
    }
    private static native boolean nativeIsTearOffMenuVisible(long nativeHandle);

    /** separatorsCollapsible（Qt separatorsCollapsible）。 */
    public boolean separatorsCollapsible() {
        return nativeSeparatorsCollapsible(nativeHandle);
    }
    private static native boolean nativeSeparatorsCollapsible(long nativeHandle);

    /** setSeparatorsCollapsible（Qt setSeparatorsCollapsible）。 */
    public void setSeparatorsCollapsible(boolean arg0) {
        nativeSetSeparatorsCollapsible(nativeHandle, arg0);
    }
    private static native void nativeSetSeparatorsCollapsible(long nativeHandle, boolean arg0);

    /** setTearOffEnabled（Qt setTearOffEnabled）。 */
    public void setTearOffEnabled(boolean arg0) {
        nativeSetTearOffEnabled(nativeHandle, arg0);
    }
    private static native void nativeSetTearOffEnabled(long nativeHandle, boolean arg0);

    /** setToolTipsVisible（Qt setToolTipsVisible）。 */
    public void setToolTipsVisible(boolean arg0) {
        nativeSetToolTipsVisible(nativeHandle, arg0);
    }
    private static native void nativeSetToolTipsVisible(long nativeHandle, boolean arg0);

    /** showTearOffMenu（Qt showTearOffMenu）。 */
    public void showTearOffMenu() {
        nativeShowTearOffMenu(nativeHandle);
    }
    private static native void nativeShowTearOffMenu(long nativeHandle);

    /** toolTipsVisible（Qt toolTipsVisible）。 */
    public boolean toolTipsVisible() {
        return nativeToolTipsVisible(nativeHandle);
    }
    private static native boolean nativeToolTipsVisible(long nativeHandle);

}