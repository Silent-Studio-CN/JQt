/*
 * JQt - FLUENT 变量表崩溃复现（用户报告：0.7.5 访问 FLUENT 变量表 exit 1 无输出）。
 */
package org.jqt;

import java.util.Map;

public class SmokeFluent {

    public static void main(String[] args) {
        System.out.println("[fluent] start");
        QApplication app = new QApplication();

        // 1. 纯 Java 访问变量表
        System.out.println("[fluent] FLUENT_DARK size=" + QApplication.FLUENT_DARK.size());
        System.out.println("[fluent] FLUENT_LIGHT size=" + QApplication.FLUENT_LIGHT.size());
        System.out.println("[fluent] accent=" + QApplication.FLUENT_DARK.get("accent"));

        // 2. 模拟 renderTheme：模板 + 变量表 → setStyleSheet
        StringBuilder tpl = new StringBuilder("QWidget { background-color: %window%; color: %text%; }");
        for (Map.Entry<String, String> e : QApplication.FLUENT_DARK.entrySet()) {
            String k = tpl.toString();
            tpl = new StringBuilder(k.replace("%" + e.getKey() + "%", e.getValue()));
        }
        String qss = tpl.toString();
        System.out.println("[fluent] qss head=" + qss.substring(0, Math.min(80, qss.length())));
        app.setStyleSheet(qss);
        System.out.println("[fluent] setStyleSheet(渲染后) OK");

        // 3. 含残留占位符的 QSS（模拟变量表缺键）
        app.setStyleSheet("QWidget { background-color: %missing_var%; }");
        System.out.println("[fluent] setStyleSheet(占位符残留) OK");

        // 4. 官方 setTheme 路径
        app.setTheme("fluent-dark");
        System.out.println("[fluent] setTheme(fluent-dark) OK");

        System.out.println("[fluent] ALL PASS");
        app.quit();
    }
}
