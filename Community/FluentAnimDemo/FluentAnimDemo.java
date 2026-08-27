import org.jqt.*;

/**
 * FluentAnimDemo —— 用 JQt v0.2 动画系统复刻 qfluentwidgets 的经典动效
 * 对应映射（qf -> JQt）：
 *   FADE_IN_OUT (83ms)      -> w.fadeIn(ms)
 *   SwitchButton slider    -> JQtSwitch 自带 120ms 滑块动画
 *   TranslateYAnimation    -> QSS :pressed + padding 下沉（按钮）
 *   FluentAnimation SCALE  -> animateResize + JQtEasing 缓动
 */
public class FluentAnimDemo {

    static QApplication app;
    static QFrame card;
    static boolean expanded = false;

    public static void main(String[] args) {
        app = new QApplication();
        app.setStyleSheet(
            "QWidget { font-family: 'Segoe UI','Microsoft YaHei'; }" +
            "QPushButton#animBtn { background: #3b64e8; color: white; border: none; border-radius: 6px;" +
            "  padding: 10px 24px; font-size: 14px; font-weight: bold; }" +
            "QPushButton#animBtn:hover { background: #4a74f0; }" +
            "QPushButton#animBtn:pressed { background: #2f55d6; padding-top: 11px; padding-bottom: 9px; }" +
            "QFrame#card { background: white; border: 1px solid #e3e8f0; border-radius: 8px; }" +
            "QLabel#tip { color: #666; font-size: 12px; }"
        );

        QMainWindow w = new QMainWindow("Fluent Anim Demo", 460, 400);
        w.setFrameless(true);
        w.setRoundedCorners(true);
        w.setDraggable(true);

        // 标题栏（可拖动）
        QLabel titleBar = new QLabel("  Fluent Anim Demo");
        titleBar.setStyleSheet("background: #f3f3f3; min-height: 36px; color: #333; font-weight: bold;");
        QPushButton closeBtn = new QPushButton("✕");
        closeBtn.setStyleSheet("QPushButton { background: transparent; border: none; color: #333; padding: 4px 12px; }" +
                               "QPushButton:hover { background: #e81123; color: white; }");
        closeBtn.onClicked(() -> w.close());
        QHBoxLayout tbBox = new QHBoxLayout();
        tbBox.addWidget(titleBar);
        tbBox.addStretch(1);
        tbBox.addWidget(closeBtn);
        QFrame tbHost = new QFrame();
        tbHost.setLayout(tbBox);

        // 按压按钮（QSS :pressed 下沉模拟 TranslateYAnimation）
        QPushButton pushBtn = new QPushButton("按压我（QSS 下沉反馈）");
        pushBtn.setObjectName("animBtn");
        pushBtn.onPressed(() -> System.out.println("[ANIM] pressed  -> TranslateY 语义: QSS padding 下沉"));
        pushBtn.onReleased(() -> System.out.println("[ANIM] released -> 回弹语义: 恢复原状"));

        // JQtSwitch（对应 qf SwitchButton 的 120ms sliderX 动画）
        JQtSwitch sw = new JQtSwitch(true);
        sw.setStyleSheet("JQtSwitch { spacing: 8px; }");
        sw.onToggled(b -> System.out.println("[ANIM] switch slider -> " + b + " (120ms 滑块动画)"));

        // 卡片缩放按钮（FluentAnimation SCALE 语义 -> animateResize）
        QPushButton scaleBtn = new QPushButton("卡片缩放动画（bounce）");
        scaleBtn.setObjectName("animBtn");

        card = new QFrame();
        card.setObjectName("card");
        QLabel cardText = new QLabel("我是卡片");
        QVBoxLayout cardBox = new QVBoxLayout();
        cardBox.addWidget(cardText);
        card.setLayout(cardBox);

        QLabel tip = new QLabel("qf 动画 = 属性插值 + 重绘；JQt 内置同款引擎（JQtEasing 40 缓动）");
        tip.setObjectName("tip");

        QVBoxLayout root = new QVBoxLayout();
        root.setSpacing(14);
        root.addWidget(tbHost);
        root.addWidget(pushBtn);
        root.addWidget(sw);
        root.addWidget(scaleBtn);
        root.addWidget(card);
        root.addStretch(1);
        root.addWidget(tip);
        w.setLayout(root);

        // 卡片缩放（SCALE 语义：160x60 <-> 300x120）
        scaleBtn.onClicked(() -> {
            if (!expanded) {
                System.out.println("[ANIM] card scale up: animateResize 300x120 OUT_BOUNCE 500ms");
                card.animateResize(300, 120, 500, JQtEasing.OUT_BOUNCE);
            } else {
                System.out.println("[ANIM] card scale down: animateResize 160x60 OUT_QUAD 300ms");
                card.animateResize(160, 60, 300, JQtEasing.OUT_QUAD);
            }
            expanded = !expanded;
        });

        w.onClose(() -> app.quit());
        w.show();
        w.fadeIn(400);   // FADE_IN_OUT 语义（qf 的 83ms 是菜单级，窗口用 400ms）
        System.out.println("[FluentAnimDemo] shown, fadeIn 400ms");

        long autoClose = Long.getLong("jqt.autoClose", -1L);
        if (autoClose > 0) app.scheduleQuit(autoClose);

        app.exec();
        System.out.println("[FluentAnimDemo] exited");
    }
}

