#requires -Version 7.0
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$GameDirectory,
    [string]$DeploymentDirectory = (Join-Path ([Environment]::GetFolderPath('UserProfile')) 'Zomboid/modcore-agent'),
    [string]$JavaExecutable,
    [string]$CoordinatorJavaExecutable,
    [string]$UserHome = ([Environment]::GetFolderPath('UserProfile')),
    [switch]$VerifyOnly,
    [string[]]$GameArguments = @()
)

$ErrorActionPreference = 'Stop'
$game = (Resolve-Path -LiteralPath $GameDirectory).Path
$deployment = (Resolve-Path -LiteralPath $DeploymentDirectory).Path
$userDirectory = (Resolve-Path -LiteralPath $UserHome).Path
if (-not $JavaExecutable) { $JavaExecutable = Join-Path $game 'jre64/bin/java.exe' }
$java = (Resolve-Path -LiteralPath $JavaExecutable).Path
if (-not $CoordinatorJavaExecutable) {
    if ($env:JAVA_HOME) { $CoordinatorJavaExecutable = Join-Path $env:JAVA_HOME 'bin/java.exe' }
    else {
        foreach ($directory in ($env:PATH -split [IO.Path]::PathSeparator)) {
            if (-not $directory) { continue }
            $candidate = Join-Path $directory.Trim('"') 'java.exe'
            if (Test-Path -LiteralPath $candidate -PathType Leaf) { $CoordinatorJavaExecutable = $candidate; break }
        }
    }
}
if (-not $CoordinatorJavaExecutable -or -not (Test-Path -LiteralPath $CoordinatorJavaExecutable -PathType Leaf)) {
    throw 'Install a Windows x64 full JDK 25. Set JAVA_HOME or add its bin folder to PATH; an invalid JAVA_HOME is not replaced.'
}
$coordinatorJava = (Resolve-Path -LiteralPath $CoordinatorJavaExecutable).Path
& $coordinatorJava '-cp' (Join-Path $deployment 'coordinator.jar') 'modcore.attach.JdkRequirement'
if ($LASTEXITCODE -ne 0) { throw 'External Java check failed; a Windows x64 full JDK 25 is required. No game was started.' }
$arguments = [Collections.Generic.List[string]]::new()
foreach ($value in @('-cp',(Join-Path $deployment 'coordinator.jar'),'modcore.attach.LaunchCoordinator',
    '--game-dir',$game,'--user-home',$userDirectory,'--deployment',$deployment,'--java',$java)) { $arguments.Add($value) }
if ($VerifyOnly) {
    $arguments.Add('--verify')
} else {
    $config = [IO.File]::ReadAllText((Join-Path $game 'ProjectZomboid64.json')) | ConvertFrom-Json
    $arguments.Add('--')
    foreach ($value in $config.vmArgs) { $arguments.Add([string]$value) }
    $arguments.Add('-cp')
    $arguments.Add('./;projectzomboid.jar')
    $arguments.Add('zombie.gameStates.MainScreenState')
    foreach ($value in $GameArguments) { $arguments.Add($value) }
}
& $coordinatorJava @arguments
exit $LASTEXITCODE
