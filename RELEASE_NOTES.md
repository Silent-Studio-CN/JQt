# JQt v0.4.0-alpha

JQt 第四个 Alpha —— 新控件六件套 / Windows ARM64 / qf light 皮肤。

## 发布包

| 资产 | 说明 |
|------|------|
| `jqt-0.4.0-alpha.jar` | Java API（平台无关） |
| `jqt-0.4.0-alpha-windows-x64.zip` | Windows x64 完整包 |
| `jqt-windows-arm64-6.8.3.dll` | Windows ARM64 动态库 |
| `jqt-windows-6.8.3.dll` | Windows x64（Qt 6.8.3） |
| `libjqt-linux-6.11.2.so` / `libjqt-linux-6.8.3.so` | Linux |
| `libjqt-macos-6.11.2.dylib` / `libjqt-macos-6.8.3.dylib` | macOS |

## 本版亮点

- **新控件六件套**：JQtSlider / JQtScrollArea / JQtProgressBar / JQtNavigation / JQtMessageBox / JQtInfoBar
- **Windows ARM64** 恢复（原生 runner + MSVC 构建）
- **qf light 皮肤**（34 个 QSS 文件映射）

## 快速上手

```powershell
git clone https://github.com/Silent-Studio-CN/JQt.git
cd JQt
.\build.ps1
.\run-fluent.ps1                 # Fluent 演示（含 v0.4 新控件卡片）
```

## 许可

- JQt：JQt Source License v1.0（JSL-1.0）（LICENSE.md）
- Qt 运行时：LGPLv3（LGPL-3.0.txt）
- themes/qf/：qfluentwidgets（GPLv3）QSS，仅测试引用不随发布包分发
