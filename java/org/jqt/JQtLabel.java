package org.jqt;

/**
 * 文本标签：封装 C++ 侧的 {@code QLabel}。
 */
public class JQtLabel extends JQtWidget {

    public JQtLabel(String text) {
        nativeHandle = nativeCreate(text);
    }

    private native long nativeCreate(String text);

    /** 修改标签文字。 */
    public void setText(String text) {
        nativeSetText(nativeHandle, text);
    }
    private native void nativeSetText(long handle, String text);
}
