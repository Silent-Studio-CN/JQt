# Changelog

<details>
<summary>🌐 语言 / Language</summary>

- [中文版](#zh) · [English Version](#en)

</details>

---

<a id="zh"></a>
## 中文版

### v0.1.0-alpha（2026-08-25）— 首个 Alpha

**里程碑**：Phase 0-6 全部完成，三平台 CI 构建全绿。

**功能**：
- 控件：JQtWindow / JQtButton / JQtLabel / JQtLineEdit / JQtComboBox / JQtListWidget
- 信号槽（伪信号槽，全部可多监听器注册）：
  点击 / 按下 / 释放 / 勾选切换 / 文本变化 / 回车 / 选项切换 / 列表点击 / 窗口关闭 / 尺寸变化 / 位置变化 / 退出前回调
- 布局：JQtVBoxLayout / JQtHBoxLayout（间距、弹性空间）
- 应用：exec / quit / schedule（Qt 定时器 → Java 回调）/ scheduleQuit
- 样式：QSS 全局样式表（JQtApplication.setStyleSheet）、控件级 QSS（JQtWidget.setStyleSheet）、
  风格切换（setStyle("Fusion") 等）
- 内存管理：句柄注册表（自增 ID）、Qt destroyed 同步注销、所有权模型（Qt vs Java Cleaner）、
  GUI 线程安全回收、悬垂保护（IllegalStateException）
- 异常处理：未创建 QApplication 保护、JNI 异常清理

**构建**：
- Windows（本机）：Qt 6.11.2 + MinGW 13.1，产物 jqt.dll
- CI 三平台：Linux（apt Qt 6.8.3）/ Windows（Qt 6.8.3 + MinGW）/ macOS（Qt 6.8.3 clang_64），
  产物 libjqt.so / jqt.dll / libjqt.dylib，offscreen 冒烟测试全过

**许可**：JQt Source License v1.0（JSL-1.0），分层授权（详见 LICENSE.md）

**已知限制**：
- 控件集较小（Phase 4 完成度），菜单/树/滚动区等按 docs/api-tiering.md 第二批发货
- CI 使用 Qt 6.8.3 LTS（Qt 官方 Windows 6.11.2 在线仓库元数据 404）

---

<a id="en"></a>
## English Version

### v0.1.0-alpha (2026-08-25) — First Alpha

**Milestone**: Phases 0-6 complete; three-platform CI builds green.

**Features**:
- Widgets: JQtWindow / JQtButton / JQtLabel / JQtLineEdit / JQtComboBox / JQtListWidget
- Signals (pseudo signal-slot, multi-listener):
  click / press / release / toggle / text changed / return pressed / index changed / item clicked /
  window close / resized / moved / about to quit
- Layouts: JQtVBoxLayout / JQtHBoxLayout (spacing, stretch)
- App: exec / quit / schedule (Qt timer to Java callback) / scheduleQuit
- Memory: handle registry (incrementing IDs), Qt destroyed sync, ownership model (Qt vs Java Cleaner),
  GUI-thread-safe disposal, dangling guard (IllegalStateException)
- Errors: missing-QApplication guard, JNI exception hygiene

**Builds**:
- Windows (local): Qt 6.11.2 + MinGW 13.1, artifact jqt.dll
- CI matrix: Linux (apt Qt 6.8.3) / Windows (Qt 6.8.3 + MinGW) / macOS (Qt 6.8.3 clang_64),
  artifacts libjqt.so / jqt.dll / libjqt.dylib; offscreen smoke tests pass

**License**: JQt Source License v1.0 (JSL-1.0), tiered (see LICENSE.md)

**Known limits**:
- Widget set is small (Phase 4 scope); menus/tree/scroll area ship in the second batch per docs/api-tiering.md
- CI uses Qt 6.8.3 LTS (Qt's official Windows 6.11.2 online repo metadata is 404)
