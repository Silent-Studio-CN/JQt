/* ============================================================
 * JQt Fluent Theme Template (self-authored)
 * %var% placeholders are filled by JQtApplication.setTheme().
 * ============================================================ */
* { font-family: "Microsoft YaHei UI", "Segoe UI"; font-size: 13px; }
QWidget { background: %win-bg%; color: %fg%; }

/* ---- 卡片 ---- */
JQtPanel { background: %card-bg%; border: 1px solid %card-border%; border-radius: 8px; }

/* ---- 按钮 ---- */
QPushButton { background: %btn-bg%; color: %btn-fg%; border: none; border-radius: 4px; padding: 6px 14px; }
QPushButton:hover { background: %btn-hover%; }
QPushButton:pressed { background: %btn-pressed%; }
QPushButton:disabled { background: %btn-disabled%; color: %fg-disabled%; }
QPushButton:checked { background: %accent%; color: %accent-fg%; }
QPushButton:checked:hover { background: %accent-hover%; }

/* ---- 开关（checkable 按钮胶囊）---- */
QPushButton:checked { background: %accent%; }

/* ---- 复选框开关 ---- */
QCheckBox { spacing: 8px; color: %fg%; }
QCheckBox::indicator { width: 40px; height: 20px; border-radius: 10px; background: %switch-off%; }
QCheckBox::indicator:hover { background: %switch-off-hover%; }
QCheckBox::indicator:checked { background: %accent%; }
QCheckBox::indicator:checked:hover { background: %accent-hover%; }

/* ---- 导航（列表）---- */
QListWidget { background: transparent; border: none; outline: none; }
QListWidget::item { padding: 9px 12px; border-radius: 4px; color: %nav-fg%; }
QListWidget::item:hover { background: %nav-hover%; }
QListWidget::item:selected { background: %nav-selected%; color: %accent%; }

/* ---- 输入框 ---- */
QLineEdit { background: %input-bg%; border: 1px solid %input-border%; border-radius: 4px; padding: 5px 8px; color: %fg%; }
QLineEdit:focus { border-color: %accent%; }
QLineEdit:disabled { color: %fg-disabled%; }

/* ---- 下拉框 ---- */
QComboBox { background: %btn-bg%; border: 1px solid %input-border%; border-radius: 4px; padding: 5px 8px; color: %fg%; }
QComboBox:hover { background: %btn-hover%; }
QComboBox QAbstractItemView { background: %card-bg%; color: %fg%; border: 1px solid %input-border%; selection-background-color: %nav-selected%; selection-color: %accent%; }

/* ---- 标签 ---- */
QLabel { color: %fg%; background: transparent; }
QLabel[role="title"] { font-size: 16px; font-weight: bold; color: %fg-strong%; }
QLabel[role="hint"] { color: %fg-hint%; }

/* ---- 标题栏按钮（Fluent 打磨）---- */
QPushButton#titlebarBtn { background: transparent; border: none; border-radius: 4px; padding: 4px 12px; font-size: 12px; min-height: 0; max-height: 34px; color: %fg%; }
QPushButton#titlebarBtn:hover { background: rgba(255, 255, 255, 0.05); }
QPushButton#titlebarBtn:pressed { background: rgba(255, 255, 255, 0.09); }
QPushButton#titlebarClose { background: transparent; border: none; border-radius: 4px; padding: 4px 12px; font-size: 12px; min-height: 0; max-height: 34px; color: %fg%; }
QPushButton#titlebarClose:hover { background: #c42b1c; color: #ffffff; }
QPushButton#titlebarClose:pressed { background: #a02418; color: #ffffff; }