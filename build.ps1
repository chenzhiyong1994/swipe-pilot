#requires -Version 7.0
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Tasks
)

$ErrorActionPreference = 'Stop'
if ($env:SWIPE_JAVA_HOME) {
    $taskJavaName = if ($IsWindows) { 'java.exe' } else { 'java' }
    $taskJavaPath = Join-Path $env:SWIPE_JAVA_HOME "bin/$taskJavaName"
    if (-not (Test-Path -LiteralPath $taskJavaPath -PathType Leaf)) {
        throw 'SWIPE_JAVA_HOME must point to an existing JDK installation.'
    }
    $env:JAVA_HOME = $env:SWIPE_JAVA_HOME
}
if (-not $Tasks -or $Tasks.Count -eq 0) {
    $Tasks = @(':policy:test', ':app:lintDebug', ':app:assembleDebug')
}

Push-Location $PSScriptRoot
try {
    & (Join-Path $PSScriptRoot 'gradlew.bat') @Tasks
    $buildExitCode = $LASTEXITCODE
} finally {
    Pop-Location
}
exit $buildExitCode
