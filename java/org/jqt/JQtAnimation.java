/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.function.Consumer;

/**
 * 高级属性动画（QPropertyAnimation 的 Java 封装）。
 * <p>
 * 用法：
 * <pre>
 * JQtAnimation anim = new JQtAnimation(button, "geometry",
 *         new JQtRect(0, 0, 100, 100), new JQtRect(100, 100, 200, 200), 400,
 *         JQtEasing.OUT_BOUNCE);
 * anim.setLoopCount(3);
 * anim.onFinished(() -&gt; System.out.println("done"));
 * anim.start();
 * </pre>
 * <p>
 * 说明：目前几何/数值属性以 {@code double} 双端点驱动
 * （{@code geometry} 之外的自定义数值属性用 {@code double} from/to 最稳）。
 * 窗口/控件便捷动画请用 {@link JQtWidget#animateMove} 等。
 */
public class JQtAnimation {

    /** 动画句柄（C++ QPropertyAnimation 注册表 ID）。 */
    private final long animHandle;

    private Consumer<JQtAnimation> onFinishedListener;

    /**
     * 创建属性动画。
     *
     * @param widget   目标控件
     * @param property Qt 属性名（如 "pos" / "size" / "windowOpacity"）
     * @param from     起始值（double；几何属性按当前值缩放）
     * @param to       目标值
     * @param ms       时长（毫秒）
     * @param easing   缓动函数
     */
    public JQtAnimation(JQtWidget widget, String property, double from, double to, long ms, JQtEasing easing) {
        this.animHandle = JQtWidget.nativeCreateAnimation(widget.nativeHandle(), property, from, to, ms, easing.qtType);
        JQtWidget.nativeRegisterAnimation(animHandle, this);
    }

    /** 设置循环次数（-1 = 无限循环）。 */
    public JQtAnimation setLoopCount(int loops) {
        JQtWidget.nativeAnimationSetLoopCount(animHandle, loops);
        return this;
    }

    /** 启动动画（结束后 C++ 对象自动销毁）。 */
    public void start() {
        JQtWidget.nativeAnimationStart(animHandle);
    }

    /** 停止动画（不销毁；可再次 start）。 */
    public void stop() {
        JQtWidget.nativeAnimationStop(animHandle);
    }

    /** 注册完成回调（动画 finished 信号时在 GUI 线程调用）。 */
    public JQtAnimation onFinished(Consumer<JQtAnimation> listener) {
        this.onFinishedListener = listener;
        return this;
    }

    /** JNI 侧 finished 信号回调入口（勿手动调用）。 */
    void nativeHandleFinished() {
        if (onFinishedListener != null) {
            onFinishedListener.accept(this);
        }
    }
}
