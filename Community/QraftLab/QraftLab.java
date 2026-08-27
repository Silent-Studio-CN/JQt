import org.jqt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * QraftLab v0.5.1 —— Qraft 实验室（合并 FluentButtonDemo + FluentAnimDemo）
 * 路线：QSS 美术 + 动画（不走 JQTG 的控件全演示路线）
 * 四区：① 按钮（颜色/质感，qf 立体边升级）② 动画（按压/弹跳/心跳/呼吸/脉冲）
 *       ③ 画布（QCanvasWidget 自绘）④ 渲染（rhiBackend 实操）
 * 主题：官方渲染式（FLUENT_DARK/LIGHT 22 变量 + 自定义模板追加）
 */
public class QraftLab {

    static QApplication app;
    static QMainWindow w;
    static QLabel logLabel;
    static QFrame btnPanel, animPanel, canvasPanel, rhiPanel;

    // ===== 主题状态（官方渲染式） =====
    static Map<String, String> currentVars;
    static boolean currentLight = false;
    static String themeName = "fluent-dark";

    // ===== 动画状态 =====
    static QPushButton pulseBtn;
    static boolean pulsing = false, beating = false, breathing = false;
    static QFrame card;
    static boolean expanded = false;

    static void log(String line) {
        System.out.println("[QL] " + line);
        logLabel.setText(line);
    }

    // ================= 主题模板（文件→jar 双回退） =================
    static String readThemeTemplate() {
        try {
            String f = new String(Files.readAllBytes(Paths.get("themes/fluent.qss.tpl")), StandardCharsets.UTF_8);
            if (!f.isEmpty()) return f;
        } catch (Exception ignored) { }
        try (InputStream in = QraftLab.class.getResourceAsStream("/themes/fluent.qss.tpl")) {
            if (in != null) return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ignored) { }
        return "";
    }

    /** 读取分离的自定义 QSS（themes/qraft-styles.qss，文件→jar 双回退） */
    static String readQraftStyles() {
        try {
            String f = new String(Files.readAllBytes(Paths.get("themes/qraft-styles.qss")), StandardCharsets.UTF_8);
            if (!f.isEmpty()) return f;
        } catch (Exception ignored) { }
        try (InputStream in = QraftLab.class.getResourceAsStream("/themes/qraft-styles.qss")) {
            if (in != null) return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ignored) { }
        return "";
    }
    /** 渲染核心（官方同款） */
    static void renderTheme(String name, Map<String, String> vars, boolean light) {
        themeName = name;
        currentVars = new HashMap<>(vars);
        currentLight = light;
        String tpl = readThemeTemplate();
        if (tpl.isEmpty()) {
            log("警告: 主题模板缺失，回退内置 fluent-dark");
            app.setTheme("fluent-dark");
            return;
        }
        tpl = tpl + "\n" + readQraftStyles();
        String accent = currentVars.getOrDefault("accent", "#0078D4");
        currentVars.put("accent-hover", lighten(accent, 0.12));
        currentVars.put("accent-pressed", darken(accent, 0.12));
        currentVars.put("accent-deep", darken(accent, 0.25));
        currentVars.put("accent-light", lighten(accent, 0.35));
        currentVars.put("accent-ghost-hover", hexWithAlpha(accent, 0.10));
        currentVars.put("accent-ghost-pressed", hexWithAlpha(accent, 0.18));
        for (Map.Entry<String, String> e : currentVars.entrySet()) {
            tpl = tpl.replace("%" + e.getKey() + "%", e.getValue());
        }
        app.setStyleSheet(tpl);
        log("主题 -> " + name);
    }

    static void applyTheme(String name) {
        if (name.equals("fluent-dark")) renderTheme("fluent-dark(官方暗色)", QApplication.FLUENT_DARK, false);
        else if (name.equals("fluent-light")) renderTheme("fluent-light(官方浅色)", QApplication.FLUENT_LIGHT, true);
    }

    static void setAccent(String hex) {
        if (currentVars != null) {
            currentVars.put("accent", hex);
            currentVars.put("accent-fg", currentLight ? "#FFFFFF" : "#000000");
            renderTheme(themeName + " + 强调色", currentVars, currentLight);
        } else {
            app.setAccentColor(hex);
        }
        log("强调色 -> " + hex);
    }

    static String lighten(String hex, double f) {
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            r = Math.min(255, (int)(r + (255 - r) * f));
            g = Math.min(255, (int)(g + (255 - g) * f));
            b = Math.min(255, (int)(b + (255 - b) * f));
            return String.format("#%02x%02x%02x", r, g, b);
        } catch (Exception e) { return hex; }
    }
    static String darken(String hex, double f) {
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            r = (int)(r * (1 - f)); g = (int)(g * (1 - f)); b = (int)(b * (1 - f));
            return String.format("#%02x%02x%02x", r, g, b);
        } catch (Exception e) { return hex; }
    }
    static String hexWithAlpha(String hex, double a) {
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            return String.format("rgba(%d, %d, %d, %d)", r, g, b, (int)(a * 255));
        } catch (Exception e) { return hex; }
    }

    static QPushButton makeBtn(String text, String objName) {
        QPushButton b = new QPushButton(text);
        b.setObjectName(objName);
        return b;
    }

    static QLabel note(String text) {
        QLabel l = new QLabel(text);
        l.setObjectName("btnNote");
        return l;
    }

    // ================= 区 1：按钮（颜色 + 质感） =================
    static QFrame buildBtnPanel() {
        QFrame panel = new QFrame();
        panel.setObjectName("card1");
        QVBoxLayout box = new QVBoxLayout();
        box.setSpacing(8);
        box.addWidget(new QLabel("① 按钮 · 颜色（qf 立体边：border-bottom 深 1px，pressed 压平）"));

        QPushButton b1 = makeBtn("主操作 Primary", "solidPrimary");
        b1.onClicked(() -> log("主按钮（%accent% 模板）"));
        QPushButton b2 = makeBtn("成功 Success", "solidSuccess");
        b2.onClicked(() -> log("成功绿"));
        QPushButton b3 = makeBtn("危险 Danger", "solidDanger");
        b3.onClicked(() -> log("危险红"));
        QPushButton b4 = makeBtn("青 Teal", "solidTeal");
        b4.onClicked(() -> log("青色"));
        QHBoxLayout row1 = new QHBoxLayout();
        row1.setSpacing(10);
        row1.addWidget(b1); row1.addWidget(b2); row1.addWidget(b3); row1.addWidget(b4);
        row1.addStretch(1);
        box.addLayout(row1);

        box.addWidget(new QLabel("② 按钮 · 质感"));

        QPushButton g1 = makeBtn("幽灵 Ghost", "ghost");
        g1.onClicked(() -> log("幽灵"));
        QPushButton g2 = makeBtn("描边 Outline", "outline");
        g2.onClicked(() -> log("描边"));
        QPushButton g3 = makeBtn("胶囊 Pill", "pill");
        g3.onClicked(() -> log("胶囊"));
        QHBoxLayout row2 = new QHBoxLayout();
        row2.setSpacing(10);
        row2.addWidget(g1); row2.addWidget(g2); row2.addWidget(g3);
        row2.addStretch(1);
        box.addLayout(row2);

        box.addWidget(new QLabel("③ 主题控制（明暗 / 强调色）"));
        QPushButton themeBtn = makeBtn("切换明暗", "smallBtn");
        themeBtn.onClicked(() -> {
            if (themeName.startsWith("fluent-dark")) applyTheme("fluent-light");
            else applyTheme("fluent-dark");
        });
        QPushButton accentBtn = makeBtn("强调色: 蓝", "smallBtn");
        final String[][] accents = {{"蓝","#0078D4"},{"青","#00BFA5"},{"紫","#9C27B0"},{"橙","#FF6D00"}};
        final int[] ai = {0};
        accentBtn.onClicked(() -> {
            ai[0] = (ai[0] + 1) % 4;
            accentBtn.setText("强调色: " + accents[ai[0]][0]);
            setAccent(accents[ai[0]][1]);
        });
        QHBoxLayout row3 = new QHBoxLayout();
        row3.setSpacing(10);
        row3.addWidget(themeBtn); row3.addWidget(accentBtn);
        row3.addStretch(1);
        box.addLayout(row3);

        panel.setLayout(box);
        return panel;
    }

    // ================= 区 2：动画 =================
    static QFrame buildAnimPanel() {
        QFrame panel = new QFrame();
        panel.setObjectName("card2");
        QVBoxLayout box = new QVBoxLayout();
        box.setSpacing(8);
        box.addWidget(new QLabel("① 动画 · 按压 / 弹跳 / 心跳 / 呼吸 / 脉冲"));

        QPushButton sinkBtn = makeBtn("按压下沉", "solidPrimary");
        sinkBtn.onPressed(() -> log("按下：立体边压平 + padding 下沉"));
        sinkBtn.onReleased(() -> log("松开：恢复"));
        QPushButton bounceBtn = makeBtn("弹跳 Bounce", "solidSuccess");
        bounceBtn.onClicked(() -> {
            log("animateResize OUT_BOUNCE 300ms");
            bounceBtn.animateResize(bounceBtn.width() + 20, bounceBtn.height() + 10, 300, JQtEasing.OUT_BOUNCE);
            app.schedule(() -> bounceBtn.animateResize(bounceBtn.width() - 20, bounceBtn.height() - 10, 300, JQtEasing.OUT_QUAD), 320);
        });
        QPushButton beatBtn = makeBtn("心跳 Beat", "solidDanger");
        beatBtn.onClicked(() -> {
            beating = !beating;
            log("心跳 " + (beating ? "开" : "关"));
            if (beating) beatLoop(beatBtn);
        });
        QPushButton breatheBtn = makeBtn("呼吸 Breathe", "solidTeal");
        breatheBtn.onClicked(() -> {
            breathing = !breathing;
            log("呼吸 " + (breathing ? "开" : "关"));
            if (breathing) breatheLoop(breatheBtn);
        });
        QHBoxLayout row1 = new QHBoxLayout();
        row1.setSpacing(10);
        row1.addWidget(sinkBtn); row1.addWidget(bounceBtn); row1.addWidget(beatBtn); row1.addWidget(breatheBtn);
        row1.addStretch(1);
        box.addLayout(row1);

        box.addWidget(new QLabel("② 开关滑块 + 卡片缩放"));
        JQtSwitch sw = new JQtSwitch(true);
        sw.setStyleSheet("JQtSwitch { spacing: 8px; }");
        sw.onToggled(b -> log("switch -> " + b + " (120ms)"));
        QPushButton scaleBtn = makeBtn("卡片缩放", "solidPrimary");
        scaleBtn.onClicked(() -> {
            if (!expanded) {
                log("card 360x140 OUT_BOUNCE");
                card.animateResize(360, 140, 500, JQtEasing.OUT_BOUNCE);
            } else {
                log("card 200x100 OUT_QUAD");
                card.animateResize(200, 100, 300, JQtEasing.OUT_QUAD);
            }
            expanded = !expanded;
        });
        // 缩放目标卡片（阴影 + 圆角）
        card = new QFrame();
        card.setObjectName("card");
        card.setBorderRadius(14);
        card.setDropShadow(0, 6, 24, 40);
        QLabel cardText = new QLabel("阴影卡片 · 点击缩放");
        QVBoxLayout cardBox = new QVBoxLayout();
        cardBox.setContentsMargins(16, 14, 16, 14);
        cardBox.addWidget(cardText);
        card.setLayout(cardBox);
        QHBoxLayout row2 = new QHBoxLayout();
        row2.setSpacing(10);
        row2.addWidget(sw); row2.addWidget(scaleBtn); row2.addWidget(card);
        row2.addStretch(1);
        box.addLayout(row2);

        pulseBtn = makeBtn("●", "pulseBtn");
        pulseBtn.setFixedSize(56, 56);
        pulseBtn.onClicked(() -> {
            pulsing = !pulsing;
            log("脉冲 " + (pulsing ? "开" : "关"));
            if (pulsing) pulseLoop(pulseBtn);
        });
        QHBoxLayout row3 = new QHBoxLayout();
        row3.setSpacing(10);
        row3.addWidget(pulseBtn);
        row3.addWidget(note("圆形脉冲 · 缩放循环"));
        row3.addStretch(1);
        box.addLayout(row3);

        panel.setLayout(box);
        return panel;
    }

    static void beatLoop(QPushButton btn) {
        if (!beating) return;
        btn.animateResize(btn.width() + 16, btn.height() + 8, 150, JQtEasing.OUT_QUAD);
        app.schedule(() -> {
            btn.animateResize(btn.width() - 16, btn.height() - 8, 150, JQtEasing.OUT_QUAD);
            app.schedule(() -> beatLoop(btn), 200);
        }, 170);
    }
    static void breatheLoop(QPushButton btn) {
        if (!breathing) return;
        btn.fadeOut(400);
        app.schedule(() -> {
            btn.fadeIn(400);
            app.schedule(() -> breatheLoop(btn), 100);
        }, 450);
    }
    static void pulseLoop(QPushButton btn) {
        if (!pulsing) return;
        btn.animateResize(64, 64, 200, JQtEasing.OUT_QUAD);
        app.schedule(() -> {
            btn.animateResize(56, 56, 200, JQtEasing.OUT_QUAD);
            app.schedule(() -> pulseLoop(btn), 80);
        }, 220);
    }

    // ================= 区 3：画布（QCanvasWidget 自绘） =================
    static QFrame buildCanvasPanel() {
        QFrame panel = new QFrame();
        panel.setObjectName("card3");
        QVBoxLayout box = new QVBoxLayout();
        box.setSpacing(8);
        box.addWidget(new QLabel("① QCanvasWidget + QPainter 自绘（线/矩形/圆/文本）"));

        QCanvasWidget canvas = new QCanvasWidget();
        canvas.setFixedSize(360, 120);
        canvas.onPaint(p -> {
            p.setColor(0xFF3b64e8);
            p.setStrokeWidth(2);
            p.drawRect(15, 15, 60, 45);
            p.fillRect(90, 15, 60, 45);
            p.setColor(0xFF00BFA5);
            p.drawCircle(175, 37, 22);
            p.fillCircle(230, 37, 16);
            p.setColor(0xFFE81123);
            p.drawRoundRect(15, 80, 120, 30, 10);
            p.setColor(0xFF9C27B0);
            p.drawLine(160, 100, 340, 70);
            p.setColor(0xFF000000);
            p.setFont("Microsoft YaHei UI", 11);
            p.drawText(200, 105, "QraftLab · Canvas");
        });

        QPushButton repaintBtn = makeBtn("重绘 repaint()", "smallBtn");
        repaintBtn.onClicked(() -> {
            canvas.repaint();
            log("画布重绘");
        });
        QHBoxLayout row1 = new QHBoxLayout();
        row1.setSpacing(10);
        row1.addWidget(repaintBtn);
        row1.addStretch(1);
        box.addLayout(row1);

        box.addWidget(new QLabel("② 滑块联动（QSlider）"));
        QSlider slider = new QSlider(0, 255, 100);
        QLabel sliderVal = new QLabel("100");
        sliderVal.setObjectName("cardMeta");
        slider.onValueChanged(v -> sliderVal.setText(String.valueOf(v)));
        QHBoxLayout row2 = new QHBoxLayout();
        row2.setSpacing(10);
        row2.addWidget(slider);
        row2.addWidget(sliderVal);
        row2.addStretch(1);
        box.addLayout(row2);

        panel.setLayout(box);
        return panel;
    }

    // ================= 区 4：渲染后端（rhiBackend 实操） =================
    static QFrame buildRhiPanel() {
        QFrame panel = new QFrame();
        panel.setObjectName("card4");
        QVBoxLayout box = new QVBoxLayout();
        box.setSpacing(8);
        box.addWidget(new QLabel("① 当前渲染后端（v0.5 rhiBackend API）"));

        QLabel rhiNow = new QLabel("查询中...");
        rhiNow.setObjectName("cardMeta");
        try {
            rhiNow.setText("当前后端: " + app.rhiBackend());
        } catch (Exception e) {
            rhiNow.setText("查询失败: " + e.getMessage());
        }
        box.addWidget(rhiNow);

        box.addWidget(new QLabel("② 尝试切换后端（官方注明：QWidget 在 Windows 固定 D3D11，opengl/vulkan 会回退）"));
        String[] backends = {"d3d11", "opengl", "vulkan", "software"};
        QHBoxLayout row = new QHBoxLayout();
        row.setSpacing(10);
        for (String bk : backends) {
            QPushButton b = makeBtn(bk, "smallBtn");
            b.onClicked(() -> {
                try {
                    app.rhiBackend(bk);
                    log("rhiBackend(" + bk + ") -> 现在: " + app.rhiBackend());
                } catch (Exception e) {
                    log("rhiBackend(" + bk + ") 失败: " + e.getMessage());
                }
            });
            row.addWidget(b);
        }
        row.addStretch(1);
        box.addLayout(row);

        box.addWidget(new QLabel("③ 自绘动画（Canvas + 帧循环，纯 Java 属性插值思路）"));
        QPushButton animCanvasBtn = makeBtn("旋转矩形动画", "smallBtn");
        animCanvasBtn.onClicked(() -> {
            log("Canvas 动画演示（简版：schedule 循环重绘）");
            app.schedule(() -> canvasRepaintTick(), 100);
        });
        QHBoxLayout row2 = new QHBoxLayout();
        row2.setSpacing(10);
        row2.addWidget(animCanvasBtn);
        row2.addStretch(1);
        box.addLayout(row2);

        panel.setLayout(box);
        return panel;
    }

    static int canvasTick = 0;
    static QCanvasWidget animCanvas;
    static void canvasRepaintTick() {
        canvasTick++;
        if (canvasTick > 60) { canvasTick = 0; log("画布动画结束"); return; }
        if (animCanvas != null) animCanvas.repaint();
        app.schedule(() -> canvasRepaintTick(), 33);
    }

    // ================= 主流程（官方 JQtGallery 模式） =================
    public static void main(String[] args) {
        app = new QApplication();
        app.setFontFamily("Microsoft YaHei UI", 13);
        app.setAutoTheme(true);

        w = new QMainWindow("QraftLab v0.5.1", 720, 660);
        w.setFrameless(true);
        w.setRoundedCorners(true);
        w.setDraggable(true);

        // 标题区
        QLabel title = new QLabel("QraftLab v0.5.1");
        QLabel sub = new QLabel("QSS 美术 + 动画 · 官方渲染式主题");
        sub.setObjectName("cardMeta");

        // 选项卡
        JQtPivot pivot = new JQtPivot();
        pivot.addItem("按钮");
        pivot.addItem("动画");
        pivot.addItem("画布");
        pivot.addItem("渲染");

        // 四个页面（直接进 root）
        btnPanel = buildBtnPanel();
        animPanel = buildAnimPanel();
        canvasPanel = buildCanvasPanel();
        rhiPanel = buildRhiPanel();

        logLabel = new QLabel("就绪");
        logLabel.setObjectName("logLine");

        QVBoxLayout root = new QVBoxLayout();
        root.setContentsMargins(14, 10, 14, 10);
        root.setSpacing(8);
        root.addWidget(title);
        root.addWidget(sub);
        root.addWidget(pivot);
        root.addWidget(btnPanel);
        root.addWidget(animPanel);
        root.addWidget(canvasPanel);
        root.addWidget(rhiPanel);
        root.addWidget(logLabel);
        w.setLayout(root);

        // setLayout 后 hide（官方顺序）
        animPanel.hide(); canvasPanel.hide(); rhiPanel.hide();
        pivot.onChanged(i -> {
            if (i == 0) { btnPanel.show(); } else { btnPanel.hide(); }
            if (i == 1) { animPanel.show(); } else { animPanel.hide(); }
            if (i == 2) { canvasPanel.show(); } else { canvasPanel.hide(); }
            if (i == 3) { rhiPanel.show(); } else { rhiPanel.hide(); }
            log("分区 -> " + i);
        });

        w.onClose(() -> app.quit());
        applyTheme("fluent-dark");
        w.show();
        w.fadeIn(300);
        System.out.println("[QraftLab v0.5.1] shown");

        long autoClose = Long.getLong("jqt.autoClose", -1L);
        if (autoClose > 0) app.scheduleQuit(autoClose);

        app.exec();
        System.out.println("[QraftLab v0.5.1] exited");
    }
}
