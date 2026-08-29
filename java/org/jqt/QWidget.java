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

    /** 控件背景色（0xAARRGGBB；简化形态的 palette 查询）。 */
    public int palette() { return nativePalette(nativeHandle); }
    static native int nativePalette(long handle);

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

    // ---- L1 补全（v0.8.0）：find / layout / windowIcon ----

    /**
     * 按原生窗口句柄查找控件（QWidget::find 静态版）。
     * 返回控件句柄（可配合 nativeHandle() 使用）；未找到返回 0。
     */
    public static long find(long winId) {
        return nativeFind(winId);
    }
    private static native long nativeFind(long winId);

    /**
     * 当前布局管理器句柄（QWidget::layout 简化版）。
     * 返回 C++ 侧 QLayout 句柄（可与 QLayout 对象互操作）；未设置返回 0。
     */
    public long layout() {
        return nativeLayout(nativeHandle);
    }
    private native long nativeLayout(long handle);

    /** 设置窗口图标（图片文件路径；QWidget::setWindowIcon 以路径简化传递）。 */
    public void setWindowIcon(String iconPath) {
        nativeSetWindowIcon(nativeHandle, iconPath);
    }
    private native void nativeSetWindowIcon(long handle, String iconPath);

    // ---- v0.7.2 工业模块：打印 ----

    /**
     * 将控件渲染导出为 PDF（QWidget::render 到 PDF 打印机）。
     * @param path 输出 PDF 路径
     * @return 是否成功
     */
    public boolean printToPdf(String path) {
        return nativePrintToPdf(nativeHandle, path);
    }
    private native boolean nativePrintToPdf(long handle, String path);

    // ---- L2 批次（v0.7.4：QWidget 高频少用 API）----

    /** 最小尺寸（QWidget::setMinimumSize）。 */
    public void setMinimumSize(int w, int h) { nativeSetMinimumSize(nativeHandle, w, h); }
    private native void nativeSetMinimumSize(long handle, int w, int h);

    /** 最大尺寸（QWidget::setMaximumSize）。 */
    public void setMaximumSize(int w, int h) { nativeSetMaximumSize(nativeHandle, w, h); }
    private native void nativeSetMaximumSize(long handle, int w, int h);

    /** 最小尺寸，返回高 32 位宽、低 32 位高的打包值（-1 表示未设置）。 */
    public long minimumSize() { return nativeMinimumSize(nativeHandle); }
    private native long nativeMinimumSize(long handle);

    /** 最大尺寸，返回高 32 位宽、低 32 位高的打包值（-1 表示未设置）。 */
    public long maximumSize() { return nativeMaximumSize(nativeHandle); }
    private native long nativeMaximumSize(long handle);

    /** 固定宽度（高度自适应；QWidget::setFixedWidth）。 */
    public void setFixedWidth(int w) { nativeSetFixedWidth(nativeHandle, w); }
    private native void nativeSetFixedWidth(long handle, int w);

    /** 固定高度（宽度自适应；QWidget::setFixedHeight）。 */
    public void setFixedHeight(int h) { nativeSetFixedHeight(nativeHandle, h); }
    private native void nativeSetFixedHeight(long handle, int h);

    /** 是否有焦点（QWidget::hasFocus）。 */
    public boolean hasFocus() { return nativeHasFocus(nativeHandle); }
    private native boolean nativeHasFocus(long handle);

    /** 获取键盘焦点（QWidget::setFocus）。 */
    public void setFocus() { nativeSetFocus(nativeHandle); }
    private native void nativeSetFocus(long handle);

    /** 清除焦点（QWidget::clearFocus）。 */
    public void clearFocus() { nativeClearFocus(nativeHandle); }
    private native void nativeClearFocus(long handle);

    /** 鼠标跟踪（QWidget::setMouseTracking；开启后无需按键即可收到 move 事件）。 */
    public void setMouseTracking(boolean on) { nativeSetMouseTracking(nativeHandle, on); }
    private native void nativeSetMouseTracking(long handle, boolean on);

    /** 是否启用鼠标跟踪。 */
    public boolean hasMouseTracking() { return nativeHasMouseTracking(nativeHandle); }
    private native boolean nativeHasMouseTracking(long handle);

    /** 是否为活动窗口（QWidget::isActiveWindow）。 */
    public boolean isActiveWindow() { return nativeIsActiveWindow(nativeHandle); }
    private native boolean nativeIsActiveWindow(long handle);

    /** 激活窗口（QWidget::activateWindow）。 */
    public void activateWindow() { nativeActivateWindow(nativeHandle); }
    private native void nativeActivateWindow(long handle);

    /** 置顶（QWidget::raise）。 */
    public void raise() { nativeRaise(nativeHandle); }
    private native void nativeRaise(long handle);

    /** 置底（QWidget::lower）。 */
    public void lower() { nativeLower(nativeHandle); }
    private native void nativeLower(long handle);

    /** 窗口透明度（0.0-1.0；QWidget::setWindowOpacity）。 */
    public void setWindowOpacity(double opacity) { nativeSetWindowOpacity(nativeHandle, opacity); }
    private native void nativeSetWindowOpacity(long handle, double opacity);

    /** 窗口透明度。 */
    public double windowOpacity() { return nativeWindowOpacity(nativeHandle); }
    private native double nativeWindowOpacity(long handle);

    /** 是否全屏（QWidget::isFullScreen）。 */
    public boolean isFullScreen() { return nativeIsFullScreen(nativeHandle); }
    private native boolean nativeIsFullScreen(long handle);

    /** 是否最小化（QWidget::isMinimized）。 */
    public boolean isMinimized() { return nativeIsMinimized(nativeHandle); }
    private native boolean nativeIsMinimized(long handle);

    /** 自动填充背景（QWidget::setAutoFillBackground）。 */
    public void setAutoFillBackground(boolean on) { nativeSetAutoFillBackground(nativeHandle, on); }
    private native void nativeSetAutoFillBackground(long handle, boolean on);

    /** 是否自动填充背景。 */
    public boolean autoFillBackground() { return nativeAutoFillBackground(nativeHandle); }
    private native boolean nativeAutoFillBackground(long handle);

    /** 独占键盘（QWidget::grabKeyboard）。 */
    public void grabKeyboard() { nativeGrabKeyboard(nativeHandle); }
    private native void nativeGrabKeyboard(long handle);

    /** 释放键盘独占（QWidget::releaseKeyboard）。 */
    public void releaseKeyboard() { nativeReleaseKeyboard(nativeHandle); }
    private native void nativeReleaseKeyboard(long handle);

    // ---- 值对象批（手写精修：几何/抓取/光标/字体/调色板/坐标映射） ----

    /** 设置几何（位置+尺寸）。 */
    public void setGeometry(int x, int y, int w, int h) { nativeSetGeometry(nativeHandle, x, y, w, h); }
    private native void nativeSetGeometry(long handle, int x, int y, int w, int h);

    /** 设置几何。 */
    public void setGeometry(QRect rect) {
        if (rect != null) setGeometry(rect.x(), rect.y(), rect.width(), rect.height());
    }

    /** 抓取控件渲染为像素图。 */
    public QPixmap grab() {
        long h = nativeGrab(nativeHandle);
        return h != 0 ? new QPixmap(h) : new QPixmap();
    }
    private native long nativeGrab(long handle);

    /** 抓取指定区域渲染为像素图。 */
    public QPixmap grab(QRect rect) {
        if (rect == null) return grab();
        long h = nativeGrabRect(nativeHandle, rect.x(), rect.y(), rect.width(), rect.height());
        return h != 0 ? new QPixmap(h) : new QPixmap();
    }
    private native long nativeGrabRect(long handle, int x, int y, int w, int h);

    /** 独占鼠标（全部事件发送到本控件）。 */
    public void grabMouse() { nativeGrabMouse(nativeHandle, 0); }
    private native void nativeGrabMouse(long handle, int cursorShape);

    /** 独占鼠标并设置光标形状。 */
    public void grabMouse(QCursor cursor) {
        if (cursor != null) nativeGrabMouse(nativeHandle, cursor.shape().value);
    }

    /** 释放鼠标独占。 */
    public void releaseMouse() { nativeReleaseMouse(nativeHandle); }
    private native void nativeReleaseMouse(long handle);

    /** 抓取快捷键，返回 id（context：0=WindowShortcut 1=WidgetShortcut 2=ApplicationShortcut）。 */
    public int grabShortcut(QKeySequence sequence, int context) {
        if (sequence == null) return 0;
        return nativeGrabShortcut(nativeHandle, sequence.toString(), context);
    }
    private native int nativeGrabShortcut(long handle, String sequence, int context);

    /** 释放快捷键。 */
    public void releaseShortcut(int id) { nativeReleaseShortcut(nativeHandle, id); }
    private native void nativeReleaseShortcut(long handle, int id);

    /** 设置裁剪区域。 */
    public void setMask(QRegion region) {
        if (region == null || region.isEmpty()) { clearMask(); return; }
        nativeSetMaskRect(nativeHandle, region.boundingRect().x(), region.boundingRect().y(),
                          region.boundingRect().width(), region.boundingRect().height());
    }
    private native void nativeSetMaskRect(long handle, int x, int y, int w, int h);

    /** 清除裁剪区域。 */
    public void clearMask() { nativeClearMask(nativeHandle); }
    private native void nativeClearMask(long handle);

    /** 设置光标（值对象重载）。 */
    public void setCursor(QCursor cursor) {
        if (cursor == null) return;
        nativeSetCursorShape(nativeHandle, cursor.shape().value);
    }
    private native void nativeSetCursorShape(long handle, int shape);

    /** 设置字体（值对象重载）。 */
    public void setFont(QFont font) {
        if (font == null) return;
        nativeSetFontQFont(nativeHandle, font.family(), font.pointSize(), font.weight(), font.italic());
    }
    private native void nativeSetFontQFont(long handle, String family, int pointSize, int weight, boolean italic);

    /** 设置调色板（8 个角色色：Window/WindowText/Base/Text/Button/ButtonText/Highlight/HighlightedText）。 */
    public void setPalette(QPalette palette) {
        if (palette == null) return;
        nativeSetPalette(nativeHandle,
            palette.color(QPalette.ColorRole.Window).rgba(),
            palette.color(QPalette.ColorRole.WindowText).rgba(),
            palette.color(QPalette.ColorRole.Base).rgba(),
            palette.color(QPalette.ColorRole.Text).rgba(),
            palette.color(QPalette.ColorRole.Button).rgba(),
            palette.color(QPalette.ColorRole.ButtonText).rgba(),
            palette.color(QPalette.ColorRole.Highlight).rgba(),
            palette.color(QPalette.ColorRole.HighlightedText).rgba());
    }
    private native void nativeSetPalette(long handle, int window, int windowText, int base, int text,
                                         int button, int buttonText, int highlight, int highlightedText);

    /** 保存几何（QByteArray，配合 restoreGeometry）。 */
    public QByteArray saveGeometry() { return new QByteArray(nativeSaveGeometry(nativeHandle)); }
    private native byte[] nativeSaveGeometry(long handle);

    /** 恢复几何。 */
    public boolean restoreGeometry(QByteArray geometry) {
        if (geometry == null) return false;
        return nativeRestoreGeometry(nativeHandle, geometry.data());
    }
    private native boolean nativeRestoreGeometry(long handle, byte[] data);

    /** 坐标映射到全局。 */
    public QPoint mapToGlobal(QPoint p) {
        int[] r = nativeMapToGlobal(nativeHandle, p.x(), p.y());
        return new QPoint(r[0], r[1]);
    }
    private native int[] nativeMapToGlobal(long handle, int x, int y);

    /** 全局坐标映射到本控件。 */
    public QPoint mapFromGlobal(QPoint p) {
        int[] r = nativeMapFromGlobal(nativeHandle, p.x(), p.y());
        return new QPoint(r[0], r[1]);
    }
    private native int[] nativeMapFromGlobal(long handle, int x, int y);

    /** 坐标映射到另一控件。 */
    public QPoint mapTo(QWidget other, QPoint p) {
        int[] r = nativeMapTo(nativeHandle, other != null ? other.nativeHandle : 0, p.x(), p.y());
        return new QPoint(r[0], r[1]);
    }
    private native int[] nativeMapTo(long handle, long otherHandle, int x, int y);

    /** 另一控件坐标映射到本控件。 */
    public QPoint mapFrom(QWidget other, QPoint p) {
        int[] r = nativeMapFrom(nativeHandle, other != null ? other.nativeHandle : 0, p.x(), p.y());
        return new QPoint(r[0], r[1]);
    }
    private native int[] nativeMapFrom(long handle, long otherHandle, int x, int y);

    /** 设置内容边距。 */
    public void setContentsMargins(int left, int top, int right, int bottom) {
        nativeSetContentsMargins(nativeHandle, left, top, right, bottom);
    }
    private native void nativeSetContentsMargins(long handle, int left, int top, int right, int bottom);

    /** 设置内容边距。 */
    public void setContentsMargins(QMargins margins) {
        if (margins != null) setContentsMargins(margins.left(), margins.top(), margins.right(), margins.bottom());
    }



    // ---- 值对象批：窗口图标（QIcon） ----

    /** 设置窗口图标（值对象版）。 */
    public void setWindowIcon(QIcon icon) {
        if (icon == null || icon.isNull()) return;
        long pm = icon.pixmap().nativeHandle();
        if (pm != 0) nativeSetWindowIconPixmap(nativeHandle, pm);
    }
    private native void nativeSetWindowIconPixmap(long handle, long pixmapHandle);

    /** 窗口图标变化回调（windowIconChanged 信号，参数为 QIcon）。 */
    public QWidget onWindowIconChanged(java.util.function.Consumer<QIcon> handler) {
        windowIconChangedHandlers.add(handler);
        if (!windowIconChangedConnected) {
            windowIconChangedConnected = true;
            nativeConnectWindowIconChanged(nativeHandle);
        }
        return this;
    }
    private final java.util.List<java.util.function.Consumer<QIcon>> windowIconChangedHandlers = new java.util.ArrayList<>();
    private boolean windowIconChangedConnected;
    private native void nativeConnectWindowIconChanged(long handle);

    void nativeHandleWindowIconChanged(long pixmapHandle) {
        QIcon icon = pixmapHandle != 0 ? new QIcon(new QPixmap(pixmapHandle)) : new QIcon();
        for (java.util.function.Consumer<QIcon> h : windowIconChangedHandlers) h.accept(icon);
    }

    
    // ---- 生成器批次（jqt-gen 自动生成，直传型） ----

    /** accessibleDescription（Qt accessibleDescription）。 */
    public String accessibleDescription() {
        return nativeAccessibleDescription(nativeHandle);
    }
    private static native String nativeAccessibleDescription(long nativeHandle);

    /** accessibleIdentifier（Qt accessibleIdentifier）。 */
    public String accessibleIdentifier() {
        return nativeAccessibleIdentifier(nativeHandle);
    }
    private static native String nativeAccessibleIdentifier(long nativeHandle);

    /** accessibleName（Qt accessibleName）。 */
    public String accessibleName() {
        return nativeAccessibleName(nativeHandle);
    }
    private static native String nativeAccessibleName(long nativeHandle);

    /** adjustSize（Qt adjustSize）。 */
    public void adjustSize() {
        nativeAdjustSize(nativeHandle);
    }
    private static native void nativeAdjustSize(long nativeHandle);

    /** ensurePolished（Qt ensurePolished）。 */
    public void ensurePolished() {
        nativeEnsurePolished(nativeHandle);
    }
    private static native void nativeEnsurePolished(long nativeHandle);

    /** hasHeightForWidth（Qt hasHeightForWidth）。 */
    public boolean hasHeightForWidth() {
        return nativeHasHeightForWidth(nativeHandle);
    }
    private static native boolean nativeHasHeightForWidth(long nativeHandle);

    /** hasTabletTracking（Qt hasTabletTracking）。 */
    public boolean hasTabletTracking() {
        return nativeHasTabletTracking(nativeHandle);
    }
    private static native boolean nativeHasTabletTracking(long nativeHandle);

    /** heightForWidth（Qt heightForWidth）。 */
    public int heightForWidth(int arg0) {
        return nativeHeightForWidth(nativeHandle, arg0);
    }
    private static native int nativeHeightForWidth(long nativeHandle, int arg0);

    /** isMaximized（Qt isMaximized）。 */
    public boolean isMaximized() {
        return nativeIsMaximized(nativeHandle);
    }
    private static native boolean nativeIsMaximized(long nativeHandle);

    /** isModal（Qt isModal）。 */
    public boolean isModal() {
        return nativeIsModal(nativeHandle);
    }
    private static native boolean nativeIsModal(long nativeHandle);

    /** isWindowModified（Qt isWindowModified）。 */
    public boolean isWindowModified() {
        return nativeIsWindowModified(nativeHandle);
    }
    private static native boolean nativeIsWindowModified(long nativeHandle);

    /** maximumHeight（Qt maximumHeight）。 */
    public int maximumHeight() {
        return nativeMaximumHeight(nativeHandle);
    }
    private static native int nativeMaximumHeight(long nativeHandle);

    /** maximumWidth（Qt maximumWidth）。 */
    public int maximumWidth() {
        return nativeMaximumWidth(nativeHandle);
    }
    private static native int nativeMaximumWidth(long nativeHandle);

    /** minimumHeight（Qt minimumHeight）。 */
    public int minimumHeight() {
        return nativeMinimumHeight(nativeHandle);
    }
    private static native int nativeMinimumHeight(long nativeHandle);

    /** minimumWidth（Qt minimumWidth）。 */
    public int minimumWidth() {
        return nativeMinimumWidth(nativeHandle);
    }
    private static native int nativeMinimumWidth(long nativeHandle);

    /** scroll（Qt scroll）。 */
    public void scroll(int arg0, int arg1) {
        nativeScroll(nativeHandle, arg0, arg1);
    }
    private static native void nativeScroll(long nativeHandle, int arg0, int arg1);

    /** setAccessibleDescription（Qt setAccessibleDescription）。 */
    public void setAccessibleDescription(String arg0) {
        nativeSetAccessibleDescription(nativeHandle, arg0);
    }
    private static native void nativeSetAccessibleDescription(long nativeHandle, String arg0);

    /** setAccessibleIdentifier（Qt setAccessibleIdentifier）。 */
    public void setAccessibleIdentifier(String arg0) {
        nativeSetAccessibleIdentifier(nativeHandle, arg0);
    }
    private static native void nativeSetAccessibleIdentifier(long nativeHandle, String arg0);

    /** setAccessibleName（Qt setAccessibleName）。 */
    public void setAccessibleName(String arg0) {
        nativeSetAccessibleName(nativeHandle, arg0);
    }
    private static native void nativeSetAccessibleName(long nativeHandle, String arg0);

    /** setDisabled（Qt setDisabled）。 */
    public void setDisabled(boolean arg0) {
        nativeSetDisabled(nativeHandle, arg0);
    }
    private static native void nativeSetDisabled(long nativeHandle, boolean arg0);

    /** setHidden（Qt setHidden）。 */
    public void setHidden(boolean arg0) {
        nativeSetHidden(nativeHandle, arg0);
    }
    private static native void nativeSetHidden(long nativeHandle, boolean arg0);

    /** setMaximumHeight（Qt setMaximumHeight）。 */
    public void setMaximumHeight(int arg0) {
        nativeSetMaximumHeight(nativeHandle, arg0);
    }
    private static native void nativeSetMaximumHeight(long nativeHandle, int arg0);

    /** setMaximumWidth（Qt setMaximumWidth）。 */
    public void setMaximumWidth(int arg0) {
        nativeSetMaximumWidth(nativeHandle, arg0);
    }
    private static native void nativeSetMaximumWidth(long nativeHandle, int arg0);

    /** setMinimumHeight（Qt setMinimumHeight）。 */
    public void setMinimumHeight(int arg0) {
        nativeSetMinimumHeight(nativeHandle, arg0);
    }
    private static native void nativeSetMinimumHeight(long nativeHandle, int arg0);

    /** setMinimumWidth（Qt setMinimumWidth）。 */
    public void setMinimumWidth(int arg0) {
        nativeSetMinimumWidth(nativeHandle, arg0);
    }
    private static native void nativeSetMinimumWidth(long nativeHandle, int arg0);

    /** setShortcutAutoRepeat（Qt setShortcutAutoRepeat）。 */
    public void setShortcutAutoRepeat(int arg0, boolean arg1) {
        nativeSetShortcutAutoRepeat(nativeHandle, arg0, arg1);
    }
    private static native void nativeSetShortcutAutoRepeat(long nativeHandle, int arg0, boolean arg1);

    /** setShortcutEnabled（Qt setShortcutEnabled）。 */
    public void setShortcutEnabled(int arg0, boolean arg1) {
        nativeSetShortcutEnabled(nativeHandle, arg0, arg1);
    }
    private static native void nativeSetShortcutEnabled(long nativeHandle, int arg0, boolean arg1);

    /** setSizeIncrement（Qt setSizeIncrement）。 */
    public void setSizeIncrement(int arg0, int arg1) {
        nativeSetSizeIncrement(nativeHandle, arg0, arg1);
    }
    private static native void nativeSetSizeIncrement(long nativeHandle, int arg0, int arg1);

    /** setStatusTip（Qt setStatusTip）。 */
    public void setStatusTip(String arg0) {
        nativeSetStatusTip(nativeHandle, arg0);
    }
    private static native void nativeSetStatusTip(long nativeHandle, String arg0);

    /** setTabletTracking（Qt setTabletTracking）。 */
    public void setTabletTracking(boolean arg0) {
        nativeSetTabletTracking(nativeHandle, arg0);
    }
    private static native void nativeSetTabletTracking(long nativeHandle, boolean arg0);

    /** setToolTipDuration（Qt setToolTipDuration）。 */
    public void setToolTipDuration(int arg0) {
        nativeSetToolTipDuration(nativeHandle, arg0);
    }
    private static native void nativeSetToolTipDuration(long nativeHandle, int arg0);

    /** setWhatsThis（Qt setWhatsThis）。 */
    public void setWhatsThis(String arg0) {
        nativeSetWhatsThis(nativeHandle, arg0);
    }
    private static native void nativeSetWhatsThis(long nativeHandle, String arg0);

    /** setWindowFilePath（Qt setWindowFilePath）。 */
    public void setWindowFilePath(String arg0) {
        nativeSetWindowFilePath(nativeHandle, arg0);
    }
    private static native void nativeSetWindowFilePath(long nativeHandle, String arg0);

    /** setWindowModified（Qt setWindowModified）。 */
    public void setWindowModified(boolean arg0) {
        nativeSetWindowModified(nativeHandle, arg0);
    }
    private static native void nativeSetWindowModified(long nativeHandle, boolean arg0);

    /** showFullScreen（Qt showFullScreen）。 */
    public void showFullScreen() {
        nativeShowFullScreen(nativeHandle);
    }
    private static native void nativeShowFullScreen(long nativeHandle);

    /** showMaximized（Qt showMaximized）。 */
    public void showMaximized() {
        nativeShowMaximized(nativeHandle);
    }
    private static native void nativeShowMaximized(long nativeHandle);

    /** showMinimized（Qt showMinimized）。 */
    public void showMinimized() {
        nativeShowMinimized(nativeHandle);
    }
    private static native void nativeShowMinimized(long nativeHandle);

    /** showNormal（Qt showNormal）。 */
    public void showNormal() {
        nativeShowNormal(nativeHandle);
    }
    private static native void nativeShowNormal(long nativeHandle);

    /** statusTip（Qt statusTip）。 */
    public String statusTip() {
        return nativeStatusTip(nativeHandle);
    }
    private static native String nativeStatusTip(long nativeHandle);

    /** toolTipDuration（Qt toolTipDuration）。 */
    public int toolTipDuration() {
        return nativeToolTipDuration(nativeHandle);
    }
    private static native int nativeToolTipDuration(long nativeHandle);

    /** underMouse（Qt underMouse）。 */
    public boolean underMouse() {
        return nativeUnderMouse(nativeHandle);
    }
    private static native boolean nativeUnderMouse(long nativeHandle);

    /** unsetCursor（Qt unsetCursor）。 */
    public void unsetCursor() {
        nativeUnsetCursor(nativeHandle);
    }
    private static native void nativeUnsetCursor(long nativeHandle);

    /** unsetLayoutDirection（Qt unsetLayoutDirection）。 */
    public void unsetLayoutDirection() {
        nativeUnsetLayoutDirection(nativeHandle);
    }
    private static native void nativeUnsetLayoutDirection(long nativeHandle);

    /** unsetLocale（Qt unsetLocale）。 */
    public void unsetLocale() {
        nativeUnsetLocale(nativeHandle);
    }
    private static native void nativeUnsetLocale(long nativeHandle);

    /** updateGeometry（Qt updateGeometry）。 */
    public void updateGeometry() {
        nativeUpdateGeometry(nativeHandle);
    }
    private static native void nativeUpdateGeometry(long nativeHandle);

    /** updatesEnabled（Qt updatesEnabled）。 */
    public boolean updatesEnabled() {
        return nativeUpdatesEnabled(nativeHandle);
    }
    private static native boolean nativeUpdatesEnabled(long nativeHandle);

    /** whatsThis（Qt whatsThis）。 */
    public String whatsThis() {
        return nativeWhatsThis(nativeHandle);
    }
    private static native String nativeWhatsThis(long nativeHandle);

    /** windowFilePath（Qt windowFilePath）。 */
    public String windowFilePath() {
        return nativeWindowFilePath(nativeHandle);
    }
    private static native String nativeWindowFilePath(long nativeHandle);

    /** windowRole（Qt windowRole）。 */
    public String windowRole() {
        return nativeWindowRole(nativeHandle);
    }
    private static native String nativeWindowRole(long nativeHandle);

}
