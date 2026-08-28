package org.jqt;
public class SmokeL1fin {
    public static void main(String[] args) {
        QApplication app = new QApplication();
        System.out.println("[l1f] QColor.value(#000000)=" + QColor.value("#000000")
            + " hue(#ff0000)=" + QColor.hue("#ff0000")
            + " saturation(#ff0000)=" + QColor.saturation("#ff0000")
            + " value(#00ff00)=" + QColor.value("#00ff00"));
        int t = QApplication.paletteText();
        int p = QApplication.palettePlaceholderText();
        System.out.println("[l1f] paletteText=0x" + Integer.toHexString(t) + " placeholder=0x" + Integer.toHexString(p));
        app.scheduleQuit(300);
        app.exec();
        System.out.println("[l1f] PASS");
    }
}
