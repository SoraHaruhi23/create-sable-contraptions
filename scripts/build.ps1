param(
    [string]$JavaHome = $env:JAVA_HOME,
    [switch]$Offline
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
if (-not $JavaHome -or -not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin/java.exe'))) {
    throw 'Set JAVA_HOME to a Java 21 JDK, or pass -JavaHome <JDK directory>.'
}
$env:JAVA_HOME = $JavaHome
$env:GRADLE_USER_HOME = Join-Path $projectRoot '.gradle-user-home'
$buildArgs = @('build', '--console=plain')
if ($Offline) { $buildArgs += '--offline' }
# Java does not automatically use HTTPS_PROXY. Forward only a valid HTTP proxy URL.
$proxyUri = $null
if ([Uri]::TryCreate($env:HTTPS_PROXY, [UriKind]::Absolute, [ref]$proxyUri) -and $proxyUri.Scheme -eq 'http') {
    $buildArgs += "-Dhttps.proxyHost=$($proxyUri.Host)", "-Dhttps.proxyPort=$($proxyUri.Port)"
    $buildArgs += "-Dhttp.proxyHost=$($proxyUri.Host)", "-Dhttp.proxyPort=$($proxyUri.Port)"
}
Push-Location $projectRoot
try {
    $localGradle = Join-Path $projectRoot '.tools/gradle-8.14.3/bin/gradle.bat'
    if (Test-Path -LiteralPath $localGradle) { & $localGradle @buildArgs }
    else { & (Join-Path $projectRoot 'gradlew.bat') @buildArgs }
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" }
} finally {
    Pop-Location
}
