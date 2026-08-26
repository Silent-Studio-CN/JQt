// svg2png: render an SVG file to a PNG using Qt (QSvgRenderer)
// Keeps aspect ratio: scaled to fit, centered. Optional fill ratio < 1.0
// leaves transparent padding (useful for square avatar crops).
// usage: svg2png <in.svg> <out.png> <size> [fill]
#include <QGuiApplication>
#include <QSvgRenderer>
#include <QImage>
#include <QPainter>

int main(int argc, char** argv) {
    QGuiApplication app(argc, argv);
    if (argc < 4) {
        fprintf(stderr, "usage: svg2png <in.svg> <out.png> <size> [fill]\n");
        return 1;
    }
    QSvgRenderer renderer(QString::fromLocal8Bit(argv[1]));
    const int size = atoi(argv[3]);
    const double fill = argc > 4 ? atof(argv[4]) : 1.0;
    const QRectF v = renderer.viewBoxF();
    const double scale = qMin(size / v.width(), size / v.height()) * fill;
    const double w = v.width() * scale;
    const double h = v.height() * scale;
    const QRectF target((size - w) / 2.0, (size - h) / 2.0, w, h);
    fprintf(stderr, "[svg2png] viewBox %.2fx%.2f -> %dx%d fill %.2f\n",
            v.width(), v.height(), qRound(w), qRound(h), fill);
    QImage img(size, size, QImage::Format_ARGB32_Premultiplied);
    img.fill(Qt::transparent);
    QPainter p(&img);
    renderer.render(&p, target);
    p.end();
    return img.save(QString::fromLocal8Bit(argv[2])) ? 0 : 1;
}
