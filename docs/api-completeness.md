# JQt 方法完整度矩阵（Qt 6 成员 vs 已实现）

> 数据：doc.qt.io/qt-6/<class>-members.html 成员链接数（含属性/信号/重载），JQt public 方法数（源码统计）。2026-08-29。

| 类 | Qt 成员 | JQt 已实现 | 缺口 | 完整度 | 梯队 |
|-----|--------|-----------|------|--------|------|
| qcolor | 102 | 3 | 99 | 2.9% | 🔴 |
| qformlayout | 64 | 2 | 62 | 3.1% | 🔴 |
| qfiledialog | 81 | 3 | 78 | 3.7% | 🔴 |
| qdir | 78 | 3 | 75 | 3.8% | 🔴 |
| qframe | 23 | 1 | 22 | 4.3% | 🔴 |
| qinputdialog | 61 | 3 | 58 | 4.9% | 🔴 |
| qfontdialog | 19 | 1 | 18 | 5.3% | 🔴 |
| qdatetimeedit | 72 | 4 | 68 | 5.6% | 🔴 |
| qpainter | 203 | 12 | 191 | 5.9% | 🔴 |
| qgroupbox | 26 | 2 | 24 | 7.7% | 🔴 |
| qrect | 75 | 6 | 69 | 8% | 🔴 |
| qmenubar | 41 | 4 | 37 | 9.8% | 🔴 |
| qgridlayout | 41 | 4 | 37 | 9.8% | 🔴 |
| qtabwidget | 68 | 7 | 61 | 10.3% | 🟠 |
| qsettings | 46 | 5 | 41 | 10.9% | 🟠 |
| qprinter | 63 | 7 | 56 | 11.1% | 🟠 |
| qcolordialog | 24 | 3 | 21 | 12.5% | 🟠 |
| qsqlquery | 51 | 7 | 44 | 13.7% | 🟠 |
| qsize | 27 | 4 | 23 | 14.8% | 🟠 |
| qpoint | 20 | 3 | 17 | 15% | 🟠 |
| qstackedlayout | 26 | 4 | 22 | 15.4% | 🟠 |
| qcombobox | 96 | 15 | 81 | 15.6% | 🟠 |
| qstatusbar | 19 | 3 | 16 | 15.8% | 🟠 |
| qsplitter | 38 | 6 | 32 | 15.8% | 🟠 |
| qtextedit | 126 | 20 | 106 | 15.9% | 🟠 |
| qdialog | 25 | 4 | 21 | 16% | 🟠 |
| qlistview | 72 | 12 | 60 | 16.7% | 🟠 |
| qmenu | 64 | 11 | 53 | 17.2% | 🟠 |
| qtablewidget | 79 | 14 | 65 | 17.7% | 🟠 |
| qmessagebox | 63 | 12 | 51 | 19% | 🟠 |
| qprogressbar | 31 | 6 | 25 | 19.4% | 🟠 |
| qtoolbar | 38 | 8 | 30 | 21.1% | 🟠 |
| qdial | 19 | 4 | 15 | 21.1% | 🟠 |
| qopenglwidget | 33 | 7 | 26 | 21.2% | 🟠 |
| qscrollarea | 18 | 4 | 14 | 22.2% | 🟠 |
| qaction | 70 | 17 | 53 | 24.3% | 🟠 |
| qfile | 47 | 12 | 35 | 25.5% | 🟠 |
| qlabel | 54 | 14 | 40 | 25.9% | 🟠 |
| qlayout | 50 | 13 | 37 | 26% | 🟠 |
| qsystemtrayicon | 23 | 6 | 17 | 26.1% | 🟠 |
| qlineedit | 99 | 26 | 73 | 26.3% | 🟠 |
| qsqldatabase | 49 | 13 | 36 | 26.5% | 🟠 |
| qtreewidget | 63 | 17 | 46 | 27% | 🟠 |
| qwidget | 304 | 84 | 220 | 27.6% | 🟠 |
| qclipboard | 20 | 6 | 14 | 30% | 🟢 |
| qserialport | 72 | 24 | 48 | 33.3% | 🟢 |
| qlistwidget | 57 | 19 | 38 | 33.3% | 🟢 |
| qradiobutton | 10 | 4 | 6 | 40% | 🟢 |
| qslider | 16 | 8 | 8 | 50% | 🟢 |
| qmainwindow | 60 | 31 | 29 | 51.7% | 🟢 |
| qcheckbox | 17 | 9 | 8 | 52.9% | 🟢 |
| qspinbox | 27 | 16 | 11 | 59.3% | 🟢 |
| qapplication | 50 | 32 | 18 | 64% | 🟢 |
| qpushbutton | 23 | 17 | 6 | 73.9% | 🟢 |

**汇总**：平均完整度 21.4%，总缺口 2461 个方法（54 个已实现类）。

## 补全顺序建议

1. **值对象/基础类（🔴，完整度 <10%）**：QColor/QPoint/QRect/QSize/QDir/QFile/QSettings/QPainter/QDateTimeEdit/QFileDialog/QInputDialog/QFontDialog/QFrame/QFormLayout/QGridLayout —— 同时解锁带对象签名的方法
2. **常用控件（🟠）**：QWidget(缺220)/QTextEdit(106)/QComboBox(81)/QLineEdit(73)/QTableWidget(65)/QLabel(40)
3. **其余 🟠🟢** 按批次推进
