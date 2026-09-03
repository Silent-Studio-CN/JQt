# 发布流程（Release Process）

> 适用：每次发版（示例 v0.7.5-Generator-Kit → 下版 Emerge-Kit）。目标：资产一致、渠道同步、可审计。

## 0. 前置（一次性）

- 本地：Qt 6.11.2 + 6.8.3、MinGW 13.1、JDK 17+（编译用 --release 17，任何 ≥17 的 JDK 均可）
- 远程构建机（可选，Windows Server 2025）：用于原生 Windows 冒烟
- 凭据：`.signing/`（GPG 私钥、SONATYPE token、cli-1 token）——永不上库

## 1. 版本与内容

1. `VERSION` 写新版本号（如 `0.7.5-Generator-Kit`）
2. `CHANGELOG.md` 追加条目（中英）
3. `docs/releases/<version>.md` 新建发布说明（公开版，无内部记录）
4. jqt-gen 重新导出 `docs/api-implemented.md`（标题版本同步）
5. README/README.zh：Quick Start 资产名、Releases 表、Latest 链接、badge 更新

## 2. 构建与校验

```powershell
# 构建（Java 17 字节码；jar 只含 org/jqt API 类；zip 排除 .bak）
.\build.ps1          # javac --release 17 + g++ bridge + windeployqt
.\build-release.ps1  # jar + windows-x64 zip（自动过滤测试类/.bak）
# 校验（上传前必跑）
.\tools\release-check.ps1
#   断言：jar major=61 · 无 Smoke/Demo/Community 类 · zip 无 .bak/cred · VERSION 一致
```

> Linux/macOS 裸库由 CI 或对应平台脚本产出（build-linux.sh / build-macos.sh / build-arm64.ps1）。

## 3. 上传 GitHub Release

```powershell
gh release create <tag> dist\<资产>... --title "JQt <tag>" --notes "见 docs/releases/<version>.md"
# 更新已有 release 资产：
gh release upload <tag> <file> --clobber
```

- 资产集（Windows 为主）：jar + windows-x64.zip + 各平台裸库（命名见 README Releases 表）
- 上传前在本地解压抽查一次 zip（无 .bak / 无多余文件）

## 4. Maven Central（纯数字版本）

1. `tools/publish-central.ps1 -Version 0.7.5`（gradle 构建 → 签名 → Portal 上传 → 轮询 VALIDATED → publish）
2. 验证：repo1.maven.org 的 pom/jar/sources/javadoc HTTP 200；class major=61
3. Central artifact 不可变——发现错误只能发新版本，发布前必须过 release-check

## 5. JitPack

- tag 与 GitHub Release 同名（`v0.7.5-Generator-Kit`）；JitPack 自动从 tag 构建
- 验证：https://jitpack.io/com/github/Silent-Studio-CN/JQt/<tag>/ 构建状态

## 6. 发版后清单

- [ ] 三渠道坐标一致（Central 数字版 / GitHub+JitPack 代号版）
- [ ] 官网 jqt.silentstudio.cn 版本徽章/下载页同步（如有）
- [ ] 远程构建机 fetch 对齐 main（若有远程冒烟）
- [ ] Android 产物（如本期有）：APK 资产命名 + PoC 验证记录
