/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * Fluent 风格演示：无边框窗口 + 自绘标题栏 + 卡片 + 开关 + 导航。
 * 运行：.\\run.ps1 -Class org.jqt.JQtFluentDemo [-AutoClose 8000]
 */
public class JQtFluentDemo {

    public static void main(String[] args) throws Exception {
        JQtApplication app = new JQtApplication();

        // 统一主题入口（QSS + 调色板一致打包）
        String theme = System.getProperty("jqt.theme", "dark");
        app.setStyle("Fusion");
        if ("light".equals(theme)) {
            app.setTheme("fluent-light");
        } else {
            app.setTheme("fluent-dark");
        }
        System.out.println("[Fluent] 已应用主题 fluent-" + theme);

        JQtWindow window = new JQtWindow("JQt Fluent", 760, 520);
        window.setFrameless(true);
        window.setAcrylic(true);
        window.setRoundedCorners(true);

        // ---- 跨平台标题栏（Windows 三件套 / macOS 交通灯）----
        JQtTitleBar titleBar = new JQtTitleBar("JQt Fluent", window);

        // ---- 导航（列表）----
        JQtListWidget nav = new JQtListWidget();
        nav.addItem("首页");
        nav.addItem("设置");
        nav.addItem("关于");
        nav.setStyleSheet("font-size: 14px;");

        // ---- 卡片 1：开关组 ----
        JQtPanel card1 = new JQtPanel();
        JQtVBoxLayout card1L = new JQtVBoxLayout();
        card1L.setSpacing(10);
        JQtLabel card1Title = new JQtLabel("开关组");
        card1Title.setStyleSheet("font-size: 14px; font-weight: bold;");
        card1L.addWidget(card1Title);
        JQtCheckBox switch1 = new JQtCheckBox("自动更新");
        JQtCheckBox switch2 = new JQtCheckBox("开机自启");
        switch1.onToggled(on -> System.out.println("[Fluent] 自动更新 = " + on));
        switch2.onToggled(on -> System.out.println("[Fluent] 开机自启 = " + on));
        card1L.addWidget(switch1);
        card1L.addWidget(switch2);
        card1.setLayout(card1L);

        // ---- 卡片 2：输入与按钮 ----
        JQtPanel card2 = new JQtPanel();
        JQtVBoxLayout card2L = new JQtVBoxLayout();
        card2L.setSpacing(10);
        JQtLabel card2Title = new JQtLabel("输入与按钮");
        card2Title.setStyleSheet("font-size: 14px; font-weight: bold;");
        JQtLineEdit edit = new JQtLineEdit("");
        edit.setPlaceholderText("输入内容...");
        JQtButton ok = new JQtButton("确定");
        ok.onClick(() -> System.out.println("[Fluent] 输入：" + edit.text()));
        card2L.addWidget(card2Title);
        card2L.addWidget(edit);
        card2L.addWidget(ok);
        card2.setLayout(card2L);

        // ---- 主布局：标题栏 + 导航 + 卡片 ----
        JQtHBoxLayout body = new JQtHBoxLayout();
        body.setSpacing(12);
        body.addWidget(nav);
        JQtVBoxLayout cards = new JQtVBoxLayout();
        cards.setSpacing(12);
        cards.addWidget(card1);
        cards.addWidget(card2);
        cards.addStretch(1);
        body.addLayout(cards);
        body.addStretch(1);

        JQtVBoxLayout main = new JQtVBoxLayout();
        main.setSpacing(8);
        main.addWidget(titleBar);
        main.addLayout(body);
        window.setLayout(main);

        window.show();
        window.fadeIn(300);   // 窗口淡入动画

        long autoClose = Long.getLong("jqt.autoClose", -1L);
        if (autoClose > 0) {
            app.scheduleQuit(autoClose);
        }

        System.out.println("[Fluent] 事件循环开始");
        app.exec();
        System.out.println("[Fluent] 正常退出 ✅");
    }
}