#requires -Version 7.0
param(
    [Parameter(Mandatory)][ValidatePattern('^[A-Za-z0-9._:-]+$')][string]$Serial,
    [ValidateSet('com.local.swipepilot', 'com.local.swipepilot.debug')][string]$Package = 'com.local.swipepilot',
    [string]$AndroidSdk,
    [string]$OutputDirectory
)
. (Join-Path $PSScriptRoot 'environment.ps1')
$projectRoot = Split-Path $PSScriptRoot
$sdk = Resolve-ProjectSdk -AndroidSdk $AndroidSdk -ProjectRoot $projectRoot
$adb = Join-Path $sdk 'platform-tools/adb.exe'
if (-not (Test-Path -LiteralPath $adb -PathType Leaf)) { throw "找不到 ADB：$adb" }
# A serial is always explicit; never select or modify every connected device.
$connection = & $adb -s $Serial get-state 2>&1
if ($LASTEXITCODE -ne 0 -or ($connection | Out-String).Trim() -ne 'device') { throw "设备 $Serial 未连接或未授权：$connection" }
if (-not $OutputDirectory) { $OutputDirectory = Join-Path $projectRoot ('diagnostics/' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + $Serial.Replace(':', '_')) }
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$captures = [ordered]@{
    'device.txt' = @('shell', 'getprop')
    'accessibility-enabled.txt' = @('shell', 'settings', 'get', 'secure', 'accessibility_enabled')
    'enabled-services.txt' = @('shell', 'settings', 'get', 'secure', 'enabled_accessibility_services')
    'accessibility.txt' = @('shell', 'dumpsys', 'accessibility')
    'package.txt' = @('shell', 'dumpsys', 'package', $Package)
    'exit-info.txt' = @('shell', 'dumpsys', 'activity', 'exit-info', $Package)
    'power.txt' = @('shell', 'dumpsys', 'power')
    'deviceidle.txt' = @('shell', 'dumpsys', 'deviceidle')
    'logcat.txt' = @('logcat', '-d', '-v', 'threadtime', '-t', '2000', 'SwipePilot:I', 'AndroidRuntime:E', 'ActivityManager:I', '*:S')
}
$failures = @()
foreach ($capture in $captures.GetEnumerator()) {
    $commandArguments = $capture.Value
    $result = & $adb -s $Serial @commandArguments 2>&1
    $exitCode = $LASTEXITCODE
    $result | Set-Content -LiteralPath (Join-Path $OutputDirectory $capture.Key) -Encoding utf8
    if ($exitCode -ne 0) { $failures += "$($capture.Key): exit $exitCode" }
}
@("Captured at: $([DateTimeOffset]::Now.ToString('o'))", "Package: $Package", 'Only read-only ADB commands were issued; no UI tree, authorization changes, install or uninstall.', 'System logs may contain device/app information; review before sharing.') + $failures |
    Set-Content -LiteralPath (Join-Path $OutputDirectory 'README.txt') -Encoding utf8
Write-Output "诊断资料：$((Resolve-Path -LiteralPath $OutputDirectory).Path)"
if ($failures.Count -gt 0) { Write-Error "部分系统命令不受此设备支持：$($failures -join ', ')"; exit 1 }
