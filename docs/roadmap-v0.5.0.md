# JQt v0.5.0-test 路线图

> 版本命名：v0.5.0-test（v0.4.1-alpha 之后，后缀 alpha → test）
> 范围：十项全收（用户全选）+ 渲染适配（用户新增），共 11 项，分 5 批。

## 背景

- 当前基线：v0.4.1-alpha（17 个 Q 前缀类 + 7 个 JQt 原创类，约 220 方法，双 Qt 6.8.3/6.11.2，ARM64 CI 全绿）
- 网站 jqt.silentstudio.cn 由构建方 AI 维护（7 语言文档站重建中）

## 批次划分

### 批 1：控件海啸（纯新控件批量生产）

1. **对话框家族**：QFileDialog / QColorDialog / QFontDialog / QInputDialog + QMessageBox 增强
2. **数据表格/树**：QTableWidget / QTreeWidget
3. **选项卡/分组/分割**：QTabWidget / QGroupBox / QStackedLayout / QSplitter
4. **输入控件补齐**：QSpinBox / QDial / QRadioButton / QDateTimeEdit 等
5. **布局升级**：QGridLayout / QFormLayout

### 批 2：窗口体系

6. **菜单/工具栏/状态栏/托盘**：QMenu / QToolBar / QStatusBar / QSystemTrayIcon
7. **富文本编辑**：QTextEdit / QTextBrowser / QTextDocument

### 批 3：渲染适配（RHI 三后端）

8. **渲染后端可选**：
   - `QApplication.rhiBackend(String)` 静态配置（构造前调用，native 侧 qputenv：QSG_RHI_BACKEND + QT_WIDGETS_RHI=1）
   - 命令行 `--rhi=d3d11|opengl|vulkan|software` 透传
   - Windows 默认 D3D11（最稳）；OpenGL 兼容性兜底；Vulkan 实验性
   - **三后端冒烟矩阵**：FluentDemo / QfDemo / JQtGallery × {d3d11, opengl, vulkan}，截图对比；CI 无 GPU 用 software + llvmpipe 兜底，真显卡人工验证
   - 自绘控件（JQtSwitch 等）重点验证渐变/抗锯齿差异

### 批 4：能力

9. **自绘画布**：暴露 QPainter API（画笔/画刷/路径/渐变/变换），支持自定义控件绘制
10. **多线程/异步**：QThread 绑定、非阻塞任务、异步回调回主线程

### 批 5：稳定发布

11. **稳定性/性能**：内存/GC 优化、崩溃恢复、双 Qt（6.8.3 LTS + 6.11.2）CI 持续维护、ARM64 持续

## 发布物

- v0.5.0-test 发布：jar + Windows x64 完整包 + 双 Qt 裸库（x64/arm64/linux/macos）
- API 文档同步：api-implemented.md / qt-mapping.md 增量更新
- 版本标注：VERSION / README / CHANGELOG / RELEASE_NOTES 统一 v0.5.0-test
