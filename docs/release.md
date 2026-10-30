# 发布与签名

轻滑助手的用户安装包使用独立发布密钥签名，应用名为“轻滑助手”，包名为 `com.local.swipepilot`，Release 不可调试。当前版本为 1.3.3 / versionCode 9，由根目录 `version.properties` 提供。

用户从 [GitHub Releases](https://github.com/chenzhiyong1994/swipe-pilot/releases/latest) 下载安装包。公开文件名为 `SwipePilot-1.3.3.apk`，同页提供 `SHA256SUMS` 校验文件。

发布证书 SHA-256：`02a470f00436e136a03aa0313aacdc5c39b2606e7e725cc4f581dd8948610209`。证书摘要可公开，私钥与密码保存在仓库外。

## 构建变体

| 项目 | Debug | Release |
|---|---|---|
| 应用名 | 轻滑助手·开发版 | 轻滑助手 |
| 包名 | `com.local.swipepilot.debug` | `com.local.swipepilot` |
| 用途 | 本地开发与设备验证 | 用户安装与分发 |
| 签名 | Gradle Debug 签名 | 项目发布密钥 |
| 设置和授权 | 与 Release 独立 | 后续同签名升级可保留 |

发布密钥须安全备份，并在后续升级中保持一致。签名不同的 APK 无法覆盖安装；不能通过擅自卸载应用或清数据解决冲突。Debug 包不作为用户正式下载入口。

## 配置与构建

仅在本地环境或 CI Secret 中设置以下四项，不写入源码、日志或文档：

| 环境变量 | 值 |
|---|---|
| `SWIPE_KEYSTORE` | 发布 keystore 的绝对路径 |
| `SWIPE_KEY_ALIAS` | 密钥别名 |
| `SWIPE_STORE_PASSWORD` | keystore 密码 |
| `SWIPE_KEY_PASSWORD` | 密钥密码 |

四项全部为空时，`assembleRelease` 仅生成 unsigned APK，不能安装或发布；部分配置缺失时构建报错，不生成替代密钥。用于分发时须提供完整有效配置：

```powershell
.\gradlew.bat :app:assembleRelease
```

输出在 `app/build/outputs/apk/release/`。按实际产物核对包名、版本、SDK、不可调试属性和证书，并用 SDK 的 `apksigner verify --verbose --print-certs <apk>` 验证签名，再计算 SHA-256。不能分发 `*-unsigned.apk`。

## 发布检查

1. 在 `version.properties` 配置版本并同步 `CHANGELOG.md`，完成改动所需检查。
2. 在日期验证记录中保存检查结果、APK 校验和、证书摘要及实际设备限制。
3. 将已签名 Release APK 命名为 `SwipePilot-<version>.apk`，与 `SHA256SUMS` 一起发布到对应版本 tag，并核对下载地址。
4. 更新 README 下载链接；文件名含版本号，切换最新版本时须一并修改。
5. 确认公开内容不含私钥、密码、私人媒体、设备诊断或构建缓存。

本地构建、签名验证、设备安装与公开发布分别记录。公开 Git 发布按项目适用的安全发布流程执行，不把签名成功等同于真机验收通过。
