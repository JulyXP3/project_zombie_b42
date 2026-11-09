#requires -Version 7.0
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$JavaHome,
    [Parameter(Mandatory = $true)][string]$GameDir,
    [ValidateSet('Full', 'Fast')][string]$Mode = 'Full',
    [switch]$SkipDeploymentTests
)

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$lock = $null
$result = 1
try {
    . (Join-Path $PSScriptRoot 'prepare-native-fixture.ps1')
    $jdk = Get-BuildPath $JavaHome
    Assert-BuildPath $jdk
    & (Join-Path $PSHOME 'pwsh.exe') -NoLogo -NoProfile -NonInteractive -File (Join-Path $PSScriptRoot 'check-build-jdk.ps1') -JavaHome $jdk
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    $inputPaths = Get-GameBuildInput $GameDir (Join-Path $PSScriptRoot 'lib/projectzomboid.jar') $PSScriptRoot -Native:($Mode -eq 'Full')
    if (Test-BuildPathInside $jdk (Get-BuildPath $GameDir)) {
        throw 'The build JDK must be an external full JDK 25, not the game runtime.'
    }

    $temp = Split-Path -Parent $inputPaths.Store
    Assert-BuildPath $temp
    [IO.Directory]::CreateDirectory($temp) | Out-Null
    $lockPath = Join-Path $temp '.build-entry.lock'
    Assert-BuildPath $lockPath
    try { $lock = [IO.File]::Open($lockPath, [IO.FileMode]::OpenOrCreate, [IO.FileAccess]::ReadWrite, [IO.FileShare]::None) }
    catch { throw "Another build is running, or the build lock is in use: $lockPath" }

    $env:JAVA_HOME = $jdk
    Remove-Item Env:MODCORE_NATIVE_FIXTURE -ErrorAction SilentlyContinue
    if ($Mode -eq 'Full') {
        $env:MODCORE_NATIVE_FIXTURE = Get-PreparedNativeFixture $inputPaths
        $gradleArguments = @('clean', 'test', 'assemble', '--continue', '--console=plain', '--no-daemon')
        if ($SkipDeploymentTests) { $gradleArguments += '-PskipDeploymentTests' }
    } else {
        $gradleArguments = @('clean', 'jar', '--console=plain', '--no-daemon', '-x', 'test')
    }
    Write-Host "Build Java: $jdk"
    Push-Location -LiteralPath $PSScriptRoot
    try {
        & (Join-Path $PSScriptRoot 'gradlew.bat') @gradleArguments
        $result = $LASTEXITCODE
    } finally { Pop-Location }
} catch {
    Write-Host "[ERROR] $($_.Exception.Message)"
    $result = 1
} finally {
    if ($null -ne $lock) { $lock.Dispose() }
}
exit $result
