# WebView2 SDK（随仓库携带，构建 JQt 的 Windows 自研 WebView 宿主用）

## 为什么这里有这个目录

官方 Qt 6.12 的 `qtwebview` 包（MinGW 与 MSVC 两个版本都实测过）**只包含**
`Qt6WebView.dll` / `Qt6WebViewQuick.dll` / 头文件 / QML 模块，**没有 `plugins/webview/` 后端插件**。
后果：实例化 QML 的 `WebView` 元素时 Qt 直接 `qFatal("No WebView plug-in found!")`，
进程 fail-fast（实测退出码 `0xC0000409`）。

所以 Windows 上 `QWebView` 不再走 QtWebView，改为 JQt 自己写的 **WebView2(COM) 宿主**
（见 `native/jqt_bridge.cpp` 里的 `jqtwv2` 命名空间）。

## 内容与来源

| 文件 | 来源 | 说明 |
| --- | --- | --- |
| `include/WebView2.h` | NuGet 包 `Microsoft.Web.WebView2` **1.0.2903.40** → `build/native/include/WebView2.h` | 官方 SDK 头（2.5 MB，MIDL 生成） |
| `x64/WebView2Loader.dll` | 同一个包 → `runtimes/win-x64/native/WebView2Loader.dll` | PE machine = `0x8664`（x64） |
| `LICENSE.txt` / `NOTICE.txt` | 同一个包根目录 | Microsoft 的再分发条款，勿删 |
| `Microsoft.Web.WebView2.nuspec` | 同一个包根目录 | 版本溯源用 |

获取方式（本机实际执行的命令）：

```powershell
Invoke-WebRequest -Uri 'https://www.nuget.org/api/v2/package/Microsoft.Web.WebView2/1.0.2903.40' `
                  -OutFile "$env:TEMP\wv2.zip"
Expand-Archive "$env:TEMP\wv2.zip" -DestinationPath "$env:TEMP\wv2sdk"
```

> 沙箱封了 `github.com`（WebView2 SDK 的另一个常见来源），但 `nuget.org` 可用 —— 所以走 NuGet。

## 关键结论（实测，不是推测）

1. **`WebView2.h` 能被 MinGW g++ 13.1.0 直接编译**（`-std=c++17`）。
   它里面的 IID 全部是 `EXTERN_C __declspec(selectany) const IID IID_ICoreWebView2... = {...};`
   —— **自带定义**，所以**不需要链接任何 `.lib`**，也就绕开了 MSVC 专有的
   `WebView2LoaderStatic.lib` / `WebView2Loader.dll.lib`（MinGW 的 `ld` 吃不下 MSVC 的 import lib）。
   因此我们只 include 头文件，vtable 顺序由官方头保证，不用手写接口声明。
2. **`WebView2Loader.dll` 用 `LoadLibrary` + `GetProcAddress` 动态加载**
   （只用到 `CreateCoreWebView2EnvironmentWithOptions` 与
   `GetAvailableCoreWebView2BrowserVersionString` 两个导出），
   所以 `jqt.dll` 对它没有硬链接依赖 —— 没装运行时的机器上 `jqt.dll` 照样能加载，
   只是 `QWebView.isBackendAvailable()` 返回 `false`。
3. 构建脚本把 `x64/WebView2Loader.dll` 拷到 `lib/`；运行期优先从 `jqt.dll` 同目录加载，
   加载失败再退回系统搜索路径（兼容"机器上已装 VS / 其它应用带了 loader"的情况）。

## 注意

- 这个目录**只是构建期依赖**，里面的 `WebView2Loader.dll` 必须在运行期能被找到
  （`build.ps1` 会拷进 `lib/`）。
- 运行期真正渲染网页的是**系统里的 WebView2 运行时**（Edge WebView2 Runtime / Microsoft Edge），
  不是这里的任何文件。运行时缺失时 `QWebView.isBackendAvailable()` 会如实返回 `false`。
