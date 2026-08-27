/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 跨平台标题栏：Windows 显示 — ▢ ✕（右侧，Fluent 风格），
 * macOS 显示 ● ● ● 交通灯（左侧）。
 * 配合 {@link QMainWindow#setFrameless(boolean)} 使用。
 * <p>
 * 用法：
 * <pre>
 * JQtTitleBar bar = new JQtTitleBar("我的应用", window);
 * vbox.addWidget(bar);   // 放在窗口布局顶部
 * </pre>
 */
public class JQtTitleBar extends QWidget {

    private static final boolean IS_MAC =
            System.getProperty("os.name", "").toLowerCase().contains("mac");
    private static final boolean IS_WIN =
            System.getProperty("os.name", "").toLowerCase().contains("win");

    // Segoe MDL2 Assets 字形（Windows 10/11 原生窗口按钮图标）
    private static final String GLYPH_MINIMIZE = "\uE921";
    private static final String GLYPH_MAXIMIZE = "\uE922";
    private static final String GLYPH_RESTORE   = "\uE923";
    private static final String GLYPH_CLOSE     = "\uE8BB";

    /**
     * 创建标题栏。
     * @param title 窗口标题文字
     * @param window 关联窗口（按钮控制其最小化/最大化/关闭）
     */
    public JQtTitleBar(String title, QMainWindow window) {
        nativeHandle = nativeCreate();
        registerCleaner();
        setObjectName("titleBar");   // QSS: QFrame#titleBar 可单独定制标题栏背景

        QHBoxLayout bar = new QHBoxLayout();
        bar.setSpacing(4);

        QLabel titleLabel = new QLabel("  " + title);
        titleLabel.setStyleSheet("font-size: 13px; font-weight: bold;");

        QPushButton minBtn = new QPushButton("—");
        QPushButton maxBtn = new QPushButton("▢");
        QPushButton closeBtn = new QPushButton("✕");
        minBtn.setObjectName("titlebarBtn");
        maxBtn.setObjectName("titlebarBtn");
        closeBtn.setObjectName("titlebarClose");

        minBtn.onClicked(() -> window.minimize());
        closeBtn.onClicked(() -> window.close());
        // 注意：maxBtn 的 onClicked 在各平台分支注册（Windows 带图标切换），
        // 避免重复注册导致 toggle 两次 = 点击无效果（历史 bug）

        if (IS_MAC) {
            // macOS 交通灯：红黄绿圆点（左侧）
            QPushButton red = new QPushButton("●");
            QPushButton yellow = new QPushButton("●");
            QPushButton green = new QPushButton("●");
            red.setStyleSheet("color: #ff5f57; background: transparent; border: none; font-size: 10px; padding: 0;");
            yellow.setStyleSheet("color: #febc2e; background: transparent; border: none; font-size: 10px; padding: 0;");
            green.setStyleSheet("color: #28c840; background: transparent; border: none; font-size: 10px; padding: 0;");
            red.onClicked(() -> window.close());
            yellow.onClicked(() -> window.minimize());
            green.onClicked(() -> window.toggleMaximize());
            bar.addWidget(red);
            bar.addWidget(yellow);
            bar.addWidget(green);
            bar.addWidget(titleLabel);
            bar.addStretch(1);
        } else if (IS_WIN) {
            // Windows：Segoe MDL2 Assets 原生字形（与系统窗口按钮一致）
            minBtn.setText(GLYPH_MINIMIZE);
            maxBtn.setText(GLYPH_MAXIMIZE);
            closeBtn.setText(GLYPH_CLOSE);
            // 显式 background: transparent：避免按钮级 QSS 合并时漏掉全局 #titlebarBtn 的透明背景
            String glyphQss = "font-family: \"Segoe MDL2 Assets\"; font-size: 10px; padding: 4px 12px; min-height: 0; max-height: 34px; background: transparent;";
            minBtn.setStyleSheet(glyphQss);
            maxBtn.setStyleSheet(glyphQss);
            closeBtn.setStyleSheet(glyphQss);
            // 最大化 ↔ 还原 图标随状态切换
            maxBtn.onClicked(() -> {
                window.toggleMaximize();
                maxBtn.setText(window.isMaximized() ? GLYPH_RESTORE : GLYPH_MAXIMIZE);
            });
            bar.addWidget(titleLabel);
            bar.addStretch(1);
            bar.addWidget(minBtn);
            bar.addWidget(maxBtn);
            bar.addWidget(closeBtn);
        } else {
            // Linux：标题在左，三件套在右（字符图标）
            maxBtn.onClicked(() -> window.toggleMaximize());
            bar.addWidget(titleLabel);
            bar.addStretch(1);
            bar.addWidget(minBtn);
            bar.addWidget(maxBtn);
            bar.addWidget(closeBtn);
        }
        setLayout(bar);
    }

    private native long nativeCreate();
}





