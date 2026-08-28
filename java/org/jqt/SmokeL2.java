package org.jqt;
public class SmokeL2 {
    public static void main(String[] args) {
        QApplication app = new QApplication();
        QMainWindow w = new QMainWindow("SmokeL2", 500, 400);
        QPushButton b1 = new QPushButton("A");
        QVBoxLayout vb = new QVBoxLayout();
        vb.addWidget(b1);
        w.setLayout(vb);

        // 尺寸约束
        w.setMinimumSize(300, 200);
        w.setMaximumSize(900, 700);
        long min = w.minimumSize();
        long max = w.maximumSize();
        System.out.println("[l2] min=" + (int)(min >> 32) + "x" + (int)(min & 0xFFFFFFFFL)
            + " max=" + (int)(max >> 32) + "x" + (int)(max & 0xFFFFFFFFL));
        w.setFixedWidth(520);
        w.setFixedHeight(420);
        w.setMinimumSize(0, 0);
        w.setMaximumSize(16777215, 16777215);

        // 焦点
        w.show();
        b1.setFocus();
        System.out.println("[l2] hasFocus=" + b1.hasFocus());
        b1.clearFocus();

        // 鼠标跟踪
        b1.setMouseTracking(true);
        System.out.println("[l2] mouseTracking=" + b1.hasMouseTracking());

        // 窗口
        System.out.println("[l2] isActiveWindow=" + w.isActiveWindow() + " isFullScreen=" + w.isFullScreen()
            + " isMinimized=" + w.isMinimized());
        w.activateWindow();
        w.setWindowOpacity(0.9);
        System.out.println("[l2] opacity=" + String.format("%.1f", w.windowOpacity()));
        w.raise();
        w.lower();

        // 背景
        w.setAutoFillBackground(true);
        System.out.println("[l2] autoFill=" + w.autoFillBackground());

        // 键盘独占
        w.grabKeyboard();
        w.releaseKeyboard();
        System.out.println("[l2] keyboard grab/release ok");

        app.scheduleQuit(800);
        app.exec();
        System.out.println("[l2] PASS");
    }
}
