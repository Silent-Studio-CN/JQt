# JQt API 分级设计（L1 / L2 / L3）

> 战略定位：JQt 的护城河是**手写 API 质量**。QtJambi 是机器生成的 C++ 翻译腔；
> JQt 用人工设计的分级 API，让 Java 开发者"不需要懂 Qt 也能写出漂亮的 Qt 程序"。
>
> 本文档是 API 实现蓝图：每个类的方法按使用频率分三级，
> 级别决定它在 Java 层的位置（主类直接方法 / 分组对象 / .native() 高级入口）。

## 1. 分级标准

| 级别 | 名称 | 判断标准 | 实现策略 |
|------|------|---------|---------|
| **L1** | 常用 | 80% 的 Java 开发者在 80% 的场景下会用到 | 直接放在主类上，IDE 自动补全优先展示 |
| **L2** | 少用 | 20% 的开发者会在特定场景下用到，但一旦用到就很关键 | 通过分组对象访问（如 `.window()`、`.style()`、`.drag()`） |
| **L3** | Java 不用/几乎不用 | Java 开发者几乎不需要直接调用，或有更好的 Java 替代 | 放在 `.native()` 高级入口中，文档标注"高级 API" |

## 2. L2 分组对象设计

所有 L2 方法按语义分组，通过 `QWidget` 基类上的惰性访问器获取：

| 访问器 | 分组 | 涵盖内容 |
|--------|------|---------|
| `.window()` | 窗口级状态 | 全屏/最大化/最小化/模态/不透明度/窗口状态/几何保存恢复/激活 |
| `.style()` | 外观样式 | 样式表/字体/调色板/光标/图标/工具提示/边框 |
| `.drag()` | 拖拽与鼠标 | 拖放接受/鼠标追踪/鼠标键盘捕获 |
| `.focus()` | 焦点管理 | 焦点策略/焦点代理/焦点链 |
| `.event()` | 事件系统（预留） | 自定义事件处理、输入法（Phase 后置） |
| `.native()` | 高级 API（L3） | 原生句柄/绘制引擎/底层能力，文档标注"高级 API" |

## 3. 命名与呈现规则

- Java 命名：camelCase；布尔 getter 保留 `isXxx`；信号统一 `onXxx`；
- 语义去冗余：`QMainWindow.setTitle`（而非 setWindowTitle——类本身是窗口）；
- 默认参数：Qt 的 `parent` 参数在 Java 中省略（用 `addWidget`/布局建立父子关系）；
- 返回 `this` 支持链式调用（L1 setter 尽量链式）；
- 文档注释标注"①常用 / ②少用（window()） / ③高级（native()）"。

---

## 4. 逐类分级

### 4.1 QWidget（基类，262 个 Qt 方法 → 分级后约 40 L1 + 45 L2 + 其余 L3）

**L1（直接方法，约 40 个）**：

| Java API | Qt 方法 | 说明 |
|----------|---------|------|
| show() / hide() | show / hide | 显示/隐藏 |
| setVisible(boolean) | setVisible | |
| isVisible() / isHidden() | isVisible / isHidden | |
| resize(w,h) / size() | resize / size | |
| width() / height() | width / height | |
| setFixedSize(w,h) | setFixedSize | 锁定尺寸 |
| setFixedWidth / setFixedHeight | 同左 | |
| setMinimumSize / setMaximumSize | 同左 | |
| setTitle(String) | setWindowTitle | 窗口标题 |
| title() | windowTitle | |
| setEnabled(boolean) / isEnabled() | 同左 | |
| close() | close | |
| setToolTip(String) | setToolTip | |
| setCursor(Cursor) | setCursor | |
| setFont(Font) | setFont | |
| setStyleSheet(String) | setStyleSheet | |
| setFocus() | setFocus | |
| hasFocus() | hasFocus | |
| setWindowIcon(Icon) | setWindowIcon | |
| setParent(QWidget) | setParent | |
| raise() / lower() | raise / lower | 层级 |
| update() / repaint() | update / repaint | 重绘 |
| move(x,y) / pos() | move / pos | |
| x() / y() | x / y | |
| geometry() / setGeometry(x,y,w,h) | 同左 | |
| setLayout(QLayout) | setLayout | |
| grab() → Image | grab | 控件截图 |
| isWindow() | isWindow | |
| setAttribute(attr, on) | setAttribute | 常用属性（WA_*） |
| setAutoFillBackground(boolean) | 同左 | |
| setContentsMargins(l,t,r,b) | 同左 | |
| setSizePolicy / sizePolicy | 同左 | |

**L2（分组对象，约 45 个）**：

| 分组 | Java API | Qt 方法 |
|------|----------|---------|
| window() | fullScreen() / showFullScreen | showFullScreen |
| window() | maximize() / minimize() / normal() | showMaximized / showMinimized / showNormal |
| window() | isFullScreen() / isMaximized() / isMinimized() | 同左 |
| window() | setOpacity(double) / opacity() | setWindowOpacity |
| window() | setModal(boolean) / isModal() | 同左 |
| window() | setWindowState(State) / windowState() | 同左 |
| window() | saveGeometry() / restoreGeometry(bytes) | 同左 |
| window() | activate() | activateWindow |
| window() | isActive() | isActiveWindow |
| window() | setWindowFlag(flag, on) / setWindowFlags(flags) | 同左 |
| style() | font() / setFont | 同左 |
| style() | palette() / setPalette | 同左 |
| style() | styleSheet() | 同左 |
| style() | setStyle(QStyle) | setStyle |
| style() | setMouseTracking(boolean) | 同左 |
| style() | setWindowOpacity（已列入 window()） | |
| drag() | setAcceptDrops(boolean) / acceptDrops() | 同左 |
| drag() | grabMouse() / releaseMouse() | 同左 |
| drag() | grabKeyboard() / releaseKeyboard() | 同左 |
| focus() | setFocusPolicy(Policy) / focusPolicy() | 同左 |
| focus() | setFocusProxy(widget) / focusProxy() | 同左 |
| focus() | nextInFocusChain() / previousInFocusChain() | 同左 |
| focus() | clearFocus() | 同左 |
| event() | setInputMethodHints(hints) | 同左（预留给输入法） |

**L3（.native() 高级入口）**：winId/effectiveWinId（原生句柄）、nativeEvent、render/paintEngine/initPainter/metric/backingStore（底层绘制）、grabGesture/ungrabGesture（手势）、setMask/mask（异形窗口）、setWindowRole/windowRole、setWindowFilePath、setTabletTracking、grabShortcut/releaseShortcut、setShortcutAutoRepeat、setupUi、setLocale/unsetLocale、setLayoutDirection、setGraphicsEffect、setScreen、createWindowContainer、QWIDGETSIZE_MAX（常量）、actionEvent/changeEvent 等事件回调（事件回调统一走 onXxx 模式，不暴露 protected 方法）。

### 4.2 QApplication（45 个方法）

**L1**：exec()、quit()、onAboutToQuit()、schedule(Runnable, ms)、scheduleQuit(ms)（现有实现已覆盖）

**L2（QApplication 直接方法）**：setStyle / style、setStyleSheet / styleSheet、setFont / font、setPalette / palette、beep()、closeAllWindows()、alert(window, ms)、activeWindow()、focusWidget()、widgetAt(x,y)、topLevelAt(x,y)、topLevelWidgets()、allWidgets()、setQuitOnLastWindowClosed / quitOnLastWindowClosed、setDoubleClickInterval、setCursorFlashTime、setWheelScrollLines、setStartDragDistance / setStartDragTime、setKeyboardInputInterval

**L3（.native()）**：notify、event、qApp（单例访问——Java 直接用全局静态实例即可，无需暴露）、setEffectEnabled / isEffectEnabled、navigationMode、autoSipEnabled、activeModalWidget、activePopupWidget

### 4.3 QPushButton + QAbstractButton（67 个方法合并）

**L1**：setText / text、onClicked(Runnable)、onPressed(Runnable)、onReleased(Runnable)、setCheckable(boolean) / isCheckable()、setChecked(boolean) / isChecked()、onToggled(Consumer<Boolean>)、setEnabled / isEnabled（继承）、setIcon(Icon)、click()（程序化点击）、setDefault(boolean)、isDown() / setDown(boolean)、toggle()

**L2**：setAutoRepeat(boolean) / autoRepeat()、setAutoRepeatDelay(ms)、setAutoRepeatInterval(ms)、setIconSize(Size) / iconSize()、setShortcut(KeySequence)、setAutoExclusive(boolean) / autoExclusive()、setFlat(boolean) / isFlat()、animateClick(ms)、group()（返回按钮组）

**L3（.native()）**：hitButton、nextCheckState、checkStateSet、initStyleOption、paintEvent、event 系列（全部由 onXxx 信号模式替代）

### 4.4 QLabel（52 个方法）

**L1**：setText / text、setNum(number)、clear()、setWordWrap(boolean) / wordWrap()、setAlignment(Alignment)、alignment()、setPixmap(Image)、setScaledContents(boolean)、setMargin(int) / margin()、setIndent(int) / indent()

**L2**：setTextFormat(Format)、setTextInteractionFlags(flags)、setOpenExternalLinks(boolean) / openExternalLinks()、onLinkActivated(Consumer<String>)、onLinkHovered(Consumer<String>)、setBuddy(widget)、setMovie、setPicture、selectedText()、setSelection(start, len)、hasSelectedText()

**L3（.native()）**：heightForWidth、setResourceProvider、resourceProvider、paintEvent、event 系列

### 4.5 QLineEdit（95 个方法）

**L1**：setText / text、text()、clear()、setPlaceholderText / placeholderText、setReadOnly(boolean) / isReadOnly()、setMaxLength(int) / maxLength()、setEchoMode(Mode) / echoMode()（Password 等）、onTextChanged(Consumer<String>)、onReturnPressed(Runnable)、onEditingFinished(Runnable)、selectAll()、deselect()、hasSelectedText()、selectedText()、setEnabled / isEnabled（继承）、setFrame(boolean)、isModified() / setModified(boolean)

**L2**：setAlignment / alignment、setValidator / validator、setInputMask / inputMask、copy() / cut() / paste() / undo() / redo()（isUndoAvailable/isRedoAvailable）、setCursorPosition / cursorPosition、cursorPositionAt(pos)、insert(text)、backspace() / del()、home() / end()、cursorBackward/Forward、setSelection(start, len)、setTextMargins / textMargins、setClearButtonEnabled(boolean) / isClearButtonEnabled()、setDragEnabled(boolean) / dragEnabled()、onTextEdited(Consumer<String>)、onSelectionChanged(Runnable)、onCursorPositionChanged(BiConsumer)

**L3（.native()）**：completer/setCompleter（可用 Java 侧 JQtCompleter 高级 API）、createStandardContextMenu、setCursorMoveStyle、cursorRect、displayText、hasAcceptableInput、inputRejected、setCompleter、initStyleOption、事件系列

### 4.6 QComboBox（91 个方法）

**L1**：addItem(String)、addItems(List<String>)、insertItem(idx, String)、removeItem(idx)、clear()、count()、itemText(idx)、setCurrentIndex(idx) / currentIndex()、setCurrentText / currentText()、onCurrentIndexChanged(Consumer<Integer>)、onCurrentTextChanged(Consumer<String>)、setEditable(boolean) / isEditable()、setPlaceholderText / placeholderText、setMaxCount / maxCount、setMaxVisibleItems、currentData()

**L2**：setItemText(idx, text)、setItemIcon、itemIcon、findText / findData、onActivated(Consumer<Integer>)、onHighlighted(Consumer<Integer>)、setDuplicatesEnabled / duplicatesEnabled、setSizeAdjustPolicy、setInsertPolicy、insertSeparator、setIconSize、setValidator、onEditTextChanged(Consumer<String>)、setCurrentIndex 配合 onActivated

**L3（.native()）**：model/setModel、view/setView、lineEdit/setLineEdit、itemDelegate/setItemDelegate、setItemData、rootModelIndex、setLabelDrawingMode、showPopup / hidePopup、completer 相关

### 4.7 QListWidget（52 个方法）

**L1**：addItem(String)、addItems(List<String>)、insertItem(idx, String)、removeItem / takeItem、clear()、count()、item(row) → String、setCurrentRow / currentRow、setCurrentItem、onItemClicked(Consumer<Integer>)、onCurrentRowChanged(Consumer<Integer>)、onItemDoubleClicked、selectedItems() → List<Integer>、setSortingEnabled / isSortingEnabled、sortItems()

**L2**：findItems(text, matchFlags)、itemAt(pos)、row(item)、onItemSelectionChanged(Runnable)、onItemChanged、onItemActivated、setSelectionMode(Mode)、setItemWidget / removeItemWidget、scrollToItem、visualItemRect、openPersistentEditor / closePersistentEditor

**L3（.native()）**：mimeData/mimeTypes/dropMimeData（拖放数据）、setSupportedDragActions、setSelectionModel、itemFromIndex、indexFromItem、editItem、itemEntered

### 4.8 QMainWindow（56 个方法）

**L1**：setCentralWidget(widget) / centralWidget()、setMenuBar(bar) / menuBar()、setStatusBar(bar) / statusBar()、addToolBar(bar)、addDockWidget(area, widget)、removeToolBar、removeDockWidget、setWindowTitle / resize / show（继承）

**L2**：setIconSize / iconSize、setToolButtonStyle / toolButtonStyle、setAnimated / isAnimated、setDockOptions / dockOptions、setDocumentMode / documentMode、saveState() / restoreState(bytes)、setCorner / corner、tabifyDockWidget / tabifiedDockWidgets、splitDockWidget、resizeDocks、setDockNestingEnabled、setUnifiedTitleAndToolBarOnMac

**L3（.native()）**：createPopupMenu、takeCentralWidget、insertToolBarBreak、setMenuWidget、restoreDockWidget、dockWidgetArea、toolBarArea、tabPosition/setTabPosition、tabShape/setTabShape、menuWidget、setTabPosition

### 4.9 QBoxLayout / QGridLayout（34 + 38 个方法）

**L1**：addWidget(widget)、addLayout(layout)（嵌套）、addStretch(int)（box）、setSpacing(int) / spacing()、setContentsMargins（继承）、addSpacing(int)（box）、insertWidget(idx, widget)（box）、setStretch / setStretchFactor（box）

**L2**：setDirection（box）、addStrut（box）、setRowStretch / setColumnStretch（grid）、setRowMinimumHeight / setColumnMinimumWidth（grid）、setHorizontalSpacing / setVerticalSpacing（grid）、addWidget(widget, row, col)（grid）、addLayout(layout, row, col)、setOriginCorner、cellRect、getItemPosition

**L3（.native()）**：addSpacerItem / insertSpacerItem、takeAt、itemAt、count、invalidate、setGeometry、expandingDirections、maximumSize / minimumSize、heightForWidth 系列

### 4.10 QDialog / QMessageBox（25 + 54 个方法）

**JQtDialog L1**：exec()（模态阻塞）、accept()、reject()、done(int)、result()、setModal / isModal、show()、close()、setTitle、onAccepted(Runnable)、onRejected(Runnable)、setWindowTitle

**QMessageBox L2-L1 静态工厂**：info(parent, title, text)、warning(...)、critical(...)、question(...)（静态方法，返回用户选择）、setText / setInformativeText / setDetailedText、addButton(StandardButton)、setDefaultButton、setIcon(Icon)

**L3（.native()）**：open()、showEvent、setSizeGripEnabled、adjustPosition、最小化按钮行为等

### 4.11 其他类（分级概览）

| 类 | L1（主类直接） | L2（分组） | L3（native） |
|----|--------------|-----------|-------------|
| **JQtMenuBar** | addMenu(String)/addMenu(JQtMenu)、addAction | insertMenu、setNativeMenuBar | setCornerWidget |
| **JQtMenu** | addAction(String/icon)、addSeparator、addSubMenu、popup(pos)、exec() | insertAction、removeAction、setTitle、onTriggered | setTearOffEnabled、setToolTipsVisible |
| **JQtAction** | setText、setIcon、setEnabled、setCheckable、setChecked、onTriggered、setShortcut | setIconText、setMenu、setSeparator、onToggled | setShortcutContext、setData、setStatusTip |
| **JQtToolBar** | addAction、addSeparator、addWidget、setMovable、onActionTriggered | setToolButtonStyle、setIconSize、setAllowedAreas | setFloatable、toggleViewAction |
| **JQtTreeWidget** | addTopLevelItem、setHeaderLabels、clear、currentItem、onItemClicked、setCurrentItem | expandAll/collapseAll、expandItem、setItemText、selectedItems、onItemDoubleClicked | setItemWidget、setRootIsDecorated、header() |
| **QScrollArea** | setWidget、widget()、setWidgetResizable | setAlignment、ensureVisible、onVerticalScrollBarChanged | setFrameShape、takeWidget |
| **JQtSpinBox** | value()、setValue、onValueChanged、setRange、setSuffix | setPrefix、setSingleStep、setDecimals（double）、onEditingFinished | setGroupSeparatorShown、setSpecialValueText |
| **QCheckBox** | setChecked、isChecked、setText、onToggled、onStateChanged | setTristate、checkState、setTextVisible | setIconSize |
| **JQtColorDialog** | 静态 getColor()、setCurrentColor、onColorSelected | setOption、setWindowTitle | setCustomColor、customColorCount |
| **JQtTabWidget** | addTab、insertTab、removeTab、setCurrentIndex、currentIndex、onCurrentChanged | setTabText、setTabIcon、setTabsClosable、onTabCloseRequested | setCornerWidget、setDocumentMode、setMovable |
| **JQtPlainTextEdit** | setPlainText、toPlainText、appendPlainText、clear、setReadOnly、onTextChanged | insertPlainText、setPlaceholderText、setTabStopDistance、undo/redo、find | setDocument、setLineWrapMode、setMaximumBlockCount |
| **JQtStatusBar** | showMessage(String, ms)、clearMessage、addWidget | addPermanentWidget、onMessageChanged | setSizeGripEnabled |
| **QProgressBar** | setValue、value、setRange、setMaximum、setMinimum | setTextVisible、setFormat、onValueChanged | setAlignment、setInvertedAppearance |

## 5. 实现顺序建议

1. **第一批（已完成）**：QMainWindow/QPushButton/QLabel/QLineEdit/QComboBox/QListWidget + 布局（L1 子集已实现）
2. **第二批（HtmlWorkbench 前置）**：JQtMenuBar/JQtMenu/JQtAction/JQtToolBar/JQtTreeWidget/QScrollArea/JQtStatusBar/JQtSpinBox/QCheckBox/JQtColorDialog/JQtDialog（L1 全量）
3. **第三批**：L2 分组对象（window()/style()/drag()/focus()）落地到 QWidget 基类
4. **第四批**：QMessageBox 静态工厂、JQtTabWidget、JQtPlainTextEdit、QProgressBar
5. **第五批**：.native() 高级入口（winId、绘制引擎等），文档标注"高级 API"
6. **持续**：每个新控件先出分级表（本文档），评审后再实现

## 6. 质量红线（对 QtJambi 的差异化）

- 禁止"翻译腔"：方法签名按 Java 习惯设计（去掉 parent 参数、信号用 onXxx、枚举用 Java enum）；
- 每个 L1 方法必须有中文 javadoc + 一个代码示例（在文档中维护）；
- L1 方法必须链式友好（返回 this）；
- 所有信号回调在 GUI 线程（无需用户加锁）；
- 任何方法不允许 native 崩溃：统一走句柄注册表 + IllegalStateException。
