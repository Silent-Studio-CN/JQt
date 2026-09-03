/*
 * JQt Android PoC - Java-driven UI entry.
 * Waits for Qt main() (QApplication created + attached to the bridge), then
 * builds the whole UI from Java on the Qt thread:
 *   new QApplication() -> reuses main()'s instance (g_app guard)
 *   QWidget + QPushButton + layout + onClicked callback (Java -> JNI -> Qt -> Java)
 * NOTE: ASCII-only.
 */
package org.jqt;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import org.qtproject.qt.android.bindings.QtActivity;

public class JQtPocActivity extends QtActivity {

    private static final int MAX_WAIT_MS = 30000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tryStartJavaDemo(0);
    }

    /** Poll Qt readiness. QtLoader dlopens libjqt asynchronously (can take seconds);
     *  until then static native lookups throw UnsatisfiedLinkError - keep polling. */
    private void tryStartJavaDemo(final int elapsedMs) {
        boolean ready = false;
        try {
            ready = QApplication.isQtReady();
            if (elapsedMs == 0) System.out.println("[jqt-poc] isQtReady=" + ready);
        } catch (UnsatisfiedLinkError e) {
            if (elapsedMs == 0) System.out.println("[jqt-poc] link pending: " + e);
            ready = false;   // libjqt not yet loaded by QtLoader
        }
        if (ready) {
            QApplication.runOnQtThread(() -> {
                try {
                    QApplication app = new QApplication();   // reuses main()'s QApplication
                    QMainWindow win = new QMainWindow("JQt on Android", 720, 1280);
                    QPushButton btn = new QPushButton("Java button");
                    btn.onClicked(() -> System.out.println("[jqt-poc] Java clicked"));
                    QVBoxLayout layout = new QVBoxLayout();
                    layout.addWidget(btn);
                    win.setLayout(layout);
                    win.show();
                    System.out.println("[jqt-poc] Java-driven UI ready");
                } catch (Throwable t) {
                    System.out.println("[jqt-poc] FAILED: " + t);
                    t.printStackTrace();
                }
            });
        } else if (elapsedMs < MAX_WAIT_MS) {
            new Handler(Looper.getMainLooper())
                    .postDelayed(() -> tryStartJavaDemo(elapsedMs + 500), 500);
        } else {
            System.out.println("[jqt-poc] Qt not ready within " + MAX_WAIT_MS + "ms");
        }
    }
}