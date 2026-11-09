#requires -Version 7.0
[CmdletBinding()]
param(
    [string]$GameJar = (Join-Path $PSScriptRoot 'lib/projectzomboid.jar'),
    [string]$RuntimeJar = (Join-Path $PSScriptRoot 'build/slim/modcore-payload.jar'),
    [string]$InstallerJar,
    [string]$OutputDirectory = (Join-Path $PSScriptRoot 'build/dist'),
    [string]$JavaExecutable
)

$ErrorActionPreference = 'Stop'
$game = (Resolve-Path -LiteralPath $GameJar).Path
$runtime = (Resolve-Path -LiteralPath $RuntimeJar).Path
if (-not $InstallerJar) {
    $candidates = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'build') -Filter 'modcore-*.jar' -File)
    if ($candidates.Count -ne 1) { throw 'Build first or provide an exact InstallerJar; multiple artifacts must not be guessed.' }
    $InstallerJar = $candidates[0].FullName
}
$installer = (Resolve-Path -LiteralPath $InstallerJar).Path
if (-not $JavaExecutable) {
    if ($env:JAVA_HOME) { $JavaExecutable = Join-Path $env:JAVA_HOME 'bin/java.exe' }
    else {
        foreach ($directory in ($env:PATH -split [IO.Path]::PathSeparator)) {
            if (-not $directory) { continue }
            $candidate = Join-Path $directory.Trim('"') 'java.exe'
            if (Test-Path -LiteralPath $candidate -PathType Leaf) { $JavaExecutable = $candidate; break }
        }
    }
}
if (-not $JavaExecutable -or -not (Test-Path -LiteralPath $JavaExecutable -PathType Leaf)) {
    throw 'Install a Windows x64 full JDK 25. Set JAVA_HOME or add its bin folder to PATH; an invalid JAVA_HOME is not replaced.'
}
$java = (Resolve-Path -LiteralPath $JavaExecutable).Path
& $java '-cp' $installer 'modcore.attach.JdkRequirement'
if ($LASTEXITCODE -ne 0) { throw 'External Java check failed; a Windows x64 full JDK 25 is required. No payload was prepared.' }
$output = [System.IO.Path]::GetFullPath($OutputDirectory)
$gameDirectory = [System.IO.Path]::GetDirectoryName($game)
if ($output.Equals($gameDirectory, [StringComparison]::OrdinalIgnoreCase) -or
    $output.StartsWith($gameDirectory + [System.IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'The output directory must be separate from the game JAR directory.'
}
$workspace = Split-Path -Parent $PSScriptRoot
$sandbox = Join-Path $workspace 'temp/instrumentation-prepare'
foreach ($part in @('work', 'home', 'tmp')) {
    New-Item -ItemType Directory -Path (Join-Path $sandbox $part) -Force | Out-Null
}
New-Item -ItemType Directory -Path $output -Force | Out-Null
$arguments = @(('-Duser.home=' + (Join-Path $sandbox 'home')), ('-Djava.io.tmpdir=' + (Join-Path $sandbox 'tmp')))
$arguments += @('-jar', $installer, '--prepare-runtime', $game, $runtime, (Join-Path $output 'runtime.jar'))
$previousAppData = $env:LOCALAPPDATA
Push-Location -LiteralPath (Join-Path $sandbox 'work')
try {
    $env:LOCALAPPDATA = Join-Path $sandbox 'appdata'
    & $java @arguments
    $result = $LASTEXITCODE
} finally {
    $env:LOCALAPPDATA = $previousAppData
    Pop-Location
}
if ($result -ne 0) { exit $result }
Write-Host ('Prepared runtime: ' + (Join-Path $output 'runtime.jar'))
Write-Host 'This prepares a development artifact only. Use the three-file installer for a complete managed deployment.'
Write-Host 'This script does not install or modify the game.'
