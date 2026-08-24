# Third-Party Notices（第三方组件声明）

JQt 的构建产物 `lib/` 包含以下第三方组件，使用时须遵守其各自许可条款。

## Qt 6 — LGPLv3

**版权**：The Qt Company 及其贡献者（Copyright (C) The Qt Company Ltd and other contributors）。

**许可**：Qt 以 LGPLv3（`LGPL-3.0.txt`）或商业许可提供，详见
https://www.qt.io/licensing

**本仓库 `lib/` 中包含的 Qt 组件**：

| 组件 | 说明 |
|------|------|
| Qt6Core.dll / Qt6Gui.dll / Qt6Widgets.dll / Qt6Network.dll / Qt6Svg.dll | Qt 运行库（动态链接） |
| platforms/qwindows.dll | Windows 平台插件 |
| imageformats/、styles/、iconengines/ 等 | 图像格式 / 样式 / 图标插件 |

**LGPLv3 合规说明**：

1. **动态链接**：JQt 通过 `jqt.dll` 动态链接 Qt6*.dll，满足 LGPLv3 的
   可再链接（relink）要求，用户应用无需以 LGPLv3 开源；
2. **许可声明**：分发 `lib/` 时必须一并保留 `LGPL-3.0.txt` 与本声明；
3. **无附加限制**：不得对 Qt 组件施加额外技术限制（LGPLv3 第 3 条）；
4. Qt 源码获取：https://code.qt.io 或 https://www.qt.io/download-open-source

## 其他

JQt 自身（Java 源码、C++ 胶水层）的许可见 `LICENSE`（JQt Source License v1.0）。
