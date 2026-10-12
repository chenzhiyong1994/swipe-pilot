# 轻滑助手 / SwipePilot

面向老年人的本地 Android 自动翻页工具：大字、大点击区域，选择应用后即可启动，侧边悬浮窗可暂停、停止和切换模式。支持 Android 8.0（API 26）及以上。

项目从 `SwipePilot-handoff-20261011.zip` 的 1.3.2 源码恢复为可重复构建、可测试的迭代工程。当前维护版本为 **1.3.3 / versionCode 9**，版本由 `version.properties` 统一管理。它沿用原产品行为，本次工程整理不表示已解决小米无障碍开关被系统关闭、或已验证真实淘宝弹窗。

## 产品做什么

- **计时翻页**：默认每次随机等待 15–20 秒后上滑；识别到明确广告倒计时则等待，手动广告等待默认 15 秒。
- **广告返回**：正常播放时不定时上滑；只有先看到“即将播放广告”、后看到“已获得奖励”的链路成立，才尝试通过最近任务返回原应用。不固定游戏名称，不自动点播放。
- 普通离开目标应用立即暂停，2 分钟内回来可继续；离开超过 2 分钟、锁屏超过 30 秒结束会话。默认不限连续使用时长，定时停止为可选设置。
- 淘宝“睡前小福利”弹窗首次匹配后暂停翻页，等待 10 秒并重新确认关闭节点。位置或节点不明确时保持暂停，让用户手动关闭。

应用只读取当前页面的无障碍节点并执行手势，无网络权限、不上传屏幕内容、不做 OCR。详细操作和权限说明见 [使用指南](docs/user-guide.md)。

## 开发与验证

需要 JDK 17、Android SDK Platform 36 和 Build Tools 35.0.0。项目固定 Gradle 8.14.5 / Android Gradle Plugin 8.11.1，`minSdk=26`、`targetSdk=34`。首次 Gradle 构建需要下载构建依赖。

Windows 需要 PowerShell 7（`pwsh`）：

```powershell
.\scripts\doctor.ps1
.\gradlew.bat :policy:test :app:lintDebug :app:assembleDebug
```

macOS / Linux：

```bash
./gradlew :policy:test :app:lintDebug :app:assembleDebug
```

只检查策略、不使用 Android SDK 或网络时，Windows 运行 `.\scripts\test-policy.ps1`，macOS / Linux 运行 `./scripts/test-policy.sh`。根目录 `build.ps1` / `build.sh` 为兼容构建入口，完整环境配置见 [开发指南](docs/development.md)。

Debug APK 位于 `app/build/outputs/apk/debug/`，名称为“轻滑助手·开发版”，包名为 `com.local.swipepilot.debug`，可与原应用并存，但设置和无障碍授权独立。Release 包名为 `com.local.swipepilot`；交接包没有原签名私钥，使用新的密钥无法覆盖原安装。见 [发布与签名](docs/release.md)。

## 维护入口

| 资料 | 用途 |
|---|---|
| [架构](docs/architecture.md) | `:policy` 纯 Java 策略与 `:app` Android 适配层的职责 |
| [真机验收](docs/device-validation.md) | 两种模式、会话边界、弹窗和授权故障的验证清单 |
| [本次验证](docs/validation/2026-10-12.md) | 当前工程的实际验证结果与未验证项 |
| [路线与待办](docs/roadmap.md) | 已知问题、下一步证据和完成判据 |
| [来源追溯](docs/provenance.md) | 交接材料、历史证据和敏感材料的存放边界 |
| [变更记录](CHANGELOG.md) | 面向维护者的版本变化 |
| [AGENTS.md](AGENTS.md) | 项目范围与 Agent 工作入口 |

提交改动前运行覆盖改动面的策略测试、Lint 和构建；影响无障碍、窗口、手势或系统生命周期的改动必须补真机证据。纯策略测试和内置练习页不能代替真实第三方应用验证。

仓库已提供 GitHub Actions 检查与 Dependabot 更新配置；本次仅本地验证，尚未在 GitHub 远程运行。
