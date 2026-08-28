# JQtGallery - JQt full-featured demo

Full-featured JQt demo: themes / widgets / motion / window / v0.5+ widgets.

**Versioning convention (since v0.6.0):**
- Root directory = latest version (currently v0.6.1 compatible)
- Old versions archived in subdirectories: v5.0/

| Version | Compatible JQt | Notes |
|---------|---------------|-------|
| root    | v0.6.1+        | Latest: v0.6 L1 API + v0.6.1 Exclusive Kit (Mica/DWM/taskbar/GlobalHotkey) |
| v5.0/   | v0.5.x         | Five sections: theme/widgets/motion/window/v0.5 widgets, 16:9 window |

## Sections (pivot navigation)

1. Theme: 5 themes (Nord/Solarized/Terminal/official dark/light) + accent colors
   + slider color picker + QSS hot-reload (qf-dark-jqt.qss watch)
2. Widgets: switch/checkbox/lineedit/combo/list/disable/dangling guard
3. Motion: window fade / card scale (5 easings) / drop shadow / radius
4. Window: acrylic/rounded/frameless/minimize/maximize/geometry
5. v0.5 widgets: table/tree/tab/spinbox/dial/radio/textedit/canvas/menu/toolbar/
   tray/runOnUiThread/statusbar/rhi backend
6. v0.6 L1 API (big feedback label at top, all results shown live):
   ① QClipboard copy/paste  ② QDir.count/QFile.exists/QFile.size
   ③ QSettings counter      ④ QWidget move/pos/size on the window itself
   ⑤ right-click menu (onCustomContextMenuRequested)
   ⑥ QLineEdit copy/cut/paste/undo/redo/selectAll
   ⑦ tristate QCheckBox + toggle + QLabel wordWrap/alignment
   ⑧ editable QComboBox + QSpinBox prefix/suffix + QProgressBar text/alignment
   ⑨ QApplication.beep/alert/showAbout + QRect + toolTip
   (auto demo -Dg.auto=1 clicks all 24 buttons in sequence)
7. v0.6.1 Exclusive Kit (Windows): Mica on/off, DWM border/caption/text colors,
   dark title bar, taskbar progress (30/70/100/clear), GlobalHotkey Ctrl+Alt+G
   register/unregister, QApplication.setAutoStart (manual only, not in auto demo)
   (auto demo also clicks these after switching to section 6)

## Build

    javac -encoding UTF-8 -cp jqt-0.6.1-Exclusive-Kit.jar;theme-pack -d out JQtGallery.java
    java --enable-native-access=ALL-UNNAMED -Djava.library.path=<runtime> -cp "out;jqt-0.6.1-Exclusive-Kit.jar;theme-pack" JQtGallery

Themes: NordTheme/SolarizedTheme/TerminalTheme (in this dir, version-independent).

(C) SilentStudio, All rights reserved.