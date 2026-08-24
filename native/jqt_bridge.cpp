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

#include <QApplication>
#include <QBoxLayout>
#include <QCloseEvent>
#include <QComboBox>
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

#include <atomic>
#include <functional>
#include <mutex>
#include <unordered_map>

#include "generated/org_jqt_JQtApplication.h"
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

class JQtWindowShell : public QWidget {
public:
    std::function<void()> onClose;
    std::function<void(int, int)> onResized;
    std::function<void(int, int)> onMoved;

protected:
    void closeEvent(QCloseEvent* event) override {
        if (onClose) {
            onClose();
        }
        QWidget::closeEvent(event);
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

// ----------------------------------------------------------------------------
// JQtButton：QPushButton 的封装（clicked/pressed/released/toggled 信号）
// ----------------------------------------------------------------------------

JNIEXPORT jlong JNICALL Java_org_jqt_JQtButton_nativeCreate(JNIEnv* env, jobject thiz, jstring text) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    QPushButton* btn = new QPushButton(QString::fromUtf8(utf));
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
// JQtLineEdit：QLineEdit 的封装（textChanged / returnPressed 信号）
// ----------------------------------------------------------------------------

JNIEXPORT jlong JNICALL Java_org_jqt_JQtLineEdit_nativeCreate(JNIEnv* env, jobject thiz, jstring text) {
    if (requireApp(env) == nullptr) {
        return 0;
    }
    const char* utf = env->GetStringUTFChars(text, nullptr);
    QLineEdit* edit = new QLineEdit(QString::fromUtf8(utf));
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
