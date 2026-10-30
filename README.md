# 轻滑助手 / SwipePilot

面向老年人的开源 Android 自动翻页工具。大字、大点击区域，选择应用后即可启动；通过侧边悬浮窗暂停、停止或切换模式。支持 Android 8.0（API 26）及以上。

## 下载安装

**[下载 Android 安装包 · v1.3.3](https://github.com/chenzhiyong1994/swipe-pilot/releases/latest/download/SwipePilot-1.3.3.apk)** · [发布说明与校验信息](https://github.com/chenzhiyong1994/swipe-pilot/releases/latest)

1. 在手机上下载 APK，按系统提示允许本次安装，然后打开“轻滑助手”。
2. 点击首页右上角的无障碍入口，在系统设置中开启“轻滑助手”，返回后确认状态为绿色。
3. 选择要使用的应用，点击“保存并启动”。点击侧边圆钮可展开控制面板，随时暂停或停止。

下载包为正式 Release 版本，应用名“轻滑助手”，包名 `com.local.swipepilot`。详细操作见 [使用指南](docs/user-guide.md)。

## 功能

- **计时翻页**：默认每次随机等待 15–20 秒后上滑；识别到明确广告倒计时则等待，也可手动等待，默认 15 秒。
- **广告返回**：正常播放时不定时上滑；先识别“即将播放广告”，后识别“已获得奖励”，再尝试通过最近任务返回目标应用。不固定游戏名称，不自动点播放。
- **会话管理**：普通离开目标应用立即暂停，2 分钟内回来可继续；离开超过 2 分钟、锁屏超过 30 秒结束会话。默认不限连续使用时长，定时停止可选。
- **弹窗处理**：淘宝“睡前小福利”弹窗匹配后暂停翻页，等待 10 秒并重新确认关闭节点；无法明确定位时提示手动关闭。

应用只读取当前页面的无障碍节点并执行手势，无网络权限、不上传屏幕内容、不做 OCR。图片或视频中没有可读取节点的文字可能无法识别；真实淘宝弹窗兼容性及部分系统的无障碍连接稳定性仍待验证。普通切换不会被强行拉回。

## 构建与贡献

需要 JDK 17、Android SDK Platform 36 和 Build Tools 35.0.0。项目固定 Gradle 8.14.5 / Android Gradle Plugin 8.11.1，`minSdk=26`、`targetSdk=34`；版本统一配置在 `version.properties`。

Windows 使用 PowerShell 7：

```powershell
.\scripts\doctor.ps1
.\gradlew.bat :policy:test :app:lintDebug :app:assembleDebug
```

macOS / Linux：

```bash
./gradlew :policy:test :app:lintDebug :app:assembleDebug
```

首次构建需要下载依赖。仅检查策略时可运行 `scripts/test-policy.ps1` 或 `scripts/test-policy.sh`，只需 JDK。开发变体包名为 `com.local.swipepilot.debug`，与正式版的设置和授权独立；面向用户的安装包使用签名 Release 构建。

贡献前按改动范围运行必要检查，并同步相关文档。问题反馈请附应用版本、手机与系统版本、复现步骤和脱敏证据；涉及节点、手势或系统生命周期的行为需在实际设备上验证。

- [开发指南](docs/development.md) · [架构](docs/architecture.md) · [发布与签名](docs/release.md)
- [真机验收](docs/device-validation.md) · [验证记录](docs/validation/2026-10-12.md) · [后续路线](docs/roadmap.md) · [变更记录](CHANGELOG.md)
