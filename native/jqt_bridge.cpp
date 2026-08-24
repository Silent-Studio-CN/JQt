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
//   2. 把 Qt 的信号（clicked/pressed/released/toggled/aboutToQuit 等）与
//      事件（closeEvent/resizeEvent/moveEvent）通过 JNI 回调回 Java 层，
//      实现 "C++ 信号 → JNI → Java lambda" 的伪信号槽机制；
//   3. 布局管理器（QVBoxLayout/QHBoxLayout）的封装。
//
// 构建：项目根目录 build.ps1（MinGW 13.1 + Qt 6.11.2 mingw_64 kit）
// ============================================================================

#include <jni.h>

#include <QApplication>
#include <QBoxLayout>
#include <QCloseEvent>
#include <QHBoxLayout>
#include <QLabel>
#include <QLayout>
#include <QMoveEvent>
#include <QPushButton>
#include <QResizeEvent>
#include <QTimer>
#include <QVBoxLayout>
#include <QWidget>

#include <functional>

#include "generated/org_jqt_JQtApplication.h"
#include "generated/org_jqt_JQtWindow.h"
#include "generated/org_jqt_JQtButton.h"
#include "generated/org_jqt_JQtLabel.h"
#include "generated/org_jqt_JQtLayout.h"
#include "generated/org_jqt_JQtVBoxLayout.h"
#include "generated/org_jqt_JQtHBoxLayout.h"

// ----------------------------------------------------------------------------
// 全局状态
// ----------------------------------------------------------------------------

static JavaVM* g_jvm = nullptr;        // 缓存的 JVM 句柄（供 C++ → Java 回调）
static QApplication* g_app = nullptr;  // 进程级唯一的 QApplication

// 取得当前线程的 JNIEnv：已附加 JVM 则复用，否则挂载。
// Qt 信号总是在 GUI（主）线程发出，而主线程执行 app.exec() 前已被 JVM 附加，
// 因此回调是线程安全的。
static JNIEnv* callbackEnv() {
    JNIEnv* env = nullptr;
    if (g_jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_8) == JNI_EDETACHED) {
        g_jvm->AttachCurrentThread(reinterpret_cast<void**>(&env), nullptr);
    }
    return env;
}

// ----------------------------------------------------------------------------
// 窗口壳：在 C++ 侧继承 QWidget，拦截 closeEvent / resizeEvent / moveEvent，
// 把窗口事件回调给 Java 层（对应 JQtWindow.onClose / onResized / onMoved）。
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

    // 连接 aboutToQuit 信号 → Java nativeHandleAboutToQuit()
    // gRef 不删除：JVM 退出时统一清理
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(g_app, &QApplication::aboutToQuit, [gRef]() {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleAboutToQuit", "()V");
        if (mid != nullptr) {
            e->CallVoidMethod(gRef, mid);
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
            e->CallVoidMethod(gTask, mid);
        }
        e->DeleteGlobalRef(gTask);
    });
}

// ----------------------------------------------------------------------------
// JQtWindow：顶级 QWidget 的封装
// ----------------------------------------------------------------------------

JNIEXPORT jlong JNICALL Java_org_jqt_JQtWindow_nativeCreate(JNIEnv* env, jobject thiz, jstring title, jint width, jint height) {
    const char* utf = env->GetStringUTFChars(title, nullptr);
    JQtWindowShell* win = new JQtWindowShell();
    win->setWindowTitle(QString::fromUtf8(utf));
    win->resize(static_cast<int>(width), static_cast<int>(height));
    env->ReleaseStringUTFChars(title, utf);

    // 持有 Java 窗口对象的全局引用，供窗口事件回调使用（进程退出时统一清理，不手动删除）
    jobject gRef = env->NewGlobalRef(thiz);
    win->onClose = [gRef]() {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleClose", "()V");
        if (mid != nullptr) {
            e->CallVoidMethod(gRef, mid);
        }
    };
    win->onResized = [gRef](int w, int h) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleResized", "(II)V");
        if (mid != nullptr) {
            e->CallVoidMethod(gRef, mid, static_cast<jint>(w), static_cast<jint>(h));
        }
    };
    win->onMoved = [gRef](int x, int y) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleMoved", "(II)V");
        if (mid != nullptr) {
            e->CallVoidMethod(gRef, mid, static_cast<jint>(x), static_cast<jint>(y));
        }
    };

    return reinterpret_cast<jlong>(win);
}

JNIEXPORT void JNICALL Java_org_jqt_JQtWindow_nativeShow(JNIEnv* /*env*/, jobject /*thiz*/, jlong handle) {
    reinterpret_cast<QWidget*>(handle)->show();
}

JNIEXPORT void JNICALL Java_org_jqt_JQtWindow_nativeHide(JNIEnv* /*env*/, jobject /*thiz*/, jlong handle) {
    reinterpret_cast<QWidget*>(handle)->hide();
}

JNIEXPORT void JNICALL Java_org_jqt_JQtWindow_nativeResize(JNIEnv* /*env*/, jobject /*thiz*/, jlong handle, jint width, jint height) {
    reinterpret_cast<QWidget*>(handle)->resize(static_cast<int>(width), static_cast<int>(height));
}

JNIEXPORT void JNICALL Java_org_jqt_JQtWindow_nativeSetTitle(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring title) {
    const char* utf = env->GetStringUTFChars(title, nullptr);
    reinterpret_cast<QWidget*>(handle)->setWindowTitle(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(title, utf);
}

// 把子控件加入窗口（未设置布局时）：建立 Qt 父子关系并按顺序自动摆放。
// 设置了布局管理器后应改用布局的 addWidget（Phase 3）。
JNIEXPORT void JNICALL Java_org_jqt_JQtWindow_nativeAddWidget(JNIEnv* /*env*/, jobject /*thiz*/, jlong handle, jlong childHandle) {
    QWidget* parent = reinterpret_cast<QWidget*>(handle);
    QWidget* child = reinterpret_cast<QWidget*>(childHandle);

    child->setParent(parent);

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

// 设置布局管理器：布局接管子控件的位置与大小（Qt 6 重复设置会删除旧布局）
JNIEXPORT void JNICALL Java_org_jqt_JQtWindow_nativeSetLayout(JNIEnv* /*env*/, jobject /*thiz*/, jlong handle, jlong layoutHandle) {
    QWidget* widget = reinterpret_cast<QWidget*>(handle);
    QLayout* layout = reinterpret_cast<QLayout*>(layoutHandle);
    widget->setLayout(layout);
}

// ----------------------------------------------------------------------------
// JQtButton：QPushButton 的封装（clicked/pressed/released/toggled 信号）
// ----------------------------------------------------------------------------

JNIEXPORT jlong JNICALL Java_org_jqt_JQtButton_nativeCreate(JNIEnv* env, jobject thiz, jstring text) {
    const char* utf = env->GetStringUTFChars(text, nullptr);
    QPushButton* btn = new QPushButton(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);

    // 把 Qt 的四个信号接回 Java 层：C++ lambda → JNI → Java nativeHandleXxx()
    // gRef 是全局引用，保证 Java 按钮对象不被 GC，lambda 生命周期内始终有效
    jobject gRef = env->NewGlobalRef(thiz);
    QObject::connect(btn, &QPushButton::clicked, [gRef]() {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleClick", "()V");
        if (mid != nullptr) {
            e->CallVoidMethod(gRef, mid);
        }
    });
    QObject::connect(btn, &QPushButton::pressed, [gRef]() {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandlePressed", "()V");
        if (mid != nullptr) {
            e->CallVoidMethod(gRef, mid);
        }
    });
    QObject::connect(btn, &QPushButton::released, [gRef]() {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleReleased", "()V");
        if (mid != nullptr) {
            e->CallVoidMethod(gRef, mid);
        }
    });
    QObject::connect(btn, &QPushButton::toggled, [gRef](bool checked) {
        JNIEnv* e = callbackEnv();
        jclass cls = e->GetObjectClass(gRef);
        jmethodID mid = e->GetMethodID(cls, "nativeHandleToggled", "(Z)V");
        if (mid != nullptr) {
            e->CallVoidMethod(gRef, mid, static_cast<jboolean>(checked));
        }
    });

    return reinterpret_cast<jlong>(btn);
}

JNIEXPORT void JNICALL Java_org_jqt_JQtButton_nativeSetText(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring text) {
    const char* utf = env->GetStringUTFChars(text, nullptr);
    reinterpret_cast<QPushButton*>(handle)->setText(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
}

JNIEXPORT void JNICALL Java_org_jqt_JQtButton_nativeSetCheckable(JNIEnv* /*env*/, jobject /*thiz*/, jlong handle, jboolean checkable) {
    reinterpret_cast<QPushButton*>(handle)->setCheckable(checkable == JNI_TRUE);
}

JNIEXPORT void JNICALL Java_org_jqt_JQtButton_nativeSetChecked(JNIEnv* /*env*/, jobject /*thiz*/, jlong handle, jboolean checked) {
    reinterpret_cast<QPushButton*>(handle)->setChecked(checked == JNI_TRUE);
}

// ----------------------------------------------------------------------------
// JQtLabel：QLabel 的封装
// ----------------------------------------------------------------------------

JNIEXPORT jlong JNICALL Java_org_jqt_JQtLabel_nativeCreate(JNIEnv* env, jobject /*thiz*/, jstring text) {
    const char* utf = env->GetStringUTFChars(text, nullptr);
    QLabel* label = new QLabel(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
    return reinterpret_cast<jlong>(label);
}

JNIEXPORT void JNICALL Java_org_jqt_JQtLabel_nativeSetText(JNIEnv* env, jobject /*thiz*/, jlong handle, jstring text) {
    const char* utf = env->GetStringUTFChars(text, nullptr);
    reinterpret_cast<QLabel*>(handle)->setText(QString::fromUtf8(utf));
    env->ReleaseStringUTFChars(text, utf);
}

// ----------------------------------------------------------------------------
// JQtLayout / JQtVBoxLayout / JQtHBoxLayout：布局管理器（Phase 3）
// ----------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_org_jqt_JQtLayout_nativeAddWidget(JNIEnv* /*env*/, jobject /*thiz*/, jlong handle, jlong childHandle) {
    QBoxLayout* layout = reinterpret_cast<QBoxLayout*>(handle);
    QWidget* child = reinterpret_cast<QWidget*>(childHandle);
    layout->addWidget(child);
    child->show();
}

JNIEXPORT void JNICALL Java_org_jqt_JQtLayout_nativeSetSpacing(JNIEnv* /*env*/, jobject /*thiz*/, jlong handle, jint spacing) {
    reinterpret_cast<QBoxLayout*>(handle)->setSpacing(static_cast<int>(spacing));
}

JNIEXPORT void JNICALL Java_org_jqt_JQtLayout_nativeAddStretch(JNIEnv* /*env*/, jobject /*thiz*/, jlong handle, jint stretch) {
    reinterpret_cast<QBoxLayout*>(handle)->addStretch(static_cast<int>(stretch));
}

JNIEXPORT jlong JNICALL Java_org_jqt_JQtVBoxLayout_nativeCreate(JNIEnv* /*env*/, jobject /*thiz*/) {
    // 布局对象由窗口 setLayout 后归 Qt 父子关系管理；未设置前由进程退出回收
    return reinterpret_cast<jlong>(new QVBoxLayout());
}

JNIEXPORT jlong JNICALL Java_org_jqt_JQtHBoxLayout_nativeCreate(JNIEnv* /*env*/, jobject /*thiz*/) {
    return reinterpret_cast<jlong>(new QHBoxLayout());
}
