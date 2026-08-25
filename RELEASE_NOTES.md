# JQt v0.1.0-alpha

JQt 首个 Alpha 发布 —— Java 绑定 Qt 框架。Phase 0-6 全部完成，三平台 CI 全绿。

## 发布包（Qt 6.8.3 + 6.11.2 双版本）

| 资产 | 说明 |
|------|------|
| `jqt-0.1.0-alpha.jar` | Java API（平台无关） |
| `jqt-windows-6.11.2-full.zip` | Windows 完整包（Qt 6.11.2 运行时，16MB） |
| `jqt-windows-6.8.3-full.zip` | Windows 完整包（Qt 6.8.3 运行时，14MB） |
| `libjqt-linux-6.11.2.so` / `libjqt-linux-6.8.3.so` | Linux 动态库 |
| `libjqt-macos-6.11.2.dylib` / `libjqt-macos-6.8.3.dylib` | macOS 动态库 |

## 功能

- **控件**：窗口 / 按钮 / 标签 / 输入框 / 下拉框 / 列表
- **信号槽**（伪信号槽，多监听器）：点击 / 按下 / 释放 / 勾选切换 / 文本变化 / 回车 / 选项切换 / 列表点击 / 窗口关闭 / 尺寸变化 / 位置变化 / 退出前回调
- **布局**：VBox / HBox（间距、弹性空间）
- **内存管理**：句柄注册表 + Cleaner + 悬垂保护（IllegalStateException）
- **跨平台**：Windows / Linux / macOS 三平台 CI 自动构建

## 快速上手

```java
import org.jqt.*;

public class Hello {
    public static void main(String[] args) {
        JQtApplication app = new JQtApplication();
        JQtWindow window = new JQtWindow("Hello JQt", 640, 480);
        JQtButton button = new JQtButton("点我");
        button.onClick(() -> System.out.println("clicked!"));
        JQtVBoxLayout vbox = new JQtVBoxLayout();
        vbox.addWidget(button);
        window.setLayout(vbox);
        window.show();
        app.exec();
    }
}
```

```powershell
java -Djava.library.path=lib -cp jqt-0.1.0-alpha.jar Hello
```

## 许可

- JQt：JQt Source License v1.0（JSL-1.0）分层授权（LICENSE.md）
- Qt 运行时：LGPLv3（LGPL-3.0.txt，动态链接合规）

## 已知限制

- 控件集为 Phase 4 规模（菜单/树/滚动区在第二批发货，见 docs/api-tiering.md）
- Windows CI 使用 Qt 6.8.3（官方仓库缺 Windows 6.11.2 在线包；Windows 6.11.2 由本机构建提供）
