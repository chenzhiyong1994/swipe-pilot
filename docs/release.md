# 发布与签名

当前维护版为 1.3.3 / versionCode 9，由根目录 `version.properties` 提供。构建产物不是源码；历史 APK 仅作追溯，不能替代新工程生成的版本。

## Debug 与 Release

| 项目 | Debug | Release |
|---|---|---|
| 包名 | `com.local.swipepilot.debug` | `com.local.swipepilot` |
| 用途 | 开发与真机验证，可与旧版并存 | 本地正式分发 |
| 签名 | Gradle Debug 签名 | 显式提供外部私钥 |
| 设置/授权 | 与原应用独立 | 同签名覆盖时可保留原设置 |

交接包未包含原 `local-signing.jks`。新生成的私钥无法覆盖原签名安装，即使包名和版本相同也不行。需要覆盖旧应用时，必须通过私有方式获得原签名材料并核对证书。不能为安装成功而擅自卸载旧版或清数据，也不能宣称 Debug 能覆盖旧版。

## Release 配置

仅在本地环境或 CI Secret 中设置这四项，不写入仓库、日志或 Markdown：

| 环境变量 | 值 |
|---|---|
| `SWIPE_KEYSTORE` | 现有 keystore 的绝对路径 |
| `SWIPE_KEY_ALIAS` | 密钥别名 |
| `SWIPE_STORE_PASSWORD` | keystore 密码 |
| `SWIPE_KEY_PASSWORD` | 密钥密码 |

四项全部为空时，`assembleRelease` 仅生成 **unsigned Release APK**，不能安装或发布；只提供部分配置时构建会报告缺失项。配置不会生成替代密钥。用于安装或分发的 Release，四项必须完整有效。准备好签名环境后：

```powershell
.\gradlew.bat :policy:test :app:lintDebug :app:assembleDebug
.\gradlew.bat :app:assembleRelease
```

Release 输出在 `app/build/outputs/apk/release/`。先确认产物为已签名 APK，不能将 `*-unsigned.apk` 作为交付。按实际构建检查包名、versionName、versionCode、SDK 和签名证书，用 SDK 的 `apksigner verify --verbose --print-certs <apk>` 核实，再计算 SHA-256。签名校验只证明 APK 签名完整，不证明它与目标设备的旧安装同签名。

## 发布检查

1. 为版本变化更新 `version.properties` 和 `CHANGELOG.md`，完成对应的策略、Lint、构建与真机验收。
2. 在日期验证记录中保存命令、通过/失败/未执行项、APK 校验和、设备场景和实际限制。
3. 明确覆盖安装还是 Debug 并存；每次只操作用户确认的当前设备，先核对原安装签名。
4. 检查发布范围不含 keystore、密码、私有媒体、设备诊断或构建缓存。公开 Git 发布须先按全局 `safe-open-source-release` 流程审计。

本地提交、生成 APK、设备安装、公开推送、应用商店发布是不同动作，分别记录，不将任一步写成完整发布完成。
