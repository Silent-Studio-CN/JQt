/*
 * JQt Gallery —— 全功能演示（主题 / 控件 / 动画 / 窗口）
 * 社区贡献（jpackage 打包版随附）；本源码由 gallery.jar 反编译恢复（CFR 0.152），
 * 变量名已泛化——欢迎原作者提供原始源码替换（Community/ 政策：只收源码）。
 * (C) SilentStudio, All rights reserved.
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 */
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.jqt.JQtAnimationTheme;
import org.jqt.QApplication;
import org.jqt.QPushButton;
import org.jqt.QCheckBox;
import org.jqt.QComboBox;
import org.jqt.JQtEasing;
import org.jqt.QHBoxLayout;
import org.jqt.QLabel;
import org.jqt.QLineEdit;
import org.jqt.QListWidget;
import org.jqt.QFrame;
import org.jqt.JQtPivot;
import org.jqt.JQtSwitch;
import org.jqt.QVBoxLayout;
import org.jqt.QMainWindow;

public class JQtGallery {
    static QApplication app;
    static QMainWindow w;
    static QLabel logLabel;
    static QLabel geoLabel;
    static QFrame themePanel;
    static QFrame ctrlPanel;
    static QFrame animPanel;
    static QFrame winPanel;
    static Map<String, String> currentVars;
    static boolean currentLight;
    static String themeName;
    static StringBuilder logBuf;

    static void log(String string) {
        String[] stringArray;
        String string2;
        System.out.println("[G] " + string);
        logBuf.append(string).append('\n');
        if (logBuf.length() > 400) {
            logBuf.delete(0, logBuf.length() - 400);
        }
        String string3 = string2 = (stringArray = logBuf.toString().split("\\n")).length > 0 ? stringArray[stringArray.length - 1] : "";
        if (string2.length() > 80) {
            string2 = string2.substring(string2.length() - 80);
        }
        logLabel.setText(string2);
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    static String readThemeTemplate() {
        try {
            String tpl = new String(Files.readAllBytes(Paths.get("themes/fluent.qss.tpl", new String[0])), StandardCharsets.UTF_8);
            if (!tpl.isEmpty()) {
                return tpl;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        try (InputStream in = JQtGallery.class.getResourceAsStream("/themes/fluent.qss.tpl")) {
            if (in == null) return "";
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (Exception exception) {
            // empty catch block
        }
        return "";
    }

    static void renderTheme(String string, Map<String, String> map, boolean bl) {
        themeName = string;
        currentVars = new HashMap<String, String>(map);
        currentLight = bl;
        String string2 = JQtGallery.readThemeTemplate();
        if (string2.isEmpty()) {
            JQtGallery.log("\u8b66\u544a: \u4e3b\u9898\u6a21\u677f\u7f3a\u5931\uff0c\u56de\u9000\u5185\u7f6e fluent-dark");
            app.setTheme("fluent-dark");
            return;
        }
        for (Map.Entry<String, String> entry : currentVars.entrySet()) {
            string2 = string2.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        app.setStyleSheet(string2);
        JQtGallery.log("\u4e3b\u9898 -> " + string);
    }

    static void applyTheme(String string) {
        if (string.equals("fluent-dark")) {
            JQtGallery.renderTheme("fluent-dark(\u5b98\u65b9\u6697\u8272)", QApplication.FLUENT_DARK, false);
        } else if (string.equals("fluent-light")) {
            JQtGallery.renderTheme("fluent-light(\u5b98\u65b9\u6d45\u8272)", QApplication.FLUENT_LIGHT, true);
        } else if (string.equals("nord")) {
            JQtGallery.renderTheme("Nord \u5317\u6781\u84dd", NordTheme.vars(), false);
        } else if (string.equals("solarized")) {
            JQtGallery.renderTheme("Solarized \u62a4\u773c", SolarizedTheme.vars(), true);
        } else if (string.equals("terminal")) {
            JQtGallery.renderTheme("Terminal \u8367\u5149\u7eff", TerminalTheme.vars(), false);
        }
    }

    static void setAccent(String string) {
        if (currentVars != null) {
            currentVars.put("accent", string);
            currentVars.put("accent-fg", currentLight ? "#FFFFFF" : "#000000");
            JQtGallery.renderTheme(themeName + " + \u5f3a\u8c03\u8272", currentVars, currentLight);
        } else {
            app.setAccentColor(string);
        }
        JQtGallery.log("\u5f3a\u8c03\u8272 -> " + string);
    }

    static QPushButton makeBtn(String string, Runnable runnable) {
        QPushButton QPushButton = new QPushButton(string);
        QPushButton.onClicked(runnable);
        return QPushButton;
    }

    public static void main(String[] stringArray) {
        QMainWindow QMainWindow;
        boolean bl2 = Long.getLong("g.auto", 0L) > 0L;
        app = new QApplication();
        w = QMainWindow = new QMainWindow("JQt Gallery \u5168\u529f\u80fd\u6f14\u793a", 560, 780);
        QMainWindow.setFrameless(true);
        QMainWindow.setRoundedCorners(true);
        QMainWindow.setDraggable(true);
        QLabel QLabel = new QLabel("JQt Gallery \u5168\u529f\u80fd\u6f14\u793a");
        QLabel QLabel2 = new QLabel("\u4e3b\u9898 / \u63a7\u4ef6 / \u52a8\u753b / \u7a97\u53e3 \u00b7 \u6bcf\u9879\u90fd\u53ef\u70b9\u51fb\u4f53\u9a8c");
        QLabel2.setObjectName("cardMeta");
        JQtPivot jQtPivot = new JQtPivot();
        jQtPivot.addItem("\u4e3b\u9898");
        jQtPivot.addItem("\u63a7\u4ef6");
        jQtPivot.addItem("\u52a8\u753b");
        jQtPivot.addItem("\u7a97\u53e3");
        themePanel = new QFrame();
        themePanel.setObjectName("card1");
        QVBoxLayout QVBoxLayout = new QVBoxLayout();
        QVBoxLayout.setSpacing(8);
        QVBoxLayout.addWidget(new QLabel("\u2460 \u4e3b\u9898\u5207\u6362\uff08ThemePack \u4e09\u5957\u539f\u521b + \u5b98\u65b9\u4e24\u5957\uff09"));
        QHBoxLayout QHBoxLayout = new QHBoxLayout();
        QHBoxLayout.setSpacing(6);
        QHBoxLayout.addWidget(JQtGallery.makeBtn("Nord \u5317\u6781\u84dd", () -> JQtGallery.applyTheme("nord")));
        QHBoxLayout.addWidget(JQtGallery.makeBtn("Solarized \u62a4\u773c", () -> JQtGallery.applyTheme("solarized")));
        QHBoxLayout.addWidget(JQtGallery.makeBtn("Terminal \u8367\u5149\u7eff", () -> JQtGallery.applyTheme("terminal")));
        QVBoxLayout.addLayout(QHBoxLayout);
        QHBoxLayout QHBoxLayout2 = new QHBoxLayout();
        QHBoxLayout2.setSpacing(6);
        QHBoxLayout2.addWidget(JQtGallery.makeBtn("\u5b98\u65b9\u6697\u8272", () -> JQtGallery.applyTheme("fluent-dark")));
        QHBoxLayout2.addWidget(JQtGallery.makeBtn("\u5b98\u65b9\u6d45\u8272", () -> JQtGallery.applyTheme("fluent-light")));
        QVBoxLayout.addLayout(QHBoxLayout2);
        QVBoxLayout.addWidget(new QLabel("\u2461 \u5f3a\u8c03\u8272\uff08\u6539 %accent% \u53d8\u91cf\u91cd\u6e32\u67d3\u6a21\u677f\uff09"));
        QHBoxLayout QHBoxLayout3 = new QHBoxLayout();
        QHBoxLayout3.setSpacing(6);
        QHBoxLayout3.addWidget(JQtGallery.makeBtn("\u84dd #0078D4", () -> JQtGallery.setAccent("#0078D4")));
        QHBoxLayout3.addWidget(JQtGallery.makeBtn("\u9752 #00BFA5", () -> JQtGallery.setAccent("#00BFA5")));
        QHBoxLayout3.addWidget(JQtGallery.makeBtn("\u7d2b #9C27B0", () -> JQtGallery.setAccent("#9C27B0")));
        QHBoxLayout3.addWidget(JQtGallery.makeBtn("\u6a59 #FF6D00", () -> JQtGallery.setAccent("#FF6D00")));
        QVBoxLayout.addLayout(QHBoxLayout3);
        QVBoxLayout.addWidget(new QLabel("\u2462 \u5f3a\u8c03\u8272\u9884\u89c8\uff1a\u52fe\u9009\u4e0b\u9762\u7684\u6309\u94ae / \u590d\u9009\u6846\u770b %accent% \u6548\u679c"));
        QHBoxLayout QHBoxLayout4 = new QHBoxLayout();
        QHBoxLayout4.setSpacing(6);
        QPushButton QPushButton = new QPushButton("\u52fe\u9009\u6211\uff08\u9009\u4e2d\u6001=\u5f3a\u8c03\u8272\uff09");
        QPushButton.setCheckable(true);
        QPushButton.onToggled(bl -> JQtGallery.log("\u5f3a\u8c03\u8272\u6309\u94ae -> " + bl + "\uff08QPushButton:checked \u7528 %accent% \u586b\u5145\uff09"));
        QCheckBox QCheckBox = new QCheckBox("\u5f3a\u8c03\u8272\u590d\u9009\u6846");
        QCheckBox.onToggled(bl -> JQtGallery.log("\u5f3a\u8c03\u8272\u590d\u9009\u6846 -> " + bl + "\uff08indicator:checked \u7528 %accent% \u586b\u5145\uff09"));
        QHBoxLayout4.addWidget(QPushButton);
        QHBoxLayout4.addWidget(QCheckBox);
        QVBoxLayout.addLayout(QHBoxLayout4);
        QVBoxLayout.addWidget(new QLabel("\u2463 \u8ddf\u968f\u7cfb\u7edf\u6df1\u6d45\u8272 + \u5168\u5c40\u52a8\u753b\u8282\u594f"));
        QHBoxLayout QHBoxLayout5 = new QHBoxLayout();
        QHBoxLayout5.setSpacing(6);
        JQtSwitch jQtSwitch = new JQtSwitch(false);
        jQtSwitch.onToggled(bl -> {
            app.setAutoTheme((boolean)bl);
            JQtGallery.log("\u81ea\u52a8\u4e3b\u9898 -> " + bl);
        });
        QHBoxLayout5.addWidget(new QLabel("\u81ea\u52a8\u4e3b\u9898"));
        QHBoxLayout5.addWidget(jQtSwitch);
        QComboBox QComboBox = new QComboBox();
        QComboBox.addItem("\u52a8\u753b\u8282\u594f: \u9ed8\u8ba4");
        QComboBox.addItem("\u52a8\u753b\u8282\u594f: \u5feb");
        QComboBox.addItem("\u52a8\u753b\u8282\u594f: \u8212\u7f13");
        QComboBox.addItem("\u52a8\u753b\u8282\u594f: \u5173\u95ed");
        QComboBox.onCurrentIndexChanged(n -> {
            JQtAnimationTheme jQtAnimationTheme = n == 0 ? JQtAnimationTheme.DEFAULT : (n == 1 ? JQtAnimationTheme.FAST : (n == 2 ? JQtAnimationTheme.RELAXED : JQtAnimationTheme.OFF));
            QApplication.setAnimationTheme(jQtAnimationTheme);
            JQtGallery.log("\u52a8\u753b\u8282\u594f -> " + n);
        });
        QHBoxLayout5.addWidget(QComboBox);
        QVBoxLayout.addLayout(QHBoxLayout5);
        themePanel.setLayout(QVBoxLayout);
        ctrlPanel = new QFrame();
        ctrlPanel.setObjectName("card1");
        QVBoxLayout QVBoxLayout2 = new QVBoxLayout();
        QVBoxLayout2.setSpacing(8);
        QVBoxLayout2.addWidget(new QLabel("\u2460 \u5f00\u5173 / \u590d\u9009\u6846\uff08toggled \u4e8b\u4ef6\uff09"));
        QHBoxLayout QHBoxLayout6 = new QHBoxLayout();
        JQtSwitch jQtSwitch2 = new JQtSwitch(true);
        jQtSwitch2.onToggled(bl -> JQtGallery.log("\u5f00\u5173 -> " + bl));
        QCheckBox QCheckBox2 = new QCheckBox("\u52fe\u9009\u6211");
        QCheckBox2.onToggled(bl -> JQtGallery.log("\u590d\u9009\u6846 -> " + bl));
        QHBoxLayout6.addWidget(jQtSwitch2);
        QHBoxLayout6.addWidget(QCheckBox2);
        QVBoxLayout2.addLayout(QHBoxLayout6);
        QVBoxLayout2.addWidget(new QLabel("\u2461 \u8f93\u5165\u6846\uff08\u5b9e\u65f6 textChanged / \u56de\u8f66\u786e\u8ba4\uff09"));
        QLineEdit QLineEdit = new QLineEdit("");
        QLineEdit.setPlaceholderText("\u5728\u8fd9\u91cc\u8f93\u5165\u6587\u5b57\uff0c\u6309\u56de\u8f66...");
        QLineEdit.onTextChanged(string -> {
            if (!string.isEmpty()) {
                JQtGallery.log("\u8f93\u5165: " + string);
            }
        });
        QLineEdit.onReturnPressed(() -> JQtGallery.log("\u56de\u8f66\u786e\u8ba4: " + QLineEdit.text()));
        QVBoxLayout2.addWidget(QLineEdit);
        QVBoxLayout2.addWidget(new QLabel("\u2462 \u4e0b\u62c9\u6846 + \u5217\u8868\uff08\u9009\u62e9\u4e8b\u4ef6\uff09"));
        QHBoxLayout QHBoxLayout7 = new QHBoxLayout();
        QComboBox QComboBox2 = new QComboBox();
        QComboBox2.addItem("\u9009\u9879 A");
        QComboBox2.addItem("\u9009\u9879 B");
        QComboBox2.addItem("\u9009\u9879 C");
        QComboBox2.onCurrentIndexChanged(n -> JQtGallery.log("\u4e0b\u62c9\u6846 -> " + n + " " + QComboBox2.currentText()));
        QListWidget QListWidget = new QListWidget();
        QListWidget.addItem("\u884c 1");
        QListWidget.addItem("\u884c 2");
        QListWidget.addItem("\u884c 3");
        QListWidget.onItemClicked(n -> JQtGallery.log("\u5217\u8868\u70b9\u51fb -> " + n));
        QHBoxLayout7.addWidget(QComboBox2);
        QHBoxLayout7.addWidget(QListWidget);
        QVBoxLayout2.addLayout(QHBoxLayout7);
        QVBoxLayout2.addWidget(new QLabel("\u2463 \u7981\u7528 / \u60ac\u5782\u4fdd\u62a4\uff08\u5185\u5b58\u7ba1\u7406\u6f14\u793a\uff09"));
        QHBoxLayout QHBoxLayout8 = new QHBoxLayout();
        QPushButton QPushButton2 = JQtGallery.makeBtn("\u6211\u53ef\u80fd\u88ab\u7981\u7528", () -> JQtGallery.log("\u6309\u94ae\u88ab\u70b9\u51fb\u4e86"));
        QPushButton QPushButton3 = JQtGallery.makeBtn("\u7981\u7528 / \u542f\u7528", () -> {
            QPushButton2.setEnabled(!QPushButton2.isEnabled());
            JQtGallery.log("\u6309\u94ae\u53ef\u7528 -> " + QPushButton2.isEnabled());
        });
        QPushButton QPushButton4 = JQtGallery.makeBtn("\u60ac\u5782\u6f14\u793a", () -> {
            try {
                QPushButton ghostBtn = new QPushButton("ghost");
                ghostBtn.dispose();
                ghostBtn.setText("boom");
                JQtGallery.log("\u60ac\u5782\u4fdd\u62a4\u672a\u751f\u6548 (\u5f02\u5e38)");
            }
            catch (IllegalStateException illegalStateException) {
                JQtGallery.log("\u60ac\u5782\u4fdd\u62a4 OK: " + illegalStateException.getMessage());
            }
        });
        QHBoxLayout8.addWidget(QPushButton2);
        QHBoxLayout8.addWidget(QPushButton3);
        QHBoxLayout8.addWidget(QPushButton4);
        QVBoxLayout2.addLayout(QHBoxLayout8);
        ctrlPanel.setLayout(QVBoxLayout2);
        animPanel = new QFrame();
        animPanel.setObjectName("card1");
        QVBoxLayout QVBoxLayout3 = new QVBoxLayout();
        QVBoxLayout3.setSpacing(8);
        QVBoxLayout3.addWidget(new QLabel("\u2460 \u7a97\u53e3\u52a8\u753b\uff08\u6de1\u5165 / \u6de1\u51fa\uff09"));
        QHBoxLayout QHBoxLayout9 = new QHBoxLayout();
        QHBoxLayout9.addWidget(JQtGallery.makeBtn("\u7a97\u53e3\u6de1\u5165 400ms", () -> {
            w.fadeIn(400L);
            JQtGallery.log("\u7a97\u53e3\u6de1\u5165");
        }));
        QHBoxLayout9.addWidget(JQtGallery.makeBtn("\u7a97\u53e3\u6de1\u51fa 400ms", () -> {
            w.fadeOut(400L);
            JQtGallery.log("\u7a97\u53e3\u6de1\u51fa");
        }));
        QVBoxLayout3.addLayout(QHBoxLayout9);
        QVBoxLayout3.addWidget(new QLabel("\u2461 \u5361\u7247\u7f29\u653e\u52a8\u753b\uff085 \u79cd\u7f13\u52a8\u53ef\u9009\uff09"));
        QHBoxLayout QHBoxLayout10 = new QHBoxLayout();
        QFrame QFrame = new QFrame();
        QFrame.setObjectName("card");
        QFrame.setBorderRadius(10);
        QFrame.addWidget(new QLabel("\u5361\u7247"));
        QComboBox QComboBox3 = new QComboBox();
        QComboBox3.addItem("\u56de\u5f39 OUT_BOUNCE");
        QComboBox3.addItem("\u6a61\u76ae\u7b4b OUT_ELASTIC");
        QComboBox3.addItem("\u8fc7\u51b2 OUT_BACK");
        QComboBox3.addItem("\u7f13\u5165\u7f13\u51fa IN_OUT_CUBIC");
        QComboBox3.addItem("\u5300\u901f LINEAR");
        QPushButton QPushButton5 = JQtGallery.makeBtn("\u653e\u5927 300x120", () -> {
            JQtEasing jQtEasing = QComboBox3.currentIndex() == 0 ? JQtEasing.OUT_BOUNCE : (QComboBox3.currentIndex() == 1 ? JQtEasing.OUT_ELASTIC : (QComboBox3.currentIndex() == 2 ? JQtEasing.OUT_BACK : (QComboBox3.currentIndex() == 3 ? JQtEasing.IN_OUT_CUBIC : JQtEasing.LINEAR)));
            QFrame.animateResize(300, 120, 500L, jQtEasing);
            JQtGallery.log("\u5361\u7247\u653e\u5927 300x120 " + QComboBox3.currentText());
        });
        QPushButton QPushButton6 = JQtGallery.makeBtn("\u8fd8\u539f 180x60", () -> {
            QFrame.animateResize(180, 60, 300L, JQtEasing.OUT_QUAD);
            JQtGallery.log("\u5361\u7247\u8fd8\u539f 180x60");
        });
        QHBoxLayout10.addWidget(QPushButton5);
        QHBoxLayout10.addWidget(QPushButton6);
        QVBoxLayout3.addLayout(QHBoxLayout10);
        QVBoxLayout3.addWidget(QFrame);
        QVBoxLayout3.addWidget(QComboBox3);
        QVBoxLayout3.addWidget(new QLabel("\u2462 \u9634\u5f71 / \u5706\u89d2\uff08v0.3 \u6837\u5f0f API\uff09"));
        QHBoxLayout QHBoxLayout11 = new QHBoxLayout();
        QHBoxLayout11.addWidget(JQtGallery.makeBtn("\u52a0\u6295\u5f71", () -> {
            QFrame.setDropShadow(0, 6, 24, 50);
            JQtGallery.log("\u6295\u5f71\u5f00");
        }));
        QHBoxLayout11.addWidget(JQtGallery.makeBtn("\u53bb\u6295\u5f71", () -> {
            QFrame.clearDropShadow();
            JQtGallery.log("\u6295\u5f71\u5173");
        }));
        QHBoxLayout11.addWidget(JQtGallery.makeBtn("\u5706\u89d2 16", () -> {
            QFrame.setBorderRadius(16);
            JQtGallery.log("\u5706\u89d2 16");
        }));
        QVBoxLayout3.addLayout(QHBoxLayout11);
        animPanel.setLayout(QVBoxLayout3);
        winPanel = new QFrame();
        winPanel.setObjectName("card1");
        QVBoxLayout QVBoxLayout4 = new QVBoxLayout();
        QVBoxLayout4.setSpacing(8);
        QVBoxLayout4.addWidget(new QLabel("\u2460 \u7a97\u53e3\u7279\u6027\u5f00\u5173\uff08\u5b9e\u65f6\u5207\u6362\uff09"));
        QHBoxLayout QHBoxLayout12 = new QHBoxLayout();
        JQtSwitch jQtSwitch3 = new JQtSwitch(true);
        jQtSwitch3.onToggled(bl -> {
            w.setAcrylic((boolean)bl);
            JQtGallery.log("\u6bdb\u73bb\u7483 -> " + bl);
        });
        JQtSwitch jQtSwitch4 = new JQtSwitch(true);
        jQtSwitch4.onToggled(bl -> {
            w.setRoundedCorners((boolean)bl);
            JQtGallery.log("\u5706\u89d2 -> " + bl);
        });
        JQtSwitch jQtSwitch5 = new JQtSwitch(false);
        jQtSwitch5.onToggled(bl -> {
            w.setFrameless((boolean)bl);
            JQtGallery.log("\u65e0\u8fb9\u6846 -> " + bl);
        });
        QHBoxLayout12.addWidget(new QLabel("\u6bdb\u73bb\u7483"));
        QHBoxLayout12.addWidget(jQtSwitch3);
        QHBoxLayout12.addWidget(new QLabel("\u5706\u89d2"));
        QHBoxLayout12.addWidget(jQtSwitch4);
        QHBoxLayout12.addWidget(new QLabel("\u8fb9\u6846"));
        QHBoxLayout12.addWidget(jQtSwitch5);
        QVBoxLayout4.addLayout(QHBoxLayout12);
        QVBoxLayout4.addWidget(new QLabel("\u2461 \u7a97\u53e3\u63a7\u5236"));
        QHBoxLayout QHBoxLayout13 = new QHBoxLayout();
        QHBoxLayout13.addWidget(JQtGallery.makeBtn("\u6700\u5c0f\u5316", () -> {
            w.minimize();
            JQtGallery.log("\u6700\u5c0f\u5316");
        }));
        QHBoxLayout13.addWidget(JQtGallery.makeBtn("\u6700\u5927\u5316", () -> {
            w.maximize();
            JQtGallery.log("\u6700\u5927\u5316");
        }));
        QHBoxLayout13.addWidget(JQtGallery.makeBtn("\u5207\u6362\u6700\u5927", () -> {
            w.toggleMaximize();
            JQtGallery.log("\u5207\u6362\u6700\u5927\u5316");
        }));
        QVBoxLayout4.addLayout(QHBoxLayout13);
        geoLabel = new QLabel("\u51e0\u4f55\u4fe1\u606f: -");
        geoLabel.setObjectName("cardMeta");
        QVBoxLayout4.addWidget(geoLabel);
        winPanel.setLayout(QVBoxLayout4);
        logLabel = new QLabel("\u5c31\u7eea");
        logLabel.setObjectName("logLine");
        QVBoxLayout QVBoxLayout5 = new QVBoxLayout();
        QVBoxLayout5.setContentsMargins(14, 10, 14, 10);
        QVBoxLayout5.setSpacing(8);
        QVBoxLayout5.addWidget(QLabel);
        QVBoxLayout5.addWidget(QLabel2);
        QVBoxLayout5.addWidget(jQtPivot);
        QVBoxLayout5.addWidget(themePanel);
        QVBoxLayout5.addWidget(ctrlPanel);
        QVBoxLayout5.addWidget(animPanel);
        QVBoxLayout5.addWidget(winPanel);
        QVBoxLayout5.addWidget(logLabel);
        QMainWindow.setLayout(QVBoxLayout5);
        ctrlPanel.hide();
        animPanel.hide();
        winPanel.hide();
        jQtPivot.onChanged(n -> {
            if (n == 0) {
                themePanel.show();
            } else {
                themePanel.hide();
            }
            if (n == 1) {
                ctrlPanel.show();
            } else {
                ctrlPanel.hide();
            }
            if (n == 2) {
                animPanel.show();
            } else {
                animPanel.hide();
            }
            if (n == 3) {
                winPanel.show();
            } else {
                winPanel.hide();
            }
            JQtGallery.log("\u5206\u533a -> " + n);
        });
        JQtGallery.scheduleGeo(1000L);
        QMainWindow.onClose(() -> app.quit());
        JQtGallery.applyTheme("nord");
        QMainWindow.show();
        QMainWindow.fadeIn(300L);
        JQtGallery.log("\u6f14\u793a\u5c31\u7eea\uff0c\u4e3b\u9898=Nord");
        if (bl2) {
            app.schedule(() -> JQtGallery.applyTheme("solarized"), 1600L);
            app.schedule(() -> JQtGallery.setAccent("#00BFA5"), 2600L);
            app.schedule(() -> jQtPivot.setCurrentIndex(1), 3600L);
            app.schedule(() -> jQtSwitch2.setChecked(false), 4600L);
            app.schedule(() -> {
                try {
                    QPushButton closeBtn = new QPushButton("x");
                    closeBtn.dispose();
                    closeBtn.setText("y");
                    JQtGallery.log("\u81ea\u52a8\u60ac\u5782\u672a\u751f\u6548 (\u5f02\u5e38)");
                }
                catch (IllegalStateException illegalStateException) {
                    JQtGallery.log("\u81ea\u52a8\u60ac\u5782\u4fdd\u62a4 OK");
                }
            }, 5600L);
            app.schedule(() -> {
                JQtGallery.log("\u81ea\u52a8\u6f14\u793a\u5b8c\u6210");
                app.quit();
            }, 6500L);
        }
        app.exec();
    }

    static void scheduleGeo(long l) {
        app.schedule(() -> {
            geoLabel.setText("\u51e0\u4f55\u4fe1\u606f: x=" + w.x() + " y=" + w.y() + " \u5bbd=" + w.width() + " \u9ad8=" + w.height() + " \u53ef\u89c1=" + w.isVisible());
            JQtGallery.scheduleGeo(1000L);
        }, l);
    }

    static {
        currentLight = false;
        themeName = "nord";
        logBuf = new StringBuilder();
    }
}
