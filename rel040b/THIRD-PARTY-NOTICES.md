# Third-Party Notices（第三方组件声明）

<details>
<summary>🌐 语言 / Language</summary>

- [中文版](#zh) · [English Version](#en)

</details>

---

<a id="zh"></a>
## 中文版

JQt 的构建产物 `lib/` 包含以下第三方组件，使用时须遵守其各自许可条款。

### Qt 6 — LGPLv3

**版权**：The Qt Company 及其贡献者（Copyright (C) The Qt Company Ltd and other contributors）。

**许可**：Qt 以 LGPLv3（`LGPL-3.0.txt`）或商业许可提供，详见 https://www.qt.io/licensing

**本仓库 `lib/` 中包含的 Qt 组件**：

| 组件 | 说明 |
|------|------|
| Qt6Core.dll / Qt6Gui.dll / Qt6Widgets.dll / Qt6Network.dll / Qt6Svg.dll | Qt 运行库（动态链接） |
| platforms/qwindows.dll | Windows 平台插件 |
| imageformats/、styles/、iconengines/ 等 | 图像格式 / 样式 / 图标插件 |

**LGPLv3 合规说明**：

1. **动态链接**：JQt 通过 `jqt.dll` 动态链接 Qt6*.dll，满足 LGPLv3 的可再链接（relink）要求，用户应用无需以 LGPLv3 开源；
2. **许可声明**：分发 `lib/` 时必须一并保留 `LGPL-3.0.txt` 与本声明；
3. **无附加限制**：不得对 Qt 组件施加额外技术限制（LGPLv3 第 3 条）；
4. Qt 源码获取：https://code.qt.io 或 https://www.qt.io/download-open-source

### 其他

JQt 自身（Java 源码、C++ 胶水层）的许可见 `LICENSE.md`（JQt Source License v1.0）。

## 设计参考声明（Design References）

JQt 的 Fluent 风格窗口能力（无边框、亚克力、圆角、缩放热区）与 Fluent 视觉主题，
在**实现思路上**参考了以下开源项目。**JQt 未复制其任何代码或资源**（二者均为 GPLv3
许可，为避免 GPL 传染与 JSL-1.0 冲突，JQt 仅参考公开的 Win32 API 用法与微软
Fluent Design 公开设计规范，代码为独立编写）：

| 项目 | 作者 | 许可 | 参考内容 |
|------|------|------|---------|
| [PyQt-Fluent-Widgets (qfluentwidgets)](https://github.com/zhiyiYo/PyQt-Fluent-Widgets) | zhiyiYo | GPLv3 / 商业 | Fluent Design 视觉风格、控件配色与布局思路 |
| [PyQt-Frameless-Window (qframelesswindow)](https://github.com/zhiyiYo/PyQt-Frameless-Window) | zhiyiYo | GPLv3 / 商业 | 无边框窗口实现思路（WM_NCHITTEST 缩放热区、DWM 阴影、SetWindowCompositionAttribute 亚克力） |

**English**: JQt's Fluent-style window capabilities and visual theme reference the
*implementation ideas* of the projects above. JQt copies **no code or assets** from them
(both are GPLv3; to avoid GPL contamination conflicting with JSL-1.0, JQt only uses public
Win32 API knowledge and Microsoft's public Fluent Design guidelines, with independently
written code).

### qfluentwidgets QSS 皮肤（测试引用，不随 JQt 分发）

`themes/qf/` 目录下的 QSS 文件从 [PyQt-Fluent-Widgets](https://github.com/zhiyiYo/PyQt-Fluent-Widgets)
（作者 zhiyiYo，GPLv3 / 商业许可）的 `qfluentwidgets/_rc/qss/dark/` 完整下载合并而来，
**仅用于本地测试**「JQt 作为 QSS 引擎可渲染任意第三方皮肤」的能力验证，**不包含在任何
JQt 发布包中**。`qf-dark-jqt.qss` 为测试用途做了类名映射（qf 自定义类名 → JQt 实际
控件类名）与主题变量（`--ThemeColor*`）内联替换。

使用这些文件需自行遵守其 GPLv3 许可。**与 JQt 的边界**：QSS 是数据而非代码——
JQt 只负责解析渲染，用户在自己的应用里导入任何第三方 QSS 皮肤（含 GPL 皮肤），
责任与合规由用户自行承担（与导入字体、图标资源同理）。

---

<a id="en"></a>
## English Version

The build artifacts of JQt under `lib/` contain the following third-party components. Their respective license terms must be complied with when used.

### Qt 6 — LGPLv3

**Copyright**: The Qt Company and its contributors (Copyright (C) The Qt Company Ltd and other contributors).

**License**: Qt is available under LGPLv3 (`LGPL-3.0.txt`) or a commercial license. See https://www.qt.io/licensing

**Qt components included in `lib/`**:

| Component | Description |
|-----------|-------------|
| Qt6Core.dll / Qt6Gui.dll / Qt6Widgets.dll / Qt6Network.dll / Qt6Svg.dll | Qt runtime libraries (dynamic linking) |
| platforms/qwindows.dll | Windows platform plugin |
| imageformats/, styles/, iconengines/, etc. | Image format / style / icon plugins |

**LGPLv3 compliance notes**:

1. **Dynamic linking**: JQt links Qt6*.dll dynamically via `jqt.dll`, satisfying LGPLv3's relinkability requirement; user applications are not required to be open source under LGPLv3;
2. **License notices**: when distributing `lib/`, `LGPL-3.0.txt` and this notice must be retained;
3. **No additional restrictions**: no additional technological restrictions may be imposed on Qt components (LGPLv3 Section 3);
4. Qt source code: https://code.qt.io or https://www.qt.io/download-open-source

### Other

The licensing of JQt itself (Java sources, C++ bridge) is governed by `LICENSE.md` (JQt Source License v1.0).
