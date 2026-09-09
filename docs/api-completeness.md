# JQt Qt 对齐度矩阵(Qt 6 All-Members vs JQt 同名实现)

> **口径与方法(2026-09-03 修订,取代 08-29 旧表)**:
> - **Qt 侧** = doc.qt.io/qt-6 `<class>-members.html` **全部成员**(含继承成员/属性/信号/重载展开),数据缓存于 `tools/jqt-gen/qt-classes.json`(51 类;重抓:`jqt-gen fetch`)。
> - **JQt 侧** = 类文件**自声明 public 方法**与 Qt 成员**同名**的数量(继承未摊入子类;JQt 扁平化设计——如盒布局方法承载在 QLayout、各条方法承载在基类——会使子类行数值系统性偏低)。
> - **与 08-29 旧表不可直接比较**(旧表 Qt 侧为"自身成员"口径,且 QColor/QFont 行失真:QColor 当时记 3/102,同日稍后值类重构为 106 方法,实际≈100%)。
> - 用法:本表是**全景对齐度参考**;缺口工作单以 `tools/jqt-gen/qt-plan.json` 与 roadmap A2 为准;L1 分级完成度另见 api-tiering.md(2026-09-03 起 L1 清单 178/178=100%)。

## 对齐度矩阵(2026-09-03)

| 类 | Qt 成员(All) | JQt 同名实现 | 对齐度 |
|----|------|------|------|
| QWidget | 393 | 141 | 35.9% |
| QSqlDatabase | 66 | 17 | 25.8% |
| QAction | 145 | 34 | 23.4% |
| QSqlQuery | 67 | 14 | 20.9% |
| QSerialPort | 191 | 33 | 17.3% |
| QApplication | 237 | 28 | 11.8% |
| QSystemTrayIcon | 100 | 9 | 9.0% |
| QLineEdit | 466 | 40 | 8.6% |
| QSettings | 119 | 10 | 8.4% |
| QGridLayout | 161 | 13 | 8.1% |
| QInputDialog | 458 | 36 | 7.9% |
| QFile | 192 | 14 | 7.3% |
| QComboBox | 466 | 33 | 7.1% |
| QLayout | 136 | 9 | 6.6% |
| QTabWidget | 447 | 24 | 5.4% |
| QPushButton | 437 | 23 | 5.3% |
| QClipboard | 99 | 5 | 5.1% |
| QLabel | 444 | 21 | 4.7% |
| QFormLayout | 183 | 8 | 4.4% |
| QMainWindow | 447 | 19 | 4.3% |
| QProgressBar | 416 | 17 | 4.1% |
| QMenu | 440 | 17 | 3.9% |
| QSpinBox | 450 | 16 | 3.6% |
| QMessageBox | 451 | 16 | 3.5% |
| QStackedLayout | 149 | 5 | 3.4% |
| QDateTimeEdit | 482 | 16 | 3.3% |
| QDir | 90 | 3 | 3.3% |
| QSplitter | 437 | 14 | 3.2% |
| QDialog | 406 | 12 | 3.0% |
| QListView | 572 | 16 | 2.8% |
| QListWidget | 624 | 16 | 2.6% |
| QTableWidget | 660 | 17 | 2.6% |
| QToolBar | 425 | 10 | 2.4% |
| QDial | 434 | 10 | 2.3% |
| QGroupBox | 407 | 8 | 2.0% |
| QMenuBar | 414 | 8 | 1.9% |
| QScrollArea | 440 | 7 | 1.6% |
| QCheckBox | 432 | 7 | 1.6% |
| QSlider | 432 | 7 | 1.6% |
| QFrame | 407 | 6 | 1.5% |
| QTreeWidget | 658 | 9 | 1.4% |
| QStatusBar | 406 | 5 | 1.2% |
| QFileDialog | 475 | 5 | 1.1% |
| QRadioButton | 427 | 3 | 0.7% |
| QOpenGLWidget | 417 | 3 | 0.7% |
| QColorDialog | 423 | 2 | 0.5% |
| QFontDialog | 418 | 1 | 0.2% |
| QStackedWidget | 420 | 1 | 0.2% |
| QHBoxLayout | 155 | 0 | 0.0% |
| QScrollBar | 429 | 0 | 0.0% |
| QVBoxLayout | 155 | 0 | 0.0% |

**合计**:51 类,平均对齐 5.6%(All-Members 口径含继承,数值天然偏低;看相对增长与缺口单)。

## 纯 Java 值类(未入抓取表,单独说明)

QColor / QFont / QPoint / QRect 等值类走"纯 Java 重实现"策略(零 native)。例:QColor 自实现 **106** 个 public 方法,对照 Qt 自身成员 ~102,覆盖≈100%(曾因同日重构被旧表误记为 3)。

## 失真修复记录

- 2026-08-29 旧表 QColor 3/102(2.9% 🔴)系快照早于当日值类重构 13 分钟,同款风险:QFont。
- 2026-09-03 起:本表带口径与缓存文件,可复现;防再犯 = roadmap A2.0(矩阵重出接线到发布检查)。
