/*
 * JQt - Java bindings for Qt.
 * Copyright (c) 2025 SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.
 */
package org.jqt;

/**
 * JQt Phase 0 / Phase 1 演示程序。
 * <p>
 * 运行方式：{@code .\run.ps1}
 * 自动化演示：{@code .\run.ps1 -AutoClose 3000}（3 秒后自动关闭）
 */
public class JQtDemo {

    public static void main(String[] args) {
        System.out.println("[JQt] 创建 QApplication ...");

        JQtApplication app = new JQtApplication();

        JQtWindow window = new JQtWindow("JQt 第一个窗口", 800, 600);
        JQtLabel label = new JQtLabel("Hello, JQt! 这是用 Java 写的 Qt 界面");
        JQtButton button = new JQtButton("点我试试");

        // 信号槽：Qt clicked 信号 → JNI 回调 → Java lambda
        button.onClick(() -> {
            System.out.println("[JQt] ✅ 按钮被点击了！(C++ 信号 → JNI 回调 → Java)");
            label.setText("点击成功！这行文字是 Java 回调改的");
        });

        window.onClose(() -> System.out.println("[JQt] 窗口关闭"));

        window.addWidget(label);
        window.addWidget(button);
        window.show();

        // 自动化演示：-Djqt.autoClose=3000 表示 3 秒后自动关闭
        long autoClose = Long.getLong("jqt.autoClose", -1L);
        if (autoClose > 0) {
            System.out.println("[JQt] " + autoClose + " ms 后自动退出（jqt.autoClose）");
            app.scheduleQuit(autoClose);
        }

        System.out.println("[JQt] 进入 Qt 事件循环 ...");
        app.exec();
        System.out.println("[JQt] 事件循环结束，JQt 运行正常 ✅");
    }
}
