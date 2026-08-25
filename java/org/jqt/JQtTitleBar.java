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
 * 配合 {@link JQtWindow#setFrameless(boolean)} 使用。
 * <p>
 * 用法：
 * <pre>
 * JQtTitleBar bar = new JQtTitleBar("我的应用", window);
 * vbox.addWidget(bar);   // 放在窗口布局顶部
 * </pre>
 */
public class JQtTitleBar extends JQtWidget {

    private static final boolean IS_MAC =
            System.getProperty("os.name", "").toLowerCase().contains("mac");

    /**
     * 创建标题栏。
     * @param title 窗口标题文字
     * @param window 关联窗口（按钮控制其最小化/最大化/关闭）
     */
    public JQtTitleBar(String title, JQtWindow window) {
        nativeHandle = nativeCreate();
        registerCleaner();

        JQtHBoxLayout bar = new JQtHBoxLayout();
        bar.setSpacing(4);

        JQtLabel titleLabel = new JQtLabel("  " + title);
        titleLabel.setStyleSheet("font-size: 13px; font-weight: bold;");

        JQtButton minBtn = new JQtButton("—");
        JQtButton maxBtn = new JQtButton("▢");
        JQtButton closeBtn = new JQtButton("✕");
        minBtn.setObjectName("titlebarBtn");
        maxBtn.setObjectName("titlebarBtn");
        closeBtn.setObjectName("titlebarClose");

        minBtn.onClick(() -> window.minimize());
        maxBtn.onClick(() -> window.toggleMaximize());
        closeBtn.onClick(() -> window.close());

        if (IS_MAC) {
            // macOS 交通灯：红黄绿圆点（左侧）
            JQtButton red = new JQtButton("●");
            JQtButton yellow = new JQtButton("●");
            JQtButton green = new JQtButton("●");
            red.setStyleSheet("color: #ff5f57; background: transparent; border: none; font-size: 10px; padding: 0;");
            yellow.setStyleSheet("color: #febc2e; background: transparent; border: none; font-size: 10px; padding: 0;");
            green.setStyleSheet("color: #28c840; background: transparent; border: none; font-size: 10px; padding: 0;");
            red.onClick(() -> window.close());
            yellow.onClick(() -> window.minimize());
            green.onClick(() -> window.toggleMaximize());
            bar.addWidget(red);
            bar.addWidget(yellow);
            bar.addWidget(green);
            bar.addWidget(titleLabel);
            bar.addStretch(1);
        } else {
            // Windows/Linux：标题在左，三件套在右
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