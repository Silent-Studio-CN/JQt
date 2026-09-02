// JQt Android C++ entry - called by QtLoader on the Qt thread.
// Creates THE process-wide QApplication (bridge reuses it via jqtAndroidAttachApp),
// shows the PoC button, then runs the Qt event loop.
// NOTE: ASCII-only.
#include <QApplication>
#include <QPushButton>
#include <android/log.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "jqt", __VA_ARGS__)

extern "C" void jqtAndroidAttachApp(void* app);

int main(int argc, char **argv) {
    LOGI("main entry argc=%d", argc);
    QApplication* app = new QApplication(argc, argv);
    LOGI("QApplication created");
    jqtAndroidAttachApp(app);
    QPushButton* btn = new QPushButton("Hello JQt on Android");
    static int clicks = 0;
    QObject::connect(btn, &QPushButton::clicked, [btn]() {
        ++clicks;
        btn->setText(QString("Clicked! %1").arg(clicks));
        LOGI("button clicked, count=%d", clicks);
    });
    btn->show();
    LOGI("button shown");
    int rc = app->exec();
    LOGI("event loop exited rc=%d", rc);
    return rc;
}
