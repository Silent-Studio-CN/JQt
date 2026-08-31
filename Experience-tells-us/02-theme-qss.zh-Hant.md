# 02 · 主題渲染與 QSS

> JQt 的主題體系：官方 setTheme(path, vars) 模板渲染 + 社區自定義雙主題。
> 本章記錄模板機制、樣式優先級、殘留問題的全部經驗。

## 1. fluent.qss.tpl 模板渲染機制

- 官方模板有 **22 個 %var% 佔位符**（win-bg/fg/accent/btn-*/card-*/input-*/nav-*...）。
- 渲染方式：讀模板 → `replace("%"+key+"%", value)` → `app.setStyleSheet(qss)`。
- **渲染式（render-based）**：不依賴 setTheme 的文件路徑查找，exe 打包版安全。
- 模板讀取優先級：文件系統 themes/fluent.qss.tpl 優先，jar 內資源兜底。
- 社區版常用三段拼接：fluent.qss.tpl（基礎）+ qraft-styles.qss（立體按鈕）+ sck-extra.qss（專屬控件）。

## 2. 樣式優先級（本目錄最容易踩的坑）

優先級從高到低：
1. **控件級 setStyleSheet**（`widget.setStyleSheet(...)`）—— 最高，永遠蓋住全局
2. **對象名選擇器**（`QPushButton#themeBtn`）—— 次高
3. **app 級 / win 級 setStyleSheet** —— 全局

### 坑 1：控件級暗色覆蓋釘死主題
```java
// 错误示范：启动时对每个控件单独 setStyleSheet 暗色
applyDarkStyles();   // 每个控件 setStyleSheet 暗色
// 之后无论怎么切全局主题，控件级样式优先级更高 → 永远暗色
```
**修復**：刪除控件級批量覆蓋，全部交給全局 QSS 模板渲染；
個別控件需要特殊樣式時，用 objectName 選擇器放進模板。

### 坑 2：內聯樣式硬編碼顏色（黑殘留）
```java
topBar.setStyleSheet("QFrame#topBar { background-color: #1f1f1f; }");  // 硬编码
```
內聯樣式優先級最高，模板渲染無法覆蓋 → 切淺色後頂欄還是黑的。
**修復**：移除內聯樣式，改爲模板規則 + 變量（`%topbar-bg%`）。

### 坑 3：QSS 文件裏硬編碼深色
```css
QTextEdit, QPlainTextEdit { background: #1a1a1a; }   // 日志区永远黑
```
**修復**：變量化 —— `%terminal-bg%` / `%terminal-fg%`，淺色表白底深字、
深色表保持終端黑底。

## 3. 雙主題設計（淺色/深色變量表）

- 兩個同構變量表：`lightVars()` / `darkVars()`，鍵集合必須完全一致，
否則某主題渲染後殘留 %var% 導致 QSS 解析失敗。
- **設計令牌**統一進變量表：border/border-deep/radius/card-radius/pill-radius/fontsize。
- 強調色（accent）動態變量：切換主題色時用 lighten/darken/withAlpha 生成
accent-hover/pressed/deep/ghost-hover/ghost-pressed。
- **applyTheme(name) 必須真正使用 name 參數**——歷史上出現過硬編碼暗色
QSS 完全忽略參數的版本（切淺色無效的最大元兇）。

## 4. QSS 解析失敗的排查

- Qt 報 `Could not parse application stylesheet` 時：
1. 檢查渲染結果是否有**未替換的 %var%**（正則 `%[a-z-]+%` 掃描）；
2. 檢查顏色格式（rgba() 與 #RRGGBB 混用）；
3. 檢查選擇器語法（`QLabel#detailPanel QLabel` 這種後代+ID 組合）。
- 模板註釋裏的字面 %var%（如 "filled by setTheme()" 說明文字）無害，不用管。

## 5. 主題按鈕交互設計

- 主題切換按鈕圖標應顯示"點擊後的去向"：深色模式顯示 ☀（點它回淺色）、
淺色模式顯示 ☾（點它轉深色）。
- 切換後同步更新按鈕文字，否則用戶不知道當前狀態。

## 6. 驗證方法論

- **渲染探針**：直接調 renderThemeQss + 變量表，斷言輸出包含/不包含關鍵色值：
`LIGHT has #f3f3f3: true`、`LIGHT terminal dark: false`。
- **運行日誌**：`[SCK] theme=fluent-light qss len=11085`，stderr 無 parse 錯誤。
- 注意：前景文字色（color:）和背景色（background:）都要檢查，
搜索"殘留色值"時先確認是 fg 還是 bg。

