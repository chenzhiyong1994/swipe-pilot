#requires -Version 7.0
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Resolve-ProjectJava {
    param([string]$JavaHome)
    if (-not $JavaHome) {
        $JavaHome = if ($env:SWIPE_JAVA_HOME) { $env:SWIPE_JAVA_HOME } else { $env:JAVA_HOME }
    }
    if ($JavaHome) {
        $javaCommand = Join-Path $JavaHome 'bin/java.exe'
        $javacCommand = Join-Path $JavaHome 'bin/javac.exe'
        if (-not (Test-Path -LiteralPath $javaCommand -PathType Leaf) -or
            -not (Test-Path -LiteralPath $javacCommand -PathType Leaf)) {
            throw "指定 JDK 缺少 java/javac：$JavaHome"
        }
    } else {
        $javaCommand = (Get-Command java -ErrorAction Stop).Source
        $javacCommand = Join-Path (Split-Path $javaCommand) 'javac.exe'
        if (-not (Test-Path -LiteralPath $javacCommand -PathType Leaf)) {
            throw 'PATH 上的 java 不是完整 JDK；请传入 -JavaHome 或设置 JAVA_HOME。'
        }
        $JavaHome = Split-Path (Split-Path $javaCommand)
    }
    $version = (& $javaCommand -version 2>&1 | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or $version -notmatch 'version "17\.') {
        throw "工程要求 JDK 17，实际为：$version"
    }
    return [pscustomobject]@{ Home = $JavaHome; Java = $javaCommand; Javac = $javacCommand; Version = $version }
}

function Resolve-ProjectSdk {
    param([string]$AndroidSdk, [string]$ProjectRoot)
    if (-not $AndroidSdk) {
        $properties = Join-Path $ProjectRoot 'local.properties'
        if (Test-Path -LiteralPath $properties -PathType Leaf) {
            $line = Get-Content -LiteralPath $properties | Where-Object { $_ -match '^\s*sdk\.dir\s*=' } | Select-Object -First 1
            if ($line) { $AndroidSdk = ($line -replace '^\s*sdk\.dir\s*=\s*', '').Replace('\:', ':').Replace('\\', '\').Replace('\ ', ' ') }
        }
        if (-not $AndroidSdk) {
            $AndroidSdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { $env:ANDROID_SDK_ROOT }
        }
    }
    if (-not $AndroidSdk) { throw '请传入 -AndroidSdk、设置 ANDROID_HOME，或在 local.properties 中声明 sdk.dir。' }
    if (-not (Test-Path -LiteralPath $AndroidSdk -PathType Container)) { throw "指定 Android SDK 不存在：$AndroidSdk" }
    return (Resolve-Path -LiteralPath $AndroidSdk).Path
}
