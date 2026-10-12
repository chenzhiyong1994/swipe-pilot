#requires -Version 7.0
param([string]$JavaHome, [string]$AndroidSdk)
. (Join-Path $PSScriptRoot 'environment.ps1')
$projectRoot = Split-Path $PSScriptRoot
try {
    $jdk = Resolve-ProjectJava -JavaHome $JavaHome
    $sdk = Resolve-ProjectSdk -AndroidSdk $AndroidSdk -ProjectRoot $projectRoot
    Write-Output "JDK: $($jdk.Home)"
    Write-Output $jdk.Version
    Write-Output "Android SDK: $sdk"
    foreach ($relative in @('platforms/android-36/android.jar', 'build-tools/35.0.0/aapt2.exe', 'build-tools/35.0.0/apksigner.bat', 'platform-tools/adb.exe')) {
        if (-not (Test-Path -LiteralPath (Join-Path $sdk $relative) -PathType Leaf)) { throw "SDK 缺少 $relative；用 SDK Manager 安装对应包。" }
        Write-Output "OK: $relative"
    }
    foreach ($relative in @('gradlew.bat', 'gradle/wrapper/gradle-wrapper.jar', 'gradle/wrapper/gradle-wrapper.properties', 'version.properties')) {
        if (-not (Test-Path -LiteralPath (Join-Path $projectRoot $relative) -PathType Leaf)) { throw "工程缺少 $relative" }
    }
    Write-Output 'PASS: 本地构建依赖齐备。首次 Gradle 构建还需要解析构建依赖；本命令不安装工具。'
} catch { Write-Error $_; exit 1 }
