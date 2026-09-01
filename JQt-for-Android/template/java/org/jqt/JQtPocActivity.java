/*
 * JQt Android PoC entry activity.
 * Extends QtActivity: QtLoader loads libjqt and runs main() on the Qt thread,
 * which owns QApplication + the demo button + the event loop.
 * Java-side QApplication creation is intentionally deferred (race with Qt
 * thread startup; bridge reuses g_app once main() attaches it).
 * NOTE: ASCII-only (Android build pipeline).
 */
package org.jqt;

import android.os.Bundle;
import org.qtproject.qt.android.bindings.QtActivity;

public class JQtPocActivity extends QtActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Qt thread main() owns QApplication/button/loop for the PoC.
    }
}
