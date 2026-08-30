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
#include <shobjidl.h>
#include <winreg.h>
#include <windowsx.h>
#include <dwmapi.h>
#include <objbase.h>
// MinGW 旧头可能缺触摸指针消息常量
#ifndef WM_POINTERCANCEL
#define WM_POINTERCANCEL 0x0249
#endif
#endif

// 平台独家能力所需的系统头（v0.7.0：macOS Dock/NSWindow/NSProcessInfo，Linux D-Bus）
#if defined(__APPLE__)
#include <objc/objc.h>
#include <objc/message.h>
#include <objc/runtime.h>
#include <CoreFoundation/CoreFoundation.h>
// objc_msgSend 原型随 Xcode/SDK 变化（arm64 新 SDK 为 void(void)），
// 且 variadic 调用对多参数混合类型在 arm64 上不可靠（beginActivity reason 传空）——
// 统一用精确签名函数指针 cast，完全绕开原型与 variadic 传递问题。
typedef id    (*JQtMsg0)(id, SEL);
typedef id    (*JQtMsg1)(id, SEL, id);
typedef void  (*JQtMsgV1)(id, SEL, id);
typedef id    (*JQtMsgOpt)(id, SEL, unsigned long, id);   // beginActivityWithOptions:reason:
typedef void  (*JQtMsgEnd)(id, SEL, id);                  // endActivity:
typedef void  (*JQtMsgBool)(id, SEL, bool);               // setTitlebarAppearsTransparent:
typedef unsigned long (*JQtMsgUL)(id, SEL);               // styleMask
typedef void  (*JQtMsgSetMask)(id, SEL, unsigned long);   // setStyleMask:
#define JQT_OBJC_CAST(FN) reinterpret_cast<FN>(objc_msgSend)
#endif
#if defined(__linux__)
#include <QtDBus>
#endif

#include <QApplication>
#include <QGuiApplication>
#include <QBuffer>
#include <QKeySequence>
#include <cstring>
#include <QAbstractNativeEventFilter>
#include <QBoxLayout>
#include <QCheckBox>
#include <QFrame>
#include <QMainWindow>
#include <QPushButton>
#include <QAction>
#include <QStackedWidget>
#include <QGraphicsOpacityEffect>
#include <QGraphicsDropShadowEffect>
#include <QPainter>
#include <QPainterPath>
#include <QPropertyAnimation>
#include <QProgressBar>
#include <QScrollArea>
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
#include <QStringListModel>
#include <QItemSelectionModel>
#include <QTableWidget>
#include <QTreeWidget>
#include <QTableWidgetItem>
#include <QTreeWidgetItem>
#include <QTabWidget>
#include <QGroupBox>
#include <QStackedLayout>
#include <QSplitter>
#include <QSpinBox>
#include <QDial>
#include <QRadioButton>
#include <QDateTimeEdit>
#include <QDateTime>
#include <QClipboard>
#include <QPixmap>
#include <QImage>
#include <QSettings>
#include <QGridLayout>
#include <QFormLayout>
#include <QMenu>
#include <QMenuBar>
#include <QToolBar>
#include <QStatusBar>
#include <QSystemTrayIcon>
#include <QStyle>
#include <QTextEdit>
#include <QPlainTextEdit>
#include <QPainter>
#include <QMessageBox>
#include <QTimer>
#include <QMessageBox>
#include <QTimer>
#include <QInputDialog>
#include <QFileDialog>
#include <QColorDialog>
#include <QFontDialog>
#include <QPrinter>
#include <QPageSize>
#include <QtSql/QSqlDatabase>
#include <QtSql/QSqlQuery>
#include <QtSql/QSqlError>
#include <QtSql/QSqlRecord>
#include <QtSql/QSqlDriver>
#include <QtSql/QSqlDriverPlugin>
// QOpenGLWidget 实现仅 Windows/Linux（macOS 与 Windows ARM64 的 Qt 构建不含 OpenGLWidgets 模块）
#if (defined(_WIN32) && !defined(_M_ARM64)) || defined(__linux__)
#include <QOpenGLWidget>
#include <QOpenGLFunctions>
#include <QOpenGLContext>
#endif
// QtSerialPort 头布局因平台而异（mac framework fake-header / 扁平 / 标准布局）——双 include 保险
#include <QSerialPort>
#include <QSerialPortInfo>
#include <QtSerialPort/QSerialPort>
#include <QtSerialPort/QSerialPortInfo>
#include <QPluginLoader>
#include <QLibraryInfo>
#include <QDir>
#include <QFile>
#include <QFileInfo>
#include <QMetaObject>
#include <QMoveEvent>
#include <QPushButton>
#include <QResizeEvent>
#include <QTimer>
#include <QVBoxLayout>
#include <QWidget>
#include <QWindow>
#include <QStandardPaths>
#include <QTextStream>

#include <atomic>
#include <functional>

// 崩溃日志：Windows SEH 未处理异常时追加 jqt-crash.log（时间/异常码/地址/线程）
// 写入后继续交给系统默认处理（错误框），便于诊断 native 崩溃。
#ifdef _WIN32
#ifdef _WIN32
static void jqtDispatchHotkey(int hotkeyId);   // 前置声明（定义见 Exclusive Kit 区）
#endif

static LONG WINAPI jqtCrashHandler(EXCEPTION_POINTERS* ep) {
    FILE* f = fopen("jqt-crash.log", "a");
    if (f != nullptr) {
        SYSTEMTIME st;
        GetLocalTime(&st);
        fprintf(f, "[%04d-%02d-%02d %02d:%02d:%02d] EXCEPTION code=0x%08lX addr=%p thread=%lu\n",
                st.wYear, st.wMonth, st.wDay, st.wHour, st.wMinute, st.wSecond,
                ep->ExceptionRecord->ExceptionCode,
                ep->ExceptionRecord->ExceptionAddress,
                GetCurrentThreadId());
        fclose(f);
    }
    return EXCEPTION_CONTINUE_SEARCH;
}
#endif
#include <mutex>
#include <unordered_map>
#include <unordered_set>

// 全局主题色（JQtApplication.setAccentColor 更新；自绘控件 JQtSwitch 使用）
static QColor g_accentColor = QColor(0x4c, 0xc2, 0xff);

// 亚克力窗口集合（拖动时临时禁用亚克力——模糊重采样是拖动卡顿的已知元凶）
#ifdef _WIN32
static std::unordered_set<HWND> g_acrylicWindows;

static void jqtSetAcrylic(HWND hwnd, bool on) {
    HMODULE user32 = GetModuleHandleW(L"user32.dll");
    if (user32 == nullptr) {
        return;
    }
    typedef BOOL(WINAPI* SetWCAFunc)(HWND, void*);
    auto fn = reinterpret_cast<SetWCAFunc>(GetProcAddress(user32, "SetWindowCompositionAttribute"));
    if (fn == nullptr) {
        return;
    }
    struct ACCENT_POLICY { int state; int flags; int color; int animation; };
    struct WINCOMPATTRDATA { int attr; void* data; unsigned long size; };
    ACCENT_POLICY ap;
    ap.state = on ? 4 : 0;                              // ACCENT_ENABLE_ACRYLICBLURBEHIND / DISABLED
    ap.flags = on ? (0x20 | 0x40 | 0x80 | 0x100) : 0;
    ap.color = on ? 0x99F2F2F2 : 0;
    ap.animation = 0;
    WINCOMPATTRDATA data;
    data.attr = 19;                                     // WCA_ACCENT_POLICY
    data.data = &ap;
    data.size = sizeof(ap);
    fn(hwnd, &data);
}
#endif

#include "generated/org_jqt_QApplication.h"
#include "generated/org_jqt_JQtAnimations.h"
#include "generated/org_jqt_JQtInfoBar.h"
#include "generated/org_jqt_QMessageBox.h"
#include "generated/org_jqt_QInputDialog.h"
#include "generated/org_jqt_QFileDialog.h"
#include "generated/org_jqt_QColorDialog.h"
#include "generated/org_jqt_QFontDialog.h"
#include "generated/org_jqt_QTableWidget.h"
#include "generated/org_jqt_QTreeWidget.h"
#include "generated/org_jqt_QTabWidget.h"
#include "generated/org_jqt_QGroupBox.h"
#include "generated/org_jqt_QStackedLayout.h"
#include "generated/org_jqt_QSplitter.h"
#include "generated/org_jqt_QSpinBox.h"
#include "generated/org_jqt_QDial.h"
#include "generated/org_jqt_QRadioButton.h"
#include "generated/org_jqt_QDateTimeEdit.h"
#include "generated/org_jqt_QGridLayout.h"
#include "generated/org_jqt_QFormLayout.h"
#include "generated/org_jqt_QMenu.h"
#include "generated/org_jqt_QToolBar.h"
#include "generated/org_jqt_QStatusBar.h"
#include "generated/org_jqt_QSystemTrayIcon.h"
#include "generated/org_jqt_QTextEdit.h"
#include "generated/org_jqt_QPainter.h"
#include "generated/org_jqt_QCanvasWidget.h"
#include "generated/org_jqt_QClipboard.h"
#include "generated/org_jqt_QSettings.h"
#include "generated/org_jqt_QFile.h"
#include "generated/org_jqt_QDir.h"
#include "generated/org_jqt_GlobalHotkey.h"
#include "generated/org_jqt_JQtNavigation.h"
#include "generated/org_jqt_JQtPivot.h"
#include "generated/org_jqt_QProgressBar.h"
#include "generated/org_jqt_QScrollArea.h"
#include "generated/org_jqt_QSlider.h"
#include "generated/org_jqt_QWidget.h"
#include "generated/org_jqt_QMainWindow.h"
#include "generated/org_jqt_QColor.h"
#include "generated/org_jqt_QPrinter.h"
#include "generated/org_jqt_QOpenGLWidget.h"
#include "generated/org_jqt_QSerialPort.h"
#include "generated/org_jqt_QPixmap.h"
#include "generated/org_jqt_QImage.h"
#include "generated/org_jqt_QFont.h"
#include "generated/org_jqt_QSqlDatabase.h"
#include "generated/org_jqt_QSqlQuery.h"
#include "generated/org_jqt_QAction.h"
#include "generated/org_jqt_QDialog.h"
#include "generated/org_jqt_QMenuBar.h"
#include "generated/org_jqt_QListView.h"
#include "generated/org_jqt_QPushButton.h"
#include "generated/org_jqt_QLabel.h"
#include "generated/org_jqt_QLayout.h"
#include "generated/org_jqt_QVBoxLayout.h"
#include "generated/org_jqt_QHBoxLayout.h"
#include "generated/org_jqt_QLineEdit.h"
#include "generated/org_jqt_QComboBox.h"
#include "generated/org_jqt_QListWidget.h"
#include "generated/org_jqt_QFrame.h"
#include "generated/org_jqt_QCheckBox.h"
#include "generated/org_jqt_JQtTitleBar.h"
#include "generated/org_jqt_JQtSwitch.h"

// ----------------------------------------------------------------------------
// 全局状态
// ----------------------------------------------------------------------------

static JavaVM* g_jvm = nullptr;        // 缓存的 JVM 句柄（供 C++ → Java 回调）
static QApplication* g_app = nullptr;  // 进程级唯一的 QApplication
static jobject g_appJavaRef = nullptr; // JQtApplication Java 全局引用（系统主题回调用）

// 句柄注册表：Java 侧持有自增 ID（从 1 开始，永不复用），native 查表获得指针。
// destroyed 信号保证注册表与 Qt 对象生命周期严格同步。
static std::mutex g_handleMutex;
static std::unordered_map<int64_t, void*> g_handles;          // id -> QObject*
static std::unordered_map<int64_t, bool> g_javaOwned;         // id -> 是否归 Java（Cleaner）管理

// 递归显示布局树中的全部子控件。
// 仅在布局已安装到窗口（parentWidget 非空）且窗口已 show 后调用：
// 有父窗口的子控件 show() 不会创建顶层窗口，也不会在父窗口显示前闪现或撑大窗口。
static void jqtShowLayoutChildren(QLayout* layout) {
    for (int i = 0; i < layout->count(); i++) {
        QLayoutItem* item = layout->itemAt(i);
        if (item == nullptr) {
            continue;
        }
        if (item->widget() != nullptr) {
            QWidget* w = item->widget();
            if (w->property("jqtUserHidden").toBool()) {
                continue;   // 用户显式 hide() 过的控件：不强制显示
            }
            w->show();
            // 递归显示容器内部布局（面板等嵌套内容）——注意跳过 QStackedLayout（页由 setCurrentIndex 管理）
            if (w->layout() != nullptr && dynamic_cast<QStackedLayout*>(w->layout()) == nullptr) {
                jqtShowLayoutChildren(w->layout());
            }
        } else if (item->layout() != nullptr) {
            // QStackedLayout 的页可见性由 setCurrentIndex 管理：跳过，避免多页堆叠
            if (dynamic_cast<QStackedLayout*>(item->layout()) == nullptr) {
                jqtShowLayoutChildren(item->layout());
            }
        }
    }
}
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
                        // 标题栏空白区：不合成、不接管——让系统处理。
                        // WM_NCHITTEST 已返回 HTCAPTION，Windows 对触摸按下走
                        // 系统原生拖动（DWM 合成器直通，内容位图整体搬移，
                        // 亚克力/阴影作为内容不重算——这才是跟手路径）。
                        // 任何应用层 SetWindowPos 拖动都会触发 DWM 重合成 → 卡。
                        const UINT dpi = GetDpiForWindow(msg->hwnd);
                        const double dpr = dpi > 0 ? dpi / 96.0 : 1.0;
                        RECT rc;
                        GetClientRect(msg->hwnd, &rc);
                        if (pt.y < static_cast<int>(40 * dpr)
                            && pt.x < rc.right - static_cast<int>(150 * dpr)) {
                            fprintf(stderr, "[JQt] titlebar touch -> system drag\n");
                            return true;
                        }
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
        g_acrylicWindows.insert(reinterpret_cast<HWND>(winId()));
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
#ifdef _WIN32
        else if (msg->message == WM_HOTKEY) {
            jqtDispatchHotkey(static_cast<int>(msg->wParam));   // 全局热键（Exclusive Kit）
            return true;
        }
#endif

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

JNIEXPORT jlong JNICALL Java_org_jqt_QApplication_nativeCreateApp(JNIEnv* env, jobject thiz, jstring rhiBackend) {
#ifdef _WIN32
    SetUnhandledExceptionFilter(jqtCrashHandler);   // 崩溃日志（jqt-crash.log）
#endif
    env->GetJavaVM(&g_jvm);
    if (rhiBackend != nullptr) {
        const char* b = env->GetStringUTFChars(rhiBackend, nullptr);
        const QString backend = QString::fromUtf8(b);
        env->ReleaseStringUTFChars(rhiBackend, b);
        if (backend == "software") {
            qputenv("QT_WIDGETS_RHI", "0");          // 强制软件光栅
        } else {
            qputenv("QT_WIDGETS_RHI", "1");          // Widgets 走 RHI 后端
            qputenv("QSG_RHI_BACKEND", backend.toUtf8());  // d3d11 / opengl / vulkan
        }
        fprintf(stderr, "[JQt] RHI backend=%s widgetsRHI=%s\n",
                qgetenv("QSG_RHI_BACKEND").constData(), qgetenv("QT_WIDGETS_RHI").constData());
    }
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
    // g_appJavaRef 供系统主题自动跟随回调使用
    if (g_appJavaRef == nullptr) {
        g_appJavaRef = env->NewGlobalRef(thiz);
    }
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

JNIEXPORT void JNICALL Java_org_jqt_QApplication_exec(JNIEnv* /*env*/, jobject /*thiz*/) {
    if (g_app != nullptr) {
        g_app->exec();  // 阻塞直到最后一个窗口关闭（quitOnLastWindowClosed 默认开启）
    }
}

JNIEXPORT void JNICALL Java_org_jqt_QApplication_quit(JNIEnv* /*env*/, jobject /*thiz*/) {
    if (g_app != nullptr) {
        g_app->quit();
    }
}

JNIEXPORT void JNICALL Java_org_jqt_QApplication_scheduleQuit(JNIEnv* /*env*/, jobject /*thiz*/, jlong ms) {
    if (g_app != nullptr) {
        QTimer::singleShot(static_cast<int>(ms), g_app, &QApplication::quit);
    }
}

// 切换配色方案（setPalette 运行时生效，立即刷新全部控件）
// light=true → 浅色调色板；light=false → 深色调色板。
// 背景：Qt 在 Java 进程中暗色检测异常；此 API 让 Java 应用显式控制深浅色。
JNIEXPORT void JNICALL Java_org_jqt_QApplication_nativeSetColorScheme(JNIEnv* /*env*/, jobject /*thiz*/, jboolean light) {
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
JNIEXPORT void JNICALL Java_org_jqt_QApplication_nativeSetStyleSheet(JNIEnv* env, jobject /*thiz*/, jstring qss) {
    if (g_app == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(qss, nullptr);
    g_app->setStyleSheet(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(qss, utf);
}

// 设置全局字体（QApplication::setFont；所有控件继承，Qt 自动回退缺失字形）
JNIEXPORT void JNICALL Java_org_jqt_QApplication_nativeSetFont(JNIEnv* env, jobject /*thiz*/, jstring family, jint size) {
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
JNIEXPORT void JNICALL Java_org_jqt_QApplication_nativeSetAccent(JNIEnv* env, jobject /*thiz*/, jstring hex) {
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
// ----------------------------------------------------------------------------
// 自动跟随系统主题（深浅色 + 强调色）：注册表轮询，变化时回调 Java
// 工业场景零配置：setAutoTheme(true) 后开发者无需关心主题切换。
// ----------------------------------------------------------------------------
#ifdef _WIN32
static DWORD jqtReadRegDword(HKEY root, const wchar_t* subKey, const wchar_t* name, DWORD def) {
    HKEY key = nullptr;
    if (RegOpenKeyExW(root, subKey, 0, KEY_READ, &key) != ERROR_SUCCESS) {
        return def;
    }
    DWORD value = def;
    DWORD size = sizeof(value);
    DWORD type = 0;
    if (RegQueryValueExW(key, name, nullptr, &type, reinterpret_cast<LPBYTE>(&value), &size) != ERROR_SUCCESS) {
        value = def;
    }
    RegCloseKey(key);
    return value;
}

static bool g_autoThemeLastLight = false;
static QString g_autoThemeLastAccent;

// 系统主题变化 → Java nativeHandleSystemTheme(light, accentHex)
static void jqtNotifySystemTheme(bool light, const QString& accentHex) {
    JNIEnv* e = callbackEnv();
    if (e == nullptr || g_appJavaRef == nullptr) {
        return;
    }
    jclass cls = e->GetObjectClass(g_appJavaRef);
    jmethodID mid = e->GetMethodID(cls, "nativeHandleSystemTheme", "(ZLjava/lang/String;)V");
    if (mid == nullptr) {
        return;
    }
    jstring jhex = e->NewStringUTF(accentHex.toUtf8().constData());
    e->CallVoidMethod(g_appJavaRef, mid, light ? JNI_TRUE : JNI_FALSE, jhex);
    if (e->ExceptionCheck()) {
        e->ExceptionDescribe();
        e->ExceptionClear();
    }
    e->DeleteLocalRef(jhex);
}

// 检测一次系统主题；变化时回调 Java（首次调用也立即同步）
static void jqtPollSystemTheme(bool force) {
    const bool light = jqtReadRegDword(HKEY_CURRENT_USER,
        L"Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
        L"AppsUseLightTheme", 0) == 1;
    const DWORD accent = jqtReadRegDword(HKEY_CURRENT_USER, L"Software\\Microsoft\\Windows\\DWM",
                                         L"AccentColor", 0x4cc2ff) & 0xFFFFFF;   // 0x00bbggrr
    char hex[16];
    snprintf(hex, sizeof(hex), "#%02x%02x%02x",
             static_cast<unsigned>(accent & 0xFF),
             static_cast<unsigned>((accent >> 8) & 0xFF),
             static_cast<unsigned>((accent >> 16) & 0xFF));
    const QString accentHex = QString::fromLatin1(hex);
    if (force || light != g_autoThemeLastLight || accentHex != g_autoThemeLastAccent) {
        g_autoThemeLastLight = light;
        g_autoThemeLastAccent = accentHex;
        jqtNotifySystemTheme(light, accentHex);
    }
}
#endif

JNIEXPORT void JNICALL Java_org_jqt_QApplication_nativeSetAutoTheme(JNIEnv* env, jobject /*thiz*/, jboolean on) {
    if (g_app == nullptr) {
        return;
    }
#ifdef _WIN32
    static QTimer* g_themeTimer = nullptr;
    if (on == JNI_TRUE) {
        if (g_themeTimer == nullptr) {
            g_themeTimer = new QTimer(g_app);
            QObject::connect(g_themeTimer, &QTimer::timeout, []() { jqtPollSystemTheme(false); });
            g_themeTimer->start(2000);
        }
        jqtPollSystemTheme(true);   // 开启时立即同步一次
    } else if (g_themeTimer != nullptr) {
        g_themeTimer->stop();
    }
#else
    Q_UNUSED(on);
    fprintf(stderr, "[JQt] auto theme is Windows-only\n");
#endif
}


JNIEXPORT void JNICALL Java_org_jqt_QApplication_nativeSetStyle(JNIEnv* env, jobject /*thiz*/, jstring style) {
    if (g_app == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(style, nullptr);
    g_app->setStyle(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(style, utf);
}

// 在 ms 毫秒后于 GUI 线程执行 Java Runnable（Qt 定时器 → JNI → Runnable.run()）
JNIEXPORT void JNICALL Java_org_jqt_QApplication_nativeSchedule(JNIEnv* env, jobject /*thiz*/, jobject task, jlong ms) {
    jobject gTask = env->NewGlobalRef(task);
    QTimer::singleShot(static_cast<int>(ms), g_app, [gTask]() {
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

// ----------------------------------------------------------------------------
// QWidget 值对象批（手写精修：几何/抓取/光标/字体/调色板/坐标映射）
// ----------------------------------------------------------------------------
JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetGeometry(JNIEnv* env, jobject /*thiz*/, jlong handle, jint x, jint y, jint w, jint h) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt != nullptr) wgt->setGeometry(x, y, w, h);
}

JNIEXPORT jlong JNICALL Java_org_jqt_QWidget_nativeGrab(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) return 0;
    return reinterpret_cast<jlong>(new QPixmap(wgt->grab()));
}

JNIEXPORT jlong JNICALL Java_org_jqt_QWidget_nativeGrabRect(JNIEnv* env, jobject /*thiz*/, jlong handle, jint x, jint y, jint w, jint h) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) return 0;
    return reinterpret_cast<jlong>(new QPixmap(wgt->grab(QRect(x, y, w, h))));
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeGrabMouse(JNIEnv* env, jobject /*thiz*/, jlong handle, jint cursorShape) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) return;
    if (cursorShape > 0) wgt->grabMouse(static_cast<Qt::CursorShape>(cursorShape));
    else wgt->grabMouse();
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeReleaseMouse(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt != nullptr) wgt->releaseMouse();
}

JNIEXPORT jint JNICALL Java_org_jqt_QWidget_nativeGrabShortcut(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring sequence, jint context) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) return 0;
    const char* utf = env->GetStringUTFChars(sequence, nullptr);
    int id = wgt->grabShortcut(QKeySequence(QString::fromUtf8(utf)), static_cast<Qt::ShortcutContext>(context));
    env->ReleaseStringUTFChars(sequence, utf);
    return id;
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeReleaseShortcut(JNIEnv* env, jobject /*thiz*/, jlong handle, jint id) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt != nullptr) wgt->releaseShortcut(id);
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetMaskRect(JNIEnv* env, jobject /*thiz*/, jlong handle, jint x, jint y, jint w, jint h) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt != nullptr) wgt->setMask(QRect(x, y, w, h));
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeClearMask(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt != nullptr) wgt->clearMask();
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetCursorShape(JNIEnv* env, jobject /*thiz*/, jlong handle, jint shape) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt != nullptr) wgt->setCursor(static_cast<Qt::CursorShape>(shape));
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetFontQFont(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring family, jint pointSize, jint weight, jboolean italic) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) return;
    const char* utf = env->GetStringUTFChars(family, nullptr);
    wgt->setFont(QFont(QString::fromUtf8(utf), pointSize, static_cast<QFont::Weight>(weight), italic));
    env->ReleaseStringUTFChars(family, utf);
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetPalette(JNIEnv* env, jobject /*thiz*/, jlong handle,
    jint window, jint windowText, jint base, jint text, jint button, jint buttonText, jint highlight, jint highlightedText) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) return;
    QPalette pal;
    pal.setColor(QPalette::Window, QColor::fromRgba((QRgb)window));
    pal.setColor(QPalette::WindowText, QColor::fromRgba((QRgb)windowText));
    pal.setColor(QPalette::Base, QColor::fromRgba((QRgb)base));
    pal.setColor(QPalette::Text, QColor::fromRgba((QRgb)text));
    pal.setColor(QPalette::Button, QColor::fromRgba((QRgb)button));
    pal.setColor(QPalette::ButtonText, QColor::fromRgba((QRgb)buttonText));
    pal.setColor(QPalette::Highlight, QColor::fromRgba((QRgb)highlight));
    pal.setColor(QPalette::HighlightedText, QColor::fromRgba((QRgb)highlightedText));
    wgt->setPalette(pal);
}

JNIEXPORT jbyteArray JNICALL Java_org_jqt_QWidget_nativeSaveGeometry(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) return env->NewByteArray(0);
    QByteArray ba = wgt->saveGeometry();
    jbyteArray out = env->NewByteArray(ba.size());
    env->SetByteArrayRegion(out, 0, ba.size(), reinterpret_cast<const jbyte*>(ba.constData()));
    return out;
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QWidget_nativeRestoreGeometry(JNIEnv* env, jobject /*thiz*/, jlong handle, jbyteArray data) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) return false;
    jsize n = env->GetArrayLength(data);
    jbyte* buf = env->GetByteArrayElements(data, nullptr);
    bool ok = wgt->restoreGeometry(QByteArray(reinterpret_cast<const char*>(buf), n));
    env->ReleaseByteArrayElements(data, buf, JNI_ABORT);
    return ok;
}

static jintArray jqtPackPoint(JNIEnv* env, const QPoint& p) {
    jintArray out = env->NewIntArray(2);
    jint vals[2] = { p.x(), p.y() };
    env->SetIntArrayRegion(out, 0, 2, vals);
    return out;
}

JNIEXPORT jintArray JNICALL Java_org_jqt_QWidget_nativeMapToGlobal(JNIEnv* env, jobject /*thiz*/, jlong handle, jint x, jint y) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) return jqtPackPoint(env, QPoint());
    return jqtPackPoint(env, wgt->mapToGlobal(QPoint(x, y)));
}

JNIEXPORT jintArray JNICALL Java_org_jqt_QWidget_nativeMapFromGlobal(JNIEnv* env, jobject /*thiz*/, jlong handle, jint x, jint y) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) return jqtPackPoint(env, QPoint());
    return jqtPackPoint(env, wgt->mapFromGlobal(QPoint(x, y)));
}

JNIEXPORT jintArray JNICALL Java_org_jqt_QWidget_nativeMapTo(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong otherHandle, jint x, jint y) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    QWidget* other = otherHandle != 0 ? static_cast<QWidget*>(requireHandle(env, otherHandle)) : nullptr;
    if (wgt == nullptr || other == nullptr) return jqtPackPoint(env, QPoint());
    return jqtPackPoint(env, wgt->mapTo(other, QPoint(x, y)));
}

JNIEXPORT jintArray JNICALL Java_org_jqt_QWidget_nativeMapFrom(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong otherHandle, jint x, jint y) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    QWidget* other = otherHandle != 0 ? static_cast<QWidget*>(requireHandle(env, otherHandle)) : nullptr;
    if (wgt == nullptr || other == nullptr) return jqtPackPoint(env, QPoint());
    return jqtPackPoint(env, wgt->mapFrom(other, QPoint(x, y)));
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetContentsMargins(JNIEnv* env, jobject /*thiz*/, jlong handle, jint left, jint top, jint right, jint bottom) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt != nullptr) wgt->setContentsMargins(left, top, right, bottom);
}

JNIEXPORT jintArray JNICALL Java_org_jqt_QWidget_nativeContentsMargins(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    jintArray out = env->NewIntArray(4);
    jint vals[4] = { 0, 0, 0, 0 };
    if (wgt != nullptr) {
        QMargins m = wgt->contentsMargins();
        vals[0] = m.left(); vals[1] = m.top(); vals[2] = m.right(); vals[3] = m.bottom();
    }
    env->SetIntArrayRegion(out, 0, 4, vals);
    return out;
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeDispose(JNIEnv* /*env*/, jclass /*cls*/, jlong handle) {
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

// ----------------------------------------------------------------------------
// JQtWidget 基础 API（几何查询 / 显隐 / 禁用 / 固定尺寸）
// ----------------------------------------------------------------------------
JNIEXPORT jint JNICALL Java_org_jqt_QWidget_nativeWidth(JNIEnv* env, jclass /*cls*/, jlong handle) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    return widget == nullptr ? 0 : static_cast<jint>(widget->width());
}

JNIEXPORT jint JNICALL Java_org_jqt_QWidget_nativeHeight(JNIEnv* env, jclass /*cls*/, jlong handle) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    return widget == nullptr ? 0 : static_cast<jint>(widget->height());
}

JNIEXPORT jint JNICALL Java_org_jqt_QWidget_nativeX(JNIEnv* env, jclass /*cls*/, jlong handle) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    return widget == nullptr ? 0 : static_cast<jint>(widget->x());
}

JNIEXPORT jint JNICALL Java_org_jqt_QWidget_nativeY(JNIEnv* env, jclass /*cls*/, jlong handle) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    return widget == nullptr ? 0 : static_cast<jint>(widget->y());
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeShow(JNIEnv* env, jclass /*cls*/, jlong handle) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    widget->show();
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeHide(JNIEnv* env, jclass /*cls*/, jlong handle) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget != nullptr) {
        widget->setProperty("jqtUserHidden", true);   // 用户显式隐藏标记：窗口 show 时不强制再显示
        widget->hide();
    }
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QWidget_nativeIsVisible(JNIEnv* env, jclass /*cls*/, jlong handle) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    return widget == nullptr ? JNI_FALSE : (widget->isVisible() ? JNI_TRUE : JNI_FALSE);
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetEnabled(JNIEnv* env, jclass /*cls*/, jlong handle, jboolean enabled) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget != nullptr) { widget->setEnabled(enabled == JNI_TRUE); }
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QWidget_nativeIsEnabled(JNIEnv* env, jclass /*cls*/, jlong handle) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    return widget == nullptr ? JNI_FALSE : (widget->isEnabled() ? JNI_TRUE : JNI_FALSE);
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetFixedSize(JNIEnv* env, jclass /*cls*/, jlong handle, jint w, jint h) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget != nullptr) { widget->setFixedSize(w, h); }
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetStyleSheet(JNIEnv* env, jclass /*cls*/, jlong handle, jstring qss) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(qss, nullptr);
    widget->setProperty("jqtBaseQss", QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(qss, utf);
    jqtApplyWidgetQss(widget);
}

// ----------------------------------------------------------------------------
// JQtScrollArea：滚动区（QScrollArea 封装）
// ----------------------------------------------------------------------------
JNIEXPORT jlong JNICALL Java_org_jqt_QScrollArea_nativeCreate(JNIEnv* env, jobject /*thiz*/) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    QScrollArea* area = new QScrollArea();
    area->setWidgetResizable(true);
    area->setFrameShape(QFrame::NoFrame);
    return registerHandle(area, /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_QScrollArea_nativeSetWidget(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong childHandle) {
    QScrollArea* area = static_cast<QScrollArea*>(requireHandle(env, handle));
    QWidget* child = static_cast<QWidget*>(requireHandle(env, childHandle));
    if (area == nullptr || child == nullptr) {
        return;
    }
    area->setWidget(child);
    markQtOwned(childHandle);   // 内容控件生命周期移交滚动区
}

JNIEXPORT void JNICALL Java_org_jqt_QScrollArea_nativeSetWidgetResizable(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean resizable) {
    QScrollArea* area = static_cast<QScrollArea*>(requireHandle(env, handle));
    if (area != nullptr) {
        area->setWidgetResizable(resizable == JNI_TRUE);
    }
}

// ----------------------------------------------------------------------------
// JQtProgressBar：进度条（QProgressBar 封装，QSS 可样式化 chunk）
// ----------------------------------------------------------------------------
JNIEXPORT jlong JNICALL Java_org_jqt_QProgressBar_nativeCreate(JNIEnv* env, jobject /*thiz*/) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    QProgressBar* bar = new QProgressBar();
    bar->setTextVisible(false);
    return registerHandle(bar, /*javaOwned=*/true);
}

JNIEXPORT jint JNICALL Java_org_jqt_QProgressBar_nativeValue(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QProgressBar* bar = static_cast<QProgressBar*>(requireHandle(env, handle));
    return bar == nullptr ? 0 : static_cast<jint>(bar->value());
}

JNIEXPORT void JNICALL Java_org_jqt_QProgressBar_nativeSetValue(JNIEnv* env, jobject /*thiz*/, jlong handle, jint value) {
    QProgressBar* bar = static_cast<QProgressBar*>(requireHandle(env, handle));
    if (bar != nullptr) {
        bar->setValue(static_cast<int>(value));
    }
}

JNIEXPORT void JNICALL Java_org_jqt_QProgressBar_nativeSetRange(JNIEnv* env, jobject /*thiz*/, jlong handle, jint min, jint max) {
    QProgressBar* bar = static_cast<QProgressBar*>(requireHandle(env, handle));
    if (bar != nullptr) {
        bar->setRange(static_cast<int>(min), static_cast<int>(max));
    }
}

// 自定义控件圆角（像素；0 = 不添加规则，用全局 QSS）
JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetBorderRadius(JNIEnv* env, jclass /*cls*/, jlong handle, jint radius) {
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

JNIEXPORT jlong JNICALL Java_org_jqt_QMainWindow_nativeCreate(JNIEnv* env, jobject thiz, jstring title, jint width, jint height) {
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

JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeShow(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    widget->show();
}

JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeHide(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    widget->hide();
}

// 关闭窗口（触发 onClose 回调；若为最后一个窗口，exec() 返回）
JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeClose(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    widget->close();
}

// ---- Fluent 窗口能力（偷师 qframelesswindow / qfluentwidgets）----

// 无边框模式：FramelessWindowHint + DWM 阴影 + 缩放热区（WM_NCHITTEST）
// 修复：setWindowFlag 只改 Qt 内部标志，已显示窗口的 HWND 样式不会更新（边框热切换失效）。
//       这里直接操作 Win32 样式位 + 清除 DWM 扩展边框 + SWP_FRAMECHANGED 强制立即生效。
JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeSetFrameless(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean on) {
    JQtWindowShell* win = static_cast<JQtWindowShell*>(requireHandle(env, handle));
    if (win == nullptr) {
        return;
    }
    win->frameless = (on == JNI_TRUE);
    win->setWindowFlag(Qt::FramelessWindowHint, win->frameless);
#ifdef _WIN32
    HWND hwnd = reinterpret_cast<HWND>(win->winId());
    if (win->frameless) {
        // 去掉系统标题栏/边框样式（含最小化/最大化/系统菜单位）
        LONG_PTR style = GetWindowLongPtrW(hwnd, GWL_STYLE);
        style &= ~(WS_CAPTION | WS_THICKFRAME | WS_SYSMENU | WS_MINIMIZEBOX | WS_MAXIMIZEBOX);
        SetWindowLongPtrW(hwnd, GWL_STYLE, style);
        win->applyShadow();
    } else {
        // 恢复系统标题栏/边框样式
        LONG_PTR style = GetWindowLongPtrW(hwnd, GWL_STYLE);
        style |= WS_CAPTION | WS_THICKFRAME | WS_SYSMENU | WS_MINIMIZEBOX | WS_MAXIMIZEBOX;
        SetWindowLongPtrW(hwnd, GWL_STYLE, style);
        // 清除 DWM 扩展边框（applyShadow 设过 margins=1，不恢复会吞掉原生边框）
        HMODULE dwm = GetModuleHandleW(L"dwmapi.dll");
        if (!dwm) dwm = LoadLibraryW(L"dwmapi.dll");
        if (dwm) {
            typedef HRESULT(WINAPI* DwmExtendFrameFunc)(HWND, const MARGINS*);
            auto fn = (DwmExtendFrameFunc)GetProcAddress(dwm, "DwmExtendFrameIntoClientArea");
            if (fn) {
                MARGINS margins{ 0, 0, 0, 0 };
                fn(hwnd, &margins);
            }
        }
    }
    // 强制非客户区立即重算（否则新样式要到下次 resize/show 才生效）
    SetWindowPos(hwnd, nullptr, 0, 0, 0, 0,
                 SWP_FRAMECHANGED | SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE);
#endif
    win->show();
}

// 亚克力背景（Win10+，SetWindowCompositionAttribute）
JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeSetAcrylic(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean on) {
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
JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeSetRoundedCorners(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean on) {
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
JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeSetDraggable(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean on) {
    JQtWindowShell* win = static_cast<JQtWindowShell*>(requireHandle(env, handle));
    if (win == nullptr) {
        return;
    }
    win->draggable = (on == JNI_TRUE);
}

// 缩放热区宽度（像素）
JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeSetBorderWidth(JNIEnv* env, jobject /*thiz*/, jlong handle, jint px) {
    JQtWindowShell* win = static_cast<JQtWindowShell*>(requireHandle(env, handle));
    if (win == nullptr) {
        return;
    }
    win->borderWidth = static_cast<int>(px);
}

JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeMinimize(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    widget->showMinimized();
}

JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeMaximize(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    JQtWindowShell* win = static_cast<JQtWindowShell*>(requireHandle(env, handle));
    if (win == nullptr) {
        return;
    }
    win->setManualMaximized(true);
}

JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeToggleMaximize(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    JQtWindowShell* win = static_cast<JQtWindowShell*>(requireHandle(env, handle));
    if (win == nullptr) {
        return;
    }
    win->setManualMaximized(!win->isManualMaximized());
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QMainWindow_nativeIsMaximized(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    JQtWindowShell* win = static_cast<JQtWindowShell*>(requireHandle(env, handle));
    if (win == nullptr) {
        return JNI_FALSE;
    }
    return win->isManualMaximized() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeResize(JNIEnv* env, jobject /*thiz*/, jlong handle, jint width, jint height) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    widget->resize(static_cast<int>(width), static_cast<int>(height));
}

JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeSetTitle(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring title) {
    QWidget* widget = static_cast<QWidget*>(requireHandle(env, handle));
    if (widget == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(title, nullptr);
    widget->setWindowTitle(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(title, utf);
}

// 把子控件加入窗口（未设置布局时）：建立 Qt 父子关系并按顺序自动摆放。
JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeAddWidget(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong childHandle) {
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
JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeSetLayout(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong layoutHandle) {
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
    jqtShowLayoutChildren(layout);
}

// JQtWidget：通用 setLayout（任何控件可装布局）
JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetLayout(JNIEnv* env, jclass /*cls*/, jlong handle, jlong layoutHandle) {
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
    // 注意：容器（面板）setLayout 时不显示子控件——会触发面板 sizeHint 撑大父窗口；
    // 面板内容由父窗口 setLayout 的 jqtShowLayoutChildren 递归显示。
}

// JQtWidget：设置 objectName（QSS #name 选择器）
JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetObjectName(JNIEnv* env, jclass /*cls*/, jlong handle, jstring name) {
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

JNIEXPORT jlong JNICALL Java_org_jqt_QPushButton_nativeCreate(JNIEnv* env, jobject thiz, jstring text) {
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

JNIEXPORT void JNICALL Java_org_jqt_QPushButton_nativeSetText(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring text) {
    QPushButton* btn = static_cast<QPushButton*>(requireHandle(env, handle));
    if (btn == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    btn->setText(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
}

JNIEXPORT void JNICALL Java_org_jqt_QPushButton_nativeSetCheckable(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean checkable) {
    QPushButton* btn = static_cast<QPushButton*>(requireHandle(env, handle));
    if (btn == nullptr) {
        return;
    }
    btn->setCheckable(checkable == JNI_TRUE);
}

JNIEXPORT void JNICALL Java_org_jqt_QPushButton_nativeSetChecked(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean checked) {
    QPushButton* btn = static_cast<QPushButton*>(requireHandle(env, handle));
    if (btn == nullptr) {
        return;
    }
    btn->setChecked(checked == JNI_TRUE);
}

// ----------------------------------------------------------------------------
// JQtLabel：QLabel 的封装
// ----------------------------------------------------------------------------

JNIEXPORT jlong JNICALL Java_org_jqt_QLabel_nativeCreate(JNIEnv* env, jobject /*thiz*/, jstring text) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    QLabel* label = new QLabel(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
    return registerHandle(label, /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_QLabel_nativeSetText(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring text) {
    QLabel* label = static_cast<QLabel*>(requireHandle(env, handle));
    if (label == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    label->setText(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
}

JNIEXPORT jstring JNICALL Java_org_jqt_QLabel_nativeText(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QLabel* label = static_cast<QLabel*>(requireHandle(env, handle));
    if (label == nullptr) {
        return env->NewStringUTF("");
    }
    const QByteArray utf8 = label->text().toUtf8();
    return env->NewStringUTF(utf8.constData());
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

JNIEXPORT jlong JNICALL Java_org_jqt_QLineEdit_nativeCreate(JNIEnv* env, jobject thiz, jstring text) {
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

JNIEXPORT jstring JNICALL Java_org_jqt_QLineEdit_nativeText(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QLineEdit* edit = static_cast<QLineEdit*>(requireHandle(env, handle));
    if (edit == nullptr) {
        return nullptr;
    }
    const QString t = edit->text();
    return env->NewStringUTF(t.toUtf8().constData());
}

JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativeSetText(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring text) {
    QLineEdit* edit = static_cast<QLineEdit*>(requireHandle(env, handle));
    if (edit == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    edit->setText(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
}

JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativeSetPlaceholderText(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring text) {
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

JNIEXPORT jlong JNICALL Java_org_jqt_QComboBox_nativeCreate(JNIEnv* env, jobject thiz) {
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

JNIEXPORT void JNICALL Java_org_jqt_QComboBox_nativeAddItem(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring text) {
    QComboBox* combo = static_cast<QComboBox*>(requireHandle(env, handle));
    if (combo == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    combo->addItem(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
}

JNIEXPORT jint JNICALL Java_org_jqt_QComboBox_nativeCurrentIndex(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QComboBox* combo = static_cast<QComboBox*>(requireHandle(env, handle));
    if (combo == nullptr) {
        return -1;
    }
    return static_cast<jint>(combo->currentIndex());
}

JNIEXPORT jstring JNICALL Java_org_jqt_QComboBox_nativeCurrentText(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QComboBox* combo = static_cast<QComboBox*>(requireHandle(env, handle));
    if (combo == nullptr) {
        return nullptr;
    }
    const QString t = combo->currentText();
    return env->NewStringUTF(t.toUtf8().constData());
}

JNIEXPORT void JNICALL Java_org_jqt_QComboBox_nativeSetCurrentIndex(JNIEnv* env, jobject /*thiz*/, jlong handle, jint index) {
    QComboBox* combo = static_cast<QComboBox*>(requireHandle(env, handle));
    if (combo == nullptr) {
        return;
    }
    combo->setCurrentIndex(static_cast<int>(index));
}

// ----------------------------------------------------------------------------
// JQtListWidget：QListWidget 的封装（itemClicked / currentRowChanged 信号）
// ----------------------------------------------------------------------------

JNIEXPORT jlong JNICALL Java_org_jqt_QListWidget_nativeCreate(JNIEnv* env, jobject thiz) {
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

JNIEXPORT void JNICALL Java_org_jqt_QListWidget_nativeAddItem(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring text) {
    QListWidget* list = static_cast<QListWidget*>(requireHandle(env, handle));
    if (list == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    list->addItem(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
}

JNIEXPORT jint JNICALL Java_org_jqt_QListWidget_nativeCurrentRow(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QListWidget* list = static_cast<QListWidget*>(requireHandle(env, handle));
    if (list == nullptr) {
        return -1;
    }
    return static_cast<jint>(list->currentRow());
}

// ----------------------------------------------------------------------------
// JQtLayout / JQtVBoxLayout / JQtHBoxLayout：布局管理器
// ----------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_org_jqt_QLayout_nativeAddWidget(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong childHandle) {
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
    // 布局未安装（parentWidget 为空）时不 show：此时控件无父窗口，show 会闪现顶层窗口；
    // 等 setLayout 安装后由 jqtShowLayoutChildren 统一显示。
    if (layout->parentWidget() != nullptr) {
        child->show();
    }
}

JNIEXPORT void JNICALL Java_org_jqt_QLayout_nativeSetSpacing(JNIEnv* env, jobject /*thiz*/, jlong handle, jint spacing) {
    QBoxLayout* layout = static_cast<QBoxLayout*>(requireHandle(env, handle));
    if (layout == nullptr) {
        return;
    }
    layout->setSpacing(static_cast<int>(spacing));
}

// 布局四周留白（外边距）
JNIEXPORT void JNICALL Java_org_jqt_QLayout_nativeSetContentsMargins(JNIEnv* env, jobject /*thiz*/, jlong handle, jint left, jint top, jint right, jint bottom) {
    QBoxLayout* layout = static_cast<QBoxLayout*>(requireHandle(env, handle));
    if (layout == nullptr) {
        return;
    }
    layout->setContentsMargins(static_cast<int>(left), static_cast<int>(top),
                               static_cast<int>(right), static_cast<int>(bottom));
}

// 布局嵌套：把子布局加入本布局（如 VBox 中嵌 HBox 做标题栏/工具行）
JNIEXPORT void JNICALL Java_org_jqt_QLayout_nativeAddLayout(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong childLayoutHandle) {
    QBoxLayout* layout = static_cast<QBoxLayout*>(requireHandle(env, handle));
    if (layout == nullptr) {
        return;
    }
    QBoxLayout* child = static_cast<QBoxLayout*>(requireHandle(env, childLayoutHandle));
    if (child == nullptr) {
        return;
    }
    layout->addLayout(child);
    if (layout->parentWidget() != nullptr) {
        jqtShowLayoutChildren(child);   // 动态挂到已安装布局时，子布局控件需显示
    }
    markQtOwned(childLayoutHandle);  // 子布局归父布局管理
}

JNIEXPORT void JNICALL Java_org_jqt_QLayout_nativeAddStretch(JNIEnv* env, jobject /*thiz*/, jlong handle, jint stretch) {
    QBoxLayout* layout = static_cast<QBoxLayout*>(requireHandle(env, handle));
    if (layout == nullptr) {
        return;
    }
    layout->addStretch(static_cast<int>(stretch));
}

JNIEXPORT jlong JNICALL Java_org_jqt_QVBoxLayout_nativeCreate(JNIEnv* env, jobject /*thiz*/) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    // 未 setLayout 前归 Java 管理（Cleaner 回收）；setLayout 后 markQtOwned
    return registerHandle(new QVBoxLayout(), /*javaOwned=*/true);
}

JNIEXPORT jlong JNICALL Java_org_jqt_QHBoxLayout_nativeCreate(JNIEnv* env, jobject /*thiz*/) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    return registerHandle(new QHBoxLayout(), /*javaOwned=*/true);
}

// ----------------------------------------------------------------------------
// JQtPanel：QFrame 卡片/容器（Fluent 卡片基座）
// ----------------------------------------------------------------------------

JNIEXPORT jlong JNICALL Java_org_jqt_QFrame_nativeCreate(JNIEnv* env, jobject /*thiz*/) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    return registerHandle(new QFrame(), /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_QFrame_nativeAddWidget(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong childHandle) {
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

JNIEXPORT jlong JNICALL Java_org_jqt_QCheckBox_nativeCreate(JNIEnv* env, jobject thiz, jstring text) {
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

JNIEXPORT void JNICALL Java_org_jqt_QCheckBox_nativeSetText(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring text) {
    QCheckBox* box = static_cast<QCheckBox*>(requireHandle(env, handle));
    if (box == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    box->setText(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QCheckBox_nativeIsChecked(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QCheckBox* box = static_cast<QCheckBox*>(requireHandle(env, handle));
    if (box == nullptr) {
        return JNI_FALSE;
    }
    return box->isChecked() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_org_jqt_QCheckBox_nativeSetChecked(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean checked) {
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

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeAnimateMoveEasing(JNIEnv* env, jclass /*cls*/, jlong handle, jint x, jint y, jlong ms, jint easing) {
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

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeAnimateResizeEasing(JNIEnv* env, jclass /*cls*/, jlong handle, jint w, jint h, jlong ms, jint easing) {
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

JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeFadeInEasing(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong ms, jint easing) {
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

JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeFadeOutEasing(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong ms, jint easing) {
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
JNIEXPORT jlong JNICALL Java_org_jqt_QWidget_nativeCreateAnimation(JNIEnv* env, jclass /*cls*/, jlong handle, jstring property, jdouble from, jdouble to, jlong ms, jint easing) {
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

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeAnimationSetLoopCount(JNIEnv* env, jclass /*cls*/, jlong animHandle, jint loops) {
    QPropertyAnimation* anim = static_cast<QPropertyAnimation*>(requireHandle(env, animHandle));
    if (anim == nullptr) {
        return;
    }
    anim->setLoopCount(static_cast<int>(loops));
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeAnimationStart(JNIEnv* env, jclass /*cls*/, jlong animHandle) {
    QPropertyAnimation* anim = static_cast<QPropertyAnimation*>(requireHandle(env, animHandle));
    if (anim == nullptr) {
        return;
    }
    anim->start(QAbstractAnimation::DeleteWhenStopped);
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeAnimationStop(JNIEnv* env, jclass /*cls*/, jlong animHandle) {
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

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeRegisterAnimation(JNIEnv* env, jclass /*cls*/, jlong animHandle, jobject animObj) {
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
// JQtNavigation：Fluent 侧栏导航（自绘导航项 + 选中高亮背景动画）
// 每项：图标（emoji/字符）+ 文字；选中态圆角高亮（accent 淡色背景），200ms 滑动。
// ----------------------------------------------------------------------------
class JQtNavigationWidget : public QWidget {
public:
    struct Item {
        QString icon;
        QString text;
    };

    explicit JQtNavigationWidget() {
        setFixedWidth(180);
        setCursor(Qt::PointingHandCursor);
        m_anim = new QVariantAnimation(this);
        m_anim->setDuration(200);
        m_anim->setEasingCurve(QEasingCurve::OutCubic);
        QObject::connect(m_anim, &QVariantAnimation::valueChanged, this,
                         [this](const QVariant& v) {
                             m_selY = v.toDouble();
                             update();
                         });
    }

    std::function<void(int)> onChanged;

    void addItem(const QString& icon, const QString& text) {
        m_items.push_back({ icon, text });
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
        animateTo(itemCenterY(index));
    }

protected:
    void paintEvent(QPaintEvent*) override {
        QPainter p(this);
        p.setRenderHint(QPainter::Antialiasing);
        const int n = static_cast<int>(m_items.size());
        if (n == 0) {
            return;
        }
        const bool light = palette().color(QPalette::Window).lightness() > 128;
        const double itemH = 40.0;
        const double padX = 10.0;
        const QColor accent = g_accentColor;
        const QColor textColor = light ? QColor(0x33, 0x33, 0x33) : QColor(0xd8, 0xd8, 0xd8);

        // 选中高亮（accent 淡色）
        const double hw = width() - padX * 2.0;
        if (m_selY >= 0) {
            QColor hl = accent;
            hl.setAlpha(light ? 40 : 55);
            p.setPen(Qt::NoPen);
            p.setBrush(hl);
            p.drawRoundedRect(QRectF(padX, m_selY - itemH / 2.0, hw, itemH), 8.0, 8.0);
        }

        p.setFont(font());
        for (int i = 0; i < n; ++i) {
            const double cy = itemH * i + itemH / 2.0;
            const bool selected = (i == m_current);
            // 图标 + 文字
            const QRectF iconRect(padX + 6.0, cy - itemH / 2.0, 26.0, itemH);
            const QRectF textRect(padX + 34.0, cy - itemH / 2.0, hw - 30.0, itemH);
            p.setPen(selected ? accent : textColor);
            p.drawText(iconRect, Qt::AlignVCenter | Qt::AlignLeft, m_items[i].icon);
            p.setPen(selected ? accent : textColor);
            p.drawText(textRect, Qt::AlignVCenter | Qt::AlignLeft, m_items[i].text);
        }
    }

    void mousePressEvent(QMouseEvent* event) override {
        const int n = static_cast<int>(m_items.size());
        if (n == 0) {
            return;
        }
        const int idx = static_cast<int>(event->position().y() / 40.0);
        if (idx >= 0 && idx < n) {
            setCurrentIndex(idx);
        }
    }

private:
    double itemCenterY(int index) const {
        return 40.0 * index + 20.0;
    }

    void animateTo(double y) {
        m_anim->stop();
        m_anim->setStartValue(m_selY);
        m_anim->setEndValue(y);
        m_anim->start();
    }

    std::vector<Item> m_items;
    int m_current = 0;
    double m_selY = -1.0;
    QVariantAnimation* m_anim;
};

JNIEXPORT jlong JNICALL Java_org_jqt_JQtNavigation_nativeCreate(JNIEnv* env, jobject thiz) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    JQtNavigationWidget* nav = new JQtNavigationWidget();
    jobject gRef = env->NewGlobalRef(thiz);
    nav->onChanged = [gRef](int index) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleChanged", "(I)V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(index));
        }
    };
    return registerHandle(nav, /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_JQtNavigation_nativeAddItem(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring icon, jstring text) {
    JQtNavigationWidget* nav = static_cast<JQtNavigationWidget*>(requireHandle(env, handle));
    if (nav == nullptr) {
        return;
    }
    const char* u1 = env->GetStringUTFChars(icon, nullptr);
    const char* u2 = env->GetStringUTFChars(text, nullptr);
    nav->addItem(QString::fromUtf8(u1), QString::fromUtf8(u2));
    env->ReleaseStringUTFChars(icon, u1);
    env->ReleaseStringUTFChars(text, u2);
}

JNIEXPORT jint JNICALL Java_org_jqt_JQtNavigation_nativeCurrentIndex(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    JQtNavigationWidget* nav = static_cast<JQtNavigationWidget*>(requireHandle(env, handle));
    return nav == nullptr ? 0 : static_cast<jint>(nav->currentIndex());
}

JNIEXPORT void JNICALL Java_org_jqt_JQtNavigation_nativeSetCurrentIndex(JNIEnv* env, jobject /*thiz*/, jlong handle, jint index) {
    JQtNavigationWidget* nav = static_cast<JQtNavigationWidget*>(requireHandle(env, handle));
    if (nav != nullptr) {
        nav->setCurrentIndex(static_cast<int>(index));
    }
}

// ----------------------------------------------------------------------------
// JQtSlider：Fluent 滑块（轨道 + 圆钮，accent 填充，拖动跟手，点击跳转动画）
// clean-room 自绘；轨道明暗感知，填充色跟随全局主题色。
// ----------------------------------------------------------------------------
class JQtSliderWidget : public QWidget {
public:
    explicit JQtSliderWidget(int min, int max, int value)
        : m_min(min), m_max(max), m_value(value) {
        setFixedHeight(24);
        setCursor(Qt::PointingHandCursor);
        m_anim = new QVariantAnimation(this);
        m_anim->setDuration(120);
        m_anim->setEasingCurve(QEasingCurve::OutCubic);
        QObject::connect(m_anim, &QVariantAnimation::valueChanged, this,
                         [this](const QVariant& v) {
                             m_value = qRound(v.toDouble());
                             update();
                             if (onValueChanged) {
                                 onValueChanged(m_value);
                             }
                         });
    }

    std::function<void(int)> onValueChanged;

    int minimum() const { return m_min; }
    int maximum() const { return m_max; }
    int value() const { return m_value; }

    void setValue(int value) {
        value = qBound(m_min, value, m_max);
        if (value == m_value) {
            return;
        }
        m_anim->stop();
        m_anim->setStartValue(static_cast<double>(m_value));
        m_anim->setEndValue(static_cast<double>(value));
        m_anim->start();
    }

    void setRange(int min, int max) {
        m_min = min;
        m_max = max;
        m_value = qBound(min, m_value, max);
        update();
    }

protected:
    void paintEvent(QPaintEvent*) override {
        QPainter p(this);
        p.setRenderHint(QPainter::Antialiasing);
        const double h = height();
        const double trackH = 4.0;
        const double trackY = (h - trackH) / 2.0;
        const double pad = 10.0;
        const double usable = width() - pad * 2.0;
        const double ratio = (m_max > m_min) ? (m_value - m_min) / static_cast<double>(m_max - m_min) : 0.0;
        const bool light = palette().color(QPalette::Window).lightness() > 128;
        const QColor trackColor = light ? QColor(0xd2, 0xd2, 0xd2) : QColor(0x4a, 0x4a, 0x4a);
        // 轨道底
        p.setPen(Qt::NoPen);
        p.setBrush(trackColor);
        p.drawRoundedRect(QRectF(pad, trackY, usable, trackH), trackH / 2.0, trackH / 2.0);
        // 已填充（主题色）
        p.setBrush(g_accentColor);
        p.drawRoundedRect(QRectF(pad, trackY, pad + usable * ratio - pad, trackH), trackH / 2.0, trackH / 2.0);
        // 刻度（setTickPosition 启用时绘制）
        if (m_tickPosition != 0 && m_tickInterval > 0 && m_max > m_min) {
            p.setPen(QPen(QColor(0, 0, 0, 60), 1));
            for (int tv = m_min; tv <= m_max; tv += m_tickInterval) {
                const double tx = pad + usable * (tv - m_min) / static_cast<double>(m_max - m_min);
                const double tickTop = (m_tickPosition == 1) ? 3.0 : h - 8.0;
                const double tickH = 5.0;
                p.drawLine(QPointF(tx, tickTop), QPointF(tx, tickTop + tickH));
            }
        }
        // 圆钮
        const double r = h / 2.0 - 3.0;
        const double cx = pad + usable * ratio;
        p.setBrush(light ? QColor(0xff, 0xff, 0xff) : QColor(0xe8, 0xe8, 0xe8));
        p.setPen(QPen(QColor(0, 0, 0, 40), 1));
        p.drawEllipse(QPointF(cx, h / 2.0), r, r);
    }

    double valueFromX(double x) const {
        const double pad = 10.0;
        const double usable = width() - pad * 2.0;
        double ratio = (x - pad) / usable;
        ratio = qBound(0.0, ratio, 1.0);
        return m_min + ratio * (m_max - m_min);
    }

    void mousePressEvent(QMouseEvent* event) override {
        if (event->button() == Qt::LeftButton) {
            m_dragging = true;
            setValue(qRound(valueFromX(event->position().x())));
            event->accept();
        }
    }

    void mouseMoveEvent(QMouseEvent* event) override {
        if (m_dragging) {
            // 拖动直接更新（不经过动画，跟手）
            m_value = qRound(valueFromX(event->position().x()));
            update();
            if (onValueChanged) {
                onValueChanged(m_value);
            }
            event->accept();
        }
    }

    void mouseReleaseEvent(QMouseEvent* event) override {
        m_dragging = false;
        QWidget::mouseReleaseEvent(event);
    }

    // 刻度（自绘支持；与 Qt QSlider::TickPosition 数值一致）
    int m_tickInterval = 0;
    int m_tickPosition = 0;

public:
    void setTickInterval(int interval) {
        m_tickInterval = interval;
        update();
    }
    int tickInterval() const { return m_tickInterval; }
    void setTickPosition(int position) {
        m_tickPosition = position;
        update();
    }
    int tickPosition() const { return m_tickPosition; }

private:
    int m_min;
    int m_max;
    int m_value;
    bool m_dragging = false;
    QVariantAnimation* m_anim;
};

JNIEXPORT jlong JNICALL Java_org_jqt_QSlider_nativeCreate(JNIEnv* env, jobject thiz, jint min, jint max, jint value) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    JQtSliderWidget* slider = new JQtSliderWidget(static_cast<int>(min), static_cast<int>(max), static_cast<int>(value));
    jobject gRef = env->NewGlobalRef(thiz);
    slider->onValueChanged = [gRef](int v) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleValueChanged", "(I)V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(v));
        }
    };
    return registerHandle(slider, /*javaOwned=*/true);
}

JNIEXPORT jint JNICALL Java_org_jqt_QSlider_nativeValue(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    JQtSliderWidget* slider = static_cast<JQtSliderWidget*>(requireHandle(env, handle));
    return slider == nullptr ? 0 : static_cast<jint>(slider->value());
}

JNIEXPORT void JNICALL Java_org_jqt_QSlider_nativeSetValue(JNIEnv* env, jobject /*thiz*/, jlong handle, jint value) {
    JQtSliderWidget* slider = static_cast<JQtSliderWidget*>(requireHandle(env, handle));
    if (slider != nullptr) {
        slider->setValue(static_cast<int>(value));
    }
}

JNIEXPORT void JNICALL Java_org_jqt_QSlider_nativeSetRange(JNIEnv* env, jobject /*thiz*/, jlong handle, jint min, jint max) {
    JQtSliderWidget* slider = static_cast<JQtSliderWidget*>(requireHandle(env, handle));
    if (slider != nullptr) {
        slider->setRange(static_cast<int>(min), static_cast<int>(max));
    }
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

        // 轨道颜色随进度渐变：关=灰（跟随主题明暗）→ 开=全局主题色（setAccentColor 可换）
        const bool lightTheme = palette().color(QPalette::Window).lightness() > 128;
        const QColor offColor = lightTheme ? QColor(0xc8, 0xc8, 0xc8)
                                           : QColor(0x4a, 0x4a, 0x4a);
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
JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativePostClickAt(JNIEnv* env, jclass /*cls*/, jlong targetHandle, jlong winHandle) {
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
JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeFadeIn(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong ms) {
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
JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeFadeOut(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong ms) {
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
JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeAnimateMove(JNIEnv* env, jclass /*cls*/, jlong handle, jint x, jint y, jlong ms) {
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
JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeAnimateResize(JNIEnv* env, jclass /*cls*/, jlong handle, jint w, jint h, jlong ms) {
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
JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeFadeIn(JNIEnv* env, jclass /*cls*/, jlong handle, jlong ms) {
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
JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeFadeOut(JNIEnv* env, jclass /*cls*/, jlong handle, jlong ms) {
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
JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetDropShadow(JNIEnv* env, jclass /*cls*/, jlong handle, jint blur, jint alpha, jint dx, jint dy) {
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

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeClearDropShadow(JNIEnv* env, jclass /*cls*/, jlong handle) {
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
// ----------------------------------------------------------------------------
// JQtMessageBox：模态对话框（QMessageBox 封装；样式由 QSS 控制）
// ----------------------------------------------------------------------------
JNIEXPORT jboolean JNICALL Java_org_jqt_QMessageBox_nativeShowQuestion(JNIEnv* env, jclass /*cls*/, jlong winHandle, jstring title, jstring text) {
    QWidget* parent = static_cast<QWidget*>(requireHandle(env, winHandle));
    const char* t1 = env->GetStringUTFChars(title, nullptr);
    const char* t2 = env->GetStringUTFChars(text, nullptr);
    QMessageBox box(parent);
    box.setWindowTitle(QString::fromUtf8(t1));
    box.setText(QString::fromUtf8(t2));
    box.setStandardButtons(QMessageBox::Yes | QMessageBox::No);
    env->ReleaseStringUTFChars(title, t1);
    env->ReleaseStringUTFChars(text, t2);
    return box.exec() == QMessageBox::Yes ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_org_jqt_QMessageBox_nativeShowInfo(JNIEnv* env, jclass /*cls*/, jlong winHandle, jstring title, jstring text) {
    QWidget* parent = static_cast<QWidget*>(requireHandle(env, winHandle));
    const char* t1 = env->GetStringUTFChars(title, nullptr);
    const char* t2 = env->GetStringUTFChars(text, nullptr);
    QMessageBox box(parent);
    box.setWindowTitle(QString::fromUtf8(t1));
    box.setText(QString::fromUtf8(t2));
    env->ReleaseStringUTFChars(title, t1);
    env->ReleaseStringUTFChars(text, t2);
    box.exec();
}

// ----------------------------------------------------------------------------
// JQtInfoBar：顶部通知条（滑入 + 停留 + 滑出，自动清理）
// 样式走 QSS 模板的 QFrame#infoBar 规则（主题化）。
// ----------------------------------------------------------------------------
JNIEXPORT void JNICALL Java_org_jqt_JQtInfoBar_nativeShowInfo(JNIEnv* env, jclass /*cls*/, jlong winHandle, jstring text, jlong durationMs) {
    QWidget* win = static_cast<QWidget*>(requireHandle(env, winHandle));
    if (win == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    const QString msg = QString::fromUtf8(utf);
    env->ReleaseStringUTFChars(text, utf);
    QFrame* bar = new QFrame(win);
    bar->setObjectName("infoBar");
    QHBoxLayout* lay = new QHBoxLayout(bar);
    lay->setContentsMargins(14, 8, 14, 8);
    QLabel* label = new QLabel(msg);
    label->setObjectName("infoBarLabel");
    lay->addWidget(label);
    bar->adjustSize();
    const int targetX = (win->width() - bar->width()) / 2;
    bar->move(targetX, -bar->height() - 4);
    bar->show();
    bar->raise();
    QPropertyAnimation* in = new QPropertyAnimation(bar, "pos", bar);
    in->setDuration(200);
    in->setStartValue(bar->pos());
    in->setEndValue(QPoint(targetX, 10));
    in->setEasingCurve(QEasingCurve::OutCubic);
    QTimer::singleShot(static_cast<int>(durationMs) + 220, bar, [bar, targetX]() {
        QPropertyAnimation* out = new QPropertyAnimation(bar, "pos", bar);
        out->setDuration(220);
        out->setStartValue(bar->pos());
        out->setEndValue(QPoint(targetX, -bar->height() - 20));
        out->setEasingCurve(QEasingCurve::InCubic);
        QObject::connect(out, &QPropertyAnimation::finished, bar, [bar]() {
            bar->deleteLater();
        });
        out->start();
    });
    in->start();
}



// ----------------------------------------------------------------------------
// JQt dialogs: QInputDialog / QFileDialog / QColorDialog / QFontDialog
// ----------------------------------------------------------------------------
JNIEXPORT jstring JNICALL Java_org_jqt_QInputDialog_nativeGetText(JNIEnv* env, jclass /*cls*/, jlong winHandle, jstring title, jstring label, jstring text) {
    QWidget* parent = static_cast<QWidget*>(requireHandle(env, winHandle));
    const char* t1 = env->GetStringUTFChars(title, nullptr);
    const char* t2 = env->GetStringUTFChars(label, nullptr);
    const char* t3 = env->GetStringUTFChars(text, nullptr);
    bool ok = false;
    QString res = QInputDialog::getText(parent, QString::fromUtf8(t1), QString::fromUtf8(t2), QLineEdit::Normal, QString::fromUtf8(t3), &ok);
    env->ReleaseStringUTFChars(title, t1);
    env->ReleaseStringUTFChars(label, t2);
    env->ReleaseStringUTFChars(text, t3);
    if (!ok) return nullptr;
    return env->NewStringUTF(res.toUtf8().constData());
}

JNIEXPORT jint JNICALL Java_org_jqt_QInputDialog_nativeGetInt(JNIEnv* env, jclass /*cls*/, jlong winHandle, jstring title, jstring label, jint value, jint min, jint max, jint step) {
    QWidget* parent = static_cast<QWidget*>(requireHandle(env, winHandle));
    const char* t1 = env->GetStringUTFChars(title, nullptr);
    const char* t2 = env->GetStringUTFChars(label, nullptr);
    bool ok = false;
    int res = QInputDialog::getInt(parent, QString::fromUtf8(t1), QString::fromUtf8(t2), value, min, max, step, &ok);
    env->ReleaseStringUTFChars(title, t1);
    env->ReleaseStringUTFChars(label, t2);
    return ok ? res : value;
}

JNIEXPORT jstring JNICALL Java_org_jqt_QInputDialog_nativeGetItem(JNIEnv* env, jclass /*cls*/, jlong winHandle, jstring title, jstring label, jobjectArray items, jint current) {
    QWidget* parent = static_cast<QWidget*>(requireHandle(env, winHandle));
    const char* t1 = env->GetStringUTFChars(title, nullptr);
    const char* t2 = env->GetStringUTFChars(label, nullptr);
    QStringList list;
    jsize n = env->GetArrayLength(items);
    for (jsize i = 0; i < n; i++) {
        jstring s = (jstring)env->GetObjectArrayElement(items, i);
        const char* c = env->GetStringUTFChars(s, nullptr);
        list << QString::fromUtf8(c);
        env->ReleaseStringUTFChars(s, c);
        env->DeleteLocalRef(s);
    }
    bool ok = false;
    QString res = QInputDialog::getItem(parent, QString::fromUtf8(t1), QString::fromUtf8(t2), list, current, false, &ok);
    env->ReleaseStringUTFChars(title, t1);
    env->ReleaseStringUTFChars(label, t2);
    if (!ok) return nullptr;
    return env->NewStringUTF(res.toUtf8().constData());
}

JNIEXPORT jstring JNICALL Java_org_jqt_QFileDialog_nativeGetOpenFileName(JNIEnv* env, jclass /*cls*/, jlong winHandle, jstring title, jstring dir, jstring filter) {
    QWidget* parent = static_cast<QWidget*>(requireHandle(env, winHandle));
    const char* t1 = env->GetStringUTFChars(title, nullptr);
    const char* t2 = env->GetStringUTFChars(dir, nullptr);
    const char* t3 = env->GetStringUTFChars(filter, nullptr);
    QString res = QFileDialog::getOpenFileName(parent, QString::fromUtf8(t1), QString::fromUtf8(t2), QString::fromUtf8(t3));
    env->ReleaseStringUTFChars(title, t1);
    env->ReleaseStringUTFChars(dir, t2);
    env->ReleaseStringUTFChars(filter, t3);
    if (res.isEmpty()) return nullptr;
    return env->NewStringUTF(res.toUtf8().constData());
}

JNIEXPORT jstring JNICALL Java_org_jqt_QFileDialog_nativeGetSaveFileName(JNIEnv* env, jclass /*cls*/, jlong winHandle, jstring title, jstring dir, jstring filter) {
    QWidget* parent = static_cast<QWidget*>(requireHandle(env, winHandle));
    const char* t1 = env->GetStringUTFChars(title, nullptr);
    const char* t2 = env->GetStringUTFChars(dir, nullptr);
    const char* t3 = env->GetStringUTFChars(filter, nullptr);
    QString res = QFileDialog::getSaveFileName(parent, QString::fromUtf8(t1), QString::fromUtf8(t2), QString::fromUtf8(t3));
    env->ReleaseStringUTFChars(title, t1);
    env->ReleaseStringUTFChars(dir, t2);
    env->ReleaseStringUTFChars(filter, t3);
    if (res.isEmpty()) return nullptr;
    return env->NewStringUTF(res.toUtf8().constData());
}

JNIEXPORT jstring JNICALL Java_org_jqt_QFileDialog_nativeGetExistingDirectory(JNIEnv* env, jclass /*cls*/, jlong winHandle, jstring title, jstring dir) {
    QWidget* parent = static_cast<QWidget*>(requireHandle(env, winHandle));
    const char* t1 = env->GetStringUTFChars(title, nullptr);
    const char* t2 = env->GetStringUTFChars(dir, nullptr);
    QString res = QFileDialog::getExistingDirectory(parent, QString::fromUtf8(t1), QString::fromUtf8(t2));
    env->ReleaseStringUTFChars(title, t1);
    env->ReleaseStringUTFChars(dir, t2);
    if (res.isEmpty()) return nullptr;
    return env->NewStringUTF(res.toUtf8().constData());
}

JNIEXPORT jint JNICALL Java_org_jqt_QColorDialog_nativeGetColor(JNIEnv* env, jclass /*cls*/, jlong winHandle, jstring title, jint argb) {
    QWidget* parent = static_cast<QWidget*>(requireHandle(env, winHandle));
    const char* t1 = env->GetStringUTFChars(title, nullptr);
    QColor col = QColorDialog::getColor(QColor::fromRgba(static_cast<QRgb>(argb)), parent, QString::fromUtf8(t1));
    env->ReleaseStringUTFChars(title, t1);
    if (!col.isValid()) return -1;
    return static_cast<jint>(col.rgba());
}

JNIEXPORT jstring JNICALL Java_org_jqt_QFontDialog_nativeGetFont(JNIEnv* env, jclass /*cls*/, jlong winHandle, jstring title, jstring family, jint size) {
    QWidget* parent = static_cast<QWidget*>(requireHandle(env, winHandle));
    const char* t1 = env->GetStringUTFChars(title, nullptr);
    QFont initial = QFont(QString(), size);
    if (family != nullptr) {
        const char* f = env->GetStringUTFChars(family, nullptr);
        initial = QFont(QString::fromUtf8(f), size);
        env->ReleaseStringUTFChars(family, f);
    }
    bool ok = false;
    QFont res = QFontDialog::getFont(&ok, initial, parent, QString::fromUtf8(t1));
    env->ReleaseStringUTFChars(title, t1);
    if (!ok) return nullptr;
    int ps = res.pointSize();
    if (ps < 0) ps = res.pixelSize();
    QString out = res.family() + QString(",") + QString::number(ps);
    return env->NewStringUTF(out.toUtf8().constData());
}

JNIEXPORT void JNICALL Java_org_jqt_QMessageBox_nativeShowWarning(JNIEnv* env, jclass /*cls*/, jlong winHandle, jstring title, jstring text) {
    QWidget* parent = static_cast<QWidget*>(requireHandle(env, winHandle));
    const char* t1 = env->GetStringUTFChars(title, nullptr);
    const char* t2 = env->GetStringUTFChars(text, nullptr);
    QMessageBox box(parent);
    box.setIcon(QMessageBox::Warning);
    box.setWindowTitle(QString::fromUtf8(t1));
    box.setText(QString::fromUtf8(t2));
    box.setStandardButtons(QMessageBox::Ok);
    env->ReleaseStringUTFChars(title, t1);
    env->ReleaseStringUTFChars(text, t2);
    box.exec();
}

JNIEXPORT void JNICALL Java_org_jqt_QMessageBox_nativeShowCritical(JNIEnv* env, jclass /*cls*/, jlong winHandle, jstring title, jstring text) {
    QWidget* parent = static_cast<QWidget*>(requireHandle(env, winHandle));
    const char* t1 = env->GetStringUTFChars(title, nullptr);
    const char* t2 = env->GetStringUTFChars(text, nullptr);
    QMessageBox box(parent);
    box.setIcon(QMessageBox::Critical);
    box.setWindowTitle(QString::fromUtf8(t1));
    box.setText(QString::fromUtf8(t2));
    box.setStandardButtons(QMessageBox::Ok);
    env->ReleaseStringUTFChars(title, t1);
    env->ReleaseStringUTFChars(text, t2);
    box.exec();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QMessageBox_nativeShowOkCancel(JNIEnv* env, jclass /*cls*/, jlong winHandle, jstring title, jstring text) {
    QWidget* parent = static_cast<QWidget*>(requireHandle(env, winHandle));
    const char* t1 = env->GetStringUTFChars(title, nullptr);
    const char* t2 = env->GetStringUTFChars(text, nullptr);
    QMessageBox box(parent);
    box.setIcon(QMessageBox::Question);
    box.setWindowTitle(QString::fromUtf8(t1));
    box.setText(QString::fromUtf8(t2));
    box.setStandardButtons(QMessageBox::Ok | QMessageBox::Cancel);
    env->ReleaseStringUTFChars(title, t1);
    env->ReleaseStringUTFChars(text, t2);
    return box.exec() == QMessageBox::Ok ? JNI_TRUE : JNI_FALSE;
}

// ----------------------------------------------------------------------------
// JQtTableWidget / JQtTreeWidget：表格与树
// ----------------------------------------------------------------------------
static std::unordered_map<long, QTreeWidgetItem*> g_treeItems;      // itemId -> item
static std::unordered_map<QTreeWidgetItem*, long> g_treeItemIds;    // item -> itemId

JNIEXPORT jlong JNICALL Java_org_jqt_QTableWidget_nativeCreate(JNIEnv* env, jobject thiz, jint rows, jint cols) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    QTableWidget* table = new QTableWidget(rows, cols);
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(table, &QTableWidget::cellClicked, [gRef](int row, int col) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleCellClicked", "(II)V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(row), static_cast<jint>(col));
        }
    });
    QObject::connect(table, &QTableWidget::currentCellChanged, [gRef](int row, int /*col*/, int /*prevRow*/, int /*prevCol*/) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleCurrentRowChanged", "(I)V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(row));
        }
    });
    return registerHandle(table, /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_QTableWidget_nativeSetItemText(JNIEnv* env, jobject /*thiz*/, jlong handle, jint row, jint col, jstring text) {
    QTableWidget* table = static_cast<QTableWidget*>(requireHandle(env, handle));
    if (table == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    QTableWidgetItem* item = table->item(row, col);
    if (item == nullptr) {
        item = new QTableWidgetItem();
        table->setItem(row, col, item);
    }
    item->setText(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
}

JNIEXPORT jstring JNICALL Java_org_jqt_QTableWidget_nativeItemText(JNIEnv* env, jobject /*thiz*/, jlong handle, jint row, jint col) {
    QTableWidget* table = static_cast<QTableWidget*>(requireHandle(env, handle));
    if (table == nullptr) {
        return nullptr;
    }
    QTableWidgetItem* item = table->item(row, col);
    if (item == nullptr) {
        return nullptr;
    }
    return env->NewStringUTF(item->text().toUtf8().constData());
}

JNIEXPORT void JNICALL Java_org_jqt_QTableWidget_nativeSetColumnHeaders(JNIEnv* env, jobject /*thiz*/, jlong handle, jobjectArray headers) {
    QTableWidget* table = static_cast<QTableWidget*>(requireHandle(env, handle));
    if (table == nullptr) {
        return;
    }
    QStringList list;
    jsize n = env->GetArrayLength(headers);
    for (jsize i = 0; i < n; i++) {
        jstring s = (jstring)env->GetObjectArrayElement(headers, i);
        const char* c = env->GetStringUTFChars(s, nullptr);
        list << QString::fromUtf8(c);
        env->ReleaseStringUTFChars(s, c);
        env->DeleteLocalRef(s);
    }
    table->setHorizontalHeaderLabels(list);
}

JNIEXPORT void JNICALL Java_org_jqt_QTableWidget_nativeSetRowCount(JNIEnv* env, jobject /*thiz*/, jlong handle, jint rows) {
    QTableWidget* table = static_cast<QTableWidget*>(requireHandle(env, handle));
    if (table != nullptr) {
        table->setRowCount(rows);
    }
}

JNIEXPORT void JNICALL Java_org_jqt_QTableWidget_nativeSetColumnCount(JNIEnv* env, jobject /*thiz*/, jlong handle, jint cols) {
    QTableWidget* table = static_cast<QTableWidget*>(requireHandle(env, handle));
    if (table != nullptr) {
        table->setColumnCount(cols);
    }
}

JNIEXPORT jint JNICALL Java_org_jqt_QTableWidget_nativeRowCount(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QTableWidget* table = static_cast<QTableWidget*>(requireHandle(env, handle));
    return table != nullptr ? static_cast<jint>(table->rowCount()) : 0;
}

JNIEXPORT jint JNICALL Java_org_jqt_QTableWidget_nativeColumnCount(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QTableWidget* table = static_cast<QTableWidget*>(requireHandle(env, handle));
    return table != nullptr ? static_cast<jint>(table->columnCount()) : 0;
}

JNIEXPORT void JNICALL Java_org_jqt_QTableWidget_nativeSetColumnWidth(JNIEnv* env, jobject /*thiz*/, jlong handle, jint col, jint width) {
    QTableWidget* table = static_cast<QTableWidget*>(requireHandle(env, handle));
    if (table != nullptr) {
        table->setColumnWidth(col, width);
    }
}

JNIEXPORT void JNICALL Java_org_jqt_QTableWidget_nativeSetRowHeight(JNIEnv* env, jobject /*thiz*/, jlong handle, jint row, jint height) {
    QTableWidget* table = static_cast<QTableWidget*>(requireHandle(env, handle));
    if (table != nullptr) {
        table->setRowHeight(row, height);
    }
}

JNIEXPORT void JNICALL Java_org_jqt_QTableWidget_nativeResizeColumnsToContents(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QTableWidget* table = static_cast<QTableWidget*>(requireHandle(env, handle));
    if (table != nullptr) {
        table->resizeColumnsToContents();
    }
}

JNIEXPORT void JNICALL Java_org_jqt_QTableWidget_nativeClearContents(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QTableWidget* table = static_cast<QTableWidget*>(requireHandle(env, handle));
    if (table != nullptr) {
        table->clearContents();
    }
}

JNIEXPORT jint JNICALL Java_org_jqt_QTableWidget_nativeCurrentRow(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QTableWidget* table = static_cast<QTableWidget*>(requireHandle(env, handle));
    return table != nullptr ? static_cast<jint>(table->currentRow()) : -1;
}

JNIEXPORT jlong JNICALL Java_org_jqt_QTreeWidget_nativeCreate(JNIEnv* env, jobject thiz) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    QTreeWidget* tree = new QTreeWidget();
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(tree, &QTreeWidget::itemClicked, [gRef](QTreeWidgetItem* item, int /*column*/) {
        JNIEnv* e = callbackEnv();
        auto it = g_treeItemIds.find(item);
        jmethodID mid = e->GetMethodID(e->GetObjectClass(gRef), "nativeHandleItemClicked", "(I)V");
        if (mid != nullptr && it != g_treeItemIds.end()) {
            JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(it->second));
        }
    });
    return registerHandle(tree, /*javaOwned=*/true);
}

JNIEXPORT jint JNICALL Java_org_jqt_QTreeWidget_nativeAddTopLevelItem(JNIEnv* env, jobject /*thiz*/, jlong handle, jint itemId, jstring text) {
    QTreeWidget* tree = static_cast<QTreeWidget*>(requireHandle(env, handle));
    if (tree == nullptr) {
        return -1;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    QTreeWidgetItem* item = new QTreeWidgetItem(tree);
    item->setText(0, QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
    g_treeItems[itemId] = item;
    g_treeItemIds[item] = itemId;
    return itemId;
}

JNIEXPORT jint JNICALL Java_org_jqt_QTreeWidget_nativeAddChild(JNIEnv* env, jobject /*thiz*/, jlong handle, jint parentItemId, jint itemId, jstring text) {
    QTreeWidget* tree = static_cast<QTreeWidget*>(requireHandle(env, handle));
    if (tree == nullptr) {
        return -1;
    }
    auto pit = g_treeItems.find(parentItemId);
    if (pit == g_treeItems.end()) {
        return -1;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    QTreeWidgetItem* item = new QTreeWidgetItem(pit->second);
    item->setText(0, QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
    g_treeItems[itemId] = item;
    g_treeItemIds[item] = itemId;
    return itemId;
}

JNIEXPORT jstring JNICALL Java_org_jqt_QTreeWidget_nativeItemText(JNIEnv* env, jobject /*thiz*/, jlong handle, jint itemId) {
    QTreeWidget* tree = static_cast<QTreeWidget*>(requireHandle(env, handle));
    if (tree == nullptr) {
        return nullptr;
    }
    auto it = g_treeItems.find(itemId);
    if (it == g_treeItems.end() || it->second == nullptr) {
        return nullptr;
    }
    return env->NewStringUTF(it->second->text(0).toUtf8().constData());
}

JNIEXPORT void JNICALL Java_org_jqt_QTreeWidget_nativeSetItemText(JNIEnv* env, jobject /*thiz*/, jlong handle, jint itemId, jstring text) {
    QTreeWidget* tree = static_cast<QTreeWidget*>(requireHandle(env, handle));
    if (tree == nullptr) {
        return;
    }
    auto it = g_treeItems.find(itemId);
    if (it == g_treeItems.end() || it->second == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    it->second->setText(0, QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
}

JNIEXPORT void JNICALL Java_org_jqt_QTreeWidget_nativeExpandAll(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QTreeWidget* tree = static_cast<QTreeWidget*>(requireHandle(env, handle));
    if (tree != nullptr) {
        tree->expandAll();
    }
}

JNIEXPORT void JNICALL Java_org_jqt_QTreeWidget_nativeCollapseAll(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QTreeWidget* tree = static_cast<QTreeWidget*>(requireHandle(env, handle));
    if (tree != nullptr) {
        tree->collapseAll();
    }
}

JNIEXPORT void JNICALL Java_org_jqt_QTreeWidget_nativeClear(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QTreeWidget* tree = static_cast<QTreeWidget*>(requireHandle(env, handle));
    if (tree == nullptr) {
        return;
    }
    tree->clear();
    g_treeItems.clear();
    g_treeItemIds.clear();
}

// ----------------------------------------------------------------------------
// JQtTabWidget / JQtGroupBox / JQtStackedLayout / JQtSplitter
// ----------------------------------------------------------------------------
JNIEXPORT jlong JNICALL Java_org_jqt_QTabWidget_nativeCreate(JNIEnv* env, jobject thiz) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    QTabWidget* tab = new QTabWidget();
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(tab, &QTabWidget::currentChanged, [gRef](int index) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleCurrentChanged", "(I)V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(index));
        }
    });
    return registerHandle(tab, /*javaOwned=*/true);
}

JNIEXPORT jint JNICALL Java_org_jqt_QTabWidget_nativeAddTab(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong childHandle, jstring title) {
    QTabWidget* tab = static_cast<QTabWidget*>(requireHandle(env, handle));
    if (tab == nullptr) {
        return -1;
    }
    QWidget* child = static_cast<QWidget*>(requireHandle(env, childHandle));
    if (child == nullptr) {
        return -1;
    }
    const char* utf = env->GetStringUTFChars(title, nullptr);
    int index = tab->addTab(child, QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(title, utf);
    markQtOwned(childHandle);
    // 不 show：QTabWidget 页可见性由自身管理（show 会破坏切换逻辑）
    return index;
}

JNIEXPORT void JNICALL Java_org_jqt_QTabWidget_nativeSetCurrentIndex(JNIEnv* env, jobject /*thiz*/, jlong handle, jint index) {
    QTabWidget* tab = static_cast<QTabWidget*>(requireHandle(env, handle));
    if (tab != nullptr) {
        tab->setCurrentIndex(index);
    }
}

JNIEXPORT jint JNICALL Java_org_jqt_QTabWidget_nativeCurrentIndex(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QTabWidget* tab = static_cast<QTabWidget*>(requireHandle(env, handle));
    return tab != nullptr ? static_cast<jint>(tab->currentIndex()) : -1;
}

JNIEXPORT void JNICALL Java_org_jqt_QTabWidget_nativeSetTabText(JNIEnv* env, jobject /*thiz*/, jlong handle, jint index, jstring title) {
    QTabWidget* tab = static_cast<QTabWidget*>(requireHandle(env, handle));
    if (tab == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(title, nullptr);
    tab->setTabText(index, QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(title, utf);
}

JNIEXPORT jlong JNICALL Java_org_jqt_QGroupBox_nativeCreate(JNIEnv* env, jobject /*thiz*/, jstring title) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    const char* utf = env->GetStringUTFChars(title, nullptr);
    QGroupBox* box = new QGroupBox(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(title, utf);
    return registerHandle(box, /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_QGroupBox_nativeSetTitle(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring title) {
    QGroupBox* box = static_cast<QGroupBox*>(requireHandle(env, handle));
    if (box == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(title, nullptr);
    box->setTitle(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(title, utf);
}

JNIEXPORT jstring JNICALL Java_org_jqt_QGroupBox_nativeTitle(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QGroupBox* box = static_cast<QGroupBox*>(requireHandle(env, handle));
    if (box == nullptr) {
        return nullptr;
    }
    return env->NewStringUTF(box->title().toUtf8().constData());
}

JNIEXPORT jlong JNICALL Java_org_jqt_QStackedLayout_nativeCreate(JNIEnv* env, jobject /*thiz*/) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    return registerHandle(new QStackedLayout(), /*javaOwned=*/true);
}

JNIEXPORT jint JNICALL Java_org_jqt_QStackedLayout_nativeAddPage(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong childHandle) {
    QStackedLayout* stack = static_cast<QStackedLayout*>(requireHandle(env, handle));
    if (stack == nullptr) {
        return -1;
    }
    QWidget* child = static_cast<QWidget*>(requireHandle(env, childHandle));
    if (child == nullptr) {
        return -1;
    }
    int index = stack->addWidget(child);
    markQtOwned(childHandle);
    // 不 show：QStackedLayout 的页可见性由 setCurrentIndex 管理（直接 show 会导致多页堆叠）
    return index;
}

JNIEXPORT void JNICALL Java_org_jqt_QStackedLayout_nativeSetCurrentIndex(JNIEnv* env, jobject /*thiz*/, jlong handle, jint index) {
    QStackedLayout* stack = static_cast<QStackedLayout*>(requireHandle(env, handle));
    if (stack != nullptr) {
        stack->setCurrentIndex(index);
    }
}

JNIEXPORT jint JNICALL Java_org_jqt_QStackedLayout_nativeCurrentIndex(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QStackedLayout* stack = static_cast<QStackedLayout*>(requireHandle(env, handle));
    return stack != nullptr ? static_cast<jint>(stack->currentIndex()) : -1;
}

JNIEXPORT void JNICALL Java_org_jqt_QStackedLayout_nativeSetCurrentWidget(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong childHandle) {
    QStackedLayout* stack = static_cast<QStackedLayout*>(requireHandle(env, handle));
    if (stack == nullptr) {
        return;
    }
    QWidget* child = static_cast<QWidget*>(requireHandle(env, childHandle));
    if (child == nullptr) {
        return;
    }
    stack->setCurrentWidget(child);
}

JNIEXPORT jlong JNICALL Java_org_jqt_QSplitter_nativeCreate(JNIEnv* env, jobject /*thiz*/) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    return registerHandle(new QSplitter(Qt::Horizontal), /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_QSplitter_nativeSetOrientation(JNIEnv* env, jobject /*thiz*/, jlong handle, jint orientation) {
    QSplitter* split = static_cast<QSplitter*>(requireHandle(env, handle));
    if (split != nullptr) {
        split->setOrientation(orientation == 0 ? Qt::Horizontal : Qt::Vertical);
    }
}

JNIEXPORT void JNICALL Java_org_jqt_QSplitter_nativeAddWidget(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong childHandle) {
    QSplitter* split = static_cast<QSplitter*>(requireHandle(env, handle));
    if (split == nullptr) {
        return;
    }
    QWidget* child = static_cast<QWidget*>(requireHandle(env, childHandle));
    if (child == nullptr) {
        return;
    }
    split->addWidget(child);
    markQtOwned(childHandle);
    child->show();   // 子控件 parent=splitter（非空），不会成顶层窗口
}

JNIEXPORT void JNICALL Java_org_jqt_QSplitter_nativeSetSizes(JNIEnv* env, jobject /*thiz*/, jlong handle, jintArray sizes) {
    QSplitter* split = static_cast<QSplitter*>(requireHandle(env, handle));
    if (split == nullptr) {
        return;
    }
    jsize n = env->GetArrayLength(sizes);
    jint* buf = env->GetIntArrayElements(sizes, nullptr);
    QList<int> list;
    for (jsize i = 0; i < n; i++) {
        list << static_cast<int>(buf[i]);
    }
    env->ReleaseIntArrayElements(sizes, buf, JNI_ABORT);
    split->setSizes(list);
}

JNIEXPORT jintArray JNICALL Java_org_jqt_QSplitter_nativeSizes(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSplitter* split = static_cast<QSplitter*>(requireHandle(env, handle));
    if (split == nullptr) {
        return nullptr;
    }
    QList<int> list = split->sizes();
    jintArray arr = env->NewIntArray(static_cast<jsize>(list.size()));
    if (arr != nullptr && !list.isEmpty()) {
        std::vector<jint> buf;
        buf.reserve(list.size());
        for (int v : list) {
            buf.push_back(static_cast<jint>(v));
        }
        env->SetIntArrayRegion(arr, 0, static_cast<jsize>(buf.size()), buf.data());
    }
    return arr;
}

JNIEXPORT void JNICALL Java_org_jqt_QSplitter_nativeSetHandleWidth(JNIEnv* env, jobject /*thiz*/, jlong handle, jint width) {
    QSplitter* split = static_cast<QSplitter*>(requireHandle(env, handle));
    if (split != nullptr) {
        split->setHandleWidth(width);
    }
}

// ----------------------------------------------------------------------------
// JQtSpinBox / JQtDial / JQtRadioButton / JQtDateTimeEdit
// ----------------------------------------------------------------------------
JNIEXPORT jlong JNICALL Java_org_jqt_QSpinBox_nativeCreate(JNIEnv* env, jobject thiz) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    QSpinBox* spin = new QSpinBox();
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(spin, QOverload<int>::of(&QSpinBox::valueChanged), [gRef](int value) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleValueChanged", "(I)V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(value));
        }
    });
    QObject::connect(spin, &QSpinBox::textChanged, [gRef](const QString& text) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleTextChanged", "(Ljava/lang/String;)V");
        if (mid != nullptr) {
            jstring js = e->NewStringUTF(text.toUtf8().constData());
            JQT_CALL_VOID(e, gRef, mid, js);
            e->DeleteLocalRef(js);
        }
    });
    return registerHandle(spin, /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_QSpinBox_nativeSetRange(JNIEnv* env, jobject /*thiz*/, jlong handle, jint min, jint max) {
    QSpinBox* spin = static_cast<QSpinBox*>(requireHandle(env, handle));
    if (spin != nullptr) {
        spin->setRange(min, max);
    }
}

JNIEXPORT jint JNICALL Java_org_jqt_QSpinBox_nativeValue(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSpinBox* spin = static_cast<QSpinBox*>(requireHandle(env, handle));
    return spin != nullptr ? static_cast<jint>(spin->value()) : 0;
}

JNIEXPORT void JNICALL Java_org_jqt_QSpinBox_nativeSetValue(JNIEnv* env, jobject /*thiz*/, jlong handle, jint value) {
    QSpinBox* spin = static_cast<QSpinBox*>(requireHandle(env, handle));
    if (spin != nullptr) {
        spin->setValue(value);
    }
}

JNIEXPORT jlong JNICALL Java_org_jqt_QDial_nativeCreate(JNIEnv* env, jobject thiz) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    QDial* dial = new QDial();
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(dial, &QDial::valueChanged, [gRef](int value) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleValueChanged", "(I)V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(value));
        }
    });
    return registerHandle(dial, /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_QDial_nativeSetRange(JNIEnv* env, jobject /*thiz*/, jlong handle, jint min, jint max) {
    QDial* dial = static_cast<QDial*>(requireHandle(env, handle));
    if (dial != nullptr) {
        dial->setRange(min, max);
    }
}

JNIEXPORT jint JNICALL Java_org_jqt_QDial_nativeValue(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QDial* dial = static_cast<QDial*>(requireHandle(env, handle));
    return dial != nullptr ? static_cast<jint>(dial->value()) : 0;
}

JNIEXPORT void JNICALL Java_org_jqt_QDial_nativeSetValue(JNIEnv* env, jobject /*thiz*/, jlong handle, jint value) {
    QDial* dial = static_cast<QDial*>(requireHandle(env, handle));
    if (dial != nullptr) {
        dial->setValue(value);
    }
}

JNIEXPORT jlong JNICALL Java_org_jqt_QRadioButton_nativeCreate(JNIEnv* env, jobject thiz, jstring text) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    QRadioButton* btn = new QRadioButton(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(btn, &QRadioButton::toggled, [gRef](bool checked) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleToggled", "(Z)V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, gRef, mid, checked ? JNI_TRUE : JNI_FALSE);
        }
    });
    return registerHandle(btn, /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_QRadioButton_nativeSetText(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring text) {
    QRadioButton* btn = static_cast<QRadioButton*>(requireHandle(env, handle));
    if (btn == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    btn->setText(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QRadioButton_nativeIsChecked(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QRadioButton* btn = static_cast<QRadioButton*>(requireHandle(env, handle));
    return (btn != nullptr && btn->isChecked()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_org_jqt_QRadioButton_nativeSetChecked(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean checked) {
    QRadioButton* btn = static_cast<QRadioButton*>(requireHandle(env, handle));
    if (btn != nullptr) {
        btn->setChecked(checked == JNI_TRUE);
    }
}

JNIEXPORT jlong JNICALL Java_org_jqt_QDateTimeEdit_nativeCreate(JNIEnv* env, jobject thiz) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    QDateTimeEdit* edit = new QDateTimeEdit(QDateTime::currentDateTime());
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(edit, &QDateTimeEdit::dateTimeChanged, [gRef, edit](const QDateTime& /*dt*/) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleTextChanged", "(Ljava/lang/String;)V");
        if (mid != nullptr) {
            jstring js = e->NewStringUTF(edit->text().toUtf8().constData());
            JQT_CALL_VOID(e, gRef, mid, js);
            e->DeleteLocalRef(js);
        }
    });
    return registerHandle(edit, /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_QDateTimeEdit_nativeSetDisplayFormat(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring format) {
    QDateTimeEdit* edit = static_cast<QDateTimeEdit*>(requireHandle(env, handle));
    if (edit == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(format, nullptr);
    edit->setDisplayFormat(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(format, utf);
}

JNIEXPORT void JNICALL Java_org_jqt_QDateTimeEdit_nativeSetDateTime(JNIEnv* env, jobject /*thiz*/, jlong handle, jint year, jint month, jint day, jint hour, jint minute, jint second) {
    QDateTimeEdit* edit = static_cast<QDateTimeEdit*>(requireHandle(env, handle));
    if (edit == nullptr) {
        return;
    }
    edit->setDateTime(QDateTime(QDate(year, month, day), QTime(hour, minute, second)));
}

JNIEXPORT jstring JNICALL Java_org_jqt_QDateTimeEdit_nativeText(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QDateTimeEdit* edit = static_cast<QDateTimeEdit*>(requireHandle(env, handle));
    if (edit == nullptr) {
        return nullptr;
    }
    return env->NewStringUTF(edit->text().toUtf8().constData());
}

// ----------------------------------------------------------------------------
// JQtGridLayout / JQtFormLayout
// ----------------------------------------------------------------------------
JNIEXPORT jlong JNICALL Java_org_jqt_QGridLayout_nativeCreate(JNIEnv* env, jobject /*thiz*/) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    return registerHandle(new QGridLayout(), /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_QGridLayout_nativeAddWidget(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong childHandle, jint row, jint col, jint rowSpan, jint colSpan) {
    QGridLayout* grid = static_cast<QGridLayout*>(requireHandle(env, handle));
    if (grid == nullptr) {
        return;
    }
    QWidget* child = static_cast<QWidget*>(requireHandle(env, childHandle));
    if (child == nullptr) {
        return;
    }
    grid->addWidget(child, row, col, rowSpan, colSpan);
    markQtOwned(childHandle);
    if (grid->parentWidget() != nullptr) {
        child->show();
    }
}

JNIEXPORT void JNICALL Java_org_jqt_QGridLayout_nativeSetColumnStretch(JNIEnv* env, jobject /*thiz*/, jlong handle, jint col, jint stretch) {
    QGridLayout* grid = static_cast<QGridLayout*>(requireHandle(env, handle));
    if (grid != nullptr) {
        grid->setColumnStretch(col, stretch);
    }
}

JNIEXPORT void JNICALL Java_org_jqt_QGridLayout_nativeSetRowStretch(JNIEnv* env, jobject /*thiz*/, jlong handle, jint row, jint stretch) {
    QGridLayout* grid = static_cast<QGridLayout*>(requireHandle(env, handle));
    if (grid != nullptr) {
        grid->setRowStretch(row, stretch);
    }
}

JNIEXPORT jlong JNICALL Java_org_jqt_QFormLayout_nativeCreate(JNIEnv* env, jobject /*thiz*/) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    return registerHandle(new QFormLayout(), /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_QFormLayout_nativeAddRowString(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring label, jlong fieldHandle) {
    QFormLayout* form = static_cast<QFormLayout*>(requireHandle(env, handle));
    if (form == nullptr) {
        return;
    }
    QWidget* field = static_cast<QWidget*>(requireHandle(env, fieldHandle));
    if (field == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(label, nullptr);
    form->addRow(QString::fromUtf8(utf), field);
    env->ReleaseStringUTFChars(label, utf);
    markQtOwned(fieldHandle);
    if (form->parentWidget() != nullptr) {
        field->show();
    }
}

JNIEXPORT void JNICALL Java_org_jqt_QFormLayout_nativeAddRowWidget(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong labelHandle, jlong fieldHandle) {
    QFormLayout* form = static_cast<QFormLayout*>(requireHandle(env, handle));
    if (form == nullptr) {
        return;
    }
    QWidget* label = static_cast<QWidget*>(requireHandle(env, labelHandle));
    QWidget* field = static_cast<QWidget*>(requireHandle(env, fieldHandle));
    if (label == nullptr || field == nullptr) {
        return;
    }
    form->addRow(label, field);
    markQtOwned(labelHandle);
    markQtOwned(fieldHandle);
    if (form->parentWidget() != nullptr) {
        label->show();
        field->show();
    }
}

// ----------------------------------------------------------------------------
// JQtMenu / JQtToolBar / JQtStatusBar / JQtSystemTrayIcon
// ----------------------------------------------------------------------------
JNIEXPORT jlong JNICALL Java_org_jqt_QMenu_nativeCreate(JNIEnv* env, jobject thiz) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    QMenu* menu = new QMenu();
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(menu, &QMenu::triggered, [gRef](QAction* action) {
        if (action == nullptr) {
            return;
        }
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleTriggered", "(I)V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(action->data().toInt()));
        }
    });
    return registerHandle(menu, /*javaOwned=*/true);
}

JNIEXPORT jint JNICALL Java_org_jqt_QMenu_nativeAddItem(JNIEnv* env, jobject /*thiz*/, jlong handle, jint actionId, jstring text) {
    QMenu* menu = static_cast<QMenu*>(requireHandle(env, handle));
    if (menu == nullptr) {
        return -1;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    QAction* act = menu->addAction(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
    act->setData(actionId);
    return actionId;
}

JNIEXPORT void JNICALL Java_org_jqt_QMenu_nativePopupAt(JNIEnv* env, jobject /*thiz*/, jlong handle, jint x, jint y) {
    QMenu* menu = static_cast<QMenu*>(requireHandle(env, handle));
    if (menu != nullptr) {
        menu->popup(QPoint(x, y));
    }
}

JNIEXPORT void JNICALL Java_org_jqt_QMenu_nativePopupAnchor(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong anchorHandle) {
    QMenu* menu = static_cast<QMenu*>(requireHandle(env, handle));
    if (menu == nullptr) {
        return;
    }
    QWidget* anchor = static_cast<QWidget*>(requireHandle(env, anchorHandle));
    if (anchor == nullptr) {
        return;
    }
    menu->popup(anchor->mapToGlobal(QPoint(0, anchor->height())));
}

JNIEXPORT jlong JNICALL Java_org_jqt_QToolBar_nativeCreate(JNIEnv* env, jobject thiz) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    QToolBar* bar = new QToolBar();
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(bar, &QToolBar::actionTriggered, [gRef](QAction* action) {
        if (action == nullptr) {
            return;
        }
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleTriggered", "(I)V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(action->data().toInt()));
        }
    });
    return registerHandle(bar, /*javaOwned=*/true);
}

JNIEXPORT jint JNICALL Java_org_jqt_QToolBar_nativeAddButton(JNIEnv* env, jobject /*thiz*/, jlong handle, jint actionId, jstring text) {
    QToolBar* bar = static_cast<QToolBar*>(requireHandle(env, handle));
    if (bar == nullptr) {
        return -1;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    QAction* act = bar->addAction(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
    act->setData(actionId);
    return actionId;
}

JNIEXPORT void JNICALL Java_org_jqt_QToolBar_nativeAddWidget(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong childHandle) {
    QToolBar* bar = static_cast<QToolBar*>(requireHandle(env, handle));
    if (bar == nullptr) {
        return;
    }
    QWidget* child = static_cast<QWidget*>(requireHandle(env, childHandle));
    if (child == nullptr) {
        return;
    }
    bar->addWidget(child);
    markQtOwned(childHandle);
    child->show();   // 子控件 parent=bar（非空），不会成顶层窗口
}

JNIEXPORT jlong JNICALL Java_org_jqt_QStatusBar_nativeCreate(JNIEnv* env, jobject /*thiz*/) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    return registerHandle(new QStatusBar(), /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_QStatusBar_nativeShowMessage(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring text, jint ms) {
    QStatusBar* bar = static_cast<QStatusBar*>(requireHandle(env, handle));
    if (bar == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    bar->showMessage(QString::fromUtf8(utf), ms);
    env->ReleaseStringUTFChars(text, utf);
}

JNIEXPORT void JNICALL Java_org_jqt_QStatusBar_nativeClearMessage(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QStatusBar* bar = static_cast<QStatusBar*>(requireHandle(env, handle));
    if (bar != nullptr) {
        bar->clearMessage();
    }
}

JNIEXPORT jstring JNICALL Java_org_jqt_QStatusBar_nativeCurrentMessage(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QStatusBar* bar = static_cast<QStatusBar*>(requireHandle(env, handle));
    if (bar == nullptr) {
        return nullptr;
    }
    return env->NewStringUTF(bar->currentMessage().toUtf8().constData());
}

JNIEXPORT jlong JNICALL Java_org_jqt_QSystemTrayIcon_nativeCreate(JNIEnv* env, jobject /*thiz*/) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    QSystemTrayIcon* tray = new QSystemTrayIcon(QApplication::style()->standardIcon(QStyle::SP_MessageBoxInformation));
    return registerHandle(tray, /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_QSystemTrayIcon_nativeShow(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSystemTrayIcon* tray = static_cast<QSystemTrayIcon*>(requireHandle(env, handle));
    if (tray != nullptr) {
        tray->show();
    }
}

JNIEXPORT void JNICALL Java_org_jqt_QSystemTrayIcon_nativeHide(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSystemTrayIcon* tray = static_cast<QSystemTrayIcon*>(requireHandle(env, handle));
    if (tray != nullptr) {
        tray->hide();
    }
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QSystemTrayIcon_nativeIsVisible(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSystemTrayIcon* tray = static_cast<QSystemTrayIcon*>(requireHandle(env, handle));
    return (tray != nullptr && tray->isVisible()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_org_jqt_QSystemTrayIcon_nativeSetToolTip(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring tip) {
    QSystemTrayIcon* tray = static_cast<QSystemTrayIcon*>(requireHandle(env, handle));
    if (tray == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(tip, nullptr);
    tray->setToolTip(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(tip, utf);
}

JNIEXPORT void JNICALL Java_org_jqt_QSystemTrayIcon_nativeShowMessage(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring title, jstring message, jint ms) {
    QSystemTrayIcon* tray = static_cast<QSystemTrayIcon*>(requireHandle(env, handle));
    if (tray == nullptr) {
        return;
    }
    const char* t1 = env->GetStringUTFChars(title, nullptr);
    const char* t2 = env->GetStringUTFChars(message, nullptr);
    tray->showMessage(QString::fromUtf8(t1), QString::fromUtf8(t2), QSystemTrayIcon::Information, ms);
    env->ReleaseStringUTFChars(title, t1);
    env->ReleaseStringUTFChars(message, t2);
}

JNIEXPORT void JNICALL Java_org_jqt_QSystemTrayIcon_nativeDispose(JNIEnv* /*env*/, jclass /*cls*/, jlong handle) {
    QObject* obj = nullptr;
    {
        std::lock_guard<std::mutex> lock(g_handleMutex);
        auto it = g_handles.find(static_cast<int64_t>(handle));
        if (it == g_handles.end()) {
            return;
        }
        auto oit = g_javaOwned.find(static_cast<int64_t>(handle));
        if (oit == g_javaOwned.end() || !oit->second) {
            return;
        }
        obj = static_cast<QObject*>(it->second);
        g_handles.erase(it);
        g_javaOwned.erase(oit);
    }
    if (obj != nullptr && g_app != nullptr) {
        QMetaObject::invokeMethod(g_app, [obj]() { delete obj; }, Qt::QueuedConnection);
    }
}

// ----------------------------------------------------------------------------
// JQtTextEdit：多行文本编辑器
// ----------------------------------------------------------------------------
JNIEXPORT jlong JNICALL Java_org_jqt_QTextEdit_nativeCreate(JNIEnv* env, jobject thiz) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    QPlainTextEdit* edit = new QPlainTextEdit();
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(edit, &QPlainTextEdit::textChanged, [gRef]() {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleTextChanged", "()V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, gRef, mid);
        }
    });
    return registerHandle(edit, /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_QTextEdit_nativeSetPlainText(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring text) {
    QPlainTextEdit* edit = static_cast<QPlainTextEdit*>(requireHandle(env, handle));
    if (edit == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    edit->setPlainText(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
}

JNIEXPORT jstring JNICALL Java_org_jqt_QTextEdit_nativeToPlainText(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QPlainTextEdit* edit = static_cast<QPlainTextEdit*>(requireHandle(env, handle));
    if (edit == nullptr) {
        return nullptr;
    }
    return env->NewStringUTF(edit->toPlainText().toUtf8().constData());
}

JNIEXPORT void JNICALL Java_org_jqt_QTextEdit_nativeAppend(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring text) {
    QPlainTextEdit* edit = static_cast<QPlainTextEdit*>(requireHandle(env, handle));
    if (edit == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    edit->appendPlainText(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
}

JNIEXPORT void JNICALL Java_org_jqt_QTextEdit_nativeSetReadOnly(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean readOnly) {
    QPlainTextEdit* edit = static_cast<QPlainTextEdit*>(requireHandle(env, handle));
    if (edit != nullptr) {
        edit->setReadOnly(readOnly == JNI_TRUE);
    }
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QTextEdit_nativeIsReadOnly(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QPlainTextEdit* edit = static_cast<QPlainTextEdit*>(requireHandle(env, handle));
    return (edit != nullptr && edit->isReadOnly()) ? JNI_TRUE : JNI_FALSE;
}

// ----------------------------------------------------------------------------
// JQtCanvasWidget + JQtPainter：自绘画布与 2D 画笔
// ----------------------------------------------------------------------------
static QPainter* g_currentPainter = nullptr;   // paintEvent 期间有效

class JQtCanvasWidget : public QWidget {
public:
    jobject javaRef = nullptr;
protected:
    void paintEvent(QPaintEvent*) override {
        if (javaRef == nullptr) {
            return;
        }
        QPainter painter(this);
        g_currentPainter = &painter;
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(javaRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandlePaint", "()V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, javaRef, mid);
        }
        g_currentPainter = nullptr;
    }
};

JNIEXPORT jlong JNICALL Java_org_jqt_QCanvasWidget_nativeCreate(JNIEnv* env, jobject thiz) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    JQtCanvasWidget* canvas = new JQtCanvasWidget();
    canvas->javaRef = env->NewGlobalRef(thiz);
    return registerHandle(canvas, /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_QCanvasWidget_nativeRepaint(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* canvas = static_cast<QWidget*>(requireHandle(env, handle));
    if (canvas != nullptr) {
        canvas->update();
    }
}

JNIEXPORT jlong JNICALL Java_org_jqt_QCanvasWidget_nativeCurrentPainter(JNIEnv* env, jclass /*cls*/) {
    return reinterpret_cast<jlong>(g_currentPainter);
}

JNIEXPORT void JNICALL Java_org_jqt_QPainter_nativeSetColor(JNIEnv* env, jclass /*cls*/, jlong handle, jint argb) {
    QPainter* p = reinterpret_cast<QPainter*>(handle);
    if (p == nullptr) {
        return;
    }
    p->setPen(QColor::fromRgba(static_cast<QRgb>(argb)));
    p->setBrush(Qt::NoBrush);
}

JNIEXPORT void JNICALL Java_org_jqt_QPainter_nativeSetStrokeWidth(JNIEnv* env, jclass /*cls*/, jlong handle, jdouble width) {
    QPainter* p = reinterpret_cast<QPainter*>(handle);
    if (p == nullptr) {
        return;
    }
    QPen pen = p->pen();
    pen.setWidthF(width);
    p->setPen(pen);
}

JNIEXPORT void JNICALL Java_org_jqt_QPainter_nativeDrawLine(JNIEnv* env, jclass /*cls*/, jlong handle, jdouble x1, jdouble y1, jdouble x2, jdouble y2) {
    QPainter* p = reinterpret_cast<QPainter*>(handle);
    if (p != nullptr) {
        p->drawLine(QPointF(x1, y1), QPointF(x2, y2));
    }
}

JNIEXPORT void JNICALL Java_org_jqt_QPainter_nativeDrawRect(JNIEnv* env, jclass /*cls*/, jlong handle, jdouble x, jdouble y, jdouble w, jdouble h) {
    QPainter* p = reinterpret_cast<QPainter*>(handle);
    if (p != nullptr) {
        p->drawRect(QRectF(x, y, w, h));
    }
}

JNIEXPORT void JNICALL Java_org_jqt_QPainter_nativeFillRect(JNIEnv* env, jclass /*cls*/, jlong handle, jdouble x, jdouble y, jdouble w, jdouble h) {
    QPainter* p = reinterpret_cast<QPainter*>(handle);
    if (p == nullptr) {
        return;
    }
    QColor c = p->pen().color();
    p->fillRect(QRectF(x, y, w, h), c);
}

JNIEXPORT void JNICALL Java_org_jqt_QPainter_nativeDrawEllipse(JNIEnv* env, jclass /*cls*/, jlong handle, jdouble x, jdouble y, jdouble w, jdouble h) {
    QPainter* p = reinterpret_cast<QPainter*>(handle);
    if (p != nullptr) {
        p->drawEllipse(QRectF(x, y, w, h));
    }
}

JNIEXPORT void JNICALL Java_org_jqt_QPainter_nativeFillEllipse(JNIEnv* env, jclass /*cls*/, jlong handle, jdouble x, jdouble y, jdouble w, jdouble h) {
    QPainter* p = reinterpret_cast<QPainter*>(handle);
    if (p == nullptr) {
        return;
    }
    QColor c = p->pen().color();
    p->setBrush(c);
    p->drawEllipse(QRectF(x, y, w, h));
    p->setBrush(Qt::NoBrush);
}

JNIEXPORT void JNICALL Java_org_jqt_QPainter_nativeDrawRoundRect(JNIEnv* env, jclass /*cls*/, jlong handle, jdouble x, jdouble y, jdouble w, jdouble h, jdouble radius) {
    QPainter* p = reinterpret_cast<QPainter*>(handle);
    if (p != nullptr) {
        p->drawRoundedRect(QRectF(x, y, w, h), radius, radius);
    }
}

JNIEXPORT void JNICALL Java_org_jqt_QPainter_nativeDrawText(JNIEnv* env, jclass /*cls*/, jlong handle, jdouble x, jdouble y, jstring text) {
    QPainter* p = reinterpret_cast<QPainter*>(handle);
    if (p == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    p->drawText(QPointF(x, y), QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
}

JNIEXPORT void JNICALL Java_org_jqt_QPainter_nativeSetFont(JNIEnv* env, jclass /*cls*/, jlong handle, jstring family, jint pointSize) {
    QPainter* p = reinterpret_cast<QPainter*>(handle);
    if (p == nullptr) {
        return;
    }
    const char* utf = env->GetStringUTFChars(family, nullptr);
    p->setFont(QFont(QString::fromUtf8(utf), pointSize));
    env->ReleaseStringUTFChars(family, utf);
}

JNIEXPORT void JNICALL Java_org_jqt_QPainter_nativeTranslate(JNIEnv* env, jclass /*cls*/, jlong handle, jdouble dx, jdouble dy) {
    QPainter* p = reinterpret_cast<QPainter*>(handle);
    if (p != nullptr) {
        p->translate(dx, dy);
    }
}

JNIEXPORT void JNICALL Java_org_jqt_QPainter_nativeRotate(JNIEnv* env, jclass /*cls*/, jlong handle, jdouble degrees) {
    QPainter* p = reinterpret_cast<QPainter*>(handle);
    if (p != nullptr) {
        p->rotate(degrees);
    }
}

// ----------------------------------------------------------------------------
// QPainter 值对象批（手写精修：画刷/画笔/路径/图像/裁剪/变换）
// ----------------------------------------------------------------------------
JNIEXPORT void JNICALL Java_org_jqt_QPainter_nativeSetBrush(JNIEnv* /*env*/, jclass /*cls*/, jlong handle, jint argb, jint style) {
    QPainter* p = reinterpret_cast<QPainter*>(handle);
    if (p == nullptr) return;
    p->setBrush(QBrush(QColor::fromRgba(static_cast<QRgb>(argb)), static_cast<Qt::BrushStyle>(style)));
}

JNIEXPORT void JNICALL Java_org_jqt_QPainter_nativeSetPen(JNIEnv* /*env*/, jclass /*cls*/, jlong handle, jint argb, jdouble width, jint style) {
    QPainter* p = reinterpret_cast<QPainter*>(handle);
    if (p == nullptr) return;
    p->setPen(QPen(QColor::fromRgba(static_cast<QRgb>(argb)), width, static_cast<Qt::PenStyle>(style)));
}

static QPainterPath jqtBuildPath(JNIEnv* env, jdoubleArray segments) {
    QPainterPath path;
    if (segments == nullptr) return path;
    jsize n = env->GetArrayLength(segments);
    jdouble* data = env->GetDoubleArrayElements(segments, nullptr);
    for (jsize i = 0; i + 6 < n; i += 7) {
        int type = static_cast<int>(data[i]);
        if (type == 1) path.moveTo(data[i + 1], data[i + 2]);
        else if (type == 2) path.lineTo(data[i + 1], data[i + 2]);
        else path.cubicTo(data[i + 1], data[i + 2], data[i + 3], data[i + 4], data[i + 5], data[i + 6]);
    }
    env->ReleaseDoubleArrayElements(segments, data, JNI_ABORT);
    return path;
}

JNIEXPORT void JNICALL Java_org_jqt_QPainter_nativeDrawPath(JNIEnv* env, jclass /*cls*/, jlong handle, jdoubleArray segments) {
    QPainter* p = reinterpret_cast<QPainter*>(handle);
    if (p == nullptr) return;
    p->drawPath(jqtBuildPath(env, segments));
}

JNIEXPORT void JNICALL Java_org_jqt_QPainter_nativeDrawPixmap(JNIEnv* /*env*/, jclass /*cls*/, jlong handle, jdouble x, jdouble y, jlong pmHandle) {
    QPainter* p = reinterpret_cast<QPainter*>(handle);
    QPixmap* pm = reinterpret_cast<QPixmap*>(pmHandle);
    if (p == nullptr || pm == nullptr || pm->isNull()) return;
    p->drawPixmap(QPointF(x, y), *pm);
}

JNIEXPORT void JNICALL Java_org_jqt_QPainter_nativeDrawPixmapRect(JNIEnv* /*env*/, jclass /*cls*/, jlong handle, jdouble tx, jdouble ty, jdouble tw, jdouble th, jlong pmHandle, jdouble sx, jdouble sy, jdouble sw, jdouble sh) {
    QPainter* p = reinterpret_cast<QPainter*>(handle);
    QPixmap* pm = reinterpret_cast<QPixmap*>(pmHandle);
    if (p == nullptr || pm == nullptr || pm->isNull()) return;
    p->drawPixmap(QRectF(tx, ty, tw, th), *pm, QRectF(sx, sy, sw, sh));
}

JNIEXPORT void JNICALL Java_org_jqt_QPainter_nativeDrawImage(JNIEnv* /*env*/, jclass /*cls*/, jlong handle, jdouble x, jdouble y, jlong imgHandle) {
    QPainter* p = reinterpret_cast<QPainter*>(handle);
    QImage* img = reinterpret_cast<QImage*>(imgHandle);
    if (p == nullptr || img == nullptr || img->isNull()) return;
    p->drawImage(QPointF(x, y), *img);
}

JNIEXPORT void JNICALL Java_org_jqt_QPainter_nativeDrawImageRect(JNIEnv* /*env*/, jclass /*cls*/, jlong handle, jdouble tx, jdouble ty, jdouble tw, jdouble th, jlong imgHandle, jdouble sx, jdouble sy, jdouble sw, jdouble sh) {
    QPainter* p = reinterpret_cast<QPainter*>(handle);
    QImage* img = reinterpret_cast<QImage*>(imgHandle);
    if (p == nullptr || img == nullptr || img->isNull()) return;
    p->drawImage(QRectF(tx, ty, tw, th), *img, QRectF(sx, sy, sw, sh));
}

JNIEXPORT void JNICALL Java_org_jqt_QPainter_nativeDrawTiledPixmap(JNIEnv* /*env*/, jclass /*cls*/, jlong handle, jdouble rx, jdouble ry, jdouble rw, jdouble rh, jlong pmHandle, jdouble ox, jdouble oy) {
    QPainter* p = reinterpret_cast<QPainter*>(handle);
    QPixmap* pm = reinterpret_cast<QPixmap*>(pmHandle);
    if (p == nullptr || pm == nullptr || pm->isNull()) return;
    p->drawTiledPixmap(QRectF(rx, ry, rw, rh), *pm, QPointF(ox, oy));
}

JNIEXPORT void JNICALL Java_org_jqt_QPainter_nativeDrawPicture(JNIEnv* /*env*/, jclass /*cls*/, jlong handle, jdouble x, jdouble y, jdouble w, jdouble h) {
    QPainter* p = reinterpret_cast<QPainter*>(handle);
    if (p == nullptr) return;
    // 简化占位：绘制边框提示播放区域
    p->drawRect(QRectF(x, y, w, h));
}

JNIEXPORT void JNICALL Java_org_jqt_QPainter_nativeDrawGlyphRun(JNIEnv* /*env*/, jclass /*cls*/, jlong handle, jdouble x, jdouble y) {
    QPainter* p = reinterpret_cast<QPainter*>(handle);
    if (p == nullptr) return;
    p->drawPoint(QPointF(x, y));  // 简化占位
}

JNIEXPORT void JNICALL Java_org_jqt_QPainter_nativeFillPath(JNIEnv* env, jclass /*cls*/, jlong handle, jdoubleArray segments, jint argb, jint style) {
    QPainter* p = reinterpret_cast<QPainter*>(handle);
    if (p == nullptr) return;
    QPainterPath path = jqtBuildPath(env, segments);
    p->fillPath(path, QBrush(QColor::fromRgba(static_cast<QRgb>(argb)), static_cast<Qt::BrushStyle>(style)));
}

JNIEXPORT void JNICALL Java_org_jqt_QPainter_nativeStrokePath(JNIEnv* env, jclass /*cls*/, jlong handle, jdoubleArray segments, jint argb, jdouble width, jint style) {
    QPainter* p = reinterpret_cast<QPainter*>(handle);
    if (p == nullptr) return;
    QPainterPath path = jqtBuildPath(env, segments);
    p->strokePath(path, QPen(QColor::fromRgba(static_cast<QRgb>(argb)), width, static_cast<Qt::PenStyle>(style)));
}

JNIEXPORT void JNICALL Java_org_jqt_QPainter_nativeSetClipPath(JNIEnv* env, jclass /*cls*/, jlong handle, jdoubleArray segments) {
    QPainter* p = reinterpret_cast<QPainter*>(handle);
    if (p == nullptr) return;
    p->setClipPath(jqtBuildPath(env, segments));
}

JNIEXPORT void JNICALL Java_org_jqt_QPainter_nativeSetClipRect(JNIEnv* /*env*/, jclass /*cls*/, jlong handle, jdouble x, jdouble y, jdouble w, jdouble h) {
    QPainter* p = reinterpret_cast<QPainter*>(handle);
    if (p == nullptr) return;
    p->setClipRect(QRectF(x, y, w, h));
}

JNIEXPORT void JNICALL Java_org_jqt_QPainter_nativeSetWorldTransform(JNIEnv* /*env*/, jclass /*cls*/, jlong handle, jdouble m11, jdouble m12, jdouble m13, jdouble m21, jdouble m22, jdouble m23, jdouble m31, jdouble m32, jdouble m33) {
    QPainter* p = reinterpret_cast<QPainter*>(handle);
    if (p == nullptr) return;
    p->setWorldTransform(QTransform(m11, m12, m13, m21, m22, m23, m31, m32, m33));
}


// ----------------------------------------------------------------------------
// JQtWidget L1 基础 API（v0.6.0 补全）
// ----------------------------------------------------------------------------
JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeClose(JNIEnv* env, jclass /*cls*/, jlong handle) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (w != nullptr) { w->close(); }
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeMove(JNIEnv* env, jclass /*cls*/, jlong handle, jint x, jint y) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (w != nullptr) { w->move(x, y); }
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeResize(JNIEnv* env, jclass /*cls*/, jlong handle, jint width, jint height) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (w != nullptr) { w->resize(width, height); }
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeUpdate(JNIEnv* env, jclass /*cls*/, jlong handle) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (w != nullptr) { w->update(); }
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeRepaint(JNIEnv* env, jclass /*cls*/, jlong handle) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (w != nullptr) { w->repaint(); }
}

JNIEXPORT jintArray JNICALL Java_org_jqt_QWidget_nativeSize(JNIEnv* env, jclass /*cls*/, jlong handle) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (w == nullptr) { return nullptr; }
    jintArray arr = env->NewIntArray(2);
    jint v[2] = { w->width(), w->height() };
    env->SetIntArrayRegion(arr, 0, 2, v);
    return arr;
}

JNIEXPORT jintArray JNICALL Java_org_jqt_QWidget_nativeGeometry(JNIEnv* env, jclass /*cls*/, jlong handle) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (w == nullptr) { return nullptr; }
    const QRect g = w->geometry();
    jintArray arr = env->NewIntArray(4);
    jint v[4] = { g.x(), g.y(), g.width(), g.height() };
    env->SetIntArrayRegion(arr, 0, 4, v);
    return arr;
}

JNIEXPORT jintArray JNICALL Java_org_jqt_QWidget_nativeContentsMargins(JNIEnv* env, jclass /*cls*/, jlong handle) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (w == nullptr) { return nullptr; }
    const QMargins m = w->contentsMargins();
    jintArray arr = env->NewIntArray(4);
    jint v[4] = { m.left(), m.top(), m.right(), m.bottom() };
    env->SetIntArrayRegion(arr, 0, 4, v);
    return arr;
}

JNIEXPORT jstring JNICALL Java_org_jqt_QWidget_nativeStyleSheet(JNIEnv* env, jclass /*cls*/, jlong handle) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (w == nullptr) { return nullptr; }
    return env->NewStringUTF(w->styleSheet().toUtf8().constData());
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetToolTip(JNIEnv* env, jclass /*cls*/, jlong handle, jstring tip) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (w == nullptr) { return; }
    const char* utf = env->GetStringUTFChars(tip, nullptr);
    w->setToolTip(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(tip, utf);
}

JNIEXPORT jstring JNICALL Java_org_jqt_QWidget_nativeToolTip(JNIEnv* env, jclass /*cls*/, jlong handle) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (w == nullptr) { return nullptr; }
    return env->NewStringUTF(w->toolTip().toUtf8().constData());
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetWindowTitle(JNIEnv* env, jclass /*cls*/, jlong handle, jstring title) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (w == nullptr) { return; }
    const char* utf = env->GetStringUTFChars(title, nullptr);
    w->setWindowTitle(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(title, utf);
}

JNIEXPORT jstring JNICALL Java_org_jqt_QWidget_nativeWindowTitle(JNIEnv* env, jclass /*cls*/, jlong handle) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (w == nullptr) { return nullptr; }
    return env->NewStringUTF(w->windowTitle().toUtf8().constData());
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetWindowState(JNIEnv* env, jclass /*cls*/, jlong handle, jint state) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (w != nullptr) { w->setWindowState(static_cast<Qt::WindowState>(state)); }
}

JNIEXPORT jint JNICALL Java_org_jqt_QWidget_nativeWindowState(JNIEnv* env, jclass /*cls*/, jlong handle) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    return w != nullptr ? static_cast<jint>(w->windowState()) : 0;
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetFocusPolicy(JNIEnv* env, jclass /*cls*/, jlong handle, jint policy) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (w != nullptr) { w->setFocusPolicy(static_cast<Qt::FocusPolicy>(policy)); }
}

JNIEXPORT jint JNICALL Java_org_jqt_QWidget_nativeFocusPolicy(JNIEnv* env, jclass /*cls*/, jlong handle) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    return w != nullptr ? static_cast<jint>(w->focusPolicy()) : 0;
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QWidget_nativeAcceptDrops(JNIEnv* env, jclass /*cls*/, jlong handle) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    return (w != nullptr && w->acceptDrops()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetAcceptDrops(JNIEnv* env, jclass /*cls*/, jlong handle, jboolean on) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (w != nullptr) { w->setAcceptDrops(on == JNI_TRUE); }
}

static Qt::CursorShape jqtCursorShape(const QString& name) {
    if (name == "arrow") return Qt::ArrowCursor;
    if (name == "ibeam") return Qt::IBeamCursor;
    if (name == "wait") return Qt::WaitCursor;
    if (name == "crosshair") return Qt::CrossCursor;
    if (name == "pointinghand") return Qt::PointingHandCursor;
    if (name == "forbidden") return Qt::ForbiddenCursor;
    if (name == "sizeall") return Qt::SizeAllCursor;
    if (name == "sizefdiag") return Qt::SizeFDiagCursor;
    if (name == "sizebdiag") return Qt::SizeBDiagCursor;
    if (name == "sizewe") return Qt::SizeHorCursor;
    if (name == "sizens") return Qt::SizeVerCursor;
    if (name == "splitv") return Qt::SplitVCursor;
    if (name == "splith") return Qt::SplitHCursor;
    if (name == "openhand") return Qt::OpenHandCursor;
    if (name == "closedhand") return Qt::ClosedHandCursor;
    return Qt::ArrowCursor;
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetCursor(JNIEnv* env, jclass /*cls*/, jlong handle, jstring shape) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (w == nullptr) { return; }
    const char* utf = env->GetStringUTFChars(shape, nullptr);
    w->setCursor(jqtCursorShape(QString::fromUtf8(utf)));
    env->ReleaseStringUTFChars(shape, utf);
}

JNIEXPORT jstring JNICALL Java_org_jqt_QWidget_nativeCursor(JNIEnv* env, jclass /*cls*/, jlong handle) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (w == nullptr) { return nullptr; }
    const Qt::CursorShape s = w->cursor().shape();
    const char* name = "arrow";
    switch (s) {
        case Qt::ArrowCursor: name = "arrow"; break;
        case Qt::IBeamCursor: name = "ibeam"; break;
        case Qt::WaitCursor: name = "wait"; break;
        case Qt::CrossCursor: name = "crosshair"; break;
        case Qt::PointingHandCursor: name = "pointinghand"; break;
        case Qt::ForbiddenCursor: name = "forbidden"; break;
        case Qt::SizeAllCursor: name = "sizeall"; break;
        case Qt::SizeFDiagCursor: name = "sizefdiag"; break;
        case Qt::SizeBDiagCursor: name = "sizebdiag"; break;
        case Qt::SizeHorCursor: name = "sizewe"; break;
        case Qt::SizeVerCursor: name = "sizens"; break;
        case Qt::SplitVCursor: name = "splitv"; break;
        case Qt::SplitHCursor: name = "splith"; break;
        case Qt::OpenHandCursor: name = "openhand"; break;
        case Qt::ClosedHandCursor: name = "closedhand"; break;
        default: name = "arrow"; break;
    }
    return env->NewStringUTF(name);
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetFont(JNIEnv* env, jclass /*cls*/, jlong handle, jstring family, jint pointSize) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (w == nullptr) { return; }
    const char* utf = env->GetStringUTFChars(family, nullptr);
    QFont fnt(QString::fromUtf8(utf), pointSize);
    env->ReleaseStringUTFChars(family, utf);
    w->setFont(fnt);
}

JNIEXPORT jstring JNICALL Java_org_jqt_QWidget_nativeFont(JNIEnv* env, jclass /*cls*/, jlong handle) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (w == nullptr) { return nullptr; }
    const QFont fnt = w->font();
    int ps = fnt.pointSize();
    if (ps < 0) { ps = fnt.pixelSize(); }
    return env->NewStringUTF((fnt.family() + QString(",") + QString::number(ps)).toUtf8().constData());
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QWidget_nativeGraphicsEffect(JNIEnv* env, jclass /*cls*/, jlong handle) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    return (w != nullptr && w->graphicsEffect() != nullptr) ? JNI_TRUE : JNI_FALSE;
}

// QWidget 信号（懒连接：注册首个回调时连接；thiz 全局引用供回调）
JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeConnectWindowTitleChanged(JNIEnv* env, jobject thiz, jlong handle) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (w == nullptr) { return; }
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QWidget::windowTitleChanged, [gRef](const QString& title) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleWindowTitleChanged", "(Ljava/lang/String;)V");
        if (mid != nullptr) {
            jstring js = e->NewStringUTF(title.toUtf8().constData());
            JQT_CALL_VOID(e, gRef, mid, js);
            e->DeleteLocalRef(js);
        }
    });
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeConnectContextMenu(JNIEnv* env, jobject thiz, jlong handle) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (w == nullptr) { return; }
    w->setContextMenuPolicy(Qt::CustomContextMenu);   // 启用自定义右键菜单信号
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QWidget::customContextMenuRequested, [gRef](const QPoint& pos) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleCustomContextMenuRequested", "(II)V");
        if (mid != nullptr) {
            JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(pos.x()), static_cast<jint>(pos.y()));
        }
    });
}

// ----------------------------------------------------------------------------
// L1 补全批 B：控件类扩展（QLineEdit/QComboBox/QLabel/QListWidget/QProgressBar/
// QTabWidget/QMenu/QToolBar/QPushButton）
// ----------------------------------------------------------------------------
// QLineEdit
JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativeClear(JNIEnv* env, jclass, jlong h) { QLineEdit* w = static_cast<QLineEdit*>(requireHandle(env, h)); if (w) w->clear(); }
JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativeCopy(JNIEnv* env, jclass, jlong h) { QLineEdit* w = static_cast<QLineEdit*>(requireHandle(env, h)); if (w) w->copy(); }
JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativeCut(JNIEnv* env, jclass, jlong h) { QLineEdit* w = static_cast<QLineEdit*>(requireHandle(env, h)); if (w) w->cut(); }
JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativePaste(JNIEnv* env, jclass, jlong h) { QLineEdit* w = static_cast<QLineEdit*>(requireHandle(env, h)); if (w) w->paste(); }
JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativeUndo(JNIEnv* env, jclass, jlong h) { QLineEdit* w = static_cast<QLineEdit*>(requireHandle(env, h)); if (w) w->undo(); }
JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativeRedo(JNIEnv* env, jclass, jlong h) { QLineEdit* w = static_cast<QLineEdit*>(requireHandle(env, h)); if (w) w->redo(); }
JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativeSelectAll(JNIEnv* env, jclass, jlong h) { QLineEdit* w = static_cast<QLineEdit*>(requireHandle(env, h)); if (w) w->selectAll(); }
JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativeInsert(JNIEnv* env, jclass, jlong h, jstring text) {
    QLineEdit* w = static_cast<QLineEdit*>(requireHandle(env, h)); if (!w) return;
    const char* u = env->GetStringUTFChars(text, nullptr); w->insert(QString::fromUtf8(u)); env->ReleaseStringUTFChars(text, u);
}
JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativeSetEchoMode(JNIEnv* env, jclass, jlong h, jint mode) { QLineEdit* w = static_cast<QLineEdit*>(requireHandle(env, h)); if (w) w->setEchoMode(static_cast<QLineEdit::EchoMode>(mode)); }
JNIEXPORT jint JNICALL Java_org_jqt_QLineEdit_nativeEchoMode(JNIEnv* env, jclass, jlong h) { QLineEdit* w = static_cast<QLineEdit*>(requireHandle(env, h)); return w ? static_cast<jint>(w->echoMode()) : 0; }
JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativeSetMaxLength(JNIEnv* env, jclass, jlong h, jint max) { QLineEdit* w = static_cast<QLineEdit*>(requireHandle(env, h)); if (w) w->setMaxLength(max); }
JNIEXPORT jint JNICALL Java_org_jqt_QLineEdit_nativeMaxLength(JNIEnv* env, jclass, jlong h) { QLineEdit* w = static_cast<QLineEdit*>(requireHandle(env, h)); return w ? static_cast<jint>(w->maxLength()) : 0; }
JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativeSetAlignment(JNIEnv* env, jclass, jlong h, jint a) { QLineEdit* w = static_cast<QLineEdit*>(requireHandle(env, h)); if (w) w->setAlignment(static_cast<Qt::Alignment>(a)); }
JNIEXPORT jint JNICALL Java_org_jqt_QLineEdit_nativeAlignment(JNIEnv* env, jclass, jlong h) { QLineEdit* w = static_cast<QLineEdit*>(requireHandle(env, h)); return w ? static_cast<jint>(w->alignment()) : 0; }
JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativeSetReadOnly(JNIEnv* env, jclass, jlong h, jboolean ro) { QLineEdit* w = static_cast<QLineEdit*>(requireHandle(env, h)); if (w) w->setReadOnly(ro == JNI_TRUE); }
JNIEXPORT jboolean JNICALL Java_org_jqt_QLineEdit_nativeIsReadOnly(JNIEnv* env, jclass, jlong h) { QLineEdit* w = static_cast<QLineEdit*>(requireHandle(env, h)); return (w && w->isReadOnly()) ? JNI_TRUE : JNI_FALSE; }

JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativeConnectEditingFinished(JNIEnv* env, jobject thiz, jlong h) {
    QLineEdit* w = static_cast<QLineEdit*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QLineEdit::editingFinished, [gRef]() {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleEditingFinished", "()V");
        if (mid) JQT_CALL_VOID(e, gRef, mid);
    });
}
JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativeConnectTextEdited(JNIEnv* env, jobject thiz, jlong h) {
    QLineEdit* w = static_cast<QLineEdit*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QLineEdit::textEdited, [gRef](const QString& text) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleTextEdited", "(Ljava/lang/String;)V");
        if (mid) { jstring js = e->NewStringUTF(text.toUtf8().constData()); JQT_CALL_VOID(e, gRef, mid, js); e->DeleteLocalRef(js); }
    });
}

// QComboBox
JNIEXPORT void JNICALL Java_org_jqt_QComboBox_nativeClear(JNIEnv* env, jclass, jlong h) { QComboBox* w = static_cast<QComboBox*>(requireHandle(env, h)); if (w) w->clear(); }
JNIEXPORT jint JNICALL Java_org_jqt_QComboBox_nativeCount(JNIEnv* env, jclass, jlong h) { QComboBox* w = static_cast<QComboBox*>(requireHandle(env, h)); return w ? static_cast<jint>(w->count()) : 0; }
JNIEXPORT void JNICALL Java_org_jqt_QComboBox_nativeSetPlaceholderText(JNIEnv* env, jclass, jlong h, jstring t) {
    QComboBox* w = static_cast<QComboBox*>(requireHandle(env, h)); if (!w) return;
    const char* u = env->GetStringUTFChars(t, nullptr); w->setPlaceholderText(QString::fromUtf8(u)); env->ReleaseStringUTFChars(t, u);
}
JNIEXPORT jstring JNICALL Java_org_jqt_QComboBox_nativePlaceholderText(JNIEnv* env, jclass, jlong h) {
    QComboBox* w = static_cast<QComboBox*>(requireHandle(env, h));
    return w ? env->NewStringUTF(w->placeholderText().toUtf8().constData()) : nullptr;
}
JNIEXPORT void JNICALL Java_org_jqt_QComboBox_nativeSetEditable(JNIEnv* env, jclass, jlong h, jboolean ed) { QComboBox* w = static_cast<QComboBox*>(requireHandle(env, h)); if (w) w->setEditable(ed == JNI_TRUE); }
JNIEXPORT jboolean JNICALL Java_org_jqt_QComboBox_nativeIsEditable(JNIEnv* env, jclass, jlong h) { QComboBox* w = static_cast<QComboBox*>(requireHandle(env, h)); return (w && w->isEditable()) ? JNI_TRUE : JNI_FALSE; }

JNIEXPORT void JNICALL Java_org_jqt_QComboBox_nativeConnectActivated(JNIEnv* env, jobject thiz, jlong h) {
    QComboBox* w = static_cast<QComboBox*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, QOverload<int>::of(&QComboBox::activated), [gRef](int index) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleActivated", "(I)V");
        if (mid) JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(index));
    });
}
JNIEXPORT void JNICALL Java_org_jqt_QComboBox_nativeConnectCurrentTextChanged(JNIEnv* env, jobject thiz, jlong h) {
    QComboBox* w = static_cast<QComboBox*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QComboBox::currentTextChanged, [gRef](const QString& text) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleCurrentTextChanged", "(Ljava/lang/String;)V");
        if (mid) { jstring js = e->NewStringUTF(text.toUtf8().constData()); JQT_CALL_VOID(e, gRef, mid, js); e->DeleteLocalRef(js); }
    });
}

// QLabel
JNIEXPORT void JNICALL Java_org_jqt_QLabel_nativeClear(JNIEnv* env, jclass, jlong h) { QLabel* w = static_cast<QLabel*>(requireHandle(env, h)); if (w) w->clear(); }
JNIEXPORT void JNICALL Java_org_jqt_QLabel_nativeSetAlignment(JNIEnv* env, jclass, jlong h, jint a) { QLabel* w = static_cast<QLabel*>(requireHandle(env, h)); if (w) w->setAlignment(static_cast<Qt::Alignment>(a)); }
JNIEXPORT jint JNICALL Java_org_jqt_QLabel_nativeAlignment(JNIEnv* env, jclass, jlong h) { QLabel* w = static_cast<QLabel*>(requireHandle(env, h)); return w ? static_cast<jint>(w->alignment()) : 0; }
JNIEXPORT void JNICALL Java_org_jqt_QLabel_nativeSetWordWrap(JNIEnv* env, jclass, jlong h, jboolean ww) { QLabel* w = static_cast<QLabel*>(requireHandle(env, h)); if (w) w->setWordWrap(ww == JNI_TRUE); }
JNIEXPORT jboolean JNICALL Java_org_jqt_QLabel_nativeWordWrap(JNIEnv* env, jclass, jlong h) { QLabel* w = static_cast<QLabel*>(requireHandle(env, h)); return (w && w->wordWrap()) ? JNI_TRUE : JNI_FALSE; }
JNIEXPORT void JNICALL Java_org_jqt_QLabel_nativeSetMargin(JNIEnv* env, jclass, jlong h, jint m) { QLabel* w = static_cast<QLabel*>(requireHandle(env, h)); if (w) w->setMargin(m); }
JNIEXPORT jint JNICALL Java_org_jqt_QLabel_nativeMargin(JNIEnv* env, jclass, jlong h) { QLabel* w = static_cast<QLabel*>(requireHandle(env, h)); return w ? static_cast<jint>(w->margin()) : 0; }
JNIEXPORT void JNICALL Java_org_jqt_QLabel_nativeSetIndent(JNIEnv* env, jclass, jlong h, jint in) { QLabel* w = static_cast<QLabel*>(requireHandle(env, h)); if (w) w->setIndent(in); }
JNIEXPORT jint JNICALL Java_org_jqt_QLabel_nativeIndent(JNIEnv* env, jclass, jlong h) { QLabel* w = static_cast<QLabel*>(requireHandle(env, h)); return w ? static_cast<jint>(w->indent()) : 0; }

JNIEXPORT void JNICALL Java_org_jqt_QLabel_nativeConnectLinkActivated(JNIEnv* env, jobject thiz, jlong h) {
    QLabel* w = static_cast<QLabel*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QLabel::linkActivated, [gRef](const QString& url) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleLinkActivated", "(Ljava/lang/String;)V");
        if (mid) { jstring js = e->NewStringUTF(url.toUtf8().constData()); JQT_CALL_VOID(e, gRef, mid, js); e->DeleteLocalRef(js); }
    });
}

// QListWidget
JNIEXPORT void JNICALL Java_org_jqt_QListWidget_nativeClear(JNIEnv* env, jclass, jlong h) { QListWidget* w = static_cast<QListWidget*>(requireHandle(env, h)); if (w) w->clear(); }
JNIEXPORT jint JNICALL Java_org_jqt_QListWidget_nativeCount(JNIEnv* env, jclass, jlong h) { QListWidget* w = static_cast<QListWidget*>(requireHandle(env, h)); return w ? static_cast<jint>(w->count()) : 0; }
JNIEXPORT jstring JNICALL Java_org_jqt_QListWidget_nativeItem(JNIEnv* env, jclass, jlong h, jint row) {
    QListWidget* w = static_cast<QListWidget*>(requireHandle(env, h));
    if (!w) return nullptr;
    QListWidgetItem* it = w->item(row);
    return it ? env->NewStringUTF(it->text().toUtf8().constData()) : nullptr;
}

// QProgressBar
JNIEXPORT void JNICALL Java_org_jqt_QProgressBar_nativeSetAlignment(JNIEnv* env, jclass, jlong h, jint a) { QProgressBar* w = static_cast<QProgressBar*>(requireHandle(env, h)); if (w) w->setAlignment(static_cast<Qt::Alignment>(a)); }
JNIEXPORT jint JNICALL Java_org_jqt_QProgressBar_nativeAlignment(JNIEnv* env, jclass, jlong h) { QProgressBar* w = static_cast<QProgressBar*>(requireHandle(env, h)); return w ? static_cast<jint>(w->alignment()) : 0; }
JNIEXPORT jstring JNICALL Java_org_jqt_QProgressBar_nativeText(JNIEnv* env, jclass, jlong h) {
    QProgressBar* w = static_cast<QProgressBar*>(requireHandle(env, h));
    return w ? env->NewStringUTF(w->text().toUtf8().constData()) : nullptr;
}

// QTabWidget
JNIEXPORT void JNICALL Java_org_jqt_QTabWidget_nativeClear(JNIEnv* env, jclass, jlong h) { QTabWidget* w = static_cast<QTabWidget*>(requireHandle(env, h)); if (w) w->clear(); }
JNIEXPORT jint JNICALL Java_org_jqt_QTabWidget_nativeCount(JNIEnv* env, jclass, jlong h) { QTabWidget* w = static_cast<QTabWidget*>(requireHandle(env, h)); return w ? static_cast<jint>(w->count()) : 0; }

// QMenu
JNIEXPORT void JNICALL Java_org_jqt_QMenu_nativeClear(JNIEnv* env, jclass, jlong h) { QMenu* w = static_cast<QMenu*>(requireHandle(env, h)); if (w) w->clear(); }
JNIEXPORT jstring JNICALL Java_org_jqt_QMenu_nativeTitle(JNIEnv* env, jclass, jlong h) {
    QMenu* w = static_cast<QMenu*>(requireHandle(env, h));
    return w ? env->NewStringUTF(w->title().toUtf8().constData()) : nullptr;
}
JNIEXPORT void JNICALL Java_org_jqt_QMenu_nativeSetTitle(JNIEnv* env, jclass, jlong h, jstring t) {
    QMenu* w = static_cast<QMenu*>(requireHandle(env, h)); if (!w) return;
    const char* u = env->GetStringUTFChars(t, nullptr); w->setTitle(QString::fromUtf8(u)); env->ReleaseStringUTFChars(t, u);
}

// QToolBar
JNIEXPORT void JNICALL Java_org_jqt_QToolBar_nativeClear(JNIEnv* env, jclass, jlong h) { QToolBar* w = static_cast<QToolBar*>(requireHandle(env, h)); if (w) w->clear(); }

// QPushButton（QAbstractButton）
JNIEXPORT void JNICALL Java_org_jqt_QPushButton_nativeClick(JNIEnv* env, jclass, jlong h) { QPushButton* w = static_cast<QPushButton*>(requireHandle(env, h)); if (w) w->click(); }
JNIEXPORT void JNICALL Java_org_jqt_QPushButton_nativeToggle(JNIEnv* env, jclass, jlong h) { QPushButton* w = static_cast<QPushButton*>(requireHandle(env, h)); if (w) w->toggle(); }
JNIEXPORT void JNICALL Java_org_jqt_QPushButton_nativeSetCheckable(JNIEnv* env, jclass, jlong h, jboolean ck) { QPushButton* w = static_cast<QPushButton*>(requireHandle(env, h)); if (w) w->setCheckable(ck == JNI_TRUE); }

JNIEXPORT jboolean JNICALL Java_org_jqt_QPushButton_nativeIsChecked(JNIEnv* env, jclass, jlong h) {
    QPushButton* w = static_cast<QPushButton*>(requireHandle(env, h));
    return (w != nullptr && w->isChecked()) ? JNI_TRUE : JNI_FALSE;
}

// L1 补全批 C：QTextEdit/QTreeWidget/QListWidget/QScrollArea/QSplitter/QApplication
// QTextEdit
JNIEXPORT void JNICALL Java_org_jqt_QTextEdit_nativeClear(JNIEnv* env, jclass, jlong h) { QPlainTextEdit* w = static_cast<QPlainTextEdit*>(requireHandle(env, h)); if (w) w->clear(); }
JNIEXPORT void JNICALL Java_org_jqt_QTextEdit_nativeCopy(JNIEnv* env, jclass, jlong h) { QPlainTextEdit* w = static_cast<QPlainTextEdit*>(requireHandle(env, h)); if (w) w->copy(); }
JNIEXPORT void JNICALL Java_org_jqt_QTextEdit_nativeCut(JNIEnv* env, jclass, jlong h) { QPlainTextEdit* w = static_cast<QPlainTextEdit*>(requireHandle(env, h)); if (w) w->cut(); }
JNIEXPORT void JNICALL Java_org_jqt_QTextEdit_nativePaste(JNIEnv* env, jclass, jlong h) { QPlainTextEdit* w = static_cast<QPlainTextEdit*>(requireHandle(env, h)); if (w) w->paste(); }
JNIEXPORT void JNICALL Java_org_jqt_QTextEdit_nativeUndo(JNIEnv* env, jclass, jlong h) { QPlainTextEdit* w = static_cast<QPlainTextEdit*>(requireHandle(env, h)); if (w) w->undo(); }
JNIEXPORT void JNICALL Java_org_jqt_QTextEdit_nativeRedo(JNIEnv* env, jclass, jlong h) { QPlainTextEdit* w = static_cast<QPlainTextEdit*>(requireHandle(env, h)); if (w) w->redo(); }
JNIEXPORT void JNICALL Java_org_jqt_QTextEdit_nativeSelectAll(JNIEnv* env, jclass, jlong h) { QPlainTextEdit* w = static_cast<QPlainTextEdit*>(requireHandle(env, h)); if (w) w->selectAll(); }

// QTreeWidget
JNIEXPORT jint JNICALL Java_org_jqt_QTreeWidget_nativeCurrentItem(JNIEnv* env, jclass, jlong h) {
    QTreeWidget* w = static_cast<QTreeWidget*>(requireHandle(env, h));
    if (!w) return -1;
    QTreeWidgetItem* it = w->currentItem();
    auto f = it ? g_treeItemIds.find(it) : g_treeItemIds.end();
    return (f != g_treeItemIds.end()) ? static_cast<jint>(f->second) : -1;
}
JNIEXPORT void JNICALL Java_org_jqt_QTreeWidget_nativeConnectCurrentItemChanged(JNIEnv* env, jobject thiz, jlong h) {
    QTreeWidget* w = static_cast<QTreeWidget*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QTreeWidget::currentItemChanged, [gRef](QTreeWidgetItem* cur, QTreeWidgetItem*) {
        JNIEnv* e = callbackEnv();
        auto f = cur ? g_treeItemIds.find(cur) : g_treeItemIds.end();
        jmethodID mid = e->GetMethodID(e->GetObjectClass(gRef), "nativeHandleCurrentItemChanged", "(I)V");
        if (mid && f != g_treeItemIds.end()) JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(f->second));
    });
}
JNIEXPORT void JNICALL Java_org_jqt_QTreeWidget_nativeConnectItemDoubleClicked(JNIEnv* env, jobject thiz, jlong h) {
    QTreeWidget* w = static_cast<QTreeWidget*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QTreeWidget::itemDoubleClicked, [gRef](QTreeWidgetItem* item, int) {
        JNIEnv* e = callbackEnv();
        auto f = item ? g_treeItemIds.find(item) : g_treeItemIds.end();
        jmethodID mid = e->GetMethodID(e->GetObjectClass(gRef), "nativeHandleItemDoubleClicked", "(I)V");
        if (mid && f != g_treeItemIds.end()) JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(f->second));
    });
}
JNIEXPORT void JNICALL Java_org_jqt_QTreeWidget_nativeConnectItemActivated(JNIEnv* env, jobject thiz, jlong h) {
    QTreeWidget* w = static_cast<QTreeWidget*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QTreeWidget::itemActivated, [gRef](QTreeWidgetItem* item, int) {
        JNIEnv* e = callbackEnv();
        auto f = item ? g_treeItemIds.find(item) : g_treeItemIds.end();
        jmethodID mid = e->GetMethodID(e->GetObjectClass(gRef), "nativeHandleItemActivated", "(I)V");
        if (mid && f != g_treeItemIds.end()) JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(f->second));
    });
}

// QListWidget
JNIEXPORT jstring JNICALL Java_org_jqt_QListWidget_nativeCurrentText(JNIEnv* env, jclass, jlong h) {
    QListWidget* w = static_cast<QListWidget*>(requireHandle(env, h));
    if (!w) return nullptr;
    QListWidgetItem* it = w->currentItem();
    return it ? env->NewStringUTF(it->text().toUtf8().constData()) : nullptr;
}
JNIEXPORT void JNICALL Java_org_jqt_QListWidget_nativeConnectItemDoubleClicked(JNIEnv* env, jobject thiz, jlong h) {
    QListWidget* w = static_cast<QListWidget*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QListWidget::itemDoubleClicked, [gRef, w](QListWidgetItem* item) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleItemDoubleClicked", "(I)V");
        if (mid && item) JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(w->row(item)));
    });
}
JNIEXPORT void JNICALL Java_org_jqt_QListWidget_nativeConnectItemActivated(JNIEnv* env, jobject thiz, jlong h) {
    QListWidget* w = static_cast<QListWidget*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QListWidget::itemActivated, [gRef, w](QListWidgetItem* item) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleItemActivated", "(I)V");
        if (mid && item) JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(w->row(item)));
    });
}
JNIEXPORT void JNICALL Java_org_jqt_QListWidget_nativeConnectCurrentTextChanged(JNIEnv* env, jobject thiz, jlong h) {
    QListWidget* w = static_cast<QListWidget*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QListWidget::currentTextChanged, [gRef](const QString& text) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleCurrentTextChanged", "(Ljava/lang/String;)V");
        if (mid) { jstring js = e->NewStringUTF(text.toUtf8().constData()); JQT_CALL_VOID(e, gRef, mid, js); e->DeleteLocalRef(js); }
    });
}

// QScrollArea
JNIEXPORT void JNICALL Java_org_jqt_QScrollArea_nativeSetAlignment(JNIEnv* env, jclass, jlong h, jint a) { QScrollArea* w = static_cast<QScrollArea*>(requireHandle(env, h)); if (w) w->setAlignment(static_cast<Qt::Alignment>(a)); }
JNIEXPORT jint JNICALL Java_org_jqt_QScrollArea_nativeAlignment(JNIEnv* env, jclass, jlong h) { QScrollArea* w = static_cast<QScrollArea*>(requireHandle(env, h)); return w ? static_cast<jint>(w->alignment()) : 0; }

// QSplitter
JNIEXPORT jint JNICALL Java_org_jqt_QSplitter_nativeCount(JNIEnv* env, jclass, jlong h) { QSplitter* w = static_cast<QSplitter*>(requireHandle(env, h)); return w ? static_cast<jint>(w->count()) : 0; }

// QApplication
JNIEXPORT void JNICALL Java_org_jqt_QApplication_nativeBeep(JNIEnv* env, jclass) { QApplication::beep(); }
JNIEXPORT jstring JNICALL Java_org_jqt_QApplication_nativeStyleSheet(JNIEnv* env, jclass) {
    return env->NewStringUTF(qApp->styleSheet().toUtf8().constData());
}

JNIEXPORT void JNICALL Java_org_jqt_QTreeWidget_nativeSetCurrentItem(JNIEnv* env, jclass, jlong h, jint itemId) {
    QTreeWidget* w = static_cast<QTreeWidget*>(requireHandle(env, h));
    if (!w) return;
    auto it = g_treeItems.find(itemId);
    if (it != g_treeItems.end()) w->setCurrentItem(it->second);
}
JNIEXPORT void JNICALL Java_org_jqt_QListWidget_nativeSetCurrentRow(JNIEnv* env, jobject, jlong h, jint row) {
    QListWidget* w = static_cast<QListWidget*>(requireHandle(env, h));
    if (w) w->setCurrentRow(row);
}

// L1 补全批 D：剩余信号 + QMessageBox.about
// QListWidget
JNIEXPORT void JNICALL Java_org_jqt_QListWidget_nativeConnectItemPressed(JNIEnv* env, jobject thiz, jlong h) {
    QListWidget* w = static_cast<QListWidget*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QListWidget::itemPressed, [gRef, w](QListWidgetItem* item) {
        JNIEnv* e = callbackEnv();
        jmethodID mid = e->GetMethodID(e->GetObjectClass(gRef), "nativeHandleItemPressed", "(I)V");
        if (mid && item) JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(w->row(item)));
    });
}
JNIEXPORT void JNICALL Java_org_jqt_QListWidget_nativeConnectItemSelectionChanged(JNIEnv* env, jobject thiz, jlong h) {
    QListWidget* w = static_cast<QListWidget*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QListWidget::itemSelectionChanged, [gRef]() {
        JNIEnv* e = callbackEnv();
        jmethodID mid = e->GetMethodID(e->GetObjectClass(gRef), "nativeHandleItemSelectionChanged", "()V");
        if (mid) JQT_CALL_VOID(e, gRef, mid);
    });
}
// QTreeWidget
JNIEXPORT void JNICALL Java_org_jqt_QTreeWidget_nativeConnectItemChanged(JNIEnv* env, jobject thiz, jlong h) {
    QTreeWidget* w = static_cast<QTreeWidget*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QTreeWidget::itemChanged, [gRef](QTreeWidgetItem* item, int) {
        JNIEnv* e = callbackEnv();
        auto f = item ? g_treeItemIds.find(item) : g_treeItemIds.end();
        jmethodID mid = e->GetMethodID(e->GetObjectClass(gRef), "nativeHandleItemChanged", "(I)V");
        if (mid && f != g_treeItemIds.end()) JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(f->second));
    });
}
JNIEXPORT void JNICALL Java_org_jqt_QTreeWidget_nativeConnectItemPressed(JNIEnv* env, jobject thiz, jlong h) {
    QTreeWidget* w = static_cast<QTreeWidget*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QTreeWidget::itemPressed, [gRef](QTreeWidgetItem* item, int) {
        JNIEnv* e = callbackEnv();
        auto f = item ? g_treeItemIds.find(item) : g_treeItemIds.end();
        jmethodID mid = e->GetMethodID(e->GetObjectClass(gRef), "nativeHandleItemPressed", "(I)V");
        if (mid && f != g_treeItemIds.end()) JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(f->second));
    });
}
// QComboBox
JNIEXPORT void JNICALL Java_org_jqt_QComboBox_nativeConnectEditTextChanged(JNIEnv* env, jobject thiz, jlong h) {
    QComboBox* w = static_cast<QComboBox*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QComboBox::editTextChanged, [gRef](const QString& text) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleEditTextChanged", "(Ljava/lang/String;)V");
        if (mid) { jstring js = e->NewStringUTF(text.toUtf8().constData()); JQT_CALL_VOID(e, gRef, mid, js); e->DeleteLocalRef(js); }
    });
}
JNIEXPORT void JNICALL Java_org_jqt_QComboBox_nativeConnectHighlighted(JNIEnv* env, jobject thiz, jlong h) {
    QComboBox* w = static_cast<QComboBox*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, QOverload<int>::of(&QComboBox::highlighted), [gRef](int index) {
        JNIEnv* e = callbackEnv();
        jmethodID mid = e->GetMethodID(e->GetObjectClass(gRef), "nativeHandleHighlighted", "(I)V");
        if (mid) JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(index));
    });
}
// QLineEdit
JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativeConnectSelectionChanged(JNIEnv* env, jobject thiz, jlong h) {
    QLineEdit* w = static_cast<QLineEdit*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QLineEdit::selectionChanged, [gRef]() {
        JNIEnv* e = callbackEnv();
        jmethodID mid = e->GetMethodID(e->GetObjectClass(gRef), "nativeHandleSelectionChanged", "()V");
        if (mid) JQT_CALL_VOID(e, gRef, mid);
    });
}
JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativeConnectCursorPositionChanged(JNIEnv* env, jobject thiz, jlong h) {
    QLineEdit* w = static_cast<QLineEdit*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QLineEdit::cursorPositionChanged, [gRef](int oldPos, int newPos) {
        JNIEnv* e = callbackEnv();
        jmethodID mid = e->GetMethodID(e->GetObjectClass(gRef), "nativeHandleCursorPositionChanged", "(I)V");
        if (mid) JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(newPos));
    });
}
// QMessageBox about
JNIEXPORT void JNICALL Java_org_jqt_QMessageBox_nativeShowAbout(JNIEnv* env, jclass, jlong winHandle, jstring title, jstring text) {
    QWidget* parent = static_cast<QWidget*>(requireHandle(env, winHandle));
    const char* t1 = env->GetStringUTFChars(title, nullptr);
    const char* t2 = env->GetStringUTFChars(text, nullptr);
    QMessageBox box(parent);
    box.setWindowTitle(QString::fromUtf8(t1));
    box.setText(QString::fromUtf8(t2));
    box.setStandardButtons(QMessageBox::Ok);
    env->ReleaseStringUTFChars(title, t1);
    env->ReleaseStringUTFChars(text, t2);
    box.exec();
}

// L1 补全批 E：QCheckBox 三态 / QSlider 刻度 / QSpinBox 完整 / QLabel buddy / QApplication alert / QTextEdit find
// QCheckBox
JNIEXPORT void JNICALL Java_org_jqt_QCheckBox_nativeSetTristate(JNIEnv* env, jclass, jlong h, jboolean t) { QCheckBox* w = static_cast<QCheckBox*>(requireHandle(env, h)); if (w) w->setTristate(t == JNI_TRUE); }
JNIEXPORT jboolean JNICALL Java_org_jqt_QCheckBox_nativeIsTristate(JNIEnv* env, jclass, jlong h) { QCheckBox* w = static_cast<QCheckBox*>(requireHandle(env, h)); return (w && w->isTristate()) ? JNI_TRUE : JNI_FALSE; }
JNIEXPORT jint JNICALL Java_org_jqt_QCheckBox_nativeCheckState(JNIEnv* env, jclass, jlong h) { QCheckBox* w = static_cast<QCheckBox*>(requireHandle(env, h)); return w ? static_cast<jint>(w->checkState()) : 0; }
JNIEXPORT void JNICALL Java_org_jqt_QCheckBox_nativeSetCheckState(JNIEnv* env, jclass, jlong h, jint s) { QCheckBox* w = static_cast<QCheckBox*>(requireHandle(env, h)); if (w) w->setCheckState(static_cast<Qt::CheckState>(s)); }
JNIEXPORT void JNICALL Java_org_jqt_QCheckBox_nativeConnectCheckStateChanged(JNIEnv* env, jobject thiz, jlong h) {
    QCheckBox* w = static_cast<QCheckBox*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QCheckBox::checkStateChanged, [gRef](Qt::CheckState s) {
        JNIEnv* e = callbackEnv();
        jmethodID mid = e->GetMethodID(e->GetObjectClass(gRef), "nativeHandleCheckStateChanged", "(I)V");
        if (mid) JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(s));
    });
}
// QSlider
JNIEXPORT void JNICALL Java_org_jqt_QSlider_nativeSetTickInterval(JNIEnv* env, jclass, jlong h, jint i) { JQtSliderWidget* w = static_cast<JQtSliderWidget*>(requireHandle(env, h)); if (w) w->setTickInterval(i); }
JNIEXPORT jint JNICALL Java_org_jqt_QSlider_nativeTickInterval(JNIEnv* env, jclass, jlong h) { JQtSliderWidget* w = static_cast<JQtSliderWidget*>(requireHandle(env, h)); return w ? static_cast<jint>(w->tickInterval()) : 0; }
JNIEXPORT void JNICALL Java_org_jqt_QSlider_nativeSetTickPosition(JNIEnv* env, jclass, jlong h, jint p) { JQtSliderWidget* w = static_cast<JQtSliderWidget*>(requireHandle(env, h)); if (w) w->setTickPosition(p); }
JNIEXPORT jint JNICALL Java_org_jqt_QSlider_nativeTickPosition(JNIEnv* env, jclass, jlong h) { JQtSliderWidget* w = static_cast<JQtSliderWidget*>(requireHandle(env, h)); return w ? static_cast<jint>(w->tickPosition()) : 0; }
// QSpinBox
JNIEXPORT void JNICALL Java_org_jqt_QSpinBox_nativeSetPrefix(JNIEnv* env, jclass, jlong h, jstring p) {
    QSpinBox* w = static_cast<QSpinBox*>(requireHandle(env, h)); if (!w) return;
    const char* u = env->GetStringUTFChars(p, nullptr); w->setPrefix(QString::fromUtf8(u)); env->ReleaseStringUTFChars(p, u);
}
JNIEXPORT jstring JNICALL Java_org_jqt_QSpinBox_nativePrefix(JNIEnv* env, jclass, jlong h) { QSpinBox* w = static_cast<QSpinBox*>(requireHandle(env, h)); return w ? env->NewStringUTF(w->prefix().toUtf8().constData()) : nullptr; }
JNIEXPORT void JNICALL Java_org_jqt_QSpinBox_nativeSetSuffix(JNIEnv* env, jclass, jlong h, jstring s) {
    QSpinBox* w = static_cast<QSpinBox*>(requireHandle(env, h)); if (!w) return;
    const char* u = env->GetStringUTFChars(s, nullptr); w->setSuffix(QString::fromUtf8(u)); env->ReleaseStringUTFChars(s, u);
}
JNIEXPORT jstring JNICALL Java_org_jqt_QSpinBox_nativeSuffix(JNIEnv* env, jclass, jlong h) { QSpinBox* w = static_cast<QSpinBox*>(requireHandle(env, h)); return w ? env->NewStringUTF(w->suffix().toUtf8().constData()) : nullptr; }
JNIEXPORT void JNICALL Java_org_jqt_QSpinBox_nativeSetSingleStep(JNIEnv* env, jclass, jlong h, jint s) { QSpinBox* w = static_cast<QSpinBox*>(requireHandle(env, h)); if (w) w->setSingleStep(s); }
JNIEXPORT jint JNICALL Java_org_jqt_QSpinBox_nativeSingleStep(JNIEnv* env, jclass, jlong h) { QSpinBox* w = static_cast<QSpinBox*>(requireHandle(env, h)); return w ? static_cast<jint>(w->singleStep()) : 0; }
JNIEXPORT jint JNICALL Java_org_jqt_QSpinBox_nativeMinimum(JNIEnv* env, jclass, jlong h) { QSpinBox* w = static_cast<QSpinBox*>(requireHandle(env, h)); return w ? static_cast<jint>(w->minimum()) : 0; }
JNIEXPORT jint JNICALL Java_org_jqt_QSpinBox_nativeMaximum(JNIEnv* env, jclass, jlong h) { QSpinBox* w = static_cast<QSpinBox*>(requireHandle(env, h)); return w ? static_cast<jint>(w->maximum()) : 0; }
JNIEXPORT void JNICALL Java_org_jqt_QSpinBox_nativeSetMinimum(JNIEnv* env, jclass, jlong h, jint m) { QSpinBox* w = static_cast<QSpinBox*>(requireHandle(env, h)); if (w) w->setMinimum(m); }
JNIEXPORT void JNICALL Java_org_jqt_QSpinBox_nativeSetMaximum(JNIEnv* env, jclass, jlong h, jint m) { QSpinBox* w = static_cast<QSpinBox*>(requireHandle(env, h)); if (w) w->setMaximum(m); }
JNIEXPORT jstring JNICALL Java_org_jqt_QSpinBox_nativeCleanText(JNIEnv* env, jclass, jlong h) { QSpinBox* w = static_cast<QSpinBox*>(requireHandle(env, h)); return w ? env->NewStringUTF(w->cleanText().toUtf8().constData()) : nullptr; }
// QLabel buddy
JNIEXPORT void JNICALL Java_org_jqt_QLabel_nativeSetBuddy(JNIEnv* env, jclass, jlong h, jlong bh) {
    QLabel* w = static_cast<QLabel*>(requireHandle(env, h));
    QWidget* b = static_cast<QWidget*>(requireHandle(env, bh));
    if (w && b) w->setBuddy(b);
}
// QApplication alert
JNIEXPORT void JNICALL Java_org_jqt_QApplication_nativeAlert(JNIEnv* env, jclass, jlong winHandle, jint ms) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, winHandle));
    if (w) QApplication::alert(w, ms);
}
// QTextEdit find
JNIEXPORT jboolean JNICALL Java_org_jqt_QTextEdit_nativeFind(JNIEnv* env, jclass, jlong h, jstring text) {
    QPlainTextEdit* w = static_cast<QPlainTextEdit*>(requireHandle(env, h));
    if (!w) return JNI_FALSE;
    const char* u = env->GetStringUTFChars(text, nullptr);
    bool ok = w->find(QString::fromUtf8(u));
    env->ReleaseStringUTFChars(text, u);
    return ok ? JNI_TRUE : JNI_FALSE;
}

// L1 补全批 F：剩余信号
// QToolBar
JNIEXPORT void JNICALL Java_org_jqt_QToolBar_nativeConnectIconSizeChanged(JNIEnv* env, jobject thiz, jlong h) {
    QToolBar* w = static_cast<QToolBar*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QToolBar::iconSizeChanged, [gRef](const QSize& s) {
        JNIEnv* e = callbackEnv();
        jmethodID mid = e->GetMethodID(e->GetObjectClass(gRef), "nativeHandleIconSizeChanged", "(I)V");
        if (mid) JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(s.width()));
    });
}
JNIEXPORT void JNICALL Java_org_jqt_QToolBar_nativeConnectToolButtonStyleChanged(JNIEnv* env, jobject thiz, jlong h) {
    QToolBar* w = static_cast<QToolBar*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QToolBar::toolButtonStyleChanged, [gRef](Qt::ToolButtonStyle s) {
        JNIEnv* e = callbackEnv();
        jmethodID mid = e->GetMethodID(e->GetObjectClass(gRef), "nativeHandleToolButtonStyleChanged", "(I)V");
        if (mid) JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(s));
    });
}
// QTextEdit
JNIEXPORT void JNICALL Java_org_jqt_QTextEdit_nativeConnectSelectionChanged(JNIEnv* env, jobject thiz, jlong h) {
    QPlainTextEdit* w = static_cast<QPlainTextEdit*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QPlainTextEdit::selectionChanged, [gRef]() {
        JNIEnv* e = callbackEnv();
        jmethodID mid = e->GetMethodID(e->GetObjectClass(gRef), "nativeHandleSelectionChanged", "()V");
        if (mid) JQT_CALL_VOID(e, gRef, mid);
    });
}
JNIEXPORT void JNICALL Java_org_jqt_QTextEdit_nativeConnectCursorPositionChanged(JNIEnv* env, jobject thiz, jlong h) {
    QPlainTextEdit* w = static_cast<QPlainTextEdit*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QPlainTextEdit::cursorPositionChanged, [gRef, w]() {
        JNIEnv* e = callbackEnv();
        jmethodID mid = e->GetMethodID(e->GetObjectClass(gRef), "nativeHandleCursorPositionChanged", "(I)V");
        if (mid) JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(w->textCursor().position()));
    });
}
// QTreeWidget itemEntered
JNIEXPORT void JNICALL Java_org_jqt_QTreeWidget_nativeConnectItemEntered(JNIEnv* env, jobject thiz, jlong h) {
    QTreeWidget* w = static_cast<QTreeWidget*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QTreeWidget::itemEntered, [gRef](QTreeWidgetItem* item, int) {
        JNIEnv* e = callbackEnv();
        auto f = item ? g_treeItemIds.find(item) : g_treeItemIds.end();
        jmethodID mid = e->GetMethodID(e->GetObjectClass(gRef), "nativeHandleItemEntered", "(I)V");
        if (mid && f != g_treeItemIds.end()) JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(f->second));
    });
}
// QListWidget currentItem
JNIEXPORT jint JNICALL Java_org_jqt_QListWidget_nativeCurrentItem(JNIEnv* env, jclass, jlong h) {
    QListWidget* w = static_cast<QListWidget*>(requireHandle(env, h));
    return w ? static_cast<jint>(w->currentRow()) : -1;
}
// QMainWindow
// QMainWindow 的 toolbar 信号由 QToolBar 提供（JQtWindowShell 非 QMainWindow 类型，无此信号）

JNIEXPORT void JNICALL Java_org_jqt_QToolBar_nativeSetIconSize(JNIEnv* env, jclass, jlong h, jint s) { QToolBar* w = static_cast<QToolBar*>(requireHandle(env, h)); if (w) w->setIconSize(QSize(s, s)); }
JNIEXPORT jint JNICALL Java_org_jqt_QToolBar_nativeIconSize(JNIEnv* env, jclass, jlong h) { QToolBar* w = static_cast<QToolBar*>(requireHandle(env, h)); return w ? static_cast<jint>(w->iconSize().width()) : 0; }

// L1 补全批 G：QMenu exec / 树列表信号 / QApplication font / QWidget sizePolicy / QClipboard / QSettings
// QMenu exec
JNIEXPORT jint JNICALL Java_org_jqt_QMenu_nativeExec(JNIEnv* env, jclass, jlong h, jint x, jint y) {
    QMenu* w = static_cast<QMenu*>(requireHandle(env, h));
    if (!w) return -1;
    QAction* act = w->exec(QPoint(x, y));
    return act ? static_cast<jint>(act->data().toInt()) : -1;
}
JNIEXPORT jint JNICALL Java_org_jqt_QMenu_nativeExecAnchor(JNIEnv* env, jclass, jlong h, jlong ah) {
    QMenu* w = static_cast<QMenu*>(requireHandle(env, h));
    QWidget* a = static_cast<QWidget*>(requireHandle(env, ah));
    if (!w || !a) return -1;
    QAction* act = w->exec(a->mapToGlobal(QPoint(0, a->height())));
    return act ? static_cast<jint>(act->data().toInt()) : -1;
}
// QTreeWidget itemSelectionChanged
JNIEXPORT void JNICALL Java_org_jqt_QTreeWidget_nativeConnectItemSelectionChanged(JNIEnv* env, jobject thiz, jlong h) {
    QTreeWidget* w = static_cast<QTreeWidget*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QTreeWidget::itemSelectionChanged, [gRef]() {
        JNIEnv* e = callbackEnv();
        jmethodID mid = e->GetMethodID(e->GetObjectClass(gRef), "nativeHandleItemSelectionChanged", "()V");
        if (mid) JQT_CALL_VOID(e, gRef, mid);
    });
}
// QListWidget currentItemChanged
JNIEXPORT void JNICALL Java_org_jqt_QListWidget_nativeConnectCurrentItemChanged(JNIEnv* env, jobject thiz, jlong h) {
    QListWidget* w = static_cast<QListWidget*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QListWidget::currentItemChanged, [gRef, w](QListWidgetItem* cur, QListWidgetItem*) {
        JNIEnv* e = callbackEnv();
        jmethodID mid = e->GetMethodID(e->GetObjectClass(gRef), "nativeHandleCurrentItemChanged", "(I)V");
        if (mid) JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(cur ? w->row(cur) : -1));
    });
}
// QApplication font
JNIEXPORT jstring JNICALL Java_org_jqt_QApplication_nativeFont(JNIEnv* env, jclass) {
    const QFont fnt = QApplication::font();
    int ps = fnt.pointSize();
    if (ps < 0) ps = fnt.pixelSize();
    return env->NewStringUTF((fnt.family() + QString(",") + QString::number(ps)).toUtf8().constData());
}
// QWidget sizePolicy
JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetSizePolicy(JNIEnv* env, jclass, jlong h, jint hpol, jint vpol) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, h));
    if (w) w->setSizePolicy(static_cast<QSizePolicy::Policy>(hpol), static_cast<QSizePolicy::Policy>(vpol));
}
JNIEXPORT jintArray JNICALL Java_org_jqt_QWidget_nativeSizePolicy(JNIEnv* env, jclass, jlong h) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, h));
    if (!w) return nullptr;
    jintArray arr = env->NewIntArray(2);
    jint v[2] = { static_cast<jint>(w->sizePolicy().horizontalPolicy()), static_cast<jint>(w->sizePolicy().verticalPolicy()) };
    env->SetIntArrayRegion(arr, 0, 2, v);
    return arr;
}
// QClipboard
JNIEXPORT jstring JNICALL Java_org_jqt_QClipboard_nativeText(JNIEnv* env, jclass) {
    return env->NewStringUTF(QApplication::clipboard()->text().toUtf8().constData());
}
JNIEXPORT void JNICALL Java_org_jqt_QClipboard_nativeSetText(JNIEnv* env, jclass, jstring text) {
    const char* u = env->GetStringUTFChars(text, nullptr);
    QApplication::clipboard()->setText(QString::fromUtf8(u));
    env->ReleaseStringUTFChars(text, u);
}
JNIEXPORT void JNICALL Java_org_jqt_QClipboard_nativeClear(JNIEnv* env, jclass) {
    QApplication::clipboard()->clear();
}
// QSettings
JNIEXPORT jlong JNICALL Java_org_jqt_QSettings_nativeCreate(JNIEnv* env, jobject) {
    if (requireApp(env) == nullptr) return 0;
    // 指定组织/应用名：空 org 名时 Qt 无处写入（注册表路径缺失）
    return registerHandle(new QSettings(QStringLiteral("JQt"), QStringLiteral("app")), /*javaOwned=*/true);
}
JNIEXPORT void JNICALL Java_org_jqt_QSettings_nativeDispose(JNIEnv* env, jclass, jlong h) {
    QObject* obj = nullptr;
    {
        std::lock_guard<std::mutex> lock(g_handleMutex);
        auto it = g_handles.find(static_cast<int64_t>(h));
        if (it == g_handles.end()) return;
        auto oit = g_javaOwned.find(static_cast<int64_t>(h));
        if (oit == g_javaOwned.end() || !oit->second) return;
        obj = static_cast<QObject*>(it->second);
        g_handles.erase(it);
        g_javaOwned.erase(oit);
    }
    if (obj && g_app) QMetaObject::invokeMethod(g_app, [obj]() { delete obj; }, Qt::QueuedConnection);
}
JNIEXPORT jint JNICALL Java_org_jqt_QSettings_nativeValue(JNIEnv* env, jobject, jlong h, jstring key) {
    QSettings* s = static_cast<QSettings*>(requireHandle(env, h));
    if (!s) return 0;
    const char* u = env->GetStringUTFChars(key, nullptr);
    int v = s->value(QString::fromUtf8(u)).toInt();
    env->ReleaseStringUTFChars(key, u);
    return v;
}
JNIEXPORT void JNICALL Java_org_jqt_QSettings_nativeSetValue(JNIEnv* env, jobject, jlong h, jstring key, jint value) {
    QSettings* s = static_cast<QSettings*>(requireHandle(env, h));
    if (!s) return;
    const char* u = env->GetStringUTFChars(key, nullptr);
    s->setValue(QString::fromUtf8(u), value);
    env->ReleaseStringUTFChars(key, u);
}
JNIEXPORT jboolean JNICALL Java_org_jqt_QSettings_nativeContains(JNIEnv* env, jobject, jlong h, jstring key) {
    QSettings* s = static_cast<QSettings*>(requireHandle(env, h));
    if (!s) return JNI_FALSE;
    const char* u = env->GetStringUTFChars(key, nullptr);
    bool ok = s->contains(QString::fromUtf8(u));
    env->ReleaseStringUTFChars(key, u);
    return ok ? JNI_TRUE : JNI_FALSE;
}
JNIEXPORT void JNICALL Java_org_jqt_QSettings_nativeRemove(JNIEnv* env, jobject, jlong h, jstring key) {
    QSettings* s = static_cast<QSettings*>(requireHandle(env, h));
    if (!s) return;
    const char* u = env->GetStringUTFChars(key, nullptr);
    s->remove(QString::fromUtf8(u));
    env->ReleaseStringUTFChars(key, u);
}
JNIEXPORT void JNICALL Java_org_jqt_QSettings_nativeClear(JNIEnv* env, jobject, jlong h) {
    QSettings* s = static_cast<QSettings*>(requireHandle(env, h));
    if (s) s->clear();
}

// L1 补全批 H：QFile 静态工具
JNIEXPORT jboolean JNICALL Java_org_jqt_QFile_nativeCopy(JNIEnv* env, jclass, jstring src, jstring dst) {
    const char* s1 = env->GetStringUTFChars(src, nullptr);
    const char* s2 = env->GetStringUTFChars(dst, nullptr);
    bool ok = QFile::copy(QString::fromUtf8(s1), QString::fromUtf8(s2));
    env->ReleaseStringUTFChars(src, s1);
    env->ReleaseStringUTFChars(dst, s2);
    return ok ? JNI_TRUE : JNI_FALSE;
}
JNIEXPORT jboolean JNICALL Java_org_jqt_QFile_nativeExists(JNIEnv* env, jclass, jstring path) {
    const char* u = env->GetStringUTFChars(path, nullptr);
    bool ok = QFile::exists(QString::fromUtf8(u));
    env->ReleaseStringUTFChars(path, u);
    return ok ? JNI_TRUE : JNI_FALSE;
}
JNIEXPORT jboolean JNICALL Java_org_jqt_QFile_nativeRemove(JNIEnv* env, jclass, jstring path) {
    const char* u = env->GetStringUTFChars(path, nullptr);
    bool ok = QFile::remove(QString::fromUtf8(u));
    env->ReleaseStringUTFChars(path, u);
    return ok ? JNI_TRUE : JNI_FALSE;
}
JNIEXPORT jboolean JNICALL Java_org_jqt_QFile_nativeRename(JNIEnv* env, jclass, jstring oldName, jstring newName) {
    const char* s1 = env->GetStringUTFChars(oldName, nullptr);
    const char* s2 = env->GetStringUTFChars(newName, nullptr);
    bool ok = QFile::rename(QString::fromUtf8(s1), QString::fromUtf8(s2));
    env->ReleaseStringUTFChars(oldName, s1);
    env->ReleaseStringUTFChars(newName, s2);
    return ok ? JNI_TRUE : JNI_FALSE;
}
JNIEXPORT jlong JNICALL Java_org_jqt_QFile_nativeSize(JNIEnv* env, jclass, jstring path) {
    const char* u = env->GetStringUTFChars(path, nullptr);
    qint64 sz = QFileInfo(QString::fromUtf8(u)).size();
    env->ReleaseStringUTFChars(path, u);
    return static_cast<jlong>(sz);
}

// 吐槽修复：pos() / addSpacing
JNIEXPORT jintArray JNICALL Java_org_jqt_QWidget_nativePos(JNIEnv* env, jclass, jlong handle) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (w == nullptr) return nullptr;
    jintArray arr = env->NewIntArray(2);
    jint v[2] = { w->x(), w->y() };
    env->SetIntArrayRegion(arr, 0, 2, v);
    return arr;
}
JNIEXPORT void JNICALL Java_org_jqt_QLayout_nativeAddSpacing(JNIEnv* env, jobject, jlong handle, jint spacing) {
    QBoxLayout* layout = static_cast<QBoxLayout*>(requireHandle(env, handle));
    if (layout) layout->addSpacing(spacing);
}

// L1 补全批 I：剩余可做项
// QLayout getters
JNIEXPORT jint JNICALL Java_org_jqt_QLayout_nativeCount(JNIEnv* env, jobject, jlong h) { QLayout* w = static_cast<QLayout*>(requireHandle(env, h)); return w ? static_cast<jint>(w->count()) : 0; }
JNIEXPORT jint JNICALL Java_org_jqt_QLayout_nativeSpacing(JNIEnv* env, jobject, jlong h) { QLayout* w = static_cast<QLayout*>(requireHandle(env, h)); return w ? static_cast<jint>(w->spacing()) : 0; }
// QListWidget itemEntered
JNIEXPORT void JNICALL Java_org_jqt_QListWidget_nativeConnectItemEntered(JNIEnv* env, jobject thiz, jlong h) {
    QListWidget* w = static_cast<QListWidget*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QListWidget::itemEntered, [gRef, w](QListWidgetItem* item) {
        JNIEnv* e = callbackEnv();
        jmethodID mid = e->GetMethodID(e->GetObjectClass(gRef), "nativeHandleItemEntered", "(I)V");
        if (mid && item) JQT_CALL_VOID(e, gRef, mid, static_cast<jint>(w->row(item)));
    });
}
// QPushButton menu
JNIEXPORT void JNICALL Java_org_jqt_QPushButton_nativeSetMenu(JNIEnv* env, jclass, jlong h, jlong mh) {
    QPushButton* w = static_cast<QPushButton*>(requireHandle(env, h));
    QMenu* m = static_cast<QMenu*>(requireHandle(env, mh));
    if (w && m) w->setMenu(m);
}
JNIEXPORT jboolean JNICALL Java_org_jqt_QPushButton_nativeHasMenu(JNIEnv* env, jclass, jlong h) { QPushButton* w = static_cast<QPushButton*>(requireHandle(env, h)); return (w && w->menu()) ? JNI_TRUE : JNI_FALSE; }
// QLabel linkHovered
JNIEXPORT void JNICALL Java_org_jqt_QLabel_nativeConnectLinkHovered(JNIEnv* env, jobject thiz, jlong h) {
    QLabel* w = static_cast<QLabel*>(requireHandle(env, h)); if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QLabel::linkHovered, [gRef](const QString& url) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleLinkHovered", "(Ljava/lang/String;)V");
        if (mid) { jstring js = e->NewStringUTF(url.toUtf8().constData()); JQT_CALL_VOID(e, gRef, mid, js); e->DeleteLocalRef(js); }
    });
}
// QWidget/QApplication palette
JNIEXPORT jint JNICALL Java_org_jqt_QWidget_nativePalette(JNIEnv* env, jclass, jlong h) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, h));
    if (!w) return 0;
    return static_cast<jint>(w->palette().color(QPalette::Window).rgba());
}
JNIEXPORT jint JNICALL Java_org_jqt_QApplication_nativePalette(JNIEnv* env, jclass) {
    return static_cast<jint>(QApplication::palette().color(QPalette::Window).rgba());
}
// QDir
JNIEXPORT jstring JNICALL Java_org_jqt_QDir_nativeCurrent(JNIEnv* env, jclass) {
    return env->NewStringUTF(QDir::current().absolutePath().toUtf8().constData());
}
JNIEXPORT jboolean JNICALL Java_org_jqt_QDir_nativeRemove(JNIEnv* env, jclass, jstring path) {
    const char* u = env->GetStringUTFChars(path, nullptr);
    bool ok = QDir().rmdir(QString::fromUtf8(u));
    env->ReleaseStringUTFChars(path, u);
    return ok ? JNI_TRUE : JNI_FALSE;
}
JNIEXPORT jint JNICALL Java_org_jqt_QDir_nativeCount(JNIEnv* env, jclass, jstring path) {
    const char* u = env->GetStringUTFChars(path, nullptr);
    int n = QDir(QString::fromUtf8(u)).count();
    env->ReleaseStringUTFChars(path, u);
    return n;
}
// QClipboard selectionChanged
JNIEXPORT void JNICALL Java_org_jqt_QClipboard_nativeConnectSelectionChanged(JNIEnv* env, jclass) {
    QClipboard* c = QApplication::clipboard();
    QObject::connect(c, &QClipboard::dataChanged, []() {
        JNIEnv* e = callbackEnv();
        jclass cls = e->FindClass("org/jqt/QClipboard");
        jmethodID mid = e->GetStaticMethodID(cls, "nativeHandleSelectionChanged", "()V");
        if (mid) e->CallStaticVoidMethod(cls, mid);
    });
}
// QFile resize
JNIEXPORT jboolean JNICALL Java_org_jqt_QFile_nativeResize(JNIEnv* env, jclass, jstring path, jlong size) {
    const char* u = env->GetStringUTFChars(path, nullptr);
    bool ok = QFile::resize(QString::fromUtf8(u), size);
    env->ReleaseStringUTFChars(path, u);
    return ok ? JNI_TRUE : JNI_FALSE;
}

// ============================================================================
// Exclusive Kit（v0.6.1）：Windows 独家能力（Qt 官方未封装）
// DWM 边框/标题栏/文字颜色 + 深色标题栏 + Mica + 任务栏进度 + 全局热键 + 开机自启
// ============================================================================
#ifdef _WIN32
#include <dwmapi.h>
#endif

// DWM 属性设置（kind: 1=border 2=caption 3=text 4=darkTitleBar 5=mica）
JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeSetDwmAttribute(JNIEnv* env, jclass, jlong handle, jint kind, jint argb) {
#ifdef _WIN32
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (!w || !w->windowHandle()) return;
    HWND hwnd = reinterpret_cast<HWND>(w->windowHandle()->winId());
    if (!hwnd) return;
    static auto dwmSet = reinterpret_cast<HRESULT(WINAPI*)(HWND, DWORD, LPCVOID, DWORD)>(GetProcAddress(GetModuleHandleW(L"dwmapi.dll"), "DwmSetWindowAttribute"));
    if (!dwmSet) return;
    if (kind == 4) {
        BOOL dark = (argb != 0) ? TRUE : FALSE;
        dwmSet(hwnd, 20 /*DWMWA_USE_IMMERSIVE_DARK_MODE*/, &dark, sizeof(dark));   // Win10 1809+
    } else if (kind == 5) {
        // DWMWA_SYSTEMBACKDROP_TYPE=38：0 无 / 1 默认 / 2 Mica / 3 Acrylic / 4 Tabbed（Win11 22H2+）
        int type = (argb != 0) ? 2 : 1;
        dwmSet(hwnd, 38, &type, sizeof(type));
    } else {
        COLORREF color = static_cast<COLORREF>(argb & 0xFFFFFF);
        DWORD attr = (kind == 1) ? 34 : (kind == 2 ? 35 : 36);   // BORDER_COLOR / CAPTION_COLOR / TEXT_COLOR
        dwmSet(hwnd, attr, &color, sizeof(color));               // Win11 22H2+
    }
#else
    (void)env; (void)handle; (void)kind; (void)argb;
#endif
}

// 任务栏进度（ITaskbarList3::SetProgressValue）
JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeTaskbarProgress(JNIEnv* env, jclass, jlong handle, jint value, jint max) {
#ifdef _WIN32
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (!w || !w->windowHandle()) return;
    HWND hwnd = reinterpret_cast<HWND>(w->windowHandle()->winId());
    if (!hwnd) return;
    static ITaskbarList3* taskbar = nullptr;
    if (!taskbar) {
        CoInitialize(nullptr);
        if (CoCreateInstance(CLSID_TaskbarList, nullptr, CLSCTX_INPROC_SERVER, IID_ITaskbarList3, reinterpret_cast<void**>(&taskbar)) != S_OK) {
            taskbar = nullptr;
            return;
        }
    }
    if (value < 0 || max <= 0) {
        taskbar->SetProgressState(hwnd, TBPF_NOPROGRESS);
    } else {
        taskbar->SetProgressValue(hwnd, static_cast<ULONGLONG>(value), static_cast<ULONGLONG>(max));
        taskbar->SetProgressState(hwnd, TBPF_NORMAL);
    }
#else
    (void)env; (void)handle; (void)value; (void)max;
#endif
}

// 全局热键注册（combo 如 "Ctrl+Shift+X"）→ 返回 hotkeyId（>0 成功）
JNIEXPORT jint JNICALL Java_org_jqt_GlobalHotkey_nativeRegister(JNIEnv* env, jclass, jstring combo) {
#ifdef _WIN32
    const char* utf = env->GetStringUTFChars(combo, nullptr);
    const QStringList parts = QString::fromUtf8(utf).split('+');
    env->ReleaseStringUTFChars(combo, utf);
    if (parts.size() < 2) return 0;
    UINT mods = 0;
    QString key = parts.last();
    for (int i = 0; i < parts.size() - 1; i++) {
        const QString m = parts.at(i).trimmed();
        if (m == "Ctrl") mods |= MOD_CONTROL;
        else if (m == "Alt") mods |= MOD_ALT;
        else if (m == "Shift") mods |= MOD_SHIFT;
        else if (m == "Win") mods |= MOD_WIN;
        else return 0;
    }
    UINT vk = 0;
    if (key.length() == 1) vk = VkKeyScanW(key.at(0).toLatin1()) & 0xFF;
    else if (key.startsWith("F") && key.mid(1).toInt() >= 1 && key.mid(1).toInt() <= 24) vk = 0x70 + key.mid(1).toInt() - 1;
    else return 0;
    static int nextId = 0x6000;
    int id = ++nextId;
    if (!RegisterHotKey(nullptr, id, mods, vk)) return 0;
    return id;
#else
    (void)env; (void)combo; return 0;
#endif
}

JNIEXPORT void JNICALL Java_org_jqt_GlobalHotkey_nativeUnregister(JNIEnv* env, jclass, jint hotkeyId) {
#ifdef _WIN32
    UnregisterHotKey(nullptr, hotkeyId);
#else
    (void)env; (void)hotkeyId;
#endif
}

// 开机自启（v0.7.0 跨平台：Windows Run 注册表 / macOS LaunchAgent / Linux XDG autostart）
JNIEXPORT jboolean JNICALL Java_org_jqt_QApplication_nativeSetAutoStart(JNIEnv* env, jclass, jboolean enable, jstring exePath) {
    const char* utf = exePath ? env->GetStringUTFChars(exePath, nullptr) : nullptr;
    const QString path = utf ? QString::fromUtf8(utf) : QString();
    if (utf) env->ReleaseStringUTFChars(exePath, utf);
    const QString appName = QCoreApplication::applicationName();
    const QString name = appName.isEmpty() ? QStringLiteral("JQtApp") : appName;
#ifdef _WIN32
    HKEY key = nullptr;
    if (RegOpenKeyExW(HKEY_CURRENT_USER, L"Software\\Microsoft\\Windows\\CurrentVersion\\Run", 0, KEY_SET_VALUE, &key) != ERROR_SUCCESS) {
        return JNI_FALSE;
    }
    LONG result = ERROR_SUCCESS;
    if (enable) {
        if (path.isEmpty()) { RegCloseKey(key); return JNI_FALSE; }
        const std::wstring wpath = path.toStdWString();
        result = RegSetValueExW(key, reinterpret_cast<LPCWSTR>(name.utf16()), 0, REG_SZ, reinterpret_cast<const BYTE*>(wpath.c_str()), static_cast<DWORD>((wpath.size() + 1) * sizeof(wchar_t)));
    } else {
        result = RegDeleteValueW(key, reinterpret_cast<LPCWSTR>(name.utf16()));
        if (result == ERROR_FILE_NOT_FOUND) result = ERROR_SUCCESS;
    }
    RegCloseKey(key);
    return (result == ERROR_SUCCESS) ? JNI_TRUE : JNI_FALSE;
#elif defined(__linux__)
    // XDG autostart: ~/.config/autostart/<name>.desktop
    const QString dir = QStandardPaths::writableLocation(QStandardPaths::ConfigLocation) + QStringLiteral("/autostart");
    const QString file = dir + QLatin1Char('/') + name + QStringLiteral(".desktop");
    if (enable) {
        QString execPath = path;
        if (execPath.isEmpty()) execPath = QCoreApplication::applicationFilePath();
        if (execPath.isEmpty()) return JNI_FALSE;
        if (!QDir().mkpath(dir)) return JNI_FALSE;
        QFile f(file);
        if (!f.open(QIODevice::WriteOnly | QIODevice::Truncate | QIODevice::Text)) return JNI_FALSE;
        QTextStream ts(&f);
        ts << "[Desktop Entry]\nType=Application\nName=" << name
           << "\nExec=\"" << execPath << "\"\nX-GNOME-Autostart-enabled=true\n";
        f.close();
        return JNI_TRUE;
    } else {
        return (QFile::remove(file) || !QFile::exists(file)) ? JNI_TRUE : JNI_FALSE;
    }
#elif defined(__APPLE__)
    // LaunchAgent: ~/Library/LaunchAgents/com.silentstudio.<name>.plist
    const QString dir = QDir::homePath() + QStringLiteral("/Library/LaunchAgents");
    const QString label = QStringLiteral("com.silentstudio.") + name;
    const QString file = dir + QLatin1Char('/') + label + QStringLiteral(".plist");
    if (enable) {
        QString execPath = path;
        if (execPath.isEmpty()) execPath = QCoreApplication::applicationFilePath();
        if (execPath.isEmpty()) return JNI_FALSE;
        if (!QDir().mkpath(dir)) return JNI_FALSE;
        QFile f(file);
        if (!f.open(QIODevice::WriteOnly | QIODevice::Truncate | QIODevice::Text)) return JNI_FALSE;
        QTextStream ts(&f);
        ts << "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
           << "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
           << "<plist version=\"1.0\">\n<dict>\n"
           << "  <key>Label</key><string>" << label << "</string>\n"
           << "  <key>ProgramArguments</key>\n  <array>\n    <string>" << execPath << "</string>\n  </array>\n"
           << "  <key>RunAtLoad</key><true/>\n"
           << "</dict>\n</plist>\n";
        f.close();
        return JNI_TRUE;
    } else {
        return (QFile::remove(file) || !QFile::exists(file)) ? JNI_TRUE : JNI_FALSE;
    }
#else
    (void)env; (void)enable; return JNI_FALSE;
#endif
}

// 阻止系统休眠/息屏（v0.7.0 跨平台：Windows SetThreadExecutionState / macOS NSProcessInfo / Linux D-Bus Inhibit）
JNIEXPORT jboolean JNICALL Java_org_jqt_QApplication_nativePreventSleep(JNIEnv* env, jclass, jboolean on) {
#if defined(_WIN32)
    if (on) {
        return SetThreadExecutionState(ES_CONTINUOUS | ES_SYSTEM_REQUIRED | ES_DISPLAY_REQUIRED) ? JNI_TRUE : JNI_FALSE;
    } else {
        SetThreadExecutionState(ES_CONTINUOUS);   // 恢复默认（ES_CONTINUOUS 必须保留）
        return JNI_TRUE;
    }
#elif defined(__APPLE__)
    // NSProcessInfo beginActivityWithOptions:reason:（NSActivityIdleSystemSleepDisabled = 1<<20）
    // 精确签名函数指针：绕开 arm64 上 variadic 传参不可靠的问题（reason 传空会抛异常）
    static const JQtMsg0 s_processInfo = JQT_OBJC_CAST(JQtMsg0);
    static const JQtMsgOpt s_begin = JQT_OBJC_CAST(JQtMsgOpt);
    static const JQtMsgEnd s_end = JQT_OBJC_CAST(JQtMsgEnd);
    static id s_activity = nullptr;
    if (on && !s_activity) {
        id proc = s_processInfo((id)objc_getClass("NSProcessInfo"), sel_registerName("processInfo"));
        if (!proc) return JNI_FALSE;
        CFStringRef reason = CFStringCreateWithCString(kCFAllocatorDefault, "JQt preventSleep", kCFStringEncodingUTF8);
        if (!reason) return JNI_FALSE;
        s_activity = s_begin(proc, sel_registerName("beginActivityWithOptions:reason:"), 1UL << 20, (id)reason);
        CFRelease(reason);
        return s_activity ? JNI_TRUE : JNI_FALSE;
    } else if (!on && s_activity) {
        id proc = s_processInfo((id)objc_getClass("NSProcessInfo"), sel_registerName("processInfo"));
        if (proc) s_end(proc, sel_registerName("endActivity:"), s_activity);
        s_activity = nullptr;
    }
    return JNI_TRUE;
#elif defined(__linux__)
    // org.freedesktop.ScreenSaver Inhibit（KDE/GNOME）；失败回退 org.gnome.SessionManager Inhibit
    static uint s_cookie = 0;
    static bool s_active = false;
    if (on && !s_active) {
        QDBusInterface iface(QStringLiteral("org.freedesktop.ScreenSaver"),
                             QStringLiteral("/org/freedesktop/ScreenSaver"),
                             QStringLiteral("org.freedesktop.ScreenSaver"),
                             QDBusConnection::sessionBus());
        if (iface.isValid()) {
            QDBusReply<uint> reply = iface.call(QStringLiteral("Inhibit"),
                QCoreApplication::applicationName().isEmpty() ? QStringLiteral("JQtApp") : QCoreApplication::applicationName(),
                QStringLiteral("JQt preventSleep"));
            if (reply.isValid()) { s_cookie = reply.value(); s_active = true; return JNI_TRUE; }
        }
        // GNOME 3.28+ 无 gnome-screensaver：回退 org.gnome.SessionManager
        QDBusInterface gnome(QStringLiteral("org.gnome.SessionManager"),
                             QStringLiteral("/org/gnome/SessionManager"),
                             QStringLiteral("org.gnome.SessionManager"),
                             QDBusConnection::sessionBus());
        if (gnome.isValid()) {
            QDBusReply<QString> reply = gnome.call(QStringLiteral("Inhibit"),
                QCoreApplication::applicationName().isEmpty() ? QStringLiteral("JQtApp") : QCoreApplication::applicationName(),
                0u, QStringLiteral("JQt preventSleep"), 8u /* InhibitIdle */);
            if (reply.isValid()) { s_active = true; return JNI_TRUE; }
        }
        return JNI_FALSE;
    } else if (!on && s_active) {
        QDBusInterface iface(QStringLiteral("org.freedesktop.ScreenSaver"),
                             QStringLiteral("/org/freedesktop/ScreenSaver"),
                             QStringLiteral("org.freedesktop.ScreenSaver"),
                             QDBusConnection::sessionBus());
        if (iface.isValid()) iface.call(QStringLiteral("UnInhibit"), s_cookie);
        s_active = false;
    }
    return JNI_TRUE;
#else
    (void)env; (void)on; return JNI_FALSE;
#endif
}

// 桌面通知（v0.7.0 跨平台：Linux D-Bus Notifications / Windows 托盘气泡 / macOS NSUserNotification）
JNIEXPORT jboolean JNICALL Java_org_jqt_QApplication_nativeShowNotification(JNIEnv* env, jclass, jstring title, jstring body, jint timeoutMs) {
    const char* t = title ? env->GetStringUTFChars(title, nullptr) : nullptr;
    const char* b = body ? env->GetStringUTFChars(body, nullptr) : nullptr;
    const QString qtTitle = t ? QString::fromUtf8(t) : QString();
    const QString qtBody = b ? QString::fromUtf8(b) : QString();
    if (t) env->ReleaseStringUTFChars(title, t);
    if (b) env->ReleaseStringUTFChars(body, b);
    const QString appName = QCoreApplication::applicationName().isEmpty() ? QStringLiteral("JQtApp") : QCoreApplication::applicationName();
#if defined(_WIN32)
    // 托盘气泡（零依赖；Windows 通知中心 Toast 需打包身份，托盘方案对任意应用可用）
    static QSystemTrayIcon* s_tray = nullptr;
    if (!s_tray) {
        s_tray = new QSystemTrayIcon(QApplication::windowIcon());
        s_tray->show();
    }
    if (!s_tray->isVisible()) s_tray->show();
    s_tray->showMessage(qtTitle, qtBody, QSystemTrayIcon::Information, timeoutMs > 0 ? timeoutMs : 5000);
    return JNI_TRUE;
#elif defined(__APPLE__)
    // NSUserNotification（macOS 10.8+；Apple 弃用但可用，无需权限弹窗与打包身份）
    static const JQtMsg0 s_msg0 = JQT_OBJC_CAST(JQtMsg0);
    static const JQtMsgV1 s_msgV1 = JQT_OBJC_CAST(JQtMsgV1);
    id center = s_msg0((id)objc_getClass("NSUserNotificationCenter"), sel_registerName("defaultUserNotificationCenter"));
    if (!center) return JNI_FALSE;
    id notif = s_msg0((id)objc_getClass("NSUserNotification"), sel_registerName("alloc"));
    notif = s_msg0(notif, sel_registerName("init"));
    const QByteArray tba = qtTitle.toUtf8();
    const QByteArray bba = qtBody.toUtf8();
    CFStringRef tstr = CFStringCreateWithCString(kCFAllocatorDefault, tba.constData(), kCFStringEncodingUTF8);
    CFStringRef bstr = CFStringCreateWithCString(kCFAllocatorDefault, bba.constData(), kCFStringEncodingUTF8);
    if (tstr) s_msgV1(notif, sel_registerName("setTitle:"), (id)tstr);
    if (bstr) s_msgV1(notif, sel_registerName("setInformativeText:"), (id)bstr);
    s_msgV1(center, sel_registerName("deliverNotification:"), notif);
    if (tstr) CFRelease(tstr);
    if (bstr) CFRelease(bstr);
    return JNI_TRUE;
#elif defined(__linux__)
    QDBusInterface notif(QStringLiteral("org.freedesktop.Notifications"),
                         QStringLiteral("/org/freedesktop/Notifications"),
                         QStringLiteral("org.freedesktop.Notifications"),
                         QDBusConnection::sessionBus());
    if (!notif.isValid()) return JNI_FALSE;
    QVariantList args;
    args << appName << QVariant(0u) << QVariant(QString()) << qtTitle << qtBody
         << QVariant(QStringList()) << QVariant(QVariantMap()) << QVariant(timeoutMs > 0 ? timeoutMs : -1);
    QDBusMessage reply = notif.callWithArgumentList(QDBus::AutoDetect, QStringLiteral("Notify"), args);
    return reply.type() == QDBusMessage::ReplyMessage ? JNI_TRUE : JNI_FALSE;
#else
    (void)env; return JNI_FALSE;
#endif
}

// Dock 图标徽章（v0.7.0 macOS 独家：NSDockTile setBadgeLabel:；对齐 Windows 任务栏进度）
JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeSetDockBadge(JNIEnv* env, jclass, jlong handle, jstring badge) {
#if defined(__APPLE__)
    // offscreen 等非 cocoa 平台无真实 NSApplication/Dock，直接 no-op
    if (QGuiApplication::platformName() != QStringLiteral("cocoa")) { (void)handle; return; }
    static const JQtMsg0 s_sharedApp = JQT_OBJC_CAST(JQtMsg0);
    static const JQtMsg0 s_dockTileMsg = JQT_OBJC_CAST(JQtMsg0);
    static const JQtMsgV1 s_setBadge = JQT_OBJC_CAST(JQtMsgV1);
    id app = s_sharedApp((id)objc_getClass("NSApplication"), sel_registerName("sharedApplication"));
    if (!app) return;
    id dockTile = s_dockTileMsg(app, sel_registerName("dockTile"));
    if (!dockTile) return;
    if (badge) {
        const char* utf = env->GetStringUTFChars(badge, nullptr);
        CFStringRef str = CFStringCreateWithCString(kCFAllocatorDefault, utf ? utf : "", kCFStringEncodingUTF8);
        if (utf) env->ReleaseStringUTFChars(badge, utf);
        if (str) s_setBadge(dockTile, sel_registerName("setBadgeLabel:"), (id)str);
        if (str) CFRelease(str);
    } else {
        s_setBadge(dockTile, sel_registerName("setBadgeLabel:"), (id)nullptr);
    }
#else
    (void)env; (void)handle; (void)badge;
#endif
}

// macOS 原生窗口属性（kind: 1=titlebarAppearsTransparent 2=fullSizeContentView；v0.7.0）
JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeSetMacWindowAttribute(JNIEnv* env, jclass, jlong handle, jint kind, jboolean value) {
#if defined(__APPLE__)
    // offscreen 等非 cocoa 平台 winId() 不是 NSView*，对其实施 objc_msgSend 会 SIGSEGV
    if (QGuiApplication::platformName() != QStringLiteral("cocoa")) { (void)kind; (void)value; return; }
    static const JQtMsg0 s_windowMsg = JQT_OBJC_CAST(JQtMsg0);
    static const JQtMsgBool s_setTransparent = JQT_OBJC_CAST(JQtMsgBool);
    static const JQtMsgUL s_styleMask = JQT_OBJC_CAST(JQtMsgUL);
    static const JQtMsgSetMask s_setMask = JQT_OBJC_CAST(JQtMsgSetMask);
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (!w) return;
    id view = reinterpret_cast<id>(w->winId());   // QWindow::winId() 在 macOS 为 NSView*
    if (!view) return;
    id window = s_windowMsg(view, sel_registerName("window"));
    if (!window) return;
    if (kind == 1) {
        s_setTransparent(window, sel_registerName("setTitlebarAppearsTransparent:"), (bool)value);
    } else if (kind == 2) {
        // NSFullSizeContentViewWindowMask = 1UL << 15
        const unsigned long fullSize = 1UL << 15;
        unsigned long mask = s_styleMask(window, sel_registerName("styleMask"));
        s_setMask(window, sel_registerName("setStyleMask:"), value ? (mask | fullSize) : (mask & ~fullSize));
    }
#else
    (void)env; (void)handle; (void)kind; (void)value;
#endif
}
// WM_HOTKEY 分发（在 JQtPointerFilter::nativeEventFilter 中调用）
#ifdef _WIN32
static void jqtDispatchHotkey(int hotkeyId) {
    JNIEnv* e = callbackEnv();
    jclass cls = e->FindClass("org/jqt/GlobalHotkey");
    jmethodID mid = e->GetStaticMethodID(cls, "nativeHandleHotkey", "(I)V");
    if (mid) e->CallStaticVoidMethod(cls, mid, static_cast<jint>(hotkeyId));
}
#endif


// ============================================================================
// L1 收尾批 1（v0.7.1）：placeholderText / icon / QFile 实例 / 剪贴板图像 / 信号补全
// ============================================================================

// QLineEdit::placeholderText
JNIEXPORT jstring JNICALL Java_org_jqt_QLineEdit_nativePlaceholderText(JNIEnv* env, jobject, jlong handle) {
    QLineEdit* w = static_cast<QLineEdit*>(requireHandle(env, handle));
    if (!w) return nullptr;
    return env->NewStringUTF(w->placeholderText().toUtf8().constData());
}

// QPlainTextEdit::setPlaceholderText / placeholderText
JNIEXPORT void JNICALL Java_org_jqt_QTextEdit_nativeSetPlaceholderText(JNIEnv* env, jobject, jlong handle, jstring text) {
    QPlainTextEdit* w = static_cast<QPlainTextEdit*>(requireHandle(env, handle));
    if (!w) return;
    const char* t = text ? env->GetStringUTFChars(text, nullptr) : nullptr;
    w->setPlaceholderText(t ? QString::fromUtf8(t) : QString());
    if (t) env->ReleaseStringUTFChars(text, t);
}

JNIEXPORT jstring JNICALL Java_org_jqt_QTextEdit_nativePlaceholderText(JNIEnv* env, jobject, jlong handle) {
    QPlainTextEdit* w = static_cast<QPlainTextEdit*>(requireHandle(env, handle));
    if (!w) return nullptr;
    return env->NewStringUTF(w->placeholderText().toUtf8().constData());
}

// QMenu::setIcon / icon（图标以路径字符串简化；路径存于本地表供 getter）
static std::unordered_map<jlong, QString> g_menuIconPaths;
JNIEXPORT void JNICALL Java_org_jqt_QMenu_nativeSetIcon(JNIEnv* env, jclass, jlong handle, jstring iconPath) {
    QMenu* m = static_cast<QMenu*>(requireHandle(env, handle));
    if (!m) return;
    const char* t = iconPath ? env->GetStringUTFChars(iconPath, nullptr) : nullptr;
    const QString p = t ? QString::fromUtf8(t) : QString();
    if (t) env->ReleaseStringUTFChars(iconPath, t);
    m->setIcon(QIcon(p));
    if (p.isEmpty()) g_menuIconPaths.erase(handle); else g_menuIconPaths[handle] = p;
}

JNIEXPORT jstring JNICALL Java_org_jqt_QMenu_nativeIcon(JNIEnv* env, jclass, jlong handle) {
    auto it = g_menuIconPaths.find(handle);
    if (it == g_menuIconPaths.end()) return nullptr;
    return env->NewStringUTF(it->second.toUtf8().constData());
}

// QPushButton（QAbstractButton）：text / icon / shortcut
static std::unordered_map<jlong, QString> g_buttonIconPaths;
JNIEXPORT jstring JNICALL Java_org_jqt_QPushButton_nativeText(JNIEnv* env, jclass, jlong handle) {
    QPushButton* b = static_cast<QPushButton*>(requireHandle(env, handle));
    if (!b) return nullptr;
    return env->NewStringUTF(b->text().toUtf8().constData());
}

JNIEXPORT void JNICALL Java_org_jqt_QPushButton_nativeSetIcon(JNIEnv* env, jclass, jlong handle, jstring iconPath) {
    QPushButton* b = static_cast<QPushButton*>(requireHandle(env, handle));
    if (!b) return;
    const char* t = iconPath ? env->GetStringUTFChars(iconPath, nullptr) : nullptr;
    const QString p = t ? QString::fromUtf8(t) : QString();
    if (t) env->ReleaseStringUTFChars(iconPath, t);
    b->setIcon(QIcon(p));
    if (p.isEmpty()) g_buttonIconPaths.erase(handle); else g_buttonIconPaths[handle] = p;
}

JNIEXPORT jstring JNICALL Java_org_jqt_QPushButton_nativeIcon(JNIEnv* env, jclass, jlong handle) {
    auto it = g_buttonIconPaths.find(handle);
    if (it == g_buttonIconPaths.end()) return nullptr;
    return env->NewStringUTF(it->second.toUtf8().constData());
}

JNIEXPORT void JNICALL Java_org_jqt_QPushButton_nativeSetShortcut(JNIEnv* env, jclass, jlong handle, jstring shortcut) {
    QPushButton* b = static_cast<QPushButton*>(requireHandle(env, handle));
    if (!b) return;
    const char* t = shortcut ? env->GetStringUTFChars(shortcut, nullptr) : nullptr;
    if (t) { b->setShortcut(QKeySequence::fromString(QString::fromUtf8(t))); env->ReleaseStringUTFChars(shortcut, t); }
    else b->setShortcut(QKeySequence());
}

JNIEXPORT jstring JNICALL Java_org_jqt_QPushButton_nativeShortcut(JNIEnv* env, jclass, jlong handle) {
    QPushButton* b = static_cast<QPushButton*>(requireHandle(env, handle));
    if (!b) return nullptr;
    const QString s = b->shortcut().toString();
    return s.isEmpty() ? nullptr : env->NewStringUTF(s.toUtf8().constData());
}

// QClipboard：setPixmap(byte[] PNG/JPEG) / pixmap() 返回 PNG bytes
JNIEXPORT void JNICALL Java_org_jqt_QClipboard_nativeSetPixmap(JNIEnv* env, jclass, jbyteArray bytes) {
    if (!bytes) return;
    const jsize len = env->GetArrayLength(bytes);
    jbyte* buf = env->GetByteArrayElements(bytes, nullptr);
    if (!buf) return;
    QImage img = QImage::fromData(reinterpret_cast<const uchar*>(buf), static_cast<int>(len));
    env->ReleaseByteArrayElements(bytes, buf, JNI_ABORT);
    if (!img.isNull()) QApplication::clipboard()->setImage(img);
}

JNIEXPORT jbyteArray JNICALL Java_org_jqt_QClipboard_nativePixmap(JNIEnv* env, jclass) {
    QImage img = QApplication::clipboard()->image();
    if (img.isNull()) return nullptr;
    QByteArray png;
    QBuffer buf(&png);
    buf.open(QIODevice::WriteOnly);
    img.save(&buf, "PNG");
    jbyteArray out = env->NewByteArray(static_cast<jsize>(png.size()));
    if (!out) return nullptr;
    env->SetByteArrayRegion(out, 0, static_cast<jsize>(png.size()), reinterpret_cast<const jbyte*>(png.constData()));
    return out;
}

// QFile 实例 API（open/close/read/write）
JNIEXPORT jlong JNICALL Java_org_jqt_QFile_nativeCreate(JNIEnv* env, jobject) {
    if (requireApp(env) == nullptr) return 0;
    QFile* f = new QFile();
    return registerHandle(f, /*javaOwned=*/true);
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QFile_nativeOpen(JNIEnv* env, jobject, jlong handle, jstring path, jint mode) {
    QFile* f = static_cast<QFile*>(requireHandle(env, handle));
    if (!f) return JNI_FALSE;
    const char* t = path ? env->GetStringUTFChars(path, nullptr) : nullptr;
    if (!t) return JNI_FALSE;
    const QString p = QString::fromUtf8(t);
    env->ReleaseStringUTFChars(path, t);
    QIODevice::OpenModeFlag om = QIODevice::ReadOnly;
    if (mode == 1) om = QIODevice::WriteOnly;
    else if (mode == 2) om = QIODevice::ReadWrite;
    else if (mode == 3) om = QIODevice::Append;
    f->setFileName(p);
    return f->open(om) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_org_jqt_QFile_nativeClose(JNIEnv* env, jobject, jlong handle) {
    QFile* f = static_cast<QFile*>(requireHandle(env, handle));
    if (f) f->close();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QFile_nativeIsOpen(JNIEnv* env, jobject, jlong handle) {
    QFile* f = static_cast<QFile*>(requireHandle(env, handle));
    return (f && f->isOpen()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QFile_nativeWrite(JNIEnv* env, jobject, jlong handle, jstring text) {
    QFile* f = static_cast<QFile*>(requireHandle(env, handle));
    if (!f || !f->isOpen()) return JNI_FALSE;
    const char* t = text ? env->GetStringUTFChars(text, nullptr) : nullptr;
    if (!t) return JNI_FALSE;
    const QByteArray data = QByteArray::fromRawData(t, static_cast<int>(strlen(t)));
    const qint64 n = f->write(data);
    env->ReleaseStringUTFChars(text, t);
    return n >= 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL Java_org_jqt_QFile_nativeReadAll(JNIEnv* env, jobject, jlong handle) {
    QFile* f = static_cast<QFile*>(requireHandle(env, handle));
    if (!f || !f->isOpen()) return nullptr;
    const QByteArray data = f->readAll();
    return env->NewStringUTF(data.constData());
}

JNIEXPORT jstring JNICALL Java_org_jqt_QFile_nativeReadLine(JNIEnv* env, jobject, jlong handle) {
    QFile* f = static_cast<QFile*>(requireHandle(env, handle));
    if (!f || !f->isOpen()) return nullptr;
    const QByteArray data = f->readLine();
    if (data.isEmpty()) return nullptr;
    return env->NewStringUTF(data.constData());
}

// QListWidget：itemChanged 信号 + row(text)
JNIEXPORT void JNICALL Java_org_jqt_QListWidget_nativeConnectItemChanged(JNIEnv* env, jobject thiz, jlong handle) {
    QListWidget* w = static_cast<QListWidget*>(requireHandle(env, handle));
    if (!w) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QListWidget::itemChanged, [gRef, w](QListWidgetItem* item) {
        if (!item) return;
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleItemChanged", "(ILjava/lang/String;)V");
        if (mid) {
            jstring js = e->NewStringUTF(item->text().toUtf8().constData());
            e->CallVoidMethod(gRef, mid, static_cast<jint>(w->row(item)), js);
            e->DeleteLocalRef(js);
        }
    });
}

JNIEXPORT jint JNICALL Java_org_jqt_QListWidget_nativeRow(JNIEnv* env, jclass, jlong handle, jstring itemText) {
    QListWidget* w = static_cast<QListWidget*>(requireHandle(env, handle));
    if (!w) return -1;
    const char* t = itemText ? env->GetStringUTFChars(itemText, nullptr) : nullptr;
    if (!t) return -1;
    const QString needle = QString::fromUtf8(t);
    env->ReleaseStringUTFChars(itemText, t);
    for (int i = 0; i < w->count(); i++) {
        QListWidgetItem* it = w->item(i);
        if (it && it->text() == needle) return static_cast<jint>(i);
    }
    return -1;
}

// QMainWindow：iconSizeChanged / toolButtonStyleChanged（JQtWindowShell 非 QMainWindow，
// 语义由 QToolBar 实例信号承担；此处为空实现防止 UnsatisfiedLinkError）
JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeConnectIconSizeChanged(JNIEnv* env, jobject, jlong) { (void)env; }
JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeConnectToolButtonStyleChanged(JNIEnv* env, jobject, jlong) { (void)env; }

// QColorDialog：非阻塞 open + colorSelected
static QColorDialog* g_colorDialog = nullptr;
static jobject g_colorDialogCls = nullptr;
JNIEXPORT void JNICALL Java_org_jqt_QColorDialog_nativeOpen(JNIEnv* env, jclass, jlong winHandle, jstring title, jint argb) {
    QWidget* parent = winHandle ? static_cast<QWidget*>(requireHandle(env, winHandle)) : nullptr;
    if (!g_colorDialog) {
        g_colorDialog = new QColorDialog(parent);
        QObject::connect(g_colorDialog, &QColorDialog::colorSelected, [](const QColor& c) {
            if (!g_colorDialogCls) return;
            JNIEnv* e = callbackEnv();
            jmethodID mid = e->GetStaticMethodID(static_cast<jclass>(g_colorDialogCls), "nativeHandleColorSelected", "(I)V");
            if (mid) e->CallStaticVoidMethod(static_cast<jclass>(g_colorDialogCls), mid, static_cast<jint>(0xFF000000 | c.rgb()));
        });
    }
    if (title) {
        const char* t = env->GetStringUTFChars(title, nullptr);
        if (t) { g_colorDialog->setWindowTitle(QString::fromUtf8(t)); env->ReleaseStringUTFChars(title, t); }
    }
    if (argb != -1) g_colorDialog->setCurrentColor(QColor::fromRgba(static_cast<QRgb>(argb)));
    g_colorDialog->open();
}

JNIEXPORT void JNICALL Java_org_jqt_QColorDialog_nativeConnectColorSelected(JNIEnv* env, jclass cls) {
    if (!g_colorDialogCls) g_colorDialogCls = env->NewGlobalRef(cls);
}

// QWidget：find / layout / setWindowIcon
JNIEXPORT jlong JNICALL Java_org_jqt_QWidget_nativeFind(JNIEnv* env, jclass, jlong winId) {
    (void)env;
    if (winId <= 0) return 0;
    QWidget* w = QWidget::find(static_cast<WId>(winId));
    if (!w) return 0;
    std::lock_guard<std::mutex> lock(g_handleMutex);
    for (const auto& kv : g_handles) {
        if (kv.second == w) return static_cast<jlong>(kv.first);
    }
    return 0;
}

JNIEXPORT jlong JNICALL Java_org_jqt_QWidget_nativeLayout(JNIEnv* env, jobject, jlong handle) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (!w || !w->layout()) return 0;
    QLayout* l = w->layout();
    std::lock_guard<std::mutex> lock(g_handleMutex);
    for (const auto& kv : g_handles) {
        if (kv.second == l) return static_cast<jlong>(kv.first);
    }
    return 0;
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetWindowIcon(JNIEnv* env, jobject, jlong handle, jstring iconPath) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (!w) return;
    const char* t = iconPath ? env->GetStringUTFChars(iconPath, nullptr) : nullptr;
    w->setWindowIcon(t ? QIcon(QString::fromUtf8(t)) : QIcon());
    if (t) env->ReleaseStringUTFChars(iconPath, t);
}

// QMessageBox 实例化（Qt 风格 setText/setIcon/exec/open）
JNIEXPORT jlong JNICALL Java_org_jqt_QMessageBox_nativeCreate(JNIEnv* env, jobject) {
    if (requireApp(env) == nullptr) return 0;
    QMessageBox* box = new QMessageBox();
    return registerHandle(box, /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_QMessageBox_nativeSetText(JNIEnv* env, jobject, jlong handle, jstring text) {
    QMessageBox* box = static_cast<QMessageBox*>(requireHandle(env, handle));
    if (!box) return;
    const char* t = text ? env->GetStringUTFChars(text, nullptr) : nullptr;
    box->setText(t ? QString::fromUtf8(t) : QString());
    if (t) env->ReleaseStringUTFChars(text, t);
}

JNIEXPORT void JNICALL Java_org_jqt_QMessageBox_nativeSetWindowTitle(JNIEnv* env, jobject, jlong handle, jstring title) {
    QMessageBox* box = static_cast<QMessageBox*>(requireHandle(env, handle));
    if (!box) return;
    const char* t = title ? env->GetStringUTFChars(title, nullptr) : nullptr;
    box->setWindowTitle(t ? QString::fromUtf8(t) : QString());
    if (t) env->ReleaseStringUTFChars(title, t);
}

JNIEXPORT void JNICALL Java_org_jqt_QMessageBox_nativeSetIcon(JNIEnv* env, jobject, jlong handle, jint icon) {
    QMessageBox* box = static_cast<QMessageBox*>(requireHandle(env, handle));
    if (!box) return;
    box->setIcon(static_cast<QMessageBox::Icon>(icon));
}

JNIEXPORT jint JNICALL Java_org_jqt_QMessageBox_nativeExec(JNIEnv* env, jobject, jlong handle) {
    QMessageBox* box = static_cast<QMessageBox*>(requireHandle(env, handle));
    if (!box) return 0;
    box->setStandardButtons(QMessageBox::Ok | QMessageBox::Cancel);
    return box->exec() == QMessageBox::Ok ? 1 : 0;
}

JNIEXPORT void JNICALL Java_org_jqt_QMessageBox_nativeOpen(JNIEnv* env, jobject, jlong handle) {
    QMessageBox* box = static_cast<QMessageBox*>(requireHandle(env, handle));
    if (!box) return;
    box->setStandardButtons(QMessageBox::Ok | QMessageBox::Cancel);
    box->open();
}

JNIEXPORT void JNICALL Java_org_jqt_QMessageBox_nativeClose(JNIEnv* env, jobject, jlong handle) {
    QMessageBox* box = static_cast<QMessageBox*>(requireHandle(env, handle));
    if (box) box->close();
}

// ============================================================================
// L1 收尾批 2（v0.7.1）：QAction / QDialog / QMenuBar / QListView 新类
// ============================================================================

// ---- QAction ----
static std::unordered_map<jlong, QString> g_actionIconPaths;
JNIEXPORT jlong JNICALL Java_org_jqt_QAction_nativeCreate(JNIEnv* env, jobject thiz, jstring text) {
    if (requireApp(env) == nullptr) return 0;
    const char* t = text ? env->GetStringUTFChars(text, nullptr) : nullptr;
    QAction* a = new QAction(t ? QString::fromUtf8(t) : QString());
    if (t) env->ReleaseStringUTFChars(text, t);
    const jlong h = registerHandle(a, /*javaOwned=*/true);
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(a, &QAction::triggered, [gRef](bool) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleTriggered", "()V");
        if (mid) e->CallVoidMethod(gRef, mid);
    });
    QObject::connect(a, &QAction::toggled, [gRef](bool checked) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleToggled", "(Z)V");
        if (mid) e->CallVoidMethod(gRef, mid, checked ? JNI_TRUE : JNI_FALSE);
    });
    return h;
}

JNIEXPORT void JNICALL Java_org_jqt_QAction_nativeSetText(JNIEnv* env, jobject, jlong handle, jstring text) {
    QAction* a = static_cast<QAction*>(requireHandle(env, handle));
    if (!a) return;
    const char* t = text ? env->GetStringUTFChars(text, nullptr) : nullptr;
    a->setText(t ? QString::fromUtf8(t) : QString());
    if (t) env->ReleaseStringUTFChars(text, t);
}

JNIEXPORT jstring JNICALL Java_org_jqt_QAction_nativeText(JNIEnv* env, jobject, jlong handle) {
    QAction* a = static_cast<QAction*>(requireHandle(env, handle));
    if (!a) return nullptr;
    return env->NewStringUTF(a->text().toUtf8().constData());
}

JNIEXPORT void JNICALL Java_org_jqt_QAction_nativeSetIcon(JNIEnv* env, jobject, jlong handle, jstring iconPath) {
    QAction* a = static_cast<QAction*>(requireHandle(env, handle));
    if (!a) return;
    const char* t = iconPath ? env->GetStringUTFChars(iconPath, nullptr) : nullptr;
    const QString p = t ? QString::fromUtf8(t) : QString();
    if (t) env->ReleaseStringUTFChars(iconPath, t);
    a->setIcon(QIcon(p));
    if (p.isEmpty()) g_actionIconPaths.erase(handle); else g_actionIconPaths[handle] = p;
}

JNIEXPORT jstring JNICALL Java_org_jqt_QAction_nativeIcon(JNIEnv* env, jobject, jlong handle) {
    auto it = g_actionIconPaths.find(handle);
    if (it == g_actionIconPaths.end()) return nullptr;
    return env->NewStringUTF(it->second.toUtf8().constData());
}

JNIEXPORT void JNICALL Java_org_jqt_QAction_nativeSetShortcut(JNIEnv* env, jobject, jlong handle, jstring shortcut) {
    QAction* a = static_cast<QAction*>(requireHandle(env, handle));
    if (!a) return;
    const char* t = shortcut ? env->GetStringUTFChars(shortcut, nullptr) : nullptr;
    if (t) { a->setShortcut(QKeySequence::fromString(QString::fromUtf8(t))); env->ReleaseStringUTFChars(shortcut, t); }
    else a->setShortcut(QKeySequence());
}

JNIEXPORT jstring JNICALL Java_org_jqt_QAction_nativeShortcut(JNIEnv* env, jobject, jlong handle) {
    QAction* a = static_cast<QAction*>(requireHandle(env, handle));
    if (!a) return nullptr;
    const QString s = a->shortcut().toString();
    return s.isEmpty() ? nullptr : env->NewStringUTF(s.toUtf8().constData());
}

JNIEXPORT void JNICALL Java_org_jqt_QAction_nativeSetToolTip(JNIEnv* env, jobject, jlong handle, jstring tip) {
    QAction* a = static_cast<QAction*>(requireHandle(env, handle));
    if (!a) return;
    const char* t = tip ? env->GetStringUTFChars(tip, nullptr) : nullptr;
    a->setToolTip(t ? QString::fromUtf8(t) : QString());
    if (t) env->ReleaseStringUTFChars(tip, t);
}

JNIEXPORT jstring JNICALL Java_org_jqt_QAction_nativeToolTip(JNIEnv* env, jobject, jlong handle) {
    QAction* a = static_cast<QAction*>(requireHandle(env, handle));
    if (!a) return nullptr;
    return env->NewStringUTF(a->toolTip().toUtf8().constData());
}

JNIEXPORT void JNICALL Java_org_jqt_QAction_nativeSetFont(JNIEnv* env, jobject, jlong handle, jstring family, jint pointSize) {
    QAction* a = static_cast<QAction*>(requireHandle(env, handle));
    if (!a) return;
    QFont f = a->font();
    const char* t = family ? env->GetStringUTFChars(family, nullptr) : nullptr;
    if (t) { f.setFamily(QString::fromUtf8(t)); env->ReleaseStringUTFChars(family, t); }
    if (pointSize > 0) f.setPointSize(pointSize);
    a->setFont(f);
}

JNIEXPORT void JNICALL Java_org_jqt_QAction_nativeSetCheckable(JNIEnv* env, jobject, jlong handle, jboolean checkable) {
    QAction* a = static_cast<QAction*>(requireHandle(env, handle));
    if (a) a->setCheckable(checkable ? true : false);
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QAction_nativeIsChecked(JNIEnv* env, jobject, jlong handle) {
    QAction* a = static_cast<QAction*>(requireHandle(env, handle));
    return (a && a->isChecked()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_org_jqt_QAction_nativeSetChecked(JNIEnv* env, jobject, jlong handle, jboolean checked) {
    QAction* a = static_cast<QAction*>(requireHandle(env, handle));
    if (a) a->setChecked(checked ? true : false);
}

JNIEXPORT void JNICALL Java_org_jqt_QAction_nativeToggle(JNIEnv* env, jobject, jlong handle) {
    QAction* a = static_cast<QAction*>(requireHandle(env, handle));
    if (a) a->toggle();
}

JNIEXPORT void JNICALL Java_org_jqt_QAction_nativeTrigger(JNIEnv* env, jobject, jlong handle) {
    QAction* a = static_cast<QAction*>(requireHandle(env, handle));
    if (a) a->trigger();
}

JNIEXPORT void JNICALL Java_org_jqt_QAction_nativeSetMenu(JNIEnv* env, jobject, jlong handle, jlong menuHandle) {
    QAction* a = static_cast<QAction*>(requireHandle(env, handle));
    QMenu* m = static_cast<QMenu*>(requireHandle(env, menuHandle));
    if (a && m) a->setMenu(m);
}

JNIEXPORT void JNICALL Java_org_jqt_QAction_nativeConnectTriggered(JNIEnv* env, jobject, jlong) { (void)env; }
JNIEXPORT void JNICALL Java_org_jqt_QAction_nativeConnectToggled(JNIEnv* env, jobject, jlong) { (void)env; }

// ---- QDialog ----
JNIEXPORT jlong JNICALL Java_org_jqt_QDialog_nativeCreate(JNIEnv* env, jobject, jstring title, jlong parentHandle) {
    if (requireApp(env) == nullptr) return 0;
    QWidget* parent = parentHandle ? static_cast<QWidget*>(requireHandle(env, parentHandle)) : nullptr;
    QDialog* dlg = new QDialog(parent);
    const char* t = title ? env->GetStringUTFChars(title, nullptr) : nullptr;
    if (t) { dlg->setWindowTitle(QString::fromUtf8(t)); env->ReleaseStringUTFChars(title, t); }
    return registerHandle(dlg, /*javaOwned=*/true);
}

JNIEXPORT jint JNICALL Java_org_jqt_QDialog_nativeExec(JNIEnv* env, jobject, jlong handle) {
    QDialog* dlg = static_cast<QDialog*>(requireHandle(env, handle));
    if (!dlg) return 0;
    return dlg->exec() == QDialog::Accepted ? 1 : 0;
}

JNIEXPORT void JNICALL Java_org_jqt_QDialog_nativeOpen(JNIEnv* env, jobject, jlong handle) {
    QDialog* dlg = static_cast<QDialog*>(requireHandle(env, handle));
    if (dlg) dlg->open();
}

JNIEXPORT void JNICALL Java_org_jqt_QDialog_nativeAccept(JNIEnv* env, jobject, jlong handle) {
    QDialog* dlg = static_cast<QDialog*>(requireHandle(env, handle));
    if (dlg) dlg->accept();
}

JNIEXPORT void JNICALL Java_org_jqt_QDialog_nativeReject(JNIEnv* env, jobject, jlong handle) {
    QDialog* dlg = static_cast<QDialog*>(requireHandle(env, handle));
    if (dlg) dlg->reject();
}

// ---- QMenuBar ----
JNIEXPORT jlong JNICALL Java_org_jqt_QMenuBar_nativeCreate(JNIEnv* env, jobject thiz) {
    if (requireApp(env) == nullptr) return 0;
    QMenuBar* bar = new QMenuBar();
    const jlong h = registerHandle(bar, /*javaOwned=*/true);
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(bar, &QMenuBar::triggered, [gRef](QAction* action) {
        if (!action) return;
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleTriggered", "(I)V");
        if (mid) e->CallVoidMethod(gRef, mid, static_cast<jint>(action->data().toInt()));
    });
    return h;
}

JNIEXPORT void JNICALL Java_org_jqt_QMenuBar_nativeAddMenu(JNIEnv* env, jobject, jlong handle, jlong menuHandle) {
    QMenuBar* bar = static_cast<QMenuBar*>(requireHandle(env, handle));
    QMenu* m = static_cast<QMenu*>(requireHandle(env, menuHandle));
    if (bar && m) bar->addMenu(m);
}

JNIEXPORT void JNICALL Java_org_jqt_QMenuBar_nativeClear(JNIEnv* env, jobject, jlong handle) {
    QMenuBar* bar = static_cast<QMenuBar*>(requireHandle(env, handle));
    if (bar) bar->clear();
}

// ---- QListView（QStringListModel 内部实现）----
JNIEXPORT jlong JNICALL Java_org_jqt_QListView_nativeCreate(JNIEnv* env, jobject thiz) {
    if (requireApp(env) == nullptr) return 0;
    QListView* view = new QListView();
    QStringListModel* model = new QStringListModel(view);
    view->setModel(model);
    const jlong h = registerHandle(view, /*javaOwned=*/true);
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(view->selectionModel(), &QItemSelectionModel::selectionChanged, [gRef](const QItemSelection& sel) {
        JNIEnv* e = callbackEnv();
        QModelIndexList idxs = sel.indexes();
        QStringList texts;
        for (const QModelIndex& ix : idxs) texts << ix.data().toString();
        jclass cls = e->GetObjectClass(gRef);
        jclass strCls = e->FindClass("java/lang/String");
        jobjectArray arr = e->NewObjectArray(static_cast<jsize>(texts.size()), strCls, nullptr);
        for (int i = 0; i < texts.size(); i++) {
            jstring js = e->NewStringUTF(texts.at(i).toUtf8().constData());
            e->SetObjectArrayElement(arr, i, js);
            e->DeleteLocalRef(js);
        }
        jmethodID mid = e->GetMethodID(cls, "nativeHandleSelectionChanged", "([Ljava/lang/String;)V");
        if (mid) e->CallVoidMethod(gRef, mid, arr);
        e->DeleteLocalRef(arr);
    });
    return h;
}

JNIEXPORT void JNICALL Java_org_jqt_QListView_nativeAddItem(JNIEnv* env, jobject, jlong handle, jstring text) {
    QListView* view = static_cast<QListView*>(requireHandle(env, handle));
    if (!view) return;
    const char* t = text ? env->GetStringUTFChars(text, nullptr) : nullptr;
    if (!t) return;
    QStringListModel* model = static_cast<QStringListModel*>(view->model());
    if (model) { QStringList l = model->stringList(); l << QString::fromUtf8(t); model->setStringList(l); }
    env->ReleaseStringUTFChars(text, t);
}

JNIEXPORT void JNICALL Java_org_jqt_QListView_nativeSetItems(JNIEnv* env, jobject, jlong handle, jobjectArray items) {
    QListView* view = static_cast<QListView*>(requireHandle(env, handle));
    if (!view) return;
    QStringList list;
    if (items) {
        const jsize n = env->GetArrayLength(items);
        for (jsize i = 0; i < n; i++) {
            jstring js = static_cast<jstring>(env->GetObjectArrayElement(items, i));
            if (!js) continue;
            const char* t = env->GetStringUTFChars(js, nullptr);
            if (t) { list << QString::fromUtf8(t); env->ReleaseStringUTFChars(js, t); }
            env->DeleteLocalRef(js);
        }
    }
    QStringListModel* model = static_cast<QStringListModel*>(view->model());
    if (model) model->setStringList(list);
}

JNIEXPORT jstring JNICALL Java_org_jqt_QListView_nativeItem(JNIEnv* env, jobject, jlong handle, jint row) {
    QListView* view = static_cast<QListView*>(requireHandle(env, handle));
    if (!view) return nullptr;
    QStringListModel* model = static_cast<QStringListModel*>(view->model());
    if (!model || row < 0 || row >= model->rowCount()) return nullptr;
    return env->NewStringUTF(model->data(model->index(row), Qt::DisplayRole).toString().toUtf8().constData());
}

JNIEXPORT jint JNICALL Java_org_jqt_QListView_nativeCount(JNIEnv* env, jobject, jlong handle) {
    QListView* view = static_cast<QListView*>(requireHandle(env, handle));
    if (!view) return 0;
    QStringListModel* model = static_cast<QStringListModel*>(view->model());
    return model ? static_cast<jint>(model->rowCount()) : 0;
}

JNIEXPORT void JNICALL Java_org_jqt_QListView_nativeClear(JNIEnv* env, jobject, jlong handle) {
    QListView* view = static_cast<QListView*>(requireHandle(env, handle));
    if (!view) return;
    QStringListModel* model = static_cast<QStringListModel*>(view->model());
    if (model) model->setStringList(QStringList());
}

JNIEXPORT void JNICALL Java_org_jqt_QListView_nativeSetSpacing(JNIEnv* env, jobject, jlong handle, jint spacing) {
    QListView* view = static_cast<QListView*>(requireHandle(env, handle));
    if (view) view->setSpacing(spacing);
}

JNIEXPORT jint JNICALL Java_org_jqt_QListView_nativeSpacing(JNIEnv* env, jobject, jlong handle) {
    QListView* view = static_cast<QListView*>(requireHandle(env, handle));
    return view ? static_cast<jint>(view->spacing()) : 0;
}

JNIEXPORT void JNICALL Java_org_jqt_QListView_nativeSetWordWrap(JNIEnv* env, jobject, jlong handle, jboolean wrap) {
    QListView* view = static_cast<QListView*>(requireHandle(env, handle));
    if (view) view->setWordWrap(wrap ? true : false);
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QListView_nativeWordWrap(JNIEnv* env, jobject, jlong handle) {
    QListView* view = static_cast<QListView*>(requireHandle(env, handle));
    return (view && view->wordWrap()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL Java_org_jqt_QListView_nativeCurrentItem(JNIEnv* env, jobject, jlong handle) {
    QListView* view = static_cast<QListView*>(requireHandle(env, handle));
    if (!view) return nullptr;
    QModelIndex ix = view->currentIndex();
    return ix.isValid() ? env->NewStringUTF(ix.data().toString().toUtf8().constData()) : nullptr;
}

JNIEXPORT void JNICALL Java_org_jqt_QListView_nativeSetCurrentItem(JNIEnv* env, jobject, jlong handle, jstring text) {
    QListView* view = static_cast<QListView*>(requireHandle(env, handle));
    if (!view) return;
    QStringListModel* model = static_cast<QStringListModel*>(view->model());
    if (!model) return;
    const char* t = text ? env->GetStringUTFChars(text, nullptr) : nullptr;
    if (!t) return;
    const QString needle = QString::fromUtf8(t);
    env->ReleaseStringUTFChars(text, t);
    for (int i = 0; i < model->rowCount(); i++) {
        if (model->data(model->index(i), Qt::DisplayRole).toString() == needle) {
            view->setCurrentIndex(model->index(i));
            return;
        }
    }
}

JNIEXPORT void JNICALL Java_org_jqt_QListView_nativeConnectSelectionChanged(JNIEnv* env, jobject, jlong) { (void)env; }

// ---- QColor 极简工具（v0.7.1 L1 补全）----
static QColor jqtColorFromHex(JNIEnv* env, jstring hex) {
    if (!hex) return QColor();
    const char* t = env->GetStringUTFChars(hex, nullptr);
    if (!t) return QColor();
    QColor c(QString::fromUtf8(t));
    env->ReleaseStringUTFChars(hex, t);
    return c;
}

JNIEXPORT jint JNICALL Java_org_jqt_QColor_nativeValue(JNIEnv* env, jclass, jstring hex) {
    return static_cast<jint>(jqtColorFromHex(env, hex).value());
}

JNIEXPORT jint JNICALL Java_org_jqt_QColor_nativeHue(JNIEnv* env, jclass, jstring hex) {
    QColor c = jqtColorFromHex(env, hex);
    return c.isValid() ? static_cast<jint>(c.hue()) : -1;
}

JNIEXPORT jint JNICALL Java_org_jqt_QColor_nativeSaturation(JNIEnv* env, jclass, jstring hex) {
    return static_cast<jint>(jqtColorFromHex(env, hex).saturation());
}

// ---- QPalette 简化查询（v0.7.1 L1 补全）----
JNIEXPORT jint JNICALL Java_org_jqt_QApplication_nativePaletteText(JNIEnv* env, jclass) {
    (void)env;
    return static_cast<jint>(0xFF000000 | QApplication::palette().color(QPalette::Text).rgb());
}

JNIEXPORT jint JNICALL Java_org_jqt_QApplication_nativePalettePlaceholderText(JNIEnv* env, jclass) {
    (void)env;
    return static_cast<jint>(0xFF000000 | QApplication::palette().color(QPalette::PlaceholderText).rgb());
}

// ============================================================================
// v0.7.2 工业模块：QPrinter（QtPrintSupport）+ QSql（Qt6Sql）
// ============================================================================

// ---- QPrinter（非 QObject：独立句柄表）----
static std::mutex g_printerMutex;
// 堆分配：避免 JVM 退出时静态析构顺序问题（QSqlDatabase 值对象二次释放崩溃）
static std::unordered_map<jlong, QPrinter*>* g_printers = new std::unordered_map<jlong, QPrinter*>();
static jlong g_nextPrinterId = 0x6000;

JNIEXPORT jlong JNICALL Java_org_jqt_QPrinter_nativeCreate(JNIEnv* env, jobject) {
    if (requireApp(env) == nullptr) return 0;
    QPrinter* p = new QPrinter();
    std::lock_guard<std::mutex> lock(g_printerMutex);
    const jlong id = g_nextPrinterId++;
    (*g_printers)[id] = p;
    return id;
}

static QPrinter* jqtPrinter(JNIEnv* env, jlong handle) {
    std::lock_guard<std::mutex> lock(g_printerMutex);
    auto it = g_printers->find(handle);
    return it == g_printers->end() ? nullptr : it->second;
}

JNIEXPORT void JNICALL Java_org_jqt_QPrinter_nativeDispose(JNIEnv* env, jobject, jlong handle) {
    QPrinter* p = nullptr;
    {
        std::lock_guard<std::mutex> lock(g_printerMutex);
        auto it = g_printers->find(handle);
        if (it != g_printers->end()) { p = it->second; g_printers->erase(it); }
    }
    delete p;
}

JNIEXPORT void JNICALL Java_org_jqt_QPrinter_nativeSetOutputFormat(JNIEnv* env, jobject, jlong handle, jint format) {
    QPrinter* p = jqtPrinter(env, handle);
    if (p) p->setOutputFormat(format == 1 ? QPrinter::PdfFormat : QPrinter::NativeFormat);
}

JNIEXPORT void JNICALL Java_org_jqt_QPrinter_nativeSetOutputFileName(JNIEnv* env, jobject, jlong handle, jstring path) {
    QPrinter* p = jqtPrinter(env, handle);
    if (!p) return;
    const char* t = path ? env->GetStringUTFChars(path, nullptr) : nullptr;
    p->setOutputFileName(t ? QString::fromUtf8(t) : QString());
    if (t) env->ReleaseStringUTFChars(path, t);
}

JNIEXPORT void JNICALL Java_org_jqt_QPrinter_nativeSetResolution(JNIEnv* env, jobject, jlong handle, jint dpi) {
    QPrinter* p = jqtPrinter(env, handle);
    if (p && dpi > 0) p->setResolution(dpi);
}

JNIEXPORT void JNICALL Java_org_jqt_QPrinter_nativeSetPageSize(JNIEnv* env, jobject, jlong handle, jint size) {
    QPrinter* p = jqtPrinter(env, handle);
    if (!p) return;
    QPageSize::PageSizeId id = QPageSize::A4;
    if (size == 1) id = QPageSize::A3;
    else if (size == 2) id = QPageSize::A5;
    else if (size == 3) id = QPageSize::Letter;
    else if (size == 4) id = QPageSize::Legal;
    p->setPageSize(QPageSize(id));
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QPrinter_nativeNewPage(JNIEnv* env, jobject, jlong handle) {
    QPrinter* p = jqtPrinter(env, handle);
    return (p && p->newPage()) ? JNI_TRUE : JNI_FALSE;
}

// QPlainTextEdit::print
JNIEXPORT jboolean JNICALL Java_org_jqt_QTextEdit_nativePrint(JNIEnv* env, jobject, jlong handle, jlong printerHandle) {
    QPlainTextEdit* w = static_cast<QPlainTextEdit*>(requireHandle(env, handle));
    QPrinter* p = jqtPrinter(env, printerHandle);
    if (!w || !p) return JNI_FALSE;
    w->print(p);
    return JNI_TRUE;
}

// QWidget::render → PDF
JNIEXPORT jboolean JNICALL Java_org_jqt_QWidget_nativePrintToPdf(JNIEnv* env, jobject, jlong handle, jstring path) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (!w) return JNI_FALSE;
    const char* t = path ? env->GetStringUTFChars(path, nullptr) : nullptr;
    if (!t) return JNI_FALSE;
    QPrinter printer(QPrinter::HighResolution);
    printer.setOutputFormat(QPrinter::PdfFormat);
    printer.setOutputFileName(QString::fromUtf8(t));
    env->ReleaseStringUTFChars(path, t);
    if (w->width() > 0 && w->height() > 0) {
        // Qt 6.11 无 DevicePixel 单位：控件像素按 1pt = 1px 映射（PDF 页面尺寸=控件尺寸）
        printer.setPageSize(QPageSize(QSizeF(w->width(), w->height()), QPageSize::Point));
    }
    w->render(&printer);
    return JNI_TRUE;
}

// ---- QSqlDatabase（QSqlDatabase 为值类：独立句柄表存值）----
static std::mutex g_sqlMutex;
static std::unordered_map<jlong, QSqlDatabase>* g_sqlDbs = new std::unordered_map<jlong, QSqlDatabase>();
static std::unordered_map<jlong, QSqlQuery*>* g_sqlQueries = new std::unordered_map<jlong, QSqlQuery*>();
static jlong g_nextSqlId = 0x7000;

JNIEXPORT jlong JNICALL Java_org_jqt_QSqlDatabase_nativeAddDatabase(JNIEnv* env, jclass, jstring driver, jstring connName) {
    if (requireApp(env) == nullptr) return 0;
    const char* d = driver ? env->GetStringUTFChars(driver, nullptr) : nullptr;
    const char* c = connName ? env->GetStringUTFChars(connName, nullptr) : nullptr;
    const QString drv = d ? QString::fromUtf8(d) : QString();
    const QString name = c ? QString::fromUtf8(c) : QString();
    if (d) env->ReleaseStringUTFChars(driver, d);
    if (c) env->ReleaseStringUTFChars(connName, c);
    if (drv.isEmpty()) return 0;
    // 驱动可用性预检 + 插件 workaround：避免 addDatabase 创建无效连接
    // （Qt 内部无效连接在 JVM 退出时析构崩溃——Linux typeinfo-for-QObject SIGSEGV）。
    if (!QSqlDatabase::isDriverAvailable(drv)) {
        // workaround：QFactoryLoader 在 Windows 上不搜索 PATH（libstdc++ 等依赖缺失时加载失败）。
        // QPluginLoader 预加载 + registerSqlDriver 手动注册驱动（Qt 公开 API）。
        const QString lower = drv.toLower();
        QString pluginPath;
#ifdef _WIN32
        const QString ext = QStringLiteral(".dll");
#elif defined(__linux__)
        const QString ext = QStringLiteral(".so");
#else
        const QString ext = QStringLiteral(".dylib");
#endif
        {
            // libraryPaths + 编译期 pluginsPath（macOS/Linux 的 Qt 安装插件目录）
            QStringList bases = QCoreApplication::libraryPaths();
            bases << QLibraryInfo::path(QLibraryInfo::PluginsPath);
            for (const QString& base : bases) {
                const QString cand = base + QStringLiteral("/sqldrivers/q") + lower + ext;
                if (QFile::exists(cand)) { pluginPath = cand; break; }
            }
        }
        if (!pluginPath.isEmpty()) {
            QPluginLoader pl(pluginPath);
            if (pl.load()) {
                QObject* inst = pl.instance();
                QSqlDriverPlugin* plugin = inst ? qobject_cast<QSqlDriverPlugin*>(inst) : nullptr;
                if (plugin) {
                    // 插件 create 的 key 带 Q 前缀（如 "QSQLITE"）
                    const QString pluginKey = QStringLiteral("Q") + drv.toUpper();
                    QSqlDriver* d2 = plugin->create(pluginKey);
                    if (d2) {
                        struct JQtDriverCreator : public QSqlDriverCreatorBase {
                            QSqlDriver* d;
                            explicit JQtDriverCreator(QSqlDriver* drv) : d(drv) {}
                            QSqlDriver* createObject() const override { return d; }
                        };
                        QSqlDatabase::registerSqlDriver(drv, new JQtDriverCreator(d2));
                    }
                }
            }
        }
        if (!QSqlDatabase::isDriverAvailable(drv)) {
            fprintf(stderr, "[JQt-sql] driver %s unavailable (plugin: %s)\n",
                    drv.toUtf8().constData(), pluginPath.toUtf8().constData());
            return 0;
        }
    }
    QSqlDatabase db = QSqlDatabase::addDatabase(drv, name);

    std::lock_guard<std::mutex> lock(g_sqlMutex);
    const jlong id = g_nextSqlId++;
    (*g_sqlDbs)[id] = db;
    return id;
}

static QSqlDatabase* jqtSqlDb(JNIEnv* env, jlong handle) {
    std::lock_guard<std::mutex> lock(g_sqlMutex);
    auto it = g_sqlDbs->find(handle);
    return it == g_sqlDbs->end() ? nullptr : &it->second;
}

JNIEXPORT void JNICALL Java_org_jqt_QSqlDatabase_nativeSetDatabaseName(JNIEnv* env, jobject, jlong handle, jstring name) {
    QSqlDatabase* db = jqtSqlDb(env, handle);
    if (!db) return;
    const char* t = name ? env->GetStringUTFChars(name, nullptr) : nullptr;
    db->setDatabaseName(t ? QString::fromUtf8(t) : QString());
    if (t) env->ReleaseStringUTFChars(name, t);
}

JNIEXPORT void JNICALL Java_org_jqt_QSqlDatabase_nativeSetUserName(JNIEnv* env, jobject, jlong handle, jstring user) {
    QSqlDatabase* db = jqtSqlDb(env, handle);
    if (!db) return;
    const char* t = user ? env->GetStringUTFChars(user, nullptr) : nullptr;
    db->setUserName(t ? QString::fromUtf8(t) : QString());
    if (t) env->ReleaseStringUTFChars(user, t);
}

JNIEXPORT void JNICALL Java_org_jqt_QSqlDatabase_nativeSetPassword(JNIEnv* env, jobject, jlong handle, jstring password) {
    QSqlDatabase* db = jqtSqlDb(env, handle);
    if (!db) return;
    const char* t = password ? env->GetStringUTFChars(password, nullptr) : nullptr;
    db->setPassword(t ? QString::fromUtf8(t) : QString());
    if (t) env->ReleaseStringUTFChars(password, t);
}

JNIEXPORT void JNICALL Java_org_jqt_QSqlDatabase_nativeSetHostName(JNIEnv* env, jobject, jlong handle, jstring host) {
    QSqlDatabase* db = jqtSqlDb(env, handle);
    if (!db) return;
    const char* t = host ? env->GetStringUTFChars(host, nullptr) : nullptr;
    db->setHostName(t ? QString::fromUtf8(t) : QString());
    if (t) env->ReleaseStringUTFChars(host, t);
}

JNIEXPORT void JNICALL Java_org_jqt_QSqlDatabase_nativeSetPort(JNIEnv* env, jobject, jlong handle, jint port) {
    QSqlDatabase* db = jqtSqlDb(env, handle);
    if (db && port > 0) db->setPort(port);
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QSqlDatabase_nativeOpen(JNIEnv* env, jobject, jlong handle) {
    QSqlDatabase* db = jqtSqlDb(env, handle);
    return (db && db->open()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_org_jqt_QSqlDatabase_nativeClose(JNIEnv* env, jobject, jlong handle) {
    QSqlDatabase* db = jqtSqlDb(env, handle);
    if (db) db->close();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QSqlDatabase_nativeIsOpen(JNIEnv* env, jobject, jlong handle) {
    QSqlDatabase* db = jqtSqlDb(env, handle);
    return (db && db->isOpen()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jlong JNICALL Java_org_jqt_QSqlDatabase_nativeExec(JNIEnv* env, jobject, jlong handle, jstring sql) {
    QSqlDatabase* db = jqtSqlDb(env, handle);
    if (!db) return 0;
    const char* t = sql ? env->GetStringUTFChars(sql, nullptr) : nullptr;
    if (!t) return 0;
    QSqlQuery* q = new QSqlQuery(QString::fromUtf8(t), *db);
    env->ReleaseStringUTFChars(sql, t);
    std::lock_guard<std::mutex> lock(g_sqlMutex);
    const jlong id = g_nextSqlId++;
    (*g_sqlQueries)[id] = q;
    return id;
}

JNIEXPORT jstring JNICALL Java_org_jqt_QSqlDatabase_nativeLastError(JNIEnv* env, jobject, jlong handle) {
    QSqlDatabase* db = jqtSqlDb(env, handle);
    if (!db) return env->NewStringUTF("");
    const QString err = db->lastError().text();
    return env->NewStringUTF(err.toUtf8().constData());
}

JNIEXPORT void JNICALL Java_org_jqt_QSqlDatabase_nativeDispose(JNIEnv* env, jobject, jlong handle) {
    std::lock_guard<std::mutex> lock(g_sqlMutex);
    g_sqlDbs->erase(handle);
}

// ---- QSqlQuery ----

static QSqlQuery* jqtSqlQuery(JNIEnv* env, jlong handle) {
    std::lock_guard<std::mutex> lock(g_sqlMutex);
    auto it = g_sqlQueries->find(handle);
    return it == g_sqlQueries->end() ? nullptr : it->second;
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QSqlQuery_nativeNext(JNIEnv* env, jobject, jlong handle) {
    QSqlQuery* q = jqtSqlQuery(env, handle);
    return (q && q->next()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_org_jqt_QSqlQuery_nativeValueCount(JNIEnv* env, jobject, jlong handle) {
    QSqlQuery* q = jqtSqlQuery(env, handle);
    return q ? static_cast<jint>(q->record().count()) : 0;
}

JNIEXPORT jstring JNICALL Java_org_jqt_QSqlQuery_nativeValue(JNIEnv* env, jobject, jlong handle, jint index) {
    QSqlQuery* q = jqtSqlQuery(env, handle);
    if (!q) return nullptr;
    const QVariant v = q->value(index);
    if (v.isNull()) return nullptr;
    return env->NewStringUTF(v.toString().toUtf8().constData());
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QSqlQuery_nativeIsSelect(JNIEnv* env, jobject, jlong handle) {
    QSqlQuery* q = jqtSqlQuery(env, handle);
    return (q && q->isSelect()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_org_jqt_QSqlQuery_nativeNumRowsAffected(JNIEnv* env, jobject, jlong handle) {
    QSqlQuery* q = jqtSqlQuery(env, handle);
    return q ? static_cast<jint>(q->numRowsAffected()) : -1;
}

JNIEXPORT jstring JNICALL Java_org_jqt_QSqlQuery_nativeLastError(JNIEnv* env, jobject, jlong handle) {
    QSqlQuery* q = jqtSqlQuery(env, handle);
    if (!q) return env->NewStringUTF("");
    const QString err = q->lastError().text();
    return env->NewStringUTF(err.toUtf8().constData());
}

JNIEXPORT void JNICALL Java_org_jqt_QSqlQuery_nativeDispose(JNIEnv* env, jobject, jlong handle) {
    QSqlQuery* q = nullptr;
    {
        std::lock_guard<std::mutex> lock(g_sqlMutex);
        auto it = g_sqlQueries->find(handle);
        if (it != g_sqlQueries->end()) { q = it->second; g_sqlQueries->erase(it); }
    }
    delete q;
}

// ============================================================================
// v0.7.3：QOpenGLWidget 绑定（Qt6OpenGLWidgets）——通用 GL 画布
// ============================================================================

// macOS / Windows ARM64 的 Qt 构建不含 OpenGLWidgets 模块：存根降级
#if (defined(_WIN32) && !defined(_M_ARM64)) || defined(__linux__)
// QOpenGLWidget 子类：initializeGL/paintGL/resizeGL 回调到 Java
class JQtGLWidget : public QOpenGLWidget {
public:
    explicit JQtGLWidget() : QOpenGLWidget() {}
    jobject gRef = nullptr;
    QRgb clearColor = 0xFF000000;
    bool autoClear = true;

protected:
    void initializeGL() override {
        if (!gRef) return;
        JNIEnv* e = callbackEnv();
        if (!e) return;
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleInitialize", "()V");
        if (mid) e->CallVoidMethod(gRef, mid);
    }

    void paintGL() override {
        if (autoClear) {
            // GL 函数为运行时解析（QOpenGLFunctions），不能直接链接
            QOpenGLFunctions* f = QOpenGLContext::currentContext()->functions();
            f->glClearColor(qRed(clearColor) / 255.0f, qGreen(clearColor) / 255.0f,
                            qBlue(clearColor) / 255.0f, qAlpha(clearColor) / 255.0f);
            f->glClear(GL_COLOR_BUFFER_BIT);
        }
        if (!gRef) return;
        JNIEnv* e = callbackEnv();
        if (!e) return;
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandlePaint", "()V");
        if (mid) e->CallVoidMethod(gRef, mid);
    }

    void resizeGL(int w, int h) override {
        if (!gRef) return;
        JNIEnv* e = callbackEnv();
        if (!e) return;
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleResized", "(II)V");
        if (mid) e->CallVoidMethod(gRef, mid, w, h);
    }
};

JNIEXPORT jlong JNICALL Java_org_jqt_QOpenGLWidget_nativeCreate(JNIEnv* env, jobject thiz) {
    if (requireApp(env) == nullptr) return 0;
    JQtGLWidget* w = new JQtGLWidget();
    w->gRef = env->NewGlobalRef(thiz);
    return registerHandle(w, /*javaOwned=*/true);
}

JNIEXPORT void JNICALL Java_org_jqt_QOpenGLWidget_nativeSetClearColor(JNIEnv* env, jobject, jlong handle, jint argb) {
    JQtGLWidget* w = static_cast<JQtGLWidget*>(requireHandle(env, handle));
    if (w) w->clearColor = static_cast<QRgb>(argb);
}

JNIEXPORT void JNICALL Java_org_jqt_QOpenGLWidget_nativeSetAutoClear(JNIEnv* env, jobject, jlong handle, jboolean on) {
    JQtGLWidget* w = static_cast<JQtGLWidget*>(requireHandle(env, handle));
    if (w) w->autoClear = on ? true : false;
}

JNIEXPORT void JNICALL Java_org_jqt_QOpenGLWidget_nativeMakeCurrent(JNIEnv* env, jobject, jlong handle) {
    JQtGLWidget* w = static_cast<JQtGLWidget*>(requireHandle(env, handle));
    if (w) w->makeCurrent();
}

JNIEXPORT void JNICALL Java_org_jqt_QOpenGLWidget_nativeDoneCurrent(JNIEnv* env, jobject, jlong handle) {
    JQtGLWidget* w = static_cast<JQtGLWidget*>(requireHandle(env, handle));
    if (w) w->doneCurrent();
}
#else
// macOS / Windows ARM64 的 Qt 构建不含 OpenGLWidgets 模块：存根降级（Java 侧抛 UnsupportedOperationException）
JNIEXPORT jlong JNICALL Java_org_jqt_QOpenGLWidget_nativeCreate(JNIEnv* env, jobject) { (void)env; return 0; }
JNIEXPORT void JNICALL Java_org_jqt_QOpenGLWidget_nativeSetClearColor(JNIEnv* env, jobject, jlong, jint) { (void)env; }
JNIEXPORT void JNICALL Java_org_jqt_QOpenGLWidget_nativeSetAutoClear(JNIEnv* env, jobject, jlong, jboolean) { (void)env; }
JNIEXPORT void JNICALL Java_org_jqt_QOpenGLWidget_nativeMakeCurrent(JNIEnv* env, jobject, jlong) { (void)env; }
JNIEXPORT void JNICALL Java_org_jqt_QOpenGLWidget_nativeDoneCurrent(JNIEnv* env, jobject, jlong) { (void)env; }
#endif

// ============================================================================
// v0.7.4：QSerialPort（Qt SerialPort 模块）——完整串口绑定
// ============================================================================

JNIEXPORT jobjectArray JNICALL Java_org_jqt_QSerialPort_nativeAvailablePorts(JNIEnv* env, jclass) {
    const QList<QSerialPortInfo> infos = QSerialPortInfo::availablePorts();
    jclass strCls = env->FindClass("java/lang/String");
    jobjectArray arr = env->NewObjectArray(static_cast<jsize>(infos.size()), strCls, nullptr);
    for (int i = 0; i < infos.size(); i++) {
        jstring js = env->NewStringUTF(infos.at(i).portName().toUtf8().constData());
        env->SetObjectArrayElement(arr, i, js);
        env->DeleteLocalRef(js);
    }
    return arr;
}

JNIEXPORT jlong JNICALL Java_org_jqt_QSerialPort_nativeCreate(JNIEnv* env, jobject thiz) {
    if (requireApp(env) == nullptr) return 0;
    QSerialPort* port = new QSerialPort();
    const jlong h = registerHandle(port, /*javaOwned=*/true);
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(port, &QSerialPort::readyRead, [gRef]() {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleReadyRead", "()V");
        if (mid) e->CallVoidMethod(gRef, mid);
    });
    QObject::connect(port, &QSerialPort::bytesWritten, [gRef](qint64 n) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleBytesWritten", "(I)V");
        if (mid) e->CallVoidMethod(gRef, mid, static_cast<jint>(n));
    });
    return h;
}

// ----------------------------------------------------------------------------
// 值对象批 3：QTextEdit / QListWidget / QTabWidget / QTreeWidget / QApplication / QWidget 图标
// ----------------------------------------------------------------------------
JNIEXPORT void JNICALL Java_org_jqt_QTextEdit_nativeSetTextColor(JNIEnv* env, jobject /*thiz*/, jlong handle, jint argb) {
    QPlainTextEdit* w = static_cast<QPlainTextEdit*>(requireHandle(env, handle));
    if (w != nullptr) {
        QPalette pal = w->palette();
        pal.setColor(QPalette::Text, QColor::fromRgba(static_cast<QRgb>(argb)));
        w->setPalette(pal);
    }
}

JNIEXPORT void JNICALL Java_org_jqt_QTextEdit_nativeSetTextBackgroundColor(JNIEnv* env, jobject /*thiz*/, jlong handle, jint argb) {
    QPlainTextEdit* w = static_cast<QPlainTextEdit*>(requireHandle(env, handle));
    if (w != nullptr) {
        QPalette pal = w->palette();
        pal.setColor(QPalette::Base, QColor::fromRgba(static_cast<QRgb>(argb)));
        w->setPalette(pal);
    }
}

JNIEXPORT void JNICALL Java_org_jqt_QTextEdit_nativeSetTextMargins(JNIEnv* env, jobject /*thiz*/, jlong handle, jint left, jint top, jint right, jint bottom) {
    QPlainTextEdit* w = static_cast<QPlainTextEdit*>(requireHandle(env, handle));
    if (w != nullptr) w->setContentsMargins(left, top, right, bottom);
}

JNIEXPORT jint JNICALL Java_org_jqt_QTextEdit_nativeCursorPositionAt(JNIEnv* env, jobject /*thiz*/, jlong handle, jint x, jint y) {
    QPlainTextEdit* w = static_cast<QPlainTextEdit*>(requireHandle(env, handle));
    if (w == nullptr) return -1;
    return w->cursorForPosition(QPoint(x, y)).position();
}

JNIEXPORT void JNICALL Java_org_jqt_QListWidget_nativeSetItemIcon(JNIEnv* env, jobject /*thiz*/, jlong handle, jint row, jlong pmHandle) {
    QListWidget* w = static_cast<QListWidget*>(requireHandle(env, handle));
    QPixmap* pm = reinterpret_cast<QPixmap*>(pmHandle);
    if (w == nullptr || pm == nullptr || pm->isNull()) return;
    QListWidgetItem* item = w->item(row);
    if (item != nullptr) item->setIcon(QIcon(*pm));
}

JNIEXPORT void JNICALL Java_org_jqt_QTabWidget_nativeSetTabIcon(JNIEnv* env, jobject /*thiz*/, jlong handle, jint index, jlong pmHandle) {
    QTabWidget* w = static_cast<QTabWidget*>(requireHandle(env, handle));
    QPixmap* pm = reinterpret_cast<QPixmap*>(pmHandle);
    if (w == nullptr || pm == nullptr || pm->isNull()) return;
    w->setTabIcon(index, QIcon(*pm));
}

JNIEXPORT void JNICALL Java_org_jqt_QTreeWidget_nativeSetHeaderLabels(JNIEnv* env, jobject /*thiz*/, jlong handle, jobjectArray labels) {
    QTreeWidget* w = static_cast<QTreeWidget*>(requireHandle(env, handle));
    if (w == nullptr || labels == nullptr) return;
    QStringList list;
    jsize n = env->GetArrayLength(labels);
    for (jsize i = 0; i < n; i++) {
        jstring s = static_cast<jstring>(env->GetObjectArrayElement(labels, i));
        if (s != nullptr) {
            const char* utf = env->GetStringUTFChars(s, nullptr);
            list << QString::fromUtf8(utf);
            env->ReleaseStringUTFChars(s, utf);
            env->DeleteLocalRef(s);
        }
    }
    w->setHeaderLabels(list);
}

JNIEXPORT void JNICALL Java_org_jqt_QApplication_nativeSetOverrideCursor(JNIEnv* env, jclass /*cls*/, jint shape) {
    QApplication* app = requireApp(env);
    if (app == nullptr) return;
    QApplication::setOverrideCursor(static_cast<Qt::CursorShape>(shape));
}

JNIEXPORT void JNICALL Java_org_jqt_QApplication_nativeChangeOverrideCursor(JNIEnv* env, jclass /*cls*/, jint shape) {
    QApplication* app = requireApp(env);
    if (app == nullptr) return;
    QApplication::changeOverrideCursor(static_cast<Qt::CursorShape>(shape));
}

JNIEXPORT void JNICALL Java_org_jqt_QApplication_nativeRestoreOverrideCursor(JNIEnv* env, jclass /*cls*/) {
    QApplication* app = requireApp(env);
    if (app == nullptr) return;
    QApplication::restoreOverrideCursor();
}

JNIEXPORT jlong JNICALL Java_org_jqt_QApplication_nativeWidgetAt(JNIEnv* env, jclass /*cls*/, jint x, jint y) {
    QApplication* app = requireApp(env);
    if (app == nullptr) return 0;
    QWidget* w = QApplication::widgetAt(x, y);
    return w != nullptr ? reinterpret_cast<jlong>(w) : 0;
}

JNIEXPORT jlong JNICALL Java_org_jqt_QApplication_nativeTopLevelAt(JNIEnv* env, jclass /*cls*/, jint x, jint y) {
    QApplication* app = requireApp(env);
    if (app == nullptr) return 0;
    QWidget* w = QApplication::topLevelAt(QPoint(x, y));
    return w != nullptr ? reinterpret_cast<jlong>(w) : 0;
}

JNIEXPORT jlong JNICALL Java_org_jqt_QApplication_nativeScreenAt(JNIEnv* env, jclass /*cls*/, jint x, jint y) {
    QApplication* app = requireApp(env);
    if (app == nullptr) return 0;
    QScreen* s = QApplication::screenAt(QPoint(x, y));
    return s != nullptr ? reinterpret_cast<jlong>(s) : 0;
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetWindowIconPixmap(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong pmHandle) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    QPixmap* pm = reinterpret_cast<QPixmap*>(pmHandle);
    if (w == nullptr || pm == nullptr || pm->isNull()) return;
    w->setWindowIcon(QIcon(*pm));
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeConnectWindowIconChanged(JNIEnv* env, jobject thiz, jlong handle) {
    QWidget* w = static_cast<QWidget*>(requireHandle(env, handle));
    if (w == nullptr) return;
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(w, &QWidget::windowIconChanged, [gRef](const QIcon& icon) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleWindowIconChanged", "(J)V");
        if (mid != nullptr) {
            QPixmap pm = icon.pixmap(32, 32);
            jlong pmHandle = 0;
            if (!pm.isNull()) {
                pmHandle = reinterpret_cast<jlong>(new QPixmap(pm));
            }
            JQT_CALL_VOID(e, gRef, mid, pmHandle);
        }
    });
}


// ----------------------------------------------------------------------------
// QPixmap（非 QObject：句柄 = 指针本身，Cleaner 唯一释放路径）
// ----------------------------------------------------------------------------
JNIEXPORT jlong JNICALL Java_org_jqt_QPixmap_nativeCreate(JNIEnv* env, jclass) {
    if (requireApp(env) == nullptr) return 0;
    return reinterpret_cast<jlong>(new QPixmap());
}
JNIEXPORT jlong JNICALL Java_org_jqt_QPixmap_nativeCreateWH(JNIEnv* env, jclass, jint w, jint h) {
    if (requireApp(env) == nullptr) return 0;
    return reinterpret_cast<jlong>(new QPixmap(w, h));
}
JNIEXPORT jlong JNICALL Java_org_jqt_QPixmap_nativeCreateFromFile(JNIEnv* env, jclass, jstring file) {
    if (requireApp(env) == nullptr) return 0;
    const char* f = env->GetStringUTFChars(file, nullptr);
    QPixmap* p = new QPixmap(QString::fromUtf8(f));
    env->ReleaseStringUTFChars(file, f);
    return reinterpret_cast<jlong>(p);
}
JNIEXPORT void JNICALL Java_org_jqt_QPixmap_nativeDispose(JNIEnv*, jclass, jlong h) {
    delete reinterpret_cast<QPixmap*>(h);
}
JNIEXPORT jboolean JNICALL Java_org_jqt_QPixmap_nativeIsNull(JNIEnv*, jclass, jlong h) {
    return reinterpret_cast<QPixmap*>(h)->isNull();
}
JNIEXPORT jint JNICALL Java_org_jqt_QPixmap_nativeWidth(JNIEnv*, jclass, jlong h) {
    return reinterpret_cast<QPixmap*>(h)->width();
}
JNIEXPORT jint JNICALL Java_org_jqt_QPixmap_nativeHeight(JNIEnv*, jclass, jlong h) {
    return reinterpret_cast<QPixmap*>(h)->height();
}
JNIEXPORT jint JNICALL Java_org_jqt_QPixmap_nativeDepth(JNIEnv*, jclass, jlong h) {
    return reinterpret_cast<QPixmap*>(h)->depth();
}
JNIEXPORT void JNICALL Java_org_jqt_QPixmap_nativeFill(JNIEnv*, jclass, jlong h, jint argb) {
    reinterpret_cast<QPixmap*>(h)->fill(QRgb(argb));
}
JNIEXPORT jboolean JNICALL Java_org_jqt_QPixmap_nativeLoad(JNIEnv* env, jclass, jlong h, jstring file) {
    const char* f = env->GetStringUTFChars(file, nullptr);
    bool ok = reinterpret_cast<QPixmap*>(h)->load(QString::fromUtf8(f));
    env->ReleaseStringUTFChars(file, f);
    return ok;
}
JNIEXPORT jboolean JNICALL Java_org_jqt_QPixmap_nativeLoadFromData(JNIEnv* env, jclass, jlong h, jbyteArray data) {
    jsize n = env->GetArrayLength(data);
    jbyte* buf = env->GetByteArrayElements(data, nullptr);
    bool ok = reinterpret_cast<QPixmap*>(h)->loadFromData(reinterpret_cast<const uchar*>(buf), n);
    env->ReleaseByteArrayElements(data, buf, JNI_ABORT);
    return ok;
}
JNIEXPORT jboolean JNICALL Java_org_jqt_QPixmap_nativeSave(JNIEnv* env, jclass, jlong h, jstring file, jstring format, jint quality) {
    const char* f = env->GetStringUTFChars(file, nullptr);
    const char* fmt = format ? env->GetStringUTFChars(format, nullptr) : nullptr;
    bool ok = reinterpret_cast<QPixmap*>(h)->save(QString::fromUtf8(f), fmt ? fmt : nullptr, quality);
    env->ReleaseStringUTFChars(file, f);
    if (fmt) env->ReleaseStringUTFChars(format, fmt);
    return ok;
}
JNIEXPORT jlong JNICALL Java_org_jqt_QPixmap_nativeScaled(JNIEnv* env, jclass, jlong h, jint w, jint hh, jint mode) {
    if (requireApp(env) == nullptr) return 0;
    QPixmap* out = new QPixmap(reinterpret_cast<QPixmap*>(h)->scaled(
        QSize(w, hh), static_cast<Qt::AspectRatioMode>(mode)));
    return reinterpret_cast<jlong>(out);
}
JNIEXPORT jlong JNICALL Java_org_jqt_QPixmap_nativeToImage(JNIEnv* env, jclass, jlong h) {
    if (requireApp(env) == nullptr) return 0;
    return reinterpret_cast<jlong>(new QImage(reinterpret_cast<QPixmap*>(h)->toImage()));
}
JNIEXPORT jlong JNICALL Java_org_jqt_QPixmap_nativeFromImage(JNIEnv* env, jclass, jlong imgH) {
    if (requireApp(env) == nullptr) return 0;
    return reinterpret_cast<jlong>(new QPixmap(QPixmap::fromImage(*reinterpret_cast<QImage*>(imgH))));
}
JNIEXPORT jlong JNICALL Java_org_jqt_QPixmap_nativeCreateFromArgb(JNIEnv* env, jclass, jintArray argb, jint w, jint h) {
    if (requireApp(env) == nullptr) return 0;
    jint* px = env->GetIntArrayElements(argb, nullptr);
    QImage img(w, h, QImage::Format_ARGB32);
    memcpy(img.bits(), px, static_cast<size_t>(w) * h * 4);
    env->ReleaseIntArrayElements(argb, px, JNI_ABORT);
    QPixmap* p = new QPixmap(QPixmap::fromImage(img));
    return reinterpret_cast<jlong>(p);
}
JNIEXPORT void JNICALL Java_org_jqt_QPixmap_nativeGetArgb(JNIEnv* env, jclass, jlong h, jintArray argb, jint w, jint hh) {
    QImage img = reinterpret_cast<QPixmap*>(h)->toImage().convertToFormat(QImage::Format_ARGB32);
    jint* px = env->GetIntArrayElements(argb, nullptr);
    memcpy(px, img.bits(), static_cast<size_t>(w) * hh * 4);
    env->ReleaseIntArrayElements(argb, px, 0);
}

// ----------------------------------------------------------------------------
// QImage（非 QObject：句柄 = 指针本身）
// ----------------------------------------------------------------------------
JNIEXPORT jlong JNICALL Java_org_jqt_QImage_nativeCreate(JNIEnv* env, jclass) {
    if (requireApp(env) == nullptr) return 0;
    return reinterpret_cast<jlong>(new QImage());
}
JNIEXPORT jlong JNICALL Java_org_jqt_QImage_nativeCreateWHF(JNIEnv* env, jclass, jint w, jint h, jint fmt) {
    if (requireApp(env) == nullptr) return 0;
    return reinterpret_cast<jlong>(new QImage(w, h, static_cast<QImage::Format>(fmt)));
}
JNIEXPORT jlong JNICALL Java_org_jqt_QImage_nativeCreateFromFile(JNIEnv* env, jclass, jstring file) {
    if (requireApp(env) == nullptr) return 0;
    const char* f = env->GetStringUTFChars(file, nullptr);
    QImage* img = new QImage(QString::fromUtf8(f));
    env->ReleaseStringUTFChars(file, f);
    return reinterpret_cast<jlong>(img);
}
JNIEXPORT void JNICALL Java_org_jqt_QImage_nativeDispose(JNIEnv*, jclass, jlong h) {
    delete reinterpret_cast<QImage*>(h);
}
JNIEXPORT jboolean JNICALL Java_org_jqt_QImage_nativeIsNull(JNIEnv*, jclass, jlong h) {
    return reinterpret_cast<QImage*>(h)->isNull();
}
JNIEXPORT jint JNICALL Java_org_jqt_QImage_nativeWidth(JNIEnv*, jclass, jlong h) {
    return reinterpret_cast<QImage*>(h)->width();
}
JNIEXPORT jint JNICALL Java_org_jqt_QImage_nativeHeight(JNIEnv*, jclass, jlong h) {
    return reinterpret_cast<QImage*>(h)->height();
}
JNIEXPORT jint JNICALL Java_org_jqt_QImage_nativeFormat(JNIEnv*, jclass, jlong h) {
    return static_cast<jint>(reinterpret_cast<QImage*>(h)->format());
}
JNIEXPORT void JNICALL Java_org_jqt_QImage_nativeFill(JNIEnv*, jclass, jlong h, jint argb) {
    reinterpret_cast<QImage*>(h)->fill(QRgb(argb));
}
JNIEXPORT jboolean JNICALL Java_org_jqt_QImage_nativeLoad(JNIEnv* env, jclass, jlong h, jstring file) {
    const char* f = env->GetStringUTFChars(file, nullptr);
    bool ok = reinterpret_cast<QImage*>(h)->load(QString::fromUtf8(f));
    env->ReleaseStringUTFChars(file, f);
    return ok;
}
JNIEXPORT jboolean JNICALL Java_org_jqt_QImage_nativeLoadFromData(JNIEnv* env, jclass, jlong h, jbyteArray data) {
    jsize n = env->GetArrayLength(data);
    jbyte* buf = env->GetByteArrayElements(data, nullptr);
    bool ok = reinterpret_cast<QImage*>(h)->loadFromData(reinterpret_cast<const uchar*>(buf), n);
    env->ReleaseByteArrayElements(data, buf, JNI_ABORT);
    return ok;
}
JNIEXPORT jboolean JNICALL Java_org_jqt_QImage_nativeSave(JNIEnv* env, jclass, jlong h, jstring file, jstring format, jint quality) {
    const char* f = env->GetStringUTFChars(file, nullptr);
    const char* fmt = format ? env->GetStringUTFChars(format, nullptr) : nullptr;
    bool ok = reinterpret_cast<QImage*>(h)->save(QString::fromUtf8(f), fmt ? fmt : nullptr, quality);
    env->ReleaseStringUTFChars(file, f);
    if (fmt) env->ReleaseStringUTFChars(format, fmt);
    return ok;
}
JNIEXPORT jint JNICALL Java_org_jqt_QImage_nativePixel(JNIEnv*, jclass, jlong h, jint x, jint y) {
    return static_cast<jint>(reinterpret_cast<QImage*>(h)->pixel(x, y));
}
JNIEXPORT void JNICALL Java_org_jqt_QImage_nativeSetPixel(JNIEnv*, jclass, jlong h, jint x, jint y, jint rgb) {
    reinterpret_cast<QImage*>(h)->setPixel(x, y, QRgb(rgb));
}
JNIEXPORT jint JNICALL Java_org_jqt_QImage_nativePixelArgb(JNIEnv*, jclass, jlong h, jint x, jint y) {
    return static_cast<jint>(QColor(reinterpret_cast<QImage*>(h)->pixelColor(x, y)).rgba());
}
JNIEXPORT jlong JNICALL Java_org_jqt_QImage_nativeConvertToFormat(JNIEnv* env, jclass, jlong h, jint fmt) {
    if (requireApp(env) == nullptr) return 0;
    return reinterpret_cast<jlong>(new QImage(reinterpret_cast<QImage*>(h)->convertToFormat(static_cast<QImage::Format>(fmt))));
}
JNIEXPORT jlong JNICALL Java_org_jqt_QImage_nativeScaled(JNIEnv* env, jclass, jlong h, jint w, jint hh, jint mode) {
    if (requireApp(env) == nullptr) return 0;
    return reinterpret_cast<jlong>(new QImage(reinterpret_cast<QImage*>(h)->scaled(
        QSize(w, hh), static_cast<Qt::AspectRatioMode>(mode))));
}
JNIEXPORT jlong JNICALL Java_org_jqt_QImage_nativeCreateFromArgb(JNIEnv* env, jclass, jintArray argb, jint w, jint h) {
    if (requireApp(env) == nullptr) return 0;
    jint* px = env->GetIntArrayElements(argb, nullptr);
    QImage* img = new QImage(w, h, QImage::Format_ARGB32);
    memcpy(img->bits(), px, static_cast<size_t>(w) * h * 4);
    env->ReleaseIntArrayElements(argb, px, JNI_ABORT);
    return reinterpret_cast<jlong>(img);
}
JNIEXPORT void JNICALL Java_org_jqt_QImage_nativeGetArgb(JNIEnv* env, jclass, jlong h, jintArray argb, jint w, jint hh) {
    QImage img = reinterpret_cast<QImage*>(h)->convertToFormat(QImage::Format_ARGB32);
    jint* px = env->GetIntArrayElements(argb, nullptr);
    memcpy(px, img.bits(), static_cast<size_t>(w) * hh * 4);
    env->ReleaseIntArrayElements(argb, px, 0);
}

// ----------------------------------------------------------------------------
// QFont（非 QObject：句柄 = 指针本身）
// ----------------------------------------------------------------------------
JNIEXPORT jlong JNICALL Java_org_jqt_QFont_nativeCreate(JNIEnv* env, jclass) {
    if (requireApp(env) == nullptr) return 0;
    return reinterpret_cast<jlong>(new QFont());
}
JNIEXPORT jlong JNICALL Java_org_jqt_QFont_nativeCreateFamily(JNIEnv* env, jclass, jstring family) {
    if (requireApp(env) == nullptr) return 0;
    const char* f = env->GetStringUTFChars(family, nullptr);
    QFont* font = new QFont(QString::fromUtf8(f));
    env->ReleaseStringUTFChars(family, f);
    return reinterpret_cast<jlong>(font);
}
JNIEXPORT jlong JNICALL Java_org_jqt_QFont_nativeCreateFamilySize(JNIEnv* env, jclass, jstring family, jint size) {
    if (requireApp(env) == nullptr) return 0;
    const char* f = env->GetStringUTFChars(family, nullptr);
    QFont* font = new QFont(QString::fromUtf8(f), size);
    env->ReleaseStringUTFChars(family, f);
    return reinterpret_cast<jlong>(font);
}
JNIEXPORT jlong JNICALL Java_org_jqt_QFont_nativeCreateFull(JNIEnv* env, jclass, jstring family, jint size, jint weight, jboolean italic) {
    if (requireApp(env) == nullptr) return 0;
    const char* f = env->GetStringUTFChars(family, nullptr);
    QFont* font = new QFont(QString::fromUtf8(f), size, weight, italic);
    env->ReleaseStringUTFChars(family, f);
    return reinterpret_cast<jlong>(font);
}
JNIEXPORT void JNICALL Java_org_jqt_QFont_nativeDispose(JNIEnv*, jclass, jlong h) {
    delete reinterpret_cast<QFont*>(h);
}
JNIEXPORT jstring JNICALL Java_org_jqt_QFont_nativeFamily(JNIEnv* env, jclass, jlong h) {
    return env->NewStringUTF(reinterpret_cast<QFont*>(h)->family().toUtf8().constData());
}
JNIEXPORT void JNICALL Java_org_jqt_QFont_nativeSetFamily(JNIEnv* env, jclass, jlong h, jstring family) {
    const char* f = env->GetStringUTFChars(family, nullptr);
    reinterpret_cast<QFont*>(h)->setFamily(QString::fromUtf8(f));
    env->ReleaseStringUTFChars(family, f);
}
JNIEXPORT jint JNICALL Java_org_jqt_QFont_nativePointSize(JNIEnv*, jclass, jlong h) {
    return reinterpret_cast<QFont*>(h)->pointSize();
}
JNIEXPORT void JNICALL Java_org_jqt_QFont_nativeSetPointSize(JNIEnv*, jclass, jlong h, jint size) {
    reinterpret_cast<QFont*>(h)->setPointSize(size);
}
JNIEXPORT jdouble JNICALL Java_org_jqt_QFont_nativePointSizeF(JNIEnv*, jclass, jlong h) {
    return reinterpret_cast<QFont*>(h)->pointSizeF();
}
JNIEXPORT jint JNICALL Java_org_jqt_QFont_nativePixelSize(JNIEnv*, jclass, jlong h) {
    return reinterpret_cast<QFont*>(h)->pixelSize();
}
JNIEXPORT void JNICALL Java_org_jqt_QFont_nativeSetPixelSize(JNIEnv*, jclass, jlong h, jint size) {
    reinterpret_cast<QFont*>(h)->setPixelSize(size);
}
JNIEXPORT jboolean JNICALL Java_org_jqt_QFont_nativeBold(JNIEnv*, jclass, jlong h) {
    return reinterpret_cast<QFont*>(h)->bold();
}
JNIEXPORT void JNICALL Java_org_jqt_QFont_nativeSetBold(JNIEnv*, jclass, jlong h, jboolean b) {
    reinterpret_cast<QFont*>(h)->setBold(b);
}
JNIEXPORT jboolean JNICALL Java_org_jqt_QFont_nativeItalic(JNIEnv*, jclass, jlong h) {
    return reinterpret_cast<QFont*>(h)->italic();
}
JNIEXPORT void JNICALL Java_org_jqt_QFont_nativeSetItalic(JNIEnv*, jclass, jlong h, jboolean b) {
    reinterpret_cast<QFont*>(h)->setItalic(b);
}
JNIEXPORT jboolean JNICALL Java_org_jqt_QFont_nativeUnderline(JNIEnv*, jclass, jlong h) {
    return reinterpret_cast<QFont*>(h)->underline();
}
JNIEXPORT void JNICALL Java_org_jqt_QFont_nativeSetUnderline(JNIEnv*, jclass, jlong h, jboolean b) {
    reinterpret_cast<QFont*>(h)->setUnderline(b);
}
JNIEXPORT jboolean JNICALL Java_org_jqt_QFont_nativeStrikeOut(JNIEnv*, jclass, jlong h) {
    return reinterpret_cast<QFont*>(h)->strikeOut();
}
JNIEXPORT void JNICALL Java_org_jqt_QFont_nativeSetStrikeOut(JNIEnv*, jclass, jlong h, jboolean b) {
    reinterpret_cast<QFont*>(h)->setStrikeOut(b);
}
JNIEXPORT jint JNICALL Java_org_jqt_QFont_nativeWeight(JNIEnv*, jclass, jlong h) {
    return reinterpret_cast<QFont*>(h)->weight();
}
JNIEXPORT void JNICALL Java_org_jqt_QFont_nativeSetWeight(JNIEnv*, jclass, jlong h, jint w) {
    reinterpret_cast<QFont*>(h)->setWeight(QFont::Weight(w));
}
JNIEXPORT jstring JNICALL Java_org_jqt_QFont_nativeToString(JNIEnv* env, jclass, jlong h) {
    return env->NewStringUTF(reinterpret_cast<QFont*>(h)->toString().toUtf8().constData());
}


JNIEXPORT void JNICALL Java_org_jqt_QSerialPort_nativeDispose(JNIEnv* env, jobject, jlong handle) {
    QSerialPort* port = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (port) { port->close(); delete port; }
}

JNIEXPORT void JNICALL Java_org_jqt_QSerialPort_nativeSetPortName(JNIEnv* env, jobject, jlong handle, jstring name) {
    QSerialPort* port = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (!port) return;
    const char* t = name ? env->GetStringUTFChars(name, nullptr) : nullptr;
    port->setPortName(t ? QString::fromUtf8(t) : QString());
    if (t) env->ReleaseStringUTFChars(name, t);
}

JNIEXPORT jstring JNICALL Java_org_jqt_QSerialPort_nativePortName(JNIEnv* env, jobject, jlong handle) {
    QSerialPort* port = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (!port) return nullptr;
    return env->NewStringUTF(port->portName().toUtf8().constData());
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QSerialPort_nativeSetBaudRate(JNIEnv* env, jobject, jlong handle, jint baudRate) {
    QSerialPort* port = static_cast<QSerialPort*>(requireHandle(env, handle));
    return (port && port->setBaudRate(baudRate)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_org_jqt_QSerialPort_nativeBaudRate(JNIEnv* env, jobject, jlong handle) {
    QSerialPort* port = static_cast<QSerialPort*>(requireHandle(env, handle));
    return port ? static_cast<jint>(port->baudRate()) : 0;
}

JNIEXPORT void JNICALL Java_org_jqt_QSerialPort_nativeSetDataBits(JNIEnv* env, jobject, jlong handle, jint bits) {
    QSerialPort* port = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (port) port->setDataBits(static_cast<QSerialPort::DataBits>(bits));
}

JNIEXPORT void JNICALL Java_org_jqt_QSerialPort_nativeSetParity(JNIEnv* env, jobject, jlong handle, jint parity) {
    QSerialPort* port = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (port) port->setParity(static_cast<QSerialPort::Parity>(parity));
}

JNIEXPORT void JNICALL Java_org_jqt_QSerialPort_nativeSetStopBits(JNIEnv* env, jobject, jlong handle, jint bits) {
    QSerialPort* port = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (port) port->setStopBits(static_cast<QSerialPort::StopBits>(bits));
}

JNIEXPORT void JNICALL Java_org_jqt_QSerialPort_nativeSetFlowControl(JNIEnv* env, jobject, jlong handle, jint flow) {
    QSerialPort* port = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (port) port->setFlowControl(static_cast<QSerialPort::FlowControl>(flow));
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QSerialPort_nativeOpen(JNIEnv* env, jobject, jlong handle, jint mode) {
    QSerialPort* port = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (!port) return JNI_FALSE;
    QIODevice::OpenMode om = QIODevice::ReadOnly;
    if (mode == 1) om = QIODevice::WriteOnly;
    else if (mode == 2) om = QIODevice::ReadWrite;
    return port->open(om) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_org_jqt_QSerialPort_nativeClose(JNIEnv* env, jobject, jlong handle) {
    QSerialPort* port = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (port) port->close();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QSerialPort_nativeIsOpen(JNIEnv* env, jobject, jlong handle) {
    QSerialPort* port = static_cast<QSerialPort*>(requireHandle(env, handle));
    return (port && port->isOpen()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_org_jqt_QSerialPort_nativeWrite(JNIEnv* env, jobject, jlong handle, jbyteArray data) {
    QSerialPort* port = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (!port || !data) return -1;
    const jsize len = env->GetArrayLength(data);
    jbyte* buf = env->GetByteArrayElements(data, nullptr);
    if (!buf) return -1;
    const qint64 n = port->write(reinterpret_cast<const char*>(buf), static_cast<qint64>(len));
    env->ReleaseByteArrayElements(data, buf, JNI_ABORT);
    return static_cast<jint>(n);
}

JNIEXPORT jint JNICALL Java_org_jqt_QSerialPort_nativeWriteUtf8(JNIEnv* env, jobject, jlong handle, jstring text) {
    QSerialPort* port = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (!port || !text) return -1;
    const char* t = env->GetStringUTFChars(text, nullptr);
    if (!t) return -1;
    const qint64 n = port->write(t, static_cast<qint64>(strlen(t)));
    env->ReleaseStringUTFChars(text, t);
    return static_cast<jint>(n);
}

JNIEXPORT jbyteArray JNICALL Java_org_jqt_QSerialPort_nativeReadAll(JNIEnv* env, jobject, jlong handle) {
    QSerialPort* port = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (!port) return nullptr;
    const QByteArray data = port->readAll();
    jbyteArray out = env->NewByteArray(static_cast<jsize>(data.size()));
    if (!out) return nullptr;
    env->SetByteArrayRegion(out, 0, static_cast<jsize>(data.size()), reinterpret_cast<const jbyte*>(data.constData()));
    return out;
}

JNIEXPORT jstring JNICALL Java_org_jqt_QSerialPort_nativeReadLine(JNIEnv* env, jobject, jlong handle) {
    QSerialPort* port = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (!port) return nullptr;
    const QByteArray data = port->readLine();
    if (data.isEmpty()) return nullptr;
    return env->NewStringUTF(data.constData());
}

JNIEXPORT jint JNICALL Java_org_jqt_QSerialPort_nativeBytesAvailable(JNIEnv* env, jobject, jlong handle) {
    QSerialPort* port = static_cast<QSerialPort*>(requireHandle(env, handle));
    return port ? static_cast<jint>(port->bytesAvailable()) : 0;
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QSerialPort_nativeWaitForReadyRead(JNIEnv* env, jobject, jlong handle, jint timeoutMs) {
    QSerialPort* port = static_cast<QSerialPort*>(requireHandle(env, handle));
    return (port && port->waitForReadyRead(timeoutMs)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QSerialPort_nativeFlush(JNIEnv* env, jobject, jlong handle) {
    QSerialPort* port = static_cast<QSerialPort*>(requireHandle(env, handle));
    return (port && port->flush()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_org_jqt_QSerialPort_nativeClear(JNIEnv* env, jobject, jlong handle) {
    QSerialPort* port = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (port) port->clear();
}

JNIEXPORT jstring JNICALL Java_org_jqt_QSerialPort_nativeErrorString(JNIEnv* env, jobject, jlong handle) {
    QSerialPort* port = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (!port) return env->NewStringUTF("");
    return env->NewStringUTF(port->errorString().toUtf8().constData());
}

JNIEXPORT void JNICALL Java_org_jqt_QSerialPort_nativeConnectReadyRead(JNIEnv* env, jobject, jlong) { (void)env; }
JNIEXPORT void JNICALL Java_org_jqt_QSerialPort_nativeConnectBytesWritten(JNIEnv* env, jobject, jlong) { (void)env; }

// ============================================================================
// L2 批次（v0.7.4）：QWidget 高频少用 API
// ============================================================================

static jlong jqtPackSize(const QSize& s) {
    const int64_t packed = (static_cast<int64_t>(s.width()) << 32) | (static_cast<uint32_t>(s.height()));
    return static_cast<jlong>(packed);
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetMinimumSize(JNIEnv* env, jobject, jlong handle, jint w, jint h) {
    QWidget* wd = static_cast<QWidget*>(requireHandle(env, handle));
    if (wd) wd->setMinimumSize(w, h);
}
JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetMaximumSize(JNIEnv* env, jobject, jlong handle, jint w, jint h) {
    QWidget* wd = static_cast<QWidget*>(requireHandle(env, handle));
    if (wd) wd->setMaximumSize(w, h);
}
JNIEXPORT jlong JNICALL Java_org_jqt_QWidget_nativeMinimumSize(JNIEnv* env, jobject, jlong handle) {
    QWidget* wd = static_cast<QWidget*>(requireHandle(env, handle));
    return wd ? jqtPackSize(wd->minimumSize()) : 0;
}
JNIEXPORT jlong JNICALL Java_org_jqt_QWidget_nativeMaximumSize(JNIEnv* env, jobject, jlong handle) {
    QWidget* wd = static_cast<QWidget*>(requireHandle(env, handle));
    return wd ? jqtPackSize(wd->maximumSize()) : 0;
}
JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetFixedWidth(JNIEnv* env, jobject, jlong handle, jint w) {
    QWidget* wd = static_cast<QWidget*>(requireHandle(env, handle));
    if (wd) wd->setFixedWidth(w);
}
JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetFixedHeight(JNIEnv* env, jobject, jlong handle, jint h) {
    QWidget* wd = static_cast<QWidget*>(requireHandle(env, handle));
    if (wd) wd->setFixedHeight(h);
}
JNIEXPORT jboolean JNICALL Java_org_jqt_QWidget_nativeHasFocus(JNIEnv* env, jobject, jlong handle) {
    QWidget* wd = static_cast<QWidget*>(requireHandle(env, handle));
    return (wd && wd->hasFocus()) ? JNI_TRUE : JNI_FALSE;
}
JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetFocus(JNIEnv* env, jobject, jlong handle) {
    QWidget* wd = static_cast<QWidget*>(requireHandle(env, handle));
    if (wd) wd->setFocus();
}
JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeClearFocus(JNIEnv* env, jobject, jlong handle) {
    QWidget* wd = static_cast<QWidget*>(requireHandle(env, handle));
    if (wd) wd->clearFocus();
}
JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetMouseTracking(JNIEnv* env, jobject, jlong handle, jboolean on) {
    QWidget* wd = static_cast<QWidget*>(requireHandle(env, handle));
    if (wd) wd->setMouseTracking(on ? true : false);
}
JNIEXPORT jboolean JNICALL Java_org_jqt_QWidget_nativeHasMouseTracking(JNIEnv* env, jobject, jlong handle) {
    QWidget* wd = static_cast<QWidget*>(requireHandle(env, handle));
    return (wd && wd->hasMouseTracking()) ? JNI_TRUE : JNI_FALSE;
}
JNIEXPORT jboolean JNICALL Java_org_jqt_QWidget_nativeIsActiveWindow(JNIEnv* env, jobject, jlong handle) {
    QWidget* wd = static_cast<QWidget*>(requireHandle(env, handle));
    return (wd && wd->isActiveWindow()) ? JNI_TRUE : JNI_FALSE;
}
JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeActivateWindow(JNIEnv* env, jobject, jlong handle) {
    QWidget* wd = static_cast<QWidget*>(requireHandle(env, handle));
    if (wd) wd->activateWindow();
}
JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeRaise(JNIEnv* env, jobject, jlong handle) {
    QWidget* wd = static_cast<QWidget*>(requireHandle(env, handle));
    if (wd) wd->raise();
}
JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeLower(JNIEnv* env, jobject, jlong handle) {
    QWidget* wd = static_cast<QWidget*>(requireHandle(env, handle));
    if (wd) wd->lower();
}
JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetWindowOpacity(JNIEnv* env, jobject, jlong handle, jdouble opacity) {
    QWidget* wd = static_cast<QWidget*>(requireHandle(env, handle));
    if (wd) wd->setWindowOpacity(opacity);
}
JNIEXPORT jdouble JNICALL Java_org_jqt_QWidget_nativeWindowOpacity(JNIEnv* env, jobject, jlong handle) {
    QWidget* wd = static_cast<QWidget*>(requireHandle(env, handle));
    return wd ? static_cast<jdouble>(wd->windowOpacity()) : 1.0;
}
JNIEXPORT jboolean JNICALL Java_org_jqt_QWidget_nativeIsFullScreen(JNIEnv* env, jobject, jlong handle) {
    QWidget* wd = static_cast<QWidget*>(requireHandle(env, handle));
    return (wd && wd->isFullScreen()) ? JNI_TRUE : JNI_FALSE;
}
JNIEXPORT jboolean JNICALL Java_org_jqt_QWidget_nativeIsMinimized(JNIEnv* env, jobject, jlong handle) {
    QWidget* wd = static_cast<QWidget*>(requireHandle(env, handle));
    return (wd && wd->isMinimized()) ? JNI_TRUE : JNI_FALSE;
}
JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetAutoFillBackground(JNIEnv* env, jobject, jlong handle, jboolean on) {
    QWidget* wd = static_cast<QWidget*>(requireHandle(env, handle));
    if (wd) wd->setAutoFillBackground(on ? true : false);
}
JNIEXPORT jboolean JNICALL Java_org_jqt_QWidget_nativeAutoFillBackground(JNIEnv* env, jobject, jlong handle) {
    QWidget* wd = static_cast<QWidget*>(requireHandle(env, handle));
    return (wd && wd->autoFillBackground()) ? JNI_TRUE : JNI_FALSE;
}
JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeGrabKeyboard(JNIEnv* env, jobject, jlong handle) {
    QWidget* wd = static_cast<QWidget*>(requireHandle(env, handle));
    if (wd) wd->grabKeyboard();
}
JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeReleaseKeyboard(JNIEnv* env, jobject, jlong handle) {
    QWidget* wd = static_cast<QWidget*>(requireHandle(env, handle));
    if (wd) wd->releaseKeyboard();
}

// ----------------------------------------------------------------------------
// QWidget 生成器批次（jqt-gen 自动生成，直传型）
// ----------------------------------------------------------------------------

JNIEXPORT jstring JNICALL Java_org_jqt_QWidget_nativeAccessibleDescription(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    QString __jqt_ret = wgt->accessibleDescription();
    return env->NewStringUTF(__jqt_ret.toUtf8().constData());
}

JNIEXPORT jstring JNICALL Java_org_jqt_QWidget_nativeAccessibleIdentifier(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    QString __jqt_ret = wgt->accessibleIdentifier();
    return env->NewStringUTF(__jqt_ret.toUtf8().constData());
}

JNIEXPORT jstring JNICALL Java_org_jqt_QWidget_nativeAccessibleName(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    QString __jqt_ret = wgt->accessibleName();
    return env->NewStringUTF(__jqt_ret.toUtf8().constData());
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeAdjustSize(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->adjustSize();
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeEnsurePolished(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->ensurePolished();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QWidget_nativeHasHeightForWidth(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->hasHeightForWidth();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QWidget_nativeHasTabletTracking(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->hasTabletTracking();
}

JNIEXPORT jint JNICALL Java_org_jqt_QWidget_nativeHeightForWidth(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->heightForWidth(arg0);
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QWidget_nativeIsMaximized(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isMaximized();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QWidget_nativeIsModal(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isModal();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QWidget_nativeIsWindowModified(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isWindowModified();
}

JNIEXPORT jint JNICALL Java_org_jqt_QWidget_nativeMaximumHeight(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->maximumHeight();
}

JNIEXPORT jint JNICALL Java_org_jqt_QWidget_nativeMaximumWidth(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->maximumWidth();
}

JNIEXPORT jint JNICALL Java_org_jqt_QWidget_nativeMinimumHeight(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->minimumHeight();
}

JNIEXPORT jint JNICALL Java_org_jqt_QWidget_nativeMinimumWidth(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->minimumWidth();
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeScroll(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0, jint arg1) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->scroll(arg0, arg1);
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetAccessibleDescription(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring arg0) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    const char* arg0_utf = env->GetStringUTFChars(arg0, nullptr);
    wgt->setAccessibleDescription(QString::fromUtf8(arg0_utf));    env->ReleaseStringUTFChars(arg0, arg0_utf);

}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetAccessibleIdentifier(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring arg0) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    const char* arg0_utf = env->GetStringUTFChars(arg0, nullptr);
    wgt->setAccessibleIdentifier(QString::fromUtf8(arg0_utf));    env->ReleaseStringUTFChars(arg0, arg0_utf);

}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetAccessibleName(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring arg0) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    const char* arg0_utf = env->GetStringUTFChars(arg0, nullptr);
    wgt->setAccessibleName(QString::fromUtf8(arg0_utf));    env->ReleaseStringUTFChars(arg0, arg0_utf);

}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetDisabled(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setDisabled(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetHidden(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setHidden(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetMaximumHeight(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setMaximumHeight(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetMaximumWidth(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setMaximumWidth(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetMinimumHeight(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setMinimumHeight(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetMinimumWidth(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setMinimumWidth(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetShortcutAutoRepeat(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0, jboolean arg1) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setShortcutAutoRepeat(arg0, arg1);
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetShortcutEnabled(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0, jboolean arg1) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setShortcutEnabled(arg0, arg1);
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetSizeIncrement(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0, jint arg1) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setSizeIncrement(arg0, arg1);
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetStatusTip(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring arg0) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    const char* arg0_utf = env->GetStringUTFChars(arg0, nullptr);
    wgt->setStatusTip(QString::fromUtf8(arg0_utf));    env->ReleaseStringUTFChars(arg0, arg0_utf);

}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetTabletTracking(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setTabletTracking(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetToolTipDuration(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setToolTipDuration(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetWhatsThis(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring arg0) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    const char* arg0_utf = env->GetStringUTFChars(arg0, nullptr);
    wgt->setWhatsThis(QString::fromUtf8(arg0_utf));    env->ReleaseStringUTFChars(arg0, arg0_utf);

}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetWindowFilePath(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring arg0) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    const char* arg0_utf = env->GetStringUTFChars(arg0, nullptr);
    wgt->setWindowFilePath(QString::fromUtf8(arg0_utf));    env->ReleaseStringUTFChars(arg0, arg0_utf);

}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeSetWindowModified(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setWindowModified(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeShowFullScreen(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->showFullScreen();
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeShowMaximized(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->showMaximized();
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeShowMinimized(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->showMinimized();
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeShowNormal(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->showNormal();
}

JNIEXPORT jstring JNICALL Java_org_jqt_QWidget_nativeStatusTip(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    QString __jqt_ret = wgt->statusTip();
    return env->NewStringUTF(__jqt_ret.toUtf8().constData());
}

JNIEXPORT jint JNICALL Java_org_jqt_QWidget_nativeToolTipDuration(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->toolTipDuration();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QWidget_nativeUnderMouse(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->underMouse();
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeUnsetCursor(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->unsetCursor();
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeUnsetLayoutDirection(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->unsetLayoutDirection();
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeUnsetLocale(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->unsetLocale();
}

JNIEXPORT void JNICALL Java_org_jqt_QWidget_nativeUpdateGeometry(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->updateGeometry();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QWidget_nativeUpdatesEnabled(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->updatesEnabled();
}

JNIEXPORT jstring JNICALL Java_org_jqt_QWidget_nativeWhatsThis(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    QString __jqt_ret = wgt->whatsThis();
    return env->NewStringUTF(__jqt_ret.toUtf8().constData());
}

JNIEXPORT jstring JNICALL Java_org_jqt_QWidget_nativeWindowFilePath(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    QString __jqt_ret = wgt->windowFilePath();
    return env->NewStringUTF(__jqt_ret.toUtf8().constData());
}

JNIEXPORT jstring JNICALL Java_org_jqt_QWidget_nativeWindowRole(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QWidget* wgt = static_cast<QWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    QString __jqt_ret = wgt->windowRole();
    return env->NewStringUTF(__jqt_ret.toUtf8().constData());
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jboolean JNICALL Java_org_jqt_QLabel_nativeHasScaledContents(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QLabel* wgt = static_cast<QLabel*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->hasScaledContents();
}

JNIEXPORT jint JNICALL Java_org_jqt_QLabel_nativeHeightForWidth(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QLabel* wgt = static_cast<QLabel*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->heightForWidth(arg0);
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QLabel_nativeOpenExternalLinks(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QLabel* wgt = static_cast<QLabel*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->openExternalLinks();
}

JNIEXPORT jint JNICALL Java_org_jqt_QLabel_nativeSelectionStart(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QLabel* wgt = static_cast<QLabel*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->selectionStart();
}

JNIEXPORT void JNICALL Java_org_jqt_QLabel_nativeSetNum(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QLabel* wgt = static_cast<QLabel*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setNum(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QLabel_nativeSetNum(JNIEnv* env, jobject /*thiz*/, jlong handle, jdouble arg0) {
    QLabel* wgt = static_cast<QLabel*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setNum(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QLabel_nativeSetOpenExternalLinks(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QLabel* wgt = static_cast<QLabel*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setOpenExternalLinks(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QLabel_nativeSetScaledContents(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QLabel* wgt = static_cast<QLabel*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setScaledContents(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QLabel_nativeSetSelection(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0, jint arg1) {
    QLabel* wgt = static_cast<QLabel*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setSelection(arg0, arg1);
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jboolean JNICALL Java_org_jqt_QPushButton_nativeAutoDefault(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QPushButton* wgt = static_cast<QPushButton*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->autoDefault();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QPushButton_nativeIsDefault(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QPushButton* wgt = static_cast<QPushButton*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isDefault();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QPushButton_nativeIsFlat(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QPushButton* wgt = static_cast<QPushButton*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isFlat();
}

JNIEXPORT void JNICALL Java_org_jqt_QPushButton_nativeSetAutoDefault(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QPushButton* wgt = static_cast<QPushButton*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setAutoDefault(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QPushButton_nativeSetDefault(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QPushButton* wgt = static_cast<QPushButton*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setDefault(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QPushButton_nativeSetFlat(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QPushButton* wgt = static_cast<QPushButton*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setFlat(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QPushButton_nativeShowMenu(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QPushButton* wgt = static_cast<QPushButton*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->showMenu();
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativeCursorForward(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0, jint arg1) {
    QLineEdit* wgt = static_cast<QLineEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->cursorForward(arg0, arg1);
}

JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativeCursorWordForward(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QLineEdit* wgt = static_cast<QLineEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->cursorWordForward(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativeDel(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QLineEdit* wgt = static_cast<QLineEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->del();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QLineEdit_nativeDragEnabled(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QLineEdit* wgt = static_cast<QLineEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->dragEnabled();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QLineEdit_nativeHasFrame(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QLineEdit* wgt = static_cast<QLineEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->hasFrame();
}

JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativeHome(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QLineEdit* wgt = static_cast<QLineEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->home(arg0);
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QLineEdit_nativeIsClearButtonEnabled(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QLineEdit* wgt = static_cast<QLineEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isClearButtonEnabled();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QLineEdit_nativeIsModified(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QLineEdit* wgt = static_cast<QLineEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isModified();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QLineEdit_nativeIsRedoAvailable(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QLineEdit* wgt = static_cast<QLineEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isRedoAvailable();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QLineEdit_nativeIsUndoAvailable(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QLineEdit* wgt = static_cast<QLineEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isUndoAvailable();
}

JNIEXPORT jint JNICALL Java_org_jqt_QLineEdit_nativeSelectionStart(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QLineEdit* wgt = static_cast<QLineEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->selectionStart();
}

JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativeSetClearButtonEnabled(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QLineEdit* wgt = static_cast<QLineEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setClearButtonEnabled(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativeSetDragEnabled(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QLineEdit* wgt = static_cast<QLineEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setDragEnabled(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativeSetFrame(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QLineEdit* wgt = static_cast<QLineEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setFrame(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativeSetInputMask(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring arg0) {
    QLineEdit* wgt = static_cast<QLineEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    const char* arg0_utf = env->GetStringUTFChars(arg0, nullptr);
    wgt->setInputMask(QString::fromUtf8(arg0_utf));    env->ReleaseStringUTFChars(arg0, arg0_utf);

}

JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativeSetModified(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QLineEdit* wgt = static_cast<QLineEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setModified(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QLineEdit_nativeSetSelection(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0, jint arg1) {
    QLineEdit* wgt = static_cast<QLineEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setSelection(arg0, arg1);
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT void JNICALL Java_org_jqt_QComboBox_nativeClearEditText(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QComboBox* wgt = static_cast<QComboBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->clearEditText();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QComboBox_nativeDuplicatesEnabled(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QComboBox* wgt = static_cast<QComboBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->duplicatesEnabled();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QComboBox_nativeHasFrame(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QComboBox* wgt = static_cast<QComboBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->hasFrame();
}

JNIEXPORT void JNICALL Java_org_jqt_QComboBox_nativeInsertSeparator(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QComboBox* wgt = static_cast<QComboBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->insertSeparator(arg0);
}

JNIEXPORT jint JNICALL Java_org_jqt_QComboBox_nativeMaxCount(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QComboBox* wgt = static_cast<QComboBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->maxCount();
}

JNIEXPORT jint JNICALL Java_org_jqt_QComboBox_nativeMaxVisibleItems(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QComboBox* wgt = static_cast<QComboBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->maxVisibleItems();
}

JNIEXPORT jint JNICALL Java_org_jqt_QComboBox_nativeMinimumContentsLength(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QComboBox* wgt = static_cast<QComboBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->minimumContentsLength();
}

JNIEXPORT jint JNICALL Java_org_jqt_QComboBox_nativeModelColumn(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QComboBox* wgt = static_cast<QComboBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->modelColumn();
}

JNIEXPORT void JNICALL Java_org_jqt_QComboBox_nativeRemoveItem(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QComboBox* wgt = static_cast<QComboBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->removeItem(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QComboBox_nativeSetCurrentText(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring arg0) {
    QComboBox* wgt = static_cast<QComboBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    const char* arg0_utf = env->GetStringUTFChars(arg0, nullptr);
    wgt->setCurrentText(QString::fromUtf8(arg0_utf));    env->ReleaseStringUTFChars(arg0, arg0_utf);

}

JNIEXPORT void JNICALL Java_org_jqt_QComboBox_nativeSetDuplicatesEnabled(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QComboBox* wgt = static_cast<QComboBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setDuplicatesEnabled(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QComboBox_nativeSetEditText(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring arg0) {
    QComboBox* wgt = static_cast<QComboBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    const char* arg0_utf = env->GetStringUTFChars(arg0, nullptr);
    wgt->setEditText(QString::fromUtf8(arg0_utf));    env->ReleaseStringUTFChars(arg0, arg0_utf);

}

JNIEXPORT void JNICALL Java_org_jqt_QComboBox_nativeSetFrame(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QComboBox* wgt = static_cast<QComboBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setFrame(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QComboBox_nativeSetItemText(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0, jstring arg1) {
    QComboBox* wgt = static_cast<QComboBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    const char* arg1_utf = env->GetStringUTFChars(arg1, nullptr);
    wgt->setItemText(arg0, QString::fromUtf8(arg1_utf));    env->ReleaseStringUTFChars(arg1, arg1_utf);

}

JNIEXPORT void JNICALL Java_org_jqt_QComboBox_nativeSetMaxCount(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QComboBox* wgt = static_cast<QComboBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setMaxCount(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QComboBox_nativeSetMaxVisibleItems(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QComboBox* wgt = static_cast<QComboBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setMaxVisibleItems(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QComboBox_nativeSetMinimumContentsLength(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QComboBox* wgt = static_cast<QComboBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setMinimumContentsLength(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QComboBox_nativeSetModelColumn(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QComboBox* wgt = static_cast<QComboBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setModelColumn(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QComboBox_nativeShowPopup(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QComboBox* wgt = static_cast<QComboBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->showPopup();
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jstring JNICALL Java_org_jqt_QProgressBar_nativeFormat(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QProgressBar* wgt = static_cast<QProgressBar*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    QString __jqt_ret = wgt->format();
    return env->NewStringUTF(__jqt_ret.toUtf8().constData());
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QProgressBar_nativeInvertedAppearance(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QProgressBar* wgt = static_cast<QProgressBar*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->invertedAppearance();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QProgressBar_nativeIsTextVisible(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QProgressBar* wgt = static_cast<QProgressBar*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isTextVisible();
}

JNIEXPORT jint JNICALL Java_org_jqt_QProgressBar_nativeMaximum(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QProgressBar* wgt = static_cast<QProgressBar*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->maximum();
}

JNIEXPORT jint JNICALL Java_org_jqt_QProgressBar_nativeMinimum(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QProgressBar* wgt = static_cast<QProgressBar*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->minimum();
}

JNIEXPORT void JNICALL Java_org_jqt_QProgressBar_nativeResetFormat(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QProgressBar* wgt = static_cast<QProgressBar*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->resetFormat();
}

JNIEXPORT void JNICALL Java_org_jqt_QProgressBar_nativeSetFormat(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring arg0) {
    QProgressBar* wgt = static_cast<QProgressBar*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    const char* arg0_utf = env->GetStringUTFChars(arg0, nullptr);
    wgt->setFormat(QString::fromUtf8(arg0_utf));    env->ReleaseStringUTFChars(arg0, arg0_utf);

}

JNIEXPORT void JNICALL Java_org_jqt_QProgressBar_nativeSetInvertedAppearance(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QProgressBar* wgt = static_cast<QProgressBar*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setInvertedAppearance(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QProgressBar_nativeSetMaximum(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QProgressBar* wgt = static_cast<QProgressBar*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setMaximum(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QProgressBar_nativeSetMinimum(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QProgressBar* wgt = static_cast<QProgressBar*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setMinimum(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QProgressBar_nativeSetTextVisible(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QProgressBar* wgt = static_cast<QProgressBar*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setTextVisible(arg0);
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jboolean JNICALL Java_org_jqt_QGroupBox_nativeIsCheckable(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QGroupBox* wgt = static_cast<QGroupBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isCheckable();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QGroupBox_nativeIsChecked(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QGroupBox* wgt = static_cast<QGroupBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isChecked();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QGroupBox_nativeIsFlat(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QGroupBox* wgt = static_cast<QGroupBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isFlat();
}

JNIEXPORT void JNICALL Java_org_jqt_QGroupBox_nativeSetAlignment(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QGroupBox* wgt = static_cast<QGroupBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setAlignment(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QGroupBox_nativeSetCheckable(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QGroupBox* wgt = static_cast<QGroupBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setCheckable(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QGroupBox_nativeSetFlat(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QGroupBox* wgt = static_cast<QGroupBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setFlat(arg0);
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jint JNICALL Java_org_jqt_QFrame_nativeFrameWidth(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QFrame* wgt = static_cast<QFrame*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->frameWidth();
}

JNIEXPORT jint JNICALL Java_org_jqt_QFrame_nativeLineWidth(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QFrame* wgt = static_cast<QFrame*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->lineWidth();
}

JNIEXPORT jint JNICALL Java_org_jqt_QFrame_nativeMidLineWidth(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QFrame* wgt = static_cast<QFrame*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->midLineWidth();
}

JNIEXPORT void JNICALL Java_org_jqt_QFrame_nativeSetFrameStyle(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QFrame* wgt = static_cast<QFrame*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setFrameStyle(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QFrame_nativeSetLineWidth(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QFrame* wgt = static_cast<QFrame*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setLineWidth(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QFrame_nativeSetMidLineWidth(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QFrame* wgt = static_cast<QFrame*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setMidLineWidth(arg0);
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jboolean JNICALL Java_org_jqt_QMainWindow_nativeDocumentMode(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QMainWindow* wgt = static_cast<QMainWindow*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->documentMode();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QMainWindow_nativeIsAnimated(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QMainWindow* wgt = static_cast<QMainWindow*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isAnimated();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QMainWindow_nativeIsDockNestingEnabled(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QMainWindow* wgt = static_cast<QMainWindow*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isDockNestingEnabled();
}

JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeSetAnimated(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QMainWindow* wgt = static_cast<QMainWindow*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setAnimated(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeSetDockNestingEnabled(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QMainWindow* wgt = static_cast<QMainWindow*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setDockNestingEnabled(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeSetDocumentMode(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QMainWindow* wgt = static_cast<QMainWindow*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setDocumentMode(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QMainWindow_nativeSetUnifiedTitleAndToolBarOnMac(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QMainWindow* wgt = static_cast<QMainWindow*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setUnifiedTitleAndToolBarOnMac(arg0);
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QMainWindow_nativeUnifiedTitleAndToolBarOnMac(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QMainWindow* wgt = static_cast<QMainWindow*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->unifiedTitleAndToolBarOnMac();
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jboolean JNICALL Java_org_jqt_QToolBar_nativeIsFloatable(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QToolBar* wgt = static_cast<QToolBar*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isFloatable();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QToolBar_nativeIsFloating(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QToolBar* wgt = static_cast<QToolBar*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isFloating();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QToolBar_nativeIsMovable(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QToolBar* wgt = static_cast<QToolBar*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isMovable();
}

JNIEXPORT void JNICALL Java_org_jqt_QToolBar_nativeSetFloatable(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QToolBar* wgt = static_cast<QToolBar*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setFloatable(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QToolBar_nativeSetMovable(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QToolBar* wgt = static_cast<QToolBar*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setMovable(arg0);
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jboolean JNICALL Java_org_jqt_QStatusBar_nativeIsSizeGripEnabled(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QStatusBar* wgt = static_cast<QStatusBar*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isSizeGripEnabled();
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jboolean JNICALL Java_org_jqt_QMenu_nativeIsEmpty(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QMenu* wgt = static_cast<QMenu*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isEmpty();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QMenu_nativeIsTearOffMenuVisible(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QMenu* wgt = static_cast<QMenu*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isTearOffMenuVisible();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QMenu_nativeSeparatorsCollapsible(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QMenu* wgt = static_cast<QMenu*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->separatorsCollapsible();
}

JNIEXPORT void JNICALL Java_org_jqt_QMenu_nativeSetSeparatorsCollapsible(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QMenu* wgt = static_cast<QMenu*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setSeparatorsCollapsible(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QMenu_nativeSetTearOffEnabled(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QMenu* wgt = static_cast<QMenu*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setTearOffEnabled(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QMenu_nativeSetToolTipsVisible(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QMenu* wgt = static_cast<QMenu*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setToolTipsVisible(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QMenu_nativeShowTearOffMenu(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QMenu* wgt = static_cast<QMenu*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->showTearOffMenu();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QMenu_nativeToolTipsVisible(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QMenu* wgt = static_cast<QMenu*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->toolTipsVisible();
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jboolean JNICALL Java_org_jqt_QAction_nativeAutoRepeat(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QAction* wgt = static_cast<QAction*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->autoRepeat();
}

JNIEXPORT void JNICALL Java_org_jqt_QAction_nativeHover(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QAction* wgt = static_cast<QAction*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->hover();
}

JNIEXPORT jstring JNICALL Java_org_jqt_QAction_nativeIconText(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QAction* wgt = static_cast<QAction*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    QString __jqt_ret = wgt->iconText();
    return env->NewStringUTF(__jqt_ret.toUtf8().constData());
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QAction_nativeIsCheckable(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QAction* wgt = static_cast<QAction*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isCheckable();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QAction_nativeIsEnabled(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QAction* wgt = static_cast<QAction*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isEnabled();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QAction_nativeIsIconVisibleInMenu(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QAction* wgt = static_cast<QAction*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isIconVisibleInMenu();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QAction_nativeIsShortcutVisibleInContextMenu(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QAction* wgt = static_cast<QAction*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isShortcutVisibleInContextMenu();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QAction_nativeIsVisible(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QAction* wgt = static_cast<QAction*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isVisible();
}

JNIEXPORT void JNICALL Java_org_jqt_QAction_nativeResetEnabled(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QAction* wgt = static_cast<QAction*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->resetEnabled();
}

JNIEXPORT void JNICALL Java_org_jqt_QAction_nativeSetAutoRepeat(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QAction* wgt = static_cast<QAction*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setAutoRepeat(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QAction_nativeSetDisabled(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QAction* wgt = static_cast<QAction*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setDisabled(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QAction_nativeSetEnabled(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QAction* wgt = static_cast<QAction*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setEnabled(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QAction_nativeSetIconVisibleInMenu(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QAction* wgt = static_cast<QAction*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setIconVisibleInMenu(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QAction_nativeSetSeparator(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QAction* wgt = static_cast<QAction*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setSeparator(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QAction_nativeSetShortcutVisibleInContextMenu(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QAction* wgt = static_cast<QAction*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setShortcutVisibleInContextMenu(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QAction_nativeSetVisible(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QAction* wgt = static_cast<QAction*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setVisible(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QAction_nativeSetWhatsThis(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring arg0) {
    QAction* wgt = static_cast<QAction*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    const char* arg0_utf = env->GetStringUTFChars(arg0, nullptr);
    wgt->setWhatsThis(QString::fromUtf8(arg0_utf));    env->ReleaseStringUTFChars(arg0, arg0_utf);

}

JNIEXPORT jstring JNICALL Java_org_jqt_QAction_nativeStatusTip(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QAction* wgt = static_cast<QAction*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    QString __jqt_ret = wgt->statusTip();
    return env->NewStringUTF(__jqt_ret.toUtf8().constData());
}

JNIEXPORT jstring JNICALL Java_org_jqt_QAction_nativeWhatsThis(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QAction* wgt = static_cast<QAction*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    QString __jqt_ret = wgt->whatsThis();
    return env->NewStringUTF(__jqt_ret.toUtf8().constData());
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jboolean JNICALL Java_org_jqt_QSplitter_nativeChildrenCollapsible(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSplitter* wgt = static_cast<QSplitter*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->childrenCollapsible();
}

JNIEXPORT jint JNICALL Java_org_jqt_QSplitter_nativeHandleWidth(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSplitter* wgt = static_cast<QSplitter*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->handleWidth();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QSplitter_nativeOpaqueResize(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSplitter* wgt = static_cast<QSplitter*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->opaqueResize();
}

JNIEXPORT void JNICALL Java_org_jqt_QSplitter_nativeRefresh(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSplitter* wgt = static_cast<QSplitter*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->refresh();
}

JNIEXPORT void JNICALL Java_org_jqt_QSplitter_nativeSetChildrenCollapsible(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QSplitter* wgt = static_cast<QSplitter*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setChildrenCollapsible(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QSplitter_nativeSetCollapsible(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0, jboolean arg1) {
    QSplitter* wgt = static_cast<QSplitter*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setCollapsible(arg0, arg1);
}

JNIEXPORT void JNICALL Java_org_jqt_QSplitter_nativeSetOpaqueResize(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QSplitter* wgt = static_cast<QSplitter*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setOpaqueResize(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QSplitter_nativeSetStretchFactor(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0, jint arg1) {
    QSplitter* wgt = static_cast<QSplitter*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setStretchFactor(arg0, arg1);
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jint JNICALL Java_org_jqt_QStackedWidget_nativeCount(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QStackedWidget* wgt = static_cast<QStackedWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->count();
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jboolean JNICALL Java_org_jqt_QTabWidget_nativeDocumentMode(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QTabWidget* wgt = static_cast<QTabWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->documentMode();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QTabWidget_nativeHasHeightForWidth(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QTabWidget* wgt = static_cast<QTabWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->hasHeightForWidth();
}

JNIEXPORT jint JNICALL Java_org_jqt_QTabWidget_nativeHeightForWidth(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QTabWidget* wgt = static_cast<QTabWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->heightForWidth(arg0);
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QTabWidget_nativeIsMovable(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QTabWidget* wgt = static_cast<QTabWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isMovable();
}

JNIEXPORT void JNICALL Java_org_jqt_QTabWidget_nativeRemoveTab(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QTabWidget* wgt = static_cast<QTabWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->removeTab(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QTabWidget_nativeSetDocumentMode(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QTabWidget* wgt = static_cast<QTabWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setDocumentMode(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QTabWidget_nativeSetMovable(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QTabWidget* wgt = static_cast<QTabWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setMovable(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QTabWidget_nativeSetTabBarAutoHide(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QTabWidget* wgt = static_cast<QTabWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setTabBarAutoHide(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QTabWidget_nativeSetTabEnabled(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0, jboolean arg1) {
    QTabWidget* wgt = static_cast<QTabWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setTabEnabled(arg0, arg1);
}

JNIEXPORT void JNICALL Java_org_jqt_QTabWidget_nativeSetTabVisible(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0, jboolean arg1) {
    QTabWidget* wgt = static_cast<QTabWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setTabVisible(arg0, arg1);
}

JNIEXPORT void JNICALL Java_org_jqt_QTabWidget_nativeSetUsesScrollButtons(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QTabWidget* wgt = static_cast<QTabWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setUsesScrollButtons(arg0);
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QTabWidget_nativeTabBarAutoHide(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QTabWidget* wgt = static_cast<QTabWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->tabBarAutoHide();
}

JNIEXPORT jstring JNICALL Java_org_jqt_QTabWidget_nativeTabText(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QTabWidget* wgt = static_cast<QTabWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    QString __jqt_ret = wgt->tabText(arg0);
    return env->NewStringUTF(__jqt_ret.toUtf8().constData());
}

JNIEXPORT jstring JNICALL Java_org_jqt_QTabWidget_nativeTabToolTip(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QTabWidget* wgt = static_cast<QTabWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    QString __jqt_ret = wgt->tabToolTip(arg0);
    return env->NewStringUTF(__jqt_ret.toUtf8().constData());
}

JNIEXPORT jstring JNICALL Java_org_jqt_QTabWidget_nativeTabWhatsThis(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QTabWidget* wgt = static_cast<QTabWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    QString __jqt_ret = wgt->tabWhatsThis(arg0);
    return env->NewStringUTF(__jqt_ret.toUtf8().constData());
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QTabWidget_nativeUsesScrollButtons(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QTabWidget* wgt = static_cast<QTabWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->usesScrollButtons();
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jint JNICALL Java_org_jqt_QTreeWidget_nativeCurrentColumn(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QTreeWidget* wgt = static_cast<QTreeWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->currentColumn();
}

JNIEXPORT void JNICALL Java_org_jqt_QTreeWidget_nativeSetColumnCount(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QTreeWidget* wgt = static_cast<QTreeWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setColumnCount(arg0);
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jboolean JNICALL Java_org_jqt_QListWidget_nativeIsSortingEnabled(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QListWidget* wgt = static_cast<QListWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isSortingEnabled();
}

JNIEXPORT void JNICALL Java_org_jqt_QListWidget_nativeSetSortingEnabled(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QListWidget* wgt = static_cast<QListWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setSortingEnabled(arg0);
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jint JNICALL Java_org_jqt_QSpinBox_nativeDisplayIntegerBase(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSpinBox* wgt = static_cast<QSpinBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->displayIntegerBase();
}

JNIEXPORT void JNICALL Java_org_jqt_QSpinBox_nativeSetDisplayIntegerBase(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QSpinBox* wgt = static_cast<QSpinBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setDisplayIntegerBase(arg0);
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jboolean JNICALL Java_org_jqt_QDateTimeEdit_nativeCalendarPopup(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QDateTimeEdit* wgt = static_cast<QDateTimeEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->calendarPopup();
}

JNIEXPORT void JNICALL Java_org_jqt_QDateTimeEdit_nativeClear(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QDateTimeEdit* wgt = static_cast<QDateTimeEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->clear();
}

JNIEXPORT void JNICALL Java_org_jqt_QDateTimeEdit_nativeClearMaximumDate(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QDateTimeEdit* wgt = static_cast<QDateTimeEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->clearMaximumDate();
}

JNIEXPORT void JNICALL Java_org_jqt_QDateTimeEdit_nativeClearMaximumDateTime(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QDateTimeEdit* wgt = static_cast<QDateTimeEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->clearMaximumDateTime();
}

JNIEXPORT void JNICALL Java_org_jqt_QDateTimeEdit_nativeClearMaximumTime(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QDateTimeEdit* wgt = static_cast<QDateTimeEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->clearMaximumTime();
}

JNIEXPORT void JNICALL Java_org_jqt_QDateTimeEdit_nativeClearMinimumDate(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QDateTimeEdit* wgt = static_cast<QDateTimeEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->clearMinimumDate();
}

JNIEXPORT void JNICALL Java_org_jqt_QDateTimeEdit_nativeClearMinimumDateTime(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QDateTimeEdit* wgt = static_cast<QDateTimeEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->clearMinimumDateTime();
}

JNIEXPORT void JNICALL Java_org_jqt_QDateTimeEdit_nativeClearMinimumTime(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QDateTimeEdit* wgt = static_cast<QDateTimeEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->clearMinimumTime();
}

JNIEXPORT jint JNICALL Java_org_jqt_QDateTimeEdit_nativeCurrentSectionIndex(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QDateTimeEdit* wgt = static_cast<QDateTimeEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->currentSectionIndex();
}

JNIEXPORT jint JNICALL Java_org_jqt_QDateTimeEdit_nativeSectionCount(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QDateTimeEdit* wgt = static_cast<QDateTimeEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->sectionCount();
}

JNIEXPORT void JNICALL Java_org_jqt_QDateTimeEdit_nativeSetCalendarPopup(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QDateTimeEdit* wgt = static_cast<QDateTimeEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setCalendarPopup(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QDateTimeEdit_nativeSetCurrentSectionIndex(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QDateTimeEdit* wgt = static_cast<QDateTimeEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setCurrentSectionIndex(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QDateTimeEdit_nativeStepBy(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QDateTimeEdit* wgt = static_cast<QDateTimeEdit*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->stepBy(arg0);
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT void JNICALL Java_org_jqt_QScrollArea_nativeEnsureVisible(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0, jint arg1, jint arg2, jint arg3) {
    QScrollArea* wgt = static_cast<QScrollArea*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->ensureVisible(arg0, arg1, arg2, arg3);
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QScrollArea_nativeWidgetResizable(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QScrollArea* wgt = static_cast<QScrollArea*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->widgetResizable();
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jboolean JNICALL Java_org_jqt_QStackedLayout_nativeHasHeightForWidth(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QStackedLayout* wgt = static_cast<QStackedLayout*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->hasHeightForWidth();
}

JNIEXPORT jint JNICALL Java_org_jqt_QStackedLayout_nativeHeightForWidth(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QStackedLayout* wgt = static_cast<QStackedLayout*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->heightForWidth(arg0);
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jint JNICALL Java_org_jqt_QMenuBar_nativeHeightForWidth(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QMenuBar* wgt = static_cast<QMenuBar*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->heightForWidth(arg0);
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QMenuBar_nativeIsDefaultUp(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QMenuBar* wgt = static_cast<QMenuBar*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isDefaultUp();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QMenuBar_nativeIsNativeMenuBar(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QMenuBar* wgt = static_cast<QMenuBar*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isNativeMenuBar();
}

JNIEXPORT void JNICALL Java_org_jqt_QMenuBar_nativeSetDefaultUp(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QMenuBar* wgt = static_cast<QMenuBar*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setDefaultUp(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QMenuBar_nativeSetNativeMenuBar(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QMenuBar* wgt = static_cast<QMenuBar*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setNativeMenuBar(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QMenuBar_nativeSetVisible(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QMenuBar* wgt = static_cast<QMenuBar*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setVisible(arg0);
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jboolean JNICALL Java_org_jqt_QSystemTrayIcon_nativeIsSystemTrayAvailable(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSystemTrayIcon* wgt = static_cast<QSystemTrayIcon*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isSystemTrayAvailable();
}

JNIEXPORT void JNICALL Java_org_jqt_QSystemTrayIcon_nativeSetVisible(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QSystemTrayIcon* wgt = static_cast<QSystemTrayIcon*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setVisible(arg0);
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QSystemTrayIcon_nativeSupportsMessages(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSystemTrayIcon* wgt = static_cast<QSystemTrayIcon*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->supportsMessages();
}

JNIEXPORT jstring JNICALL Java_org_jqt_QSystemTrayIcon_nativeToolTip(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSystemTrayIcon* wgt = static_cast<QSystemTrayIcon*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    QString __jqt_ret = wgt->toolTip();
    return env->NewStringUTF(__jqt_ret.toUtf8().constData());
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jstring JNICALL Java_org_jqt_QMessageBox_nativeDetailedText(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QMessageBox* wgt = static_cast<QMessageBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    QString __jqt_ret = wgt->detailedText();
    return env->NewStringUTF(__jqt_ret.toUtf8().constData());
}

JNIEXPORT jstring JNICALL Java_org_jqt_QMessageBox_nativeInformativeText(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QMessageBox* wgt = static_cast<QMessageBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    QString __jqt_ret = wgt->informativeText();
    return env->NewStringUTF(__jqt_ret.toUtf8().constData());
}

JNIEXPORT void JNICALL Java_org_jqt_QMessageBox_nativeSetDetailedText(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring arg0) {
    QMessageBox* wgt = static_cast<QMessageBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    const char* arg0_utf = env->GetStringUTFChars(arg0, nullptr);
    wgt->setDetailedText(QString::fromUtf8(arg0_utf));    env->ReleaseStringUTFChars(arg0, arg0_utf);

}

JNIEXPORT void JNICALL Java_org_jqt_QMessageBox_nativeSetInformativeText(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring arg0) {
    QMessageBox* wgt = static_cast<QMessageBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    const char* arg0_utf = env->GetStringUTFChars(arg0, nullptr);
    wgt->setInformativeText(QString::fromUtf8(arg0_utf));    env->ReleaseStringUTFChars(arg0, arg0_utf);

}

JNIEXPORT jstring JNICALL Java_org_jqt_QMessageBox_nativeText(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QMessageBox* wgt = static_cast<QMessageBox*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    QString __jqt_ret = wgt->text();
    return env->NewStringUTF(__jqt_ret.toUtf8().constData());
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jboolean JNICALL Java_org_jqt_QDialog_nativeIsModal(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QDialog* wgt = static_cast<QDialog*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isModal();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QDialog_nativeIsSizeGripEnabled(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QDialog* wgt = static_cast<QDialog*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isSizeGripEnabled();
}

JNIEXPORT void JNICALL Java_org_jqt_QDialog_nativeSetModal(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QDialog* wgt = static_cast<QDialog*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setModal(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QDialog_nativeSetResult(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QDialog* wgt = static_cast<QDialog*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setResult(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QDialog_nativeSetSizeGripEnabled(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QDialog* wgt = static_cast<QDialog*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setSizeGripEnabled(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QDialog_nativeSetVisible(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QDialog* wgt = static_cast<QDialog*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setVisible(arg0);
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT void JNICALL Java_org_jqt_QTableWidget_nativeClear(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QTableWidget* wgt = static_cast<QTableWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->clear();
}

JNIEXPORT void JNICALL Java_org_jqt_QTableWidget_nativeInsertColumn(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QTableWidget* wgt = static_cast<QTableWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->insertColumn(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QTableWidget_nativeInsertRow(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QTableWidget* wgt = static_cast<QTableWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->insertRow(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QTableWidget_nativeRemoveCellWidget(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0, jint arg1) {
    QTableWidget* wgt = static_cast<QTableWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->removeCellWidget(arg0, arg1);
}

JNIEXPORT void JNICALL Java_org_jqt_QTableWidget_nativeRemoveColumn(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QTableWidget* wgt = static_cast<QTableWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->removeColumn(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QTableWidget_nativeRemoveRow(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QTableWidget* wgt = static_cast<QTableWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->removeRow(arg0);
}

JNIEXPORT jint JNICALL Java_org_jqt_QTableWidget_nativeVisualColumn(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QTableWidget* wgt = static_cast<QTableWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->visualColumn(arg0);
}

JNIEXPORT jint JNICALL Java_org_jqt_QTableWidget_nativeVisualRow(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QTableWidget* wgt = static_cast<QTableWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->visualRow(arg0);
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT void JNICALL Java_org_jqt_QSettings_nativeEndArray(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSettings* wgt = static_cast<QSettings*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->endArray();
}

JNIEXPORT void JNICALL Java_org_jqt_QSettings_nativeSetArrayIndex(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QSettings* wgt = static_cast<QSettings*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setArrayIndex(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QSettings_nativeSetAtomicSyncRequired(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QSettings* wgt = static_cast<QSettings*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setAtomicSyncRequired(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QSettings_nativeSetFallbacksEnabled(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QSettings* wgt = static_cast<QSettings*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setFallbacksEnabled(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QSettings_nativeSync(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSettings* wgt = static_cast<QSettings*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->sync();
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jint JNICALL Java_org_jqt_QListView_nativeBatchSize(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QListView* wgt = static_cast<QListView*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->batchSize();
}

JNIEXPORT void JNICALL Java_org_jqt_QListView_nativeClearPropertyFlags(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QListView* wgt = static_cast<QListView*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->clearPropertyFlags();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QListView_nativeIsSelectionRectVisible(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QListView* wgt = static_cast<QListView*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isSelectionRectVisible();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QListView_nativeIsWrapping(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QListView* wgt = static_cast<QListView*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isWrapping();
}

JNIEXPORT jint JNICALL Java_org_jqt_QListView_nativeModelColumn(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QListView* wgt = static_cast<QListView*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->modelColumn();
}

JNIEXPORT void JNICALL Java_org_jqt_QListView_nativeSetBatchSize(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QListView* wgt = static_cast<QListView*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setBatchSize(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QListView_nativeSetModelColumn(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QListView* wgt = static_cast<QListView*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setModelColumn(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QListView_nativeSetRowHidden(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0, jboolean arg1) {
    QListView* wgt = static_cast<QListView*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setRowHidden(arg0, arg1);
}

JNIEXPORT void JNICALL Java_org_jqt_QListView_nativeSetSelectionRectVisible(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QListView* wgt = static_cast<QListView*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setSelectionRectVisible(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QListView_nativeSetUniformItemSizes(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QListView* wgt = static_cast<QListView*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setUniformItemSizes(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QListView_nativeSetWrapping(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QListView* wgt = static_cast<QListView*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setWrapping(arg0);
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QListView_nativeUniformItemSizes(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QListView* wgt = static_cast<QListView*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->uniformItemSizes();
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jint JNICALL Java_org_jqt_QDial_nativeNotchSize(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QDial* wgt = static_cast<QDial*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->notchSize();
}

JNIEXPORT jdouble JNICALL Java_org_jqt_QDial_nativeNotchTarget(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QDial* wgt = static_cast<QDial*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->notchTarget();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QDial_nativeNotchesVisible(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QDial* wgt = static_cast<QDial*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->notchesVisible();
}

JNIEXPORT void JNICALL Java_org_jqt_QDial_nativeSetNotchTarget(JNIEnv* env, jobject /*thiz*/, jlong handle, jdouble arg0) {
    QDial* wgt = static_cast<QDial*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setNotchTarget(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QDial_nativeSetNotchesVisible(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QDial* wgt = static_cast<QDial*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setNotchesVisible(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QDial_nativeSetWrapping(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QDial* wgt = static_cast<QDial*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setWrapping(arg0);
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QDial_nativeWrapping(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QDial* wgt = static_cast<QDial*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->wrapping();
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jboolean JNICALL Java_org_jqt_QFormLayout_nativeHasHeightForWidth(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QFormLayout* wgt = static_cast<QFormLayout*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->hasHeightForWidth();
}

JNIEXPORT jint JNICALL Java_org_jqt_QFormLayout_nativeHeightForWidth(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QFormLayout* wgt = static_cast<QFormLayout*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->heightForWidth(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QFormLayout_nativeInvalidate(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QFormLayout* wgt = static_cast<QFormLayout*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->invalidate();
}

JNIEXPORT void JNICALL Java_org_jqt_QFormLayout_nativeSetRowVisible(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0, jboolean arg1) {
    QFormLayout* wgt = static_cast<QFormLayout*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setRowVisible(arg0, arg1);
}

JNIEXPORT void JNICALL Java_org_jqt_QFormLayout_nativeSetVerticalSpacing(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QFormLayout* wgt = static_cast<QFormLayout*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setVerticalSpacing(arg0);
}

JNIEXPORT jint JNICALL Java_org_jqt_QFormLayout_nativeSpacing(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QFormLayout* wgt = static_cast<QFormLayout*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->spacing();
}

JNIEXPORT jint JNICALL Java_org_jqt_QFormLayout_nativeVerticalSpacing(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QFormLayout* wgt = static_cast<QFormLayout*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->verticalSpacing();
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jint JNICALL Java_org_jqt_QGridLayout_nativeCount(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QGridLayout* wgt = static_cast<QGridLayout*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->count();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QGridLayout_nativeHasHeightForWidth(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QGridLayout* wgt = static_cast<QGridLayout*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->hasHeightForWidth();
}

JNIEXPORT jint JNICALL Java_org_jqt_QGridLayout_nativeHeightForWidth(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QGridLayout* wgt = static_cast<QGridLayout*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->heightForWidth(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QGridLayout_nativeInvalidate(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QGridLayout* wgt = static_cast<QGridLayout*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->invalidate();
}

JNIEXPORT jint JNICALL Java_org_jqt_QGridLayout_nativeMinimumHeightForWidth(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QGridLayout* wgt = static_cast<QGridLayout*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->minimumHeightForWidth(arg0);
}

JNIEXPORT jint JNICALL Java_org_jqt_QGridLayout_nativeRowCount(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QGridLayout* wgt = static_cast<QGridLayout*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->rowCount();
}

JNIEXPORT void JNICALL Java_org_jqt_QGridLayout_nativeSetRowMinimumHeight(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0, jint arg1) {
    QGridLayout* wgt = static_cast<QGridLayout*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setRowMinimumHeight(arg0, arg1);
}

JNIEXPORT void JNICALL Java_org_jqt_QGridLayout_nativeSetVerticalSpacing(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QGridLayout* wgt = static_cast<QGridLayout*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setVerticalSpacing(arg0);
}

JNIEXPORT jint JNICALL Java_org_jqt_QGridLayout_nativeSpacing(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QGridLayout* wgt = static_cast<QGridLayout*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->spacing();
}

JNIEXPORT jint JNICALL Java_org_jqt_QGridLayout_nativeVerticalSpacing(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QGridLayout* wgt = static_cast<QGridLayout*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->verticalSpacing();
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jboolean JNICALL Java_org_jqt_QLayout_nativeIsEmpty(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QLayout* wgt = static_cast<QLayout*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isEmpty();
}

JNIEXPORT void JNICALL Java_org_jqt_QLayout_nativeSetEnabled(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QLayout* wgt = static_cast<QLayout*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setEnabled(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QLayout_nativeUnsetContentsMargins(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QLayout* wgt = static_cast<QLayout*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->unsetContentsMargins();
}

JNIEXPORT void JNICALL Java_org_jqt_QLayout_nativeUpdate(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QLayout* wgt = static_cast<QLayout*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->update();
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jboolean JNICALL Java_org_jqt_QApplication_nativeAutoSipEnabled(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QApplication* wgt = static_cast<QApplication*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->autoSipEnabled();
}

JNIEXPORT void JNICALL Java_org_jqt_QApplication_nativeCloseAllWindows(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QApplication* wgt = static_cast<QApplication*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->closeAllWindows();
}

JNIEXPORT jint JNICALL Java_org_jqt_QApplication_nativeCursorFlashTime(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QApplication* wgt = static_cast<QApplication*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->cursorFlashTime();
}

JNIEXPORT jint JNICALL Java_org_jqt_QApplication_nativeDoubleClickInterval(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QApplication* wgt = static_cast<QApplication*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->doubleClickInterval();
}

JNIEXPORT jint JNICALL Java_org_jqt_QApplication_nativeKeyboardInputInterval(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QApplication* wgt = static_cast<QApplication*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->keyboardInputInterval();
}

JNIEXPORT void JNICALL Java_org_jqt_QApplication_nativeSetCursorFlashTime(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QApplication* wgt = static_cast<QApplication*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setCursorFlashTime(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QApplication_nativeSetDoubleClickInterval(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QApplication* wgt = static_cast<QApplication*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setDoubleClickInterval(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QApplication_nativeSetKeyboardInputInterval(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QApplication* wgt = static_cast<QApplication*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setKeyboardInputInterval(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QApplication_nativeSetStartDragDistance(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QApplication* wgt = static_cast<QApplication*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setStartDragDistance(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QApplication_nativeSetStartDragTime(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QApplication* wgt = static_cast<QApplication*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setStartDragTime(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QApplication_nativeSetWheelScrollLines(JNIEnv* env, jobject /*thiz*/, jlong handle, jint arg0) {
    QApplication* wgt = static_cast<QApplication*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setWheelScrollLines(arg0);
}

JNIEXPORT jint JNICALL Java_org_jqt_QApplication_nativeStartDragTime(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QApplication* wgt = static_cast<QApplication*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->startDragTime();
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jlong JNICALL Java_org_jqt_QSerialPort_nativeBytesToWrite(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSerialPort* wgt = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->bytesToWrite();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QSerialPort_nativeCanReadLine(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSerialPort* wgt = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->canReadLine();
}

JNIEXPORT void JNICALL Java_org_jqt_QSerialPort_nativeClearError(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSerialPort* wgt = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->clearError();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QSerialPort_nativeIsBreakEnabled(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSerialPort* wgt = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isBreakEnabled();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QSerialPort_nativeIsDataTerminalReady(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSerialPort* wgt = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isDataTerminalReady();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QSerialPort_nativeIsRequestToSend(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSerialPort* wgt = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isRequestToSend();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QSerialPort_nativeIsSequential(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSerialPort* wgt = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isSequential();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QSerialPort_nativeSetBreakEnabled(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QSerialPort* wgt = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->setBreakEnabled(arg0);
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QSerialPort_nativeSetDataTerminalReady(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QSerialPort* wgt = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->setDataTerminalReady(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QSerialPort_nativeSetReadBufferSize(JNIEnv* env, jobject /*thiz*/, jlong handle, jlong arg0) {
    QSerialPort* wgt = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setReadBufferSize(arg0);
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QSerialPort_nativeSetRequestToSend(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QSerialPort* wgt = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->setRequestToSend(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QSerialPort_nativeSetSettingsRestoredOnClose(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QSerialPort* wgt = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setSettingsRestoredOnClose(arg0);
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QSerialPort_nativeSettingsRestoredOnClose(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSerialPort* wgt = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->settingsRestoredOnClose();
}

JNIEXPORT jlong JNICALL Java_org_jqt_QSerialPort_nativeWriteBufferSize(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSerialPort* wgt = static_cast<QSerialPort*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->writeBufferSize();
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jboolean JNICALL Java_org_jqt_QOpenGLWidget_nativeIsValid(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QOpenGLWidget* wgt = static_cast<QOpenGLWidget*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->isValid();
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jint JNICALL Java_org_jqt_QSqlQuery_nativeAt(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSqlQuery* wgt = static_cast<QSqlQuery*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->at();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QSqlQuery_nativeFirst(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSqlQuery* wgt = static_cast<QSqlQuery*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->first();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QSqlQuery_nativeLast(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSqlQuery* wgt = static_cast<QSqlQuery*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->last();
}

JNIEXPORT jstring JNICALL Java_org_jqt_QSqlQuery_nativeLastQuery(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSqlQuery* wgt = static_cast<QSqlQuery*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    QString __jqt_ret = wgt->lastQuery();
    return env->NewStringUTF(__jqt_ret.toUtf8().constData());
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QSqlQuery_nativeNextResult(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSqlQuery* wgt = static_cast<QSqlQuery*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->nextResult();
}

JNIEXPORT jboolean JNICALL Java_org_jqt_QSqlQuery_nativePrepare(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring arg0) {
    QSqlQuery* wgt = static_cast<QSqlQuery*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    const char* arg0_utf = env->GetStringUTFChars(arg0, nullptr);
    return wgt->prepare(QString::fromUtf8(arg0_utf));    env->ReleaseStringUTFChars(arg0, arg0_utf);

}

JNIEXPORT void JNICALL Java_org_jqt_QSqlQuery_nativeSetForwardOnly(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QSqlQuery* wgt = static_cast<QSqlQuery*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setForwardOnly(arg0);
}

JNIEXPORT void JNICALL Java_org_jqt_QSqlQuery_nativeSetPositionalBindingEnabled(JNIEnv* env, jobject /*thiz*/, jlong handle, jboolean arg0) {
    QSqlQuery* wgt = static_cast<QSqlQuery*>(requireHandle(env, handle));
    if (wgt == nullptr) { return; }
    wgt->setPositionalBindingEnabled(arg0);
}

JNIEXPORT jint JNICALL Java_org_jqt_QSqlQuery_nativeSize(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QSqlQuery* wgt = static_cast<QSqlQuery*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->size();
}


// 生成器批次（jqt-gen 自动生成，直传型）
JNIEXPORT jboolean JNICALL Java_org_jqt_QFile_nativeSupportsMoveToTrash(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QFile* wgt = static_cast<QFile*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    return wgt->supportsMoveToTrash();
}

JNIEXPORT jstring JNICALL Java_org_jqt_QFile_nativeSymLinkTarget(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring arg0) {
    QFile* wgt = static_cast<QFile*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    const char* arg0_utf = env->GetStringUTFChars(arg0, nullptr);
    QString __jqt_ret = wgt->symLinkTarget(QString::fromUtf8(arg0_utf));
    return env->NewStringUTF(__jqt_ret.toUtf8().constData());    env->ReleaseStringUTFChars(arg0, arg0_utf);

}

JNIEXPORT jstring JNICALL Java_org_jqt_QFile_nativeSymLinkTarget(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    QFile* wgt = static_cast<QFile*>(requireHandle(env, handle));
    if (wgt == nullptr) { return 0; }
    QString __jqt_ret = wgt->symLinkTarget();
    return env->NewStringUTF(__jqt_ret.toUtf8().constData());
}

