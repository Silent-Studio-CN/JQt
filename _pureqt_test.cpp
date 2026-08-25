#include <QApplication>
#include <QPushButton>
#include <QFile>
#include <QTextStream>

static void log_signal(const char* s) {
    QFile f("D:/SilentStudio/JQt - Dev/_pq_signals.txt");
    if (f.open(QIODevice::Append)) {
        QTextStream ts(&f);
        ts << s << "\n";
    }
}

int main(int argc, char* argv[]) {
    QApplication app(argc, argv);
    QPushButton btn("pure qt button");
    QObject::connect(&btn, &QPushButton::clicked, [](){ log_signal("clicked"); });
    QObject::connect(&btn, &QPushButton::pressed, [](){ log_signal("pressed"); });
    QObject::connect(&btn, &QPushButton::released, [](){ log_signal("released"); });
    btn.resize(300, 120);
    btn.move(200, 200);
    btn.show();
    log_signal("READY");
    return app.exec();
}