// JQt Android C++ entry - called by QtLoader on the Qt thread.
// Creates the QApplication and the PoC window, then runs the Qt event loop.
// JQt is Java-driven: the Java side (JQtPocActivity) may create a second
// QApplication object later - jqt_bridge nativeCreateApp reuses g_app when
// it already exists.
// NOTE: ASCII-only.
#include <QApplication>
#include <QPushButton>

int main(int argc, char **argv) {
    QApplication app(argc, argv);
    QPushButton btn("Hello JQt on Android");
    btn.show();
    return app.exec();
}
