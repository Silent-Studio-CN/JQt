/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * JQt Phase 0-4 演示程序：布局 + 信号槽 + 常用控件。
 * <p>
 * 运行方式：{@code .\run.ps1}
 * 自动化演示：{@code .\run.ps1 -AutoClose 5000}（5 秒后自动关闭）
 */
public class JQtDemo {

    public static void main(String[] args) {
        System.out.println("[JQt] 创建 QApplication ...");

        JQtApplication app = new JQtApplication();
        app.onAboutToQuit(() -> System.out.println("[JQt] aboutToQuit 信号触发（应用即将退出）"));

        // ---- QSS 样式（Qt Style Sheets，-Djqt.demoQss=0 关闭）----
        if (Long.getLong("jqt.demoQss", 1L) > 0) {
            app.setStyle("Fusion");
            app.setStyleSheet("""
                * { font-family: "Microsoft YaHei"; font-size: 13px; }
                QPushButton { background: #3c3f41; color: #ffffff; border: 1px solid #555555;
                              border-radius: 4px; padding: 6px 16px; }
                QPushButton:hover { background: #4c5052; }
                QPushButton:pressed { background: #2d2f31; }
                QLineEdit { background: #2d2f31; color: #ffffff; border: 1px solid #555555;
                            border-radius: 4px; padding: 4px 8px; }
                QComboBox { background: #3c3f41; color: #ffffff; border: 1px solid #555555;
                            border-radius: 4px; padding: 4px 8px; }
                QListWidget { background: #2d2f31; color: #dddddd; border: 1px solid #555555;
                              border-radius: 4px; }
                QLabel { color: #cccccc; }
                """);
            System.out.println("[JQt] QSS 样式已应用（Fusion 深色主题）");
        }

        JQtWindow window = new JQtWindow("JQt Phase 4 演示", 720, 600);
        JQtLabel label = new JQtLabel("JQt 控件 + 布局 + 信号槽演示");
        JQtLineEdit edit = new JQtLineEdit("");
        JQtComboBox combo = new JQtComboBox();
        JQtListWidget list = new JQtListWidget();
        JQtButton clickBtn = new JQtButton("点击我（clicked/pressed/released）");
        JQtButton checkBtn = new JQtButton("开关按钮（toggled）");
        checkBtn.setCheckable(true);

        // ---- 输入框（Phase 4）----
        edit.setPlaceholderText("输入文字，回车确认...");
        // 自动化演示 textChanged：2 秒后程序化填充文本（-Djqt.demoAutoEdit=0 关闭）
        if (Long.getLong("jqt.demoAutoEdit", 1L) > 0) {
            app.schedule(() -> edit.setText("JQt 自动填充（textChanged）"), 2000);
            // 2.5 秒后程序化切换下拉框选项（验证 currentIndexChanged）
            app.schedule(() -> combo.setCurrentIndex(1), 2500);
            // 3 秒后演示悬垂保护（Phase 5）：dispose 后调用 → IllegalStateException
            app.schedule(() -> {
                JQtButton ghost = new JQtButton("临时按钮");
                ghost.dispose();
                try {
                    ghost.setText("应该失败");
                    System.out.println("[JQt] ⚠️ 悬垂保护未生效！");
                } catch (IllegalStateException ex) {
                    System.out.println("[JQt] ✅ 悬垂保护：dispose 后调用抛异常 → " + ex.getMessage());
                }
            }, 3000);
        }
        edit.onTextChanged(text -> System.out.println("[JQt] textChanged → " + text));
        edit.onReturnPressed(() -> {
            System.out.println("[JQt] returnPressed：确认输入 '" + edit.text() + "'");
            label.setText("已输入：" + edit.text());
        });

        // ---- 下拉框（Phase 4）----
        combo.addItem("主题：深色");
        combo.addItem("主题：浅色");
        combo.addItem("主题：跟随系统");
        combo.onCurrentIndexChanged(index ->
                System.out.println("[JQt] currentIndexChanged → " + index + " (" + combo.currentText() + ")"));

        // ---- 列表（Phase 4）----
        list.addItem("列表项 1");
        list.addItem("列表项 2");
        list.addItem("列表项 3");
        list.onItemClicked(row -> System.out.println("[JQt] itemClicked → 第 " + row + " 行"));

        // ---- 按钮信号（Phase 2）----
        clickBtn.onClick(() -> System.out.println("[JQt] ✅ clicked 信号"));
        clickBtn.onPressed(() -> System.out.println("[JQt] pressed 信号"));
        clickBtn.onReleased(() -> System.out.println("[JQt] released 信号"));
        checkBtn.onToggled(checked -> System.out.println("[JQt] toggled 信号，选中状态 = " + checked));

        // ---- 窗口事件（Phase 2）----
        window.onResized((w, h) -> System.out.println("[JQt] window resized → " + w + "x" + h));
        window.onMoved((x, y) -> System.out.println("[JQt] window moved → " + x + "," + y));
        window.onClose(() -> System.out.println("[JQt] window close 事件"));

        // ---- Fluent 窗口模式（-Djqt.demoFluent=1：无边框 + 亚克力 + 圆角）----
        if (Long.getLong("jqt.demoFluent", 0L) > 0) {
            window.setFrameless(true);
            window.setAcrylic(true);
            window.setRoundedCorners(true);
            System.out.println("[JQt] Fluent 模式：无边框 + 亚克力 + 圆角（顶部可拖拽）");
        }

        // ---- 布局（Phase 3）----
        JQtVBoxLayout vbox = new JQtVBoxLayout();
        vbox.setSpacing(10);
        if (Long.getLong("jqt.demoFluent", 0L) > 0) {
            JQtButton closeBtn = new JQtButton("✕ 关闭窗口");
            closeBtn.onClick(() -> window.close());
            vbox.addWidget(closeBtn);
        }
        vbox.addWidget(label);
        vbox.addWidget(edit);
        vbox.addWidget(combo);
        vbox.addWidget(list);
        vbox.addWidget(clickBtn);
        vbox.addWidget(checkBtn);
        vbox.addStretch(1);
        window.setLayout(vbox);

        window.show();

        // 自动化演示：
        //   -Djqt.autoClose=5000    5 秒后自动退出
        //   -Djqt.demoResize=0      关闭 resize 演示（默认开启）
        long autoClose = Long.getLong("jqt.autoClose", -1L);
        if (autoClose > 0) {
            System.out.println("[JQt] " + autoClose + " ms 后自动退出（jqt.autoClose）");
            if (Long.getLong("jqt.demoResize", 1L) > 0) {
                // 1 秒后演示 resize 事件（Qt 定时器 → Java 回调）
                app.schedule(() -> {
                    System.out.println("[JQt] 定时器触发：resize 到 800x640");
                    window.resize(800, 640);
                }, 1000);
            }
            app.scheduleQuit(autoClose);
        }

        System.out.println("[JQt] 进入 Qt 事件循环 ...");
        app.exec();
        System.out.println("[JQt] 事件循环结束，JQt 运行正常 ✅");
    }
}
