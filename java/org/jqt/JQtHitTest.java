/* 诊断：PostMessage 真实消息链命中测试。 */
package org.jqt;

public class JQtHitTest {

    public static void main(String[] args) throws Exception {
        QApplication app = new QApplication();
        app.setStyle("Fusion");
        app.setTheme("fluent-dark");

        QMainWindow window = new QMainWindow("HitTest", 520, 380);
        window.setFrameless(true);
        JQtTitleBar bar = new JQtTitleBar("HitTest", window);

        // 卡片（QFrame）里的开关和按钮
        QFrame card = new QFrame();
        QVBoxLayout cl = new QVBoxLayout();
        cl.setSpacing(10);
        final JQtSwitch sw = new JQtSwitch(false);
        sw.onToggled(on -> System.out.println("[HitTest] switch -> " + on));
        final QPushButton btn = new QPushButton("OK");
        btn.onClicked(() -> System.out.println("[HitTest] button clicked"));
        cl.addWidget(sw);
        cl.addWidget(btn);
        card.setLayout(cl);

        // 列表（窗口级布局）
        QListWidget list = new QListWidget();
        list.addItem("A");
        list.addItem("B");
        list.onItemClicked(i -> System.out.println("[HitTest] list item " + i));

        QVBoxLayout main = new QVBoxLayout();
        main.setSpacing(8);
        main.addWidget(bar);
        main.addWidget(card);
        main.addWidget(list);
        window.setLayout(main);

        window.show();
        app.schedule(() -> {
            System.out.println("[HitTest] postClick switch");
            QWidget.nativePostClickAt(sw.nativeHandle(), window.nativeHandle());
        }, 1200);
        app.schedule(() -> {
            System.out.println("[HitTest] postClick button");
            QWidget.nativePostClickAt(btn.nativeHandle(), window.nativeHandle());
        }, 1800);
        app.schedule(() -> {
            System.out.println("[HitTest] postClick list");
            QWidget.nativePostClickAt(list.nativeHandle(), window.nativeHandle());
        }, 2400);
        app.scheduleQuit(4000);
        app.exec();
        System.out.println("[HitTest] done");
    }
}








