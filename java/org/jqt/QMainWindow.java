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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * 窗口：封装 C++ 侧的顶级 {@code QWidget}。
 * <p>
 * 事件（均可注册多个监听器）：
 * <ul>
 *   <li>{@link #onClose(Runnable)} — 窗口关闭（closeEvent）</li>
 *   <li>{@link #onResized(BiConsumer)} — 窗口大小变化（resizeEvent，参数为宽高）</li>
 *   <li>{@link #onMoved(BiConsumer)} — 窗口位置变化（moveEvent，参数为 x/y）</li>
 * </ul>
 */
public class QMainWindow extends QWidget {

    private final List<Runnable> onCloseHandlers = new ArrayList<>();
    private final List<BiConsumer<Integer, Integer>> onResizedHandlers = new ArrayList<>();
    private final List<BiConsumer<Integer, Integer>> onMovedHandlers = new ArrayList<>();

    /** 创建一个 800x600 的新窗口。 */
    public QMainWindow(String title) {
        this(title, 800, 600);
    }

    /** 创建一个指定大小的新窗口。 */
    public QMainWindow(String title, int width, int height) {
        nativeHandle = nativeCreate(title, width, height);
        registerCleaner();
    }

    private native long nativeCreate(String title, int width, int height);

    // show()/hide()/close()/resize() 继承自 QWidget（基础 API，v0.6.0 上移到基类）

    // ---- Fluent 窗口能力（无边框 / 亚克力 / 圆角 / 拖拽）----

    /**
     * 无边框模式（Fluent 风格窗口的基石）。
     * 启用后：移除系统标题栏、自动添加 DWM 阴影、边框 5px 区域可缩放、
     * 顶部 40px 区域可拖拽移动。需要自绘标题栏（最小化/关闭按钮等）。
     */
    public void setFrameless(boolean frameless) {
        nativeSetFrameless(nativeHandle, frameless);
    }
    private native void nativeSetFrameless(long handle, boolean frameless);

    /** 亚克力背景（Windows 10+，模糊 + 半透明混合色）。 */
    public void setAcrylic(boolean acrylic) {
        nativeSetAcrylic(nativeHandle, acrylic);
    }
    private native void nativeSetAcrylic(long handle, boolean acrylic);

    /** Windows 11 圆角窗口。 */
    public void setRoundedCorners(boolean rounded) {
        nativeSetRoundedCorners(nativeHandle, rounded);
    }
    private native void nativeSetRoundedCorners(long handle, boolean rounded);

    /** 是否允许顶部区域拖拽移动窗口（默认 true）。 */
    public void setDraggable(boolean draggable) {
        nativeSetDraggable(nativeHandle, draggable);
    }
    private native void nativeSetDraggable(long handle, boolean draggable);

    /** 无边框窗口的缩放热区宽度（像素，默认 5）。 */
    public void setBorderWidth(int px) {
        nativeSetBorderWidth(nativeHandle, px);
    }
    private native void nativeSetBorderWidth(long handle, int px);

    /** 最小化。 */
    public void minimize() {
        nativeMinimize(nativeHandle);
    }
    private native void nativeMinimize(long handle);

    /** 最大化。 */
    public void maximize() {
        nativeMaximize(nativeHandle);
    }
    private native void nativeMaximize(long handle);

    /** 最大化/还原切换。 */
    public void toggleMaximize() {
        nativeToggleMaximize(nativeHandle);
    }
    private native void nativeToggleMaximize(long handle);

    /** 是否已最大化。 */
    public boolean isMaximized() {
        return nativeIsMaximized(nativeHandle);
    }
    private native boolean nativeIsMaximized(long handle);

    // ---- 动画（QPropertyAnimation）----

    /** 窗口淡入（透明度 0 → 1，默认 200ms）。 */
    public void fadeIn(long ms) {
        nativeFadeIn(nativeHandle, ms);
    }
    private native void nativeFadeIn(long handle, long ms);

    /** 窗口淡出（透明度 1 → 0）。 */
    public void fadeOut(long ms) {
        nativeFadeOut(nativeHandle, ms);
    }
    private native void nativeFadeOut(long handle, long ms);

    /** 窗口淡入，可指定缓动函数。 */
    public void fadeIn(long ms, JQtEasing easing) {
        nativeFadeInEasing(nativeHandle, ms, easing.qtType);
    }
    private native void nativeFadeInEasing(long handle, long ms, int easing);

    /** 窗口淡出，可指定缓动函数。 */
    public void fadeOut(long ms, JQtEasing easing) {
        nativeFadeOutEasing(nativeHandle, ms, easing.qtType);
    }
    private native void nativeFadeOutEasing(long handle, long ms, int easing);

    /** 修改窗口标题。 */
    public void setTitle(String title) {
        nativeSetTitle(nativeHandle, title);
    }
    private native void nativeSetTitle(long handle, String title);

    /**
     * 添加子控件（未设置布局时按顺序自动摆放）。
     * 若已调用 {@link #setLayout(QLayout)}，子控件应改用 {@link QLayout#addWidget(QWidget)} 加入布局。
     */
    public void addWidget(QWidget child) {
        nativeAddWidget(nativeHandle, child.nativeHandle());
    }
    private native void nativeAddWidget(long handle, long childHandle);

    /**
     * 设置布局管理器（继承自 {@link QWidget}）。
     * 布局接管子控件的位置与大小；重复设置会替换并销毁旧布局（Qt 行为）。
     */

    /** 注册窗口关闭回调（closeEvent）。 */
    public QMainWindow onClose(Runnable handler) {
        onCloseHandlers.add(handler);
        return this;
    }

    /** 注册窗口大小变化回调（resizeEvent，参数为新的宽和高）。 */
    public QMainWindow onResized(BiConsumer<Integer, Integer> handler) {
        onResizedHandlers.add(handler);
        return this;
    }

    /** 注册窗口位置变化回调（moveEvent，参数为新的 x 和 y）。 */
    public QMainWindow onMoved(BiConsumer<Integer, Integer> handler) {
        onMovedHandlers.add(handler);
        return this;
    }

    /** 由 C++ 侧在窗口关闭时回调（JNI）。 */
    void nativeHandleClose() {
        for (Runnable h : onCloseHandlers) {
            h.run();
        }
    }

    /** 由 C++ 侧在窗口大小变化时回调（JNI）。 */
    void nativeHandleResized(int width, int height) {
        for (BiConsumer<Integer, Integer> h : onResizedHandlers) {
            h.accept(width, height);
        }
    }

    /** 由 C++ 侧在窗口移动时回调（JNI）。 */
    void nativeHandleMoved(int x, int y) {
        for (BiConsumer<Integer, Integer> h : onMovedHandlers) {
            h.accept(x, y);
        }
    }

    // ---- L1 补全（v0.8.0）：iconSizeChanged / toolButtonStyleChanged 信号 ----

    private final List<Consumer<Integer>> onIconSizeChangedHandlers = new ArrayList<>();
    private final List<Consumer<Integer>> onToolButtonStyleChangedHandlers = new ArrayList<>();

    /** 工具栏图标尺寸变化回调（iconSizeChanged 信号，参数为图标边长像素）。 */
    public QMainWindow onIconSizeChanged(Consumer<Integer> handler) {
        onIconSizeChangedHandlers.add(handler);
        nativeConnectIconSizeChanged(nativeHandle);
        return this;
    }
    private native void nativeConnectIconSizeChanged(long handle);

    /** 工具栏按钮样式变化回调（toolButtonStyleChanged 信号，参数为 Qt::ToolButtonStyle：0 仅图标 / 1 仅文字 / 2 文字在图标旁 / 3 文字在图标下）。 */
    public QMainWindow onToolButtonStyleChanged(Consumer<Integer> handler) {
        onToolButtonStyleChangedHandlers.add(handler);
        nativeConnectToolButtonStyleChanged(nativeHandle);
        return this;
    }
    private native void nativeConnectToolButtonStyleChanged(long handle);

    void nativeHandleIconSizeChanged(int size) {
        for (Consumer<Integer> h : onIconSizeChangedHandlers) h.accept(size);
    }

    void nativeHandleToolButtonStyleChanged(int style) {
        for (Consumer<Integer> h : onToolButtonStyleChangedHandlers) h.accept(style);
    }
    // ---- Exclusive Kit（跨平台独家能力，Qt 官方未封装）----
    //   v0.6.1 : Windows —— DWM 边框/标题栏/文字颜色 + 深色标题栏 + Mica + 任务栏进度
    //   v0.7.0 : macOS —— Dock 徽章 + 透明标题栏 + 全尺寸内容视图（对齐 Windows 任务栏进度/DWM 能力）
    //   v0.7.0 : Linux —— XDG 开机自启 + D-Bus 防息屏/通知（见 QApplication）

    /**
     * 设置原生窗口边框颜色（0xAARRGGBB）。
     * 依赖 Windows 11 22H2+ 的 DWM 属性；旧系统静默忽略。
     */
    public void setNativeBorderColor(int argb) {
        nativeSetDwmAttribute(nativeHandle, 1, argb);
    }

    /** 设置原生标题栏颜色（0xAARRGGBB；Win11 22H2+，旧系统忽略）。 */
    public void setNativeCaptionColor(int argb) {
        nativeSetDwmAttribute(nativeHandle, 2, argb);
    }

    /** 设置原生标题栏文字颜色（0xAARRGGBB；Win11 22H2+，旧系统忽略）。 */
    public void setNativeCaptionTextColor(int argb) {
        nativeSetDwmAttribute(nativeHandle, 3, argb);
    }

    /** 深色标题栏（Win10 1809+ 支持）。 */
    public void setNativeDarkTitleBar(boolean dark) {
        nativeSetDwmAttribute(nativeHandle, 4, dark ? 1 : 0);
    }

    /** Mica 背景材质（Win11 22H2+；开启后窗口背景跟随系统 Mica 质感）。 */
    public void setMicaBackground(boolean on) {
        nativeSetDwmAttribute(nativeHandle, 5, on ? 1 : 0);
    }

    /** 任务栏图标进度（value/max，如 30/100；Win10+；max ≤ 0 或 value < 0 清除）。 */
    public void setTaskbarProgress(int value, int max) {
        nativeTaskbarProgress(nativeHandle, value, max);
    }

    /** 清除任务栏进度。 */
    public void clearTaskbarProgress() {
        nativeTaskbarProgress(nativeHandle, -1, 0);
    }

    private static native void nativeSetDwmAttribute(long handle, int kind, int argb);
    private static native void nativeTaskbarProgress(long handle, int value, int max);

    // ---- macOS 独家能力（v0.7.0；Windows/Linux 上为无操作）----

    /**
     * Dock 图标徽章（macOS 通知角标，如未读消息数）。
     * 对齐 Windows 任务栏进度：macOS 没有任务栏进度概念，用 Dock 角标呈现应用状态。
     * null 或空串清除；非 macOS 平台无操作。
     */
    public void setDockBadge(String badge) {
        nativeSetDockBadge(nativeHandle, badge);
    }

    /** 清除 Dock 徽章（macOS）。 */
    public void clearDockBadge() {
        nativeSetDockBadge(nativeHandle, null);
    }

    /**
     * 透明标题栏（macOS：保留红黄绿窗口按钮，标题栏区域透明，内容可延伸至顶部）。
     * Qt 官方只能"全有或全无"（无边框 = 连窗口按钮一起去掉）；
     * 此 API 保留原生窗口按钮的同时实现沉浸式布局。
     * 建议在窗口 show() 之前调用；非 macOS 平台无操作。
     */
    public void setMacTitlebarTransparent(boolean transparent) {
        nativeSetMacWindowAttribute(nativeHandle, 1, transparent);
    }

    /**
     * 全尺寸内容视图（macOS：内容视图延伸到标题栏区域，配合
     * setMacTitlebarTransparent 实现无边框观感但保留红黄绿按钮）。
     * 非 macOS 平台无操作。
     */
    public void setMacFullSizeContentView(boolean on) {
        nativeSetMacWindowAttribute(nativeHandle, 2, on);
    }

    private static native void nativeSetDockBadge(long handle, String badge);
    private static native void nativeSetMacWindowAttribute(long handle, int kind, boolean value);

    // ---- End Exclusive Kit ----

    // L1：toolbar 相关信号由 QToolBar 提供（JQtWindowShell 非 QMainWindow 类型）

    // ==================== v1.8.0 L1-100：主窗口语义（手搓批）====================
    // 设计（壳内组装）：不动 C++ JQtWindowShell；在 Java 侧用现有布局/控件组装
    //   [menuBar] → [toolbar…] → [topDock] → [leftDock | central | rightDock] → [bottomDock] → [statusBar]
    // 容器（QFrame）与条均为持久对象，结构变化时整体重建内部布局（Qt 会销毁旧布局，控件保留）。
    // 说明：未调用下列任何 API 的旧窗口保持原行为（addWidget 自动摆放 / setLayout 直挂），零回归。

    /** 停靠区常量（与 Qt::DockWidgetArea 一致）。 */
    public static final int DOCK_LEFT = 1;
    public static final int DOCK_RIGHT = 2;
    public static final int DOCK_TOP = 4;
    public static final int DOCK_BOTTOM = 8;

    private boolean mainStructureEngaged;
    private QMenuBar menuBarWidget;          // setMenuBar / menuBar() 惰性创建
    private QStatusBar statusBarWidget;      // setStatusBar / statusBar() 惰性创建
    private final List<QToolBar> toolBars = new ArrayList<>();
    private QWidget centralContent;
    private final List<QWidget> leftDocks = new ArrayList<>();
    private final List<QWidget> rightDocks = new ArrayList<>();
    private final List<QWidget> topDocks = new ArrayList<>();
    private final List<QWidget> bottomDocks = new ArrayList<>();
    private QFrame centralFrame;              // central 内容容器（持久）
    private QFrame leftFrame, rightFrame, topFrame, bottomFrame;
    private QFrame middleFrame;               // 中行（左停靠|中央|右停靠）容器（持久）

    /** 设置中央控件（setCentralWidget；替换旧的中央内容）。 */
    public void setCentralWidget(QWidget widget) {
        ensureCentralFrame();
        if (widget == null) {
            if (centralContent != null) {
                centralContent.setParent(null);
                centralContent = null;
            }
            return;
        }
        if (centralContent != widget) {
            if (centralContent != null) {
                centralContent.setParent(null);
            }
            centralContent = widget;
        }
        engage();
        rebuildMainLayout();
    }

    /** 中央控件（未设置返回 null）。 */
    public QWidget centralWidget() {
        return centralContent;
    }

    /** 设置菜单栏（setMenuBar；替换旧的）。 */
    public void setMenuBar(QMenuBar bar) {
        if (menuBarWidget != null && menuBarWidget != bar) {
            menuBarWidget.setParent(null);
        }
        menuBarWidget = bar;
        engage();
        rebuildMainLayout();
    }

    /** 菜单栏（menuBar；Qt 语义：无则惰性创建一个并挂载）。 */
    public QMenuBar menuBar() {
        if (menuBarWidget == null) {
            setMenuBar(new QMenuBar());
        }
        return menuBarWidget;
    }

    /** 设置状态栏（setStatusBar；替换旧的）。 */
    public void setStatusBar(QStatusBar bar) {
        if (statusBarWidget != null && statusBarWidget != bar) {
            statusBarWidget.setParent(null);
        }
        statusBarWidget = bar;
        engage();
        rebuildMainLayout();
    }

    /** 状态栏（statusBar；Qt 语义：无则惰性创建一个并挂载）。 */
    public QStatusBar statusBar() {
        if (statusBarWidget == null) {
            setStatusBar(new QStatusBar());
        }
        return statusBarWidget;
    }

    /** 添加工具条（addToolBar；多个按添加顺序纵向堆叠）。 */
    public void addToolBar(QToolBar bar) {
        if (bar == null || toolBars.contains(bar)) {
            return;
        }
        toolBars.add(bar);
        if (mainIconSize >= 0) {
            bar.setIconSize(mainIconSize);
        }
        if (mainToolStyle >= 0) {
            bar.setToolButtonStyle(mainToolStyle);
        }
        engage();
        rebuildMainLayout();
    }

    /** 移除工具条（removeToolBar；控件脱离窗口）。 */
    public void removeToolBar(QToolBar bar) {
        if (bar == null || !toolBars.remove(bar)) {
            return;
        }
        bar.setParent(null);
        rebuildMainLayout();
    }

    /** 添加停靠控件（addDockWidget；area 见 DOCK_* 常量）。 */
    public void addDockWidget(int area, QWidget widget) {
        if (widget == null) {
            return;
        }
        List<QWidget> target;
        switch (area) {
            case DOCK_LEFT:  target = leftDocks; break;
            case DOCK_RIGHT: target = rightDocks; break;
            case DOCK_TOP:   target = topDocks; break;
            case DOCK_BOTTOM: target = bottomDocks; break;
            default: throw new IllegalArgumentException("非法停靠区: " + area + "（DOCK_LEFT/RIGHT/TOP/BOTTOM）");
        }
        if (!target.contains(widget)) {
            target.add(widget);
        }
        engage();
        rebuildMainLayout();
    }

    /** 移除停靠控件（removeDockWidget；控件脱离窗口）。 */
    public void removeDockWidget(QWidget widget) {
        if (widget == null) {
            return;
        }
        boolean removed = leftDocks.remove(widget) | rightDocks.remove(widget)
                        | topDocks.remove(widget) | bottomDocks.remove(widget);
        if (removed) {
            widget.setParent(null);
            rebuildMainLayout();
        }
    }

    // ==================== v1.8.0 L2-B2：主窗口结构状态持久化 ====================
    // 语义说明:JQtWindowShell 非 Qt QMainWindow,无 Qt 内部 dock 状态机可序列化;
    // 本实现序列化"我们自己的壳结构"(chrome 存在性 + 工具条顺序 + 停靠分布),以 objectName 为稳定标识,
    // 原子恢复(先校验后应用)。跨进程重启需应用侧重建同名控件;本格式带版本头,可演进。

    private static final byte[] STATE_MAGIC = { 'J', 'Q', 'T', 'S' };

    /** 保存主窗口结构状态（menuBar/statusBar 存在性、工具条顺序、四向停靠分布）。 */
    public byte[] saveState() {
        ensureChromeNames();
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        bos.write(STATE_MAGIC, 0, STATE_MAGIC.length);
        bos.write(1);  // version
        bos.write(menuBarWidget != null ? 1 : 0);
        bos.write(statusBarWidget != null ? 1 : 0);
        bos.write(centralContent != null ? 1 : 0);
        writeNames(bos, namesOf(toolBars));
        writeNames(bos, namesOf(leftDocks));
        writeNames(bos, namesOf(rightDocks));
        writeNames(bos, namesOf(topDocks));
        writeNames(bos, namesOf(bottomDocks));
        return bos.toByteArray();
    }

    /**
     * 恢复主窗口结构状态（与 {@link #saveState()} 配对）。
     * 匹配规则:按 objectName 对当前已存在的工具条/停靠控件重排;保存时存在的 chrome 当前必须仍在,
     * 否则返回 false 且不改变任何状态（原子）。
     */
    public boolean restoreState(byte[] state) {
        if (state == null || state.length < 7) {
            return false;
        }
        try {
            java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(state);
            byte[] magic = new byte[4];
            if (bis.read(magic) != 4 || !java.util.Arrays.equals(magic, STATE_MAGIC)) {
                return false;
            }
            if (bis.read() != 1) {
                return false;  // 版本不兼容
            }
            boolean wantMenu = bis.read() == 1;
            boolean wantStatus = bis.read() == 1;
            boolean wantCentral = bis.read() == 1;
            java.util.List<String> tbNames = readNames(bis);
            java.util.List<String> left = readNames(bis);
            java.util.List<String> right = readNames(bis);
            java.util.List<String> top = readNames(bis);
            java.util.List<String> bottom = readNames(bis);
            if (tbNames == null || left == null || right == null || top == null || bottom == null) {
                return false;
            }
            if (wantMenu != (menuBarWidget != null) || wantStatus != (statusBarWidget != null)
                    || wantCentral != (centralContent != null)) {
                return false;
            }
            // 校验:目标名字与现存对象一一对应(集合相等,顺序按保存)
            java.util.List<QToolBar> newToolbars = new ArrayList<>();
            if (!matchOrder(tbNames, toolBars, newToolbars)) {
                return false;
            }
            java.util.List<QWidget> allDocks = new ArrayList<>();
            allDocks.addAll(leftDocks); allDocks.addAll(rightDocks);
            allDocks.addAll(topDocks); allDocks.addAll(bottomDocks);
            java.util.List<QWidget> newLeft = new ArrayList<>();
            java.util.List<QWidget> newRight = new ArrayList<>();
            java.util.List<QWidget> newTop = new ArrayList<>();
            java.util.List<QWidget> newBottom = new ArrayList<>();
            java.util.List<QWidget> pool = new ArrayList<>(allDocks);
            if (!matchArea(left, pool, newLeft) || !matchArea(right, pool, newRight)
                    || !matchArea(top, pool, newTop) || !matchArea(bottom, pool, newBottom)
                    || !pool.isEmpty()) {
                return false;
            }
            // 原子应用
            toolBars.clear();
            toolBars.addAll(newToolbars);
            leftDocks.clear(); leftDocks.addAll(newLeft);
            rightDocks.clear(); rightDocks.addAll(newRight);
            topDocks.clear(); topDocks.addAll(newTop);
            bottomDocks.clear(); bottomDocks.addAll(newBottom);
            rebuildMainLayout();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static java.util.List<String> namesOf(java.util.List<? extends QWidget> list) {
        java.util.List<String> out = new java.util.ArrayList<>(list.size());
        for (QWidget w : list) {
            out.add(w.objectName());
        }
        return out;
    }

    /** 按保存顺序从 pool 中逐名匹配现存对象(不匹配/数量不符 → false)。 */
    private static <T extends QWidget> boolean matchOrder(java.util.List<String> names, java.util.List<T> pool,
                                                          java.util.List<T> out) {
        if (names.size() != pool.size()) {
            return false;
        }
        java.util.List<T> copy = new java.util.ArrayList<>(pool);
        for (String n : names) {
            int idx = -1;
            for (int i = 0; i < copy.size(); i++) {
                if (copy.get(i).objectName().equals(n)) {
                    idx = i;
                    break;
                }
            }
            if (idx < 0) {
                return false;
            }
            out.add(copy.remove(idx));
        }
        return true;
    }

    private static <T extends QWidget> boolean matchArea(java.util.List<String> names, java.util.List<T> pool,
                                                         java.util.List<T> out) {
        for (String n : names) {
            int idx = -1;
            for (int i = 0; i < pool.size(); i++) {
                if (pool.get(i).objectName().equals(n)) {
                    idx = i;
                    break;
                }
            }
            if (idx < 0) {
                return false;
            }
            out.add(pool.remove(idx));
        }
        return true;
    }

    /** 为未命名的 chrome/停靠控件分配稳定默认名(仅一次,已有名不动)。 */
    private void ensureChromeNames() {
        if (menuBarWidget != null && menuBarWidget.objectName().isEmpty()) {
            menuBarWidget.setObjectName("jqt.menuBar");
        }
        if (statusBarWidget != null && statusBarWidget.objectName().isEmpty()) {
            statusBarWidget.setObjectName("jqt.statusBar");
        }
        nameIfEmpty(toolBars, "jqt.toolBar.");
        nameIfEmpty(leftDocks, "jqt.dock.left.");
        nameIfEmpty(rightDocks, "jqt.dock.right.");
        nameIfEmpty(topDocks, "jqt.dock.top.");
        nameIfEmpty(bottomDocks, "jqt.dock.bottom.");
    }

    private static void nameIfEmpty(java.util.List<? extends QWidget> list, String prefix) {
        for (QWidget w : list) {
            if (w.objectName().isEmpty()) {
                int i = 0;
                String candidate;
                do {
                    candidate = prefix + (i++);
                } while (anyNamed(list, candidate));
                w.setObjectName(candidate);
            }
        }
    }

    private static boolean anyNamed(java.util.List<? extends QWidget> list, String name) {
        for (QWidget w : list) {
            if (w.objectName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static void writeNames(java.io.ByteArrayOutputStream bos, java.util.List<String> names) {
        bos.write((names.size() >> 24) & 0xFF);
        bos.write((names.size() >> 16) & 0xFF);
        bos.write((names.size() >> 8) & 0xFF);
        bos.write(names.size() & 0xFF);
        for (String n : names) {
            byte[] b = n.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            bos.write((b.length >> 8) & 0xFF);
            bos.write(b.length & 0xFF);
            bos.write(b, 0, b.length);
        }
    }

    private static java.util.List<String> readNames(java.io.ByteArrayInputStream bis) {
        int count = (bis.read() << 24) | (bis.read() << 16) | (bis.read() << 8) | bis.read();
        if (count < 0 || count > 4096) {
            return null;
        }
        java.util.List<String> out = new java.util.ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int len = (bis.read() << 8) | bis.read();
            if (len < 0 || len > 65535) {
                return null;
            }
            byte[] b = new byte[len];
            if (bis.read(b, 0, len) != len) {
                return null;
            }
            out.add(new String(b, java.nio.charset.StandardCharsets.UTF_8));
        }
        return out;
    }

    // ---- 内部：惰性容器 + 布局重建 ----

    private void ensureCentralFrame() {
        if (centralFrame == null) {
            centralFrame = new QFrame();
        }
    }

    /** 侧停靠帧：持久对象，每次重建仅替换其内部布局（避免孤儿控件）。 */
    private QFrame ensureSideFrame(QFrame frame, List<QWidget> docks, boolean horizontal) {
        QFrame f = frame != null ? frame : new QFrame();
        QLayout fill = horizontal ? new QHBoxLayout() : new QVBoxLayout();
        fill.setContentsMargins(0, 0, 0, 0);
        fill.setSpacing(0);
        for (QWidget w : docks) {
            fill.addWidget(w);
        }
        nativeSetLayoutForce(f.nativeHandle(), fill.nativeHandle());
        return f;
    }

    private void engage() {
        if (mainStructureEngaged) {
            return;
        }
        mainStructureEngaged = true;
        ensureCentralFrame();
    }

    /** 结构变更后重建内部布局（旧主布局由 Qt 销毁；条/帧为持久对象，仅内层布局重建）。 */
    private void rebuildMainLayout() {
        if (!mainStructureEngaged) {
            return;
        }
        QVBoxLayout mainV = new QVBoxLayout();
        mainV.setContentsMargins(0, 0, 0, 0);
        mainV.setSpacing(0);

        if (menuBarWidget != null) {
            mainV.addWidget(menuBarWidget);
        }
        for (QToolBar tb : toolBars) {
            mainV.addWidget(tb);
        }
        if (!topDocks.isEmpty()) {
            topFrame = ensureSideFrame(topFrame, topDocks, true);
            mainV.addWidget(topFrame);
        }
        // 中行：左停靠 | 中央(拉伸) | 右停靠
        QHBoxLayout middle = new QHBoxLayout();
        middle.setContentsMargins(0, 0, 0, 0);
        middle.setSpacing(0);
        if (!leftDocks.isEmpty()) {
            leftFrame = ensureSideFrame(leftFrame, leftDocks, false);
            middle.addWidget(leftFrame);
        }
        ensureCentralFrame();
        QVBoxLayout centralFill = new QVBoxLayout();
        centralFill.setContentsMargins(0, 0, 0, 0);
        centralFill.setSpacing(0);
        if (centralContent != null) {
            centralFill.addWidget(centralContent);
        }
        nativeSetLayoutForce(centralFrame.nativeHandle(), centralFill.nativeHandle());
        middle.addWidget(centralFrame);
        middle.setStretch(middle.count() - 1, 1);
        if (!rightDocks.isEmpty()) {
            rightFrame = ensureSideFrame(rightFrame, rightDocks, false);
            middle.addWidget(rightFrame);
        }
        if (middleFrame == null) {
            middleFrame = new QFrame();
        }
        nativeSetLayoutForce(middleFrame.nativeHandle(), middle.nativeHandle());
        mainV.addWidget(middleFrame);
        if (!bottomDocks.isEmpty()) {
            bottomFrame = ensureSideFrame(bottomFrame, bottomDocks, true);
            mainV.addWidget(bottomFrame);
        }
        if (statusBarWidget != null) {
            mainV.addWidget(statusBarWidget);
        }
        nativeSetLayoutForce(nativeHandle, mainV.nativeHandle());
    }

    // ==================== v1.8.0 L2 功能层收尾：工具条图标尺寸/按钮样式（壳内聚合）====================
    // 语义：作用于本窗口全部已挂工具条；新挂工具条自动继承当前设置（-1 = 未显式设置）。

    private int mainIconSize = -1;
    private int mainToolStyle = -1;

    /** 统一设置已挂工具条的图标尺寸（像素；-1 恢复默认）。 */
    public void setIconSize(int size) {
        mainIconSize = size;
        for (QToolBar tb : toolBars) {
            tb.setIconSize(size);
        }
    }

    /** 当前工具条图标尺寸（-1 = 未显式设置，随各工具条默认）。 */
    public int iconSize() {
        return mainIconSize;
    }

    /** 统一设置已挂工具条的按钮样式（0 图标/1 文字/2 旁/3 下；-1 恢复默认）。 */
    public void setToolButtonStyle(int style) {
        mainToolStyle = style;
        for (QToolBar tb : toolBars) {
            tb.setToolButtonStyle(style);
        }
    }

    /** 当前工具条按钮样式（-1 = 未显式设置）。 */
    public int toolButtonStyle() {
        return mainToolStyle;
    }

// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** documentMode（Qt documentMode）。 */
    public boolean documentMode() {
        return nativeDocumentMode(nativeHandle);
    }
    private static native boolean nativeDocumentMode(long nativeHandle);

    /** isAnimated（Qt isAnimated）。 */
    public boolean isAnimated() {
        return nativeIsAnimated(nativeHandle);
    }
    private static native boolean nativeIsAnimated(long nativeHandle);

    /** isDockNestingEnabled（Qt isDockNestingEnabled）。 */
    public boolean isDockNestingEnabled() {
        return nativeIsDockNestingEnabled(nativeHandle);
    }
    private static native boolean nativeIsDockNestingEnabled(long nativeHandle);

    /** setAnimated（Qt setAnimated）。 */
    public void setAnimated(boolean arg0) {
        nativeSetAnimated(nativeHandle, arg0);
    }
    private static native void nativeSetAnimated(long nativeHandle, boolean arg0);

    /** setDockNestingEnabled（Qt setDockNestingEnabled）。 */
    public void setDockNestingEnabled(boolean arg0) {
        nativeSetDockNestingEnabled(nativeHandle, arg0);
    }
    private static native void nativeSetDockNestingEnabled(long nativeHandle, boolean arg0);

    /** setDocumentMode（Qt setDocumentMode）。 */
    public void setDocumentMode(boolean arg0) {
        nativeSetDocumentMode(nativeHandle, arg0);
    }
    private static native void nativeSetDocumentMode(long nativeHandle, boolean arg0);

    /** setUnifiedTitleAndToolBarOnMac（Qt setUnifiedTitleAndToolBarOnMac）。 */
    public void setUnifiedTitleAndToolBarOnMac(boolean arg0) {
        nativeSetUnifiedTitleAndToolBarOnMac(nativeHandle, arg0);
    }
    private static native void nativeSetUnifiedTitleAndToolBarOnMac(long nativeHandle, boolean arg0);

    /** unifiedTitleAndToolBarOnMac（Qt unifiedTitleAndToolBarOnMac）。 */
    public boolean unifiedTitleAndToolBarOnMac() {
        return nativeUnifiedTitleAndToolBarOnMac(nativeHandle);
    }
    private static native boolean nativeUnifiedTitleAndToolBarOnMac(long nativeHandle);

}