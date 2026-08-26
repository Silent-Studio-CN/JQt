# JQt Gallery —— 全功能演示

主题换肤 / 控件 / 动画 / 窗口 四大分区，全部可点击体验。

## 功能

- **主题**：5 套主题（Nord / Solarized / Terminal / 官方暗 / 官方浅）+ 4 种强调色 + 自动跟随系统深浅色 + 动画节奏
- **控件**：Switch 开关 / 复选框 / 输入框 / 下拉框 / 列表 / 禁用 / 悬垂保护演示
- **动画**：窗口淡入淡出 / 卡片缩放（5 种缓动）/ 投影 / 圆角
- **窗口**：毛玻璃 / 圆角 / 无边框 / 最小化 / 最大化 / 几何信息实时显示

## 运行

```powershell
.\run.ps1 -Class JQtGallery        # 项目模式（需先 build.ps1）
```

原版还提供了 jpackage 打包的免安装 exe（捆绑 JRE + Qt 运行时）——
产物不入库，构建方式：`jpackage --input lib --main-jar gallery.jar --main-class JQtGallery`。

## 源码说明

本文件由打包产物 gallery.jar 反编译恢复（CFR 0.152），逻辑完整可编译运行，
但变量名已泛化——欢迎原作者提供原始源码替换。
