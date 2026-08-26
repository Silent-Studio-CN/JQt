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
import org.jqt.JQtApplication;
import org.jqt.JQtButton;
import org.jqt.JQtCheckBox;
import org.jqt.JQtComboBox;
import org.jqt.JQtEasing;
import org.jqt.JQtHBoxLayout;
import org.jqt.JQtLabel;
import org.jqt.JQtLineEdit;
import org.jqt.JQtListWidget;
import org.jqt.JQtPanel;
import org.jqt.JQtPivot;
import org.jqt.JQtSwitch;
import org.jqt.JQtVBoxLayout;
import org.jqt.JQtWindow;

public class JQtGallery {
    static JQtApplication app;
    static JQtWindow w;
    static JQtLabel logLabel;
    static JQtLabel geoLabel;
    static JQtPanel themePanel;
    static JQtPanel ctrlPanel;
    static JQtPanel animPanel;
    static JQtPanel winPanel;
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
            JQtGallery.renderTheme("fluent-dark(\u5b98\u65b9\u6697\u8272)", JQtApplication.FLUENT_DARK, false);
        } else if (string.equals("fluent-light")) {
            JQtGallery.renderTheme("fluent-light(\u5b98\u65b9\u6d45\u8272)", JQtApplication.FLUENT_LIGHT, true);
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

    static JQtButton makeBtn(String string, Runnable runnable) {
        JQtButton jQtButton = new JQtButton(string);
        jQtButton.onClick(runnable);
        return jQtButton;
    }

    public static void main(String[] stringArray) {
        JQtWindow jQtWindow;
        boolean bl2 = Long.getLong("g.auto", 0L) > 0L;
        app = new JQtApplication();
        w = jQtWindow = new JQtWindow("JQt Gallery \u5168\u529f\u80fd\u6f14\u793a", 560, 780);
        jQtWindow.setFrameless(true);
        jQtWindow.setRoundedCorners(true);
        jQtWindow.setDraggable(true);
        JQtLabel jQtLabel = new JQtLabel("JQt Gallery \u5168\u529f\u80fd\u6f14\u793a");
        JQtLabel jQtLabel2 = new JQtLabel("\u4e3b\u9898 / \u63a7\u4ef6 / \u52a8\u753b / \u7a97\u53e3 \u00b7 \u6bcf\u9879\u90fd\u53ef\u70b9\u51fb\u4f53\u9a8c");
        jQtLabel2.setObjectName("cardMeta");
        JQtPivot jQtPivot = new JQtPivot();
        jQtPivot.addItem("\u4e3b\u9898");
        jQtPivot.addItem("\u63a7\u4ef6");
        jQtPivot.addItem("\u52a8\u753b");
        jQtPivot.addItem("\u7a97\u53e3");
        themePanel = new JQtPanel();
        themePanel.setObjectName("card1");
        JQtVBoxLayout jQtVBoxLayout = new JQtVBoxLayout();
        jQtVBoxLayout.setSpacing(8);
        jQtVBoxLayout.addWidget(new JQtLabel("\u2460 \u4e3b\u9898\u5207\u6362\uff08ThemePack \u4e09\u5957\u539f\u521b + \u5b98\u65b9\u4e24\u5957\uff09"));
        JQtHBoxLayout jQtHBoxLayout = new JQtHBoxLayout();
        jQtHBoxLayout.setSpacing(6);
        jQtHBoxLayout.addWidget(JQtGallery.makeBtn("Nord \u5317\u6781\u84dd", () -> JQtGallery.applyTheme("nord")));
        jQtHBoxLayout.addWidget(JQtGallery.makeBtn("Solarized \u62a4\u773c", () -> JQtGallery.applyTheme("solarized")));
        jQtHBoxLayout.addWidget(JQtGallery.makeBtn("Terminal \u8367\u5149\u7eff", () -> JQtGallery.applyTheme("terminal")));
        jQtVBoxLayout.addLayout(jQtHBoxLayout);
        JQtHBoxLayout jQtHBoxLayout2 = new JQtHBoxLayout();
        jQtHBoxLayout2.setSpacing(6);
        jQtHBoxLayout2.addWidget(JQtGallery.makeBtn("\u5b98\u65b9\u6697\u8272", () -> JQtGallery.applyTheme("fluent-dark")));
        jQtHBoxLayout2.addWidget(JQtGallery.makeBtn("\u5b98\u65b9\u6d45\u8272", () -> JQtGallery.applyTheme("fluent-light")));
        jQtVBoxLayout.addLayout(jQtHBoxLayout2);
        jQtVBoxLayout.addWidget(new JQtLabel("\u2461 \u5f3a\u8c03\u8272\uff08\u6539 %accent% \u53d8\u91cf\u91cd\u6e32\u67d3\u6a21\u677f\uff09"));
        JQtHBoxLayout jQtHBoxLayout3 = new JQtHBoxLayout();
        jQtHBoxLayout3.setSpacing(6);
        jQtHBoxLayout3.addWidget(JQtGallery.makeBtn("\u84dd #0078D4", () -> JQtGallery.setAccent("#0078D4")));
        jQtHBoxLayout3.addWidget(JQtGallery.makeBtn("\u9752 #00BFA5", () -> JQtGallery.setAccent("#00BFA5")));
        jQtHBoxLayout3.addWidget(JQtGallery.makeBtn("\u7d2b #9C27B0", () -> JQtGallery.setAccent("#9C27B0")));
        jQtHBoxLayout3.addWidget(JQtGallery.makeBtn("\u6a59 #FF6D00", () -> JQtGallery.setAccent("#FF6D00")));
        jQtVBoxLayout.addLayout(jQtHBoxLayout3);
        jQtVBoxLayout.addWidget(new JQtLabel("\u2462 \u5f3a\u8c03\u8272\u9884\u89c8\uff1a\u52fe\u9009\u4e0b\u9762\u7684\u6309\u94ae / \u590d\u9009\u6846\u770b %accent% \u6548\u679c"));
        JQtHBoxLayout jQtHBoxLayout4 = new JQtHBoxLayout();
        jQtHBoxLayout4.setSpacing(6);
        JQtButton jQtButton = new JQtButton("\u52fe\u9009\u6211\uff08\u9009\u4e2d\u6001=\u5f3a\u8c03\u8272\uff09");
        jQtButton.setCheckable(true);
        jQtButton.onToggled(bl -> JQtGallery.log("\u5f3a\u8c03\u8272\u6309\u94ae -> " + bl + "\uff08QPushButton:checked \u7528 %accent% \u586b\u5145\uff09"));
        JQtCheckBox jQtCheckBox = new JQtCheckBox("\u5f3a\u8c03\u8272\u590d\u9009\u6846");
        jQtCheckBox.onToggled(bl -> JQtGallery.log("\u5f3a\u8c03\u8272\u590d\u9009\u6846 -> " + bl + "\uff08indicator:checked \u7528 %accent% \u586b\u5145\uff09"));
        jQtHBoxLayout4.addWidget(jQtButton);
        jQtHBoxLayout4.addWidget(jQtCheckBox);
        jQtVBoxLayout.addLayout(jQtHBoxLayout4);
        jQtVBoxLayout.addWidget(new JQtLabel("\u2463 \u8ddf\u968f\u7cfb\u7edf\u6df1\u6d45\u8272 + \u5168\u5c40\u52a8\u753b\u8282\u594f"));
        JQtHBoxLayout jQtHBoxLayout5 = new JQtHBoxLayout();
        jQtHBoxLayout5.setSpacing(6);
        JQtSwitch jQtSwitch = new JQtSwitch(false);
        jQtSwitch.onToggled(bl -> {
            app.setAutoTheme((boolean)bl);
            JQtGallery.log("\u81ea\u52a8\u4e3b\u9898 -> " + bl);
        });
        jQtHBoxLayout5.addWidget(new JQtLabel("\u81ea\u52a8\u4e3b\u9898"));
        jQtHBoxLayout5.addWidget(jQtSwitch);
        JQtComboBox jQtComboBox = new JQtComboBox();
        jQtComboBox.addItem("\u52a8\u753b\u8282\u594f: \u9ed8\u8ba4");
        jQtComboBox.addItem("\u52a8\u753b\u8282\u594f: \u5feb");
        jQtComboBox.addItem("\u52a8\u753b\u8282\u594f: \u8212\u7f13");
        jQtComboBox.addItem("\u52a8\u753b\u8282\u594f: \u5173\u95ed");
        jQtComboBox.onCurrentIndexChanged(n -> {
            JQtAnimationTheme jQtAnimationTheme = n == 0 ? JQtAnimationTheme.DEFAULT : (n == 1 ? JQtAnimationTheme.FAST : (n == 2 ? JQtAnimationTheme.RELAXED : JQtAnimationTheme.OFF));
            JQtApplication.setAnimationTheme(jQtAnimationTheme);
            JQtGallery.log("\u52a8\u753b\u8282\u594f -> " + n);
        });
        jQtHBoxLayout5.addWidget(jQtComboBox);
        jQtVBoxLayout.addLayout(jQtHBoxLayout5);
        themePanel.setLayout(jQtVBoxLayout);
        ctrlPanel = new JQtPanel();
        ctrlPanel.setObjectName("card1");
        JQtVBoxLayout jQtVBoxLayout2 = new JQtVBoxLayout();
        jQtVBoxLayout2.setSpacing(8);
        jQtVBoxLayout2.addWidget(new JQtLabel("\u2460 \u5f00\u5173 / \u590d\u9009\u6846\uff08toggled \u4e8b\u4ef6\uff09"));
        JQtHBoxLayout jQtHBoxLayout6 = new JQtHBoxLayout();
        JQtSwitch jQtSwitch2 = new JQtSwitch(true);
        jQtSwitch2.onToggled(bl -> JQtGallery.log("\u5f00\u5173 -> " + bl));
        JQtCheckBox jQtCheckBox2 = new JQtCheckBox("\u52fe\u9009\u6211");
        jQtCheckBox2.onToggled(bl -> JQtGallery.log("\u590d\u9009\u6846 -> " + bl));
        jQtHBoxLayout6.addWidget(jQtSwitch2);
        jQtHBoxLayout6.addWidget(jQtCheckBox2);
        jQtVBoxLayout2.addLayout(jQtHBoxLayout6);
        jQtVBoxLayout2.addWidget(new JQtLabel("\u2461 \u8f93\u5165\u6846\uff08\u5b9e\u65f6 textChanged / \u56de\u8f66\u786e\u8ba4\uff09"));
        JQtLineEdit jQtLineEdit = new JQtLineEdit("");
        jQtLineEdit.setPlaceholderText("\u5728\u8fd9\u91cc\u8f93\u5165\u6587\u5b57\uff0c\u6309\u56de\u8f66...");
        jQtLineEdit.onTextChanged(string -> {
            if (!string.isEmpty()) {
                JQtGallery.log("\u8f93\u5165: " + string);
            }
        });
        jQtLineEdit.onReturnPressed(() -> JQtGallery.log("\u56de\u8f66\u786e\u8ba4: " + jQtLineEdit.text()));
        jQtVBoxLayout2.addWidget(jQtLineEdit);
        jQtVBoxLayout2.addWidget(new JQtLabel("\u2462 \u4e0b\u62c9\u6846 + \u5217\u8868\uff08\u9009\u62e9\u4e8b\u4ef6\uff09"));
        JQtHBoxLayout jQtHBoxLayout7 = new JQtHBoxLayout();
        JQtComboBox jQtComboBox2 = new JQtComboBox();
        jQtComboBox2.addItem("\u9009\u9879 A");
        jQtComboBox2.addItem("\u9009\u9879 B");
        jQtComboBox2.addItem("\u9009\u9879 C");
        jQtComboBox2.onCurrentIndexChanged(n -> JQtGallery.log("\u4e0b\u62c9\u6846 -> " + n + " " + jQtComboBox2.currentText()));
        JQtListWidget jQtListWidget = new JQtListWidget();
        jQtListWidget.addItem("\u884c 1");
        jQtListWidget.addItem("\u884c 2");
        jQtListWidget.addItem("\u884c 3");
        jQtListWidget.onItemClicked(n -> JQtGallery.log("\u5217\u8868\u70b9\u51fb -> " + n));
        jQtHBoxLayout7.addWidget(jQtComboBox2);
        jQtHBoxLayout7.addWidget(jQtListWidget);
        jQtVBoxLayout2.addLayout(jQtHBoxLayout7);
        jQtVBoxLayout2.addWidget(new JQtLabel("\u2463 \u7981\u7528 / \u60ac\u5782\u4fdd\u62a4\uff08\u5185\u5b58\u7ba1\u7406\u6f14\u793a\uff09"));
        JQtHBoxLayout jQtHBoxLayout8 = new JQtHBoxLayout();
        JQtButton jQtButton2 = JQtGallery.makeBtn("\u6211\u53ef\u80fd\u88ab\u7981\u7528", () -> JQtGallery.log("\u6309\u94ae\u88ab\u70b9\u51fb\u4e86"));
        JQtButton jQtButton3 = JQtGallery.makeBtn("\u7981\u7528 / \u542f\u7528", () -> {
            jQtButton2.setEnabled(!jQtButton2.isEnabled());
            JQtGallery.log("\u6309\u94ae\u53ef\u7528 -> " + jQtButton2.isEnabled());
        });
        JQtButton jQtButton4 = JQtGallery.makeBtn("\u60ac\u5782\u6f14\u793a", () -> {
            try {
                JQtButton ghostBtn = new JQtButton("ghost");
                ghostBtn.dispose();
                ghostBtn.setText("boom");
                JQtGallery.log("\u60ac\u5782\u4fdd\u62a4\u672a\u751f\u6548 (\u5f02\u5e38)");
            }
            catch (IllegalStateException illegalStateException) {
                JQtGallery.log("\u60ac\u5782\u4fdd\u62a4 OK: " + illegalStateException.getMessage());
            }
        });
        jQtHBoxLayout8.addWidget(jQtButton2);
        jQtHBoxLayout8.addWidget(jQtButton3);
        jQtHBoxLayout8.addWidget(jQtButton4);
        jQtVBoxLayout2.addLayout(jQtHBoxLayout8);
        ctrlPanel.setLayout(jQtVBoxLayout2);
        animPanel = new JQtPanel();
        animPanel.setObjectName("card1");
        JQtVBoxLayout jQtVBoxLayout3 = new JQtVBoxLayout();
        jQtVBoxLayout3.setSpacing(8);
        jQtVBoxLayout3.addWidget(new JQtLabel("\u2460 \u7a97\u53e3\u52a8\u753b\uff08\u6de1\u5165 / \u6de1\u51fa\uff09"));
        JQtHBoxLayout jQtHBoxLayout9 = new JQtHBoxLayout();
        jQtHBoxLayout9.addWidget(JQtGallery.makeBtn("\u7a97\u53e3\u6de1\u5165 400ms", () -> {
            w.fadeIn(400L);
            JQtGallery.log("\u7a97\u53e3\u6de1\u5165");
        }));
        jQtHBoxLayout9.addWidget(JQtGallery.makeBtn("\u7a97\u53e3\u6de1\u51fa 400ms", () -> {
            w.fadeOut(400L);
            JQtGallery.log("\u7a97\u53e3\u6de1\u51fa");
        }));
        jQtVBoxLayout3.addLayout(jQtHBoxLayout9);
        jQtVBoxLayout3.addWidget(new JQtLabel("\u2461 \u5361\u7247\u7f29\u653e\u52a8\u753b\uff085 \u79cd\u7f13\u52a8\u53ef\u9009\uff09"));
        JQtHBoxLayout jQtHBoxLayout10 = new JQtHBoxLayout();
        JQtPanel jQtPanel = new JQtPanel();
        jQtPanel.setObjectName("card");
        jQtPanel.setBorderRadius(10);
        jQtPanel.addWidget(new JQtLabel("\u5361\u7247"));
        JQtComboBox jQtComboBox3 = new JQtComboBox();
        jQtComboBox3.addItem("\u56de\u5f39 OUT_BOUNCE");
        jQtComboBox3.addItem("\u6a61\u76ae\u7b4b OUT_ELASTIC");
        jQtComboBox3.addItem("\u8fc7\u51b2 OUT_BACK");
        jQtComboBox3.addItem("\u7f13\u5165\u7f13\u51fa IN_OUT_CUBIC");
        jQtComboBox3.addItem("\u5300\u901f LINEAR");
        JQtButton jQtButton5 = JQtGallery.makeBtn("\u653e\u5927 300x120", () -> {
            JQtEasing jQtEasing = jQtComboBox3.currentIndex() == 0 ? JQtEasing.OUT_BOUNCE : (jQtComboBox3.currentIndex() == 1 ? JQtEasing.OUT_ELASTIC : (jQtComboBox3.currentIndex() == 2 ? JQtEasing.OUT_BACK : (jQtComboBox3.currentIndex() == 3 ? JQtEasing.IN_OUT_CUBIC : JQtEasing.LINEAR)));
            jQtPanel.animateResize(300, 120, 500L, jQtEasing);
            JQtGallery.log("\u5361\u7247\u653e\u5927 300x120 " + jQtComboBox3.currentText());
        });
        JQtButton jQtButton6 = JQtGallery.makeBtn("\u8fd8\u539f 180x60", () -> {
            jQtPanel.animateResize(180, 60, 300L, JQtEasing.OUT_QUAD);
            JQtGallery.log("\u5361\u7247\u8fd8\u539f 180x60");
        });
        jQtHBoxLayout10.addWidget(jQtButton5);
        jQtHBoxLayout10.addWidget(jQtButton6);
        jQtVBoxLayout3.addLayout(jQtHBoxLayout10);
        jQtVBoxLayout3.addWidget(jQtPanel);
        jQtVBoxLayout3.addWidget(jQtComboBox3);
        jQtVBoxLayout3.addWidget(new JQtLabel("\u2462 \u9634\u5f71 / \u5706\u89d2\uff08v0.3 \u6837\u5f0f API\uff09"));
        JQtHBoxLayout jQtHBoxLayout11 = new JQtHBoxLayout();
        jQtHBoxLayout11.addWidget(JQtGallery.makeBtn("\u52a0\u6295\u5f71", () -> {
            jQtPanel.setDropShadow(0, 6, 24, 50);
            JQtGallery.log("\u6295\u5f71\u5f00");
        }));
        jQtHBoxLayout11.addWidget(JQtGallery.makeBtn("\u53bb\u6295\u5f71", () -> {
            jQtPanel.clearDropShadow();
            JQtGallery.log("\u6295\u5f71\u5173");
        }));
        jQtHBoxLayout11.addWidget(JQtGallery.makeBtn("\u5706\u89d2 16", () -> {
            jQtPanel.setBorderRadius(16);
            JQtGallery.log("\u5706\u89d2 16");
        }));
        jQtVBoxLayout3.addLayout(jQtHBoxLayout11);
        animPanel.setLayout(jQtVBoxLayout3);
        winPanel = new JQtPanel();
        winPanel.setObjectName("card1");
        JQtVBoxLayout jQtVBoxLayout4 = new JQtVBoxLayout();
        jQtVBoxLayout4.setSpacing(8);
        jQtVBoxLayout4.addWidget(new JQtLabel("\u2460 \u7a97\u53e3\u7279\u6027\u5f00\u5173\uff08\u5b9e\u65f6\u5207\u6362\uff09"));
        JQtHBoxLayout jQtHBoxLayout12 = new JQtHBoxLayout();
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
        jQtHBoxLayout12.addWidget(new JQtLabel("\u6bdb\u73bb\u7483"));
        jQtHBoxLayout12.addWidget(jQtSwitch3);
        jQtHBoxLayout12.addWidget(new JQtLabel("\u5706\u89d2"));
        jQtHBoxLayout12.addWidget(jQtSwitch4);
        jQtHBoxLayout12.addWidget(new JQtLabel("\u8fb9\u6846"));
        jQtHBoxLayout12.addWidget(jQtSwitch5);
        jQtVBoxLayout4.addLayout(jQtHBoxLayout12);
        jQtVBoxLayout4.addWidget(new JQtLabel("\u2461 \u7a97\u53e3\u63a7\u5236"));
        JQtHBoxLayout jQtHBoxLayout13 = new JQtHBoxLayout();
        jQtHBoxLayout13.addWidget(JQtGallery.makeBtn("\u6700\u5c0f\u5316", () -> {
            w.minimize();
            JQtGallery.log("\u6700\u5c0f\u5316");
        }));
        jQtHBoxLayout13.addWidget(JQtGallery.makeBtn("\u6700\u5927\u5316", () -> {
            w.maximize();
            JQtGallery.log("\u6700\u5927\u5316");
        }));
        jQtHBoxLayout13.addWidget(JQtGallery.makeBtn("\u5207\u6362\u6700\u5927", () -> {
            w.toggleMaximize();
            JQtGallery.log("\u5207\u6362\u6700\u5927\u5316");
        }));
        jQtVBoxLayout4.addLayout(jQtHBoxLayout13);
        geoLabel = new JQtLabel("\u51e0\u4f55\u4fe1\u606f: -");
        geoLabel.setObjectName("cardMeta");
        jQtVBoxLayout4.addWidget(geoLabel);
        winPanel.setLayout(jQtVBoxLayout4);
        logLabel = new JQtLabel("\u5c31\u7eea");
        logLabel.setObjectName("logLine");
        JQtVBoxLayout jQtVBoxLayout5 = new JQtVBoxLayout();
        jQtVBoxLayout5.setContentsMargins(14, 10, 14, 10);
        jQtVBoxLayout5.setSpacing(8);
        jQtVBoxLayout5.addWidget(jQtLabel);
        jQtVBoxLayout5.addWidget(jQtLabel2);
        jQtVBoxLayout5.addWidget(jQtPivot);
        jQtVBoxLayout5.addWidget(themePanel);
        jQtVBoxLayout5.addWidget(ctrlPanel);
        jQtVBoxLayout5.addWidget(animPanel);
        jQtVBoxLayout5.addWidget(winPanel);
        jQtVBoxLayout5.addWidget(logLabel);
        jQtWindow.setLayout(jQtVBoxLayout5);
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
        jQtWindow.onClose(() -> app.quit());
        JQtGallery.applyTheme("nord");
        jQtWindow.show();
        jQtWindow.fadeIn(300L);
        JQtGallery.log("\u6f14\u793a\u5c31\u7eea\uff0c\u4e3b\u9898=Nord");
        if (bl2) {
            app.schedule(() -> JQtGallery.applyTheme("solarized"), 1600L);
            app.schedule(() -> JQtGallery.setAccent("#00BFA5"), 2600L);
            app.schedule(() -> jQtPivot.setCurrentIndex(1), 3600L);
            app.schedule(() -> jQtSwitch2.setChecked(false), 4600L);
            app.schedule(() -> {
                try {
                    JQtButton closeBtn = new JQtButton("x");
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