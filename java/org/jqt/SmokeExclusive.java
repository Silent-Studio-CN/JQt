/*
 * JQt - v0.7.0 Exclusive Kit 冒烟测试（跨平台统一 API）。
 * 运行：java -cp out -Djqt.native=lib org.jqt.SmokeExclusive (Windows)
 */
package org.jqt;

public class SmokeExclusive {

    public static void main(String[] args) {
        System.out.println("[smoke] QApplication 创建...");
        QApplication app = new QApplication();

        // 防休眠（三平台）
        boolean p1 = QApplication.preventSleep(true);
        System.out.println("[smoke] preventSleep(true) = " + p1);
        boolean p2 = QApplication.preventSleep(false);
        System.out.println("[smoke] preventSleep(false) = " + p2);

        // 桌面通知
        boolean n1 = QApplication.showNotification("JQt 冒烟测试", "跨平台通知 API 调用成功", 2000);
        System.out.println("[smoke] showNotification = " + n1);

        // 开机自启（三平台；Windows 需可执行路径，这里传空串应返回 false 不崩溃）
        try {
            boolean a1 = QApplication.setAutoStart(true, "");
            System.out.println("[smoke] setAutoStart(true,'') = " + a1);
            boolean a2 = QApplication.setAutoStart(false, "");
            System.out.println("[smoke] setAutoStart(false,'') = " + a2);
        } catch (Throwable t) {
            System.out.println("[smoke] setAutoStart threw: " + t);
        }

        QMainWindow w = new QMainWindow("SmokeExclusive", 400, 300);
        // macOS 独家 API（非 mac 为 no-op）
        w.setDockBadge("5");
        w.clearDockBadge();
        w.setMacTitlebarTransparent(true);
        w.setMacFullSizeContentView(true);
        System.out.println("[smoke] dockBadge / macWindowAttribute 调用完成（非 mac 平台 no-op）");

        // 现有 Windows API 回归
        w.setTaskbarProgress(30, 100);
        w.clearTaskbarProgress();
        System.out.println("[smoke] taskbarProgress 回归 OK");

        w.show();
        app.scheduleQuit(1500);
        app.exec();
        System.out.println("[smoke] PASS");
    }
}
