/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * JQt Phase 0-3 演示程序：布局管理器 + 信号槽补全。
 * <p>
 * 运行方式：{@code .\run.ps1}
 * 自动化演示：{@code .\run.ps1 -AutoClose 5000}（5 秒后自动关闭）
 */
public class JQtDemo {

    public static void main(String[] args) {
        System.out.println("[JQt] 创建 QApplication ...");

        JQtApplication app = new JQtApplication();
        app.onAboutToQuit(() -> System.out.println("[JQt] aboutToQuit 信号触发（应用即将退出）"));

        JQtWindow window = new JQtWindow("JQt Phase 2+3 演示", 640, 480);
        JQtLabel label = new JQtLabel("JQt 布局 + 信号槽演示");
        JQtButton clickBtn = new JQtButton("点击我（clicked/pressed/released）");
        JQtButton checkBtn = new JQtButton("开关按钮（toggled）");
        checkBtn.setCheckable(true);

        // ---- Phase 2：信号槽（均可注册多个监听器）----
        clickBtn.onClick(() -> {
            System.out.println("[JQt] ✅ clicked 信号（C++ → JNI → Java）");
            label.setText("已点击！clicked 信号触发");
        });
        clickBtn.onPressed(() -> System.out.println("[JQt] pressed 信号"));
        clickBtn.onReleased(() -> System.out.println("[JQt] released 信号"));
        checkBtn.onToggled(checked -> System.out.println("[JQt] toggled 信号，选中状态 = " + checked));

        window.onResized((w, h) -> System.out.println("[JQt] window resized → " + w + "x" + h));
        window.onMoved((x, y) -> System.out.println("[JQt] window moved → " + x + "," + y));
        window.onClose(() -> System.out.println("[JQt] window close 事件"));

        // ---- Phase 3：布局管理器 ----
        JQtVBoxLayout vbox = new JQtVBoxLayout();
        vbox.setSpacing(12);
        vbox.addWidget(label);
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
                    System.out.println("[JQt] 定时器触发：resize 到 800x500");
                    window.resize(800, 500);
                }, 1000);
            }
            app.scheduleQuit(autoClose);
        }

        System.out.println("[JQt] 进入 Qt 事件循环 ...");
        app.exec();
        System.out.println("[JQt] 事件循环结束，JQt 运行正常 ✅");
    }
}
