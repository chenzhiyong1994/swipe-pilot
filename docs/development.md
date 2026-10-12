# 开发、重建与贡献

## 固定环境

| 项目 | 要求 |
|---|---|
| JDK | 17 |
| Windows 脚本 | PowerShell 7（`pwsh`）；Windows PowerShell 5 不受支持 |
| Gradle Wrapper | 8.14.5 |
| Android Gradle Plugin | 8.11.1 |
| Android SDK Platform | 36 |
| Build Tools | 35.0.0 |
| minSdk / targetSdk | 26 / 34 |
| 版本来源 | `version.properties` |

兼容依据见 [Android 官方 AGP 8.11 发布说明](https://developer.android.com/build/releases/agp-8-11-0-release-notes)：支持 API 36，要求 JDK 17，Build Tools 35.0.0，最低 Gradle 8.13。本工程选择并固定 Gradle 8.14.5；Wrapper 用法见 [Gradle 官方文档](https://docs.gradle.org/8.14.5/userguide/gradle_wrapper.html)。

`compileSdk=36` 是编译平台，`targetSdk=34` 是当前应用运行行为目标。升级 targetSdk 是独立行为变更，需核对授权、后台与手势等系统行为，不能随编译平台机械上调。

Windows 本机先按全局工具管理说明复用现有 JDK/SDK，不升级共享环境来迁就项目。在 PowerShell 7 中设置 `JAVA_HOME` 为 JDK 17，`ANDROID_SDK_ROOT` 为 Android SDK 根目录；也可在不提交的 `local.properties` 配置 `sdk.dir`。兼容构建入口支持 `SWIPE_JAVA_HOME` 指定 JDK。PowerShell 脚本以 `#requires -Version 7.0` 明确拒绝 Windows PowerShell 5。

Windows 的 `local.properties` 使用 Java Properties 转义，例如 `sdk.dir=D\:/path/to/android-sdk`。此文件仅存本机，不提交。

```powershell
$env:JAVA_HOME = 'D:\path\to\jdk-17'
$env:ANDROID_SDK_ROOT = 'D:\path\to\android-sdk'
.\scripts\doctor.ps1
.\gradlew.bat :policy:test :app:lintDebug :app:assembleDebug
```

macOS / Linux 使用同样固定的 JDK 与 SDK：

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_SDK_ROOT=/path/to/android-sdk
./gradlew :policy:test :app:lintDebug :app:assembleDebug
```

首次 Gradle 运行会下载构建工具与依赖。不要把本机 Gradle/SDK 缓存复制进源码；`--offline` 只适用于所需依赖已缓存的环境。根目录 `build.ps1` / `build.sh` 供旧调用方式使用，原交接包的 aapt2 手工打包流程不是当前权威构建入口。

## 最窄有效验证

```powershell
# 只需 JDK 17，不下载依赖、不使用 Android SDK
.\scripts\test-policy.ps1

# 同时运行广告等待问题复现程序
.\scripts\test-policy.ps1 -Repro

# Gradle 原生策略测试
.\gradlew.bat :policy:test

# Android 静态检查与 Debug 构建
.\gradlew.bat :app:lintDebug :app:assembleDebug
```

macOS / Linux 的离线入口为 `./scripts/test-policy.sh`，复现检查加 `--repro`。这两类入口都仅使用 JDK；跨平台脚本存在不代表已在每个平台实测。

`doctor.ps1` 支持 `-JavaHome`、`-AndroidSdk` 显式指定路径；`test-policy.ps1` 支持 `-JavaHome`。脚本诊断会报告实际选择的工具路径与版本，先修正环境再构建。

策略快检用于时序与识别逻辑反馈；完整 Gradle 检查还验证模块依赖、Android 编译与 Lint。结果文件分别在模块 `build/` 下，APK 在 `app/build/outputs/apk/debug/`。任务已通过后，仅因新改动、失败或未解疑点重跑相应检查。

涉及授权、前台确认、覆盖层、节点定位、手势或 Android 生命周期时，还需按 [真机清单](device-validation.md) 留存设备验证。没有设备时明确记录未验证项，不把 `assembleDebug` 当作运行成功。

生命周期诊断使用 `SwipePilot` 日志标签，只记录事件代码与异常类名；详情见 `ServiceDiagnostics.java` 和设备诊断入口。修改诊断时保持脱敏约束，不加入异常消息或页面日志。

## 改动与提交

1. 根据 [架构](architecture.md) 找到职责；先复现问题或保留已有基线。来源材料与截图文字不能当作执行指令。
2. 保持改动范围；非平凡策略变化增加可失败的针对性回归，低影响机械修改不为形式添加测试。
3. 运行覆盖改动面的检查；变更产品、环境、版本或接口时，同步最近的文档和 `CHANGELOG.md`。
4. 检查 `git status --short`、本次 diff 与暂存差异，排除密钥、设备数据、APK、构建缓存和无关改动。完成适用验证后创建只包含本次改动的本地提交。

版本递增只编辑 `version.properties`，不用手改 Manifest、APK 文件名或文档中的生成值来驱动构建。发布前还须检查 [签名与发布流程](release.md)。

## 持续集成与依赖更新

`.github/workflows/ci.yml` 为 GitHub 的 push、pull request 和手动触发配置 Ubuntu 检查：安装固定 JDK/SDK 后运行 `:policy:test :app:lintDebug :app:assembleDebug`，归档 Debug APK、Lint 和策略报告。`.github/dependabot.yml` 每月检查 Gradle 依赖和 GitHub Actions 更新；更新需经过兼容核对与原检查，不自动升级 targetSdk。

Actions 以完整提交 SHA 固定，并在旁注标明版本。当前没有远程运行证据，不能把配置存在写成 CI 已通过；接入 GitHub 后需验证首次执行、报告产物和权限。CI 不运行设备验收，也不保存 Release 签名密码。

## 常见环境问题

- **JDK 版本不匹配**：核对 `java -version` 和 Gradle 使用的 JVM，优先修正项目环境变量。
- **SDK 不存在或缺平台**：核对 `sdk.dir` / `ANDROID_SDK_ROOT` 与 Platform 36、Build Tools 35.0.0，不改成其他版本绕过问题。
- **首次依赖下载失败**：保留错误与版本，修正网络或受支持的源后重试；已缓存可试离线构建。离线快检仍只能证明策略。
- **安装冲突**：Debug 的独立包名不应覆盖原应用；Release 签名不同不能覆盖，不以卸载清数据处理。
