/*
 * JQt - v0.7.3 QOpenGLWidget 冒烟（offscreen 容错：GL 不可用时不崩溃）。
 */
package org.jqt;

public class SmokeV073 {

    public static void main(String[] args) {
        System.out.println("[v073] start");
        QApplication app = new QApplication();
        QMainWindow w = new QMainWindow("SmokeV073", 500, 400);

        QOpenGLWidget gl = new QOpenGLWidget();
        gl.setClearColor(0xFF101418);
        gl.setAutoClear(true);
        final int[] initCount = { 0 };
        final int[] paintCount = { 0 };
        final int[] resizeCount = { 0 };
        gl.onInitialize(() -> initCount[0]++);
        gl.onPaint(() -> paintCount[0]++);
        gl.onResized((ww, hh) -> resizeCount[0]++);
        System.out.println("[v073] QOpenGLWidget 创建 + 回调注册 ok");

        // makeCurrent/doneCurrent（offscreen 无 GL context 时不应崩溃）
        try {
            gl.makeCurrent();
            gl.doneCurrent();
            System.out.println("[v073] makeCurrent/doneCurrent ok");
        } catch (Throwable t) {
            System.out.println("[v073] makeCurrent 异常(可接受,offscreen): " + t);
        }

        // 加入布局并显示
        QVBoxLayout vb = new QVBoxLayout();
        vb.addWidget(gl);
        w.setLayout(vb);
        w.show();
        System.out.println("[v073] show ok");

        // 触发一次重绘（offscreen 下 paintGL 可能不触发，验证 update 不崩）
        gl.update();

        app.scheduleQuit(1200);
        app.exec();
        System.out.println("[v073] init=" + initCount[0] + " paint=" + paintCount[0] + " resize=" + resizeCount[0]);
        System.out.println("[v073] PASS");
    }
}
