/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * Qt Quick 3D 可用性门面(v1.9.2 P2)。
 *
 * <p><b>先理解一件事</b>:Qt Quick 3D 的入口 {@code View3D} 本身就是 QML 类型
 * ({@code import QtQuick3D}),承载它的窗口仍是普通 {@link QQuickView}。
 * 也就是说"用 QML 加载 Quick 3D 场景"并不需要任何 Quick3D 的 C++ API ——
 * {@link QQuick3DView#loadQml(String)} 把 QML 写进临时文件再 {@code setSource()}
 * 就够了。因此本类与 {@link QQuick3DView} 都不会让 jqt 对 QtQuick3D 产生
 * 编译期/链接期依赖(桥里没有 include 任何 QtQuick3D 头,也没链 Qt6Quick3D)。
 *
 * <p><b>可用性是运行期结论</b>,取决于三件事,缺一不可:
 * <ol>
 *   <li>本库编译了 QtQuick({@code JQT_HAVE_QML}),可以用 {@link QQuickView#isAvailable()} 判断;</li>
 *   <li>Qt 的 <b>QML 导入路径</b>里能找到 QtQuick3D 这个 QML 模块
 *       ({@code <导入路径>/QtQuick3D/qmldir} + 插件 {@code qquick3dplugin});
 *       只把 Qt 的 DLL 拷到 lib 是不够的 —— 还要带上 qml 目录,或用环境变量
 *       {@code QML2_IMPORT_PATH} 指过去;</li>
 *   <li>该插件的依赖能被真正加载:{@code Qt6Quick3D.dll} →
 *       {@code Qt6Quick3DRuntimeRender.dll} → <b>Qt Shader Tools</b> 的
 *       {@code Qt6ShaderTools.dll}。少一个就是
 *       "Cannot load library .../qquick3dplugin.dll"(本机 Qt 6.12.0 MinGW 实测就缺
 *       {@code Qt6ShaderTools.dll})。</li>
 * </ol>
 * 这三件事都无法在编译期判定,所以 {@link #isAvailable()} <b>不看编译期宏</b>,
 * 而是真的加载一段 {@code import QtQuick3D} 的 QML 并读加载状态
 * ({@link QQuickView} 对本地文件的加载是同步的,状态立刻可读)。
 * 构建脚本里的 {@code -DJQT_HAVE_QUICK3D}(若定义了)只在 {@link #status()} 里
 * 作为信息展示,见 {@link #compiledIn()}。
 *
 * <p><b>渲染还需要 GPU/RHI</b>:Qt Quick 3D 不支持 Qt Quick 的软件后端
 * ({@code QT_QUICK_BACKEND=software}),软件后端下场景能加载但没有画面。
 * 离屏/CI 要么有可用的 RHI(Windows 上 D3D11,含 WARP 软件光栅;
 * 或 {@code QSG_RHI_BACKEND=opengl}),要么只能渲染出空图 ——
 * {@link SmokeQuick3D} 对"加载成功但画面为空"按 SKIP 处理,不假通过。
 *
 * <p><b>Windows 上确切结论(6.12.0 MinGW 实测,别再猜)</b>:{@code QT_QPA_PLATFORM=offscreen}
 * <b>渲染不出 Quick 3D</b> —— offscreen 平台插件没有任何 GL/RHI 表面
 * ({@code qoffscreen.dll} 只导入 Qt6Core/Qt6Gui,不链接 opengl32/D3D),Qt Quick 会直接退到
 * 软件后端(日志:{@code qt.scenegraph.general: Loading backend software}),View3D 不显示任何东西;
 * 强行 {@code QT_QUICK_BACKEND=rhi QSG_RHI_BACKEND=d3d11} 虽然建起了 D3D11 QRhi,但没有 HWND
 * 建不了 swapchain({@code Failed to create D3D11 swapchain: COM error 0x80070005}),抓到的图是纯黑。
 * 结论:<b>要证明 3D 真的渲染出来,必须用真窗口</b>({@code QT_QPA_PLATFORM=windows} 或干脆不设;
 * 无独显时 D3D11 会走 WARP)。CI/无头场景要么给真窗口(Windows 计划任务/交互会话),
 * 要么用 {@code QQuickRenderControl} 自己渲染到纹理 —— 后者 JQt 目前没有绑定。
 *
 * <p><b>部署两件套(缺一个就是"能探测但渲染不出来")</b>:
 * <ol>
 *   <li>把 Qt 安装目录的 {@code qml/QtQuick3D} 部署到运行期 QML 导入路径。
 *       注意 Qt 6 的<b>可重定位前缀</b>是按 Qt6Core 动态库所在目录推导的:
 *       运行期 Qt6Core 从 {@code lib/} 加载 ⇒ {@code QmlImportsPath} 就是 {@code lib/qml},
 *       Qt 安装目录里的 qml/QtQuick3D 用户根本看不到(实测报
 *       {@code module "QtQuick3D" is not installed})。构建脚本已拷到
 *       {@code lib/qml/QtQuick3D}(打包布局)与仓库根 {@code qml/QtQuick3D}(开发/测试布局);</li>
 *   <li>{@code qquick3dplugin.dll} 的依赖链
 *       {@code Qt6Quick3D.dll → Qt6Quick3DRuntimeRender.dll → Qt6ShaderTools.dll} 必须都能加载。
 *       Qt Shader Tools 是独立 addon(qtshadertools),官方 MinGW 在线安装器默认<b>不装</b>:
 *       缺它时报 {@code Cannot load library ...qquick3dplugin.dll: 找不到指定的模块}
 *       (MSVC 版 Qt6ShaderTools.dll 来自 QtCreator/PyQt6,与 MinGW 6.12 ABI 不兼容,不能借用)。
 *       装法:在线安装器勾选 <i>Qt Shader Tools</i>,或取官方在线仓库的 MinGW 包
 *       {@code qt.qt6.6120.addons.qtshadertools.win64_mingw} 解到 Qt 安装目录
 *       (只需 {@code bin/Qt6ShaderTools.dll} 进 {@code lib/})。</li>
 * </ol>
 *
 * <pre>
 * // 用法一:先问能不能用,再决定是否建视图
 * if (QQuick3D.isAvailable()) {
 *     QQuick3DView view = new QQuick3DView(640, 480);
 *     view.loadQml("import QtQuick\nimport QtQuick3D\nView3D { ... }\n");
 *     view.show();
 * } else {
 *     System.out.println(QQuick3D.status());   // 为什么不可用(含 QML 错误原文)
 * }
 *
 * // 用法二:只在日志里留一行
 * System.out.println("Quick3D qt=" + QQuick3D.qtVersion() + " " + QQuick3D.status());
 * </pre>
 *
 * <p><b>线程与前置条件</b>:探测会创建 QQuickView(窗口对象),必须在 Qt GUI 线程调用;
 * {@code new QApplication()} 之前调用也安全(结论为"未判定",见 {@link #isAvailable()}),
 * 但真正的探测要等 QApplication 建好之后。
 *
 * @see QQuick3DView
 * @see QQuickView
 */
public final class QQuick3D {

    /** 探测结论:尚未探测(或探测条件不满足,结论未定)。 */
    private static final int UNKNOWN = 0;
    /** 探测结论:可用。 */
    private static final int AVAILABLE = 1;
    /** 探测结论:不可用({@link #reason} 说明原因)。 */
    private static final int UNAVAILABLE = 2;

    /** 已确定的探测结论;UNKNOWN 表示还没得到确定结论(可再次探测)。 */
    private static volatile int state = UNKNOWN;
    /** 不可用原因(人类可读,含 QML 错误原文)。 */
    private static volatile String reason = "";
    /** 探测视图的尺寸(只要够让 QML 引擎解析类型,不需要渲染)。 */
    private static final int PROBE_W = 16;
    private static final int PROBE_H = 16;

    /**
     * 探测用 QML:只要 {@code import QtQuick3D} 能过,Quick 3D 的 QML 插件链就是完整的。
     * 这里刻意只写最小场景(不建相机/灯光/模型),避免探测本身引入渲染负担。
     */
    private static final String PROBE_QML =
            "import QtQuick\n"
          + "import QtQuick3D\n"
          + "View3D {\n"
          + "    width: 16; height: 16\n"
          + "}\n";

    private QQuick3D() {}

    /**
     * 本机(运行期)能否加载 Qt Quick 3D 场景。
     *
     * <p>判定方式:创建 {@link QQuick3DView}、加载一段
     * {@code import QtQuick3D} 的最小 QML、读 {@link QQuickView#status()}
     * (同步加载,状态立刻可读)。结论会缓存 —— 部署环境在一个进程内不会变。
     *
     * <p>注意:本方法返回 {@code true} 只代表<b>场景能加载</b>;
     * 画面能否渲染出来还取决于 RHI/GPU(见类注释)。
     * 在 {@code new QApplication()} 之前调用会返回 {@code false},
     * 且结论不缓存({@link #status()} 会说明"需先创建 QApplication")。
     */
    public static boolean isAvailable() {
        probe();
        return state == AVAILABLE;
    }

    /**
     * Qt 运行库版本(如 {@code "6.12.0"},取自 native {@code qVersion()});
     * 仅供日志/问题定位。可在 {@code new QApplication()} 之前调用
     * (内部会先触发 native 库加载,见 {@link #ensureNativeLoaded()})。
     */
    public static String qtVersion() {
        ensureNativeLoaded();
        String v = nativeQtVersion();
        return v == null ? "" : v;
    }
    private static native String nativeQtVersion();

    /**
     * 构建脚本是否定义了 {@code -DJQT_HAVE_QUICK3D}(编译期探测到 Quick3D)。
     *
     * <p>这<b>不</b>参与可用性判定:模块在不在、插件依赖全不全,只有运行期才知道。
     * 该信息只用于日志与 {@link #status()}(便于确认构建脚本探测是否生效)。
     */
    public static boolean compiledIn() {
        ensureNativeLoaded();
        return nativeCompiled();
    }
    private static native boolean nativeCompiled();

    /**
     * 保证 native 库已加载:JQt 的 native 库是在 {@link QApplication} 的静态初始化里
     * {@code System.loadLibrary("jqt")} 装进来的(本项目既有约定,别的类都要求"先建 app")。
     * 这里主动触发一次,于是 {@link #isAvailable()}/{@link #status()}/{@link #qtVersion()}
     * 在 {@code new QApplication()} 之前调用也<b>不会</b>抛 {@link UnsatisfiedLinkError}
     * (实测踩过:不触发就直接 {@code UnsatisfiedLinkError: QQuickView.nativeAvailable()})。
     * 该方法本身不要求 app 存在(native 只回一个"g_app != nullptr")。
     */
    private static void ensureNativeLoaded() {
        QApplication.isQtReady();
    }

    /**
     * 人类可读状态:能否用、判断依据、不能用的原因(含 QML 错误原文与相关环境变量)。
     * 供冒烟、日志与问题报告直接粘贴。
     */
    public static String status() {
        probe();
        StringBuilder sb = new StringBuilder();
        sb.append("Qt Quick 3D: ");
        switch (state) {
            case AVAILABLE:   sb.append("可用 ✅\n"); break;
            case UNAVAILABLE: sb.append("不可用 ❌\n"); break;
            default:          sb.append("未判定(探测条件不满足)\n"); break;
        }
        sb.append("  Qt 版本         : ").append(qtVersion()).append('\n');
        sb.append("  编译期探测       : JQT_HAVE_QUICK3D ")
          .append(compiledIn() ? "已定义(仅信息,不参与判定)" : "未定义(不影响运行期判定)").append('\n');
        sb.append("  QtQuick(QQuickView): ")
          .append(QQuickView.isAvailable() ? "可用" : "不可用(缺 qtdeclarative)").append('\n');
        sb.append("  QApplication    : ").append(QApplication.isQtReady() ? "已就绪" : "未创建").append('\n');
        sb.append("  判定依据        : 真实加载 \"import QtQuick3D\" 的最小 QML 并读加载状态\n");
        sb.append("  QML 导入路径env : QML2_IMPORT_PATH=").append(env("QML2_IMPORT_PATH"))
          .append(", QML_IMPORT_PATH=").append(env("QML_IMPORT_PATH")).append('\n');
        sb.append("  渲染后端env     : QT_QUICK_BACKEND=").append(env("QT_QUICK_BACKEND"))
          .append(", QSG_RHI_BACKEND=").append(env("QSG_RHI_BACKEND")).append('\n');
        if (state == UNAVAILABLE) {
            sb.append("  不可用原因      : ").append(reason.isEmpty() ? "(无)" : reason).append('\n');
            sb.append("  常见修法        : 1) 装 Qt Quick 3D 及其依赖 Qt Shader Tools(qtshadertools,提供 Qt6ShaderTools.dll);\n")
              .append("                    2) 让 QML 导入路径含 QtQuick3D(qml/QtQuick3D 拷到 lib/qml —— 运行期\n")
              .append("                       Qt 的可重定位前缀就是 lib,Qt 安装目录里的 qml 不会被自动看到);\n")
              .append("                    3) 让 Qt6Quick3D*.dll / Qt6ShaderTools.dll 能被加载(PATH 含 lib 或与 jqt.dll 同目录);\n")
              .append("                    4) 3D 渲染需 RHI(D3D11/OpenGL/Vulkan),软件后端 QT_QUICK_BACKEND=software 渲染不出画面。\n");
        } else if (state == AVAILABLE) {
            sb.append("  提示            : 场景可加载;若抓图是空画面,请检查 RHI 后端。\n")
              .append("                    实测(Windows 6.12.0):QT_QPA_PLATFORM=offscreen 一定没有 3D —— 该平台插件\n")
              .append("                    没有 GL/RHI 表面,Qt Quick 退到软件后端(Loading backend software);\n")
              .append("                    QT_QUICK_BACKEND=software 同理。真渲染请用真窗口(QT_QPA_PLATFORM=windows,\n")
              .append("                    默认 D3D11,无独显走 WARP)。\n");
        } else {
            sb.append("  说明            : ").append(reason.isEmpty() ? "(无)" : reason).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    /**
     * 取某个 {@link QQuickView} 句柄的 QML 错误原文(Qt {@code QQuickView::errors()})。
     *
     * <p>为什么需要它:{@link QQuickView#errors()} 依赖 native 的 statusChanged 回调转发,
     * 而回调只带了状态码(不带文本),所以它几乎总是空串;要拿到
     * {@code module "QtQuick3D" is not installed} 这类原文必须直接问 C++。
     *
     * @param qQuickViewHandle {@link QQuickView} 的 native 句柄(包内方法)
     * @return 错误行数组;无错误时为空数组
     */
    static String[] qmlErrors(long qQuickViewHandle) {
        String[] errs = nativeQmlErrors(qQuickViewHandle);
        return errs == null ? new String[0] : errs;
    }
    private static native String[] nativeQmlErrors(long qQuickViewHandle);

    // ------------------------------------------------------------------
    // 探测实现
    // ------------------------------------------------------------------

    private static void probe() {
        if (state != UNKNOWN) return;                  // 已有确定结论:结论在一个进程内不会变
        ensureNativeLoaded();
        synchronized (QQuick3D.class) {
            if (state != UNKNOWN) return;
            if (!QQuickView.isAvailable()) {
                state = UNAVAILABLE;
                reason = "本库未编译 QtQuick(缺 qtdeclarative 模块):Quick 3D 依赖 QtQuick,必定不可用";
                return;
            }
            if (!QApplication.isQtReady()) {
                // 没有 QApplication 就不能建 QQuickView:不下结论、不缓存(创建 QApplication 后可再探)
                reason = "尚未创建 QApplication(Qt 未就绪),无法真实探测:请先 new QApplication() 再调用";
                return;
            }
            QQuick3DView view = null;
            try {
                view = new QQuick3DView(PROBE_W, PROBE_H);
                view.loadQml(PROBE_QML);
                int st = view.status();
                if (st == QQuickView.Status.Ready) {
                    state = AVAILABLE;
                    reason = "";
                } else if (st == QQuickView.Status.Error) {
                    state = UNAVAILABLE;
                    String err = view.errorString();
                    reason = err.isEmpty()
                           ? "加载 import QtQuick3D 的 QML 失败(状态 Error,但 Qt 未给出错误文本)"
                           : err;
                } else {
                    // Loading/Null:本地文件本应同步加载完,这里保守地不下结论
                    reason = "探测未完成(QML 加载状态 = " + statusName(st) + ",非同步完成?),未缓存结论";
                }
            } catch (Throwable t) {
                state = UNAVAILABLE;
                reason = "探测过程异常: " + t;
            } finally {
                if (view != null) view.dispose();
            }
        }
    }

    /** {@link QQuickView.Status} 常量 → 名字(给日志用)。 */
    static String statusName(int status) {
        switch (status) {
            case QQuickView.Status.Null:    return "Null";
            case QQuickView.Status.Ready:   return "Ready";
            case QQuickView.Status.Loading: return "Loading";
            case QQuickView.Status.Error:   return "Error";
            default:                        return "?" + status;
        }
    }

    private static String env(String name) {
        String v = System.getenv(name);
        return (v == null || v.isEmpty()) ? "(未设置)" : v;
    }
}
