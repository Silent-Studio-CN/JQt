/*
 * JQt Android PoC entry activity.
 * Extends QtActivity: Qt runtime is loaded by QtLoader; after that we
 * create the JQt QApplication via JNI and show a window on the Qt thread.
 * NOTE: ASCII-only (Android build pipeline).
 */
package org.jqt;

import android.os.Bundle;
import org.qtproject.qt.android.bindings.QtActivity;

public class JQtPocActivity extends QtActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Qt libraries are loaded by QtLoader before onCreate returns;
        // schedule JQt startup on the Qt thread.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    QApplication app = new QApplication();
                    QPushButton btn = new QPushButton("Hello JQt on Android");
                    btn.show();
                    System.out.println("[jqt-poc] QApplication + button created");
                } catch (Throwable t) {
                    System.out.println("[jqt-poc] FAILED: " + t);
                    t.printStackTrace();
                }
            }
        });
    }
}
