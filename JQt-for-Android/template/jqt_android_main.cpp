// JQt Android C++ entry - called by QtLoader on the Qt thread.
// Creates THE process-wide QApplication (bridge reuses it via jqtAndroidAttachApp)
// and runs the Qt event loop. The UI itself is driven from Java
// (JQtPocActivity polls QApplication.isQtReady() then runs on the Qt thread).
// NOTE: ASCII-only.
#include <QApplication>
#include <android/log.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "jqt", __VA_ARGS__)

extern "C" void jqtAndroidAttachApp(void* app);

int main(int argc, char **argv) {
    LOGI("main entry argc=%d", argc);
    QApplication* app = new QApplication(argc, argv);
    LOGI("QApplication created");
    jqtAndroidAttachApp(app);
    LOGI("attached, entering event loop");
    int rc = app->exec();
    LOGI("event loop exited rc=%d", rc);
    return rc;
}
