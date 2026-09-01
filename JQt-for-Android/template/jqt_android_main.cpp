// JQt Android C++ entry - called by QtLoader on the Qt thread.
// Creates THE process-wide QApplication (bridge reuses it via jqtAndroidAttachApp,
// so Java-side QApplication construction does not create a second instance),
// shows the PoC button, then runs the Qt event loop.
// NOTE: ASCII-only.
#include <QApplication>
#include <QPushButton>

extern "C" void jqtAndroidAttachApp(void* app);

int main(int argc, char **argv) {
    QApplication* app = new QApplication(argc, argv);
    jqtAndroidAttachApp(app);
    QPushButton btn("Hello JQt on Android");
    btn.show();
    return app->exec();
}
