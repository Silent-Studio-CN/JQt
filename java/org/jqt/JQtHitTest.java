/* 诊断：PostMessage 真实消息链命中测试。 */
package org.jqt;

public class JQtHitTest {

    public static void main(String[] args) throws Exception {
        JQtApplication app = new JQtApplication();
        app.setStyle("Fusion");
        app.setTheme("fluent-dark");

        JQtWindow window = new JQtWindow("HitTest", 520, 380);
        window.setFrameless(true);
        JQtTitleBar bar = new JQtTitleBar("HitTest", window);

        // 卡片（QFrame）里的开关和按钮
        JQtPanel card = new JQtPanel();
        JQtVBoxLayout cl = new JQtVBoxLayout();
        cl.setSpacing(10);
        final JQtSwitch sw = new JQtSwitch(false);
        sw.onToggled(on -> System.out.println("[HitTest] switch -> " + on));
        final JQtButton btn = new JQtButton("OK");
        btn.onClick(() -> System.out.println("[HitTest] button clicked"));
        cl.addWidget(sw);
        cl.addWidget(btn);
        card.setLayout(cl);

        // 列表（窗口级布局）
        JQtListWidget list = new JQtListWidget();
        list.addItem("A");
        list.addItem("B");
        list.onItemClicked(i -> System.out.println("[HitTest] list item " + i));

        JQtVBoxLayout main = new JQtVBoxLayout();
        main.setSpacing(8);
        main.addWidget(bar);
        main.addWidget(card);
        main.addWidget(list);
        window.setLayout(main);

        window.show();
        app.schedule(() -> {
            System.out.println("[HitTest] postClick switch");
            JQtWidget.nativePostClickAt(sw.nativeHandle(), window.nativeHandle());
        }, 1200);
        app.schedule(() -> {
            System.out.println("[HitTest] postClick button");
            JQtWidget.nativePostClickAt(btn.nativeHandle(), window.nativeHandle());
        }, 1800);
        app.schedule(() -> {
            System.out.println("[HitTest] postClick list");
            JQtWidget.nativePostClickAt(list.nativeHandle(), window.nativeHandle());
        }, 2400);
        app.scheduleQuit(4000);
        app.exec();
        System.out.println("[HitTest] done");
    }
}
