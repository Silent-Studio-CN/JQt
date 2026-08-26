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

        // ---- 动画主题（-Djqt.animTheme=fast|relaxed|off|default）----
        String animTheme = System.getProperty("jqt.animTheme", "default");
        switch (animTheme) {
            case "fast":    JQtApplication.setAnimationTheme(JQtAnimationTheme.FAST); break;
            case "relaxed": JQtApplication.setAnimationTheme(JQtAnimationTheme.RELAXED); break;
            case "off":     JQtApplication.setAnimationTheme(JQtAnimationTheme.OFF); break;
            default:        JQtApplication.setAnimationTheme(JQtAnimationTheme.DEFAULT); break;
        }
        System.out.println("[Fluent] 动画主题 = " + animTheme);

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
        card1.setObjectName("card");
        card1.setDropShadow(18, 70, 0, 3);   // 投影（QSS box-shadow 的替代）
        card1.setBorderRadius(16);           // 自定义圆角（API，与 QSS 并存）
        JQtVBoxLayout card1L = new JQtVBoxLayout();
        card1L.setSpacing(10);
        JQtLabel card1Title = new JQtLabel("开关组");
        card1Title.setStyleSheet("font-size: 14px; font-weight: bold;");
        card1L.addWidget(card1Title);
        JQtSwitch switch1 = new JQtSwitch(true);
        JQtSwitch switch2 = new JQtSwitch(false);
        switch1.onToggled(on -> System.out.println("[Fluent] 自动更新 = " + on));
        switch2.onToggled(on -> System.out.println("[Fluent] 开机自启 = " + on));
        JQtLabel switchRow = new JQtLabel("");
        // 深色主题下固定前景色（模板变量仅用于 setTheme 渲染，此处直接写字面值）
        switchRow.setStyleSheet("font-size: 13px; color: #e6e6e6;");
        card1L.addWidget(switchRow);
        card1L.addWidget(switch1);
        card1L.addWidget(switch2);
        card1.setLayout(card1L);
        // 开关行标签（联动演示）
        switch1.onToggled(on -> switchRow.setText("自动更新：" + (on ? "开" : "关") + "（滑块动画）"));
        switchRow.setText("自动更新：开（滑块动画）");

        // ---- 卡片 2：输入与按钮 ----
        JQtPanel card2 = new JQtPanel();
        card2.setObjectName("card");
        card2.setDropShadow(18, 70, 0, 3);
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

        // ---- 卡片 3：动画演示（easing + JQtAnimation）----
        JQtPanel card3 = new JQtPanel();
        card3.setObjectName("card");
        card3.setDropShadow(18, 70, 0, 3);
        JQtVBoxLayout card3L = new JQtVBoxLayout();
        card3L.setSpacing(10);
        JQtLabel card3Title = new JQtLabel("动画演示");
        card3Title.setStyleSheet("font-size: 14px; font-weight: bold;");
        JQtButton bounceBtn = new JQtButton("弹性移动 (OutBounce)");
        JQtButton colorBtn = new JQtButton("主题色");
        String[] accents = {"#4cc2ff", "#0078d4", "#9b59b6", "#2ecc71", "#e67e22"};
        final int[] ci = {0};
        colorBtn.onClick(() -> {
            ci[0] = (ci[0] + 1) % accents.length;
            app.setAccentColor(accents[ci[0]]);
            System.out.println("[Fluent] accent = " + accents[ci[0]]);
        });
        JQtButton themeToggle = new JQtButton("黑白切换");
        final boolean[] dark = {true};
        themeToggle.onClick(() -> {
            dark[0] = !dark[0];
            app.setTheme(dark[0] ? "fluent-dark" : "fluent-light");   // 自定义主题色自动保留
            System.out.println("[Fluent] theme = " + (dark[0] ? "dark" : "light"));
        });
        JQtButton fadePulse = new JQtButton("透明度脉冲 (InOutSine)");
        JQtLabel ball = new JQtLabel("●");
        ball.setStyleSheet("font-size: 20px; color: #4cc2ff;");
        card3L.addWidget(card3Title);
        card3L.addWidget(ball);
        card3L.addWidget(bounceBtn);
        card3L.addWidget(colorBtn);
        card3L.addWidget(themeToggle);
        card3L.addWidget(fadePulse);
        card3.setLayout(card3L);

        // 弹性移动：easing 重载
        bounceBtn.onClick(() -> {
            ball.animateMove(0, 40, 700, JQtEasing.OUT_BOUNCE);
            ball.animateMove(0, 0, 700, JQtEasing.OUT_BOUNCE);
        });
        // 无限循环上下浮动（windowOpacity 仅对顶层窗口有效，子控件改用位移动画）
        final int[] floatDir = {1};
        app.schedule(new Runnable() {
            @Override
            public void run() {
                ball.animateMove(0, 14 * floatDir[0], 700, JQtEasing.IN_OUT_SINE);
                floatDir[0] = -floatDir[0];
                app.schedule(this, 720);
            }
        }, 300);

        // ---- Pivot 选项卡（滑动指示器动画）----
        JQtPivot pivot = new JQtPivot();
        pivot.addItem("概览");
        pivot.addItem("详情");
        pivot.addItem("历史");
        pivot.onChanged(i -> System.out.println("[Fluent] pivot -> " + i));

        // ---- 主布局：标题栏 + Pivot + 导航 + 卡片 ----
        JQtHBoxLayout body = new JQtHBoxLayout();
        body.setSpacing(12);
        body.setContentsMargins(12, 0, 12, 10);   // 窗口边缘留白（不再贴边）
        body.addWidget(nav);
        JQtVBoxLayout cards = new JQtVBoxLayout();
        cards.setSpacing(12);
        cards.setContentsMargins(0, 4, 0, 0);
        cards.addWidget(card1);
        cards.addWidget(card2);
        cards.addWidget(card3);
        cards.addStretch(1);
        body.addLayout(cards);
        body.addStretch(1);

        JQtVBoxLayout main = new JQtVBoxLayout();
        main.setSpacing(8);
        main.addWidget(titleBar);
        main.addWidget(pivot);
        main.addLayout(body);
        window.setLayout(main);

        window.show();
        window.fadeIn(300);   // 窗口淡入动画

        // ---- Fluent 入场动画：卡片依次滑入 + 淡入（主题缩放时长）----
        app.schedule(() -> JQtAnimations.entrance(card1), 250);
        app.schedule(() -> JQtAnimations.entrance(card2), 340);
        app.schedule(() -> JQtAnimations.entrance(card3), 430);

        long autoClose = Long.getLong("jqt.autoClose", -1L);
        if (autoClose > 0) {
            app.scheduleQuit(autoClose);
        }

        System.out.println("[Fluent] 事件循环开始");
        app.exec();
        System.out.println("[Fluent] 正常退出 ✅");
    }
}