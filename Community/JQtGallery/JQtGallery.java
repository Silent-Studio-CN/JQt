import org.jqt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * JQtGallery —— JQt 全功能演示（v0.3 渲染式主题版）
 * 分区：主题换肤 / 控件 / 动画 / 窗口，每个功能块带中文注释。
 * 说明：主题模板优先读文件系统 themes/fluent.qss.tpl，找不到时回退 jar 内资源，
 *       因此 exe 打包版无需外部模板文件也能运行。
 */
public class JQtGallery {
    static QApplication app;
    static QMainWindow w;
    static QLabel logLabel, geoLabel;      // 底部事件日志行 / 窗口几何信息行
    static QFrame themePanel, ctrlPanel, animPanel, winPanel, v5Panel, v6Panel, v61Panel;  // 七个功能分区面板
    static Map<String, String> currentVars;  // 当前主题的变量表（22 个 %var%）
    static boolean currentLight = false;     // 当前是否为浅色主题
    static String themeName = "nord";        // 当前主题名
    static StringBuilder logBuf = new StringBuilder();
    static boolean qfLoaded = false;    // qf 皮肤已加载（进入热重载监听）
    static long qfMtime = -1;           // qf 文件上次修改时间
    static int qssReloads = 0;          // 热重载次数
    static QLabel qfStateRef;         // qf 状态行引用

    // ================= 日志（界面底部实时显示最后一行）=================
    static void log(String line) {
        System.out.println("[G] " + line);
        logBuf.append(line).append('\n');
        if (logBuf.length() > 400) logBuf.delete(0, logBuf.length() - 400);
        String[] ls = logBuf.toString().split("\\n");
        String last = ls.length > 0 ? ls[ls.length - 1] : "";
        if (last.length() > 80) last = last.substring(last.length() - 80);
        logLabel.setText(last);
    }

    // ================= 主题模板读取（文件优先，jar 资源兜底）=================
    // 打包成 exe 后 cwd 不可控，模板内嵌进 jar，程序自给自足
    static String readThemeTemplate() {
        try {
            String f = new String(Files.readAllBytes(Paths.get("themes/fluent.qss.tpl")), StandardCharsets.UTF_8);
            if (!f.isEmpty()) return f;
        } catch (Exception ignored) { }
        try (InputStream in = JQtGallery.class.getResourceAsStream("/themes/fluent.qss.tpl")) {
            if (in != null) return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ignored) { }
        return "";
    }

    // ================= 主题渲染核心 =================
    // 原理：官方 fluent.qss.tpl 里有 22 个 %var% 占位符（win-bg/fg/accent/btn-*...），
    //       把变量表填进去生成完整 QSS，再 setStyleSheet —— 不依赖 setTheme 的文件路径
    static void renderTheme(String name, Map<String, String> vars, boolean light) {
        themeName = name;
        currentVars = new HashMap<>(vars);
        currentLight = light;
        String tpl = readThemeTemplate();
        if (tpl.isEmpty()) {
            log("警告: 主题模板缺失，回退内置 fluent-dark");
            app.setTheme("fluent-dark");   // 仅当模板完全缺失时兜底
            return;
        }
        for (Map.Entry<String, String> e : currentVars.entrySet()) {
            tpl = tpl.replace("%" + e.getKey() + "%", e.getValue());
        }
        app.setStyleSheet(tpl);
        log("主题 -> " + name);
    }

    // 切换主题：官方两套 + ThemePack 三套原创
    static void applyTheme(String name) {
        if (name.equals("fluent-dark")) {
            renderTheme("fluent-dark(官方暗色)", QApplication.FLUENT_DARK, false);
        } else if (name.equals("fluent-light")) {
            renderTheme("fluent-light(官方浅色)", QApplication.FLUENT_LIGHT, true);
        } else if (name.equals("nord")) {
            renderTheme("Nord 北极蓝", NordTheme.vars(), false);
        } else if (name.equals("solarized")) {
            renderTheme("Solarized 护眼", SolarizedTheme.vars(), true);
        } else if (name.equals("terminal")) {
            renderTheme("Terminal 荧光绿", TerminalTheme.vars(), false);
        }
    }

    // 修改强调色：改变量表里的 accent 后重新渲染（模板重绘）
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

    // 快捷建按钮：文字 + 点击回调
    static QPushButton makeBtn(String text, Runnable act) {
        QPushButton b = new QPushButton(text);
        b.onClicked(act);
        return b;
    }

    // v0.6 分区按钮：同 makeBtn，但登记进自动演示点击列表（auto 模式逐一点击验证新 API）
    // v0.6 的 QPushButton 没有 text() 读取方法，用并行 Map 记住按钮文字
    static java.util.List<QPushButton> v6btns = new java.util.ArrayList<>();
    static java.util.Map<QPushButton, String> v6btnText = new java.util.IdentityHashMap<>();
    static java.util.List<QPushButton> v61btns = new java.util.ArrayList<>();   // v0.6.1 分区按钮（同 v6btn 注册，auto 也点击）
    static QPushButton v6btn(String text, Runnable act) {
        QPushButton b = makeBtn(text, act);
        v6btns.add(b);
        v6btnText.put(b, text);
        return b;
    }
    // v0.6.1 分区按钮：同 v6btn，登记进 v61btns（auto 模式先切分区再点击）
    static QPushButton v61btn(String text, Runnable act) {
        QPushButton b = makeBtn(text, act);
        v61btns.add(b);
        v6btnText.put(b, text);
        return b;
    }

    // ================= 主界面 =================
    public static void main(String[] args) {
        boolean auto = Long.getLong("g.auto", 0L) > 0;
        app = new QApplication();
        // 注意：不要用 app.setTheme("fluent-dark")（它读文件系统模板，exe 版会炸），
        // 一律走下面渲染式的 applyTheme()
        QMainWindow win = new QMainWindow("JQt Gallery 全功能演示", 1280, 720);   // 16:9
        w = win;
        win.setFrameless(true);      // 无边框窗口
        win.setRoundedCorners(true); // 圆角窗口
        win.setDraggable(true);      // 窗口可拖动（无边框时靠标题区拖动）
        win.setFixedSize(1280, 720); // 固定 16:9 尺寸，内容超高时内部滚动（v0.6.1 分区实测会撑大窗口）

        // ---- 标题区 ----
        QLabel title = new QLabel("JQt Gallery 全功能演示");
        QLabel sub = new QLabel("主题 / 控件 / 动画 / 窗口 · 每项都可点击体验");
        sub.setObjectName("cardMeta");

        // ---- 顶部选项卡导航（四个分区切换）----
        JQtPivot pivot = new JQtPivot();
        pivot.addItem("主题"); pivot.addItem("控件"); pivot.addItem("动画"); pivot.addItem("窗口"); pivot.addItem("v0.5 新控件"); pivot.addItem("v0.6 新功能"); pivot.addItem("v0.6.1 独家");

        // ================ 分区 1：主题换肤 ================
        themePanel = new QFrame();
        themePanel.setObjectName("card1");
        QVBoxLayout tBox = new QVBoxLayout();
        tBox.setSpacing(8);
        tBox.addWidget(new QLabel("① 主题切换（ThemePack 三套原创 + 官方两套）"));
        QHBoxLayout tRow = new QHBoxLayout();
        tRow.setSpacing(6);
        tRow.addWidget(makeBtn("Nord 北极蓝", () -> applyTheme("nord")));
        tRow.addWidget(makeBtn("Solarized 护眼", () -> applyTheme("solarized")));
        tRow.addWidget(makeBtn("Terminal 荧光绿", () -> applyTheme("terminal")));
        tBox.addLayout(tRow);
        QHBoxLayout tRow2 = new QHBoxLayout();
        tRow2.setSpacing(6);
        tRow2.addWidget(makeBtn("官方暗色", () -> applyTheme("fluent-dark")));
        tRow2.addWidget(makeBtn("官方浅色", () -> applyTheme("fluent-light")));
        tBox.addLayout(tRow2);
        tBox.addWidget(new QLabel("② 强调色（改 %accent% 变量重渲染模板）"));
        QHBoxLayout aRow = new QHBoxLayout();
        aRow.setSpacing(6);
        aRow.addWidget(makeBtn("蓝 #0078D4", () -> setAccent("#0078D4")));
        aRow.addWidget(makeBtn("青 #00BFA5", () -> setAccent("#00BFA5")));
        aRow.addWidget(makeBtn("紫 #9C27B0", () -> setAccent("#9C27B0")));
        aRow.addWidget(makeBtn("橙 #FF6D00", () -> setAccent("#FF6D00")));
        tBox.addLayout(aRow);
        tBox.addWidget(new QLabel("③ 强调色预览：勾选下面的按钮 / 复选框看 %accent% 效果"));
        QHBoxLayout pRow = new QHBoxLayout();
        pRow.setSpacing(6);
        QPushButton accentBtn = new QPushButton("勾选我（选中态=强调色）");
        accentBtn.setCheckable(true);
        accentBtn.onToggled(b -> log("强调色按钮 -> " + b + "（QPushButton:checked 用 %accent% 填充）"));
        QCheckBox accentCb = new QCheckBox("强调色复选框");
        accentCb.onToggled(b -> log("强调色复选框 -> " + b + "（indicator:checked 用 %accent% 填充）"));
        pRow.addWidget(accentBtn);
        pRow.addWidget(accentCb);
        tBox.addLayout(pRow);
        tBox.addWidget(new QLabel("④ 跟随系统深浅色 + 全局动画节奏"));
        QHBoxLayout sRow = new QHBoxLayout();
        sRow.setSpacing(6);
        JQtSwitch autoSw = new JQtSwitch(false);   // 自动跟随 Windows 明暗模式
        autoSw.onToggled(b -> { app.setAutoTheme(b); log("自动主题 -> " + b); });
        sRow.addWidget(new QLabel("自动主题"));
        sRow.addWidget(autoSw);
        QComboBox animCb = new QComboBox();    // 动画节奏预设
        animCb.addItem("动画节奏: 默认"); animCb.addItem("动画节奏: 快");
        animCb.addItem("动画节奏: 舒缓"); animCb.addItem("动画节奏: 关闭");
        animCb.onCurrentIndexChanged(i -> {
            JQtAnimationTheme th = i == 0 ? JQtAnimationTheme.DEFAULT : i == 1 ? JQtAnimationTheme.FAST
                    : i == 2 ? JQtAnimationTheme.RELAXED : JQtAnimationTheme.OFF;
            QApplication.setAnimationTheme(th);
            log("动画节奏 -> " + i);
        });
        sRow.addWidget(animCb);
        tBox.addLayout(sRow);
        tBox.addWidget(new QLabel("⑤ 滑条调色器（拖 RGB → 应用到强调色）"));
        QHBoxLayout cs1 = new QHBoxLayout();
        cs1.addWidget(new QLabel("R"));
        QSlider rS = new QSlider(0, 255, 0);
        cs1.addWidget(rS);
        tBox.addLayout(cs1);
        QHBoxLayout cs2 = new QHBoxLayout();
        cs2.addWidget(new QLabel("G"));
        QSlider gS = new QSlider(0, 255, 120);
        cs2.addWidget(gS);
        tBox.addLayout(cs2);
        QHBoxLayout cs3 = new QHBoxLayout();
        cs3.addWidget(new QLabel("B"));
        QSlider bS = new QSlider(0, 255, 212);
        cs3.addWidget(bS);
        tBox.addLayout(cs3);
        QHBoxLayout c4 = new QHBoxLayout();
        c4.setSpacing(6);
        QFrame cPreview = new QFrame();
        cPreview.setObjectName("card");
        cPreview.addWidget(new QLabel("预览"));
        QLabel hexLbl = new QLabel("#0078D4");
        hexLbl.setObjectName("cardMeta");
        QPushButton applyBtn = new QPushButton("应用为强调色");
        c4.addWidget(cPreview);
        c4.addWidget(hexLbl);
        c4.addWidget(applyBtn);
        tBox.addLayout(c4);
        // 滑块联动：预览面板实时变色 + hex 显示 + 应用到强调色
        Runnable upd = () -> {
            String h = String.format("#%02X%02X%02X", rS.value(), gS.value(), bS.value());
            cPreview.setStyleSheet("QFrame { background: " + h + "; border-radius: 8px; }");
            hexLbl.setText(h);
        };
        rS.onValueChanged(v -> upd.run());
        gS.onValueChanged(v -> upd.run());
        bS.onValueChanged(v -> upd.run());
        applyBtn.onClicked(() -> setAccent(String.format("#%02X%02X%02X", rS.value(), gS.value(), bS.value())));
        upd.run();
        tBox.addWidget(new QLabel("⑥ QSS 热重载（编辑 qf-dark-jqt.qss 存盘即生效）"));
        QHBoxLayout qfRow = new QHBoxLayout();
        qfRow.setSpacing(6);
        QPushButton loadQfBtn = new QPushButton("加载 qf 皮肤");
        QPushButton restoreBtn = new QPushButton("恢复模板主题");
        QLabel qfState = new QLabel("未加载");
        qfState.setObjectName("cardMeta");
        qfRow.addWidget(loadQfBtn);
        qfRow.addWidget(restoreBtn);
        qfRow.addWidget(qfState);
        tBox.addLayout(qfRow);
        loadQfBtn.onClicked(() -> {
            loadQfSkin();
            qfMtime = -1;
        });
        restoreBtn.onClicked(() -> {
            qfLoaded = false;
            applyTheme(themeName);
            qfState.setText("未加载");
        });
        qfLoaded = false;
        qfMtime = -1;
        qfStateRef = qfState;
        themePanel.setLayout(tBox);

        // ================ 分区 2：控件 ================
        ctrlPanel = new QFrame();
        ctrlPanel.setObjectName("card1");
        QVBoxLayout cBox = new QVBoxLayout();
        cBox.setSpacing(8);
        cBox.addWidget(new QLabel("① 开关 / 复选框（toggled 事件）"));
        QHBoxLayout c1 = new QHBoxLayout();
        JQtSwitch sw = new JQtSwitch(true);        // Fluent 滑动开关
        sw.onToggled(b -> log("开关 -> " + b));
        QCheckBox cb = new QCheckBox("勾选我"); // 复选框
        cb.onToggled(b -> log("复选框 -> " + b));
        c1.addWidget(sw); c1.addWidget(cb);
        cBox.addLayout(c1);
        cBox.addWidget(new QLabel("② 输入框（实时 textChanged / 回车确认）"));
        QLineEdit edit = new QLineEdit("");
        edit.setPlaceholderText("在这里输入文字，按回车...");
        edit.onTextChanged(s -> { if (!s.isEmpty()) log("输入: " + s); });
        edit.onReturnPressed(() -> log("回车确认: " + edit.text()));
        cBox.addWidget(edit);
        cBox.addWidget(new QLabel("③ 下拉框 + 列表（选择事件）"));
        QHBoxLayout c2 = new QHBoxLayout();
        QComboBox combo = new QComboBox();
        combo.addItem("选项 A"); combo.addItem("选项 B"); combo.addItem("选项 C");
        combo.onCurrentIndexChanged(i -> log("下拉框 -> " + i + " " + combo.currentText()));
        QListWidget list = new QListWidget();
        list.addItem("行 1"); list.addItem("行 2"); list.addItem("行 3");
        list.onItemClicked(i -> log("列表点击 -> " + i));
        c2.addWidget(combo); c2.addWidget(list);
        cBox.addLayout(c2);
        cBox.addWidget(new QLabel("④ 禁用 / 悬垂保护（内存管理演示）"));
        QHBoxLayout c3 = new QHBoxLayout();
        QPushButton victim = makeBtn("我可能被禁用", () -> log("按钮被点击了"));
        QPushButton disBtn = makeBtn("禁用 / 启用", () -> {
            victim.setEnabled(!victim.isEnabled());   // v0.3 新增的禁用 API
            log("按钮可用 -> " + victim.isEnabled());
        });
        QPushButton dangBtn = makeBtn("悬垂演示", () -> {
            // 悬垂保护：dispose 释放 native 句柄后再调用会抛 IllegalStateException
            try {
                QPushButton ghost = new QPushButton("ghost");
                ghost.dispose();                      // 释放原生资源
                ghost.setText("boom");                // 悬垂调用 -> 应抛异常
                log("悬垂保护未生效 (异常)");
            } catch (IllegalStateException e) {
                log("悬垂保护 OK: " + e.getMessage());
            }
        });
        c3.addWidget(victim); c3.addWidget(disBtn); c3.addWidget(dangBtn);
        cBox.addLayout(c3);
        ctrlPanel.setLayout(cBox);

        // ================ 分区 3：动画 ================
        animPanel = new QFrame();
        animPanel.setObjectName("card1");
        QVBoxLayout aBox = new QVBoxLayout();
        aBox.setSpacing(8);
        aBox.addWidget(new QLabel("① 窗口动画（淡入 / 淡出）"));
        QHBoxLayout m1 = new QHBoxLayout();
        m1.addWidget(makeBtn("窗口淡入 400ms", () -> { w.fadeIn(400); log("窗口淡入"); }));
        m1.addWidget(makeBtn("窗口淡出 400ms", () -> { w.fadeOut(400); log("窗口淡出"); }));
        aBox.addLayout(m1);
        aBox.addWidget(new QLabel("② 卡片缩放动画（5 种缓动可选）"));
        QHBoxLayout m2 = new QHBoxLayout();
        QFrame card = new QFrame();
        card.setObjectName("card");
        card.setBorderRadius(10);                     // v0.3 圆角 API
        card.addWidget(new QLabel("卡片"));
        QComboBox easeCb = new QComboBox();       // 缓动函数选择
        easeCb.addItem("回弹 OUT_BOUNCE"); easeCb.addItem("橡皮筋 OUT_ELASTIC");
        easeCb.addItem("过冲 OUT_BACK"); easeCb.addItem("缓入缓出 IN_OUT_CUBIC");
        easeCb.addItem("匀速 LINEAR");
        QPushButton scaleBtn = makeBtn("放大 300x120", () -> {
            JQtEasing e = easeCb.currentIndex() == 0 ? JQtEasing.OUT_BOUNCE
                    : easeCb.currentIndex() == 1 ? JQtEasing.OUT_ELASTIC
                    : easeCb.currentIndex() == 2 ? JQtEasing.OUT_BACK
                    : easeCb.currentIndex() == 3 ? JQtEasing.IN_OUT_CUBIC : JQtEasing.LINEAR;
            card.animateResize(300, 120, 500, e);    // 属性动画：尺寸插值
            log("卡片放大 300x120 " + easeCb.currentText());
        });
        QPushButton scaleBack = makeBtn("还原 180x60", () -> {
            card.animateResize(180, 60, 300, JQtEasing.OUT_QUAD);
            log("卡片还原 180x60");
        });
        m2.addWidget(scaleBtn); m2.addWidget(scaleBack);
        aBox.addLayout(m2);
        aBox.addWidget(card);
        aBox.addWidget(easeCb);
        aBox.addWidget(new QLabel("③ 阴影 / 圆角（v0.3 样式 API）"));
        QHBoxLayout m3 = new QHBoxLayout();
        m3.addWidget(makeBtn("加投影", () -> { card.setDropShadow(0, 6, 24, 50); log("投影开"); }));
        m3.addWidget(makeBtn("去投影", () -> { card.clearDropShadow(); log("投影关"); }));
        m3.addWidget(makeBtn("圆角 16", () -> { card.setBorderRadius(16); log("圆角 16"); }));
        aBox.addLayout(m3);
        animPanel.setLayout(aBox);

        // ================ 分区 4：窗口 ================
        winPanel = new QFrame();
        winPanel.setObjectName("card1");
        QVBoxLayout wBox = new QVBoxLayout();
        wBox.setSpacing(8);
        wBox.addWidget(new QLabel("① 窗口特性开关（实时切换）"));
        QHBoxLayout w1 = new QHBoxLayout();
        JQtSwitch acrylicSw = new JQtSwitch(true);    // 亚克力毛玻璃
        acrylicSw.onToggled(b -> { w.setAcrylic(b); log("毛玻璃 -> " + b); });
        JQtSwitch roundSw = new JQtSwitch(true);      // 圆角
        roundSw.onToggled(b -> { w.setRoundedCorners(b); log("圆角 -> " + b); });
        JQtSwitch frameSw = new JQtSwitch(false);     // 无边框
        frameSw.onToggled(b -> { w.setFrameless(b); log("无边框 -> " + b); });
        w1.addWidget(new QLabel("毛玻璃")); w1.addWidget(acrylicSw);
        w1.addWidget(new QLabel("圆角")); w1.addWidget(roundSw);
        w1.addWidget(new QLabel("边框")); w1.addWidget(frameSw);
        wBox.addLayout(w1);
        wBox.addWidget(new QLabel("② 窗口控制"));
        QHBoxLayout w2 = new QHBoxLayout();
        w2.addWidget(makeBtn("最小化", () -> { w.minimize(); log("最小化"); }));
        w2.addWidget(makeBtn("最大化", () -> { w.maximize(); log("最大化"); }));
        w2.addWidget(makeBtn("切换最大", () -> { w.toggleMaximize(); log("切换最大化"); }));
        wBox.addLayout(w2);
        geoLabel = new QLabel("几何信息: -");
        geoLabel.setObjectName("cardMeta");
        wBox.addWidget(geoLabel);
        winPanel.setLayout(wBox);

        // ================ 分区 5：v0.5 新控件（Q 类时代新增） ================
        v5Panel = new QFrame();
        v5Panel.setObjectName("card1");
        QVBoxLayout v5 = new QVBoxLayout();
        v5.setSpacing(8);
        v5.addWidget(new QLabel("① 表格 QTableWidget（3x3 + 表头 + 单元格点击）"));
        QTableWidget table = new QTableWidget(3, 3);
        table.setColumnHeaders(new String[]{"名称", "类型", "状态"});
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) table.setItemText(r, c, "单元格" + r + "," + c);
        }
        table.resizeColumnsToContents();
        table.onCellClicked((r, c) -> log("表格点击 -> " + r + "," + c + " = " + table.itemText(r, c)));
        v5.addWidget(table);

        v5.addWidget(new QLabel("② 树 QTreeWidget（顶层 + 子节点 + 展开）"));
        QTreeWidget tree = new QTreeWidget();
        int root1 = tree.addTopLevelItem("项目根");
        tree.addChild(root1, "子节点 1");
        tree.addChild(root1, "子节点 2");
        tree.expandAll();
        tree.onItemClicked(i -> log("树点击 -> " + tree.itemText(i)));
        v5.addWidget(tree);

        v5.addWidget(new QLabel("③ 选项卡 QTabWidget（两页切换）"));
        QTabWidget tabs = new QTabWidget();
        QFrame page1 = new QFrame();
        page1.addWidget(new QLabel("页面 1 内容"));
        QFrame page2 = new QFrame();
        page2.addWidget(new QLabel("页面 2 内容"));
        tabs.addTab(page1, "页 1");
        tabs.addTab(page2, "页 2");
        tabs.onCurrentChanged(i -> log("选项卡 -> " + i));
        v5.addWidget(tabs);

        v5.addWidget(new QLabel("④ 数值联动 QSpinBox ↔ QDial + 单选按钮"));
        QHBoxLayout v5row = new QHBoxLayout();
        v5row.setSpacing(6);
        QSpinBox spin = new QSpinBox();
        spin.setRange(0, 100);
        spin.setValue(30);
        QDial dial = new QDial();
        dial.setRange(0, 100);
        dial.setValue(30);
        spin.onValueChanged(v -> dial.setValue(v));
        dial.onValueChanged(v -> spin.setValue(v));
        QRadioButton ra = new QRadioButton("A");
        QRadioButton rb = new QRadioButton("B");
        QRadioButton rc = new QRadioButton("C");
        ra.setChecked(true);
        QRadioButton[] radios = {ra, rb, rc};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            radios[i].onToggled(b -> {
                if (b) {
                    log("单选 -> " + idx);
                    for (int j = 0; j < 3; j++) if (j != idx) radios[j].setChecked(false);
                }
            });
        }
        v5row.addWidget(spin);
        v5row.addWidget(dial);
        v5row.addWidget(ra);
        v5row.addWidget(rb);
        v5row.addWidget(rc);
        v5.addLayout(v5row);

        v5.addWidget(new QLabel("⑤ 富文本 QTextEdit + ⑥ 自绘画布 QCanvasWidget"));
        QHBoxLayout v5row2 = new QHBoxLayout();
        v5row2.setSpacing(6);
        QTextEdit ted = new QTextEdit();
        ted.setPlainText("富文本编辑区，点「追加」添加日志");
        QCanvasWidget canvas = new QCanvasWidget();
        canvas.onPaint(p -> {
            p.setColor(0xFF8800CC);
            p.fillCircle(50, 50, 25);
            p.setColor(0xFF00AAFF);
            p.drawCircle(110, 50, 30);
            p.setColor(0xFFFF6600);
            p.drawLine(10, 110, 190, 110);
            p.setColor(0xFFFFFFFF);
            p.drawText(10, 130, "JQt Canvas");
        });
        v5row2.addWidget(ted);
        v5row2.addWidget(canvas);
        v5.addLayout(v5row2);

        v5.addWidget(new QLabel("⑦ 工具栏 / 菜单 / 托盘 / 后台线程 / 状态栏"));
        QToolBar toolbar = new QToolBar();
        toolbar.addButton("工具 1");
        toolbar.addButton("工具 2");
        toolbar.onTriggered(i -> log("工具栏 -> " + i));
        v5.addWidget(toolbar);
        final QPushButton[] menuBtnRef = new QPushButton[1];
        menuBtnRef[0] = makeBtn("弹出菜单", () -> {
            QMenu menu = new QMenu();
            menu.addItem("菜单项 A");
            menu.addItem("菜单项 B");
            menu.onTriggered(i -> log("菜单 -> " + i));
            menu.popup(menuBtnRef[0]);
        });
        QPushButton menuBtn = menuBtnRef[0];
        QPushButton trayBtn = makeBtn("托盘提示", () -> {
            QSystemTrayIcon tray = new QSystemTrayIcon();
            tray.setToolTip("JQt Gallery");
            tray.show();
            tray.showMessage("JQt", "托盘消息（3 秒）", 3000);
        });
        QPushButton uiBtn = makeBtn("后台线程更新 UI", () -> {
            new Thread(() -> {
                try { Thread.sleep(1000); } catch (Exception ignored) { }
                QApplication.runOnUiThread(() -> log("runOnUiThread: 后台线程安全更新 UI ✓"));
            }).start();
        });
        QPushButton repaintBtn = makeBtn("画布重绘", () -> canvas.repaint());
        QPushButton appendBtn = makeBtn("富文本追加", () -> ted.append("追加 " + System.currentTimeMillis() % 10000));
        QHBoxLayout v5row3 = new QHBoxLayout();
        v5row3.setSpacing(6);
        v5row3.addWidget(menuBtn);
        v5row3.addWidget(trayBtn);
        v5row3.addWidget(uiBtn);
        v5row3.addWidget(repaintBtn);
        v5row3.addWidget(appendBtn);
        v5.addLayout(v5row3);

        QStatusBar sbar = new QStatusBar();
        sbar.showMessage("状态栏消息（5 秒）", 5000);
        v5.addWidget(sbar);
        v5.addWidget(new QLabel("⑧ 渲染后端: " + QApplication.rhiBackend()));
        v5Panel.setLayout(v5);
        // ================ 分区 6：v0.6 L1 补全（全 API + 直观反馈） ================
        v6Panel = new QFrame();
        v6Panel.setObjectName("card1");
        QVBoxLayout v6 = new QVBoxLayout();
        v6.setSpacing(8);

        // 大字反馈标签：所有操作结果直接显示在这里（不只是日志行）
        QLabel fb = new QLabel("v0.6 新功能演示 —— 操作结果在这里实时显示");
        fb.setObjectName("fbLabel");
        fb.setStyleSheet("QLabel#fbLabel { font-size: 15px; font-weight: bold; color: #4cc2ff; background: rgba(76,194,255,0.10); border: 1px solid rgba(76,194,255,0.45); border-radius: 8px; padding: 8px 12px; }");
        v6.addWidget(fb);

        v6.addWidget(new QLabel("① 剪贴板 QClipboard（复制/粘贴，反馈直显）"));
        QHBoxLayout v6r1 = new QHBoxLayout();
        v6r1.setSpacing(6);
        QLineEdit cbEdit = new QLineEdit("剪贴板测试文本");
        QPushButton cbCopy = v6btn("复制", () -> { QClipboard.setText(cbEdit.text()); fb.setText("已复制到剪贴板: " + cbEdit.text()); log("剪贴板复制: " + cbEdit.text()); });
        QPushButton cbPaste = v6btn("粘贴", () -> { cbEdit.setText(QClipboard.text()); fb.setText("已从剪贴板粘贴: " + QClipboard.text()); log("剪贴板粘贴: " + QClipboard.text()); });
        v6r1.addWidget(cbEdit);
        v6r1.addWidget(cbCopy);
        v6r1.addWidget(cbPaste);
        v6.addLayout(v6r1);

        v6.addWidget(new QLabel("② 文件系统 QFile/QDir（exists/count/size）"));
        QHBoxLayout v6r2 = new QHBoxLayout();
        v6r2.setSpacing(6);
        QPushButton dirBtn = v6btn("统计当前目录", () -> { int n = QDir.count("."); fb.setText("当前目录共 " + n + " 个条目（QDir.count）"); log("QDir.count = " + n); });
        QPushButton fileBtn = v6btn("检查 qf 皮肤", () -> { boolean e1 = QFile.exists("qf-dark-jqt.qss"); fb.setText("qf-dark-jqt.qss 存在: " + e1); log("QFile.exists = " + e1); });
        QPushButton sizeBtn = v6btn("QFile.size", () -> { long sz = QFile.size("qf-dark-jqt.qss"); fb.setText("qf 皮肤大小: " + sz + " 字节"); log("QFile.size = " + sz); });
        v6r2.addWidget(dirBtn);
        v6r2.addWidget(fileBtn);
        v6r2.addWidget(sizeBtn);
        v6.addLayout(v6r2);

        v6.addWidget(new QLabel("③ 配置 QSettings（保存/读取，反馈直显）"));
        QHBoxLayout v6r3 = new QHBoxLayout();
        v6r3.setSpacing(6);
        QSettings settings = new QSettings();
        QPushButton setBtn = v6btn("计数器 +1", () -> { int n = settings.value("counter") + 1; settings.setValue("counter", n); fb.setText("QSettings 计数器 = " + n); log("counter=" + n); });
        QPushButton getBtn = v6btn("读取计数", () -> { int n = settings.value("counter"); fb.setText("QSettings 当前值 = " + n); log("read=" + n); });
        v6r3.addWidget(setBtn);
        v6r3.addWidget(getBtn);
        v6.addLayout(v6r3);

        v6.addWidget(new QLabel("④ QWidget 几何 API（move + pos()/size() 反馈直显）"));
        QHBoxLayout v6r4 = new QHBoxLayout();
        v6r4.setSpacing(6);
        // 说明：布局里的控件会被布局管理器重新摆放，move() 演示直接作用于主窗口（无边框，效果直观）
        final int[] winOrig = w.pos();
        v6r4.addWidget(v6btn("move(30,30)", () -> { w.move(30, 30); fb.setText("窗口已移动，pos=" + java.util.Arrays.toString(w.pos())); log("pos=" + java.util.Arrays.toString(w.pos())); }));
        v6r4.addWidget(v6btn("读 pos/size", () -> { fb.setText("窗口 pos=" + java.util.Arrays.toString(w.pos()) + " size=" + java.util.Arrays.toString(w.size())); log("pos=" + java.util.Arrays.toString(w.pos())); }));
        v6r4.addWidget(v6btn("还原位置", () -> { w.move(winOrig[0], winOrig[1]); fb.setText("窗口已还原 pos=" + java.util.Arrays.toString(w.pos())); }));
        v6.addLayout(v6r4);

        v6.addWidget(new QLabel("⑤ 右键菜单 onCustomContextMenuRequested（区域点右键）"));
        QFrame ctxFrame = new QFrame();
        ctxFrame.setObjectName("card");
        ctxFrame.addWidget(new QLabel("在这个区域点右键"));
        ctxFrame.onCustomContextMenuRequested((x, y) -> {
            QMenu m = new QMenu();
            m.addItem("右键菜单 A");
            m.addItem("右键菜单 B");
            m.onTriggered(i -> { fb.setText("右键菜单选择了: " + i); log("右键菜单 -> " + i); });
            m.popup(ctxFrame);
        });
        v6.addWidget(ctxFrame);

        v6.addWidget(new QLabel("⑥ QLineEdit 编辑操作（copy/cut/paste/undo/redo/selectAll）"));
        QHBoxLayout v6r5 = new QHBoxLayout();
        v6r5.setSpacing(4);
        QLineEdit ed = new QLineEdit("可编辑文本");
        QPushButton bCopy = v6btn("copy", () -> { ed.copy(); fb.setText("已复制选中文本"); });
        QPushButton bCut = v6btn("cut", () -> { ed.cut(); fb.setText("已剪切"); });
        QPushButton bPaste = v6btn("paste", () -> { ed.paste(); fb.setText("已粘贴: " + ed.text()); });
        QPushButton bUndo = v6btn("undo", () -> { ed.undo(); fb.setText("已撤销"); });
        QPushButton bRedo = v6btn("redo", () -> { ed.redo(); fb.setText("已重做"); });
        QPushButton bSel = v6btn("selectAll", () -> { ed.selectAll(); fb.setText("已全选"); });
        v6r5.addWidget(ed);
        v6r5.addWidget(bCopy);
        v6r5.addWidget(bCut);
        v6r5.addWidget(bPaste);
        v6r5.addWidget(bUndo);
        v6r5.addWidget(bRedo);
        v6r5.addWidget(bSel);
        v6.addLayout(v6r5);

        v6.addWidget(new QLabel("⑦ 三态复选框 + toggle + QLabel 换行/对齐（v0.6 增强）"));
        QHBoxLayout v6r6 = new QHBoxLayout();
        v6r6.setSpacing(6);
        QCheckBox tri = new QCheckBox("三态复选框");
        tri.setTristate(true);
        tri.onCheckStateChanged(s -> { fb.setText("三态状态: " + s); log("三态 -> " + s); });
        QPushButton toggler = new QPushButton("toggle 我");
        toggler.onClicked(() -> { toggler.toggle(); fb.setText("toggle -> checked=" + toggler.isChecked()); });
        QLabel wrapLbl = new QLabel("长文本换行演示：这一行文字很长，用来展示 wordWrap 效果，点击按钮切换开关看变化");
        QPushButton wrapBtn = v6btn("切换换行", () -> { wrapLbl.setWordWrap(!wrapLbl.wordWrap()); fb.setText("wordWrap = " + wrapLbl.wordWrap()); });
        QPushButton alignBtn = v6btn("居中/左对齐", () -> { wrapLbl.setAlignment(wrapLbl.alignment() == 4 ? 1 : 4); fb.setText("alignment = " + wrapLbl.alignment()); });
        v6r6.addWidget(tri);
        v6r6.addWidget(toggler);
        v6r6.addWidget(wrapBtn);
        v6r6.addWidget(alignBtn);
        v6.addLayout(v6r6);
        v6.addWidget(wrapLbl);

        v6.addWidget(new QLabel("⑧ 输入/数值增强：QComboBox 可编辑 + QSpinBox 前后缀 + QProgressBar 文本"));
        QHBoxLayout v6r7 = new QHBoxLayout();
        v6r7.setSpacing(6);
        QComboBox edCombo = new QComboBox();
        edCombo.addItem("可编辑项 1");
        edCombo.addItem("可编辑项 2");
        edCombo.setEditable(true);
        edCombo.setPlaceholderText("选择或输入...");
        edCombo.onActivated(i -> { fb.setText("combo activated: " + i + " [" + edCombo.currentText() + "]"); log("combo -> " + edCombo.currentText()); });
        QSpinBox preSpin = new QSpinBox();
        preSpin.setRange(0, 100);
        preSpin.setValue(42);
        preSpin.setPrefix("¥");
        preSpin.setSuffix(" 元");
        preSpin.setSingleStep(5);
        preSpin.onValueChanged(v -> fb.setText("金额: " + preSpin.cleanText()));
        QProgressBar pbar = new QProgressBar();
        pbar.setRange(0, 100);
        pbar.setValue(40);
        QPushButton pInc = v6btn("进度 +10", () -> { pbar.setValue(pbar.value() + 10); fb.setText("进度 " + pbar.value() + "% 文本: " + pbar.text()); });
        QPushButton pAlign = v6btn("进度文本居中", () -> { pbar.setAlignment(4); fb.setText("进度对齐已居中"); });
        v6r7.addWidget(edCombo);
        v6r7.addWidget(preSpin);
        v6r7.addWidget(pbar);
        v6r7.addWidget(pInc);
        v6r7.addWidget(pAlign);
        v6.addLayout(v6r7);

        v6.addWidget(new QLabel("⑨ 系统反馈 + 几何类 + toolTip"));
        QHBoxLayout v6r8 = new QHBoxLayout();
        v6r8.setSpacing(6);
        v6r8.addWidget(v6btn("beep", () -> { QApplication.beep(); fb.setText("beep 已响"); }));
        v6r8.addWidget(v6btn("alert 闪烁", () -> { QApplication.alert(w, 2000); fb.setText("窗口闪烁 2 秒"); }));
        v6r8.addWidget(makeBtn("showAbout", () -> QMessageBox.showAbout(w, "JQt", "JQt v0.6.0 - L1 API 补全演示")));
        QRect rectInfo = new QRect(10, 20, 100, 200);
        v6r8.addWidget(v6btn("QRect 信息", () -> { fb.setText("QRect: x=" + rectInfo.x + " y=" + rectInfo.y + " " + rectInfo.width + "x" + rectInfo.height); log(rectInfo.toString()); }));
        QPushButton tipBtn = v6btn("悬停看 toolTip", () -> {});
        tipBtn.setToolTip("这是 QWidget.setToolTip 的效果（v0.6 新增）");
        v6r8.addWidget(tipBtn);
        v6.addLayout(v6r8);

        v6Panel.setLayout(v6);

        // ================ 分区 7：v0.6.1 Exclusive Kit（Windows 独家能力） ================
        v61Panel = new QFrame();
        v61Panel.setObjectName("card1");
        QVBoxLayout v61 = new QVBoxLayout();
        v61.setSpacing(8);

        // 大字反馈标签（橙色系，区分 v0.6 分区的蓝色）
        QLabel fb61 = new QLabel("v0.6.1 Exclusive Kit —— 操作结果在这里实时显示");
        fb61.setObjectName("fbLabel");
        fb61.setStyleSheet("QLabel#fbLabel { font-size: 15px; font-weight: bold; color: #ffb74d; background: rgba(255,183,77,0.10); border: 1px solid rgba(255,183,77,0.45); border-radius: 8px; padding: 8px 12px; }");
        v61.addWidget(fb61);

        v61.addWidget(new QLabel("① Mica 背景材质（Win11 22H2+，开启后窗口背景变 Mica 质感）"));
        QHBoxLayout v61r1 = new QHBoxLayout();
        v61r1.setSpacing(6);
        v61r1.addWidget(v61btn("Mica 开", () -> { w.setMicaBackground(true); fb61.setText("Mica 背景已开启"); log("mica=true"); }));
        v61r1.addWidget(v61btn("Mica 关", () -> { w.setMicaBackground(false); fb61.setText("Mica 背景已关闭"); log("mica=false"); }));
        v61.addLayout(v61r1);

        v61.addWidget(new QLabel("② DWM 原生边框/标题栏/文字颜色（Win11 22H2+；0xAARRGGBB）"));
        QHBoxLayout v61r2 = new QHBoxLayout();
        v61r2.setSpacing(6);
        v61r2.addWidget(v61btn("边框 青色", () -> { w.setNativeBorderColor(0xFF00BFA5); fb61.setText("原生边框色 -> #00BFA5"); log("border=#00BFA5"); }));
        v61r2.addWidget(v61btn("边框 品红", () -> { w.setNativeBorderColor(0xFFFF2D92); fb61.setText("原生边框色 -> #FF2D92"); log("border=#FF2D92"); }));
        v61r2.addWidget(v61btn("标题栏 深蓝", () -> { w.setNativeCaptionColor(0xFF1E2A44); fb61.setText("原生标题栏色 -> #1E2A44"); log("caption=#1E2A44"); }));
        v61r2.addWidget(v61btn("文字 白色", () -> { w.setNativeCaptionTextColor(0xFFFFFFFF); fb61.setText("标题栏文字色 -> 白"); log("captionText=white"); }));
        v61r2.addWidget(v61btn("深色标题栏", () -> { w.setNativeDarkTitleBar(true); fb61.setText("深色标题栏 开（Win10 1809+）"); log("darkTitleBar=true"); }));
        v61r2.addWidget(v61btn("恢复浅色", () -> { w.setNativeDarkTitleBar(false); fb61.setText("深色标题栏 关"); log("darkTitleBar=false"); }));
        v61.addLayout(v61r2);

        v61.addWidget(new QLabel("③ 任务栏图标进度（Win10+；ITaskbarList3）"));
        QHBoxLayout v61r3 = new QHBoxLayout();
        v61r3.setSpacing(6);
        v61r3.addWidget(v61btn("进度 30%", () -> { w.setTaskbarProgress(30, 100); fb61.setText("任务栏进度 30/100"); log("taskbar=30/100"); }));
        v61r3.addWidget(v61btn("进度 70%", () -> { w.setTaskbarProgress(70, 100); fb61.setText("任务栏进度 70/100"); log("taskbar=70/100"); }));
        v61r3.addWidget(v61btn("进度 100%", () -> { w.setTaskbarProgress(100, 100); fb61.setText("任务栏进度 100/100"); log("taskbar=100/100"); }));
        v61r3.addWidget(v61btn("清除进度", () -> { w.clearTaskbarProgress(); fb61.setText("任务栏进度已清除"); log("taskbar cleared"); }));
        v61.addLayout(v61r3);

        v61.addWidget(new QLabel("④ 全局热键 GlobalHotkey（应用失焦也生效；Ctrl+Alt+G）"));
        QHBoxLayout v61r4 = new QHBoxLayout();
        v61r4.setSpacing(6);
        final int[] hkCount = {0};
        final GlobalHotkey[] hkRef = new GlobalHotkey[1];
        v61r4.addWidget(v61btn("注册热键", () -> {
            GlobalHotkey hk = new GlobalHotkey();
            boolean ok = hk.register("Ctrl+Alt+G", () -> {
                hkCount[0]++;
                fb61.setText("全局热键 Ctrl+Alt+G 触发 #" + hkCount[0] + "（失焦也生效）");
                log("hotkey #" + hkCount[0]);
            });
            hkRef[0] = ok ? hk : null;
            fb61.setText(ok ? "热键注册成功，按 Ctrl+Alt+G 试试" : "热键注册失败（可能被占用）");
            log("hotkey register = " + ok);
        }));
        v61r4.addWidget(v61btn("注销热键", () -> {
            if (hkRef[0] != null) { hkRef[0].unregister(); hkRef[0] = null; fb61.setText("热键已注销"); log("hotkey unregistered"); }
            else { fb61.setText("尚未注册热键"); }
        }));
        v61.addLayout(v61r4);

        v61.addWidget(new QLabel("⑤ 边框热更新：先开原生边框（DWM 边框色才可见），宽度即时生效，改完可直接看颜色"));
        QHBoxLayout v61r5b = new QHBoxLayout();
        v61r5b.setSpacing(6);
        // 注：窗口已显示后 setFrameless 底层 setWindowFlag 不会立即重建窗口，
        //     第一次切换不生效（要先关再开才显示）。这里强制 hide/show 触发重建。
        v61r5b.addWidget(v61btn("原生边框 开", () -> {
            w.setFrameless(false);
            w.setBorderWidth(2);
            w.hide(); w.show();   // 强制重建窗口，原生边框立即生效
            fb61.setText("已切原生边框（DWM 边框可见，宽 2）");
            log("frameless=false border=2");
        }));
        v61r5b.addWidget(v61btn("无边框 回", () -> {
            w.setFrameless(true);
            w.setBorderWidth(0);
            w.hide(); w.show();   // 强制重建，阴影恢复
            fb61.setText("已切回无边框");
            log("frameless=true border=0");
        }));
        v61r5b.addWidget(v61btn("边框宽 1", () -> { w.setBorderWidth(1); fb61.setText("边框宽 1"); log("borderWidth=1"); }));
        v61r5b.addWidget(v61btn("边框宽 4", () -> { w.setBorderWidth(4); fb61.setText("边框宽 4"); log("borderWidth=4"); }));
        v61r5b.addWidget(v61btn("边框宽 8", () -> { w.setBorderWidth(8); fb61.setText("边框宽 8"); log("borderWidth=8"); }));
        v61r5b.addWidget(v61btn("圆角 开", () -> { w.setRoundedCorners(true); fb61.setText("圆角开"); log("rounded=true"); }));
        v61r5b.addWidget(v61btn("圆角 关", () -> { w.setRoundedCorners(false); fb61.setText("圆角关"); log("rounded=false"); }));
        v61.addLayout(v61r5b);

        v61.addWidget(new QLabel("⑥ 开机自启 setAutoStart（HKCU Run 注册表；Windows 需传 exe 路径）"));
        QHBoxLayout v61r5 = new QHBoxLayout();
        v61r5.setSpacing(6);
        // 自启会写注册表，不进 auto 点击列表，避免自动演示污染系统
        v61r5.addWidget(makeBtn("开启自启", () -> {
            boolean ok = QApplication.setAutoStart(true, "");
            fb61.setText("开机自启开启 " + (ok ? "成功" : "失败"));
            log("autostart on = " + ok);
        }));
        v61r5.addWidget(makeBtn("关闭自启", () -> {
            boolean ok = QApplication.setAutoStart(false, "");
            fb61.setText("开机自启关闭 " + (ok ? "成功" : "失败"));
            log("autostart off = " + ok);
        }));
        v61.addLayout(v61r5);

        v61Panel.setLayout(v61);

        // ---- 根布局：标题固定，分区面板放滚动区（窗口固定 1280x720，内容超高可滚动）----
        logLabel = new QLabel("就绪");
        logLabel.setObjectName("logLine");
        QVBoxLayout root = new QVBoxLayout();
        root.setContentsMargins(14, 10, 14, 10);
        root.setSpacing(8);
        root.addWidget(title);
        root.addWidget(sub);
        root.addWidget(pivot);
        // 面板容器（放进滚动区，避免切换分区时窗口被内容撑大）
        QFrame panelHost = new QFrame();
        panelHost.setObjectName("card1");
        QVBoxLayout panelLay = new QVBoxLayout();
        panelLay.setContentsMargins(0, 0, 0, 0);
        panelLay.setSpacing(8);
        panelLay.addWidget(themePanel);
        panelLay.addWidget(ctrlPanel);
        panelLay.addWidget(animPanel);
        panelLay.addWidget(winPanel);
        panelLay.addWidget(v5Panel);
        panelLay.addWidget(v6Panel);
        panelLay.addWidget(v61Panel);
        panelHost.setLayout(panelLay);
        QScrollArea scroll = new QScrollArea();
        scroll.setWidgetResizable(true);
        scroll.setWidget(panelHost);
        root.addWidget(scroll);
        root.addWidget(logLabel);
        win.setLayout(root);

        // 分区切换：选项卡变化时只显示对应面板（其余隐藏）
        ctrlPanel.hide(); animPanel.hide(); winPanel.hide(); v5Panel.hide(); v6Panel.hide(); v61Panel.hide();
        pivot.onChanged(i -> {
            if (i == 0) { themePanel.show(); } else { themePanel.hide(); }
            if (i == 1) { ctrlPanel.show(); } else { ctrlPanel.hide(); }
            if (i == 2) { animPanel.show(); } else { animPanel.hide(); }
            if (i == 3) { winPanel.show(); } else { winPanel.hide(); }
            if (i == 4) { v5Panel.show(); } else { v5Panel.hide(); }
            if (i == 5) { v6Panel.show(); } else { v6Panel.hide(); }
            if (i == 6) { v61Panel.show(); } else { v61Panel.hide(); }
            log("分区 -> " + i);
        });

        // 窗口几何信息每秒刷新（v0.3 几何查询 API）
        scheduleGeo(1000);

        win.onClose(() -> app.quit());
        applyTheme("nord");     // 初始主题：Nord 北极蓝
        win.show();
        win.fadeIn(300);
        log("演示就绪，主题=Nord");

        // ---- 自动化演示模式（-Dg.auto=1）：顺序触发各功能验证 ----
        if (auto) {
            app.schedule(() -> applyTheme("solarized"), 1600);
            app.schedule(() -> setAccent("#00BFA5"), 2600);
            app.schedule(() -> pivot.setCurrentIndex(1), 3600);
            app.schedule(() -> sw.setChecked(false), 4600);
            app.schedule(() -> {
                try {
                    QPushButton g = new QPushButton("x"); g.dispose(); g.setText("y");
                    log("自动悬垂未生效 (异常)");
                } catch (IllegalStateException e) { log("自动悬垂保护 OK"); }
            }, 5600);
            // 切到 v0.6 分区，逐个点击新 API 按钮（反馈大字标签实时显示）
            log("v6 自动按钮数 = " + v6btns.size());
            app.schedule(() -> { log("v6 分区切换触发"); pivot.setCurrentIndex(5); }, 6400);
            int[] ai = {0};
            for (QPushButton b : v6btns) {
                final QPushButton bb = b;
                app.schedule(() -> {
                    try { bb.click(); log("自动点击: " + v6btnText.get(bb)); }
                    catch (Exception ex) { log("点击异常: " + v6btnText.get(bb) + " -> " + ex); }
                }, 6800 + 400L * ai[0]++);
            }
            // v0.6.1 分区：切过去后逐个点击（Mica/DWM/任务栏进度/热键）
            long v61start = 6800L + 400L * v6btns.size() + 600;
            app.schedule(() -> { log("v61 分区切换触发"); pivot.setCurrentIndex(6); }, v61start);
            int[] aj = {0};
            for (QPushButton b : v61btns) {
                final QPushButton bb = b;
                app.schedule(() -> {
                    try { bb.click(); log("自动点击: " + v6btnText.get(bb)); }
                    catch (Exception ex) { log("点击异常: " + v6btnText.get(bb) + " -> " + ex); }
                }, v61start + 400 + 400L * aj[0]++);
            }
            // 复现：v61 分区点完后切回 v0.6 分区（用户手动切换路径）
            app.schedule(() -> { log("切回 v0.6 分区"); pivot.setCurrentIndex(5); }, v61start + 400 + 400L * v61btns.size() + 200);
            app.schedule(() -> { log("自动演示完成"); app.quit(); },
                    v61start + 400 + 400L * v61btns.size() + 1200);
        }
        app.exec();
    }

    // ================= QSS 热重载 =================
    // 读取 qf 皮肤（外部文件，可热重载；不打包进 jar 以规避 GPL 分发问题）
    static String readQf() {
        try {
            String f = new String(Files.readAllBytes(Paths.get("qf-dark-jqt.qss")), StandardCharsets.UTF_8);
            if (!f.isEmpty()) return f;
        } catch (Exception ignored) { }
        return "";
    }

    static void loadQfSkin() {
        String qss = readQf();
        if (qss.isEmpty()) {
            log("qf 皮肤缺失：请把 qf-dark-jqt.qss 放到程序目录");
            return;
        }
        app.setStyleSheet(qss);   // 整体替换样式表 = qf 皮肤
        qfLoaded = true;
        qfStateRef.setText("监听中 · 重载 " + qssReloads + " 次");
        log("qf 皮肤加载 (" + qss.length() + " 字符)");
    }

    // 每秒检查 qf 文件修改时间，变化即热重载
    static void checkQssHotReload() {
        if (!qfLoaded) return;
        try {
            long m = Files.getLastModifiedTime(Paths.get("qf-dark-jqt.qss")).toMillis();
            if (qfMtime < 0) {
                qfMtime = m;
            } else if (m != qfMtime) {
                qfMtime = m;
                String qss = readQf();
                if (!qss.isEmpty()) {
                    app.setStyleSheet(qss);
                    qssReloads++;
                    qfStateRef.setText("监听中 · 重载 " + qssReloads + " 次");
                    log("QSS 热重载 #" + qssReloads + " (" + qss.length() + " 字符)");
                }
            }
        } catch (Exception ignored) { }
    }

    // 每秒刷新窗口几何信息（位置/尺寸/可见性）+ QSS 热重载检查
    static void scheduleGeo(long delay) {
        app.schedule(() -> {
            geoLabel.setText("几何信息: x=" + w.x() + " y=" + w.y() + " 宽=" + w.width() + " 高=" + w.height()
                    + " 可见=" + w.isVisible());
            checkQssHotReload();
            scheduleGeo(1000);
        }, delay);
    }
}
