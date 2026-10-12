#requires -Version 7.0
param([string]$JavaHome, [switch]$Repro)
. (Join-Path $PSScriptRoot 'environment.ps1')
$projectRoot = Split-Path $PSScriptRoot
$jdk = Resolve-ProjectJava -JavaHome $JavaHome
$output = Join-Path $projectRoot 'policy/build/offline-tests'
New-Item -ItemType Directory -Path $output -Force | Out-Null
$sources = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'policy/src/main/java') -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
$testRoot = Join-Path $projectRoot 'policy/src/test/java/com/local/swipepilot'
$sources += Join-Path $testRoot 'PolicyTest.java'
$sources += Join-Path $testRoot 'AdWaitRepro.java'
& $jdk.Javac -encoding UTF-8 --release 8 -d $output @sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& $jdk.Java '-Dfile.encoding=UTF-8' -cp $output com.local.swipepilot.PolicyTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if ($Repro) {
    & $jdk.Java '-Dfile.encoding=UTF-8' -cp $output com.local.swipepilot.AdWaitRepro
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
