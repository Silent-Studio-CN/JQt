// JQt Android C++ entry - called by QtLoader on the Qt thread.
// Creates THE process-wide QApplication (bridge reuses it via jqtAndroidAttachApp,
// so Java-side QApplication construction does not create a second instance),
// shows the PoC button, then runs the Qt event loop.
// NOTE: ASCII-only.
#include <QApplication>
#include <QPushButton>
#include <cstdio>

extern "C" void jqtAndroidAttachApp(void* app);

int main(int argc, char **argv) {
    fprintf(stderr, "[jqt-main] entry argc=%d\n", argc);
    QApplication* app = new QApplication(argc, argv);
    fprintf(stderr, "[jqt-main] QApplication created\n");
    jqtAndroidAttachApp(app);
    QPushButton btn("Hello JQt on Android");
    btn.show();
    fprintf(stderr, "[jqt-main] button shown, entering event loop\n");
    return app->exec();
}
