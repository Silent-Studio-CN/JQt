// svg2png: render an SVG file to a PNG using Qt (QSvgRenderer)
// Keeps the source aspect ratio: scales to fit and centers.
#include <QGuiApplication>
#include <QSvgRenderer>
#include <QImage>
#include <QPainter>
#include <QDebug>

int main(int argc, char** argv) {
    QGuiApplication app(argc, argv);
    if (argc < 4) {
        fprintf(stderr, "usage: svg2png <in.svg> <out.png> <size>\n");
        return 1;
    }
    QSvgRenderer renderer(QString::fromLocal8Bit(argv[1]));
    const int size = atoi(argv[3]);
    const QRectF v = renderer.viewBoxF();   // source content box
    // fit inside the square, preserving aspect ratio
    const double scale = qMin(size / v.width(), size / v.height());
    const double w = v.width() * scale;
    const double h = v.height() * scale;
    const QRectF target((size - w) / 2.0, (size - h) / 2.0, w, h);
    fprintf(stderr, "[svg2png] viewBox %.2fx%.2f -> %dx%d (scale %.3f)\n",
            v.width(), v.height(), qRound(w), qRound(h), scale);
    QImage img(size, size, QImage::Format_ARGB32_Premultiplied);
    img.fill(Qt::transparent);
    QPainter p(&img);
    renderer.render(&p, target);
    p.end();
    return img.save(QString::fromLocal8Bit(argv[2])) ? 0 : 1;
}
