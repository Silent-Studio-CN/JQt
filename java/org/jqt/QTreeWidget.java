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
 * 树控件：封装 C++ 侧的 {@code QTreeWidget}。
 * <p>
 * 节点以 int itemId 标识（addTopLevelItem/addChild 返回）。
 * 信号槽：{@link #onItemClicked(Consumer)} — itemClicked 信号（参数为 itemId）。
 */
public class QTreeWidget extends QWidget {

    private final List<Consumer<Integer>> onItemClickedHandlers = new ArrayList<>();
    private int nextItemId = 1;

    public QTreeWidget() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    private native long nativeCreate();

    /** 追加顶级节点，返回其 itemId。 */
    public int addTopLevelItem(String text) {
        return nativeAddTopLevelItem(nativeHandle, nextItemId++, text);
    }
    private native int nativeAddTopLevelItem(long handle, int itemId, String text);

    /** 在指定父节点下追加子节点，返回其 itemId。 */
    public int addChild(int parentItemId, String text) {
        return nativeAddChild(nativeHandle, parentItemId, nextItemId++, text);
    }
    private native int nativeAddChild(long handle, int parentItemId, int itemId, String text);

    /** 读取节点文本。 */
    public String itemText(int itemId) {
        return nativeItemText(nativeHandle, itemId);
    }
    private native String nativeItemText(long handle, int itemId);

    /** 设置节点文本。 */
    public void setItemText(int itemId, String text) {
        nativeSetItemText(nativeHandle, itemId, text);
    }
    private native void nativeSetItemText(long handle, int itemId, String text);

    /** 展开全部节点。 */
    public void expandAll() {
        nativeExpandAll(nativeHandle);
    }
    private native void nativeExpandAll(long handle);

    /** 折叠全部节点。 */
    public void collapseAll() {
        nativeCollapseAll(nativeHandle);
    }
    private native void nativeCollapseAll(long handle);

    /** 清空全部节点。 */
    public void clear() {
        nativeClear(nativeHandle);
    }
    private native void nativeClear(long handle);

    /** 注册节点点击回调（itemClicked 信号，参数为 itemId）。 */
    public QTreeWidget onItemClicked(Consumer<Integer> handler) {
        onItemClickedHandlers.add(handler);
        return this;
    }

    /** 由 C++ 侧在点击节点时回调（JNI）。 */
    void nativeHandleItemClicked(int itemId) {
        for (Consumer<Integer> h : onItemClickedHandlers) {
            h.accept(itemId);
        }
    }

    // ---- L1 补全（v0.6.0）----

    /** 当前选中节点 itemId（无选中返回 -1）。 */
    public int currentItem() { return nativeCurrentItem(nativeHandle); }
    private static native int nativeCurrentItem(long handle);

    /** 选中指定节点（itemId）。 */
    public void setCurrentItem(int itemId) { nativeSetCurrentItem(nativeHandle, itemId); }
    private static native void nativeSetCurrentItem(long handle, int itemId);

    private final List<Consumer<Integer>> onCurrentItemChangedHandlers = new ArrayList<>();
    private final List<Consumer<Integer>> onItemDoubleClickedHandlers = new ArrayList<>();
    private final List<Consumer<Integer>> onItemActivatedHandlers = new ArrayList<>();
    private volatile boolean curConn, dblConn, actConn;

    /** 当前节点切换回调（参数为新节点 itemId）。 */
    public QTreeWidget onCurrentItemChanged(Consumer<Integer> handler) {
        onCurrentItemChangedHandlers.add(handler);
        if (!curConn) { curConn = true; nativeConnectCurrentItemChanged(nativeHandle); }
        return this;
    }

    /** 节点双击回调（参数为 itemId）。 */
    public QTreeWidget onItemDoubleClicked(Consumer<Integer> handler) {
        onItemDoubleClickedHandlers.add(handler);
        if (!dblConn) { dblConn = true; nativeConnectItemDoubleClicked(nativeHandle); }
        return this;
    }

    /** 节点激活回调（双击/回车，参数为 itemId）。 */
    public QTreeWidget onItemActivated(Consumer<Integer> handler) {
        onItemActivatedHandlers.add(handler);
        if (!actConn) { actConn = true; nativeConnectItemActivated(nativeHandle); }
        return this;
    }

    private native void nativeConnectCurrentItemChanged(long handle);
    private native void nativeConnectItemDoubleClicked(long handle);
    private native void nativeConnectItemActivated(long handle);

    void nativeHandleCurrentItemChanged(int itemId) {
        for (Consumer<Integer> h : onCurrentItemChangedHandlers) h.accept(itemId);
    }
    void nativeHandleItemDoubleClicked(int itemId) {
        for (Consumer<Integer> h : onItemDoubleClickedHandlers) h.accept(itemId);
    }
    void nativeHandleItemActivated(int itemId) {
        for (Consumer<Integer> h : onItemActivatedHandlers) h.accept(itemId);
    }
}