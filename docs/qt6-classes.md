# JQt × Qt 6 类覆盖路线图（QtWidgets 191 类全表）

> 数据源：doc.qt.io/qt-6/qtwidgets-module.html（2026-08-29 抓取）。
> 定位：JQt 绑定 **QtCore + QtGui + QtWidgets + 工业模块**；**Qt Quick/QML、Qt3D、QtGraphs、QtMultimedia 属范围外**（QtJambi 生态占位，JQt 不重复建设）。

## 写法分级

| 标记 | 含义 |
|------|------|
| ✅ 已实现 | JQt 已有绑定 |
| ✍️ 手写 | 值得手工精修（基类/值对象/model-view/集成点） |
| 🤖 机器骨架+手修 | 生成器产出骨架，人工补信号/特殊逻辑 |
| ⬜ 未规划 | 后续按需推进 |

## QtWidgets 191 类全表

| 类 | 状态 | 备注 |
|-----|------|------|
| qabstractbutton | ✍️ 手写 |  |
| qabstractgraphicsshapeitem | ⬜ 未规划 |  |
| qabstractitemdelegate | ⬜ 未规划 |  |
| qabstractitemview | ✍️ 手写 |  |
| qabstractscrollarea | ✍️ 手写 |  |
| qabstractslider | ✍️ 手写 |  |
| qabstractspinbox | ✍️ 手写 |  |
| qaccessiblewidget | ⬜ 未规划 |  |
| qapplication | ✅ 已实现 |  |
| qboxlayout | ⬜ 未规划 |  |
| qbuttongroup | ⬜ 未规划 |  |
| qcalendarwidget | ⬜ 未规划 |  |
| qcheckbox | ✅ 已实现 |  |
| qcolordialog | ✅ 已实现 |  |
| qcolumnview | ⬜ 未规划 |  |
| qcombobox | ✅ 已实现 |  |
| qcommandlinkbutton | ⬜ 未规划 |  |
| qcommonstyle | ⬜ 未规划 |  |
| qcompleter | ⬜ 未规划 |  |
| qdatawidgetmapper | ⬜ 未规划 |  |
| qdateedit | ⬜ 未规划 |  |
| qdatetimeedit | ✅ 已实现 |  |
| qdial | ✅ 已实现 |  |
| qdialog | ✅ 已实现 |  |
| qdialogbuttonbox | ⬜ 未规划 |  |
| qdockwidget | ⬜ 未规划 |  |
| qdoublespinbox | ⬜ 未规划 |  |
| qerrormessage | ⬜ 未规划 |  |
| qfiledialog | ✅ 已实现 |  |
| qfileiconprovider | ⬜ 未规划 |  |
| qfocusframe | ⬜ 未规划 |  |
| qfontcombobox | ⬜ 未规划 |  |
| qfontdialog | ✅ 已实现 |  |
| qformlayout | ✅ 已实现 |  |
| qframe | ✅ 已实现 |  |
| qgesture | ⬜ 未规划 |  |
| qgestureevent | ⬜ 未规划 |  |
| qgesturerecognizer | ⬜ 未规划 |  |
| qgraphicsanchor | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicsanchorlayout | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicsblureffect | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicscolorizeeffect | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicsdropshadoweffect | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicseffect | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicsellipseitem | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicsgridlayout | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicsitem | ✍️ 手写 |  |
| qgraphicsitemgroup | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicslayout | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicslayoutitem | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicslinearlayout | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicslineitem | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicsobject | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicsopacityeffect | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicspathitem | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicspixmapitem | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicspolygonitem | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicsproxywidget | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicsrectitem | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicsrotation | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicsscale | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicsscene | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicsscenecontextmenuevent | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicsscenedragdropevent | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicssceneevent | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicsscenehelpevent | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicsscenehoverevent | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicsscenemouseevent | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicsscenemoveevent | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicssceneresizeevent | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicsscenewheelevent | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicssimpletextitem | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicstextitem | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicstransform | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicsview | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgraphicswidget | ⬜ 未规划 | Graphics View 系（38 类，后续评估） |
| qgridlayout | ✅ 已实现 |  |
| qgroupbox | ✅ 已实现 |  |
| qhboxlayout | ✅ 已实现 |  |
| qheaderview | ✍️ 手写 |  |
| qinputdialog | ✅ 已实现 |  |
| qitemdelegate | ⬜ 未规划 |  |
| qitemeditorcreator | ⬜ 未规划 |  |
| qitemeditorcreatorbase | ⬜ 未规划 |  |
| qitemeditorfactory | ⬜ 未规划 |  |
| qkeysequenceedit | ⬜ 未规划 |  |
| qlabel | ✅ 已实现 |  |
| qlayout | ✅ 已实现 |  |
| qlayoutitem | ⬜ 未规划 |  |
| qlcdnumber | ⬜ 未规划 |  |
| qlineedit | ✅ 已实现 |  |
| qlistview | ✅ 已实现 |  |
| qlistwidget | ✅ 已实现 |  |
| qlistwidgetitem | ⬜ 未规划 |  |
| qmainwindow | ✅ 已实现 |  |
| qmdiarea | ⬜ 未规划 |  |
| qmdisubwindow | ⬜ 未规划 |  |
| qmenu | ✅ 已实现 |  |
| qmenubar | ✅ 已实现 |  |
| qmessagebox | ✅ 已实现 |  |
| qpangesture | ⬜ 未规划 |  |
| qpinchgesture | ⬜ 未规划 |  |
| qplaintextdocumentlayout | ⬜ 未规划 |  |
| qplaintextedit | ✍️ 手写 |  |
| qprogressbar | ✅ 已实现 |  |
| qprogressdialog | ⬜ 未规划 |  |
| qproxystyle | ⬜ 未规划 |  |
| qpushbutton | ✅ 已实现 |  |
| qradiobutton | ✅ 已实现 |  |
| qrhiwidget | ⬜ 未规划 |  |
| qrubberband | ⬜ 未规划 |  |
| qscrollarea | ✅ 已实现 |  |
| qscrollbar | ⬜ 未规划 |  |
| qscroller | ⬜ 未规划 |  |
| qscrollerproperties | ⬜ 未规划 |  |
| qsizegrip | ⬜ 未规划 |  |
| qsizepolicy | ⬜ 未规划 |  |
| qslider | ✅ 已实现 |  |
| qspaceritem | ⬜ 未规划 |  |
| qspinbox | ✅ 已实现 |  |
| qsplashscreen | ⬜ 未规划 |  |
| qsplitter | ✅ 已实现 |  |
| qsplitterhandle | ⬜ 未规划 |  |
| qstackedlayout | ✅ 已实现 |  |
| qstackedwidget | ⬜ 未规划 |  |
| qstandarditemeditorcreator | ⬜ 未规划 |  |
| qstatusbar | ✅ 已实现 |  |
| qstyle | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstyleditemdelegate | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstylefactory | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstylehintreturn | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstylehintreturnmask | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstylehintreturnvariant | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstyleoption | ✍️ 手写 |  |
| qstyleoptionbutton | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstyleoptioncombobox | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstyleoptioncomplex | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstyleoptiondockwidget | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstyleoptionfocusrect | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstyleoptionframe | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstyleoptiongraphicsitem | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstyleoptiongroupbox | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstyleoptionheader | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstyleoptionheaderv2 | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstyleoptionmenuitem | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstyleoptionmenuitemv2 | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstyleoptionprogressbar | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstyleoptionrubberband | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstyleoptionsizegrip | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstyleoptionslider | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstyleoptionspinbox | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstyleoptiontab | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstyleoptiontabbarbase | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstyleoptiontabwidgetframe | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstyleoptiontitlebar | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstyleoptiontoolbar | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstyleoptiontoolbox | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstyleoptiontoolbutton | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstyleoptionviewitem | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstylepainter | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qstyleplugin | ⬜ 未规划 | Style 系（QStyleOption 等，内部机制，部分随生成器覆盖） |
| qswipegesture | ⬜ 未规划 |  |
| qsystemtrayicon | ✅ 已实现 |  |
| qtabbar | ⬜ 未规划 |  |
| qtableview | ✍️ 手写 |  |
| qtablewidget | ✅ 已实现 |  |
| qtablewidgetitem | ⬜ 未规划 |  |
| qtablewidgetselectionrange | ⬜ 未规划 |  |
| qtabwidget | ✅ 已实现 |  |
| qtapandholdgesture | ⬜ 未规划 |  |
| qtapgesture | ⬜ 未规划 |  |
| qtextbrowser | ⬜ 未规划 |  |
| qtextedit | ✅ 已实现 |  |
| qtilerules | ⬜ 未规划 |  |
| qtimeedit | ⬜ 未规划 |  |
| qtoolbar | ✅ 已实现 |  |
| qtoolbox | ⬜ 未规划 |  |
| qtoolbutton | ⬜ 未规划 |  |
| qtooltip | ⬜ 未规划 |  |
| qtreeview | ✍️ 手写 |  |
| qtreewidget | ✅ 已实现 |  |
| qtreewidgetitem | ⬜ 未规划 |  |
| qtreewidgetitemiterator | ⬜ 未规划 |  |
| qundoview | ⬜ 未规划 |  |
| qvboxlayout | ✅ 已实现 |  |
| qwhatsthis | ⬜ 未规划 |  |
| qwidget | ✅ 已实现 |  |
| qwidgetaction | ⬜ 未规划 |  |
| qwidgetitem | ⬜ 未规划 |  |
| qwizard | ⬜ 未规划 |  |
| qwizardpage | ⬜ 未规划 |  |

## 手写类清单（32 个，已确认）

**基类族（10）**：QWidget、QAbstractItemView、QAbstractScrollArea、QAbstractSlider、QAbstractSpinBox、QAbstractButton、QLayout、QGraphicsItem、QStyleOption

**值对象（12）**：QColor、QPixmap、QIcon、QFont、QCursor、QRegion、QBrush、QPen、QUrl、QStringList、QDateTime、QPointF/QRectF/QSizeF

**model/view（6）**：QTableView、QTreeView、QHeaderView、QStandardItemModel、QTableWidget、QPlainTextEdit

**集成点（4）**：QClipboard、QSystemTrayIcon、QDesktopServices、QShortcut

## Widgets vs Quick 定位（FAQ）

- **并列技术栈**：Widgets=命令式控件（≈Swing），Quick=声明式场景图（≈JavaFX）——不是同一目标的两条路线
- JQt 选 Widgets：Java 强类型 API 直绑、工业模块同体系、QtJambi 已占 Quick
- Quick 属范围外：需要 QML 引擎绑定，与 Java 生态结合别扭，不重复建设

## 覆盖统计

- QtWidgets：191 类，JQt 已实现 41（21.5%），手写待做 11，机器批量候选 69
- Qt 全模块 1623 类（classes.html）；范围外：Quick/3D/Graphs/Multimedia 等
