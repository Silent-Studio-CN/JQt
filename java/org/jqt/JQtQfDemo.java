/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 验证 JQt 作为 QSS 引擎能否直接渲染 qfluentwidgets（GPLv3）的完整样式。
 * <p>
 * 加载 themes/qf/qf-dark-jqt.qss（qf 全部 dark QSS 合并 + 类名映射到 JQt 实际类名，
 * 变量替换为 qf 默认主题色）。动画部分用 JQt 自带动画 API 演示
 * （qf 的动画是 Python 代码，QSS 里没有 transition）。
 * <p>
 * 运行：.\run.ps1 -Class org.jqt.JQtQfDemo
 */
public class JQtQfDemo {

    public static void main(String[] args) throws Exception {
        QApplication app = new QApplication();
        app.setStyle("Fusion");

        // ---- 加载 qf QSS（完整 34 文件合并 + 类名映射）----
        String qssPath = System.getProperty("jqt.qss",
                "themes/qf/qf-dark-jqt.qss");
        String qss = new String(Files.readAllBytes(Paths.get(qssPath)),
                StandardCharsets.UTF_8);
        app.setStyleSheet(qss);
        System.out.println("[Qf] 已加载 qf QSS: " + qssPath + " (" + qss.length() + " chars)");

        QMainWindow window = new QMainWindow("JQt x qfluentwidgets", 800, 560);
        window.setFrameless(true);
        window.setRoundedCorners(true);
        JQtTitleBar bar = new JQtTitleBar("JQt x qfluentwidgets QSS", window);

        // ---- 导航（qf NavigationPanel → QListWidget#nav）----
        QListWidget nav = new QListWidget();
        nav.setObjectName("nav");
        nav.addItem("首页");
        nav.addItem("设置");
        nav.addItem("关于");
        nav.setStyleSheet("font-size: 14px;");
        nav.onItemClicked(i -> System.out.println("[Qf] nav item " + i));

        // ---- 卡片 1：qf 按钮族 ----
        QFrame card1 = new QFrame();
        card1.setObjectName("card");
        QVBoxLayout c1 = new QVBoxLayout();
        c1.setSpacing(10);
        QPushButton primary = new QPushButton("主按钮");
        primary.setObjectName("primary");
        primary.onClicked(() -> System.out.println("[Qf] primary clicked"));
        QPushButton normal = new QPushButton("普通按钮");
        normal.onClicked(() -> System.out.println("[Qf] normal clicked"));
        QPushButton hyper = new QPushButton("超链接按钮");
        hyper.setObjectName("hyperlink");
        hyper.onClicked(() -> System.out.println("[Qf] hyperlink clicked"));
        c1.addWidget(primary);
        c1.addWidget(normal);
        c1.addWidget(hyper);
        card1.setLayout(c1);

        // ---- 卡片 2：qf 输入族 ----
        QFrame card2 = new QFrame();
        card2.setObjectName("card");
        QVBoxLayout c2 = new QVBoxLayout();
        c2.setSpacing(10);
        QLineEdit edit = new QLineEdit("");
        edit.setPlaceholderText("输入内容...");
        edit.onTextChanged(t -> System.out.println("[Qf] edit: " + t));
        QComboBox combo = new QComboBox();
        combo.addItem("选项 A");
        combo.addItem("选项 B");
        combo.addItem("选项 C");
        combo.onCurrentIndexChanged(i -> System.out.println("[Qf] combo -> " + i));
        QCheckBox cb = new QCheckBox("启用 qf 复选框");
        cb.onToggled(on -> System.out.println("[Qf] checkbox = " + on));
        c2.addWidget(edit);
        c2.addWidget(combo);
        c2.addWidget(cb);
        card2.setLayout(c2);

        // ---- 卡片 3：开关 + 动画（JQt 自绘开关不受 qf QSS 影响）----
        QFrame card3 = new QFrame();
        card3.setObjectName("card");
        QVBoxLayout c3 = new QVBoxLayout();
        c3.setSpacing(10);
        JQtSwitch sw = new JQtSwitch(true);
        sw.onToggled(on -> System.out.println("[Qf] switch = " + on));
        QPushButton bounce = new QPushButton("弹性移动 (OutBounce)");
        QLabel ball = new QLabel("●");
        ball.setStyleSheet("font-size: 22px; color: #009faa;");
        c3.addWidget(sw);
        c3.addWidget(ball);
        c3.addWidget(bounce);
        card3.setLayout(c3);

        bounce.onClicked(() -> {
            ball.animateMove(0, 60, 800, JQtEasing.OUT_BOUNCE);
            ball.animateMove(0, 0, 800, JQtEasing.OUT_BOUNCE);
        });
        JQtAnimation pulse = new JQtAnimation(ball, "windowOpacity", 1.0, 0.3, 1000, JQtEasing.IN_OUT_SINE);
        pulse.setLoopCount(-1);
        pulse.start();

        // ---- 布局 ----
        QHBoxLayout body = new QHBoxLayout();
        body.setSpacing(12);
        body.addWidget(nav);
        QVBoxLayout cards = new QVBoxLayout();
        cards.setSpacing(12);
        cards.addWidget(card1);
        cards.addWidget(card2);
        cards.addWidget(card3);
        cards.addStretch(1);
        body.addLayout(cards);
        body.addStretch(1);
        QVBoxLayout main = new QVBoxLayout();
        main.setSpacing(8);
        main.addWidget(bar);
        main.addLayout(body);
        window.setLayout(main);

        window.show();
        window.fadeIn(350, JQtEasing.OUT_CUBIC);   // JQt 动画：qf 样式窗口淡入

        long autoClose = Long.getLong("jqt.autoClose", -1L);
        if (autoClose > 0) {
            app.scheduleQuit(autoClose);
        }
        System.out.println("[Qf] 事件循环开始");
        app.exec();
        System.out.println("[Qf] 正常退出");
    }
}












