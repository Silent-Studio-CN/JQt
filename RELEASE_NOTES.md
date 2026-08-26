# JQt v0.3.0-alpha

JQt 第三个 Alpha —— 触摸实战 / 主题系统 / 基础 API 补齐。

## 发布包

| 资产 | 说明 |
|------|------|
| `jqt-0.3.0-alpha.jar` | Java API（平台无关） |
| `jqt-0.3.0-alpha-windows-x64.zip` | Windows x64 完整包（jar + jqt.dll + Qt 6.11.2 运行时） |
| `jqt-windows-6.8.3.dll` | Windows x64（Qt 6.8.3） |
| `libjqt-linux-6.11.2.so` / `libjqt-linux-6.8.3.so` | Linux 动态库 |
| `libjqt-macos-6.11.2.dylib` / `libjqt-macos-6.8.3.dylib` | macOS 动态库 |

## 本版亮点

- **触摸全链路**（HiteVision 一体机实战）：POINTER 合成、系统级标题栏拖动、触摸键盘、弹层可点
- **主题系统**：setAccentColor 主题色 / setAutoTheme 自动跟随系统 / setFontFamily 中文字体
- **动画**：JQtAnimations 动效库、JQtPivot 选项卡、JQtAnimationTheme 节奏预设
- **样式**：setDropShadow / setBorderRadius / 布局 margins
- **基础 API**：几何查询、显隐、禁用、固定尺寸、读文本

## 快速上手

```powershell
git clone https://github.com/Silent-Studio-CN/JQt.git
cd JQt
.\build.ps1
.\run-fluent.ps1                 # Fluent 演示（触摸 / 动画 / 主题）
.\run.ps1 -Class org.jqt.JQtQfDemo   # qfluentwidgets 皮肤演示
```

## 许可

- JQt：JQt Source License v1.0（JSL-1.0）分层授权（LICENSE.md）
- Qt 运行时：LGPLv3（LGPL-3.0.txt，动态链接合规）
- themes/qf/：qfluentwidgets（GPLv3）QSS，仅测试引用不随发布包分发

## 已知限制

- Windows ARM64 支持规划在 v0.4（build-arm64.ps1 已就绪，待 CI 启用）
- 控件集仍为 Phase 4 规模（菜单/树/滚动区在后续版本发布，见 docs/api-tiering.md）
