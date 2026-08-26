// JQt - Java bindings for Qt.
// Copyright (c) SilentStudio
// SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
// Licensed under the JQt Source License v1.0 - see LICENSE.md.
//
// ============================================================================
// jqt_bridge.cpp — JQt 的 JNI 胶水层（C++ 包装层）
//
// 职责：
//   1. 把 Java 层的 JQt* 调用翻译成 Qt 的 C++ 调用；
//   2. 把 Qt 的信号与事件通过 JNI 回调回 Java 层（伪信号槽）；
//   3. 布局管理器（QVBoxLayout/QHBoxLayout）的封装；
//   4. 内存管理（Phase 5）：
//      - 句柄注册表：Java 持有自增 ID，native 查表得指针（ID 不复用 → 悬垂检测可靠）
//      - 生命周期同步：Qt 对象 destroyed 时自动注销（父删子/布局删除等一律覆盖）
//      - 所有权模型：addWidget/setLayout 后归 Qt 管理，否则由 Java Cleaner 回收
//      - 回收线程安全：Cleaner 线程 → QMetaObject::invokeMethod 排队到 GUI 线程删除
//      - 悬垂保护：调用已销毁对象 → 抛 IllegalStateException，不再 native crash
//
// 构建：项目根目录 build.ps1（MinGW 13.1 + Qt 6.11.2 mingw_64 kit）
// ============================================================================

#include <jni.h>

#ifdef _WIN32
#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>
#include <windowsx.h>
#include <dwmapi.h>
#include <objbase.h>
#endif

#include <QApplication>
#include <QAbstractNativeEventFilter>
#include <QBoxLayout>
#include <QCheckBox>
#include <QFrame>
#include <QGraphicsOpacityEffect>
#include <QGraphicsDropShadowEffect>
#include <QPainter>
#include <QPainterPath>
#include <QPropertyAnimation>
#include <QVariantAnimation>
#include <QCloseEvent>
#include <QComboBox>
#include <QEnterEvent>
#include <QParallelAnimationGroup>
#include <QHBoxLayout>
#include <QLabel>
#include <QLayout>
#include <QLineEdit>
#include <QListWidget>
#include <QMetaObject>
#include <QMoveEvent>
#include <QPushButton>
#include <QResizeEvent>
#include <QTimer>
#include <QVBoxLayout>
#include <QWidget>
#include <QWindow>

#include <atomic>
#include <functional>
#include <mutex>
#include <unordered_map>

// 全局主题色（JQtApplication.setAccentColor 更新；自绘控件 JQtSwitch 使用）
static QColor g_accentColor = QColor(0x4c, 0xc2, 0xff);

#include "generated/org_jqt_JQtApplication.h"
#include "generated/org_jqt_JQtAnimations.h"
#include "generated/org_jqt_JQtPivot.h"
#include "generated/org_jqt_JQtWidget.h"
#include "generated/org_jqt_JQtWindow.h"
#include "generated/org_jqt_JQtButton.h"
#include "generated/org_jqt_JQtLabel.h"
#include "generated/org_jqt_JQtLayout.h"
#include "generated/org_jqt_JQtVBoxLayout.h"
#include "generated/org_jqt_JQtHBoxLayout.h"
#include "generated/org_jqt_JQtLineEdit.h"
#include "generated/org_jqt_JQtComboBox.h"
#include "generated/org_jqt_JQtListWidget.h"
#include "generated/org_jqt_JQtPanel.h"
#include "generated/org_jqt_JQtCheckBox.h"
#include "generated/org_jqt_JQtTitleBar.h"
#include "generated/org_jqt_JQtSwitch.h"

// ----------------------------------------------------------------------------
// 全局状态
// ----------------------------------------------------------------------------

static JavaVM* g_jvm = nullptr;        // 缓存的 JVM 句柄（供 C++ → Java 回调）
static QApplication* g_app = nullptr;  // 进程级唯一的 QApplication

// 句柄注册表：Java 侧持有自增 ID（从 1 开始，永不复用），native 查表获得指针。
// destroyed 信号保证注册表与 Qt 对象生命周期严格同步。
static std::mutex g_handleMutex;
static std::unordered_map<int64_t, void*> g_handles;          // id -> QObject*
static std::unordered_map<int64_t, bool> g_javaOwned;         // id -> 是否归 Java（Cleaner）管理
static std::atomic<int64_t> g_nextHandleId{1};

// 取得当前线程的 JNIEnv：已附加 JVM 则复用，否则挂载。
// Qt 信号总是在 GUI（主）线程发出，而主线程执行 app.exec() 前已被 JVM 附加。
static JNIEnv* callbackEnv() {
    JNIEnv* env = nullptr;
    if (g_jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_8) == JNI_EDETACHED) {
        g_jvm->AttachCurrentThread(reinterpret_cast<void**>(&env), nullptr);
    }
    return env;
}

// JNI 回调中 Java 抛出的异常：打印并清除，避免悬挂污染后续 JNI 调用
static void checkJniException(JNIEnv* env) {
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
}

// ----------------------------------------------------------------------------
// 句柄注册表 API
// ----------------------------------------------------------------------------

// 注册对象并返回 Java 侧句柄 ID。javaOwned=true 表示归 Java 管理（GC 时回收）。
static jlong registerHandle(void* ptr, bool javaOwned) {
    const int64_t id = g_nextHandleId.fetch_add(1);
    {
        std::lock_guard<std::mutex> lock(g_handleMutex);
        g_handles[id] = ptr;
        g_javaOwned[id] = javaOwned;
    }
    // Qt 对象销毁（含父删子、布局清理、deleteLater 等一切途径）→ 自动注销
    QObject* obj = static_cast<QObject*>(ptr);
    QObject::connect(obj, &QObject::destroyed, [id](QObject*) {
        std::lock_guard<std::mutex> lock(g_handleMutex);
        g_handles.erase(id);
        g_javaOwned.erase(id);
    });
    return static_cast<jlong>(id);
}

// 句柄 → 指针；无效/已销毁 → 抛 IllegalStateException 并返回 nullptr
static void* requireHandle(JNIEnv* env, jlong handle) {
    if (handle <= 0) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"),
                      "JQt: invalid native handle (never created or already disposed)");
        return nullptr;
    }
    std::lock_guard<std::mutex> lock(g_handleMutex);
    auto it = g_handles.find(static_cast<int64_t>(handle));
    if (it == g_handles.end()) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"),
                      "JQt: native object already destroyed");
        return nullptr;
    }
    return it->second;
}

// 标记对象归 Qt 管理（addWidget/setLayout 后）：Java Cleaner 不再回收
static void markQtOwned(jlong handle) {
    std::lock_guard<std::mutex> lock(g_handleMutex);
    auto it = g_javaOwned.find(static_cast<int64_t>(handle));
    if (it != g_javaOwned.end()) {
        it->second = false;
    }
}

// QApplication 保护：控件创建前必须先创建 JQtApplication
static QApplication* requireApp(JNIEnv* env) {
    if (g_app == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"),
                      "JQt: QApplication not created - construct JQtApplication first");
        return nullptr;
    }
    return g_app;
}

// 回调宏：JNI 调用后清理 Java 异常，避免污染
#define JQT_CALL_VOID(env, obj, mid, ...)     do {         (env)->CallVoidMethod((obj), (mid), ##__VA_ARGS__);         checkJniException((env));     } while (0)

// ----------------------------------------------------------------------------
// 窗口壳：拦截 closeEvent / resizeEvent / moveEvent，回调给 Java 层
// ----------------------------------------------------------------------------

#ifdef _WIN32
// 全局 POINTER→鼠标合成过滤器：覆盖所有 Qt 顶层窗口
// （含 QComboBox 弹层等 Qt 内部创建的独立窗口——弹层里点不动就是缺这个）。
static bool g_pointerPressed = false;   // 触摸按下状态（合成 WM_MOUSEMOVE 的按键标志）

class JQtPointerFilter : public QAbstractNativeEventFilter {
public:
    bool nativeEventFilter(const QByteArray& eventType, void* message, qintptr* result) override {
        Q_UNUSED(eventType);
        Q_UNUSED(result);
        MSG* msg = static_cast<MSG*>(message);
        if (msg->hwnd == nullptr) {
            return false;
        }
        if (msg->message == WM_POINTERDOWN || msg->message == WM_POINTERUP
            || msg->message == WM_POINTERUPDATE) {
            POINTER_INFO pi;
            if (GetPointerInfo(GET_POINTERID_WPARAM(msg->wParam), &pi)) {
                POINT pt = pi.ptPixelLocation;   // 物理屏幕坐标
                if (ScreenToClient(msg->hwnd, &pt)) {
                    if (msg->message == WM_POINTERDOWN) {
                        // 标题栏拖动由 WM_NCHITTEST(HTCAPTION) 原生接管（真实系统拖动），
                        // 此处仅合成其余区域的鼠标事件
                        g_pointerPressed = true;
                        fprintf(stderr, "[JQt] POINTERDOWN at client (%d,%d) -> mouse\n",
                                static_cast<int>(pt.x), static_cast<int>(pt.y));
                        PostMessageW(msg->hwnd, WM_LBUTTONDOWN, MK_LBUTTON,
                                     MAKELPARAM(pt.x, pt.y));
                    } else if (msg->message == WM_POINTERUP) {
                        g_pointerPressed = false;
                        fprintf(stderr, "[JQt] POINTERUP\n");
                        PostMessageW(msg->hwnd, WM_LBUTTONUP, 0,
                                     MAKELPARAM(pt.x, pt.y));
                    } else {
                        // 移动：带按下状态（Qt 据此维持 buttons()/拖动）
                        PostMessageW(msg->hwnd, WM_MOUSEMOVE,
                                     g_pointerPressed ? MK_LBUTTON : 0,
                                     MAKELPARAM(pt.x, pt.y));
                    }
                }
            }
            return true;
        }
        return false;
    }
};
#endif

class JQtWindowShell : public QWidget {
public:
    std::function<void()> onClose;
    std::function<void(int, int)> onResized;
    std::function<void(int, int)> onMoved;

    // ---- Fluent 窗口状态 ----
    // 实现思路参考 PyQt-Frameless-Window (zhiyiYo, GPLv3)；
    // 仅使用公开 Win32 API 知识，代码独立编写（详见 THIRD-PARTY-NOTICES.md）----
    bool frameless = false;       // 无边框模式
    bool acrylic = false;         // 亚克力背景
    bool rounded = false;         // Win11 圆角
    bool draggable = true;        // 标题栏区域可拖拽
    int borderWidth = 5;          // 缩放热区宽度
    bool m_dragging = false;      // 手动拖动状态
    QPoint m_dragOffset;          // 按下点相对窗口原点的偏移
    bool m_manualMaximized = false;  // 手动最大化（showMaximized 对无边框窗口不可靠）
    QRect m_normalGeometry;          // 最大化前的几何（还原用）

    // 当前显示器工作区（Qt 逻辑坐标）
    QRect workArea() const {
#ifdef _WIN32
        HMONITOR mon = MonitorFromWindow(reinterpret_cast<HWND>(winId()), MONITOR_DEFAULTTONEAREST);
        MONITORINFO mi;
        mi.cbSize = sizeof(mi);
        if (GetMonitorInfoW(mon, &mi)) {
            const qreal dpr = devicePixelRatioF();
            return QRect(qRound(mi.rcWork.left / dpr), qRound(mi.rcWork.top / dpr),
                         qRound((mi.rcWork.right - mi.rcWork.left) / dpr),
                         qRound((mi.rcWork.bottom - mi.rcWork.top) / dpr));
        }
#endif
        return QRect();
    }

    // 手动最大化/还原（无边框窗口可靠方案）
    void setManualMaximized(bool maximized) {
        if (maximized == m_manualMaximized) {
            return;
        }
        if (maximized) {
            m_normalGeometry = geometry();
            const QRect wa = workArea();
            fprintf(stderr, "[JQt] max: normal=%d,%d %dx%d work=%d,%d %dx%d valid=%d\n",
                    m_normalGeometry.x(), m_normalGeometry.y(),
                    m_normalGeometry.width(), m_normalGeometry.height(),
                    wa.x(), wa.y(), wa.width(), wa.height(), wa.isValid() ? 1 : 0);
            if (wa.isValid()) {
                setGeometry(wa);
            }
            m_manualMaximized = true;
        } else {
            m_manualMaximized = false;
            if (m_normalGeometry.isValid()) {
                fprintf(stderr, "[JQt] restore: %d,%d %dx%d\n",
                        m_normalGeometry.x(), m_normalGeometry.y(),
                        m_normalGeometry.width(), m_normalGeometry.height());
                setGeometry(m_normalGeometry);
            }
        }
    }

    bool isManualMaximized() const { return m_manualMaximized; }

#ifdef _WIN32
    // DWM 阴影（无边框时启用）
    void applyShadow() {
        HMODULE dwm = GetModuleHandleW(L"dwmapi.dll");
        if (!dwm) dwm = LoadLibraryW(L"dwmapi.dll");
        if (!dwm) return;
        typedef HRESULT(WINAPI* DwmExtendFrameFunc)(HWND, const MARGINS*);
        auto fn = (DwmExtendFrameFunc)GetProcAddress(dwm, "DwmExtendFrameIntoClientArea");
        if (fn) {
            MARGINS margins{ 1, 1, 1, 1 };
            fn(reinterpret_cast<HWND>(winId()), &margins);
        }
    }

    // 亚克力背景（SetWindowCompositionAttribute，Win10+）
    void applyAcrylic() {
        HMODULE user32 = GetModuleHandleW(L"user32.dll");
        if (!user32) return;
        typedef BOOL(WINAPI* SetWCAFunc)(HWND, void*);
        auto fn = (SetWCAFunc)GetProcAddress(user32, "SetWindowCompositionAttribute");
        if (!fn) return;
        struct ACCENT_POLICY { int state; int flags; int color; int animation; };
        struct WINCOMPATTRDATA { int attr; void* data; unsigned long size; };
        ACCENT_POLICY ap;
        ap.state = 4;                                   // ACCENT_ENABLE_ACRYLICBLURBEHIND
        ap.flags = 0x20 | 0x40 | 0x80 | 0x100;          // 阴影 + 无边框属性
        ap.color = 0x99F2F2F2;                          // 亚克力混合色（ABGR）
        ap.animation = 0;
        WINCOMPATTRDATA data;
        data.attr = 19;                                 // WCA_ACCENT_POLICY
        data.data = &ap;
        data.size = sizeof(ap);
        fn(reinterpret_cast<HWND>(winId()), &data);
    }

    // Win11 圆角（DWMWA_WINDOW_CORNER_PREFERENCE = 33）
    void applyRoundedCorners() {
        HMODULE dwm = GetModuleHandleW(L"dwmapi.dll");
        if (!dwm) dwm = LoadLibraryW(L"dwmapi.dll");
        if (!dwm) return;
        typedef HRESULT(WINAPI* DwmSetAttrFunc)(HWND, DWORD, const void*, DWORD);
        auto fn = (DwmSetAttrFunc)GetProcAddress(dwm, "DwmSetWindowAttribute");
        if (fn) {
            int pref = 2;  // DWMWCP_ROUND
            fn(reinterpret_cast<HWND>(winId()), 33, &pref, sizeof(pref));
        }
    }
#endif

protected:
    void closeEvent(QCloseEvent* event) override {
        if (onClose) {
            onClose();
        }
        QWidget::closeEvent(event);
    }

#ifdef _WIN32
    // 无边框窗口：WM_NCHITTEST 手动实现缩放热区（qframelesswindow 同款逻辑）
    bool nativeEvent(const QByteArray& eventType, void* message, qintptr* result) override {
        if (!frameless) {
            return QWidget::nativeEvent(eventType, message, result);
        }
        MSG* msg = static_cast<MSG*>(message);
        if (msg->hwnd == nullptr) {
            return QWidget::nativeEvent(eventType, message, result);
        }

        if (msg->message == WM_NCHITTEST) {
            if (isManualMaximized() || isFullScreen()) {
                return QWidget::nativeEvent(eventType, message, result);
            }
            // WM_NCHITTEST 坐标是物理像素，Qt 坐标是逻辑像素：DPI 缩放时需换算
            const qreal dpr = devicePixelRatioF();
            const int x = GET_X_LPARAM(msg->lParam);
            const int y = GET_Y_LPARAM(msg->lParam);
            const QPoint pos = mapFromGlobal(QPoint(qRound(x / dpr), qRound(y / dpr)));
            const int w = width();
            const int h = height();
            const int bw = borderWidth;

            const bool l = pos.x() < bw;
            const bool r = pos.x() > w - bw;
            const bool t = pos.y() < bw;
            const bool b = pos.y() > h - bw;

            if (l && t) { *result = HTTOPLEFT; return true; }
            if (r && b) { *result = HTBOTTOMRIGHT; return true; }
            if (r && t) { *result = HTTOPRIGHT; return true; }
            if (l && b) { *result = HTBOTTOMLEFT; return true; }
            if (t) { *result = HTTOP; return true; }
            if (b) { *result = HTBOTTOM; return true; }
            if (l) { *result = HTLEFT; return true; }
            if (r) { *result = HTRIGHT; return true; }
            // 标题栏空白区（顶部 40 逻辑 px，避开右侧按钮区 ~150px）→ HTCAPTION：
            // Windows 走真实系统拖动链（鼠标/触摸原生支持，跟手且无延迟）。
            // 按钮区保持 HTCLIENT → 按钮正常点击（第一版全区域 HTCAPTION 吞按钮的教训）。
            if (draggable && !isManualMaximized() && pos.y() < 40 && pos.x() < w - 150) {
                *result = HTCAPTION;
                return true;
            }
            // 其余一律 HTCLIENT
            return QWidget::nativeEvent(eventType, message, result);
        } else if (msg->message == WM_NCCALCSIZE && msg->wParam != 0) {
            // 无边框：客户区铺满（避免系统边框占位）
            *result = 0;
            return true;
        } else if (msg->message == WM_GETMINMAXINFO) {
            // 最大化约束到显示器工作区（无边框窗口默认会盖住任务栏）
            MINMAXINFO* mmi = reinterpret_cast<MINMAXINFO*>(msg->lParam);
            HMONITOR mon = MonitorFromWindow(msg->hwnd, MONITOR_DEFAULTTONEAREST);
            MONITORINFO mi;
            mi.cbSize = sizeof(mi);
            if (GetMonitorInfoW(mon, &mi)) {
                mmi->ptMaxPosition.x = mi.rcWork.left;
                mmi->ptMaxPosition.y = mi.rcWork.top;
                mmi->ptMaxSize.x = mi.rcWork.right - mi.rcWork.left;
                mmi->ptMaxSize.y = mi.rcWork.bottom - mi.rcWork.top;
            }
            *result = 0;
            return true;
        }

        return QWidget::nativeEvent(eventType, message, result);
    }
#endif

    // 无边框窗口的标题栏区域拖拽（手动拖动方案）。
    // 不用 startSystemMove：系统拖动循环在触摸→鼠标合成链中不可靠（拖不动），
    // 手动 move() 对鼠标/触摸合成事件 100% 可靠。
    // 事件冒泡机制：子控件（按钮/开关）处理了点击就不会到达这里；
    // 标题栏空白区域（QFrame/QLabel 不处理）冒泡到窗口后启动拖动。
    void mousePressEvent(QMouseEvent* event) override {
        fprintf(stderr, "[JQt] window mousePress at %.0f,%.0f\n",
                event->position().x(), event->position().y());
        if (frameless && draggable && event->button() == Qt::LeftButton
            && event->position().y() < 40) {
            m_dragging = true;
            m_dragOffset = event->globalPosition().toPoint() - window()->pos();
            event->accept();
            return;
        }
        QWidget::mousePressEvent(event);
    }

    void mouseMoveEvent(QMouseEvent* event) override {
        if (m_dragging) {
            window()->move(event->globalPosition().toPoint() - m_dragOffset);
            event->accept();
            return;
        }
        QWidget::mouseMoveEvent(event);
    }

    void mouseReleaseEvent(QMouseEvent* event) override {
        if (m_dragging) {
            m_dragging = false;
            event->accept();
            return;
        }
        QWidget::mouseReleaseEvent(event);
    }

    void resizeEvent(QResizeEvent* event) override {
        if (onResized) {
            onResized(event->size().width(), event->size().height());
        }
        QWidget::resizeEvent(event);
    }

    void moveEvent(QMoveEvent* event) override {
        if (onMoved) {
            onMoved(event->pos().x(), event->pos().y());
        }
        QWidget::moveEvent(event);
    }
};

// ----------------------------------------------------------------------------
// JQtApplication：QApplication 的封装
// ----------------------------------------------------------------------------

JNIEXPORT jlong JNICALL Java_org_jqt_JQtApplication_nativeCreateApp(JNIEnv* env, jobject thiz) {
    env->GetJavaVM(&g_jvm);
    if (g_app == nullptr) {
        // QApplication 需要 argc/argv；JVM 的命令行参数不适用于 Qt，伪造一份最小参数。
        static int argc = 1;
        static char arg0[] = "jqt";
        static char* argv[] = { arg0, nullptr };
        g_app = new QApplication(argc, argv);
#ifdef _WIN32
        // 全局触摸→鼠标合成（覆盖所有窗口含弹层）
        static JQtPointerFilter g_pointerFilter;
        g_app->installNativeEventFilter(&g_pointerFilter);
#endif
    }

    // aboutToQuit 信号 → Java nativeHandleAboutToQuit()；gRef 由 JVM 退出时统一清理
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(g_app, &QApplication::aboutToQuit, [gRef]() {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleAboutToQuit", "()V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, gRef, mid);
        }
    });

    return reinterpret_cast<jlong>(g_app);
}

JNIEXPORT void JNICALL Java_org_jqt_JQtApplication_exec(JNIEnv* /*env*/, jobject /*thiz*/) {
    if (g_app != nullptr) {
        g_app->exec();  // 阻塞直到最后一个窗口关闭（quitOnLastWindowClosed 默认开启）
    }
}

JNIEXPORT void JNICALL Java_org_jqt_JQtApplication_quit(JNIEnv* /*env*/, jobject /*thiz*/) {
    if (g_app != nullptr) {
        g_app->quit();
    }
}

JNIEXPORT void JNICALL Java_org_jqt_JQtApplication_scheduleQuit(JNIEnv* /*env*/, jobject /*thiz*/, jlong ms) {
    if (g_app != nullptr) {
        QTimer::singleShot(static_cast<int>(ms), g_app, &QApplication::quit);
    }
}

// 切换配色方案（setPalette 运行时生效，立即刷新全部控件）
// light=true → 浅色调色板；light=false → 深色调色板。
// 背景：Qt 在 Java 进程中暗色检测异常；此 API 让 Java 应用显式控制深浅色。
JNIEXPORT void JNICALL Java_org_jqt_JQtApplication_nativeSetColorScheme(JNIEnv* /*env*/, jobject /*thiz*/, jboolean light) {
    if (g_app == nullptr) {
        return;
    }
    QPalette p;
    if (light == JNI_TRUE) {
        p.setColor(QPalette::Window, QColor(0xf0, 0xf0, 0xf0));
        p.setColor(QPalette::WindowText, Qt::black);
        p.setColor(QPalette::Base, Qt::white);
        p.setColor(QPalette::AlternateBase, QColor(0xf5, 0xf5, 0xf5));
        p.setColor(QPalette::Text, Qt::black);
        p.setColor(QPalette::Button, QColor(0xe1, 0xe1, 0xe1));
        p.setColor(QPalette::ButtonText, Qt::black);
        p.setColor(QPalette::BrightText, Qt::red);
        p.setColor(QPalette::Highlight, QColor(0x00, 0x78, 0xd7));
        p.setColor(QPalette::HighlightedText, Qt::white);
        p.setColor(QPalette::ToolTipBase, QColor(0xff, 0xff, 0xdc));
        p.setColor(QPalette::ToolTipText, Qt::black);
    } else {
        p.setColor(QPalette::Window, QColor(0x1f, 0x1f, 0x1f));
        p.setColor(QPalette::WindowText, QColor(0xe8, 0xe8, 0xe8));
        p.setColor(QPalette::Base, QColor(0x2b, 0x2b, 0x2b));
        p.setColor(QPalette::AlternateBase, QColor(0x33, 0x33, 0x33));
        p.setColor(QPalette::Text, QColor(0xe8, 0xe8, 0xe8));
        p.setColor(QPalette::Button, QColor(0x3b, 0x3b, 0x3b));
        p.setColor(QPalette::ButtonText, QColor(0xe8, 0xe8, 0xe8));
        p.setColor(QPalette::BrightText, Qt::red);
        p.setColor(QPalette::Highlight, QColor(0x4c, 0xc2, 0xff));
        p.setColor(QPalette::HighlightedText, Qt::black);
        p.setColor(QPalette::ToolTipBase, QColor(0x2b, 0x2b, 0x2b));
        p.setColor(QPalette::ToolTipText, QColor(0xe8, 0xe8, 0xe8));
    }
    g_app->setPalette(p);
}

// 设置全局样式表（QSS，QApplication::setStyleSheet）
JNIEXPORT void JNICALL Java_org_jqt_JQtApplication_nativeSetStyleSheet(JNIEnv* env, jobject /*thiz*/, jstring qss) {
    if (g_app == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(qss, nullptr);
    g_app->setStyleSheet(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(qss, utf);
}

// 设置全局字体（QApplication::setFont；所有控件继承，Qt 自动回退缺失字形）
JNIEXPORT void JNICALL Java_org_jqt_JQtApplication_nativeSetFont(JNIEnv* env, jobject /*thiz*/, jstring family, jint size) {
    if (g_app == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(family, nullptr);
    QFont font(QString::fromUtf8(utf), static_cast<int>(size));
    env->ReleaseStringUTFChars(family, utf);
    g_app->setFont(font);
}

// 设置全局主题色（强调色）：更新 QPalette::Highlight（pivot/选中态/输入框光标跟随）
// + 自绘控件主题色（JQtSwitch 轨道）；QSS 部分由 Java 侧重渲染模板。
JNIEXPORT void JNICALL Java_org_jqt_JQtApplication_nativeSetAccent(JNIEnv* env, jobject /*thiz*/, jstring hex) {
    if (g_app == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(hex, nullptr);
    QColor c(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(hex, utf);
    if (!c.isValid()) {
        return;
    }
    g_accentColor = c;
    QPalette p = g_app->palette();
    p.setColor(QPalette::Highlight, c);
    p.setColor(QPalette::HighlightedText, QColor(0xff, 0xff, 0xff));
    g_app->setPalette(p);
}

// 切换风格（QApplication::setStyle，如 "Fusion" / "Windows" / "macOS"）
JNIEXPORT void JNICALL Java_org_jqt_JQtApplication_nativeSetStyle(JNIEnv* env, jobject /*thiz*/, jstring style) {
    if (g_app == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(style, nullptr);
    g_app->setStyle(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(style, utf);
}

// 在 ms 毫秒后于 GUI 线程执行 Java Runnable（Qt 定时器 → JNI → Runnable.run()）
JNIEXPORT void JNICALL Java_org_jqt_JQtApplication_nativeSchedule(JNIEnv* env, jobject /*thiz*/, jobject task, jlong ms) {
    jobject gTask = env->NewGlobalRef(task);
    QTimer::singleShot(static_cast<int>(ms), [gTask]() {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gTask);
        jmethodID mid = e->GetMethodID(cls, "run", "()V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, gTask, mid);
        }
        e->DeleteGlobalRef(gTask);
    });
}

// ----------------------------------------------------------------------------
// JQtWidget：清理器入口（Java Cleaner 线程回调）
// ----------------------------------------------------------------------------

// Java 对象不可达时（或显式 dispose()）：若对象仍归 Java 管理则排队到 GUI 线程删除
JNIEXPORT void JNICALL Java_org_jqt_JQtWidget_nativeDispose(JNIEnv* /*env*/, jclass /*cls*/, jlong handle) {
    QObject* obj = nullptr;
    {
        std::lock_guard<std::mutex> lock(g_handleMutex);
        auto it = g_handles.find(static_cast<int64_t>(handle));
        if (it == g_handles.end()) {
            return;  // 已销毁或从未注册
        }
        auto oit = g_javaOwned.find(static_cast<int64_t>(handle));
        if (oit == g_javaOwned.end() || !oit->second) {
            return;  // 归 Qt 管理（父/布局接管），不回收
        }
        obj = static_cast<QObject*>(it->second);
        g_handles.erase(it);
        g_javaOwned.erase(oit);
    }
    if (obj != nullptr && g_app != nullptr) {
        // Cleaner 线程不是 GUI 线程：排队到事件循环中删除（destroyed 时注册表自动注销）
        QMetaObject::invokeMethod(g_app, [obj]() { delete obj; }, Qt::QueuedConnection);
    }
}

// 设置控件级样式表（QSS，QWidget::setStyleSheet；控件与全局样式可叠加）
// 控件级 QSS 合并：base（用户 setStyleSheet）+ border-radius（setBorderRadius）
// 存于 dynamic property，两 API 互相不覆盖。
static void jqtApplyWidgetQss(QWidget* widget) {
    const QString base = widget->property("jqtBaseQss").toString();
    const int radius = widget->property("jqtRadius").toInt();
    QString qss = base;
    if (radius > 0) {
        const QString cls = QString::fromLatin1(widget->metaObject()->className());
        qss += QString::fromLatin1("\n%1 { border-radius: %2px; }").arg(cls).arg(radius);
    }
    widget->setStyleSheet(qss);
}

JNIEXPORT void JNICALL Java_org_jqt_JQtWidget_nativeSetStyleSheet(JNIEnv* env, jclass /*cls*/, jlong handle, jstring qss) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(qss, nullptr);
    widget->setProperty("jqtBaseQss", QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(qss, utf);
    jqtApplyWidgetQss(widget);
}

// 自定义控件圆角（像素；0 = 不添加规则，用全局 QSS）
JNIEXPORT void JNICALL Java_org_jqt_JQtWidget_nativeSetBorderRadius(JNIEnv* env, jclass /*cls*/, jlong handle, jint radius) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    widget->setProperty("jqtRadius", static_cast<int>(radius));
    jqtApplyWidgetQss(widget);
}

// ----------------------------------------------------------------------------
// JQtWindow：顶级 QWidget 的封装
// ----------------------------------------------------------------------------

JNIEXPORT jlong JNICALL Java_org_jqt_JQtWindow_nativeCreate(JNIEnv* env, jobject thiz, jstring title, jint width, jint height) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    const char* utf = env->GetStringUTFChars(title, nullptr);
    JQtWindowShell* win = new JQtWindowShell();
    win->setWindowTitle(QString::fromUtf8(utf));
    win->resize(static_cast<int>(width), static_cast<int>(height));
    env->ReleaseStringUTFChars(title, utf);

    // 持有 Java 窗口对象的全局引用，供窗口事件回调使用（JVM 退出时统一清理）
    jobject gRef = env->NewGlobalRef(thiz);
    win->onClose = [gRef]() {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleClose", "()V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, gRef, mid);
        }
    };
    win->onResized = [gRef](int w, int h) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleResized", "(II)V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(w), static_cast<jint>(h));
        }
    };
    win->onMoved = [gRef](int x, int y) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleMoved", "(II)V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(x), static_cast<jint>(y));
        }
    };

    return registerHandle(win, /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_JQtWindow_nativeShow(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    widget->show();
}

JNIEXPORT void JNICALL Java_org_jqt_JQtWindow_nativeHide(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    widget->hide();
}

// 关闭窗口（触发 onClose 回调；若为最后一个窗口，exec() 返回）
JNIEXPORT void JNICALL Java_org_jqt_JQtWindow_nativeClose(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    widget->close();
}

// ---- Fluent 窗口能力（偷师 qframelesswindow / qfluentwidgets）----

// 无边框模式：FramelessWindowHint + DWM 阴影 + 缩放热区（WM_NCHITTEST）
JNIEXPORT void JNICALL Java_org_jqt_JQtWindow_nativeSetFrameless(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean on) {
    JQtWindowShell* win = static_cast<JQtWindowShell*>(requireHandle(env, handle));
    if (win == nullptr) {
        return;
    }
    win->frameless = (on == JNI_TRUE);
    if (win->frameless) {
        win->setWindowFlag(Qt::FramelessWindowHint, true);
#ifdef _WIN32
        win->applyShadow();
#endif
    } else {
        win->setWindowFlag(Qt::FramelessWindowHint, false);
    }
    win->show();
}

// 亚克力背景（Win10+，SetWindowCompositionAttribute）
JNIEXPORT void JNICALL Java_org_jqt_JQtWindow_nativeSetAcrylic(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean on) {
    JQtWindowShell* win = static_cast<JQtWindowShell*>(requireHandle(env, handle));
    if (win == nullptr) {
        return;
    }
    win->acrylic = (on == JNI_TRUE);
#ifdef _WIN32
    if (win->acrylic) {
        win->applyAcrylic();
    }
#endif
}

// Win11 圆角（DWMWA_WINDOW_CORNER_PREFERENCE）
JNIEXPORT void JNICALL Java_org_jqt_JQtWindow_nativeSetRoundedCorners(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean on) {
    JQtWindowShell* win = static_cast<JQtWindowShell*>(requireHandle(env, handle));
    if (win == nullptr) {
        return;
    }
    win->rounded = (on == JNI_TRUE);
#ifdef _WIN32
    if (win->rounded) {
        win->applyRoundedCorners();
    }
#endif
}

// 标题栏区域拖拽开关
JNIEXPORT void JNICALL Java_org_jqt_JQtWindow_nativeSetDraggable(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean on) {
    JQtWindowShell* win = static_cast<JQtWindowShell*>(requireHandle(env, handle));
    if (win == nullptr) {
        return;
    }
    win->draggable = (on == JNI_TRUE);
}

// 缩放热区宽度（像素）
JNIEXPORT void JNICALL Java_org_jqt_JQtWindow_nativeSetBorderWidth(JNIEnv* env, jobject /*thiz*/, jlong handle, jint px) {
    JQtWindowShell* win = static_cast<JQtWindowShell*>(requireHandle(env, handle));
    if (win == nullptr) {
        return;
    }
    win->borderWidth = static_cast<int>(px);
}

JNIEXPORT void JNICALL Java_org_jqt_JQtWindow_nativeMinimize(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    widget->showMinimized();
}

JNIEXPORT void JNICALL Java_org_jqt_JQtWindow_nativeMaximize(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    JQtWindowShell* win = static_cast<JQtWindowShell*>(requireHandle(env, handle));
    if (win == nullptr) {
        return;
    }
    win->setManualMaximized(true);
}

JNIEXPORT void JNICALL Java_org_jqt_JQtWindow_nativeToggleMaximize(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    JQtWindowShell* win = static_cast<JQtWindowShell*>(requireHandle(env, handle));
    if (win == nullptr) {
        return;
    }
    win->setManualMaximized(!win->isManualMaximized());
}

JNIEXPORT jboolean JNICALL Java_org_jqt_JQtWindow_nativeIsMaximized(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    JQtWindowShell* win = static_cast<JQtWindowShell*>(requireHandle(env, handle));
    if (win == nullptr) {
        return JNI_FALSE;
    }
    return win->isManualMaximized() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_org_jqt_JQtWindow_nativeResize(JNIEnv* env, jobject /*thiz*/, jlong handle, jint width, jint height) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    widget->resize(static_cast<int>(width), static_cast<int>(height));
}

JNIEXPORT void JNICALL Java_org_jqt_JQtWindow_nativeSetTitle(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring title) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(title, nullptr);
    widget->setWindowTitle(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(title, utf);
}

// 把子控件加入窗口（未设置布局时）：建立 Qt 父子关系并按顺序自动摆放。
JNIEXPORT void JNICALL Java_org_jqt_JQtWindow_nativeAddWidget(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong childHandle) {
    QWidget* parent = static_cast<QWidget*>(requireHandle(env, handle));
    if (parent == nullptr) {
        return;
    }
    QWidget* child = static_cast<QWidget*>(requireHandle(env, childHandle));
    if (child == nullptr) {
        return;
    }

    child->setParent(parent);
    markQtOwned(childHandle);  // 归 Qt 父子关系管理

    int n = 0;
    const QList<QObject*>& children = parent->children();
    for (QObject* obj : children) {
        if (qobject_cast<QWidget*>(obj) != nullptr) {
            ++n;
        }
    }
    child->move(30, 30 + (n - 1) * 80);
    child->show();
}

// 设置布局管理器：布局接管子控件排列（Qt 6 重复设置会删除旧布局）
JNIEXPORT void JNICALL Java_org_jqt_JQtWindow_nativeSetLayout(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong layoutHandle) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    QLayout* layout = static_cast<QLayout*>(requireHandle(env, layoutHandle));
    if (layout == nullptr) {
        return;
    }
    widget->setLayout(layout);
    markQtOwned(layoutHandle);  // 归窗口管理
}

// JQtWidget：通用 setLayout（任何控件可装布局）
JNIEXPORT void JNICALL Java_org_jqt_JQtWidget_nativeSetLayout(JNIEnv* env, jclass /*cls*/, jlong handle, jlong layoutHandle) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    QLayout* layout = static_cast<QLayout*>(requireHandle(env, layoutHandle));
    if (layout == nullptr) {
        return;
    }
    widget->setLayout(layout);
    markQtOwned(layoutHandle);
}

// JQtWidget：设置 objectName（QSS #name 选择器）
JNIEXPORT void JNICALL Java_org_jqt_JQtWidget_nativeSetObjectName(JNIEnv* env, jclass /*cls*/, jlong handle, jstring name) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(name, nullptr);
    widget->setObjectName(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(name, utf);
}

// ----------------------------------------------------------------------------
// Fluent 按钮：QPushButton + 悬停高亮过渡动画（clean-room 独立实现）
// 150ms OutCubic 高亮叠加层（白色 8.5%），Fluent 公开动效规范参数。
// ----------------------------------------------------------------------------
static bool g_hoverEnabled = true;
static double g_hoverIntensity = 0.085;   // 悬停高亮叠加透明度（JQtAnimations.setHoverIntensity）

class JQtButtonWidget : public QPushButton {
public:
    explicit JQtButtonWidget(const QString& text) : QPushButton(text) {
        m_hoverAnim = new QVariantAnimation(this);
        m_hoverAnim->setDuration(150);
        m_hoverAnim->setEasingCurve(QEasingCurve::OutCubic);
        QObject::connect(m_hoverAnim, &QVariantAnimation::valueChanged, this,
                         [this](const QVariant& v) {
                             m_hover = v.toDouble();
                             update();
                         });
    }

protected:
    void enterEvent(QEnterEvent* event) override {
        QPushButton::enterEvent(event);
        if (g_hoverEnabled && isEnabled() && !isDown()) {
            startHover(1.0);
        }
    }

    void leaveEvent(QEvent* event) override {
        QPushButton::leaveEvent(event);
        if (g_hoverEnabled) {
            startHover(0.0);
        }
    }

    void paintEvent(QPaintEvent* event) override {
        QPushButton::paintEvent(event);
        if (m_hover > 0.01 && !isDown()) {
            QPainter p(this);
            p.setRenderHint(QPainter::Antialiasing);
            const QColor overlay(255, 255, 255, qRound(255.0 * g_hoverIntensity * m_hover));
            QPainterPath path;
            path.addRoundedRect(QRectF(rect()).adjusted(0.5, 0.5, -0.5, -0.5), 5.0, 5.0);
            p.fillPath(path, overlay);
        }
    }

private:
    void startHover(double target) {
        m_hoverAnim->stop();
        m_hoverAnim->setStartValue(m_hover);
        m_hoverAnim->setEndValue(target);
        m_hoverAnim->start();
    }

    double m_hover = 0.0;
    QVariantAnimation* m_hoverAnim;
};

// ----------------------------------------------------------------------------
// JQtButton：QPushButton 的封装（clicked/pressed/released/toggled 信号）
// ----------------------------------------------------------------------------

JNIEXPORT jlong JNICALL Java_org_jqt_JQtButton_nativeCreate(JNIEnv* env, jobject thiz, jstring text) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    QPushButton* btn = new JQtButtonWidget(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);

    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(btn, &QPushButton::clicked, [gRef]() {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleClick", "()V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, gRef, mid);
        }
    });
    QObject::connect(btn, &QPushButton::pressed, [gRef]() {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandlePressed", "()V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, gRef, mid);
        }
    });
    QObject::connect(btn, &QPushButton::released, [gRef]() {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleReleased", "()V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, gRef, mid);
        }
    });
    QObject::connect(btn, &QPushButton::toggled, [gRef](bool checked) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleToggled", "(Z)V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, gRef, mid, static_cast<jboolean>(checked));
        }
    });

    return registerHandle(btn, /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_JQtButton_nativeSetText(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring text) {
    QPushButton* btn = static_cast<QPushButton*>(requireHandle(env, handle));
    if (btn == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    btn->setText(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
}

JNIEXPORT void JNICALL Java_org_jqt_JQtButton_nativeSetCheckable(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean checkable) {
    QPushButton* btn = static_cast<QPushButton*>(requireHandle(env, handle));
    if (btn == nullptr) {
        return;
    }
    btn->setCheckable(checkable == JNI_TRUE);
}

JNIEXPORT void JNICALL Java_org_jqt_JQtButton_nativeSetChecked(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean checked) {
    QPushButton* btn = static_cast<QPushButton*>(requireHandle(env, handle));
    if (btn == nullptr) {
        return;
    }
    btn->setChecked(checked == JNI_TRUE);
}

// ----------------------------------------------------------------------------
// JQtLabel：QLabel 的封装
// ----------------------------------------------------------------------------

JNIEXPORT jlong JNICALL Java_org_jqt_JQtLabel_nativeCreate(JNIEnv* env, jobject /*thiz*/, jstring text) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    QLabel* label = new QLabel(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
    return registerHandle(label, /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_JQtLabel_nativeSetText(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring text) {
    QLabel* label = static_cast<QLabel*>(requireHandle(env, handle));
    if (label == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    label->setText(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
}

// ----------------------------------------------------------------------------
// 触摸键盘（TabTip）支持
// 无边框窗口下 Windows 的 TSF 焦点检测失效，输入框聚焦时不会自动弹出
// 屏幕键盘（Qt frameless 已知缺陷）。这里在聚焦/失焦时显式 Toggle TabTip。
// 仅当系统检测到触摸设备时生效（普通桌面不受影响）。
// ----------------------------------------------------------------------------
#ifdef _WIN32
// MinGW 无 itipinvocation.h：手动声明 ITipInvocation COM 接口
#define JQT_CLSID_TipInvocation {0x4ce576fa, 0x83dc, 0x4f88, {0x95, 0x1c, 0x9d, 0x07, 0x82, 0xb4, 0xe3, 0x76}}
#define JQT_IID_ITipInvocation  {0x37c994e7, 0x432b, 0x4834, {0xa2, 0xf7, 0xdc, 0xe1, 0xf1, 0x3b, 0x83, 0x46}}

struct JQtITipInvocation;
struct JQtITipInvocationVtbl {
    BEGIN_INTERFACE
    HRESULT (STDMETHODCALLTYPE* QueryInterface)(JQtITipInvocation*, REFIID, void**) = nullptr;
    ULONG   (STDMETHODCALLTYPE* AddRef)(JQtITipInvocation*) = nullptr;
    ULONG   (STDMETHODCALLTYPE* Release)(JQtITipInvocation*) = nullptr;
    HRESULT (STDMETHODCALLTYPE* Toggle)(JQtITipInvocation*, HWND) = nullptr;
    END_INTERFACE
};
struct JQtITipInvocation {
    JQtITipInvocationVtbl* lpVtbl;
};

static bool g_tabtipVisible = false;

static void jqtShowTouchKeyboard(bool show, HWND hwnd) {
    static const bool hasTouch = []() { return GetSystemMetrics(SM_MAXIMUMTOUCHES) > 0; }();
    if (!hasTouch) {
        return;
    }
    if (show == g_tabtipVisible) {
        return;
    }
    const HRESULT hrInit = CoInitializeEx(nullptr, COINIT_APARTMENTTHREADED);
    JQtITipInvocation* tip = nullptr;
    const IID clsid = JQT_CLSID_TipInvocation;
    const IID iid = JQT_IID_ITipInvocation;
    const HRESULT hr = CoCreateInstance(clsid, nullptr, CLSCTX_INPROC_SERVER,
                                        iid, reinterpret_cast<void**>(&tip));
    if (SUCCEEDED(hr) && tip != nullptr && tip->lpVtbl != nullptr && tip->lpVtbl->Toggle != nullptr) {
        tip->lpVtbl->Toggle(tip, hwnd);
        tip->lpVtbl->Release(tip);
        g_tabtipVisible = show;
    }
    if (hrInit == S_OK) {
        CoUninitialize();
    }
}
#endif

// ----------------------------------------------------------------------------
// JQtLineEdit：QLineEdit 的封装（textChanged / returnPressed 信号）
// ----------------------------------------------------------------------------

class JQtLineEditWidget : public QLineEdit {
public:
    explicit JQtLineEditWidget(const QString& text) : QLineEdit(text) {}

protected:
    void focusInEvent(QFocusEvent* event) override {
        QLineEdit::focusInEvent(event);
#ifdef _WIN32
        jqtShowTouchKeyboard(true, reinterpret_cast<HWND>(window()->winId()));
#endif
    }
    void focusOutEvent(QFocusEvent* event) override {
        QLineEdit::focusOutEvent(event);
#ifdef _WIN32
        jqtShowTouchKeyboard(false, reinterpret_cast<HWND>(window()->winId()));
#endif
    }
};

JNIEXPORT jlong JNICALL Java_org_jqt_JQtLineEdit_nativeCreate(JNIEnv* env, jobject thiz, jstring text) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    QLineEdit* edit = new JQtLineEditWidget(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);

    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(edit, &QLineEdit::textChanged, [gRef](const QString& t) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleTextChanged", "(Ljava/lang/String;)V");
        if (mid != nullptr) {
            jstring js = e->NewStringUTF(t.toUtf8().constData());
            JQT_CALL_VOID(e, gRef, mid, js);
            e->DeleteLocalRef(js);
        }
    });
    QObject::connect(edit, &QLineEdit::returnPressed, [gRef]() {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleReturnPressed", "()V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, gRef, mid);
        }
    });

    return registerHandle(edit, /*javaOwned=*/true);
}

JNIEXPORT jstring JNICALL Java_org_jqt_JQtLineEdit_nativeText(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QLineEdit* edit = static_cast<QLineEdit*>(requireHandle(env, handle));
    if (edit == nullptr) {
        return nullptr;
    }
    const QString t = edit->text();
    return env->NewStringUTF(t.toUtf8().constData());
}

JNIEXPORT void JNICALL Java_org_jqt_JQtLineEdit_nativeSetText(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring text) {
    QLineEdit* edit = static_cast<QLineEdit*>(requireHandle(env, handle));
    if (edit == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    edit->setText(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
}

JNIEXPORT void JNICALL Java_org_jqt_JQtLineEdit_nativeSetPlaceholderText(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring text) {
    QLineEdit* edit = static_cast<QLineEdit*>(requireHandle(env, handle));
    if (edit == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    edit->setPlaceholderText(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
}

// ----------------------------------------------------------------------------
// JQtComboBox：QComboBox 的封装（currentIndexChanged 信号）
// ----------------------------------------------------------------------------

JNIEXPORT jlong JNICALL Java_org_jqt_JQtComboBox_nativeCreate(JNIEnv* env, jobject thiz) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    QComboBox* combo = new QComboBox();

    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(combo, &QComboBox::currentIndexChanged, [gRef](int index) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleCurrentIndexChanged", "(I)V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(index));
        }
    });

    return registerHandle(combo, /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_JQtComboBox_nativeAddItem(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring text) {
    QComboBox* combo = static_cast<QComboBox*>(requireHandle(env, handle));
    if (combo == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    combo->addItem(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
}

JNIEXPORT jint JNICALL Java_org_jqt_JQtComboBox_nativeCurrentIndex(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QComboBox* combo = static_cast<QComboBox*>(requireHandle(env, handle));
    if (combo == nullptr) {
        return -1;
    }
    return static_cast<jint>(combo->currentIndex());
}

JNIEXPORT jstring JNICALL Java_org_jqt_JQtComboBox_nativeCurrentText(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QComboBox* combo = static_cast<QComboBox*>(requireHandle(env, handle));
    if (combo == nullptr) {
        return nullptr;
    }
    const QString t = combo->currentText();
    return env->NewStringUTF(t.toUtf8().constData());
}

JNIEXPORT void JNICALL Java_org_jqt_JQtComboBox_nativeSetCurrentIndex(JNIEnv* env, jobject /*thiz*/, jlong handle, jint index) {
    QComboBox* combo = static_cast<QComboBox*>(requireHandle(env, handle));
    if (combo == nullptr) {
        return;
    }
    combo->setCurrentIndex(static_cast<int>(index));
}

// ----------------------------------------------------------------------------
// JQtListWidget：QListWidget 的封装（itemClicked / currentRowChanged 信号）
// ----------------------------------------------------------------------------

JNIEXPORT jlong JNICALL Java_org_jqt_JQtListWidget_nativeCreate(JNIEnv* env, jobject thiz) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    QListWidget* list = new QListWidget();

    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(list, &QListWidget::itemClicked, [gRef, list](QListWidgetItem* item) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleItemClicked", "(I)V");
        if (mid != nullptr) {
            int row = (item != nullptr) ? list->row(item) : -1;
            JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(row));
        }
    });
    QObject::connect(list, &QListWidget::currentRowChanged, [gRef](int row) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleCurrentRowChanged", "(I)V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(row));
        }
    });

    return registerHandle(list, /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_JQtListWidget_nativeAddItem(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring text) {
    QListWidget* list = static_cast<QListWidget*>(requireHandle(env, handle));
    if (list == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    list->addItem(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
}

JNIEXPORT jint JNICALL Java_org_jqt_JQtListWidget_nativeCurrentRow(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QListWidget* list = static_cast<QListWidget*>(requireHandle(env, handle));
    if (list == nullptr) {
        return -1;
    }
    return static_cast<jint>(list->currentRow());
}

// ----------------------------------------------------------------------------
// JQtLayout / JQtVBoxLayout / JQtHBoxLayout：布局管理器
// ----------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_org_jqt_JQtLayout_nativeAddWidget(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong childHandle) {
    QBoxLayout* layout = static_cast<QBoxLayout*>(requireHandle(env, handle));
    if (layout == nullptr) {
        return;
    }
    QWidget* child = static_cast<QWidget*>(requireHandle(env, childHandle));
    if (child == nullptr) {
        return;
    }
    layout->addWidget(child);
    markQtOwned(childHandle);  // 布局加入后归 Qt 管理（最终 reparent 到窗口）
    child->show();
}

JNIEXPORT void JNICALL Java_org_jqt_JQtLayout_nativeSetSpacing(JNIEnv* env, jobject /*thiz*/, jlong handle, jint spacing) {
    QBoxLayout* layout = static_cast<QBoxLayout*>(requireHandle(env, handle));
    if (layout == nullptr) {
        return;
    }
    layout->setSpacing(static_cast<int>(spacing));
}

// 布局四周留白（外边距）
JNIEXPORT void JNICALL Java_org_jqt_JQtLayout_nativeSetContentsMargins(JNIEnv* env, jobject /*thiz*/, jlong handle, jint left, jint top, jint right, jint bottom) {
    QBoxLayout* layout = static_cast<QBoxLayout*>(requireHandle(env, handle));
    if (layout == nullptr) {
        return;
    }
    layout->setContentsMargins(static_cast<int>(left), static_cast<int>(top),
                               static_cast<int>(right), static_cast<int>(bottom));
}

// 布局嵌套：把子布局加入本布局（如 VBox 中嵌 HBox 做标题栏/工具行）
JNIEXPORT void JNICALL Java_org_jqt_JQtLayout_nativeAddLayout(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong childLayoutHandle) {
    QBoxLayout* layout = static_cast<QBoxLayout*>(requireHandle(env, handle));
    if (layout == nullptr) {
        return;
    }
    QBoxLayout* child = static_cast<QBoxLayout*>(requireHandle(env, childLayoutHandle));
    if (child == nullptr) {
        return;
    }
    layout->addLayout(child);
    markQtOwned(childLayoutHandle);  // 子布局归父布局管理
}

JNIEXPORT void JNICALL Java_org_jqt_JQtLayout_nativeAddStretch(JNIEnv* env, jobject /*thiz*/, jlong handle, jint stretch) {
    QBoxLayout* layout = static_cast<QBoxLayout*>(requireHandle(env, handle));
    if (layout == nullptr) {
        return;
    }
    layout->addStretch(static_cast<int>(stretch));
}

JNIEXPORT jlong JNICALL Java_org_jqt_JQtVBoxLayout_nativeCreate(JNIEnv* env, jobject /*thiz*/) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    // 未 setLayout 前归 Java 管理（Cleaner 回收）；setLayout 后 markQtOwned
    return registerHandle(new QVBoxLayout(), /*javaOwned=*/true);
}

JNIEXPORT jlong JNICALL Java_org_jqt_JQtHBoxLayout_nativeCreate(JNIEnv* env, jobject /*thiz*/) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    return registerHandle(new QHBoxLayout(), /*javaOwned=*/true);
}

// ----------------------------------------------------------------------------
// JQtPanel：QFrame 卡片/容器（Fluent 卡片基座）
// ----------------------------------------------------------------------------

JNIEXPORT jlong JNICALL Java_org_jqt_JQtPanel_nativeCreate(JNIEnv* env, jobject /*thiz*/) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    return registerHandle(new QFrame(), /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_JQtPanel_nativeAddWidget(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong childHandle) {
    QFrame* frame = static_cast<QFrame*>(requireHandle(env, handle));
    if (frame == nullptr) {
        return;
    }
    QWidget* child = static_cast<QWidget*>(requireHandle(env, childHandle));
    if (child == nullptr) {
        return;
    }
    child->setParent(frame);
    markQtOwned(childHandle);
    int n = 0;
    const QList<QObject*>& children = frame->children();
    for (QObject* obj : children) {
        if (qobject_cast<QWidget*>(obj) != nullptr) {
            ++n;
        }
    }
    child->move(10, 10 + (n - 1) * 40);
    child->show();
}

// ----------------------------------------------------------------------------
// JQtCheckBox：QCheckBox 的封装（toggled 信号，QSS 可做 Fluent 开关）
// ----------------------------------------------------------------------------

JNIEXPORT jlong JNICALL Java_org_jqt_JQtCheckBox_nativeCreate(JNIEnv* env, jobject thiz, jstring text) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    QCheckBox* box = new QCheckBox(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);

    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(box, &QCheckBox::toggled, [gRef](bool checked) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleToggled", "(Z)V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, gRef, mid, static_cast<jboolean>(checked));
        }
    });

    return registerHandle(box, /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_JQtCheckBox_nativeSetText(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring text) {
    QCheckBox* box = static_cast<QCheckBox*>(requireHandle(env, handle));
    if (box == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    box->setText(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
}

JNIEXPORT jboolean JNICALL Java_org_jqt_JQtCheckBox_nativeIsChecked(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QCheckBox* box = static_cast<QCheckBox*>(requireHandle(env, handle));
    if (box == nullptr) {
        return JNI_FALSE;
    }
    return box->isChecked() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_org_jqt_JQtCheckBox_nativeSetChecked(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean checked) {
    QCheckBox* box = static_cast<QCheckBox*>(requireHandle(env, handle));
    if (box == nullptr) {
        return;
    }
    box->setChecked(checked == JNI_TRUE);
}

// ----------------------------------------------------------------------------
// JQtTitleBar：跨平台标题栏容器（Windows 三件套 / macOS 交通灯）
// ----------------------------------------------------------------------------

JNIEXPORT jlong JNICALL Java_org_jqt_JQtTitleBar_nativeCreate(JNIEnv* env, jobject /*thiz*/) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    QFrame* frame = new QFrame();
    frame->setFixedHeight(36);
    return registerHandle(frame, /*javaOwned=*/true);
}

// ----------------------------------------------------------------------------
// 动画扩展：带缓动函数的版本（JQtEasing 枚举 → QEasingCurve）
// ----------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_org_jqt_JQtWidget_nativeAnimateMoveEasing(JNIEnv* env, jclass /*cls*/, jlong handle, jint x, jint y, jlong ms, jint easing) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    QPropertyAnimation* anim = new QPropertyAnimation(widget, "pos", widget);
    anim->setDuration(static_cast<int>(ms));
    anim->setEndValue(QPoint(static_cast<int>(x), static_cast<int>(y)));
    anim->setEasingCurve(static_cast<QEasingCurve::Type>(easing));
    anim->start(QAbstractAnimation::DeleteWhenStopped);
}

JNIEXPORT void JNICALL Java_org_jqt_JQtWidget_nativeAnimateResizeEasing(JNIEnv* env, jclass /*cls*/, jlong handle, jint w, jint h, jlong ms, jint easing) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    QPropertyAnimation* anim = new QPropertyAnimation(widget, "size", widget);
    anim->setDuration(static_cast<int>(ms));
    anim->setEndValue(QSize(static_cast<int>(w), static_cast<int>(h)));
    anim->setEasingCurve(static_cast<QEasingCurve::Type>(easing));
    anim->start(QAbstractAnimation::DeleteWhenStopped);
}

JNIEXPORT void JNICALL Java_org_jqt_JQtWindow_nativeFadeInEasing(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong ms, jint easing) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    QPropertyAnimation* anim = new QPropertyAnimation(widget, "windowOpacity", widget);
    anim->setDuration(static_cast<int>(ms));
    anim->setStartValue(0.0);
    anim->setEndValue(1.0);
    anim->setEasingCurve(static_cast<QEasingCurve::Type>(easing));
    anim->start(QAbstractAnimation::DeleteWhenStopped);
}

JNIEXPORT void JNICALL Java_org_jqt_JQtWindow_nativeFadeOutEasing(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong ms, jint easing) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    QPropertyAnimation* anim = new QPropertyAnimation(widget, "windowOpacity", widget);
    anim->setDuration(static_cast<int>(ms));
    anim->setStartValue(widget->windowOpacity());
    anim->setEndValue(0.0);
    anim->setEasingCurve(static_cast<QEasingCurve::Type>(easing));
    anim->start(QAbstractAnimation::DeleteWhenStopped);
}

// 通用数字属性动画（JQtAnimation 的核心 native）
// 属性名如 "windowOpacity"；值范围 from → to（double）
JNIEXPORT jlong JNICALL Java_org_jqt_JQtWidget_nativeCreateAnimation(JNIEnv* env, jclass /*cls*/, jlong handle, jstring property, jdouble from, jdouble to, jlong ms, jint easing) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return 0;
    }
    const char* utf = env->GetStringUTFChars(property, nullptr);
    // windowOpacity 只对顶层窗口有效：子控件上静默无效，给提示
    if (!widget->isWindow() && strcmp(utf, "windowOpacity") == 0) {
        fprintf(stderr, "[JQt] warning: windowOpacity only affects top-level windows; "
                        "apply it to a JQtWindow or use geometry/move animations on widgets\n");
    }
    QPropertyAnimation* anim = new QPropertyAnimation(widget, QByteArray(utf), widget);
    env->ReleaseStringUTFChars(property, utf);
    anim->setDuration(static_cast<int>(ms));
    anim->setStartValue(from);
    anim->setEndValue(to);
    anim->setEasingCurve(static_cast<QEasingCurve::Type>(easing));
    return registerHandle(anim, /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_JQtWidget_nativeAnimationSetLoopCount(JNIEnv* env, jclass /*cls*/, jlong animHandle, jint loops) {
    QPropertyAnimation* anim = static_cast<QPropertyAnimation*>(requireHandle(env, animHandle));
    if (anim == nullptr) {
        return;
    }
    anim->setLoopCount(static_cast<int>(loops));
}

JNIEXPORT void JNICALL Java_org_jqt_JQtWidget_nativeAnimationStart(JNIEnv* env, jclass /*cls*/, jlong animHandle) {
    QPropertyAnimation* anim = static_cast<QPropertyAnimation*>(requireHandle(env, animHandle));
    if (anim == nullptr) {
        return;
    }
    anim->start(QAbstractAnimation::DeleteWhenStopped);
}

JNIEXPORT void JNICALL Java_org_jqt_JQtWidget_nativeAnimationStop(JNIEnv* env, jclass /*cls*/, jlong animHandle) {
    QPropertyAnimation* anim = static_cast<QPropertyAnimation*>(requireHandle(env, animHandle));
    if (anim == nullptr) {
        return;
    }
    anim->stop();
}

// 动画完成回调注册表：animHandle → Java 弱引用（JQtAnimation 构造时注册）
// 弱引用不阻止 Java 对象 GC；finished 回调一次性（触发后即注销并释放）。
// 动画对象自身带 DeleteWhenStopped（自清理），finished 信号触发 Java 侧回调。
static std::unordered_map<int64_t, jweak> g_animCallbacks;
static std::mutex g_animMutex;

JNIEXPORT void JNICALL Java_org_jqt_JQtWidget_nativeRegisterAnimation(JNIEnv* env, jclass /*cls*/, jlong animHandle, jobject animObj) {
    QPropertyAnimation* anim = static_cast<QPropertyAnimation*>(requireHandle(env, animHandle));
    if (anim == nullptr) {
        return;
    }
    jweak wRef = env->NewWeakGlobalRef(animObj);
    {
        std::lock_guard<std::mutex> lock(g_animMutex);
        g_animCallbacks[static_cast<int64_t>(animHandle)] = wRef;
    }
    QObject::connect(anim, &QPropertyAnimation::finished, [animHandle]() {
        JNIEnv* e = callbackEnv();
        jweak wRef = nullptr;
        {
            std::lock_guard<std::mutex> lock(g_animMutex);
            auto it = g_animCallbacks.find(static_cast<int64_t>(animHandle));
            if (it != g_animCallbacks.end()) {
                wRef = it->second;
                g_animCallbacks.erase(it);
            }
        }
        if (wRef == nullptr) {
            return;
        }
        // 弱引用 → 局部强引用；Java 对象已被 GC 则跳过回调
        jobject local = e->NewLocalRef(wRef);
        e->DeleteWeakGlobalRef(wRef);
        if (local == nullptr) {
            return;
        }
        jclass cls = e->GetObjectClass(local);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleFinished", "()V");
        if (mid != nullptr) {
            e->CallVoidMethod(local, mid);
            if (e->ExceptionCheck()) {
                e->ExceptionDescribe();
                e->ExceptionClear();
            }
        }
        e->DeleteLocalRef(local);
    });
}

// ----------------------------------------------------------------------------
// JQtPivot：Fluent 选项卡（文本项 + 底部滑动指示器，200ms OutCubic）
// 纯自绘（clean-room 独立实现）；文本色跟随 palette，指示器用 Highlight 色。
// ----------------------------------------------------------------------------

class JQtPivotWidget : public QWidget {
public:
    explicit JQtPivotWidget() {
        setFixedHeight(36);
        setCursor(Qt::PointingHandCursor);
        m_anim = new QVariantAnimation(this);
        m_anim->setDuration(200);
        m_anim->setEasingCurve(QEasingCurve::OutCubic);
        QObject::connect(m_anim, &QVariantAnimation::valueChanged, this,
                         [this](const QVariant& v) {
                             m_indicatorX = v.toDouble();
                             update();
                         });
    }

    std::function<void(int)> onChanged;

    void addItem(const QString& text) {
        m_items.push_back(text);
        update();
    }

    int currentIndex() const { return m_current; }

    void setCurrentIndex(int index) {
        if (index < 0 || index >= static_cast<int>(m_items.size()) || index == m_current) {
            return;
        }
        m_current = index;
        if (onChanged) {
            onChanged(index);
        }
        const double from = m_indicatorX;
        const double to = cellWidth() * m_current;
        m_anim->stop();
        m_anim->setStartValue(from);
        m_anim->setEndValue(to);
        m_anim->start();
    }

protected:
    void paintEvent(QPaintEvent*) override {
        QPainter p(this);
        p.setRenderHint(QPainter::Antialiasing);
        const int n = static_cast<int>(m_items.size());
        if (n == 0) {
            return;
        }
        const double cw = cellWidth();
        const QColor accent = palette().color(QPalette::Highlight);
        const QColor dim = palette().color(QPalette::Text);
        QColor dimFaded = dim;
        dimFaded.setAlpha(160);

        p.setFont(font());
        for (int i = 0; i < n; ++i) {
            const QRectF r(i * cw, 0, cw, height());
            p.setPen(i == m_current ? accent : dimFaded);
            p.drawText(r, Qt::AlignCenter, m_items[i]);
        }
        const double iw = cw * 0.4;
        const double ix = m_indicatorX + (cw - iw) / 2.0;
        p.fillRect(QRectF(ix, height() - 3.0, iw, 3.0), accent);
    }

    void mousePressEvent(QMouseEvent* event) override {
        if (m_items.empty()) {
            return;
        }
        const int idx = static_cast<int>(event->position().x() / cellWidth());
        if (idx >= 0 && idx < static_cast<int>(m_items.size())) {
            setCurrentIndex(idx);
        }
    }

private:
    double cellWidth() const {
        const int n = static_cast<int>(m_items.size());
        return n == 0 ? width() : width() / static_cast<double>(n);
    }

    std::vector<QString> m_items;
    int m_current = 0;
    double m_indicatorX = 0.0;
    QVariantAnimation* m_anim;
};

JNIEXPORT jlong JNICALL Java_org_jqt_JQtPivot_nativeCreate(JNIEnv* env, jobject thiz) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    JQtPivotWidget* pivot = new JQtPivotWidget();
    jobject gRef = env->NewGlobalRef(thiz);
    pivot->onChanged = [gRef](int index) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleChanged", "(I)V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(index));
        }
    };
    return registerHandle(pivot, /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_JQtPivot_nativeAddItem(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring text) {
    JQtPivotWidget* pivot = static_cast<JQtPivotWidget*>(requireHandle(env, handle));
    if (pivot == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    pivot->addItem(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
}

JNIEXPORT jint JNICALL Java_org_jqt_JQtPivot_nativeCurrentIndex(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    JQtPivotWidget* pivot = static_cast<JQtPivotWidget*>(requireHandle(env, handle));
    if (pivot == nullptr) {
        return 0;
    }
    return static_cast<jint>(pivot->currentIndex());
}

JNIEXPORT void JNICALL Java_org_jqt_JQtPivot_nativeSetCurrentIndex(JNIEnv* env, jobject /*thiz*/, jlong handle, jint index) {
    JQtPivotWidget* pivot = static_cast<JQtPivotWidget*>(requireHandle(env, handle));
    if (pivot == nullptr) {
        return;
    }
    pivot->setCurrentIndex(static_cast<int>(index));
}

// ----------------------------------------------------------------------------
// JQtSwitch：自定义开关控件（轨道 + 滑块 + 位移动画）
// 纯 paintEvent 绘制（不依赖 QSS 子控件），滑块位置由属性动画驱动。
// ----------------------------------------------------------------------------

class JQtSwitchWidget : public QWidget {
public:
    explicit JQtSwitchWidget(bool checked)
        : m_checked(checked), m_progress(checked ? 1.0 : 0.0) {
        setFixedSize(44, 22);
        setCursor(Qt::PointingHandCursor);
        // 滑块位移动画（progress 0~1，QVariantAnimation 免 moc）
        m_anim = new QVariantAnimation(this);
        m_anim->setDuration(180);
        m_anim->setEasingCurve(QEasingCurve::OutCubic);
        QObject::connect(m_anim, &QVariantAnimation::valueChanged, this,
                         [this](const QVariant& v) { setProgress(v.toDouble()); });
    }

    std::function<void(bool)> onToggled;

    bool isChecked() const { return m_checked; }

    void setChecked(bool checked) {
        if (m_checked == checked) {
            return;
        }
        m_checked = checked;
        m_anim->stop();
        m_anim->setStartValue(m_progress);
        m_anim->setEndValue(checked ? 1.0 : 0.0);
        m_anim->start();
        if (onToggled) {
            onToggled(checked);
        }
    }

    double progress() const { return m_progress; }
    void setProgress(double p) {
        m_progress = p;
        update();
    }

protected:
    void paintEvent(QPaintEvent*) override {
        QPainter painter(this);
        painter.setRenderHint(QPainter::Antialiasing);

        const double w = width();
        const double h = height();

        // 轨道颜色随进度渐变：关=灰 → 开=全局主题色（setAccentColor 可换）
        const QColor offColor(0x4a, 0x4a, 0x4a);
        const QColor onColor = g_accentColor;
        const double t = m_progress;
        const QColor trackColor(
            int(offColor.red()   + (onColor.red()   - offColor.red())   * t),
            int(offColor.green() + (onColor.green() - offColor.green()) * t),
            int(offColor.blue()  + (onColor.blue()  - offColor.blue())  * t));
        QPainterPath track;
        track.addRoundedRect(0.5, 0.5, w - 1, h - 1, h / 2.0, h / 2.0);
        painter.fillPath(track, trackColor);

        // 滑块（按 progress 平滑移动）
        const double r = h - 4;
        const double x = 2 + m_progress * (w - r - 4);
        painter.setBrush(Qt::white);
        painter.setPen(Qt::NoPen);
        painter.drawEllipse(QPointF(x + r / 2.0, h / 2.0), r / 2.0, r / 2.0);
    }

    void mousePressEvent(QMouseEvent* event) override {
        fprintf(stderr, "[JQt] switch mousePress at %.0f,%.0f\n",
                event->position().x(), event->position().y());
        if (event->button() == Qt::LeftButton) {
            setChecked(!m_checked);
        }
        QWidget::mousePressEvent(event);
    }

private:
    bool m_checked;
    double m_progress;
    QVariantAnimation* m_anim;
};

JNIEXPORT jlong JNICALL Java_org_jqt_JQtSwitch_nativeCreate(JNIEnv* env, jobject thiz, jboolean checked) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    JQtSwitchWidget* sw = new JQtSwitchWidget(checked == JNI_TRUE);
    jobject gRef = env->NewGlobalRef(thiz);
    sw->onToggled = [gRef](bool on) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleToggled", "(Z)V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, gRef, mid, static_cast<jboolean>(on));
        }
    };
    return registerHandle(sw, /*javaOwned=*/true);
}

JNIEXPORT jboolean JNICALL Java_org_jqt_JQtSwitch_nativeIsChecked(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    JQtSwitchWidget* sw = static_cast<JQtSwitchWidget*>(requireHandle(env, handle));
    if (sw == nullptr) {
        return JNI_FALSE;
    }
    return sw->isChecked() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_org_jqt_JQtSwitch_nativeSetChecked(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean checked) {
    JQtSwitchWidget* sw = static_cast<JQtSwitchWidget*>(requireHandle(env, handle));
    if (sw == nullptr) {
        return;
    }
    sw->setChecked(checked == JNI_TRUE);
}

// ---- 自动化命中测试（诊断用）----
// 向窗口发送真实 WM_LBUTTONDOWN/UP 消息，点击目标控件中心。
// 走完整链路：Windows 消息 → WM_NCHITTEST → Qt 事件分发 → 子控件。
#ifdef _WIN32
JNIEXPORT void JNICALL Java_org_jqt_JQtWidget_nativePostClickAt(JNIEnv* env, jclass /*cls*/, jlong targetHandle, jlong winHandle) {
    QWidget* target = static_cast<QWidget*>(requireHandle(env, targetHandle));
    QWidget* win = static_cast<QWidget*>(requireHandle(env, winHandle));
    if (target == nullptr || win == nullptr) {
        return;
    }
    const QPoint pos = target->mapTo(win, QPoint(target->width() / 2, target->height() / 2));
    const qreal dpr = win->devicePixelRatioF();
    HWND hwnd = reinterpret_cast<HWND>(win->winId());
    const LONG lp = MAKELPARAM(qRound(pos.x() * dpr), qRound(pos.y() * dpr));
    fprintf(stderr, "[JQt] postClick target at win (%d,%d) dpr=%.2f\n",
            static_cast<int>(pos.x()), static_cast<int>(pos.y()), dpr);
    PostMessageW(hwnd, WM_LBUTTONDOWN, MK_LBUTTON, lp);
    PostMessageW(hwnd, WM_LBUTTONUP, 0, lp);
}
#endif

// ----------------------------------------------------------------------------
// 动画系统：QPropertyAnimation 封装（淡入淡出 / 移动 / 缩放）
// 说明：QSS 不支持 CSS transition，Qt 动画必须走属性动画 API。
// ----------------------------------------------------------------------------

// 窗口淡入：透明度 0 → 1（默认 200ms）
JNIEXPORT void JNICALL Java_org_jqt_JQtWindow_nativeFadeIn(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong ms) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    QPropertyAnimation* anim = new QPropertyAnimation(widget, "windowOpacity", widget);
    anim->setDuration(static_cast<int>(ms));
    anim->setStartValue(0.0);
    anim->setEndValue(1.0);
    anim->setEasingCurve(QEasingCurve::OutCubic);
    anim->start(QAbstractAnimation::DeleteWhenStopped);
}

// 窗口淡出：透明度 1 → 0（结束后可回调）
JNIEXPORT void JNICALL Java_org_jqt_JQtWindow_nativeFadeOut(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong ms) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    QPropertyAnimation* anim = new QPropertyAnimation(widget, "windowOpacity", widget);
    anim->setDuration(static_cast<int>(ms));
    anim->setStartValue(widget->windowOpacity());
    anim->setEndValue(0.0);
    anim->setEasingCurve(QEasingCurve::InCubic);
    anim->start(QAbstractAnimation::DeleteWhenStopped);
}

// 控件平滑移动到目标位置（属性动画）
JNIEXPORT void JNICALL Java_org_jqt_JQtWidget_nativeAnimateMove(JNIEnv* env, jclass /*cls*/, jlong handle, jint x, jint y, jlong ms) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    QPropertyAnimation* anim = new QPropertyAnimation(widget, "pos", widget);
    anim->setDuration(static_cast<int>(ms));
    anim->setEndValue(QPoint(static_cast<int>(x), static_cast<int>(y)));
    anim->setEasingCurve(QEasingCurve::OutCubic);
    anim->start(QAbstractAnimation::DeleteWhenStopped);
}

// 控件平滑缩放到目标尺寸（属性动画）
JNIEXPORT void JNICALL Java_org_jqt_JQtWidget_nativeAnimateResize(JNIEnv* env, jclass /*cls*/, jlong handle, jint w, jint h, jlong ms) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    QPropertyAnimation* anim = new QPropertyAnimation(widget, "size", widget);
    anim->setDuration(static_cast<int>(ms));
    anim->setEndValue(QSize(static_cast<int>(w), static_cast<int>(h)));
    anim->setEasingCurve(QEasingCurve::OutCubic);
    anim->start(QAbstractAnimation::DeleteWhenStopped);
}

// 控件淡入（透明度 0 → 1）
JNIEXPORT void JNICALL Java_org_jqt_JQtWidget_nativeFadeIn(JNIEnv* env, jclass /*cls*/, jlong handle, jlong ms) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    QGraphicsOpacityEffect* effect = new QGraphicsOpacityEffect(widget);
    widget->setGraphicsEffect(effect);
    QPropertyAnimation* anim = new QPropertyAnimation(effect, "opacity", widget);
    anim->setDuration(static_cast<int>(ms));
    anim->setStartValue(0.0);
    anim->setEndValue(1.0);
    anim->setEasingCurve(QEasingCurve::OutCubic);
    anim->start(QAbstractAnimation::DeleteWhenStopped);
}

// 控件淡出（透明度 1 → 0）
JNIEXPORT void JNICALL Java_org_jqt_JQtWidget_nativeFadeOut(JNIEnv* env, jclass /*cls*/, jlong handle, jlong ms) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    QGraphicsOpacityEffect* effect = new QGraphicsOpacityEffect(widget);
    widget->setGraphicsEffect(effect);
    QPropertyAnimation* anim = new QPropertyAnimation(effect, "opacity", widget);
    anim->setDuration(static_cast<int>(ms));
    anim->setStartValue(1.0);
    anim->setEndValue(0.0);
    anim->setEasingCurve(QEasingCurve::InCubic);
    anim->start(QAbstractAnimation::DeleteWhenStopped);
}

// ----------------------------------------------------------------------------
// 投影阴影（QSS box-shadow 的替代：Qt QSS 不支持 box-shadow 属性）
// QGraphicsDropShadowEffect；注意：与 QSS 样式化控件组合存在崩溃风险
// （Qt 已知 issue），若目标控件应用了 QSS 背景请先实测或改用两层 QFrame 方案。
// ----------------------------------------------------------------------------
JNIEXPORT void JNICALL Java_org_jqt_JQtWidget_nativeSetDropShadow(JNIEnv* env, jclass /*cls*/, jlong handle, jint blur, jint alpha, jint dx, jint dy) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    QGraphicsDropShadowEffect* fx = new QGraphicsDropShadowEffect(widget);
    fx->setBlurRadius(static_cast<qreal>(blur));
    fx->setColor(QColor(0, 0, 0, alpha));
    fx->setOffset(static_cast<qreal>(dx), static_cast<qreal>(dy));
    widget->setGraphicsEffect(fx);
}

JNIEXPORT void JNICALL Java_org_jqt_JQtWidget_nativeClearDropShadow(JNIEnv* env, jclass /*cls*/, jlong handle) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    widget->setGraphicsEffect(nullptr);
}

// ----------------------------------------------------------------------------
// Fluent 动效库（JQtAnimations）：hover 开关 / 入场 / 退场
// clean-room 独立实现；视觉参数参考微软 Fluent 公开动效规范。
// ----------------------------------------------------------------------------

// 全局按钮悬停动画开关（跟随 JQtAnimationTheme）
JNIEXPORT void JNICALL Java_org_jqt_JQtAnimations_nativeSetHoverEnabled(JNIEnv* /*env*/, jclass /*cls*/, jboolean on) {
    g_hoverEnabled = (on == JNI_TRUE);
}

// 悬停高亮强度（0~1，默认 0.085）
JNIEXPORT void JNICALL Java_org_jqt_JQtAnimations_nativeSetHoverIntensity(JNIEnv* /*env*/, jclass /*cls*/, jdouble intensity) {
    g_hoverIntensity = intensity;
}

// 控件入场：下方 dy 滑入（纯位移动画）。
// 注意：不用 QGraphicsOpacityEffect —— QSS 样式化控件 + 透明度特效是 Qt 已知崩溃
// 组合（渲染样式表时空指针）；Fluent 入场动效以位移为主，淡入由窗口级实现。
JNIEXPORT void JNICALL Java_org_jqt_JQtAnimations_nativeEntrance(JNIEnv* env, jclass /*cls*/, jlong handle, jint dy, jlong ms, jint easing) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    const QPoint base = widget->pos();
    QPropertyAnimation* anim = new QPropertyAnimation(widget, "pos", widget);
    anim->setDuration(static_cast<int>(ms));
    anim->setStartValue(base + QPoint(0, dy));
    anim->setEndValue(base);
    anim->setEasingCurve(static_cast<QEasingCurve::Type>(easing));
    anim->start(QAbstractAnimation::DeleteWhenStopped);
}

// 控件退场：下移 + 动画结束后隐藏（纯位移，同上规避 effect 崩溃）
JNIEXPORT void JNICALL Java_org_jqt_JQtAnimations_nativeExit(JNIEnv* env, jclass /*cls*/, jlong handle, jint dy, jlong ms, jint easing) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    const QPoint base = widget->pos();
    QPropertyAnimation* anim = new QPropertyAnimation(widget, "pos", widget);
    anim->setDuration(static_cast<int>(ms));
    anim->setStartValue(base);
    anim->setEndValue(base + QPoint(0, dy));
    anim->setEasingCurve(static_cast<QEasingCurve::Type>(easing));
    QObject::connect(anim, &QPropertyAnimation::finished, widget, [widget]() {
        widget->hide();
    });
    anim->start(QAbstractAnimation::DeleteWhenStopped);
}
