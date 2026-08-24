package org.jqt;

/**
 * 按钮：封装 C++ 侧的 {@code QPushButton}。
 */
public class JQtButton extends JQtWidget {

    private Runnable onClickHandler;

    public JQtButton(String text) {
        nativeHandle = nativeCreate(text);
    }

    private native long nativeCreate(String text);

    /** 修改按钮文字。 */
    public void setText(String text) {
        nativeSetText(nativeHandle, text);
    }
    private native void nativeSetText(long handle, String text);

    /**
     * 注册点击回调（对应 Qt 的 clicked 信号）。
     * C++ 信号 → JNI 回调 → Java lambda。
     */
    public void onClick(Runnable handler) {
        this.onClickHandler = handler;
    }

    /** 由 C++ 侧在按钮被点击时回调（JNI）。 */
    void nativeHandleClick() {
        if (onClickHandler != null) {
            onClickHandler.run();
        }
    }
}
