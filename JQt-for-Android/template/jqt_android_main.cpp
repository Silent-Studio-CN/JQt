# JQt Android C++ entry stub - Qt Android plugin calls main() of the app lib.
# JQt is Java-driven: QApplication is created via JNI (nativeCreateApp) from
# Java; the Qt event loop on Android is managed by the QtActivity plugin.
# This stub only initializes the Qt runtime so Java-side JNI calls succeed.
# NOTE: ASCII-only.
#include <QGuiApplication>

extern "C" int main(int argc, char** argv) {
    // Qt Android plugin already set up the platform integration before main();
    // QGuiApplication here is a no-op instance that keeps Qt runtime alive for
    // Java-side native calls (nativeCreateApp reuses this instance).
    QGuiApplication app(argc, argv);
    return app.exec();
}
