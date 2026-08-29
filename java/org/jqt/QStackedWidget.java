package org.jqt;

/** QStackedWidget（Qt Widgets，JQt 绑定）。 */
public class QStackedWidget extends QWidget {
    private final long nativeHandle;

    public QStackedWidget(long nativeHandle) {
        this.nativeHandle = nativeHandle;
    }
// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** count（Qt count）。 */
    public int count() {
        return nativeCount(nativeHandle);
    }
    private static native int nativeCount(long nativeHandle);

}
