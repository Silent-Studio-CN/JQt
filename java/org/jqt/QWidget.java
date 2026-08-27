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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * JQt 所有控件的基类。
 * <p>
 * 每个控件内部持有 C++ 侧 Qt 对象的内存句柄（{@code nativeHandle}）。
 *
 * <h3>内存管理（Phase 5）</h3>
 * <ul>
 *   <li>句柄为自增 ID（非裸指针），由 C++ 注册表管理，永不复用；
 *       对已销毁对象调用方法会抛出 {@link IllegalStateException}，不会 native 崩溃。</li>
 *   <li>Java 对象不可达时，由 {@link Cleaner} 自动回收 C++ 对象（GUI 线程安全删除）。</li>
 *   <li>控件加入窗口/布局后，生命周期由 Qt 父子关系管理，Cleaner 不再干预。</li>
 *   <li>也可调用 {@link #dispose()} 手动提前释放。</li>
 * </ul>
 */
public abstract class QWidget {

    private static final Cleaner CLEANER = Cleaner.create();

    /** C++ 侧 Qt 对象的句柄 ID（自增、不复用），0 表示尚未创建或已释放。 */
    protected long nativeHandle;

    private volatile boolean disposed;

    /**
     * 注册清理器：对象不可达时由 Cleaner 线程调用 native 释放 C++ 对象。
     * 子类构造器在 {@code nativeHandle} 赋值后调用。
     */
    protected final void registerCleaner() {
        final long handle = nativeHandle;
        CLEANER.register(this, () -> nativeDispose(handle));
    }

    private static native void nativeDispose(long handle);

    /**
     * 设置控件级样式表（QSS），只影响本控件及其子控件。
     * 与 {@link QApplication#setStyleSheet(String)} 的全局样式可叠加（控件级优先）。
     */
    public void setStyleSheet(String qss) {
        nativeSetStyleSheet(nativeHandle, qss);
    }
    private static native void nativeSetStyleSheet(long handle, String qss);

    /**
     * 设置控件对象名（QSS 选择器 {@code #objectName} 用）。
     * 如 {@code button.setObjectName("titlebarClose")} 后，
     * QSS 中 {@code QPushButton#titlebarClose { ... }} 生效。
     */
    public void setObjectName(String name) {
        nativeSetObjectName(nativeHandle, name);
    }
    private static native void nativeSetObjectName(long handle, String name);

    /**
     * 给本控件设置布局管理器（任何控件都可用，如面板/卡片内部布局）。
     * 设置后由布局接管子控件排列。
     */
    public void setLayout(QLayout layout) {
        nativeSetLayout(nativeHandle, layout.nativeHandle());
    }
    private static native void nativeSetLayout(long handle, long layoutHandle);

    // ---- 动画（QPropertyAnimation）----

    /** 平滑移动到目标位置（属性动画，OutCubic 缓动）。 */
    public void animateMove(int x, int y, long ms) {
        nativeAnimateMove(nativeHandle, x, y, ms);
    }
    private static native void nativeAnimateMove(long handle, int x, int y, long ms);

    /** 平滑移动到目标位置，可指定缓动函数。 */
    public void animateMove(int x, int y, long ms, JQtEasing easing) {
        nativeAnimateMoveEasing(nativeHandle, x, y, ms, easing.qtType);
    }
    static native void nativeAnimateMoveEasing(long handle, int x, int y, long ms, int easing);

    /** 平滑缩放到目标尺寸。 */
    public void animateResize(int w, int h, long ms) {
        nativeAnimateResize(nativeHandle, w, h, ms);
    }
    private static native void nativeAnimateResize(long handle, int w, int h, long ms);

    /** 平滑缩放到目标尺寸，可指定缓动函数。 */
    public void animateResize(int w, int h, long ms, JQtEasing easing) {
        nativeAnimateResizeEasing(nativeHandle, w, h, ms, easing.qtType);
    }
    static native void nativeAnimateResizeEasing(long handle, int w, int h, long ms, int easing);

    /** 淡入（透明度 0 → 1，QGraphicsOpacityEffect）。 */
    public void fadeIn(long ms) {
        nativeFadeIn(nativeHandle, ms);
    }
    private static native void nativeFadeIn(long handle, long ms);

    /** 淡出（透明度 1 → 0）。 */
    public void fadeOut(long ms) {
        nativeFadeOut(nativeHandle, ms);
    }
    private static native void nativeFadeOut(long handle, long ms);

    // ---- 高级动画系统（JQtAnimation 使用；native 符号归属本类）----

    /** 创建属性动画并返回动画句柄（供 JQtAnimation 内部使用）。 */
    static native long nativeCreateAnimation(long handle, String property, double from, double to, long ms, int easing);

    /** 设置动画循环次数（-1 表示无限循环）。 */
    static native void nativeAnimationSetLoopCount(long animHandle, int loops);

    /** 启动动画（结束后自动销毁 C++ 对象）。 */
    static native void nativeAnimationStart(long animHandle);

    /** 停止动画（不销毁）。 */
    static native void nativeAnimationStop(long animHandle);

    /** 注册 Java 侧完成回调（JQtAnimation 构造时调用）。 */
    static native void nativeRegisterAnimation(long animHandle, JQtAnimation anim);

    /** 自动化命中测试：向窗口发送真实 WM_LBUTTONDOWN/UP 点击目标控件中心（诊断用）。 */
    static native void nativePostClickAt(long targetHandle, long winHandle);

    // ---- 阴影（QSS box-shadow 的替代：Qt QSS 不支持 box-shadow）----

    /** 给控件添加投影阴影（QGraphicsDropShadowEffect）。 */
    public void setDropShadow(int blurRadius, int alpha) {
        setDropShadow(blurRadius, alpha, 0, 2);
    }

    /** 给控件添加投影阴影（blur 模糊半径、alpha 透明度 0~255、dx/dy 偏移）。 */
    public void setDropShadow(int blurRadius, int alpha, int dx, int dy) {
        nativeSetDropShadow(nativeHandle, blurRadius, alpha, dx, dy);
    }
    static native void nativeSetDropShadow(long handle, int blur, int alpha, int dx, int dy);

    /** 移除投影阴影。 */
    public void clearDropShadow() {
        nativeClearDropShadow(nativeHandle);
    }
    static native void nativeClearDropShadow(long handle);

    /**
     * 自定义控件圆角（像素）。作用于 QSS 渲染的控件（QFrame/QPushButton/QLineEdit 等），
     * 与控件级 {@link #setStyleSheet(String)} 可共存（内部合并，互不覆盖）。
     * 自绘控件（JQtSwitch/JQtPivot）不受影响。
     */
    public void setBorderRadius(int radius) {
        nativeSetBorderRadius(nativeHandle, radius);
    }
    static native void nativeSetBorderRadius(long handle, int radius);

    // ---- 基础控件 API（几何 / 显隐 / 禁用 / 固定尺寸）----

    /** 控件当前宽度（像素）。 */
    public int width() { return nativeWidth(nativeHandle); }
    static native int nativeWidth(long handle);

    /** 控件当前高度（像素）。 */
    public int height() { return nativeHeight(nativeHandle); }
    static native int nativeHeight(long handle);

    /** 控件当前 x 坐标（相对父控件）。 */
    public int x() { return nativeX(nativeHandle); }
    static native int nativeX(long handle);

    /** 控件当前 y 坐标（相对父控件）。 */
    public int y() { return nativeY(nativeHandle); }
    static native int nativeY(long handle);

    /** 显示控件。 */
    public void show() { nativeShow(nativeHandle); }
    static native void nativeShow(long handle);

    /** 隐藏控件。 */
    public void hide() { nativeHide(nativeHandle); }
    static native void nativeHide(long handle);

    /** 控件是否可见（含父链）。 */
    public boolean isVisible() { return nativeIsVisible(nativeHandle); }
    static native boolean nativeIsVisible(long handle);

    /** 启用 / 禁用控件（禁用后不响应输入）。 */
    public void setEnabled(boolean enabled) { nativeSetEnabled(nativeHandle, enabled); }
    static native void nativeSetEnabled(long handle, boolean enabled);

    /** 控件是否启用。 */
    public boolean isEnabled() { return nativeIsEnabled(nativeHandle); }
    static native boolean nativeIsEnabled(long handle);

    /** 固定控件尺寸（布局中不再拉伸）。 */
    public void setFixedSize(int width, int height) { nativeSetFixedSize(nativeHandle, width, height); }
    static native void nativeSetFixedSize(long handle, int width, int height);

    // ---- L1 基础 API（v0.6.0 补全：几何 / 显隐 / 状态 / 属性）----

    /** 关闭控件（若为窗口则触发关闭流程；最后一个窗口关闭后 exec() 返回）。 */
    public void close() { nativeClose(nativeHandle); }
    static native void nativeClose(long handle);

    /** 移动到指定位置（相对父控件）。 */
    public void move(int x, int y) { nativeMove(nativeHandle, x, y); }
    static native void nativeMove(long handle, int x, int y);

    /** 调整控件尺寸。 */
    public void resize(int width, int height) { nativeResize(nativeHandle, width, height); }
    static native void nativeResize(long handle, int width, int height);

    /** 请求重绘（异步合并）。 */
    public void update() { nativeUpdate(nativeHandle); }
    static native void nativeUpdate(long handle);

    /** 立即重绘。 */
    public void repaint() { nativeRepaint(nativeHandle); }
    static native void nativeRepaint(long handle);

    /** 控件尺寸 [宽, 高]。 */
    public int[] size() { return nativeSize(nativeHandle); }
    static native int[] nativeSize(long handle);

    /** 控件几何 [x, y, w, h]（相对父控件）。 */
    public int[] geometry() { return nativeGeometry(nativeHandle); }
    static native int[] nativeGeometry(long handle);

    /** 控件位置 [x, y]（相对父控件）。 */
    public int[] pos() { return nativePos(nativeHandle); }
    static native int[] nativePos(long handle);

    /** 内容边距 [左, 上, 右, 下]。 */
    public int[] contentsMargins() { return nativeContentsMargins(nativeHandle); }
    static native int[] nativeContentsMargins(long handle);

    /** 当前控件级样式表（QSS；无则返回空串）。 */
    public String styleSheet() { return nativeStyleSheet(nativeHandle); }
    static native String nativeStyleSheet(long handle);

    /** 设置悬停提示。 */
    public void setToolTip(String tip) { nativeSetToolTip(nativeHandle, tip); }
    static native void nativeSetToolTip(long handle, String tip);

    /** 悬停提示文本。 */
    public String toolTip() { return nativeToolTip(nativeHandle); }
    static native String nativeToolTip(long handle);

    /** 设置窗口标题（触发 onWindowTitleChanged）。 */
    public void setWindowTitle(String title) { nativeSetWindowTitle(nativeHandle, title); }
    static native void nativeSetWindowTitle(long handle, String title);

    /** 窗口标题。 */
    public String windowTitle() { return nativeWindowTitle(nativeHandle); }
    static native String nativeWindowTitle(long handle);

    /**
     * 设置窗口状态（Qt::WindowState 位）：0 正常 / 1 最小化 / 2 最大化 / 4 全屏，可组合。
     */
    public void setWindowState(int state) { nativeSetWindowState(nativeHandle, state); }
    static native void nativeSetWindowState(long handle, int state);

    /** 当前窗口状态（位值）。 */
    public int windowState() { return nativeWindowState(nativeHandle); }
    static native int nativeWindowState(long handle);

    /**
     * 设置焦点策略：0 NoFocus / 1 TabFocus / 2 ClickFocus / 4 StrongFocus / 8 WheelFocus。
     */
    public void setFocusPolicy(int policy) { nativeSetFocusPolicy(nativeHandle, policy); }
    static native void nativeSetFocusPolicy(long handle, int policy);

    /** 当前焦点策略。 */
    public int focusPolicy() { return nativeFocusPolicy(nativeHandle); }
    static native int nativeFocusPolicy(long handle);

    /** 是否接受拖放。 */
    public boolean acceptDrops() { return nativeAcceptDrops(nativeHandle); }
    static native boolean nativeAcceptDrops(long handle);

    /** 设置是否接受拖放。 */
    public void setAcceptDrops(boolean on) { nativeSetAcceptDrops(nativeHandle, on); }
    static native void nativeSetAcceptDrops(long handle, boolean on);

    /**
     * 设置鼠标形状：arrow / ibeam / wait / crosshair / pointinghand / forbidden /
     * sizeall / sizefdiag / sizebdiag / sizewe / sizens / splitv / splith / openhand / closedhand。
     */
    public void setCursor(String shape) { nativeSetCursor(nativeHandle, shape); }
    static native void nativeSetCursor(long handle, String shape);

    /** 当前鼠标形状名。 */
    public String cursor() { return nativeCursor(nativeHandle); }
    static native String nativeCursor(long handle);

    /** 设置控件字体（"Family,size" 中取 Family）。 */
    public void setFont(String family, int pointSize) { nativeSetFont(nativeHandle, family, pointSize); }
    static native void nativeSetFont(long handle, String family, int pointSize);

    /** 控件字体，返回 "Family,size"（未单独设置时为默认字体）。 */
    public String font() { return nativeFont(nativeHandle); }
    static native String nativeFont(long handle);

    /** 是否带投影阴影（setDropShadow 设置过）。 */
    public boolean graphicsEffect() { return nativeGraphicsEffect(nativeHandle); }
    static native boolean nativeGraphicsEffect(long handle);

    /**
     * 尺寸策略（水平, 垂直）：0 Fixed / 1 Minimum / 2 Maximum / 3 Preferred /
     * 4 MinimumExpanding / 5 Expanding / 6 Ignored。
     */
    public void setSizePolicy(int horizontal, int vertical) { nativeSetSizePolicy(nativeHandle, horizontal, vertical); }
    static native void nativeSetSizePolicy(long handle, int horizontal, int vertical);

    /** 尺寸策略 [水平, 垂直]。 */
    public int[] sizePolicy() { return nativeSizePolicy(nativeHandle); }
    static native int[] nativeSizePolicy(long handle);


    /**
     * 手动释放 C++ 侧对象（通常无需调用——GC 时会自动释放）。
     * 释放后再次调用本控件任何方法将抛出 {@link IllegalStateException}。
     */
    public final void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        final long handle = nativeHandle;
        nativeHandle = 0;
        nativeDispose(handle);
    }

    /** 是否已释放（调用过 {@link #dispose()} 或对象已不可达）。 */
    public final boolean isDisposed() {
        return disposed;
    }

    /** 控件是否已在 C++ 侧创建且未释放。 */
    public boolean isCreated() {
        return nativeHandle != 0 && !disposed;
    }

    // ---- 信号（L1：windowTitleChanged / customContextMenuRequested）----

    private final List<Consumer<String>> onWindowTitleChangedHandlers = new ArrayList<>();
    private final List<BiConsumer<Integer, Integer>> onCustomContextMenuRequestedHandlers = new ArrayList<>();
    private volatile boolean windowTitleConnected;
    private volatile boolean contextMenuConnected;

    /** 窗口标题变化回调（参数为新标题）。 */
    public QWidget onWindowTitleChanged(Consumer<String> handler) {
        onWindowTitleChangedHandlers.add(handler);
        if (!windowTitleConnected) {
            windowTitleConnected = true;
            nativeConnectWindowTitleChanged(nativeHandle);
        }
        return this;
    }

    /** 右键菜单请求回调（参数为请求位置 x, y，相对本控件）。 */
    public QWidget onCustomContextMenuRequested(BiConsumer<Integer, Integer> handler) {
        onCustomContextMenuRequestedHandlers.add(handler);
        if (!contextMenuConnected) {
            contextMenuConnected = true;
            nativeConnectContextMenu(nativeHandle);
        }
        return this;
    }

    private native void nativeConnectWindowTitleChanged(long handle);
    private native void nativeConnectContextMenu(long handle);

    /** 由 C++ 侧在窗口标题变化时回调（JNI）。 */
    void nativeHandleWindowTitleChanged(String title) {
        for (Consumer<String> h : onWindowTitleChangedHandlers) {
            h.accept(title);
        }
    }

    /** 由 C++ 侧在右键菜单请求时回调（JNI）。 */
    void nativeHandleCustomContextMenuRequested(int x, int y) {
        for (BiConsumer<Integer, Integer> h : onCustomContextMenuRequestedHandlers) {
            h.accept(x, y);
        }
    }

    /** C++ 侧句柄 ID（仅供内部 / 高级用法）。 */
    public long nativeHandle() {
        return nativeHandle;
    }
}



