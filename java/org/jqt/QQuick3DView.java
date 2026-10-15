/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.io.IOException;
import java.lang.ref.Cleaner;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 承载 Qt Quick 3D 场景的视图(内部就是 {@link QQuickView} 的同款机制)。
 *
 * <p>Quick 3D 场景是纯 QML({@code import QtQuick3D} + {@code View3D {...}}),
 * 所以本类不新增独立的 native 视图类型:直接继承 {@link QQuickView},
 * 复用它的 native 窗口/场景图/抓图实现,只在 Java 侧补三件 Quick 3D 场景常用的事:
 * <ol>
 *   <li>{@link #loadQml(String)} —— 把 QML 文本写进<b>临时文件</b>再 {@code setSource()}
 *       (与 JQt 里 QQuickView 加载内联 QML 的做法一致:走文件路径,避免
 *       {@code QQmlComponent::setData} 在离屏/无窗口时拿不到内容的问题):</li>
 *   <li>{@link #isReady()} / {@link #errorString()} —— 一眼看"场景到底加载成功没有、为什么失败"
 *       ({@link QQuickView#errors()} 走的是状态回调、拿不到文本,这里改为直接读
 *       C++ 侧 {@code QQuickView::errors()},能给出
 *       {@code module "QtQuick3D" is not installed} 这类原文);</li>
 *   <li>{@link #dispose()} —— 隐藏 + 释放 native 视图 + 删除临时 QML(幂等)。</li>
 * </ol>
 *
 * <pre>
 * QQuick3DView view = new QQuick3DView(640, 480);
 * view.loadQml("import QtQuick\n" +
 *              "import QtQuick3D\n" +
 *              "View3D {\n" +
 *              "    environment: SceneEnvironment { backgroundMode: SceneEnvironment.Color; clearColor: \"#1e3a8a\" }\n" +
 *              "    PerspectiveCamera { position: Qt.vector3d(0, 0, 400) }\n" +
 *              "    Model { source: \"#Cube\"; materials: PrincipledMaterial { baseColor: \"#f59e0b\";\n" +
 *              "                                                          lighting: PrincipledMaterial.NoLighting } }\n" +
 *              "}\n");
 * view.resize(640, 480);
 * view.show();                       // 真窗口
 * byte[] png = view.grabToPng();     // 或离屏抓图(需要可用的 RHI,软件后端渲染不出 3D)
 * if (!view.isReady()) System.out.println(view.errorString());
 * view.dispose();
 * </pre>
 *
 * <p><b>平台差异</b>
 * <ul>
 *   <li><b>Windows</b>:默认 RHI 是 D3D11(含 WARP 软件光栅,无独显也能出画面);
 *       {@code QSG_RHI_BACKEND=opengl} 可切 OpenGL。</li>
 *   <li><b>Linux</b>:CI/无头环境需要 xvfb 之类的 X 服务 + 可用的 GL
 *       ({@code QT_QPA_PLATFORM=xcb},或 Mesa 的 llvmpipe);纯
 *       {@code QT_QPA_PLATFORM=offscreen} 通常建不起 3D 渲染上下文。</li>
 *   <li><b>macOS</b>:Metal 后端,需真图形会话;离屏 CI 一般拿不到画面。</li>
 *   <li><b>软件后端</b>({@code QT_QUICK_BACKEND=software})不支持 Quick 3D:
 *       场景能加载,但抓到的图是空的 —— 请不要把它当成"渲染成功"。</li>
 * </ul>
 *
 * <p><b>线程</b>:与 {@link QQuickView} 一致,只在 Qt GUI 线程使用。
 */
public class QQuick3DView extends QQuickView {

    /** 临时 QML 文件的 GC 兜底清理(不持有视图引用,否则永远不会被回收)。 */
    private static final Cleaner FILE_CLEANER = Cleaner.create();

    /** 当前临时 QML 文件;null = 还没 loadQml。 */
    private volatile Path qmlFile;
    /** 是否已 dispose(dispose 幂等;之后所有操作都拒绝/返回空)。 */
    private volatile boolean disposed;

    /**
     * 创建视图。
     *
     * <p>注意:这里<b>不</b>要求 {@link QQuick3D#isAvailable()} 为真 —— Quick 3D 不可用时
     * 也可以建视图并加载 QML(便于把真实错误读出来给用户看),
     * 只是 {@link #isReady()} 会是 {@code false}、{@link #errorString()} 会给出原因。
     * 但 QtQuick 本身必须可用(否则 {@link QQuickView} 构造器会抛
     * {@link IllegalStateException})。
     *
     * @param width  视图宽(像素,最小 1)
     * @param height 视图高(像素,最小 1)
     */
    public QQuick3DView(int width, int height) {
        super();
        resize(Math.max(1, width), Math.max(1, height));
    }

    /**
     * 加载 QML 文本:写成临时文件({@code %TEMP%/jqt-q3d-xxxx.qml},UTF-8)再 {@code setSource()}。
     *
     * <p>写文件的理由:Qt 对"本地文件"的加载是同步的,{@link QQuickView#status()} 立刻可读
     * (探测与冒烟都依赖这一点);同时文件所在目录会成为该 QML 的相对导入基准,
     * 因此 QML 里引用相对资源时请写<b>绝对路径</b>,或改用
     * {@link QQuickView#setSource(String)} 直接指向真实文件(那样本类不会删你的文件)。
     *
     * <p>临时文件的生命周期:上一次 {@code loadQml} 的文件会被立即删除,
     * 本次的文件在 {@link #dispose()} 时删除,若忘记 dispose 则由 GC 兜底删除。
     *
     * @param qmlText QML 源文本(UTF-8);不能为 null
     * @throws IllegalStateException    视图已 dispose
     * @throws IllegalArgumentException qmlText 为 null
     */
    public void loadQml(String qmlText) {
        if (qmlText == null) throw new IllegalArgumentException("qmlText 不能为 null");
        if (disposed) throw new IllegalStateException("QQuick3DView 已 dispose,不能再 loadQml");
        final Path file = writeTempQml(qmlText);
        deleteTempQml();                        // 删上一次的
        qmlFile = file;
        FILE_CLEANER.register(this, () -> deleteQuietly(file));
        setSource(file.toAbsolutePath().toString());
    }

    /**
     * 场景是否加载成功({@link QQuickView.Status#Ready})。
     * 注意:只表示 <b>QML 加载成功</b>,不表示画面已经渲染出来(那取决于 RHI/GPU)。
     */
    public boolean isReady() {
        return !disposed && status() == Status.Ready;
    }

    /**
     * 加载失败原因(无错误时为空串)。多行错误用 {@code \n} 连接。
     *
     * <p>覆盖了 {@link QQuickView#errors()}:基类版本的文本来自状态回调,而回调只转发状态码,
     * 实际几乎总是空串;这里直接读 C++ 侧的 {@code QQuickView::errors()},能拿到
     * {@code module "QtQuick3D" is not installed}、
     * {@code Cannot load library .../qquick3dplugin.dll} 这类可操作的原文。
     */
    public String errorString() {
        if (disposed) return "QQuick3DView 已 dispose";
        String[] errs = QQuick3D.qmlErrors(nativeHandle());
        if (errs.length == 0) return super.errors();    // 回退基类(通常为空串)
        return String.join("\n", errs);
    }

    /** 同 {@link #errorString()}(让基类的 {@code errors()} 也返回真实文本)。 */
    @Override
    public String errors() {
        if (disposed) return "";
        return errorString();
    }

    /**
     * 把当前画面抓成 PNG 字节({@link QQuickView#grabToPng()} 同款实现:
     * 未 show 时先 show 再抓,所以离屏/真窗口都能用)。
     *
     * <p>必须有可用的 RHI 才有画面:软件后端
     * ({@code QT_QUICK_BACKEND=software})下拿到的是不含 3D 内容的空场景图。
     *
     * @return PNG 字节;已 dispose 或抓图失败返回 null
     */
    @Override
    public byte[] grabToPng() {
        if (disposed) return null;
        return super.grabToPng();
    }

    /**
     * 提前释放:隐藏窗口 → 释放 native 视图(幂等)→ 删除临时 QML。
     *
     * <p>实现说明:native 侧用的是"先从句柄注册表摘除、再排队 delete"的写法
     * (与 JQt 的 {@code QWidget/QValidator.dispose()} 一致),所以
     * 重复调用安全;基类 {@link QQuickView} 的 Cleaner 之后拿着同一个句柄再调一次时,
     * native 侧查不到句柄会静默返回(不会重复 delete、也不会抛到用户面前)。
     * 之后本对象视为失效:{@link #isReady()} 为 false、{@link #grabToPng()} 返回 null。
     */
    public void dispose() {
        if (disposed) return;
        disposed = true;
        hide();                              // 先藏窗口,避免屏幕上残留
        nativeDispose(nativeHandle());
        deleteTempQml();
    }

    /** 是否已 dispose。 */
    public boolean isDisposed() {
        return disposed;
    }

    /** 当前临时 QML 文件路径;没 loadQml 或已删除时返回空串。 */
    public String qmlFile() {
        Path f = qmlFile;
        return (f == null || !Files.exists(f)) ? "" : f.toAbsolutePath().toString();
    }

    @Override
    public String toString() {
        String err = disposed ? "" : errorString();
        return "QQuick3DView(" + width() + "x" + height()
             + " ready=" + isReady() + " status=" + QQuick3D.statusName(status())
             + (err.isEmpty() ? "" : " errors='" + err + "'")
             + (disposed ? " disposed" : "") + ")";
    }

    /**
     * 释放 native 视图(桥里 {@code Java_org_jqt_QQuick3DView_nativeDispose}):
     * 句柄已消失时静默返回,保证可重复调用。
     */
    private static native void nativeDispose(long handle);

    // ------------------------------------------------------------------
    // 临时文件
    // ------------------------------------------------------------------

    /** 把 QML 文本写成 UTF-8 临时文件(唯一文件名)。 */
    private static Path writeTempQml(String qmlText) {
        try {
            Path file = Files.createTempFile("jqt-q3d-", ".qml");
            Files.write(file, qmlText.getBytes(StandardCharsets.UTF_8));
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("写入临时 QML 文件失败: " + e, e);
        }
    }

    private void deleteTempQml() {
        Path f = qmlFile;
        qmlFile = null;
        if (f != null) deleteQuietly(f);
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // 临时文件删不掉不影响功能(QML 引擎已把内容读进内存),静默
        }
    }
}
